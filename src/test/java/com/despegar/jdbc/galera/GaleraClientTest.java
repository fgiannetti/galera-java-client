package com.despegar.jdbc.galera;

import java.sql.Connection;
import java.sql.SQLException;

public class GaleraClientTest {


    public static void main(String[] args) throws Exception {
        new GaleraClientTest().testGaleraClient();
    }

    public void testGaleraClient() throws Exception {
            GaleraClient client = new GaleraClient.Builder().seeds("maria-1.mg10.dev.docker")
                    .database("").user("despegar").password("despegar").discoverPeriod(2000)
                    .connectTimeout(500).connectionTimeout(1000).readTimeout(1000)
                    .maxConnectionsPerHost(1).idleTimeout(30000).ignoreDonor(true).retriesToGetConnection(5).build();

        Connection connection = null;
        try {
            connection = client.getConnection();
            //connection = client.getConnection(ConsistencyLevel.CAUSAL_READS_ON);
            connection = client.getConnection(ConsistencyLevel.SYNC_READ_UPDATE_DELETE, true);
        } finally {
            if (connection != null) {
                connection.close();
            }

            client.shutdown();
        }
    }

}
