package com.despegar.jdbc.galera.policies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * We choose master choosing always the first active node sorted alphabetically
 */
public class MasterSortingNodesPolicy implements ElectionNodePolicy {
    private static final Logger LOG = LoggerFactory.getLogger(MasterSortingNodesPolicy.class);

    public String chooseNode(List<String> activeNodes) {

        String[] nodes = activeNodes.toArray(new String[activeNodes.size()]);
        Arrays.sort(nodes);
        String master = nodes[0];

        LOG.info("Master node selected {}", master);

        return master;
    }

    @Override
    public String getName() {
        return "MasterSortingNodes";
    }
}
