package com.despegar.jdbc.galera.settings;

import com.despegar.jdbc.galera.listener.GaleraClientListener;
import com.despegar.jdbc.galera.policies.ElectionNodePolicy;

import java.util.ArrayList;

public class ClientSettings {
    public final ArrayList<String> seeds;
    public final int retriesToGetConnection;
    public final GaleraClientListener galeraClientListener;
    public final ElectionNodePolicy masterPolicy;


    public ClientSettings(ArrayList<String> seeds, int retriesToGetConnection, GaleraClientListener galeraClientListener, ElectionNodePolicy masterPolicy) {
        this.seeds = seeds;
        this.retriesToGetConnection = retriesToGetConnection;
        this.galeraClientListener = galeraClientListener;
        this.masterPolicy = masterPolicy;
    }
}
