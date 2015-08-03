package com.despegar.jdbc.galera.settings;

import com.despegar.jdbc.galera.consistency.ConsistencyLevel;

public class PoolSettings {
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

    public PoolSettings(int maxConnectionsPerHost, int minConnectionsIdlePerHost, long connectTimeout, long connectionTimeout, long readTimeout,
                        long idleTimeout, boolean autocommit, boolean readOnly, String isolationLevel, ConsistencyLevel consistencyLevel) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
        this.minConnectionsIdlePerHost = (minConnectionsIdlePerHost <= 0) ? maxConnectionsPerHost : minConnectionsIdlePerHost;
        this.connectTimeout = connectTimeout;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.idleTimeout = idleTimeout;
        this.autocommit = autocommit;
        this.readOnly = readOnly;
        this.isolationLevel = isolationLevel;
        this.consistencyLevel = consistencyLevel;
    }
}
