package com.despegar.jdbc.galera.listener;

import com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GaleraClientLoggingListener implements GaleraClientListener {
    private static final Logger LOG = LoggerFactory.getLogger(GaleraClientLoggingListener.class);

    @Override
    public void onActivatingNode(String node) {
        LOG.info("Activating galera node: {}", node);
    }

    @Override
    public void onMarkingNodeAsDown(String node, String cause) {
        LOG.info("Marking down galera node: {} because of {}", node, cause);
    }

    @Override
    public void onRemovingNode(String node) {
        LOG.info("Removing galera node: {}", node);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).toString();
    }
}
