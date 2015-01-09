package com.despegar.jdbc.galera;

public class PoolSettings {
    public final int maxConnectionsPerHost;
    public final long connectTimeout;
    public final long connectionTimeout;
    public final long readTimeout;

    public PoolSettings(int maxConnectionsPerHost, long connectTimeout, long connectionTimeout, long readTimeout) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
        this.connectTimeout = connectTimeout;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
    }
}
