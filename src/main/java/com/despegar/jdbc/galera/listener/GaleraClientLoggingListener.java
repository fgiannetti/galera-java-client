package com.despegar.jdbc.galera.listener;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GaleraClientLoggingListener implements GaleraClientListener {
    private static final Logger LOG = LoggerFactory.getLogger(GaleraClientLoggingListener.class);

    @Override
    public void onActivatingNode(String node) {
        LOG.info("Activating galera node: {}", node);
    }

    @Override
    public void onMarkingNodeAsDown(String node, String cause) {
        LOG.info("Marking down galera node: {} because of {}", node, cause);
    }

    @Override
    public void onRemovingNode(String node) {
        LOG.info("Removing galera node: {}", node);
    }

    /**
     * @param poolName              Pool name (without port)
     * @param waitPercentile95      It shows how long requesting threads to getConnection() are waiting for a connection (or timeout exception) from the pool.
     * @param usagePercentile95     It shows how long each connection is used before being returned to the pool. This is the "out of pool" or "in-use" time.
     * @param totalConnections      This value indicates the total number of connections in the pool.
     * @param idleConnections       This value indicates the number of idle connections in the pool.
     * @param activeConnections     This value indicates the number of the number of active (in - use) connections in the pool.
     * @param waitingForConnections This value indicates the number of threads awaiting connections from the pool.
     */
    public void onDiscoveryPoolMetrics(String poolName, Optional<Double> waitPercentile95, Optional<Double> usagePercentile95,
                                       Optional<Integer> totalConnections, Optional<Integer> idleConnections, Optional<Integer> activeConnections,
                                       Optional<Integer> waitingForConnections) {
        LOG.info(
                "Metrics for pool '{}' ---> TimeWaitingForConnection (p95): {}, Usage time (p95): {}, Total connections: {}, Idle connections: {}, " +
                        "Active connections: {}, Pending connections: {}", poolName, waitPercentile95.orNull(), usagePercentile95.orNull(),
                totalConnections.orNull(), idleConnections.orNull(), activeConnections.orNull(), waitingForConnections.orNull()
        );
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).toString();
    }
}
