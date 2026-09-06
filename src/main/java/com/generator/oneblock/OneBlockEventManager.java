package com.generator.oneblock;

import com.generator.GeneratorPlugin;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OneBlockEventManager implements Listener {

    private final GeneratorPlugin plugin;
    private final OneBlockManager oneBlockManager;
    private final File eventFile;
    private final Map<String, SeasonalEvent> events = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Integer>> eventProgress = new ConcurrentHashMap<>();
    private SeasonalEvent activeEvent = null;

    public OneBlockEventManager(GeneratorPlugin plugin, OneBlockManager oneBlockManager) {
        this.plugin = plugin;
        this.oneBlockManager = oneBlockManager;
        this.eventFile = new File(plugin.getDataFolder(), "oneblock_events.yml");
        loadEvents();
        checkActiveEvent();
    }

    private void loadEvents() {
        events.clear();
        if (!eventFile.exists()) {
            createDefaultEvents();
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(eventFile);
        ConfigurationSection eventSection = config.getConfigurationSection("events");
        if (eventSection == null) {
            createDefaultEvents();
            return;
        }

        for (String key : eventSection.getKeys(false)) {
            ConfigurationSection cs = eventSection.getConfigurationSection(key);
            if (cs == null) continue;

            SeasonalEvent event = new SeasonalEvent();
            event.id = key;
            event.displayName = cs.getString("display-name", key);
            event.description = cs.getString("description", "");
            event.material = Material.matchMaterial(cs.getString("material", "PUMPKIN"));
            event.bonusMultiplier = cs.getDouble("bonus-multiplier", 2.0);
            event.bonusCoins = cs.getInt("bonus-coins", 0);
            event.startMonth = cs.getInt("start-month", 1);
            event.startDay = cs.getInt("start-day", 1);
            event.endMonth = cs.getInt("end-month", 1);
            event.endDay = cs.getInt("end-day", 31);
            event.blockTarget = cs.getString("block-target", "ANY");
            event.requiredBlocks = cs.getInt("required-blocks", 100);
            event.rewardItem = cs.getString("reward-item", "");
            event.rewardAmount = cs.getInt("reward-amount", 1);

            if (event.material != null) {
                events.put(key, event);
            }
        }

        if (events.isEmpty()) {
            createDefaultEvents();
        }
    }

    private void createDefaultEvents() {
        events.clear();

        addEvent("halloween", "Halloween Spooktacular", "Break blocks for spooky rewards!",
                "PUMPKIN", 2.0, 100, 10, 31, 11, 7, "ANY", 200, "PLAYER_HEAD", 5);
        addEvent("winter", "Winter Wonderland", "Mine through the snow for festive rewards!",
                "SNOW_BLOCK", 2.5, 150, 12, 20, 1, 5, "ANY", 300, "GOLDEN_APPLE", 10);
        addEvent("spring", "Spring Bloom", "Break blocks to grow your garden!",
                "GRASS_BLOCK", 1.5, 50, 3, 1, 5, 31, "ANY", 150, "FLOWER", 16);
        addEvent("summer", "Summer Splash", "Dig through the sand for treasure!",
                "SAND", 2.0, 75, 6, 1, 8, 31, "ANY", 250, "TRIDENT", 1);
        addEvent("newyear", "New Year Celebration", "Ring in the new year with mining!",
                "GLOWSTONE", 3.0, 200, 12, 28, 1, 7, "ANY", 500, "NETHERITE_INGOT", 4);

        saveEvents();
    }

    private void addEvent(String id, String name, String desc, String material,
                          double multiplier, int bonusCoins, int startMonth, int startDay,
                          int endMonth, int endDay, String target, int required,
                          String rewardItem, int rewardAmount) {
        SeasonalEvent event = new SeasonalEvent();
        event.id = id;
        event.displayName = name;
        event.description = desc;
        event.material = Material.matchMaterial(material);
        event.bonusMultiplier = multiplier;
        event.bonusCoins = bonusCoins;
        event.startMonth = startMonth;
        event.startDay = startDay;
        event.endMonth = endMonth;
        event.endDay = endDay;
        event.blockTarget = target;
        event.requiredBlocks = required;
        event.rewardItem = rewardItem;
        event.rewardAmount = rewardAmount;
        events.put(id, event);
    }

    public void saveEvents() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, SeasonalEvent> entry : events.entrySet()) {
            SeasonalEvent event = entry.getValue();
            String path = "events." + event.id;
            config.set(path + ".display-name", event.displayName);
            config.set(path + ".description", event.description);
            config.set(path + ".material", event.material != null ? event.material.name() : "PUMPKIN");
            config.set(path + ".bonus-multiplier", event.bonusMultiplier);
            config.set(path + ".bonus-coins", event.bonusCoins);
            config.set(path + ".start-month", event.startMonth);
            config.set(path + ".start-day", event.startDay);
            config.set(path + ".end-month", event.endMonth);
            config.set(path + ".end-day", event.endDay);
            config.set(path + ".block-target", event.blockTarget);
            config.set(path + ".required-blocks", event.requiredBlocks);
            config.set(path + ".reward-item", event.rewardItem);
            config.set(path + ".reward-amount", event.rewardAmount);
        }

        try {
            config.save(eventFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save oneblock_events.yml: " + e.getMessage());
        }
    }

    public void checkActiveEvent() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();

        for (SeasonalEvent event : events.values()) {
            if (isDateInRange(month, day, event.startMonth, event.startDay, event.endMonth, event.endDay)) {
                if (activeEvent == null || !activeEvent.id.equals(event.id)) {
                    activeEvent = event;
                    Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "EVENT: " + event.displayName + " is now active!");
                    Bukkit.broadcastMessage(ChatColor.YELLOW + event.description);
                }
                return;
            }
        }

        if (activeEvent != null) {
            Bukkit.broadcastMessage(ChatColor.RED + "Event ended: " + activeEvent.displayName);
            activeEvent = null;
        }
    }

    private boolean isDateInRange(int month, int day, int startMonth, int startDay, int endMonth, int endDay) {
        if (startMonth == endMonth) {
            return month == startMonth && day >= startDay && day <= endDay;
        } else {
            return (month == startMonth && day >= startDay) || (month == endMonth && day <= endDay) ||
                    (month > startMonth && month < endMonth);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!oneBlockManager.isOneBlockWorld(player.getWorld().getName())) return;
        if (activeEvent == null) return;

        if (!activeEvent.blockTarget.equals("ANY")) {
            String blockType = event.getBlock().getType().name();
            if (!blockType.equals(activeEvent.blockTarget)) return;
        }

        UUID uuid = player.getUniqueId();
        Map<String, Integer> progress = eventProgress.computeIfAbsent(uuid, k -> new HashMap<>());
        int current = progress.getOrDefault(activeEvent.id, 0) + 1;
        progress.put(activeEvent.id, current);

        if (current % 50 == 0) {
            player.sendMessage(ChatColor.AQUA + "Event Progress: " + current + "/" + activeEvent.requiredBlocks);
        }

        if (current >= activeEvent.requiredBlocks) {
            completeEvent(player);
        }
    }

    private void completeEvent(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, Integer> progress = eventProgress.computeIfAbsent(uuid, k -> new HashMap<>());
        progress.put(activeEvent.id, 0);

        int bonusCoins = activeEvent.bonusCoins;
        plugin.getCoinManager().addCoins(player, bonusCoins);
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "EVENT COMPLETE: " + activeEvent.displayName + "!");
        player.sendMessage(ChatColor.YELLOW + "+" + bonusCoins + " bonus coins!");

        if (!activeEvent.rewardItem.isEmpty()) {
            Material rewardMat = Material.matchMaterial(activeEvent.rewardItem);
            if (rewardMat != null) {
                ItemStack reward = new ItemStack(rewardMat, activeEvent.rewardAmount);
                player.getInventory().addItem(reward);
                player.sendMessage(ChatColor.AQUA + "Event Reward: " + activeEvent.rewardAmount + " " + rewardMat.name());
            }
        }

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
    }

    public SeasonalEvent getActiveEvent() {
        return activeEvent;
    }

    public Map<String, SeasonalEvent> getAllEvents() {
        return Collections.unmodifiableMap(events);
    }

    public int getEventProgress(UUID uuid, String eventId) {
        return eventProgress.getOrDefault(uuid, Collections.emptyMap()).getOrDefault(eventId, 0);
    }

    public double getActiveMultiplier() {
        return activeEvent != null ? activeEvent.bonusMultiplier : 1.0;
    }

    public void forceEvent(String eventId) {
        SeasonalEvent event = events.get(eventId);
        if (event != null) {
            activeEvent = event;
            Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "EVENT FORCED: " + event.displayName + "!");
        }
    }

    public void clearEvent() {
        if (activeEvent != null) {
            Bukkit.broadcastMessage(ChatColor.RED + "Event ended: " + activeEvent.displayName);
            activeEvent = null;
        }
    }

    public static class SeasonalEvent {
        public String id;
        public String displayName;
        public String description;
        public Material material;
        public double bonusMultiplier;
        public int bonusCoins;
        public int startMonth;
        public int startDay;
        public int endMonth;
        public int endDay;
        public String blockTarget;
        public int requiredBlocks;
        public String rewardItem;
        public int rewardAmount;
    }
}
