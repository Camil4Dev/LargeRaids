package com.solarrabbit.largeraids.util;

import java.lang.reflect.Method;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Vex;

public final class EntityUtil {
    private EntityUtil() {
    }

    public static LivingEntity getVexOwner(Vex vex) {
        if (vex == null) {
            return null;
        }
        LivingEntity owner = invokeLivingEntity(vex, "getOwner");
        if (owner != null) {
            return owner;
        }
        return invokeLivingEntity(vex, "getSummoner");
    }

    private static LivingEntity invokeLivingEntity(Vex vex, String methodName) {
        try {
            Method method = vex.getClass().getMethod(methodName);
            Object res = method.invoke(vex);
            return res instanceof LivingEntity ? (LivingEntity) res : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
