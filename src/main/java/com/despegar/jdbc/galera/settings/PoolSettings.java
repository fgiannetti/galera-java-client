package com.despegar.jdbc.galera.settings;

import com.despegar.jdbc.galera.consistency.ConsistencyLevel;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class PoolSettings {
    public final Optional<String> poolName;
    public final int maxConnectionsPerHost;
    public final int minConnectionsIdlePerHost;
    public final long connectTimeout;
    public final long connectionTimeout;
    public final long readTimeout;
    public final long idleTimeout;
    public final boolean autocommit;
    public final boolean readOnly;
    public final String isolationLevel;
    public final ConsistencyLevel consistencyLevel;
    public final boolean metricsEnabled;
    public final long leakDetectionThreshold;

    private PoolSettings(Builder builder) {
        Preconditions.checkArgument(builder.minConnectionsIdlePerHost >= 1, "Min connections per host must be greater or equal than 1. It was: %s",
                                    builder.minConnectionsIdlePerHost);

        maxConnectionsPerHost = (builder.maxConnectionsPerHost > 0) ? builder.maxConnectionsPerHost : builder.minConnectionsIdlePerHost;
        minConnectionsIdlePerHost = builder.minConnectionsIdlePerHost;
        connectTimeout = builder.connectTimeout;
        connectionTimeout = builder.connectionTimeout;
        readTimeout = builder.readTimeout;
        idleTimeout = builder.idleTimeout;
        autocommit = builder.autocommit;
        readOnly = builder.readOnly;
        isolationLevel = builder.isolationLevel;
        consistencyLevel = builder.consistencyLevel;
        metricsEnabled = builder.metricsEnabled;
        poolName = builder.poolName;
        leakDetectionThreshold = builder.leakDetectionThreshold;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("poolName", poolName)
                .add("maxConnectionsPerHost", maxConnectionsPerHost)
                .add("minConnectionsIdlePerHost", minConnectionsIdlePerHost)
                .add("connectTimeout", connectTimeout)
                .add("connectionTimeout", connectionTimeout)
                .add("readTimeout", readTimeout)
                .add("idleTimeout", idleTimeout)
                .add("autocommit", autocommit)
                .add("readOnly", readOnly)
                .add("isolationLevel", isolationLevel)
                .add("consistencyLevel", consistencyLevel)
                .add("leakDetectionThreshold", leakDetectionThreshold)
                .toString();
    }

    public static final class Builder {
        private int maxConnectionsPerHost;
        private Optional<String> poolName;
        private int minConnectionsIdlePerHost;
        private long connectTimeout;
        private long connectionTimeout;
        private long readTimeout;
        private long idleTimeout;
        private boolean autocommit;
        private boolean readOnly;
        private String isolationLevel;
        private ConsistencyLevel consistencyLevel;
        private boolean metricsEnabled;
        private long leakDetectionThreshold;

        private Builder() {
        }

        public Builder maxConnectionsPerHost(int maxConnectionsPerHost) {
            this.maxConnectionsPerHost = maxConnectionsPerHost;
            return this;
        }

        public Builder minConnectionsIdlePerHost(int minConnectionsIdlePerHost) {
            this.minConnectionsIdlePerHost = minConnectionsIdlePerHost;
            return this;
        }

        public Builder connectTimeout(long connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder connectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder readTimeout(long readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder idleTimeout(long idleTimeout) {
            this.idleTimeout = idleTimeout;
            return this;
        }

        public Builder autocommit(boolean autocommit) {
            this.autocommit = autocommit;
            return this;
        }

        public Builder readOnly() {
            return this.readOnly(true);
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

        public Builder metricsEnabled(boolean enableMetrics) {
            this.metricsEnabled = enableMetrics;
            return this;
        }

        public Builder poolName(Optional<String> poolName) {
            this.poolName = poolName;
            return this;
        }

        public Builder leakDetectionThreshold(long leakDetectionThreshold) {
            this.leakDetectionThreshold = leakDetectionThreshold;
            return this;
        }

        public PoolSettings build() {
            return new PoolSettings(this);
        }
    }
}
