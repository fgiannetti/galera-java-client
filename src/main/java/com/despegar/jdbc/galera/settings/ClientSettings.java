package com.despegar.jdbc.galera.settings;

import com.despegar.jdbc.galera.listener.GaleraClientListener;

import java.util.ArrayList;

public class ClientSettings {
    public final ArrayList<String> seeds;
    public final int retriesToGetConnection;
    public final GaleraClientListener galeraClientListener;


    public ClientSettings(ArrayList<String> seeds, int retriesToGetConnection, GaleraClientListener galeraClientListener) {
        this.seeds = seeds;
        this.retriesToGetConnection = retriesToGetConnection;
        this.galeraClientListener = galeraClientListener;
    }
}
