package com.despegar.jdbc.galera;

import com.google.common.base.Joiner;

import javax.annotation.Nonnull;
import java.util.List;

public class NoHostAvailableException extends RuntimeException {

    public NoHostAvailableException(@Nonnull List<String> hosts){
        super("Non of the following active hosts is available:" + Joiner.on(",").join(hosts));
    }
}
