package com.generator.smp;

import com.generator.GeneratorPlugin;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CommunityManager implements Listener {

    private final GeneratorPlugin plugin;
    private final File communityFile;
    private final Map<String, CommunityArea> areas = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> unlockedAreas = new ConcurrentHashMap<>();

    public CommunityManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
        this.communityFile = new File(plugin.getDataFolder(), "community.yml");
        loadAreas();
        loadUnlocked();
    }

    private void loadAreas() {
        areas.clear();
        if (!communityFile.exists()) {
            createDefaultAreas();
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(communityFile);
        ConfigurationSection section = config.getConfigurationSection("areas");
        if (section == null) {
            createDefaultAreas();
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection cs = section.getConfigurationSection(key);
            if (cs == null) continue;

            CommunityArea area = new CommunityArea();
            area.id = key;
            area.displayName = cs.getString("display-name", key);
            area.description = cs.getString("description", "");
            area.material = Material.matchMaterial(cs.getString("material", "CHEST"));
            area.world = cs.getString("world", "lobby");
            area.x = cs.getDouble("x");
            area.y = cs.getDouble("y");
            area.z = cs.getDouble("z");
            area.radius = cs.getInt("radius", 50);
            area.requiredLevel = cs.getInt("required-level", 0);
            area.requiredCoins = cs.getInt("required-coins", 0);
            area.isUnlocked = cs.getBoolean("default-unlocked", true);

            areas.put(key, area);
        }

        if (areas.isEmpty()) {
            createDefaultAreas();
        }
    }

    private void createDefaultAreas() {
        areas.clear();

        addArea("market", "Community Market", "Trade with other players", "EMERALD_BLOCK",
                "lobby", 0, 64, 0, 100, 0, 0, true);
        addArea("farm", "Community Farm", "Grow crops together", "WHEAT",
                "lobby", 100, 64, 0, 80, 5, 1000, true);
        addArea("mine", "Community Mine", "Mine resources together", "DIAMOND_ORE",
                "lobby", -100, 64, 0, 80, 10, 5000, true);
        addArea("arena", "Community Arena", "PvP with friends", "IRON_SWORD",
                "lobby", 0, 64, 100, 60, 15, 10000, true);
        addArea("fishing", "Fishing Dock", "Fish for treasures", "FISHING_ROD",
                "lobby", 0, 64, -100, 50, 3, 2000, true);
        addArea("library", "Community Library", "Enchant your items", "ENCHANTING_TABLE",
                "lobby", 50, 64, 50, 40, 20, 20000, false);
        addArea("nether_gate", "Nether Portal", "Access the nether", "OBSIDIAN",
                "lobby", -50, 64, 50, 30, 25, 50000, false);
        addArea("end_portal", "End Portal", "Access the end", "END_STONE",
                "lobby", 50, 64, -50, 30, 30, 100000, false);

        saveAreas();
    }

    private void addArea(String id, String name, String desc, String material,
                         String world, double x, double y, double z, int radius,
                         int requiredLevel, int requiredCoins, boolean unlocked) {
        CommunityArea area = new CommunityArea();
        area.id = id;
        area.displayName = name;
        area.description = desc;
        area.material = Material.matchMaterial(material);
        area.world = world;
        area.x = x;
        area.y = y;
        area.z = z;
        area.radius = radius;
        area.requiredLevel = requiredLevel;
        area.requiredCoins = requiredCoins;
        area.isUnlocked = unlocked;
        areas.put(id, area);
    }

    public void saveAreas() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, CommunityArea> entry : areas.entrySet()) {
            CommunityArea area = entry.getValue();
            String path = "areas." + area.id;
            config.set(path + ".display-name", area.displayName);
            config.set(path + ".description", area.description);
            config.set(path + ".material", area.material != null ? area.material.name() : "CHEST");
            config.set(path + ".world", area.world);
            config.set(path + ".x", area.x);
            config.set(path + ".y", area.y);
            config.set(path + ".z", area.z);
            config.set(path + ".radius", area.radius);
            config.set(path + ".required-level", area.requiredLevel);
            config.set(path + ".required-coins", area.requiredCoins);
            config.set(path + ".default-unlocked", area.isUnlocked);
        }

        try {
            config.save(communityFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save community.yml: " + e.getMessage());
        }
    }

    public void saveUnlocked() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, Set<String>> entry : unlockedAreas.entrySet()) {
            config.set("unlocked." + entry.getKey().toString(), new ArrayList<>(entry.getValue()));
        }

        try {
            YamlConfiguration existing = YamlConfiguration.loadConfiguration(communityFile);
            existing.set("unlocked", config.get("unlocked"));
            existing.save(communityFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save community unlock data: " + e.getMessage());
        }
    }

    private void loadUnlocked() {
        unlockedAreas.clear();
        if (!communityFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(communityFile);
        ConfigurationSection section = config.getConfigurationSection("unlocked");
        if (section == null) return;

        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                List<String> unlocked = section.getStringList(uuidStr);
                unlockedAreas.put(uuid, new HashSet<>(unlocked));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public boolean canAccess(Player player, String areaId) {
        CommunityArea area = areas.get(areaId);
        if (area == null) return false;
        if (area.isUnlocked) return true;

        Set<String> unlocked = unlockedAreas.getOrDefault(player.getUniqueId(), Collections.emptySet());
        return unlocked.contains(areaId);
    }

    public boolean unlockArea(Player player, String areaId) {
        CommunityArea area = areas.get(areaId);
        if (area == null) return false;
        if (canAccess(player, areaId)) return false;

        if (area.requiredCoins > 0) {
            if (!plugin.getCoinManager().hasCoins(player, area.requiredCoins)) {
                player.sendMessage(ChatColor.RED + "Not enough coins! Need " + area.requiredCoins);
                return false;
            }
            plugin.getCoinManager().removeCoins(player, area.requiredCoins);
        }

        unlockedAreas.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(areaId);
        saveUnlocked();

        player.sendMessage(ChatColor.GREEN + "Unlocked area: " + area.displayName + "!");
        return true;
    }

    public void teleportToArea(Player player, String areaId) {
        CommunityArea area = areas.get(areaId);
        if (area == null) {
            player.sendMessage(ChatColor.RED + "Area not found!");
            return;
        }

        if (!canAccess(player, areaId)) {
            if (area.requiredCoins > 0) {
                player.sendMessage(ChatColor.RED + "This area is locked! Unlock it with /community unlock " + areaId);
            } else {
                player.sendMessage(ChatColor.RED + "You don't have access to this area!");
            }
            return;
        }

        org.bukkit.World world = Bukkit.getWorld(area.world);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "World not found!");
            return;
        }

        Location loc = new Location(world, area.x, area.y, area.z);
        player.teleport(loc);
        player.sendMessage(ChatColor.GREEN + "Teleported to " + area.displayName + "!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        for (Map.Entry<String, CommunityArea> entry : areas.entrySet()) {
            if (entry.getValue().isUnlocked) {
                unlockedAreas.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(entry.getKey());
            }
        }
        saveUnlocked();
    }

    public CommunityArea getArea(String id) {
        return areas.get(id);
    }

    public Collection<CommunityArea> getAllAreas() {
        return areas.values();
    }

    public Set<String> getUnlockedAreas(UUID uuid) {
        return unlockedAreas.getOrDefault(uuid, Collections.emptySet());
    }

    public void addArea(CommunityArea area) {
        areas.put(area.id, area);
        saveAreas();
    }

    public boolean removeArea(String id) {
        if (areas.remove(id) != null) {
            saveAreas();
            return true;
        }
        return false;
    }

    public static class CommunityArea {
        public String id;
        public String displayName;
        public String description;
        public Material material;
        public String world;
        public double x, y, z;
        public int radius;
        public int requiredLevel;
        public int requiredCoins;
        public boolean isUnlocked;
    }
}
