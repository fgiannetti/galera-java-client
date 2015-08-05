package com.despegar.jdbc.galera;

import com.despegar.jdbc.galera.consistency.ConsistencyLevel;
import com.despegar.jdbc.galera.listener.GaleraClientLoggingListener;
import com.despegar.jdbc.galera.policies.ElectionNodePolicy;
import com.despegar.jdbc.galera.policies.RoundRobinPolicy;
import com.despegar.jdbc.galera.settings.ClientSettings;
import com.despegar.jdbc.galera.settings.DiscoverSettings;
import com.despegar.jdbc.galera.settings.PoolSettings;
import org.junit.Assert;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

//@Ignore(value = "Ignoring because of bad configuration: host, database, user, ..")
public class CausalReadsTest {
    private static final String HOST_WRITER = "<hostA:port>";
    private static final String HOST_READER = "<hostB:port>";
    private static final Integer MAX_CONN_PER_HOST = 3;
    private static final Integer MIN_CONN = 1;
    private static final Integer TIMEOUT = 30000;
    private static final boolean AUTOCOMMIT = true;
    private static final boolean READ_ONLY = false;
    private static final String ISOLATION_LEVEL = "TRANSACTION_READ_COMMITTED";

    private ArrayList<String> seeds = new ArrayList<String>(Arrays.asList(HOST_READER));
    private ClientSettings clientSettings = new ClientSettings(seeds, 5, new GaleraClientLoggingListener(), new RoundRobinPolicy());
    private DiscoverSettings discoverSettings = new DiscoverSettings(8000, false);
    private GaleraDB galeraDB = new GaleraDB("<dbName>", "<usr>", "<pwd>");
    private PoolSettings poolSettings = new PoolSettings(MAX_CONN_PER_HOST, MIN_CONN, TIMEOUT, TIMEOUT, TIMEOUT, TIMEOUT, AUTOCOMMIT, READ_ONLY,
                                                         ISOLATION_LEVEL, null);

    @Test
    public void causalReadsOnPerClient() throws Exception {
        PoolSettings poolSettingsWithConsistencyLevel = new PoolSettings(MAX_CONN_PER_HOST, MIN_CONN, TIMEOUT, TIMEOUT, TIMEOUT, TIMEOUT, AUTOCOMMIT, READ_ONLY,
                                                                         ISOLATION_LEVEL, ConsistencyLevel.CAUSAL_READS_ON);

        int totalRetries = test(null, poolSettingsWithConsistencyLevel);
        Assert.assertEquals(0, totalRetries);
    }

    @Test
    public void causalReadsOffPerClient() throws Exception {
        PoolSettings poolSettingsWithConsistencyLevel = new PoolSettings(MAX_CONN_PER_HOST, MIN_CONN, TIMEOUT, TIMEOUT, TIMEOUT, TIMEOUT, AUTOCOMMIT, READ_ONLY,
                                                                         ISOLATION_LEVEL, ConsistencyLevel.CAUSAL_READS_OFF);

        int totalRetries = test(null, poolSettingsWithConsistencyLevel);
        Assert.assertTrue(totalRetries > 0);
    }

    @Test
    public void causalReadsOnPerConnection() throws Exception {
        ConsistencyLevel consistencyLevelPerConnection = ConsistencyLevel.CAUSAL_READS_ON;

        int totalRetries = test(consistencyLevelPerConnection, poolSettings);
        Assert.assertEquals(0, totalRetries);
    }

    @Test
    public void causalReadsOffPerConnection() throws Exception {
        ConsistencyLevel consistencyLevelPerConnection = ConsistencyLevel.CAUSAL_READS_OFF;

        int totalRetries = test(consistencyLevelPerConnection, poolSettings);
        Assert.assertTrue(totalRetries > 0);
    }

    private int test(ConsistencyLevel consistencyLevelPerConnection, PoolSettings poolSettings) throws Exception {
        GaleraClient writerClient = new GaleraClientTest(HOST_WRITER, clientSettings, discoverSettings, galeraDB, poolSettings);
        GaleraClient readerClient = new GaleraClientTest(HOST_READER, clientSettings, discoverSettings, galeraDB, poolSettings);

        int rounds = 300;
        int totalRetries = 0;

        Connection writerConnection = null;
        Connection readerConnection = null;
        PreparedStatement writeStatement = null;
        PreparedStatement readStatement = null;
        for (int i = 0; i < rounds; i++) {

            if (consistencyLevelPerConnection != null) {
                writerConnection = writerClient.getConnection(consistencyLevelPerConnection, null);
                readerConnection = readerClient.getConnection(consistencyLevelPerConnection, null);
            } else {
                writerConnection = writerClient.getConnection();
                readerConnection = readerClient.getConnection();
            }


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

            if (writeStatement != null) { writeStatement.close(); }
            if (readStatement != null) { readStatement.close(); }
            writerConnection.close();
            readerConnection.close();

            totalRetries += retries;
            System.out.println(
                    "--------------------------------------------------round " + i + " number of retries: " + retries + "--------------------------------");
        }

        System.out.println("-------------------------------------------------- Total Retries: " + totalRetries + "--------------------------------");

        writerClient.shutdown();
        readerClient.shutdown();

        return totalRetries;
    }

    private class GaleraClientTest extends GaleraClient {
        private String node;

        @Override
        protected GaleraNode selectNode(ElectionNodePolicy electionNodePolicy) {
            return nodes.get(node);
        }

        protected GaleraClientTest(ClientSettings clientSettings, DiscoverSettings discoverSettings, GaleraDB galeraDB, PoolSettings poolSettings) {
            super(clientSettings, discoverSettings, galeraDB, poolSettings, poolSettings);
            throw new RuntimeException("Don't use this constructor");
        }

        public GaleraClientTest(String nodeName, ClientSettings clientSettings, DiscoverSettings discoverSettings, GaleraDB galeraDB,
                                PoolSettings poolSettings) {
            super(clientSettings, discoverSettings, galeraDB, poolSettings, poolSettings);
            this.node = nodeName;
        }
    }

}

