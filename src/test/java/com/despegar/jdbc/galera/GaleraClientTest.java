package com.despegar.jdbc.galera;

import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.sql.Connection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GaleraClientTest {

    @Test
    public void getConnection_writable() throws Exception {

        final GaleraClient instance = GaleraClient.newBuilder()
                .testMode(true)
                .jdbcUrlPrefix("jdbc:h2:")
                .seeds("mem")
                .jdbcUrlSeparator(":")
                .database("test;MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE")
                .user("sa")
                .readOnly(false)
                .build();

        final Connection connection = instance.getConnection();

        try {
            MatcherAssert.assertThat("connection", connection, notNullValue());
            MatcherAssert.assertThat(connection.isClosed(), equalTo(false));
            MatcherAssert.assertThat(connection.isReadOnly(), equalTo(false));
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

    }

    @Test(expected = NoActiveNodeException.class)
    public void getConnection_noActiveNode_throwException() throws Exception {

        final GaleraClient instance = GaleraClient.newBuilder()
                .testMode(true)
                .jdbcUrlPrefix("jdbc:h2:tcp://")
                .seeds("localhost:9290") // unexistent
                .database("test;MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE")
                .user("sa")
                .readOnly(false)
                .retriesToGetConnection(1)
                .connectionTimeout(1)
                .connectTimeout(1)
                .build();

        instance.getConnection();

    }

}