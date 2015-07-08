package com.despegar.jdbc.galera.main;

import com.despegar.jdbc.galera.GaleraClient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by aholman on 07/07/15.
 */
public class MainGalera {
    private static final String query = "select id from audit limit 1";

    public static void main(String[] args) throws Exception {
        GaleraClient client = null;
        try {
            client = new GaleraClient.Builder().seeds("watson-maria-ic-02.servers.despegar.it")
                    .database("watsonic").user("watsonRoot").password("w4ts0n-r00t").discoverPeriod(2000)
                    .connectTimeout(500).connectionTimeout(1000).readTimeout(1000)
                    .maxConnectionsPerHost(1).idleTimeout(30000).ignoreDonor(true).retriesToGetConnection(5).build();

            for (int i = 1; i < 20000; i++) {
                System.out.println("Connection #" + i);
                executeQuery(client);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (client != null) {
                client.shutdown();
            }

        }
    }

    private static void executeQuery(GaleraClient client) throws Exception {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        try {
            connection = client.getConnection();

            if (connection == null) {
                System.out.println("Could not get connection");
                return;
            }

            statement = connection.prepareStatement(query);
            if (statement == null) {
                System.out.println("Could not prepare statement");
            }

            rs = statement.executeQuery();

            while (rs.next()) {
                System.out.println(rs.getLong(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (rs != null) { rs.close(); }
                if (statement != null) { statement.close(); }
                if (connection != null) { connection.close(); }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}
