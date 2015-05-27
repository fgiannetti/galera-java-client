package com.despegar.jdbc.galera.policies;

import com.despegar.jdbc.galera.GaleraNode;

import java.util.List;

public interface ElectionNodePolicy {

    String chooseNode(List<String> activeNodes);

}
