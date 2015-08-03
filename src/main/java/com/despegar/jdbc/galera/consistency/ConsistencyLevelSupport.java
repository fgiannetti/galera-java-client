package com.despegar.jdbc.galera.consistency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ConsistencyLevelSupport {
    private static final Logger LOG = LoggerFactory.getLogger(ConsistencyLevelSupport.class);

    private static final String SET_SESSION_SYNC_WAIT = "SET SESSION wsrep_sync_wait = ?";
    private static final String SET_SESSION_CAUSAL_READS = "SET SESSION wsrep_causal_reads = ?";

    public static void set(Connection connection, String consistencyLevel, boolean supportsSyncWait) throws SQLException {
        PreparedStatement preparedStatement = null;

        try {
            if (supportsSyncWait) {
                LOG.info("Setting wsrep_sync_wait to {}", consistencyLevel);
                preparedStatement = connection.prepareStatement(SET_SESSION_SYNC_WAIT);
                preparedStatement.setInt(1, Integer.valueOf(consistencyLevel));
            } else {
                LOG.info("Setting wsrep_causal_reads to {}", consistencyLevel);
                preparedStatement = connection.prepareStatement(SET_SESSION_CAUSAL_READS);
                preparedStatement.setString(1, consistencyLevel);
            }

            preparedStatement.execute();

        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }
}
