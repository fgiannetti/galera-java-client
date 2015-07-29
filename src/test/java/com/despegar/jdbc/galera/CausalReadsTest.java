package com.despegar.jdbc.galera;

import com.despegar.jdbc.galera.listener.GaleraDataSourceLoggingListener;
import com.despegar.jdbc.galera.settings.DataSourceSettings;
import com.despegar.jdbc.galera.settings.DiscoverSettings;
import com.despegar.jdbc.galera.settings.PoolSettings;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

@Ignore(value = "Ignoring because of bad configuration: host, database, user, ..")
public class CausalReadsTest {
    private ArrayList<String> seeds = new ArrayList<String>(Arrays.asList("<host:port>"));
    private DataSourceSettings dataSourceSettings = new DataSourceSettings(seeds, 5, new GaleraDataSourceLoggingListener(), null, null);
    private DiscoverSettings discoverSettings = new DiscoverSettings(2000, false);
    private GaleraDB galeraDB = new GaleraDB("<database>", "<user>", "<pwd>");
    private PoolSettings poolSettings = new PoolSettings(2, 1, 5000, 5000, 10000, 30000, true);

    @Test
    public void causalReadsOn() throws Exception {
        int totalRetries = test(true);
        Assert.assertEquals(0, totalRetries);
    }

    @Test
    public void causalReadsOff() throws Exception {
        int totalRetries = test(false);
        Assert.assertTrue(totalRetries > 0);
    }

    private int test(Boolean causalReads) throws Exception {
        System.out.println("Starting test");

        GaleraDataSource writerDatasource = new GaleraDataSourceTest("<hostA:port>", dataSourceSettings, discoverSettings, galeraDB, poolSettings);
        GaleraDataSource readerDataSource = new GaleraDataSourceTest("<hostB:port>", dataSourceSettings, discoverSettings, galeraDB, poolSettings);

        int rounds = 1000;

        Connection writerConnection = writerDatasource.getConnection(causalReads ? ConsistencyLevel.CAUSAL_READS_ON : ConsistencyLevel.CAUSAL_READS_OFF, false);
        Connection readerConnection = readerDataSource.getConnection(causalReads ? ConsistencyLevel.CAUSAL_READS_ON : ConsistencyLevel.CAUSAL_READS_OFF, false);

        int totalRetries = 0;

        PreparedStatement writeStatement = null;
        PreparedStatement readStatement = null;
        for (int i = 0; i < rounds; i++) {

            writeStatement = writerConnection.prepareStatement("UPDATE Test set k=? where id=1");
            readStatement = readerConnection.prepareStatement("SELECT k from Test where id=1");

            String uuid = UUID.randomUUID().toString();

            writeStatement.setString(1, uuid);
            writeStatement.execute();

            int retries = 0;
            boolean endLoop = false;

            while (!endLoop) {
                ResultSet resultSet = readStatement.executeQuery();
                String result = resultSet.next() ? resultSet.getString(1) : "";
                System.out.println("result " + result + " uuid " + uuid);
                if (result.equals(uuid)) { endLoop = true; } else { retries += 1; }

                    resultSet.close();

            }

            totalRetries += retries;
            System.out.println(
                    "--------------------------------------------------round " + i + " number of retries: " + retries + "--------------------------------");
        }

        System.out.println("-------------------------------------------------- Total Retries: " + totalRetries + "--------------------------------");

        if (writeStatement != null) { writeStatement.close(); }
        if (readStatement != null) { readStatement.close(); }
        writerConnection.close();
        readerConnection.close();

        writerDatasource.shutdown();
        readerDataSource.shutdown();

        return totalRetries;
    }

    private class GaleraDataSourceTest extends GaleraDataSource {
        private String node;

        @Override
        protected GaleraNode selectNode(boolean holdsMaster) {
            return nodes.get(node);
        }

        protected GaleraDataSourceTest(DataSourceSettings dataSourceSettings, DiscoverSettings discoverSettings, GaleraDB galeraDB, PoolSettings poolSettings) {
            super(dataSourceSettings, discoverSettings, galeraDB, poolSettings);
            throw new RuntimeException("Don't use this constructor");
        }

        public GaleraDataSourceTest(String nodeName, DataSourceSettings dataSourceSettings, DiscoverSettings discoverSettings, GaleraDB galeraDB,
                                    PoolSettings poolSettings) {
            super(dataSourceSettings, discoverSettings, galeraDB, poolSettings);
            this.node = nodeName;
        }
    }

}
