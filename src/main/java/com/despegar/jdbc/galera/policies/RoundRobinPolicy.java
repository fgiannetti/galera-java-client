package com.despegar.jdbc.galera.policies;

import com.despegar.jdbc.galera.GaleraNode;
import com.despegar.jdbc.galera.NoHostAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Select the next active galera node
 */
public class RoundRobinPolicy implements ElectionNodePolicy {
    private static final Logger LOG = LoggerFactory.getLogger(RoundRobinPolicy.class);

    private AtomicInteger nextNodeIndex = new AtomicInteger(new Random().nextInt(997));

    @Override
    public String chooseNode(List<String> activeNodes) {
        String selectedNode = activeNodes.get(nextNodeIndex(activeNodes));
        LOG.debug("Selected roundRobin node {}", selectedNode);
        return selectedNode;
    }

    private int nextNodeIndex(List<String> activeNodes) {
        int activeNodesCount = activeNodes.size();
        if (activeNodesCount == 0) {
            LOG.error("NoHostAvailableException - Active node count is zero");
            throw new NoHostAvailableException();
        }

        return getNextIndex() % activeNodesCount;
    }

    private int getNextIndex() {
        int nextIndex = nextNodeIndex.getAndIncrement();

        if (nextIndex > 1000000) {
            nextNodeIndex.set(0);
        }

        return nextIndex;
    }

}
