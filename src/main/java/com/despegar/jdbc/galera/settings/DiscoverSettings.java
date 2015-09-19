package com.despegar.jdbc.galera.settings;

import com.google.common.base.MoreObjects;

public class DiscoverSettings {
    public final long discoverPeriod;

    /**
     * When this flag is true, donor nodes are marked as down, so you will not get connections from donor nodes.
     */
    public final boolean ignoreDonor;

    public DiscoverSettings(long discoverPeriod, boolean ignoreDonor) {
        this.discoverPeriod = discoverPeriod;
        this.ignoreDonor = ignoreDonor;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("discoverPeriod", discoverPeriod)
                .add("ignoreDonor", ignoreDonor)
                .toString();
    }
}
