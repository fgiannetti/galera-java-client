package com.despegar.jdbc.galera;

import java.sql.Connection;

public class GaleraDataSourceTest {


    public static void main(String[] args) throws Exception {
        new GaleraDataSourceTest().testGaleraDataSource();
    }

    public void testGaleraDataSource() throws Exception {
            GaleraDataSource dataSource = new GaleraDataSource.Builder().seeds("maria-1.mg10.dev.docker")
                    .database("").user("despegar").password("despegar").discoverPeriod(2000)
                    .connectTimeout(500).connectionTimeout(1000).readTimeout(1000)
                    .maxConnectionsPerHost(3).minConnectionsIdlePerHost(1).idleTimeout(30000)
                    .ignoreDonor(true).retriesToGetConnection(5).build();

        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            //connection = dataSource.getConnection(ConsistencyLevel.CAUSAL_READS_ON);
            connection = dataSource.getConnection(ConsistencyLevel.SYNC_READ_UPDATE_DELETE, true);
        } finally {
            if (connection != null) {
                connection.close();
            }

            dataSource.shutdown();
        }
    }

}
