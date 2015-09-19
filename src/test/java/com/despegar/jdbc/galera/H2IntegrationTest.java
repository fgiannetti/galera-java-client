package com.despegar.jdbc.galera;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class H2IntegrationTest {
    private GaleraClient client;

    @Before
    public void initialize() {
        client = new GaleraClient.Builder()
                .testMode(true)
                .jdbcUrlPrefix("jdbc:h2:")
                .seeds("mem")
                .jdbcUrlSeparator(":")
                .database("test;MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE")
                .user("sa").password("")
                .connectTimeout(500)
                .connectionTimeout(1000)
                .readTimeout(1000)
                .maxConnectionsPerHost(3)
                .minConnectionsIdlePerHost(1)
                .idleTimeout(30000)
                .ignoreDonor(true)
                .retriesToGetConnection(5)
                .build();
    }

    @After
    public void shutdown() {
        if (client != null) {
            client.shutdown();
        }
    }

    @Test
    public void test() throws SQLException {
        executeUpdate("CREATE TABLE theTable (id INTEGER);");
        executeUpdate("insert into theTable values(999);");
        int result = executeQuery("select id from theTable;");
        Assert.assertEquals(999, result);

    }

    public void executeUpdate(String stmt) throws SQLException {
        Connection conn = null;
        Statement statement = null;

        try {
            conn = client.getConnection();
            statement = conn.createStatement();
            statement.executeUpdate(stmt);
            conn.commit();

        } finally {
            if (statement != null) {
                statement.close();
            }

            if (conn != null) {
                conn.close();
            }
        }
    }

    public Integer executeQuery(String query) throws SQLException {
        Connection conn = null;
        Statement statement = null;
        ResultSet rs = null;

        try {
            conn = client.getConnection();
            statement = conn.createStatement();
            rs = statement.executeQuery(query);
            rs.next();

            return rs.getInt(1);
        } finally {
            if (rs != null) {
                rs.close();
            }

            if (statement != null) {
                statement.close();
            }

            if (conn != null) {
                conn.close();
            }
        }
    }

}
