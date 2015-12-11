package com.despegar.jdbc.galera.utils;

import com.google.common.base.Optional;

public class PoolNameHelper {

    public static final String DEFAULT_POOL_PREFIX_NAME = "hikari-pool";
    public static final String STATUS_POOL_PREFIX_NAME = "status-";

    /**
     * Because of errors when hikari pool name have ':' character, we remove the last part of the node name (:port).
     *
     * @param node it has the following pattern "host:port"
     * @return
     */
    public static String nodeNameWithoutPort(String node) {
        return node.split(":")[0];
    }

    public static String getFullPoolName(Optional<String> poolName, String node) {
        return poolName.or(DEFAULT_POOL_PREFIX_NAME) + "." + nodeNameWithoutPort(node);
    }

    public static String getFullStatusPoolName(Optional<String> poolName, String node) {
        return STATUS_POOL_PREFIX_NAME + getFullPoolName(poolName, node);
    }

}
