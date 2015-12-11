package com.despegar.jdbc.galera.utils;

public class PoolNameHelper {

    public static final String STATUS_POOL_PREFIX_NAME = "hikari-pool-status-";
    public static final String CLIENT_POOL_PREFIX_NAME = "hikari-pool-";

    /**
     * Because of errors when hikari pool name have ':' character, we remove the last part of the node name (:port).
     *
     * @param node it has the following pattern "host:port"
     * @return
     */
    public static String nodeNameWithoutPort(String node) {
        return node.split(":")[0];
    }
}
