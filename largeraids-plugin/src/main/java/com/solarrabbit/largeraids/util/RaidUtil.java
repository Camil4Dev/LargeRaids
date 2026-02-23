package com.solarrabbit.largeraids.util;

import java.lang.reflect.Method;

import org.bukkit.Raid;

public final class RaidUtil {
    private RaidUtil() {
    }

    public static boolean stopRaid(Raid raid) {
        return invokeVoid(raid, "stop");
    }

    public static boolean setBadOmenLevel(Raid raid, int level) {
        if (raid == null) {
            return false;
        }
        try {
            Method method = raid.getClass().getMethod("setBadOmenLevel", int.class);
            method.invoke(raid, level);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean invokeVoid(Raid raid, String methodName) {
        if (raid == null) {
            return false;
        }
        try {
            Method method = raid.getClass().getMethod(methodName);
            method.invoke(raid);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
