package com.despegar.jdbc.galera;

import com.despegar.jdbc.galera.consistency.ConsistencyLevel;
import com.despegar.jdbc.galera.listener.GaleraClientListener;
import com.despegar.jdbc.galera.policies.ElectionNodePolicy;
import com.google.common.base.Optional;

public class GaleraClientFactory {
    private boolean testMode;
    private String database;
    private String user;
    private String password;
    private String jdbcUrlPrefix;
    private String jdbcUrlSeparator;
    private String seeds;
    private int maxConnectionsPerHost;
    private int minConnectionsIdlePerHost;
    private long discoverPeriod;
    private long connectTimeout;
    private long connectionTimeout;
    private long readTimeout;
    private long idleTimeout;
    private boolean ignoreDonor;
    private int retriesToGetConnection;
    private boolean autocommit;
    private boolean readOnly;
    private String isolationLevel;
    private ConsistencyLevel consistencyLevel;
    private GaleraClientListener listener;
    private ElectionNodePolicy nodeSelectionPolicy;
    private boolean metricsEnabled;
    private Optional<String> poolName = Optional.absent();

    public GaleraClient getInstance() {
        return new GaleraClient.Builder().jdbcUrlPrefix(jdbcUrlPrefix).jdbcUrlSeparator(jdbcUrlSeparator).database(database).user(user).password(password)
                .seeds(seeds).poolName(poolName).maxConnectionsPerHost(maxConnectionsPerHost).minConnectionsIdlePerHost(minConnectionsIdlePerHost)
                .discoverPeriod(discoverPeriod)
                .connectionTimeout(connectionTimeout).connectTimeout(connectTimeout).readTimeout(readTimeout).idleTimeout(idleTimeout).ignoreDonor(ignoreDonor)
                .retriesToGetConnection(retriesToGetConnection).autocommit(autocommit).readOnly(readOnly).isolationLevel(isolationLevel)
                .consistencyLevel(consistencyLevel).listener(listener).nodeSelectionPolicy(nodeSelectionPolicy).testMode(testMode).metricsEnabled(
                        metricsEnabled).build();
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setSeeds(String seeds) {
        this.seeds = seeds;
    }

    public void setMaxConnectionsPerHost(int maxConnectionsPerHost) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
    }

    public void setMinConnectionsIdlePerHost(int minConnectionsIdlePerHost) {
        this.minConnectionsIdlePerHost = minConnectionsIdlePerHost;
    }

    public void setDiscoverPeriod(long discoverPeriod) {
        this.discoverPeriod = discoverPeriod;
    }

    public void setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public void setIgnoreDonor(boolean ignoreDonor) {
        this.ignoreDonor = ignoreDonor;
    }

    public void setRetriesToGetConnection(int retriesToGetConnection) {
        this.retriesToGetConnection = retriesToGetConnection;
    }

    public void setAutocommit(boolean autocommit) {
        this.autocommit = autocommit;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void setIsolationLevel(String isolationLevel) {
        this.isolationLevel = isolationLevel;
    }

    public void setConsistencyLevel(ConsistencyLevel consistencyLevel) {
        this.consistencyLevel = consistencyLevel;
    }

    public void setListener(GaleraClientListener listener) {
        this.listener = listener;
    }

    public void setNodeSelectionPolicy(ElectionNodePolicy nodeSelectionPolicy) {
        this.nodeSelectionPolicy = nodeSelectionPolicy;
    }

    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    public void setJdbcUrlPrefix(String jdbcUrlPrefix) {
        this.jdbcUrlPrefix = jdbcUrlPrefix;
    }

    public void setJdbcUrlSeparator(String jdbcUrlSeparator) {
        this.jdbcUrlSeparator = jdbcUrlSeparator;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }

    public void setPoolName(String poolName) {
        this.poolName = Optional.fromNullable(poolName);
    }

}

