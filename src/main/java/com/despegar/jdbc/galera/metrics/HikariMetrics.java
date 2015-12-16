package com.despegar.jdbc.galera.metrics;

import com.google.common.base.Optional;

/**
 * HikariCP exposes the following metrics:
 * 1) waitPercentile95      It shows how long requesting threads to getConnection() are waiting for a connection (or timeout exception) from the pool.
 * 2) usagePercentile95     It shows how long each connection is used before being returned to the pool. This is the "out of pool" or "in-use" time.
 * 3) totalConnections      This value indicates the total number of connections in the pool.
 * 4) idleConnections       This value indicates the number of idle connections in the pool.
 * 5) activeConnections     This value indicates the number of the number of active (in - use) connections in the pool.
 * 6) waitingForConnections This value indicates the number of threads awaiting connections from the pool.
 */
public class HikariMetrics {

    private Optional<Double> waitPercentile95;
    private Optional<Double> usagePercentile95;
    private Optional<Integer> totalConnections;
    private Optional<Integer> idleConnections;
    private Optional<Integer> activeConnections;
    private Optional<Integer> waitingForConnections;

    private HikariMetrics(Builder builder) {
        this.waitPercentile95 = builder.waitPercentile95;
        this.usagePercentile95 = builder.usagePercentile95;
        this.totalConnections = builder.totalConnections;
        this.activeConnections = builder.activeConnections;
        this.idleConnections = builder.idleConnections;
        this.waitingForConnections = builder.waitingForConnections;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public Optional<Double> getWaitPercentile95() {
        return waitPercentile95;
    }

    public Optional<Double> getUsagePercentile95() {
        return usagePercentile95;
    }

    public Optional<Integer> getTotalConnections() {
        return totalConnections;
    }

    public Optional<Integer> getIdleConnections() {
        return idleConnections;
    }

    public Optional<Integer> getActiveConnections() {
        return activeConnections;
    }

    public Optional<Integer> getWaitingForConnections() {
        return waitingForConnections;
    }

    public static final class Builder {
        private Optional<Double> waitPercentile95;
        private Optional<Double> usagePercentile95;
        private Optional<Integer> totalConnections;
        private Optional<Integer> idleConnections;
        private Optional<Integer> activeConnections;
        private Optional<Integer> waitingForConnections;

        private Builder() {
        }

        public Builder waitPercentile95(Optional<Double> waitPercentile95) {
            this.waitPercentile95 = waitPercentile95;
            return this;
        }

        public Builder usagePercentile95(Optional<Double> usagePercentile95) {
            this.usagePercentile95 = usagePercentile95;
            return this;
        }

        public Builder totalConnections(Optional<Integer> totalConnections) {
            this.totalConnections = totalConnections;
            return this;
        }

        public Builder idleConnections(Optional<Integer> idleConnections) {
            this.idleConnections = idleConnections;
            return this;
        }

        public Builder activeConnections(Optional<Integer> activeConnections) {
            this.activeConnections = activeConnections;
            return this;
        }

        public Builder waitingForConnections(Optional<Integer> waitingForConnections) {
            this.waitingForConnections = waitingForConnections;
            return this;
        }

        public HikariMetrics build() {
            return new HikariMetrics(this);
        }
    }

}
