package com.despegar.jdbc.galera.settings;

import com.despegar.jdbc.galera.listener.GaleraClientListener;
import com.despegar.jdbc.galera.policies.ElectionNodePolicy;
import com.google.common.base.MoreObjects;

import java.util.List;

public class ClientSettings {
    public final List<String> seeds;
    public final int retriesToGetConnection;
    public final GaleraClientListener galeraClientListener;
    public final ElectionNodePolicy defaultNodeSelectionPolicy;

    /**
     * Onyl enabled this feature for test purpouses. It disables discovery capabilities and checks for status nodes too.
     */
    public final boolean testMode;

    public ClientSettings(List<String> seeds, int retriesToGetConnection, GaleraClientListener galeraClientListener,
                          ElectionNodePolicy defaultNodeSelectionPolicy, boolean testMode) {
        this.seeds = seeds;
        this.retriesToGetConnection = retriesToGetConnection;
        this.galeraClientListener = galeraClientListener;
        this.defaultNodeSelectionPolicy = defaultNodeSelectionPolicy;
        this.testMode = testMode;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("seeds", seeds)
                .add("retriesToGetConnection", retriesToGetConnection)
                .add("galeraClientListener", galeraClientListener)
                .add("defaultNodeSelectionPolicy", defaultNodeSelectionPolicy)
                .add("testMode", testMode)
                .toString();
    }
}
