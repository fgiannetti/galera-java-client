package com.despegar.jdbc.galera.utils;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.despegar.jdbc.galera.listener.GaleraClientListener;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.SortedMap;

import static com.despegar.jdbc.galera.utils.PoolNameHelper.CLIENT_POOL_PREFIX_NAME;
import static com.despegar.jdbc.galera.utils.PoolNameHelper.nodeNameWithoutPort;

public class PoolMetrics {
    private static final Logger LOG = LoggerFactory.getLogger(PoolMetrics.class);

    public static final String METRIC_NAME_POOL_WAIT = ".pool.Wait";
    public static final String METRIC_NAME_POOL_USAGE = ".pool.Usage";
    public static final String METRIC_NAME_TOTAL_CONN = ".pool.TotalConnections";
    public static final String METRIC_NAME_IDLE_CONN = ".pool.IdleConnections";
    public static final String METRIC_NAME_ACTIVE_CONN = ".pool.ActiveConnections";
    public static final String METRIC_NAME_PENDING_CONN = ".pool.PendingConnections";

    public static void reportMetrics(MetricRegistry metricRegistry, Set<String> nodes, GaleraClientListener listener) {
        if (metricRegistry == null || nodes == null) {
            return;
        }

        for (String nodeName : nodes) {
            final String poolName = CLIENT_POOL_PREFIX_NAME + nodeNameWithoutPort(nodeName);

            Optional<Double> waitPercentile95 = getTimerPercentile95(metricRegistry, poolName + METRIC_NAME_POOL_WAIT);
            Optional<Double> usagePercentile95 = getHistogramPercentile95(metricRegistry, poolName + METRIC_NAME_POOL_USAGE);
            Optional<Integer> totalConnections = getGaugeValue(metricRegistry, poolName + METRIC_NAME_TOTAL_CONN);
            Optional<Integer> idleConnections = getGaugeValue(metricRegistry, poolName + METRIC_NAME_IDLE_CONN);
            Optional<Integer> activeConnections = getGaugeValue(metricRegistry, poolName + METRIC_NAME_ACTIVE_CONN);
            Optional<Integer> waitingForConnections = getGaugeValue(metricRegistry, poolName + METRIC_NAME_PENDING_CONN);

            listener.onDiscoveryPoolMetrics(poolName, waitPercentile95, usagePercentile95, totalConnections, idleConnections, activeConnections,
                                            waitingForConnections);
        }
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
