package com.despegar.jdbc.galera;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GaleraDB {
    public static final String MYSQL_URL_PREFIX = "jdbc:mysql://";
    public static final String MYSQL_URL_SEPARATOR = "/";

    public final String jdbcUrlPrefix;
    public final String jdbcUrlSeparator;
    public final String database;
    public final String user;
    public final String password;
    public final Optional<String> driverClassName;

    public GaleraDB(@Nonnull String database, @Nonnull String user, @Nonnull String password) {
        this(database, user, password, MYSQL_URL_PREFIX, MYSQL_URL_SEPARATOR, Optional.<String>absent());
    }

    public GaleraDB(@Nonnull String database, @Nonnull String user, @Nonnull String password, @Nonnull String jdbcUrlPrefix, @Nonnull String jdbcUrlSeparator, Optional<String> driverClassName) {
        this.database = database;
        this.user = user;
        this.password = password;
        this.jdbcUrlPrefix = jdbcUrlPrefix;
        this.jdbcUrlSeparator = jdbcUrlSeparator;
        this.driverClassName = driverClassName;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("jdbcUrlPrefix", jdbcUrlPrefix)
                .add("jdbcUrlSeparator", jdbcUrlSeparator)
                .add("database", database)
                .add("user", user)
                .add("driverClassName", driverClassName.orNull())
                .toString();
    }
}
