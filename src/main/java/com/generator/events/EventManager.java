package com.generator.events;

import com.generator.GeneratorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.YamlConfiguration;

public class EventManager implements Listener {

    private final GeneratorPlugin plugin;
    private final File eventFile;
    private final Map<String, ServerEvent> activeEvents = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Integer>> playerProgress = new ConcurrentHashMap<>();
    private final Map<String, Long> eventCooldowns = new ConcurrentHashMap<>();
    private final Set<Inventory> managedInventories = Collections.synchronizedSet(new HashSet<>());
    private final Map<Inventory, String> inventoryTitles = Collections.synchronizedMap(new HashMap<>());

    private static final long EVENT_COOLDOWN = 1800000;

    public EventManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
        this.eventFile = new File(plugin.getDataFolder(), "events.yml");
        loadEvents();
    }

    private void loadEvents() {
        activeEvents.clear();
        if (!eventFile.exists()) return;

        org.bukkit.configuration.ConfigurationSection section = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(eventFile).getConfigurationSection("active-events");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            org.bukkit.configuration.ConfigurationSection cs = section.getConfigurationSection(key);
            if (cs == null) continue;

            ServerEvent event = new ServerEvent();
            event.id = key;
            event.name = cs.getString("name", key);
            event.type = cs.getString("type", "generic");
            event.description = cs.getString("description", "");
            event.startTime = cs.getLong("start-time", 0);
            event.endTime = cs.getLong("end-time", 0);
            event.rewardCoins = cs.getInt("reward-coins", 100);
            event.requiredAmount = cs.getInt("required-amount", 100);
            event.status = cs.getString("status", "active");

            if (event.status.equals("active") && System.currentTimeMillis() < event.endTime) {
                activeEvents.put(event.id, event);
            }
        }
    }

    public void saveEvents() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, ServerEvent> entry : activeEvents.entrySet()) {
            ServerEvent event = entry.getValue();
            String path = "active-events." + event.id;
            config.set(path + ".name", event.name);
            config.set(path + ".type", event.type);
            config.set(path + ".description", event.description);
            config.set(path + ".start-time", event.startTime);
            config.set(path + ".end-time", event.endTime);
            config.set(path + ".reward-coins", event.rewardCoins);
            config.set(path + ".required-amount", event.requiredAmount);
            config.set(path + ".status", event.status);
        }

        try {
            config.save(eventFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save events.yml: " + e.getMessage());
        }
    }

    public ServerEvent createEvent(String name, String type, long duration, int rewardCoins, int requiredAmount) {
        String id = name.toLowerCase().replace(" ", "_") + "_" + System.currentTimeMillis();

        ServerEvent event = new ServerEvent();
        event.id = id;
        event.name = name;
        event.type = type;
        event.description = getDescription(type);
        event.startTime = System.currentTimeMillis();
        event.endTime = System.currentTimeMillis() + duration;
        event.rewardCoins = rewardCoins;
        event.requiredAmount = requiredAmount;
        event.status = "active";

        activeEvents.put(id, event);
        saveEvents();

        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "SERVER EVENT: " + name + "!");
        Bukkit.broadcastMessage(ChatColor.YELLOW + event.description);
        Bukkit.broadcastMessage(ChatColor.AQUA + "Duration: " + (duration / 60000) + " minutes | Reward: " + rewardCoins + " coins");

        return event;
    }

    private String getDescription(String type) {
        return switch (type.toLowerCase()) {
            case "mining" -> "Mine blocks for bonus rewards!";
            case "killing" -> "Kill mobs for bonus rewards!";
            case "farming" -> "Harvest crops for bonus rewards!";
            case "fishing" -> "Fish for rare treasures!";
            case "exploring" -> "Explore new areas for rewards!";
            case "building" -> "Build structures for prizes!";
            default -> "Complete objectives for rewards!";
        };
    }

    public void endEvent(String eventId) {
        ServerEvent event = activeEvents.remove(eventId);
        if (event == null) return;

        event.status = "ended";
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
                winnerPlayer.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You won " + event.name + "! +" + event.rewardCoins + " coins!");
            }
            Bukkit.broadcastMessage(ChatColor.GOLD + "Event ended! Winner: " + Bukkit.getOfflinePlayer(winner).getName());
        } else {
            Bukkit.broadcastMessage(ChatColor.RED + "Event ended: " + event.name);
        }

        playerProgress.clear();
        saveEvents();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        for (ServerEvent sEvent : activeEvents.values()) {
            if (sEvent.type.equals("mining")) {
                updateProgress(player, sEvent.id, 1);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        for (ServerEvent sEvent : activeEvents.values()) {
            if (sEvent.type.equals("killing")) {
                updateProgress(killer, sEvent.id, 1);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        for (ServerEvent sEvent : activeEvents.values()) {
            int progress = getPlayerProgress(player.getUniqueId(), sEvent.id);
            if (progress > 0) {
                player.sendMessage(ChatColor.AQUA + "Active event: " + sEvent.name + " - Progress: " + progress + "/" + sEvent.requiredAmount);
            }
        }
    }

    private void updateProgress(Player player, String eventId, int amount) {
        Map<String, Integer> progress = playerProgress.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        int current = progress.getOrDefault(eventId, 0) + amount;
        progress.put(eventId, current);

        ServerEvent event = activeEvents.get(eventId);
        if (event != null && current % 25 == 0) {
            player.sendMessage(ChatColor.AQUA + "Event Progress: " + current + "/" + event.requiredAmount);
        }

        if (event != null && current >= event.requiredAmount) {
            plugin.getCoinManager().addCoins(player, event.rewardCoins / 10);
            progress.put(eventId, 0);
            player.sendMessage(ChatColor.GREEN + "+" + (event.rewardCoins / 10) + " coins for completing event objectives!");
        }
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

    public boolean isEventActive(String type) {
        return activeEvents.values().stream().anyMatch(e -> e.type.equals(type) && e.status.equals("active"));
    }

    public ServerEvent getActiveEvent(String type) {
        return activeEvents.values().stream()
                .filter(e -> e.type.equals(type) && e.status.equals("active"))
                .findFirst().orElse(null);
    }

    public Collection<ServerEvent> getAllActiveEvents() {
        return activeEvents.values();
    }

    public int getPlayerProgress(UUID uuid, String eventId) {
        return playerProgress.getOrDefault(uuid, Collections.emptyMap()).getOrDefault(eventId, 0);
    }

    public long getTimeRemaining(String eventId) {
        ServerEvent event = activeEvents.get(eventId);
        if (event == null) return 0;
        return Math.max(0, event.endTime - System.currentTimeMillis());
    }

    public void openEventGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "SERVER_EVENTS");
        managedInventories.add(gui);
        inventoryTitles.put(gui, "SERVER_EVENTS");

        int slot = 0;
        for (ServerEvent event : activeEvents.values()) {
            if (slot >= 26) break;

            Material mat = switch (event.type.toLowerCase()) {
                case "mining" -> Material.DIAMOND_PICKAXE;
                case "killing" -> Material.IRON_SWORD;
                case "farming" -> Material.WHEAT;
                case "fishing" -> Material.FISHING_ROD;
                case "exploring" -> Material.COMPASS;
                case "building" -> Material.BRICK;
                default -> Material.BEACON;
            };

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + event.name);
                long remaining = getTimeRemaining(event.id);
                long minutes = remaining / 60000;
                meta.setLore(Arrays.asList(
                        "",
                        "§7" + event.description,
                        "",
                        "§7Reward: §6" + event.rewardCoins + " coins",
                        "§7Time Left: §f" + minutes + " minutes",
                        "§7Status: §aActive"
                ));
                item.setItemMeta(meta);
            }
            gui.setItem(slot, item);
            slot++;
        }

        if (activeEvents.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "No active events");
                meta.setLore(Arrays.asList("", "§7Check back later!"));
                empty.setItemMeta(meta);
            }
            gui.setItem(13, empty);
        }

        gui.setItem(26, createItem(Material.BARRIER, ChatColor.RED + "Close"));
        player.openInventory(gui);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static class ServerEvent {
        public String id;
        public String name;
        public String type;
        public String description;
        public long startTime;
        public long endTime;
        public int rewardCoins;
        public int requiredAmount;
        public String status;
    }
}
