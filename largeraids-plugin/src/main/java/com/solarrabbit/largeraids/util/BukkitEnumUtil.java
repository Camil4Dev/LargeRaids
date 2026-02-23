package com.solarrabbit.largeraids.util;

import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemFlag;

public final class BukkitEnumUtil {
    private BukkitEnumUtil() {
    }

    public static Particle particle(String... names) {
        return enumValue(Particle.class, names);
    }

    public static EntityType entityType(String... names) {
        return enumValue(EntityType.class, names);
    }

    public static PatternType patternType(String... names) {
        return enumValue(PatternType.class, names);
    }

    public static ItemFlag itemFlag(String... names) {
        return enumValue(ItemFlag.class, names);
    }

    public static Enchantment enchantment(String... names) {
        return enumValue(Enchantment.class, names);
    }

    public static Attribute attribute(String... names) {
        return enumValue(Attribute.class, names);
    }

    private static <T> T enumValue(Class<T> type, String... names) {
        if (names == null) {
            return null;
        }
        for (String name : names) {
            if (name == null) {
                continue;
            }
            T resolved = resolve(type, name);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static <T> T resolve(Class<T> type, String name) {
        if (type.isEnum()) {
            try {
                return (T) Enum.valueOf((Class<? extends Enum>) type, name);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        try {
            return type.cast(type.getMethod("valueOf", String.class).invoke(null, name));
        } catch (ReflectiveOperationException ignored) {
            // Ignore and try other strategies.
        }
        try {
            return type.cast(type.getMethod("getByName", String.class).invoke(null, name));
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
