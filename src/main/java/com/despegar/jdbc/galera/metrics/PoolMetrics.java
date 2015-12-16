package com.despegar.jdbc.galera.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.despegar.jdbc.galera.GaleraNode;
import com.despegar.jdbc.galera.listener.GaleraClientListener;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.SortedMap;

import static com.despegar.jdbc.galera.utils.PoolNameHelper.getFullPoolName;
import static com.despegar.jdbc.galera.utils.PoolNameHelper.nodeNameWithoutPort;

public class PoolMetrics {
    private static final Logger LOG = LoggerFactory.getLogger(PoolMetrics.class);

    public static final String METRIC_NAME_POOL_WAIT = ".pool.Wait";
    public static final String METRIC_NAME_POOL_USAGE = ".pool.Usage";
    public static final String METRIC_NAME_TOTAL_CONN = ".pool.TotalConnections";
    public static final String METRIC_NAME_IDLE_CONN = ".pool.IdleConnections";
    public static final String METRIC_NAME_ACTIVE_CONN = ".pool.ActiveConnections";
    public static final String METRIC_NAME_PENDING_CONN = ".pool.PendingConnections";

    public static void reportMetrics(MetricRegistry metricRegistry, Map<String, GaleraNode> nodes, GaleraClientListener listener, Optional<String> poolName) {
        if (metricRegistry == null || nodes == null) {
            return;
        }

        for (String nodeName : nodes.keySet()) {
            final String poolFullName = getFullPoolName(poolName, nodeName);

            HikariMetrics hikariMetrics = HikariMetrics.newBuilder()
                    .waitPercentile95(getTimerPercentile95(metricRegistry, poolFullName + METRIC_NAME_POOL_WAIT))
                    .usagePercentile95(getHistogramPercentile95(metricRegistry, poolFullName + METRIC_NAME_POOL_USAGE))
                    .totalConnections(getGaugeValue(metricRegistry, poolFullName + METRIC_NAME_TOTAL_CONN))
                    .idleConnections(getGaugeValue(metricRegistry, poolFullName + METRIC_NAME_IDLE_CONN))
                    .activeConnections(getGaugeValue(metricRegistry, poolFullName + METRIC_NAME_ACTIVE_CONN))
                    .waitingForConnections(getGaugeValue(metricRegistry, poolFullName + METRIC_NAME_PENDING_CONN)).build();

            Optional<Integer> threadsConnected = getThreadsConnected(nodes.get(nodeName));

            listener.onDiscoveryPoolMetrics(nodeNameWithoutPort(nodeName), poolFullName, hikariMetrics, threadsConnected);
        }
    }

    private static Optional<Integer> getThreadsConnected(GaleraNode galeraNode) {
        Optional<Integer> threadsConnected = Optional.absent();
        try {
            threadsConnected = Optional.fromNullable(galeraNode.status().threadsConnectedCount());
        } catch (Exception e) {
            LOG.warn("Error getting threadsConnected metric", e);
        }
        return threadsConnected;
    }

    private static Optional<Integer> getGaugeValue(MetricRegistry metricRegistry, String metricName) {
        SortedMap<String, Gauge> gauges = metricRegistry.getGauges();

        if (gauges != null && gauges.containsKey(metricName)) {
            return Optional.of((Integer) gauges.get(metricName).getValue());
        }

        return Optional.absent();
    }

    private static Optional<Double> getTimerPercentile95(MetricRegistry metricRegistry, String metricName) {
        SortedMap<String, Timer> timers = metricRegistry.getTimers();

        if (timers != null && timers.containsKey(metricName)) {
            return Optional.of(timers.get(metricName).getSnapshot().get95thPercentile());
        }

        return Optional.absent();
    }

    private static Optional<Double> getHistogramPercentile95(MetricRegistry metricRegistry, String metricName) {
        SortedMap<String, Histogram> histograms = metricRegistry.getHistograms();

        if (histograms != null && histograms.containsKey(metricName)) {
            return Optional.of(histograms.get(metricName).getSnapshot().get95thPercentile());
        }

        return Optional.absent();
    }

}
