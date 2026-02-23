package com.solarrabbit.largeraids.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.solarrabbit.largeraids.raid.LargeRaid;
import com.solarrabbit.largeraids.raid.RaidManager;
import com.solarrabbit.largeraids.util.BukkitEnumUtil;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Raider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class BossBarCreator implements Listener {
    private static final Map<Raider, BossBar> RAID_BOSSES = new HashMap<>();
    private static final int TICK_PERIOD = 10;
    private final RaidManager raidManager;

    public BossBarCreator(RaidManager manager) {
        raidManager = manager;
    }

    public static BossBar createRaidBossBar(Raider boss) {
        BossBar bar = Bukkit.createBossBar(boss.getName(), BarColor.PURPLE, BarStyle.SOLID, BarFlag.DARKEN_SKY);
        registerEntity(boss, bar);
        return bar;
    }

    @EventHandler
    private void onBossDeath(EntityDeathEvent evt) {
        Entity entity = evt.getEntity();
        if (!(entity instanceof Raider))
            return;
        unregisterEntity((Raider) evt.getEntity());
    }

    public void init(JavaPlugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0, TICK_PERIOD);
    }

    private void tick() {
        Raider[] bosses = RAID_BOSSES.keySet().toArray(Raider[]::new);
        for (Raider boss : bosses) {
            updateBossBarProgress(boss);
            updateBossBarVisibility(boss);
        }
    }

    private void updateBossBarProgress(Raider boss) {
        BossBar bar = RAID_BOSSES.get(boss);
        Attribute maxHealth = BukkitEnumUtil.attribute("GENERIC_MAX_HEALTH", "MAX_HEALTH");
        double max = maxHealth == null || boss.getAttribute(maxHealth) == null
                ? boss.getHealth()
                : boss.getAttribute(maxHealth).getValue();
        double progress = max == 0 ? 0 : boss.getHealth() / max;
        progress = Math.min(1, Math.max(0, progress));
        bar.setProgress(progress);
    }

    private void updateBossBarVisibility(Raider boss) {
        BossBar bar = RAID_BOSSES.get(boss);
        bar.removeAll();
        Optional<LargeRaid> lr = raidManager.getLargeRaid(boss);
        if (boss.isDead()) {
            unregisterEntity(boss);
        } else if (lr.isPresent()) {
            Set<Player> players = lr.get().getPlayersInRadius();
            for (Player player : players)
                bar.addPlayer(player);
        }
    }

    private static void registerEntity(Raider boss, BossBar bar) {
        RAID_BOSSES.put(boss, bar);
    }

    private void unregisterEntity(Raider boss) {
        BossBar bar = RAID_BOSSES.remove(boss);
        if (bar != null)
            bar.removeAll();
    }

}
