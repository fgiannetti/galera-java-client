package com.despegar.jdbc.galera;

import com.despegar.jdbc.galera.consistency.ConsistencyLevel;
import com.despegar.jdbc.galera.consistency.ConsistencyLevelSupport;
import com.despegar.jdbc.galera.consistency.GaleraProxyConnection;
import com.despegar.jdbc.galera.settings.PoolSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class GaleraNode {
    private static final Logger LOG = LoggerFactory.getLogger(GaleraNode.class);

    private static final String QUERY_STATUS = "SHOW STATUS LIKE 'wsrep_%'";
    private static final String QUERY_GLOBAL_VARIABLES = "SHOW GLOBAL VARIABLES WHERE variable_name in ('wsrep_sync_wait', 'wsrep_causal_reads');";

    public final String node;
    private final GaleraDB galeraDB;
    private final PoolSettings poolSettings;
    private final HikariDataSource statusDataSource;
    private volatile HikariDataSource dataSource;
    private volatile GaleraStatus status;

    public GaleraNode(String node, GaleraDB galeraDB, PoolSettings poolSettings, PoolSettings internalPoolSettings) {
        LOG.info("Creating galera node {}", node);
        this.node = node;
        this.galeraDB = galeraDB;
        this.poolSettings = poolSettings;

        HikariConfig hikariConfig = newHikariConfig("hikari-pool-status-" + node, node, galeraDB, internalPoolSettings);
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
        config.setMinimumIdle(poolSettings.minConnectionsIdlePerHost);
        config.setIdleTimeout(poolSettings.idleTimeout);
        config.setAutoCommit(poolSettings.autocommit);
        config.setReadOnly(poolSettings.readOnly);
        config.setTransactionIsolation(poolSettings.isolationLevel);
        config.setInitializationFailFast(false);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        config.addDataSourceProperty("connectTimeout", String.valueOf(poolSettings.connectTimeout));
        config.addDataSourceProperty("socketTimeout", String.valueOf(poolSettings.readTimeout));

        return config;
    }

    public void refreshStatus() throws Exception {
        Connection connection = statusDataSource.getConnection();

        try {
            Map<String, String> statusMap = queryStatus(connection, QUERY_STATUS);
            Map<String, String> globalVariablesMap = queryStatus(connection, QUERY_GLOBAL_VARIABLES);
            statusMap.putAll(globalVariablesMap);

            status = new GaleraStatus(statusMap);
        } finally {
            tryClose(connection);
        }
    }

    private Map<String, String> queryStatus(Connection connection, String query) throws Exception {
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = connection.prepareStatement(query);
            resultSet = preparedStatement.executeQuery();
            Map<String, String> statusMap = new HashMap<String, String>();
            while (resultSet.next()) {
                String statusKey = resultSet.getString(1);
                String statusValue = resultSet.getString(2);
                statusMap.put(statusKey, statusValue);
            }
            return statusMap;
        } finally {
            tryClose(resultSet);
            tryClose(preparedStatement);
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
        if (statusDataSource != null) { statusDataSource.shutdown(); }
    }

    public Connection getConnection() throws SQLException {
        Connection conn = dataSource.getConnection();

        if (poolSettings.consistencyLevel != null) {
            LOG.debug("Setting connection level to default configured on client: {} ", poolSettings.consistencyLevel.toString());
            ConsistencyLevelSupport.set(conn, poolSettings.consistencyLevel.value, status.supportsSyncWait());
        }

        return conn;
    }

    public Connection getConnection(ConsistencyLevel consistencyLevel) throws SQLException {
        return GaleraProxyConnection.create(dataSource.getConnection(), consistencyLevel, status);
    }

    public void onActivate() {
        dataSource = new HikariDataSource(newHikariConfig("hikari-pool-" + node, node, galeraDB, poolSettings));
    }

    public PrintWriter getLogWriter() throws SQLException {
        return dataSource.getLogWriter();
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        dataSource.setLogWriter(out);
    }

    public void onDown() {
        if (dataSource != null) {
            LOG.info("Closing all connections on node " + node);
            dataSource.shutdown();
            dataSource = null;
        }
    }
}
