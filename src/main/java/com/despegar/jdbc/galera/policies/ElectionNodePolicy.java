package com.despegar.jdbc.galera.policies;

import java.util.List;

public interface ElectionNodePolicy {

    String chooseNode(List<String> activeNodes);

    String getName();

}
