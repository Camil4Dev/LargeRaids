package com.solarrabbit.largeraids.village;

public class VillageManager {
    public boolean addVillage(org.bukkit.Location location) {
        return location != null && location.getWorld() != null;
    }

    public void removeVillage(org.bukkit.Location location) {
        // API-only build: no-op.
    }
}
