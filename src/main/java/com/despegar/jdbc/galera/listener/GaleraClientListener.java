package com.despegar.jdbc.galera.listener;

public interface GaleraClientListener {

    void onActivatingNode(String node);

    void onMarkingNodeAsDown(String node, String cause);

    void onRemovingNode(String node);

}
