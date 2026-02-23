package com.solarrabbit.largeraids.raid;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.solarrabbit.largeraids.LargeRaids;
import com.solarrabbit.largeraids.event.LargeRaidExtendEvent;
import com.solarrabbit.largeraids.event.LargeRaidTriggerEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Raid;
import org.bukkit.Raid.RaidStatus;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Raider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.event.raid.RaidSpawnWaveEvent;
import org.bukkit.event.raid.RaidStopEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.projectiles.ProjectileSource;

public class RaidManager implements Listener {
    public final Set<LargeRaid> currentRaids = new HashSet<>();
    private final LargeRaids plugin;
    private boolean isIdle;

    public RaidManager(LargeRaids plugin) {
        this.plugin = plugin;
        isIdle = false;
    }

    public boolean isIdle() {
        return isIdle;
    }

    /**
     * Idles the listener, mainly used to signify that any {@link RaidTriggerEvent}
     * fired after part of a {@link LargeRaid}.
     */
    public void setIdle() {
        isIdle = true;
    }

    /**
     * Re-activates the listener, mainly used to signify that any
     * {@link RaidTriggerEvent} fired after are vanilla.
     */
    public void setActive() {
        isIdle = false;
    }

    public int getNumOfRegisteredRaids() {
        return currentRaids.size();
    }

    @EventHandler
    private void onSpawn(RaidSpawnWaveEvent evt) {
        // TODO Confirm to prevent ConcurrentModificationException
        Bukkit.getScheduler().runTask(plugin, () -> getLargeRaid(evt.getRaid()).ifPresent(LargeRaid::onWaveSpawn));
    }

    /**
     * Disables normal raid if enabled in configurations.
     *
     * @param evt raid triggering event
     */
    @EventHandler
    private void onNormalRaidTrigger(RaidTriggerEvent evt) {
        if (evt.getRaid().getBadOmenLevel() != 0) // Raid is getting extended
            return;
        if (isIdle()) // LargeRaid triggering
            return;
        if (!plugin.getTriggerConfig().canNormalRaid())
            evt.setCancelled(true);
        if (!evt.isCancelled())
            registerRaid(evt.getRaid());
    }

    @EventHandler
    private void onFinish(RaidFinishEvent evt) {
        Raid raid = evt.getRaid();
        getLargeRaid(raid).ifPresent(largeRaid -> {
            RaidStatus status = raid.getStatus();
            if (status == RaidStatus.VICTORY)
                largeRaid.announceVictory();
            else if (status == RaidStatus.LOSS)
                largeRaid.announceDefeat();
        });
    }

    @EventHandler
    private void onRaidStop(RaidStopEvent evt) {
        getLargeRaid(evt.getRaid()).ifPresent(largeRaid -> currentRaids.remove(largeRaid));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onDamage(EntityDamageByEntityEvent evt) {
        if (evt.isCancelled())
            return;
        Entity killed = evt.getEntity();
        if (!(killed instanceof Raider))
            return;
        Raider raider = (Raider) killed;
        Entity attacker = evt.getDamager();
        Player damager;
        switch (evt.getCause()) {
            case ENTITY_ATTACK:
            case ENTITY_SWEEP_ATTACK:
                if (!(attacker instanceof Player))
                    return;
                damager = (Player) attacker;
                break;
            case PROJECTILE:
                Projectile projectile = (Projectile) attacker;
                ProjectileSource source = projectile.getShooter();
                if (!(source instanceof Player))
                    return;
                damager = (Player) source;
                break;
            default:
                return;
        }

        Optional<LargeRaid> lr = getLargeRaid(raider);
        lr.ifPresent(r -> {
            r.incrementPlayerDamage(damager, Math.min(raider.getHealth(), evt.getFinalDamage()));
            if (raider.getHealth() - evt.getFinalDamage() <= 0)
                r.incrementPlayerKill(damager);
        });
    }

    public void init() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 0, 1);
    }

    private void tick() {
        currentRaids.removeIf(raid -> !raid.isActive() && raid.getRaid().getStatus() == RaidStatus.STOPPED);
    }

    public Optional<LargeRaid> getLargeRaid(Location location) {
        return currentRaids.stream().filter(raid -> raid.isInRadius(location)).findFirst();
    }

    public Optional<LargeRaid> getLargeRaid(Raid raid) {
        return currentRaids.stream().filter(largeRaid -> largeRaid.isSimilar(raid)).findFirst();
    }

    public Optional<LargeRaid> getLargeRaid(Raider raider) {
        return currentRaids.stream().filter(largeRaid -> largeRaid.containsRaider(raider)).findFirst();
    }

    public Optional<Raid> getRaid(Location location) {
        return getLargeRaid(location).map(LargeRaid::getRaid);
    }

    /**
     * Creates a raid at a given location with the given omen level. This method
     * will fail silently if there is already an ongoing raid in this vacinity.
     *
     * @param location  to trigger the raid
     * @param omenLevel for the raid start with
     */
    public void createRaid(Location location, int omenLevel) {
        // API-only build cannot create raids; handled by Trigger.
    }

    /**
     * Extends the given raid by absorbing the given omen level.
     *
     * @param raid      to absorb omen
     * @param omenLevel levels to absorb
     */
    public void extendRaid(LargeRaid raid, int omenLevel) {
        int oldLevel = raid.getBadOmenLevel();
        raid.absorbOmenLevel(omenLevel);
        int newLevel = raid.getBadOmenLevel();
        if (newLevel != oldLevel)
            Bukkit.getPluginManager().callEvent(new LargeRaidExtendEvent(raid, oldLevel, newLevel));
    }

    public boolean canCreateRaids() {
        return false;
    }

    private void registerRaid(Raid raid) {
        if (getLargeRaid(raid).isPresent())
            return;
        LargeRaid largeRaid = new LargeRaid(plugin.getRaidConfig(), plugin.getRewardsConfig(), raid);
        LargeRaidTriggerEvent evt = new LargeRaidTriggerEvent(largeRaid);
        Bukkit.getPluginManager().callEvent(evt);
        if (!evt.isCancelled())
            currentRaids.add(largeRaid);
    }

}