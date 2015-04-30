package com.despegar.jdbc.galera;

public class PoolSettings {
    public final int maxConnectionsPerHost;
    public final long connectTimeout;
    public final long connectionTimeout;
    public final long readTimeout;
    public final long idleTimeout;

    public PoolSettings(int maxConnectionsPerHost, long connectTimeout, long connectionTimeout, long readTimeout, long idleTimeout) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
        this.connectTimeout = connectTimeout;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.idleTimeout = idleTimeout;
    }
}
