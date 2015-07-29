package com.despegar.jdbc.galera;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

public abstract class AbstractGaleraDataSource implements DataSource {

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    // C3PO implementation
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if ( this.isWrapperForThis( iface ) )
            return (T) this;
        else
            throw new SQLException(this + " is not a wrapper for or implementation of " + iface.getName());
    }

    protected final boolean isWrapperForThis(Class<?> iface) {
        return iface.isAssignableFrom( this.getClass() );
    }

    // C3PO implementation
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return isWrapperForThis( iface );
    }
}
