package com.despegar.jdbc.galera.settings;

import java.util.ArrayList;

public class ClientSettings {
    public final ArrayList<String> seeds;
    public final int retriesToGetConnection;


    public ClientSettings(ArrayList<String> seeds, int retriesToGetConnection) {
        this.seeds = seeds;
        this.retriesToGetConnection = retriesToGetConnection;
    }
}
