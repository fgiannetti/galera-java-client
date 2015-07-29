package com.despegar.jdbc.galera.settings;

import com.despegar.jdbc.galera.listener.GaleraDataSourceListener;
import com.despegar.jdbc.galera.policies.ElectionNodePolicy;

import java.util.ArrayList;

public class DataSourceSettings {
    public final ArrayList<String> seeds;
    public final int retriesToGetConnection;
    public final GaleraDataSourceListener galeraDataSourceListener;
    public final ElectionNodePolicy masterPolicy;
    public final ElectionNodePolicy nodeSelectionPolicy;


    public DataSourceSettings(ArrayList<String> seeds, int retriesToGetConnection, GaleraDataSourceListener galeraDataSourceListener, ElectionNodePolicy masterPolicy, ElectionNodePolicy nodePolicy) {
        this.seeds = seeds;
        this.retriesToGetConnection = retriesToGetConnection;
        this.galeraDataSourceListener = galeraDataSourceListener;
        this.masterPolicy = masterPolicy;
        this.nodeSelectionPolicy = nodePolicy;
    }
}
