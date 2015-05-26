package com.despegar.jdbc.galera;

import com.despegar.jdbc.galera.settings.PoolSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class GaleraNode {
    private static final Logger LOG = LoggerFactory.getLogger(GaleraNode.class);

    public final String node;
    private final GaleraDB galeraDB;
    private final PoolSettings poolSettings;
    private final HikariDataSource statusDataSource;
    private volatile HikariDataSource dataSource;
    private volatile GaleraStatus status;

    public GaleraNode(String node, GaleraDB galeraDB, PoolSettings poolSettings) {
        LOG.info("Creating galera node " + node);
        this.node = node;
        this.galeraDB = galeraDB;
        this.poolSettings = poolSettings;

        HikariConfig hikariConfig = newHikariConfig("hikari-pool-status-" + node, node, galeraDB, poolSettings);
        statusDataSource = new HikariDataSource(hikariConfig);
    }

    private HikariConfig newHikariConfig(String poolName, String node, GaleraDB galeraDB, PoolSettings poolSettings) {
        HikariConfig config = new HikariConfig();
        config.setPoolName(poolName);
        config.setJdbcUrl("jdbc:mysql://" + node + "/" + galeraDB.database);
        config.setUsername(galeraDB.user);
        config.setPassword(galeraDB.password);
        config.setConnectionTimeout(poolSettings.connectionTimeout);
        config.setMaximumPoolSize(poolSettings.maxConnectionsPerHost);
        config.setInitializationFailFast(false);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("connectTimeout", String.valueOf(poolSettings.connectTimeout));
        config.addDataSourceProperty("socketTimeout", String.valueOf(poolSettings.readTimeout));
        config.setIdleTimeout(poolSettings.idleTimeout);

        return config;
    }

    public void refreshStatus() throws Exception {
        Map<String, String> statusMap = queryStatus("SHOW STATUS LIKE 'wsrep_%'");
        Map<String, String> globalVariablesMap = queryStatus("SHOW GLOBAL VARIABLES WHERE variable_name in ('wsrep_sync_wait', 'wsrep_causal_reads');");
        statusMap.putAll(globalVariablesMap);

        status = new GaleraStatus(statusMap);
    }

    private Map<String, String> queryStatus(String query) throws Exception {
        Connection connection = statusDataSource.getConnection();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();
            Map<String, String> statusMap = new HashMap<String, String>();
            while (resultSet.next()) {
                String statusKey = resultSet.getString(1);
                String statusValue = resultSet.getString(2);
                statusMap.put(statusKey, statusValue);
            }
            return statusMap;
        } finally {
            tryClose(preparedStatement);
            tryClose(connection);
        }
    }

    public GaleraStatus status() throws Exception {
        if (status == null) {
            refreshStatus();
        }
        return status;
    }

    private void tryClose(AutoCloseable connection) throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    public void shutdown() {
        onDown();
        if (statusDataSource != null) statusDataSource.shutdown();
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public Connection getConnection(ConsistencyLevel consistencyLevel) throws Exception {
        return GaleraProxyConnection.create(dataSource.getConnection(), consistencyLevel, status);
    }

    public void onActivate() {
        dataSource = new HikariDataSource(newHikariConfig("hikari-pool-" + node, node, galeraDB, poolSettings));
    }

    public void onDown() {
        if (dataSource != null) {
            LOG.info("Closing all connections on node " + node);
            dataSource.shutdown();
            dataSource = null;
        }
    }
}
