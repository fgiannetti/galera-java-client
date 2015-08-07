package com.despegar.jdbc.galera;

public class GaleraDB {
    public static final String MYSQL_URL_PREFIX = "jdbc:mysql://";
    public static final String MYSQL_URL_SEPARATOR = "/";

    public final String jdbcUrlPrefix;
    public final String jdbcUrlSeparator;
    public final String database;
    public final String user;
    public final String password;

    public GaleraDB(String database, String user, String password) {
        this(database, user, password, MYSQL_URL_PREFIX, MYSQL_URL_SEPARATOR);
    }

    public GaleraDB(String database, String user, String password, String jdbcUrlPrefix, String jdbcUrlSeparator) {
        this.database = database;
        this.user = user;
        this.password = password;
        this.jdbcUrlPrefix = jdbcUrlPrefix;
        this.jdbcUrlSeparator = jdbcUrlSeparator;
    }

}
