package com.despegar.jdbc.galera;

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
    private List<String> activeNodes = new CopyOnWriteArrayList<String>();
    private List<String> downedNodes = new CopyOnWriteArrayList<String>();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private GaleraDB galeraDB;
    private PoolSettings poolSettings;
    private Runnable discoverRunnable = new Runnable() {
        @Override
        public void run() {
            discovery();
        }
    };

    private GaleraClient(Collection<String> seeds, long discoverPeriod, GaleraDB galeraDB, PoolSettings poolSettings) {
        this.galeraDB = galeraDB;
        this.poolSettings = poolSettings;
        registerNodes(seeds);
        startDiscovery(discoverPeriod);
    }

    private void discovery() {
        try {
            LOG.debug("Discovering Galera cluster...");
            discoverActiveNodes();
            testDownedNodes();
            LOG.debug("Active nodes: {},  Downed nodes: {}", activeNodes, downedNodes);
        } catch (Throwable reason) {
        }
    }

    private void testDownedNodes() {
        for (String downedNode : downedNodes) {
            try {
                discover(downedNode);
                if (nodes.containsKey(downedNode)) {
                    LOG.debug("Will activate a previous downed node: {}", downedNode);
                    activate(downedNode);
                }
            } catch (Exception e) {
                down(downedNode, e.getMessage());
            }
        }
    }

    private void activate(String downedNode) {
        nodes.get(downedNode).onActivate();
        if (!activeNodes.contains(downedNode)) {
            LOG.debug("Activating node:  {}", downedNode);
            activeNodes.add(downedNode);
        }
        downedNodes.remove(downedNode);
    }

    private void down(String node, String cause) {
        LOG.debug("Marking node {} as down due to {}", node, cause);
        activeNodes.remove(node);
        if (!downedNodes.contains(node)) {
            downedNodes.add(node);
        }
        closeConnections(node);
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
    }

    private void closeConnections(String node) {
        GaleraNode galeraNode = nodes.get(node);
        if (galeraNode != null) {
            galeraNode.onDown();
        }
    }

    private void shutdownGaleraNode(String node) {
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
            down(node, "non Primary");
            return;
        }
        if (!status.isSynced() && !status.isDonor()) {
            down(node, "state not ready: " + status.state());
            return;
        }
        Collection<String> discoveredNodes = status.getClusterNodes();
        for (String discoveredNode : discoveredNodes) {
            if (isNew(discoveredNode)) {
                registerNode(discoveredNode);
            }
        }
        if (!discoveredNodes.contains(node)) {
            removeNode(node);
        } else {
            if (!isActive(node)) {
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
            registerNode(seed);
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
        return nextActiveGaleraNode(1).getConnection();
    }

    private GaleraNode nextActiveGaleraNode(int retry) {
        if (retry <= 3) {
            try {
                GaleraNode galeraNode = nodes.get(activeNodes.get(nextNodeIndex()));
                return galeraNode != null ? galeraNode : nextActiveGaleraNode(++retry);
            } catch (IndexOutOfBoundsException outOfBoundsException) {
                return nextActiveGaleraNode(++retry);
            }
        } else {
            throw new NoHostAvailableException();
        }
    }

    private int nextNodeIndex() {
        int activeNodesCount = activeNodes.size();
        if (activeNodesCount == 0) {
            throw new NoHostAvailableException();
        }
        return nextNodeIndex.incrementAndGet() % activeNodesCount;
    }

    static class Builder {

        private String database;
        private String user;
        private String password;
        private String seeds;
        private int maxConnectionsPerHost;
        private long discoverPeriod;
        private long connectTimeout;
        private long connectionTimeout;
        private long readTimeout;

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

        public GaleraClient build() {
            return new GaleraClient(seeds(), discoverPeriod, new GaleraDB(database, user, password), new PoolSettings(maxConnectionsPerHost, connectTimeout, connectionTimeout, readTimeout));
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

        public Builder readTimeout(long timeout, TimeUnit timeUnit) {
            return readTimeout(timeUnit.toMillis(timeout));
        }
    }
}
