package com.solarrabbit.largeraids.util;

import org.bukkit.Bukkit;

public class VersionUtil {
    public static int getServerMinorVersion() {
        return getMinorVersion(getServerVersion());
    }

    public static boolean isSupported() {
        String version = getServerVersion();
        return getMajorVersion(version) == 1 && getMinorVersion(version) == 21;
    }

    public static int compare(String versionA, String versionB) {
        int majDiff = getMajorVersion(versionA) - getMajorVersion(versionB);
        if (majDiff != 0)
            return majDiff;
        int minorDiff = getMinorVersion(versionA) - getMinorVersion(versionB);
        if (minorDiff != 0)
            return minorDiff;
        return getPatchVersion(versionA) - getPatchVersion(versionB);
    }

    private static int getMajorVersion(String version) {
        String[] splits = version.split("\\.");
        return Integer.parseInt(splits[0]);
    }

    private static int getMinorVersion(String version) {
        String[] splits = version.split("\\.");
        return splits.length < 2 ? 0 : Integer.parseInt(splits[1]);
    }

    private static int getPatchVersion(String version) {
        String[] splits = version.split("\\.");
        return splits.length < 3 ? 0 : Integer.parseInt(splits[2]);
    }

    private static String getServerVersion() {
        String raw = Bukkit.getBukkitVersion();
        int dash = raw.indexOf('-');
        return dash == -1 ? raw : raw.substring(0, dash);
    }

}
