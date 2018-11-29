package com.despegar.jdbc.galera;

import com.codahale.metrics.MetricRegistry;
import com.despegar.jdbc.galera.consistency.ConsistencyLevel;
import com.despegar.jdbc.galera.listener.GaleraClientListener;
import com.despegar.jdbc.galera.listener.GaleraClientLoggingListener;
import com.despegar.jdbc.galera.metrics.PoolMetrics;
import com.despegar.jdbc.galera.policies.ElectionNodePolicy;
import com.despegar.jdbc.galera.policies.RoundRobinPolicy;
import com.despegar.jdbc.galera.settings.ClientSettings;
import com.despegar.jdbc.galera.settings.DiscoverSettings;
import com.despegar.jdbc.galera.settings.PoolSettings;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class GaleraClient extends AbstractGaleraDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(GaleraClient.class);
    public static MetricRegistry metricRegistry = new MetricRegistry();

    protected Map<String, GaleraNode> nodes = new ConcurrentHashMap<String, GaleraNode>();
    private List<String> activeNodes = new CopyOnWriteArrayList<String>();
    private List<String> downedNodes = new CopyOnWriteArrayList<String>();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private GaleraDB galeraDB;
    private PoolSettings poolSettings;
    private PoolSettings internalPoolSettings;
    private DiscoverSettings discoverSettings;
    private ClientSettings clientSettings;
    private AtomicBoolean isDiscoveryRunning = new AtomicBoolean(false);
    private Runnable discoverRunnable = new Runnable() {
        @Override
        public void run() {
            discovery();
        }
    };

    protected GaleraClient(ClientSettings clientSettings, DiscoverSettings discoverSettings, GaleraDB galeraDB, PoolSettings poolSettings,
                           PoolSettings internalPoolSettings) {
        this.galeraDB = galeraDB;
        this.poolSettings = poolSettings;
        this.internalPoolSettings = internalPoolSettings;
        this.discoverSettings = discoverSettings;
        this.clientSettings = clientSettings;
        registerNodes(clientSettings.seeds);
        startDiscovery(discoverSettings.discoverPeriod);
    }

    public static GaleraClient.Builder newBuilder() {
        return new GaleraClient.Builder();
    }

    private void discovery() {

        if (!isDiscoveryRunning.compareAndSet(false, true)) {
            LOG.info("Skipping discovery because it is already running");
            return;
        }

        if (nodes.isEmpty()) {
            //This should only happen if all nodes on cluster went down. Reinitializing with seeds
            LOG.info("Reinitializing from seeds. Did all nodes go down?");
            registerNodes(clientSettings.seeds);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Discovering Galera cluster...");
        }
        try {
            discoverActiveNodes();
            testDownedNodes();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Active nodes: {},  Downed nodes: {}", activeNodes, downedNodes);
            }
        } catch (Throwable reason) {
            LOG.error("Galera discovery failed", reason);
        }

        isDiscoveryRunning.compareAndSet(true, false);

        if (!clientSettings.testMode && poolSettings.metricsEnabled) {
            PoolMetrics.reportMetrics(metricRegistry, nodes, clientSettings.galeraClientListener, poolSettings.poolName);
        }

    }

    private void testDownedNodes() {
        for (String downedNode : downedNodes) {
            try {
                discover(downedNode);
                GaleraNode galeraNode = nodes.get(downedNode);
                if (galeraNode != null && !(galeraNode.status().isDonor() && discoverSettings.ignoreDonor) && galeraNode.status().isPrimary()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Will activate a previous downed node: {}", downedNode);
                    }
                    activate(downedNode);
                }
            } catch (Exception e) {
                down(downedNode, e.getMessage());
            }
        }
    }

    private void activate(String downedNode) {
        if (!activeNodes.contains(downedNode)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Activating node:  {}", downedNode);
            }

            nodes.get(downedNode).onActivate();
            activeNodes.add(downedNode);
            downedNodes.remove(downedNode);

            clientSettings.galeraClientListener.onActivatingNode(downedNode);
        }
    }

    private void down(String node, String cause) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Marking node {} as down due to {}", node, cause);
        }
        activeNodes.remove(node);
        if (!downedNodes.contains(node)) {
            downedNodes.add(node);
        }
        closeConnections(node);

        clientSettings.galeraClientListener.onMarkingNodeAsDown(node, cause);
    }

    private void discoverActiveNodes() {
        for (String node : activeNodes) {
            try {
                discover(node);
            } catch (Exception e) {
                down(node, "failure in connection. " + e.getMessage());
            }
        }
    }

    private void removeNode(String node) {
        activeNodes.remove(node);
        downedNodes.remove(node);
        shutdownGaleraNode(node);
        nodes.remove(node);

        clientSettings.galeraClientListener.onRemovingNode(node);
    }

    private void closeConnections(String node) {
        GaleraNode galeraNode = nodes.get(node);
        if (galeraNode != null) {
            galeraNode.onDown();
        }
    }

    private void shutdownGaleraNode(String node) {
        LOG.info("Shutting down galera node {}", node);
        GaleraNode galeraNode = nodes.get(node);
        if (galeraNode != null) {
            galeraNode.shutdown();
        }
    }

    private void discover(String node) throws Exception {
        LOG.trace("Discovering {}...", node);

        GaleraStatus status = null;
        try {
            status = refreshStatus(node);
        } catch (Exception e) {
            LOG.error("We could not refresh node status for " + node + " so we remove it", e);
            removeNode(node);
            return;
        }

        if (!status.isPrimary()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("On discover - Non primary node {}", node);
            }
            down(node, "non Primary");
            return;
        }

        if (!status.isSynced() && (discoverSettings.ignoreDonor || !status.isDonor())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("On discover - State not ready [{}] - Ignore donor [{}] : {}", status.state(), discoverSettings.ignoreDonor, node);
            }
            down(node, "state not ready: " + status.state());
            return;
        }

        Collection<String> discoveredNodes = status.getClusterNodes();
        LOG.trace("Cluster nodes: {}", discoveredNodes);

        for (String discoveredNode : discoveredNodes) {
            if (isNewNodeOnCluster(discoveredNode)) {
                LOG.info("Found new node {}. Actual nodes {}", discoveredNode, nodes.keySet());
                registerNode(discoveredNode);
            }
        }
        if (!discoveredNodes.contains(node)) {
            removeNode(node);
        } else {
            if (!isActive(node) && !(status.isDonor() && discoverSettings.ignoreDonor)) {
                LOG.info("Will activate a discovered node: {}", node);
                activate(node);
            }
        }
    }

    private GaleraStatus refreshStatus(String node) throws Exception {
        if (clientSettings.testMode) {
            return GaleraStatus.buildTestStatusOk(node);
        }

        GaleraNode galeraNode = nodes.get(node);
        galeraNode.refreshStatus();
        return galeraNode.status();
    }

    private boolean isActive(String node) {
        return activeNodes.contains(node);
    }

    private boolean isNewNodeOnCluster(String discoveredNode) {
        return !nodes.containsKey(discoveredNode);
    }

    private void startDiscovery(long discoverPeriod) {
        if (!clientSettings.testMode) {
            scheduler.scheduleAtFixedRate(discoverRunnable, 0, discoverPeriod, TimeUnit.MILLISECONDS);
        }
    }

    private void registerNodes(Collection<String> seeds) {
        for (String seed : seeds) {
            if (isNewNodeOnCluster(seed)) {
                registerNode(seed);
            }
        }
    }

    private void registerNode(String node) {
        LOG.info("Registering Galera node: {}", node);
        try {
            nodes.put(node, new GaleraNode(node, galeraDB, poolSettings, internalPoolSettings, clientSettings.testMode));
            discover(node);
        } catch (Exception e) {
            LOG.error("Could not register node " + node, e);
            down(node, "failure in connection. Reason: " + e.getMessage());
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            return selectNode(null).getConnection();
        } catch (Exception e) {
            LOG.info("Error getting connection. Forcing discovery...");
            discovery();
            throw e;
        }
    }

    /**
     * @param electionNodePolicy Policy to choose the node that will get a connection. If it is null, we will use the default policy configured on client.
     * @return a {@link Connection}
     * @throws SQLException - if a database access error occurs
     */
    public Connection getConnection(ElectionNodePolicy electionNodePolicy) throws SQLException {
        return getConnection(null, electionNodePolicy);
    }

    /**
     * @param consistencyLevel   Set the consistencyLevel needed.
     * @param electionNodePolicy Policy to choose the node that will get a connection. If it is null, we will use the default policy configured on client.
     * @return a {@link Connection}
     * @throws SQLException - if a database access error occurs
     */
    public Connection getConnection(ConsistencyLevel consistencyLevel, ElectionNodePolicy electionNodePolicy) throws SQLException {
        ElectionNodePolicy policy = (electionNodePolicy != null) ? electionNodePolicy : clientSettings.defaultNodeSelectionPolicy;
        GaleraNode galeraNode = selectNode(policy);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Getting connection [{}] from node {}", policy.getName(), galeraNode.node);
        }
        try {
            if (consistencyLevel != null) {
                return galeraNode.getConnection(consistencyLevel);
            } else {
                return galeraNode.getConnection();
            }

        } catch (Exception e) {
            LOG.info("Error getting connection. Forcing discovery...");
            discovery();
            throw e;
        }
    }

    protected GaleraNode selectNode(@Nullable ElectionNodePolicy electionNodePolicy) {
        return getActiveGaleraNode(1, electionNodePolicy);
    }

    private GaleraNode getActiveGaleraNode(int retry, @Nullable ElectionNodePolicy electionNodePolicy) {
        if (activeNodes.isEmpty()) {
            LOG.error("Could not get galera node cause there is no active node");
            throw new NoActiveNodeException();
        }
        if (retry <= clientSettings.retriesToGetConnection) {
            try {
                ElectionNodePolicy policy = (electionNodePolicy != null) ?
                        electionNodePolicy :
                        clientSettings.defaultNodeSelectionPolicy;
                GaleraNode galeraNode = nodes.get(policy.chooseNode(activeNodes));

                return galeraNode != null ? galeraNode : getActiveGaleraNode(++retry, electionNodePolicy);

            } catch (Exception exception) {
                LOG.warn("Error getting active galera node. Retry {}/{}. Reason {}", retry, clientSettings.retriesToGetConnection, exception);
                return getActiveGaleraNode(++retry, electionNodePolicy);
            }
        } else {
            LOG.error("NoHostAvailableException selecting an active galera node. Max attempts reached");
            throw new NoHostAvailableException(activeNodes);
        }
    }

    public void shutdown() {
        LOG.info("Shutting down Galera Client...");

        shutdownDiscoverScheduler();
        shutdownActiveNodes();
    }

    private void shutdownActiveNodes() {
        try {
            for (String activeNode : activeNodes) {
                shutdownGaleraNode(activeNode);
            }
        } catch (Exception e) {
            LOG.warn("Error closing active node pools", e);
        }
    }

    private void shutdownDiscoverScheduler() {
        try {
            scheduler.shutdown();
        } catch (Exception e) {
            LOG.warn("Error closing status scheduler", e);
        }
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        for (GaleraNode galeraNode : nodes.values()) {
            if (galeraNode.getLogWriter() != null) {
                return galeraNode.getLogWriter();
            }
        }

        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        for (GaleraNode galeraNode : nodes.values()) {
            galeraNode.setLogWriter(out);
        }
    }

    public static class Builder {

        private boolean testMode = false;
        private String database;
        private String user;
        private String password = "";
        private Optional<String> jdbcUrlPrefix = Optional.absent();
        private Optional<String> jdbcUrlSeparator = Optional.absent();
        private Optional<String> driverClassName = Optional.absent();
        private String seeds;
        private int maxConnectionsPerHost;
        private int minConnectionsIdlePerHost = 1;
        private long discoverPeriod;
        private long connectTimeout;
        private long connectionTimeout;
        private long readTimeout;
        private long idleTimeout;
        private boolean ignoreDonor = true;
        private int retriesToGetConnection = 3;
        private boolean autocommit = true; //JDBC default.
        private boolean readOnly = false;
        private boolean metricsEnabled = false;
        private String isolationLevel = "TRANSACTION_READ_COMMITTED";
        private ConsistencyLevel consistencyLevel;
        private Optional<GaleraClientListener> listener = Optional.absent();
        private Optional<ElectionNodePolicy> nodeSelectionPolicy = Optional.absent();
        private Optional<String> poolName = Optional.absent();
        private long leakDetectionThreshold = 0;

        public GaleraClient build() {
            Preconditions.checkState(seeds != null, "Seeds are required");
            Preconditions.checkState(database != null, "Database name is required");

            LOG.info("Creating galera client...");

            ClientSettings clientSettings =
                    new ClientSettings(
                            seeds(),
                            retriesToGetConnection,
                            listener.or(new GaleraClientLoggingListener()),
                            nodeSelectionPolicy.or(new RoundRobinPolicy()),
                            testMode);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Creating galera client with settings: {}", clientSettings);
            }

            DiscoverSettings discoverSettings = new DiscoverSettings(discoverPeriod, ignoreDonor);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Creating galera client with discovery settings: {}", discoverSettings);
            }

            GaleraDB galeraDB = new GaleraDB(database, user, password,
                    jdbcUrlPrefix.or(GaleraDB.MYSQL_URL_PREFIX),
                    jdbcUrlSeparator.or(GaleraDB.MYSQL_URL_SEPARATOR),
                    driverClassName);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Creating galera client with connection settings: {}", galeraDB);
            }

            PoolSettings poolSettings = PoolSettings.newBuilder()
                    .maxConnectionsPerHost(maxConnectionsPerHost)
                    .minConnectionsIdlePerHost(minConnectionsIdlePerHost)
                    .connectTimeout(connectTimeout)
                    .connectionTimeout(connectionTimeout)
                    .readTimeout(readTimeout)
                    .idleTimeout(idleTimeout)
                    .autocommit(autocommit)
                    .readOnly(readOnly)
                    .isolationLevel(isolationLevel)
                    .consistencyLevel(consistencyLevel)
                    .metricsEnabled(metricsEnabled)
                    .poolName(poolName)
                    .leakDetectionThreshold(leakDetectionThreshold)
                    .build();

            PoolSettings internalPoolSettings = PoolSettings.newBuilder()
                    .maxConnectionsPerHost(8)
                    .minConnectionsIdlePerHost(4)
                    .connectTimeout(connectTimeout)
                    .connectionTimeout(connectionTimeout)
                    .readTimeout(readTimeout)
                    .idleTimeout(idleTimeout)
                    .autocommit(false)
                    .readOnly()
                    .isolationLevel(isolationLevel)
                    .metricsEnabled(false)
                    .poolName(poolName)
                    .leakDetectionThreshold(leakDetectionThreshold)
                    .build();


            return new GaleraClient(clientSettings, discoverSettings, galeraDB, poolSettings, internalPoolSettings);
        }

        private List<String> seeds() {
            return Splitter.on(",").omitEmptyStrings().trimResults().splitToList(seeds);
        }

        /**
         * @param seeds Comma separated list of seeds with format {hostname}:{port},...
         * @return Builder instance
         */
        public Builder seeds(String seeds) {
            this.seeds = seeds;
            return this;
        }

        /**
         * @param jdbcUrlPrefix should be something like 'jdbc:mysql://'
         * @return Builder instance
         */
        public Builder jdbcUrlPrefix(String jdbcUrlPrefix) {
            this.jdbcUrlPrefix = Optional.fromNullable(jdbcUrlPrefix);
            return this;
        }

        public Builder jdbcUrlSeparator(String jdbcUrlSeparator) {
            this.jdbcUrlSeparator = Optional.fromNullable(jdbcUrlSeparator);
            return this;
        }

        public Builder driverClassName(String driverClassName) {
            this.driverClassName = Optional.fromNullable(driverClassName);
            return this;
        }

        public Builder database(String database) {
            this.database = database;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder poolName(Optional<String> poolName) {
            this.poolName = poolName;
            return this;
        }

        public Builder poolName(String poolName) {
            this.poolName = Optional.fromNullable(poolName);
            return this;
        }

        public Builder maxConnectionsPerHost(int maxConnectionsPerHost) {
            this.maxConnectionsPerHost = maxConnectionsPerHost;
            return this;
        }

        public Builder minConnectionsIdlePerHost(int minConnectionsPerHost) {
            this.minConnectionsIdlePerHost = minConnectionsPerHost;
            return this;
        }

        public Builder connectTimeout(long connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder connectTimeout(long connectTimeout, @Nonnull TimeUnit timeUnit) {
            return connectTimeout(timeUnit.toMillis(connectTimeout));
        }

        public Builder connectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder connectionTimeout(long connectionTimeout, @Nonnull TimeUnit timeUnit) {
            return connectionTimeout(timeUnit.toMillis(connectionTimeout));
        }

        public Builder listener(GaleraClientListener galeraClientListener) {
            this.listener = Optional.fromNullable(galeraClientListener);
            return this;
        }

        public Builder nodeSelectionPolicy(ElectionNodePolicy defaultPolicy) {
            this.nodeSelectionPolicy = Optional.fromNullable(defaultPolicy);
            return this;
        }

        public Builder autocommit(boolean autocommit) {
            this.autocommit = autocommit;
            return this;
        }

        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public Builder isolationLevel(String isolationLevel) {
            this.isolationLevel = isolationLevel;
            return this;
        }

        public Builder consistencyLevel(ConsistencyLevel consistencyLevel) {
            this.consistencyLevel = consistencyLevel;
            return this;
        }


        public Builder testMode(boolean testMode) {
            this.testMode = testMode;
            return this;
        }

        public Builder discoverPeriod(long discoverPeriod) {
            this.discoverPeriod = discoverPeriod;
            return this;
        }

        public Builder discoverPeriod(long discoverPeriod, @Nonnull TimeUnit timeUnit) {
            return discoverPeriod(timeUnit.toMillis(discoverPeriod));
        }

        public Builder readTimeout(long timeout) {
            this.readTimeout = timeout;
            return this;
        }

        public Builder idleTimeout(long timeout) {
            this.idleTimeout = timeout;
            return this;
        }

        public Builder idleTimeout(long idleTimeout, @Nonnull TimeUnit timeUnit) {
            return idleTimeout(timeUnit.toMillis(idleTimeout));
        }

        public Builder ignoreDonor(boolean ignore) {
            this.ignoreDonor = ignore;
            return this;
        }

        public Builder metricsEnabled(boolean metricsEnabled) {
            this.metricsEnabled = metricsEnabled;
            return this;
        }

        public Builder retriesToGetConnection(int retriesToGetConnection) {
            this.retriesToGetConnection = retriesToGetConnection;
            return this;
        }

        public Builder readTimeout(long timeout, @Nonnull TimeUnit timeUnit) {
            return readTimeout(timeUnit.toMillis(timeout));
        }

        public Builder leakDetectionThreshold(long leakDetectionThreshold) {
            this.leakDetectionThreshold = leakDetectionThreshold;
            return this;
        }
    }
}
