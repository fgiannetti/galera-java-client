package com.despegar.jdbc.galera;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * This proxy is responsible for managing wsrep_sync_wait (wsrep_causal_reads on earlier mariaDB versions) at connection level.
 * When you get a connection, the proxy sets the connection to the desired wsrep_sync_wait level.
 * The proxy releases wsrep_sync_wait value to mariadb global setting when closing connections.
 */
public class GaleraProxyConnection implements InvocationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GaleraProxyConnection.class);

    private Connection underlyingConnection;
    private int globalConsistencyLevel;
    private boolean supportsSyncWait;

    public GaleraProxyConnection(Connection conn, ConsistencyLevel connectionConsistencyLevel, GaleraStatus galeraStatus) throws Exception {
        this.underlyingConnection = conn;
        this.supportsSyncWait = galeraStatus.supportsSyncWait();
        this.globalConsistencyLevel = galeraStatus.getGlobalConsistencyLevel();

        validate(connectionConsistencyLevel);

        LOG.info("Setting connection consistency level to " + connectionConsistencyLevel);
        setConnectionConsistencyLevel(connectionConsistencyLevel.value);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("close".equals(method.getName())) {
            LOG.info("Setting wsrep_sync_wait to global default before closing connection");
            setConnectionConsistencyLevel(globalConsistencyLevel);
        }
        return method.invoke(underlyingConnection, args);
    }

    public static Connection create(Connection toWrap, ConsistencyLevel connectionConsistencyLevel, GaleraStatus galeraStatus) throws Exception {
        return (Connection)(Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class[]{Connection.class},
                new GaleraProxyConnection(toWrap, connectionConsistencyLevel, galeraStatus)));
    }

    private void validate(ConsistencyLevel connectionConsistencyLevel) {
        if (!this.supportsSyncWait && connectionConsistencyLevel != ConsistencyLevel.CAUSAL_READS_ON && connectionConsistencyLevel != ConsistencyLevel.CAUSAL_READS_OFF) {
            LOG.warn("Your MariaDB version does not support syncWait and you are trying to configure connection consistency level to " + connectionConsistencyLevel);
        }
    }

    private void setConnectionConsistencyLevel(Integer consistencyLevel) throws Exception {
        PreparedStatement preparedStatement = null;

        try {
            String stmt = (this.supportsSyncWait) ? "SET SESSION wsrep_sync_wait = ?" : "set session wsrep-causal-reads = ?";

            preparedStatement = underlyingConnection.prepareStatement(stmt);
            preparedStatement.setInt(1, consistencyLevel);
            preparedStatement.execute();

        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

}
