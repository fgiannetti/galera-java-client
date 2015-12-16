package com.despegar.jdbc.galera.listener;

import com.despegar.jdbc.galera.metrics.HikariMetrics;
import com.google.common.base.Optional;

public interface GaleraClientListener {

    void onActivatingNode(String node);

    void onMarkingNodeAsDown(String node, String cause);

    void onRemovingNode(String node);

    /**
     * @param poolName         Pool name
     * @param hikariMetrics    Internal counter and metrics from hikari cp
     * @param threadsConnected Connections open to the underlying database.
     */
    void onDiscoveryPoolMetrics(String poolName, HikariMetrics hikariMetrics, Optional<Integer> threadsConnected);

}
