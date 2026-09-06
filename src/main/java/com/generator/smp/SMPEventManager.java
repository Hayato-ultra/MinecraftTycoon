package com.generator.smp;

import com.generator.GeneratorPlugin;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SMPEventManager implements Listener {

    private final GeneratorPlugin plugin;
    private final Map<String, SMPEvent> activeEvents = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Integer>> playerProgress = new ConcurrentHashMap<>();
    private final Map<String, Long> eventCooldowns = new ConcurrentHashMap<>();

    private static final long EVENT_COOLDOWN = 3600000;

    public SMPEventManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean startEvent(String type) {
        SMPEvent event = createEvent(type);
        if (event == null) return false;

        activeEvents.put(event.id, event);
        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "SERVER EVENT: " + event.displayName + "!");
        Bukkit.broadcastMessage(ChatColor.YELLOW + event.description);
        Bukkit.broadcastMessage(ChatColor.AQUA + "Duration: " + (event.duration / 60000) + " minutes");

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            endEvent(event.id);
        }, event.duration / 50);

        return true;
    }

    private SMPEvent createEvent(String type) {
        switch (type.toLowerCase()) {
            case "treasure" -> {
                return createTreasureHunt();
            }
            case "mob" -> {
                return createMobRush();
            }
            case "mining" -> {
                return createMiningFrenzy();
            }
            case "fishing" -> {
                return createFishingBonanza();
            }
            case "builder" -> {
                return createBuildingContest();
            }
            default -> {
                return createTreasureHunt();
            }
        }
    }

    private SMPEvent createTreasureHunt() {
        SMPEvent event = new SMPEvent();
        event.id = "treasure_" + System.currentTimeMillis();
        event.displayName = "Treasure Hunt";
        event.description = "Find hidden treasure chests around the world!";
        event.type = "treasure";
        event.duration = 1800000;
        event.rewardCoins = 500;
        event.requiredAmount = 3;

        List<Location> treasureLocations = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location loc = player.getWorld().getSpawnLocation().clone();
            loc.add(Math.random() * 500 - 250, 0, Math.random() * 500 - 250);
            loc.setY(loc.getWorld().getHighestBlockYAt(loc) + 1);

            loc.getBlock().setType(Material.CHEST);
            Chest chest = (Chest) loc.getBlock().getState();
            chest.getInventory().addItem(new ItemStack(Material.DIAMOND, (int) (Math.random() * 5 + 1)));
            chest.getInventory().addItem(new ItemStack(Material.EMERALD, (int) (Math.random() * 10 + 1)));
            chest.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, (int) (Math.random() * 16 + 1)));
            chest.update();

            treasureLocations.add(loc);
        }

        event.treasureLocations = treasureLocations;
        return event;
    }

    private SMPEvent createMobRush() {
        SMPEvent event = new SMPEvent();
        event.id = "mob_" + System.currentTimeMillis();
        event.displayName = "Mob Rush";
        event.description = "Kill mobs for bonus rewards!";
        event.type = "mob";
        event.duration = 1200000;
        event.rewardCoins = 300;
        event.requiredAmount = 50;
        return event;
    }

    private SMPEvent createMiningFrenzy() {
        SMPEvent event = new SMPEvent();
        event.id = "mining_" + System.currentTimeMillis();
        event.displayName = "Mining Frenzy";
        event.description = "Mine blocks for bonus rewards!";
        event.type = "mining";
        event.duration = 1200000;
        event.rewardCoins = 200;
        event.requiredAmount = 200;
        return event;
    }

    private SMPEvent createFishingBonanza() {
        SMPEvent event = new SMPEvent();
        event.id = "fishing_" + System.currentTimeMillis();
        event.displayName = "Fishing Bonanza";
        event.description = "Fish for rare treasures!";
        event.type = "fishing";
        event.duration = 1800000;
        event.rewardCoins = 400;
        event.requiredAmount = 20;
        return event;
    }

    private SMPEvent createBuildingContest() {
        SMPEvent event = new SMPEvent();
        event.id = "builder_" + System.currentTimeMillis();
        event.displayName = "Building Contest";
        event.description = "Build something amazing! Best build wins!";
        event.type = "builder";
        event.duration = 3600000;
        event.rewardCoins = 1000;
        event.requiredAmount = 1;
        return event;
    }

    public void endEvent(String eventId) {
        SMPEvent event = activeEvents.remove(eventId);
        if (event == null) return;

        UUID winner = null;
        int highestProgress = 0;

        for (Map.Entry<UUID, Map<String, Integer>> entry : playerProgress.entrySet()) {
            int progress = entry.getValue().getOrDefault(eventId, 0);
            if (progress > highestProgress) {
                highestProgress = progress;
                winner = entry.getKey();
            }
        }

        if (winner != null) {
            Player winnerPlayer = Bukkit.getPlayer(winner);
            if (winnerPlayer != null) {
                plugin.getCoinManager().addCoins(winnerPlayer, event.rewardCoins);
                winnerPlayer.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You won " + event.displayName + "! +" + event.rewardCoins + " coins!");
            }
            Bukkit.broadcastMessage(ChatColor.GOLD + "Event ended! Winner: " + Bukkit.getOfflinePlayer(winner).getName());
        } else {
            Bukkit.broadcastMessage(ChatColor.RED + "Event ended: " + event.displayName);
        }

        playerProgress.clear();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        for (SMPEvent smpEvent : activeEvents.values()) {
            if (smpEvent.type.equals("mob")) {
                UUID uuid = killer.getUniqueId();
                Map<String, Integer> progress = playerProgress.computeIfAbsent(uuid, k -> new HashMap<>());
                int current = progress.getOrDefault(smpEvent.id, 0) + 1;
                progress.put(smpEvent.id, current);

                if (current % 10 == 0) {
                    killer.sendMessage(ChatColor.AQUA + "Event Progress: " + current + "/" + smpEvent.requiredAmount);
                }

                if (current >= smpEvent.requiredAmount) {
                    plugin.getCoinManager().addCoins(killer, 50);
                    progress.put(smpEvent.id, 0);
                    killer.sendMessage(ChatColor.GREEN + "+50 coins for killing mobs!");
                }
            }
        }
    }

    public boolean isEventActive(String type) {
        return activeEvents.values().stream().anyMatch(e -> e.type.equals(type));
    }

    public SMPEvent getActiveEvent(String type) {
        return activeEvents.values().stream().filter(e -> e.type.equals(type)).findFirst().orElse(null);
    }

    public Collection<SMPEvent> getAllActiveEvents() {
        return activeEvents.values();
    }

    public int getPlayerProgress(UUID uuid, String eventId) {
        return playerProgress.getOrDefault(uuid, Collections.emptyMap()).getOrDefault(eventId, 0);
    }

    public boolean canStartEvent(String type) {
        String cooldownKey = type + "_cooldown";
        Long lastStart = eventCooldowns.get(cooldownKey);
        if (lastStart != null && (System.currentTimeMillis() - lastStart) < EVENT_COOLDOWN) {
            return false;
        }
        eventCooldowns.put(cooldownKey, System.currentTimeMillis());
        return true;
    }

    public static class SMPEvent {
        public String id;
        public String displayName;
        public String description;
        public String type;
        public long duration;
        public int rewardCoins;
        public int requiredAmount;
        public List<Location> treasureLocations = new ArrayList<>();
    }
}
