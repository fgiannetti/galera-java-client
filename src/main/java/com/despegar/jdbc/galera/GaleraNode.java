package com.despegar.jdbc.galera;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class GaleraNode {
    private final String node;
    private final GaleraDB galeraDB;
    private final PoolSettings poolSettings;
    private final HikariDataSource statusDataSource;
    private volatile HikariDataSource dataSource;
    private volatile GaleraStatus status;

    public GaleraNode(String node, GaleraDB galeraDB, PoolSettings poolSettings) {
        this.node = node;
        this.galeraDB = galeraDB;
        this.poolSettings = poolSettings;

        HikariConfig hikariConfig = newHikariConfig(node, galeraDB, poolSettings);
        hikariConfig.setIdleTimeout(30000);
        statusDataSource = new HikariDataSource(hikariConfig);
    }

    private HikariConfig newHikariConfig(String node, GaleraDB galeraDB, PoolSettings poolSettings) {
        HikariConfig config = new HikariConfig();
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
        return config;
    }

    public void refreshStatus() throws Exception {
        Connection connection = statusDataSource.getConnection();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement("SHOW STATUS LIKE 'wsrep_%'");
            ResultSet resultSet = preparedStatement.executeQuery();
            Map<String, String> statusMap = new HashMap<String, String>();
            while (resultSet.next()) {
                String statusKey = resultSet.getString(1);
                String statusValue = resultSet.getString(2);
                statusMap.put(statusKey, statusValue);
            }
            status = new GaleraStatus(statusMap);
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

    public void onActivate() {
        dataSource = new HikariDataSource(newHikariConfig(node, galeraDB, poolSettings));
    }

    public void onDown() {
        if (dataSource != null) {
            dataSource.shutdown();
            dataSource = null;
        }
    }
}
