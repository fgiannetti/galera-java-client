package com.despegar.jdbc.galera.consistency;

public enum ConsistencyLevel {

    // Introduced MariaDB Galera 5.5.39 - MariaDB Galera 10.0.13
    SYNC_OFF("0"),
    SYNC_READS("1"),
    SYNC_UPDATE_DELETE("2"),
    SYNC_READ_UPDATE_DELETE("3"),
    SYNC_INSERT_REPLACE("4"),

    // Earlier versions
    CAUSAL_READS_OFF("OFF"),
    CAUSAL_READS_ON("ON");

    public final String value;

    ConsistencyLevel(String value) {
        this.value = value;
    }

}


