package com.despegar.jdbc.galera;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class GaleraStatus {
    private final Map<String, String> statusMap;

    public GaleraStatus(Map<String, String> statusMap) {
        this.statusMap = statusMap;
    }
    
    public Collection<String> getClusterNodes() {
        return Arrays.asList(statusMap.get("wsrep_incoming_addresses").split(","));
    }


    public boolean isPrimary() {
        return statusMap.get("wsrep_cluster_status").equals("Primary");
    }

    public boolean isSynced() {
        return state().equals("Synced");
    }

    public String state() {
        return statusMap.get("wsrep_local_state_comment");
    }

    public boolean isDonor() {
        return state().equals("Donor/Desynced");
    }
}
