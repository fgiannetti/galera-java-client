package com.despegar.jdbc.galera.settings;

import com.despegar.jdbc.galera.listener.GaleraClientListener;
import com.despegar.jdbc.galera.policies.ElectionNodePolicy;

import java.util.ArrayList;

public class ClientSettings {
    public final ArrayList<String> seeds;
    public final int retriesToGetConnection;
    public final GaleraClientListener galeraClientListener;
    public final ElectionNodePolicy defaultNodeSelectionPolicy;

    /**
     * Onyl enabled this feature for test purpouses. It disables discovery capabilities and checks for status nodes too.
     */
    public final boolean testMode;

    public ClientSettings(ArrayList<String> seeds, int retriesToGetConnection, GaleraClientListener galeraClientListener,
                          ElectionNodePolicy defaultNodeSelectionPolicy, boolean testMode) {
        this.seeds = seeds;
        this.retriesToGetConnection = retriesToGetConnection;
        this.galeraClientListener = galeraClientListener;
        this.defaultNodeSelectionPolicy = defaultNodeSelectionPolicy;
        this.testMode = testMode;
    }
}
