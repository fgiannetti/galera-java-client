package com.despegar.jdbc.galera;

import com.google.common.base.MoreObjects;

import javax.annotation.Nonnull;

public class GaleraDB {
    public static final String MYSQL_URL_PREFIX = "jdbc:mysql://";
    public static final String MYSQL_URL_SEPARATOR = "/";

    public final String jdbcUrlPrefix;
    public final String jdbcUrlSeparator;
    public final String database;
    public final String user;
    public final String password;

    public GaleraDB(@Nonnull String database, @Nonnull String user, @Nonnull String password) {
        this(database, user, password, MYSQL_URL_PREFIX, MYSQL_URL_SEPARATOR);
    }

    public GaleraDB(@Nonnull String database, @Nonnull String user, @Nonnull String password, @Nonnull String jdbcUrlPrefix, @Nonnull String jdbcUrlSeparator) {
        this.database = database;
        this.user = user;
        this.password = password;
        this.jdbcUrlPrefix = jdbcUrlPrefix;
        this.jdbcUrlSeparator = jdbcUrlSeparator;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("jdbcUrlPrefix", jdbcUrlPrefix)
                .add("jdbcUrlSeparator", jdbcUrlSeparator)
                .add("database", database)
                .add("user", user)
                .add("password", password)
                .toString();
    }
}
