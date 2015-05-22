package com.despegar.jdbc.galera;

import com.despegar.jdbc.galera.listener.GaleraClientListener;
import com.despegar.jdbc.galera.listener.GaleraClientLoggingListener;
import com.despegar.jdbc.galera.settings.ClientSettings;
import com.despegar.jdbc.galera.settings.DiscoverSettings;
import com.despegar.jdbc.galera.settings.PoolSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GaleraClient {

    private static final Logger LOG = LoggerFactory.getLogger(GaleraClient.class);

    private Map<String, GaleraNode> nodes = new ConcurrentHashMap<String, GaleraNode>();
    private AtomicInteger nextNodeIndex = new AtomicInteger(new Random().nextInt(997));
    private String masterNode;
    private List<String> activeNodes = new CopyOnWriteArrayList<String>();
    private List<String> downedNodes = new CopyOnWriteArrayList<String>();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private GaleraDB galeraDB;
    private PoolSettings poolSettings;
    private DiscoverSettings discoverSettings;
    private ClientSettings clientSettings;
    private Runnable discoverRunnable = new Runnable() {
        @Override
        public void run() {
            discovery();
        }
    };

    private GaleraClient(ClientSettings clientSettings, DiscoverSettings discoverSettings, GaleraDB galeraDB, PoolSettings poolSettings) {
        this.galeraDB = galeraDB;
        this.poolSettings = poolSettings;
        this.discoverSettings = discoverSettings;
        this.clientSettings = clientSettings;
        registerNodes(clientSettings.seeds);
        startDiscovery(discoverSettings.discoverPeriod);
    }

    private void discovery() {
        try {
            LOG.debug("Discovering Galera cluster...");
            discoverActiveNodes();
            testDownedNodes();
            LOG.debug("Active nodes: {},  Downed nodes: {}", activeNodes, downedNodes);
        } catch (Throwable reason) {
            LOG.error("Galera discovery failed", reason);
        }
    }

    private void testDownedNodes() {
        for (String downedNode : downedNodes) {
            try {
                discover(downedNode);
                if (nodes.containsKey(downedNode) && !(nodes.get(downedNode).status().isDonor() && discoverSettings.ignoreDonor) && nodes.get(downedNode).status().isPrimary()) {
                    LOG.debug("Will activate a previous downed node: {}", downedNode);
                    activate(downedNode);
                }
            } catch (Exception e) {
                down(downedNode, e.getMessage());
            }
        }
    }

    private void activate(String downedNode) {
        if (!activeNodes.contains(downedNode)) {
            LOG.debug("Activating node:  {}", downedNode);

            nodes.get(downedNode).onActivate();
            activeNodes.add(downedNode);
            downedNodes.remove(downedNode);

            clientSettings.galeraClientListener.onActivatingNode(downedNode);
        }
    }

    private void down(String node, String cause) {
        LOG.debug("Marking node {} as down due to {}", node, cause);
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
        LOG.info("Shutting down galera node " + node);
        GaleraNode galeraNode = nodes.get(node);
        if (galeraNode != null) {
            galeraNode.shutdown();
        }
    }

    private void discover(String node) throws Exception {
        LOG.trace("Discovering {}...", node);
        GaleraNode galeraNode = nodes.get(node);
        galeraNode.refreshStatus();
        GaleraStatus status = galeraNode.status();
        if (!status.isPrimary()) {
            LOG.debug("On discover - Non primary node " + node);
            down(node, "non Primary");
            return;
        }

        if (!status.isSynced() && (discoverSettings.ignoreDonor || !status.isDonor())) {
            LOG.debug("On discover - State not ready [" + status.state() + "] - Ignore donor [" + discoverSettings.ignoreDonor + "] : " + node);
            down(node, "state not ready: " + status.state());
            return;
        }

        Collection<String> discoveredNodes = status.getClusterNodes();
        for (String discoveredNode : discoveredNodes) {
            if (isNew(discoveredNode)) {
                LOG.info("Found new node {}. Actual nodes {}", discoveredNode, nodes.keySet());
                registerNode(discoveredNode);
            }
        }
        if (!discoveredNodes.contains(node)) {
            removeNode(node);
        } else {
            if (!isActive(node) && !(nodes.get(node).status().isDonor() && discoverSettings.ignoreDonor)) {
                LOG.info("Will activate a discovered node: {}", node);
                activate(node);
            }
        }
    }

    private boolean isActive(String node) {
        return activeNodes.contains(node);
    }

    private boolean isNew(String discoveredNode) {
        return !nodes.containsKey(discoveredNode);
    }

    private void startDiscovery(long discoverPeriod) {
        scheduler.scheduleAtFixedRate(discoverRunnable, 0, discoverPeriod, TimeUnit.MILLISECONDS);
    }

    private void registerNodes(Collection<String> seeds) {
        for (String seed : seeds) {
            if (isNew(seed)) {
                registerNode(seed);
            }
        }
    }

    private void registerNode(String node) {
        LOG.info("Registering Galera node: {}", node);
        try {
            nodes.put(node, new GaleraNode(node, galeraDB, poolSettings));
            discover(node);
        } catch (Exception e) {
            down(node, "failure in connection. " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        return selectNode(false).getConnection();
    }

    public Connection getConnection(String node, ConsistencyLevel consistencyLevel) throws Exception {
        LOG.debug("Getting connection from node {}", node);
        return nodes.get(node).getConnection(consistencyLevel);
    }

    public Connection getConnection(ConsistencyLevel consistencyLevel, boolean holdsMaster) throws Exception {
        GaleraNode galeraNode = selectNode(holdsMaster);
        LOG.debug("Getting connection from node " + (holdsMaster ? "[master] " : "") + galeraNode.node);
        return galeraNode.getConnection(consistencyLevel);
    }

    private GaleraNode selectNode(boolean holdsMaster) {
        if (holdsMaster) {
            evaluateAndSwapMaster();
            return nodes.get(masterNode);
        }

        return nextActiveGaleraNode(1);
    }

    private synchronized void evaluateAndSwapMaster() {
        if (masterNode == null || !isActive(masterNode) || !nodes.containsKey(masterNode)) {
            String cause = (masterNode == null) ? "it is the first connection asking for a master" : masterNode + " is not active anymore";
            LOG.info("Selecting a master node because of {}", cause);
            masterNode = nextActiveGaleraNode(1).node;

            clientSettings.galeraClientListener.onSelectingNewMaster(masterNode, cause);
        }
    }

    private GaleraNode nextActiveGaleraNode(int retry) {
        if (retry <= clientSettings.retriesToGetConnection) {
            try {
                GaleraNode galeraNode = nodes.get(activeNodes.get(nextNodeIndex()));
                return galeraNode != null ? galeraNode : nextActiveGaleraNode(++retry);
            } catch (IndexOutOfBoundsException outOfBoundsException) {
                return nextActiveGaleraNode(++retry);
            }
        } else {
            LOG.error("NoHostAvailableException selecting an active galera node. Max attempts reached");
            throw new NoHostAvailableException();
        }
    }

    private int nextNodeIndex() {
        int activeNodesCount = activeNodes.size();
        if (activeNodesCount == 0) {
            LOG.error("NoHostAvailableException - Active node count is zero");
            throw new NoHostAvailableException();
        }
        return nextNodeIndex.incrementAndGet() % activeNodesCount;
    }

    public static class Builder {

        private String database;
        private String user;
        private String password;
        private String seeds;
        private int maxConnectionsPerHost;
        private long discoverPeriod;
        private long connectTimeout;
        private long connectionTimeout;
        private long readTimeout;
        private long idleTimeout;
        private boolean ignoreDonor = true;
        private int retriesToGetConnection = 3;
        private GaleraClientListener listener;

        public Builder seeds(String seeds) {
            this.seeds = seeds;
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

        public Builder maxConnectionsPerHost(int maxConnectionsPerHost) {
            this.maxConnectionsPerHost = maxConnectionsPerHost;
            return this;
        }

        public Builder connectTimeout(long connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder connectTimeout(long connectTimeout, TimeUnit timeUnit) {
            return connectTimeout(timeUnit.toMillis(connectTimeout));
        }

        public Builder connectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder connectionTimeout(long connectionTimeout, TimeUnit timeUnit) {
            return connectionTimeout(timeUnit.toMillis(connectionTimeout));
        }

        public Builder listener(GaleraClientListener galeraClientListener) {
            this.listener = galeraClientListener;
            return this;
        }

        public GaleraClient build() {
            ClientSettings clientSettings = new ClientSettings(seeds(), retriesToGetConnection, (listener != null) ? listener : new GaleraClientLoggingListener());
            DiscoverSettings discoverSettings = new DiscoverSettings(discoverPeriod, ignoreDonor);
            GaleraDB galeraDB = new GaleraDB(database, user, password);
            PoolSettings poolSettings = new PoolSettings(maxConnectionsPerHost, connectTimeout, connectionTimeout, readTimeout, idleTimeout);

            return new GaleraClient(clientSettings, discoverSettings, galeraDB, poolSettings);
        }

        private ArrayList<String> seeds() {
            return new ArrayList<String>(Arrays.asList(seeds.split(",")));
        }

        public Builder discoverPeriod(long discoverPeriod) {
            this.discoverPeriod = discoverPeriod;
            return this;
        }

        public Builder discoverPeriod(long discoverPeriod, TimeUnit timeUnit) {
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

        public Builder idleTimeout(long idleTimeout, TimeUnit timeUnit) {
            return idleTimeout(timeUnit.toMillis(idleTimeout));
        }

        public Builder ignoreDonor(boolean ignore) {
            this.ignoreDonor = ignore;
            return this;
        }

        public Builder retriesToGetConnection(int retriesToGetConnection) {
            this.retriesToGetConnection = retriesToGetConnection;
            return this;
        }

        public Builder readTimeout(long timeout, TimeUnit timeUnit) {
            return readTimeout(timeUnit.toMillis(timeout));
        }
    }
}
