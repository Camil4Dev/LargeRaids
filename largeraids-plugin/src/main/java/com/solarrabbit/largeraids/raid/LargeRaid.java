package com.solarrabbit.largeraids.raid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import com.solarrabbit.largeraids.LargeRaids;
import com.solarrabbit.largeraids.config.RaidConfig;
import com.solarrabbit.largeraids.config.RewardsConfig;
import com.solarrabbit.largeraids.raid.mob.RiderRaider;
import com.solarrabbit.largeraids.util.RaidUtil;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Raid;
import org.bukkit.Raid.RaidStatus;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Raider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class LargeRaid {
    private static final int RADIUS = 96;
    private static final int VANILLA_RAID_OMEN_LEVEL = 2;
    private static final int INVULNERABLE_TICKS = 20;
    private final RaidConfig raidConfig;
    private final RewardsConfig rewardsConfig;
    private final int maxTotalWaves;
    private final Location startLoc;
    private final Map<UUID, Integer> playerKills;
    private final Map<UUID, Double> playerDamage;
    private int totalWaves;
    private int omenLevel;
    private final Raid currentRaid;
    private int currentWave;

    LargeRaid(RaidConfig raidConfig, RewardsConfig rewardsConfig, Raid raid) {
        this.raidConfig = raidConfig;
        this.rewardsConfig = rewardsConfig;
        this.currentRaid = raid;
        this.startLoc = raid.getLocation();
        this.maxTotalWaves = raidConfig.getMaximumWaves();
        this.currentWave = 0;
        this.playerKills = new HashMap<>();
        this.playerDamage = new HashMap<>();
        this.omenLevel = Math.min(this.maxTotalWaves, raid.getBadOmenLevel());
        this.totalWaves = Math.max(5, this.omenLevel);
    }

    void onWaveSpawn() {
        currentWave++;
        broadcastWave();
        Sound sound = raidConfig.getSounds().getSummonSound();
        if (sound != null)
            playSoundToPlayersInRadius(sound);
        spawnWave();
    }

    void spawnWave() {
        Location loc = getWaveSpawnLocation();
        if (loc == null)
            return;

        List<Raider> newRaiders = new ArrayList<>();
        for (Map.Entry<Function<Location, ? extends com.solarrabbit.largeraids.raid.mob.Raider>, Integer> kv : raidConfig
                .getRaiders().getWaveMobs(this.currentWave).entrySet()) {
            for (int i = 0; i < kv.getValue(); i++) {
                com.solarrabbit.largeraids.raid.mob.Raider entity = kv.getKey().apply(loc);
                Raider bukkitEntity = (Raider) entity.getBukkitEntity();
                bukkitEntity.setInvulnerable(true);
                newRaiders.add(bukkitEntity);
                if (entity instanceof RiderRaider) {
                    Raider ravager = (Raider) ((RiderRaider) entity).getVehicle();
                    if (ravager != null) {
                        ravager.setInvulnerable(true);
                        newRaiders.add(ravager);
                    }
                }
            }
        }
        Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(LargeRaids.class), () -> {
            for (Raider raider : newRaiders)
                raider.setInvulnerable(false);
        }, INVULNERABLE_TICKS);
    }

    public void skipWave() {
        if (isLastWave())
            return;
        else if (!isLoading())
            for (Raider raider : currentRaid.getRaiders())
                raider.remove();
    }

    public void stopRaid() {
        for (Raider raider : currentRaid.getRaiders())
            raider.remove();
        RaidUtil.stopRaid(currentRaid);
    }

    void announceVictory() {
        Sound sound = raidConfig.getSounds().getVictorySound();
        if (sound != null)
            playSoundToPlayersInRadius(sound);
        for (UUID uuid : playerDamage.keySet())
            Optional.ofNullable(Bukkit.getPlayer(uuid)).filter(this::shouldAwardPlayer).ifPresent(this::awardPlayer);
    }

    void announceDefeat() {
        Sound sound = raidConfig.getSounds().getDefeatSound();
        if (sound != null)
            playSoundToPlayersInRadius(sound);
    }

    public Location getCenter() {
        return currentRaid == null ? startLoc : currentRaid.getLocation();
    }

    public Raid getRaid() {
        return currentRaid;
    }

    public int getBadOmenLevel() {
        return omenLevel;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public int getTotalWaves() {
        return totalWaves;
    }

    public boolean isLastWave() {
        return currentWave == totalWaves;
    }

    public boolean isActive() {
        return currentRaid != null && currentRaid.getStatus() == RaidStatus.ONGOING;
    }

    public boolean isLoading() {
        return currentRaid != null && currentRaid.getRaiders().isEmpty();
    }

    public int getTotalRaidersAlive() {
        try {
            return currentRaid == null ? 0 : currentRaid.getRaiders().size();
        } catch (ConcurrentModificationException evt) {
            return 0;
        }
    }

    void absorbOmenLevel(int level) {
        omenLevel = Math.min(this.maxTotalWaves, this.omenLevel + level);
        totalWaves = Math.max(5, omenLevel);
        RaidUtil.setBadOmenLevel(currentRaid, omenLevel);
    }

    public Set<Player> getPlayersInRadius() {
        Collection<Entity> collection = getCenter().getWorld().getNearbyEntities(getCenter(), RADIUS, RADIUS, RADIUS,
                entity -> entity instanceof Player
                        && getCenter().distanceSquared(entity.getLocation()) <= Math.pow(RADIUS, 2));
        Set<Player> set = new HashSet<>();
        collection.forEach(player -> set.add((Player) player));
        return set;
    }

    public Map<UUID, Integer> getPlayerKills() {
        return playerKills;
    }

    public boolean releaseOmen() {
        if (currentRaid.getBadOmenLevel() <= VANILLA_RAID_OMEN_LEVEL)
            return false;
        return RaidUtil.setBadOmenLevel(currentRaid, VANILLA_RAID_OMEN_LEVEL);
    }

    public boolean isSimilar(Raid raid) {
        Objects.requireNonNull(currentRaid);
        return this.currentRaid.getLocation().equals(raid.getLocation());
    }

    public boolean containsRaider(Raider raider) {
        return currentRaid != null && currentRaid.getRaiders().contains(raider);
    }

    public boolean isInRadius(Location location) {
        if (location == null || location.getWorld() == null)
            return false;
        Location center = getCenter();
        if (center.getWorld() == null || !center.getWorld().equals(location.getWorld()))
            return false;
        return center.distanceSquared(location) <= Math.pow(RADIUS, 2);
    }

    void incrementPlayerKill(Player player) {
        playerKills.merge(player.getUniqueId(), 1, Integer::sum);
    }

    void incrementPlayerDamage(Player player, double damage) {
        playerDamage.merge(player.getUniqueId(), damage, Double::sum);
    }

    private void broadcastWave() {
        for (Player player : getPlayersInRadius()) {
            if (raidConfig.isTitleEnabled()) {
                String defaultStr = raidConfig.getDefaultWaveTitle(currentWave);
                String finalStr = raidConfig.getFinalWaveTitle();
                player.sendTitle(isLastWave() ? finalStr : defaultStr, null, 10, 70, 20);
            }
            if (raidConfig.isMessageEnabled()) {
                String defaultStr = raidConfig.getDefaultWaveMessage(currentWave);
                String finalStr = raidConfig.getFinalWaveMessage();
                player.sendMessage(isLastWave() ? finalStr : defaultStr);
            }
        }
    }

    private boolean shouldAwardPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        boolean hasMinKills = Optional.ofNullable(playerKills.get(uuid)).orElse(0).intValue() >= rewardsConfig
                .getMinRaiderKills();
        boolean hasMinDamage = Optional.ofNullable(playerDamage.get(uuid)).orElse(0.0).doubleValue() >= rewardsConfig
                .getMinDamageDeal();
        return hasMinKills && hasMinDamage;
    }

    private void awardPlayer(Player player) {
        int level = Math.min(rewardsConfig.getHeroLevel(), omenLevel);
        int duration = rewardsConfig.getHeroDuration() * 60 * 20;
        if (level > 0)
            player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, duration, level - 1));

        player.sendMessage(rewardsConfig.getMessage());
        player.getInventory().addItem(rewardsConfig.getItems())
                .forEach((i, item) -> player.getWorld().dropItem(player.getLocation(), item));

        for (String command : rewardsConfig.getCommands())
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("<player>", player.getName())
                    .replace("<omen>", String.valueOf(getBadOmenLevel())));
    }

    private void playSoundToPlayersInRadius(Sound sound) {
        getPlayersInRadius().forEach(player -> player.playSound(player.getLocation(), sound, 50, 1));
    }

    private Location getWaveSpawnLocation() {
        List<Raider> list = this.currentRaid.getRaiders();
        return list.isEmpty() ? null : list.get(0).getLocation();
    }

    private int getDefaultWaveNumber(World world) {
        Difficulty difficulty = world.getDifficulty();
        switch (difficulty) {
            case EASY:
                return 3;
            case NORMAL:
                return 5;
            case HARD:
                return 7;
            default:
                return 0;
        }
    }
}
