package com.generator.smp;

import com.generator.GeneratorPlugin;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnManager implements Listener {

    private final GeneratorPlugin plugin;
    private final File spawnFile;
    private final Map<String, Location> spawnPoints = new ConcurrentHashMap<>();
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private final Set<UUID> spawnProtection = ConcurrentHashMap.newKeySet();

    private static final int SPAWN_PROTECTION_RADIUS = 10;
    private static final int SPAWN_PROTECTION_TIME = 100;

    public SpawnManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
        this.spawnFile = new File(plugin.getDataFolder(), "spawn.yml");
        loadSpawns();
    }

    private void loadSpawns() {
        spawnPoints.clear();
        if (!spawnFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(spawnFile);
        ConfigurationSection section = config.getConfigurationSection("spawns");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection cs = section.getConfigurationSection(key);
            if (cs == null) continue;

            String worldName = cs.getString("world", "world");
            org.bukkit.World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            Location loc = new Location(
                    world,
                    cs.getDouble("x"),
                    cs.getDouble("y"),
                    cs.getDouble("z"),
                    (float) cs.getDouble("yaw"),
                    (float) cs.getDouble("pitch")
            );
            spawnPoints.put(key, loc);
        }
    }

    public void saveSpawns() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, Location> entry : spawnPoints.entrySet()) {
            Location loc = entry.getValue();
            String path = "spawns." + entry.getKey();
            config.set(path + ".world", loc.getWorld().getName());
            config.set(path + ".x", loc.getX());
            config.set(path + ".y", loc.getY());
            config.set(path + ".z", loc.getZ());
            config.set(path + ".yaw", loc.getYaw());
            config.set(path + ".pitch", loc.getPitch());
        }

        try {
            config.save(spawnFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save spawn.yml: " + e.getMessage());
        }
    }

    public void setSpawn(String name, Location loc) {
        spawnPoints.put(name, loc);
        saveSpawns();
    }

    public boolean removeSpawn(String name) {
        if (spawnPoints.remove(name) != null) {
            saveSpawns();
            return true;
        }
        return false;
    }

    public Location getSpawn(String name) {
        return spawnPoints.get(name);
    }

    public Location getDefaultSpawn() {
        return spawnPoints.getOrDefault("default", spawnPoints.values().stream().findFirst().orElse(null));
    }

    public Map<String, Location> getAllSpawns() {
        return Collections.unmodifiableMap(spawnPoints);
    }

    public void teleportToSpawn(Player player, String name) {
        Location loc = spawnPoints.get(name);
        if (loc == null) {
            player.sendMessage(ChatColor.RED + "Spawn '" + name + "' not found!");
            return;
        }

        lastLocations.put(player.getUniqueId(), player.getLocation().clone());
        player.teleport(loc);
        player.sendMessage(ChatColor.GREEN + "Teleported to spawn: " + name);
    }

    public void teleportToDefaultSpawn(Player player) {
        Location loc = getDefaultSpawn();
        if (loc == null) {
            player.sendMessage(ChatColor.RED + "No spawn point set!");
            return;
        }

        lastLocations.put(player.getUniqueId(), player.getLocation().clone());
        player.teleport(loc);
        player.sendMessage(ChatColor.GREEN + "Teleported to spawn!");
    }

    public boolean goBack(Player player) {
        Location lastLoc = lastLocations.remove(player.getUniqueId());
        if (lastLoc == null) {
            player.sendMessage(ChatColor.RED + "No previous location!");
            return false;
        }

        player.teleport(lastLoc);
        player.sendMessage(ChatColor.GREEN + "Returned to previous location!");
        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            Location spawn = getDefaultSpawn();
            if (spawn != null) {
                player.teleport(spawn);
                giveStarterItems(player);
            }
        }

        spawnProtection.add(player.getUniqueId());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            spawnProtection.remove(player.getUniqueId());
        }, SPAWN_PROTECTION_TIME);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Location spawn = getDefaultSpawn();
        if (spawn != null) {
            event.setRespawnLocation(spawn);
            spawnProtection.add(event.getPlayer().getUniqueId());
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                spawnProtection.remove(event.getPlayer().getUniqueId());
            }, SPAWN_PROTECTION_TIME);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!spawnProtection.contains(player.getUniqueId())) return;

        Location spawn = getDefaultSpawn();
        if (spawn == null) return;
        if (player.getWorld().equals(spawn.getWorld()) &&
                player.getLocation().distance(spawn) < SPAWN_PROTECTION_RADIUS) {
            event.setCancelled(true);
        }
    }

    private void giveStarterItems(Player player) {
        player.getInventory().addItem(new ItemStack(Material.BREAD, 16));
        player.getInventory().addItem(new ItemStack(Material.OAK_PLANKS, 32));
        player.getInventory().addItem(new ItemStack(Material.OAK_LOG, 8));
        player.getInventory().addItem(new ItemStack(Material.STONE_PICKAXE));
        player.getInventory().addItem(new ItemStack(Material.STONE_AXE));
        player.getInventory().addItem(new ItemStack(Material.STONE_SHOVEL));
        player.getInventory().addItem(new ItemStack(Material.TORCH, 16));

        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Welcome to MinecraftTycoon!");
        player.sendMessage(ChatColor.YELLOW + "Type /menu to open the server menu");
        player.sendMessage(ChatColor.YELLOW + "Type /help for a list of commands");
    }

    public boolean isSpawnProtected(UUID uuid) {
        return spawnProtection.contains(uuid);
    }
}
