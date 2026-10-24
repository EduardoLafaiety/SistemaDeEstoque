package com.estoque.service;

import java.util.List;

import com.estoque.domain.Location;

/** Dados da tela de reposição (com getters para uso nos templates). */
public class ReplenishmentView {

    private final List<ReplenishmentLine> lines;
    private final List<LocationCount> locationCounts;
    private final long allLocationsCount;
    private final long allTriggersCount;
    private final long autoCount;
    private final long manualCount;

    public ReplenishmentView(List<ReplenishmentLine> lines, List<LocationCount> locationCounts, long allLocationsCount,
                             long allTriggersCount, long autoCount, long manualCount) {
        this.lines = lines;
        this.locationCounts = locationCounts;
        this.allLocationsCount = allLocationsCount;
        this.allTriggersCount = allTriggersCount;
        this.autoCount = autoCount;
        this.manualCount = manualCount;
    }

    public List<ReplenishmentLine> getLines() { return lines; }
    public List<LocationCount> getLocationCounts() { return locationCounts; }
    public long getAllLocationsCount() { return allLocationsCount; }
    public long getAllTriggersCount() { return allTriggersCount; }
    public long getAutoCount() { return autoCount; }
    public long getManualCount() { return manualCount; }

    public static class LocationCount {
        private final Location location;
        private final long count;

        public LocationCount(Location location, long count) {
            this.location = location;
            this.count = count;
        }

        public Location getLocation() { return location; }
        public long getCount() { return count; }
    }
}
