package com.despegar.jdbc.galera;

public class GaleraDB {
    public final String database;
    public final String user;
    public final String password;

    public GaleraDB(String database, String user, String password) {
        this.database = database;
        this.user = user;
        this.password = password;
    }
}
