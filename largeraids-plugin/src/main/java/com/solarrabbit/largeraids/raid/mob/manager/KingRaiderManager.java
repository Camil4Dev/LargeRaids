package com.solarrabbit.largeraids.raid.mob.manager;

import com.solarrabbit.largeraids.LargeRaids;
import com.solarrabbit.largeraids.raid.mob.KingRaider;
import com.solarrabbit.largeraids.util.BukkitEnumUtil;
import com.solarrabbit.largeraids.util.EntityUtil;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Raider;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Spellcaster;
import org.bukkit.entity.Vex;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class KingRaiderManager implements BossRaiderManager, Listener {
    private static final double RAVAGER_MAX_HEALTH = 300;
    private static final double RAVAGER_ATTACK_DAMAGE = 48;
    private static final double FANG_DAMAGE = 6;
    private static final int FIRE_TICK = 7 * 20;
    private static final EntityType RIDER_TYPE = EntityType.EVOKER;

    @Override
    public KingRaider spawn(Location location) {
        Ravager ravager = (Ravager) location.getWorld().spawnEntity(location, EntityType.RAVAGER);
        ravager.setCustomName("Juggernaut");
        Attribute maxHealth = BukkitEnumUtil.attribute("GENERIC_MAX_HEALTH", "MAX_HEALTH");
        Attribute attackDamage = BukkitEnumUtil.attribute("GENERIC_ATTACK_DAMAGE", "ATTACK_DAMAGE");
        if (maxHealth != null && ravager.getAttribute(maxHealth) != null)
            ravager.getAttribute(maxHealth).setBaseValue(RAVAGER_MAX_HEALTH);
        if (attackDamage != null && ravager.getAttribute(attackDamage) != null)
            ravager.getAttribute(attackDamage).setBaseValue(RAVAGER_ATTACK_DAMAGE);
        ravager.setHealth(RAVAGER_MAX_HEALTH);
        ravager.getPersistentDataContainer().set(getJuggernautNamespacedKey(), PersistentDataType.BYTE, (byte) 0);
        ravager.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 4));

        Spellcaster rider = (Spellcaster) location.getWorld().spawnEntity(location, RIDER_TYPE);
        EntityEquipment equipment = rider.getEquipment();
        equipment.setHelmet(getDefaultBanner());
        equipment.setHelmetDropChance(1.0f);
        rider.getPersistentDataContainer().set(getKingNamespacedKey(), PersistentDataType.BYTE, (byte) 0);
        rider.setCustomName("King Raider");

        BossBar bossBar = createBossBar(rider);
        createBossBar(ravager);

        ravager.addPassenger(rider);
        return new KingRaider(rider, ravager, bossBar);
    }

    @EventHandler
    private void onFangsSpawn(EntitySpawnEvent evt) {
        if (evt.getEntityType() != EntityType.EVOKER_FANGS)
            return;
        EvokerFangs fangs = (EvokerFangs) evt.getEntity();
        LivingEntity owner = fangs.getOwner();
        if (owner instanceof Spellcaster && isKing((Spellcaster) owner)) {
            fangs.setVisualFire(true);
            fangs.getPersistentDataContainer().set(getKingFangsNamespacedKey(), PersistentDataType.BYTE, (byte) 0);
        }
    }

    @EventHandler
    private void onFangsAttack(EntityDamageByEntityEvent evt) {
        if (evt.getDamager().getType() != EntityType.EVOKER_FANGS)
            return;
        if (evt.getEntity() instanceof Raider)
            return;
        if (isKingFangs((EvokerFangs) evt.getDamager())) {
            evt.getEntity().setFireTicks(FIRE_TICK);
            evt.setDamage(FANG_DAMAGE);
        }
    }

    @EventHandler
    private void onSummonVex(CreatureSpawnEvent evt) {
        if (evt.getEntityType() != EntityType.VEX)
            return;
        Vex vex = (Vex) evt.getEntity();
        LivingEntity owner = EntityUtil.getVexOwner(vex);
        if (!(owner instanceof Spellcaster))
            return;
        Spellcaster evoker = (Spellcaster) owner;
        if (isKing(evoker))
            vex.getEquipment().setItemInMainHand(getKingVexSword());
    }

    @EventHandler
    private void onKingDamage(EntityDamageEvent evt) {
        if (evt.getEntityType() != RIDER_TYPE)
            return;
        Spellcaster king = (Spellcaster) evt.getEntity();
        Entity vehicle = king.getVehicle();
        // Kings riding juggernauts are invulnerable
        if (isKing(king) && vehicle instanceof Ravager && isJuggernaut((Ravager) vehicle))
            evt.setCancelled(true);
    }

    private ItemStack getDefaultBanner() {
        ItemStack banner = new ItemStack(Material.WHITE_BANNER);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        addPattern(meta, DyeColor.CYAN, "RHOMBUS_MIDDLE", "RHOMBUS");
        addPattern(meta, DyeColor.LIGHT_GRAY, "STRIPE_BOTTOM");
        addPattern(meta, DyeColor.BLACK, "HALF_HORIZONTAL", "HALF_HORIZONTAL_BOTTOM");
        addPattern(meta, DyeColor.BLACK, "STRIPE_MIDDLE");
        addPattern(meta, DyeColor.GRAY, "STRIPE_CENTER");
        addPattern(meta, DyeColor.BLACK, "SKULL");
        addPattern(meta, DyeColor.LIGHT_GRAY, "CIRCLE_MIDDLE", "CIRCLE");
        addPattern(meta, DyeColor.BLACK, "TRIANGLE_TOP");
        addPattern(meta, DyeColor.BLACK, "BORDER");
        addItemFlag(meta, "HIDE_POTION_EFFECTS", "HIDE_ADDITIONAL_TOOLTIP");
        meta.setDisplayName(ChatColor.RED.toString() + ChatColor.ITALIC + "King Raider Banner");
        banner.setItemMeta(meta);
        return banner;
    }

    private ItemStack getKingVexSword() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        item.addEnchantment(Enchantment.FIRE_ASPECT, 2);
        Enchantment sharpness = BukkitEnumUtil.enchantment("DAMAGE_ALL", "SHARPNESS");
        if (sharpness != null)
            item.addEnchantment(sharpness, 3);
        return item;
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

    private boolean isJuggernaut(Ravager entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        return pdc.has(getJuggernautNamespacedKey(), PersistentDataType.BYTE);
    }

    private boolean isKing(Raider entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        return pdc.has(getKingNamespacedKey(), PersistentDataType.BYTE);
    }

    private boolean isKingFangs(EvokerFangs fangs) {
        PersistentDataContainer pdc = fangs.getPersistentDataContainer();
        return pdc.has(getKingFangsNamespacedKey(), PersistentDataType.BYTE);
    }

    private NamespacedKey getJuggernautNamespacedKey() {
        return new NamespacedKey(JavaPlugin.getPlugin(LargeRaids.class), "juggernaut");
    }

    private NamespacedKey getKingNamespacedKey() {
        return new NamespacedKey(JavaPlugin.getPlugin(LargeRaids.class), "juggernaut_king");
    }

    private NamespacedKey getKingFangsNamespacedKey() {
        return new NamespacedKey(JavaPlugin.getPlugin(LargeRaids.class), "juggernaut_king_fangs");
    }

}
