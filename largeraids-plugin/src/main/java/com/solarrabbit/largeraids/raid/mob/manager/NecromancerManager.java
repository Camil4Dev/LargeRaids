package com.solarrabbit.largeraids.raid.mob.manager;

import com.solarrabbit.largeraids.LargeRaids;
import com.solarrabbit.largeraids.raid.mob.Necromancer;
import com.solarrabbit.largeraids.util.BukkitEnumUtil;
import com.solarrabbit.largeraids.util.EntityUtil;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Raider;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Spellcaster;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class NecromancerManager implements CustomRaiderManager, Listener {

    @Override
    public Necromancer spawn(Location location) {
        Spellcaster evoker = (Spellcaster) location.getWorld().spawnEntity(location, EntityType.EVOKER);
        EntityEquipment equipment = evoker.getEquipment();
        equipment.setHelmet(getDefaultBanner());
        equipment.setHelmetDropChance(1.0f);
        evoker.getPersistentDataContainer().set(getNecromancerNamespacedKey(), PersistentDataType.BYTE, (byte) 0);
        evoker.setCustomName("Necromancer");
        return new Necromancer(evoker);
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
        if (isNecromancer(evoker)) {
            evt.setCancelled(true);
            LivingEntity target = evoker.getTarget();
            if (target instanceof HumanEntity) {
                vex.getWorld().strikeLightningEffect(vex.getLocation());
                Skeleton skeleton = (Skeleton) vex.getWorld().spawnEntity(vex.getLocation(), EntityType.SKELETON);
                skeleton.getEquipment().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
            } else if (target instanceof Villager) {
                vex.getWorld().strikeLightningEffect(vex.getLocation());
                Zombie zombie = (Zombie) vex.getWorld().spawnEntity(vex.getLocation(), EntityType.ZOMBIE);
                zombie.getEquipment().setHelmet(new ItemStack(Material.CARVED_PUMPKIN));
            }
        }
    }

    @EventHandler
    private void onFangSpawn(EntitySpawnEvent evt) {
        if (evt.getEntityType() != EntityType.EVOKER_FANGS)
            return;
        EvokerFangs fangs = (EvokerFangs) evt.getEntity();
        LivingEntity owner = fangs.getOwner();
        if (owner instanceof Spellcaster && isNecromancer((Spellcaster) owner)) {
            evt.setCancelled(true);
            LightningStrike lightning = fangs.getWorld().strikeLightning(fangs.getLocation());
            lightning.getPersistentDataContainer().set(getNecromancerLightningNamespacedKey(), PersistentDataType.BYTE,
                    (byte) 0);
        }
    }

    @EventHandler
    private void onLightningFire(BlockIgniteEvent evt) {
        if (evt.getCause() != IgniteCause.LIGHTNING)
            return;
        LightningStrike lightning = (LightningStrike) evt.getIgnitingEntity();
        if (isNecromancerLightning(lightning))
            evt.setCancelled(true);
    }

    @EventHandler
    private void onLightningDamage(EntityDamageByEntityEvent evt) {
        if (evt.getCause() != DamageCause.LIGHTNING)
            return;
        if (!(evt.getEntity() instanceof Raider))
            return;
        if (isNecromancerLightning((LightningStrike) evt.getDamager()))
            evt.setCancelled(true);
    }

    @EventHandler
    private void onLightningSetFire(EntityCombustByEntityEvent evt) {
        if (!(evt.getCombuster() instanceof LightningStrike))
            return;
        if (!isNecromancerLightning((LightningStrike) evt.getCombuster()))
            return;
        if (evt.getEntity() instanceof Raider)
            evt.setCancelled(true);
    }

    private ItemStack getDefaultBanner() {
        ItemStack banner = new ItemStack(Material.GRAY_BANNER);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        addPattern(meta, DyeColor.YELLOW, "RHOMBUS_MIDDLE", "RHOMBUS");
        addPattern(meta, DyeColor.GRAY, "STRIPE_DOWNLEFT");
        addPattern(meta, DyeColor.YELLOW, "STRIPE_MIDDLE");
        addPattern(meta, DyeColor.BLACK, "SKULL");
        addPattern(meta, DyeColor.WHITE, "SKULL");
        addPattern(meta, DyeColor.BLACK, "BORDER");
        addItemFlag(meta, "HIDE_POTION_EFFECTS", "HIDE_ADDITIONAL_TOOLTIP");
        meta.setDisplayName(ChatColor.GOLD.toString() + ChatColor.ITALIC + "Necromancer Banner");
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

    private boolean isNecromancer(Spellcaster entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        return pdc.has(getNecromancerNamespacedKey(), PersistentDataType.BYTE);
    }

    private boolean isNecromancerLightning(LightningStrike lightning) {
        PersistentDataContainer pdc = lightning.getPersistentDataContainer();
        return pdc.has(getNecromancerLightningNamespacedKey(), PersistentDataType.BYTE);
    }

    private NamespacedKey getNecromancerNamespacedKey() {
        return new NamespacedKey(JavaPlugin.getPlugin(LargeRaids.class), "necromancer");
    }

    private NamespacedKey getNecromancerLightningNamespacedKey() {
        return new NamespacedKey(JavaPlugin.getPlugin(LargeRaids.class), "necromancer_lightning");
    }

}
