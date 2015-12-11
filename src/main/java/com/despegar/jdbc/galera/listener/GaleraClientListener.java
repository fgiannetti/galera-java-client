package com.despegar.jdbc.galera.listener;

import com.google.common.base.Optional;

public interface GaleraClientListener {

    void onActivatingNode(String node);

    void onMarkingNodeAsDown(String node, String cause);

    void onRemovingNode(String node);

    void onDiscoveryPoolMetrics(String poolName, Optional<Double> waitPercentile95, Optional<Double> usagePercentile95, Optional<Integer> totalConnections,
                                Optional<Integer> idleConnections, Optional<Integer> activeConnections, Optional<Integer> pendingConnections);

}
