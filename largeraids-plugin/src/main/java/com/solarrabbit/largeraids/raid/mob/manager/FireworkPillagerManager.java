package com.solarrabbit.largeraids.raid.mob.manager;

import com.solarrabbit.largeraids.LargeRaids;
import com.solarrabbit.largeraids.raid.mob.FireworkPillager;
import com.solarrabbit.largeraids.util.BukkitEnumUtil;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Raider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class FireworkPillagerManager implements CustomRaiderManager, Listener {
    private static final float DEFAULT_MAX_HEALTH = 48.0f;

    @Override
    public FireworkPillager spawn(Location location) {
        Pillager entity = (Pillager) location.getWorld().spawnEntity(location, EntityType.PILLAGER);
        Attribute maxHealth = BukkitEnumUtil.attribute("GENERIC_MAX_HEALTH", "MAX_HEALTH");
        if (maxHealth != null && entity.getAttribute(maxHealth) != null)
            entity.getAttribute(maxHealth).setBaseValue(DEFAULT_MAX_HEALTH);
        entity.setHealth(DEFAULT_MAX_HEALTH);
        EntityEquipment equipment = entity.getEquipment();
        equipment.setItemInOffHand(getDefaultFirework());
        equipment.setHelmet(getDefaultBanner());
        equipment.setHelmetDropChance(1.0f);
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(getPillagerNamespacedKey(), PersistentDataType.BYTE, (byte) 0);
        entity.setCustomName("Firework Pillager");
        return new FireworkPillager(entity);
    }

    @EventHandler
    private void onBowShoot(EntityShootBowEvent evt) {
        if (evt.getEntityType() != EntityType.PILLAGER)
            return;
        Pillager pillager = (Pillager) evt.getEntity();
        if (isFireworkPillager(pillager)) {
            pillager.getEquipment().setItemInOffHand(getDefaultFirework());
            evt.getProjectile().getPersistentDataContainer().set(getFireworkNamespacedKey(), PersistentDataType.BYTE,
                    (byte) 0);
        }
    }

    @EventHandler
    private void onDamageRaider(EntityDamageByEntityEvent evt) {
        if (evt.getCause() != DamageCause.ENTITY_EXPLOSION)
            return;
        EntityType fireworkType = BukkitEnumUtil.entityType("FIREWORK", "FIREWORK_ROCKET");
        if (fireworkType == null || evt.getDamager().getType() != fireworkType)
            return;
        if (!(evt.getEntity() instanceof Raider))
            return;
        Firework firework = (Firework) evt.getDamager();
        if (isPillagerFirework(firework))
            evt.setCancelled(true);
    }

    private ItemStack getDefaultFirework() {
        ItemStack firework = new ItemStack(Material.FIREWORK_ROCKET);
        FireworkMeta meta = (FireworkMeta) firework.getItemMeta();
        FireworkEffect effect = FireworkEffect.builder().with(Type.BALL_LARGE)
                .withColor(Color.AQUA, Color.ORANGE, Color.FUCHSIA).flicker(true).build();
        meta.addEffects(effect, effect, effect, effect, effect);
        firework.setItemMeta(meta);
        return firework;
    }

    private ItemStack getDefaultBanner() {
        ItemStack banner = new ItemStack(Material.YELLOW_BANNER);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        addPattern(meta, DyeColor.MAGENTA, "STRIPE_CENTER");
        addPattern(meta, DyeColor.ORANGE, "CURLY_BORDER");
        addPattern(meta, DyeColor.RED, "STRIPE_SMALL", "STRIPE_SMALL_HORIZONTAL");
        addPattern(meta, DyeColor.RED, "RHOMBUS_MIDDLE", "RHOMBUS");
        addPattern(meta, DyeColor.YELLOW, "FLOWER");
        addPattern(meta, DyeColor.BLACK, "BORDER");
        addItemFlag(meta, "HIDE_POTION_EFFECTS", "HIDE_ADDITIONAL_TOOLTIP");
        meta.setDisplayName(ChatColor.GOLD.toString() + ChatColor.ITALIC + "Firework Pillager Banner");
        banner.setItemMeta(meta);
        return banner;
    }

    private void addPattern(BannerMeta meta, DyeColor color, String... names) {
        PatternType type = BukkitEnumUtil.patternType(names);
        if (type != null)
            meta.addPattern(new Pattern(color, type));
    }

    private void addItemFlag(BannerMeta meta, String... names) {
        ItemFlag flag = BukkitEnumUtil.itemFlag(names);
        if (flag != null)
            meta.addItemFlags(flag);
    }

    private boolean isFireworkPillager(Pillager pillager) {
        PersistentDataContainer pdc = pillager.getPersistentDataContainer();
        return pdc.has(getPillagerNamespacedKey(), PersistentDataType.BYTE);
    }

    private boolean isPillagerFirework(Firework firework) {
        PersistentDataContainer pdc = firework.getPersistentDataContainer();
        return pdc.has(getFireworkNamespacedKey(), PersistentDataType.BYTE);
    }

    private NamespacedKey getPillagerNamespacedKey() {
        return new NamespacedKey(JavaPlugin.getPlugin(LargeRaids.class), "firework_pillager");
    }

    private NamespacedKey getFireworkNamespacedKey() {
        return new NamespacedKey(JavaPlugin.getPlugin(LargeRaids.class), "pillager_firework");
    }

}
