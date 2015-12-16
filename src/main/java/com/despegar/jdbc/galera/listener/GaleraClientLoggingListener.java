package com.despegar.jdbc.galera.listener;

import com.despegar.jdbc.galera.metrics.HikariMetrics;
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

    public void onDiscoveryPoolMetrics(String poolName, HikariMetrics hikariMetrics, Optional<Integer> threadsConnected) {
        LOG.info(
                "Metrics for pool '{}' ---> TimeWaitingForConnection (p95): {}, Usage time (p95): {}, Total connections: {}, Idle connections: {}, " +
                        "Active connections: {}, Pending connections: {}, Threads connected: {}",
                poolName, hikariMetrics.getWaitPercentile95().orNull(), hikariMetrics.getUsagePercentile95().orNull(),
                hikariMetrics.getTotalConnections().orNull(), hikariMetrics.getIdleConnections().orNull(), hikariMetrics.getActiveConnections().orNull(),
                hikariMetrics.getWaitingForConnections().orNull(), threadsConnected.orNull()
        );
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).toString();
    }
}
