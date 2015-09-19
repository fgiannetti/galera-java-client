package com.despegar.jdbc.galera;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GaleraStatus {
    private static final String INCOMING_ADDRESSES = "wsrep_incoming_addresses";
    private static final String PRIMARY = "Primary";

    private static final String SYNC_WAIT_VARIABLE = "wsrep_sync_wait";
    private static final String CAUSAL_READS_VARIABLE = "wsrep_causal_reads";

    private static final String CLUSTER_STATUS = "wsrep_cluster_status";
    private static final String STATUS_DONOR = "Donor/Desynced";
    private static final String STATUS_SYNCED = "Synced";
    private static final String STATE_VARIABLE = "wsrep_local_state_comment";

    private final Map<String, String> statusMap;

    public GaleraStatus(Map<String, String> statusMap) {
        this.statusMap = statusMap;
    }
    
    public Collection<String> getClusterNodes() {
        return Arrays.asList(statusMap.get(INCOMING_ADDRESSES).split(","));
    }

    public boolean isPrimary() {
        return statusMap.get(CLUSTER_STATUS).equals(PRIMARY);
    }

    public boolean isSynced() {
        return state().equals(STATUS_SYNCED);
    }

    public String state() {
        return statusMap.get(STATE_VARIABLE);
    }

    public boolean isDonor() {
        return state().equals(STATUS_DONOR);
    }

    public boolean isNotDonor() {
        return !this.isDonor();
    }

    public boolean supportsSyncWait() {
        return statusMap.keySet().contains(SYNC_WAIT_VARIABLE);
    }

    public String getGlobalConsistencyLevel() {
        if (supportsSyncWait()) {
            return statusMap.get(SYNC_WAIT_VARIABLE);
        }

        // Earlier mariadb versions
        return statusMap.get(CAUSAL_READS_VARIABLE);
    }

    public static GaleraStatus buildTestStatusOk(String node) {
        Map<String, String> statusMap = new HashMap<String, String>();
        statusMap.put(CLUSTER_STATUS, PRIMARY);
        statusMap.put(STATE_VARIABLE, STATUS_SYNCED);
        statusMap.put(INCOMING_ADDRESSES, node);
        return new GaleraStatus(statusMap);
    }

}
