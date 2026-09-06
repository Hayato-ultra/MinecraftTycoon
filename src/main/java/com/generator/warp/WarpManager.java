package com.generator.warp;

import com.generator.GeneratorPlugin;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WarpManager implements Listener {

    private final GeneratorPlugin plugin;
    private final File warpsFile;
    private final File homesFile;
    private final Map<String, Warp> warps = new ConcurrentHashMap<>();
    private final Map<UUID, List<Home>> playerHomes = new ConcurrentHashMap<>();
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private final Set<UUID> tpaRequests = ConcurrentHashMap.newKeySet();
    private final Set<UUID> tpaHereRequests = ConcurrentHashMap.newKeySet();
    private final Map<UUID, UUID> tpaTarget = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> tpaHereTarget = new ConcurrentHashMap<>();

    private static final int MAX_HOMES = 3;
    private static final int TELEPORT_DELAY = 40;
    private static final int TPA_EXPIRE = 600;

    public WarpManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
        this.warpsFile = new File(plugin.getDataFolder(), "warps.yml");
        this.homesFile = new File(plugin.getDataFolder(), "homes.yml");
        loadWarps();
        loadHomes();
    }

    private void loadWarps() {
        warps.clear();
        if (!warpsFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(warpsFile);
        ConfigurationSection section = config.getConfigurationSection("warps");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection cs = section.getConfigurationSection(key);
            if (cs == null) continue;

            Warp warp = new Warp();
            warp.name = key;
            warp.displayName = cs.getString("display-name", key);
            warp.world = cs.getString("world", "world");
            warp.x = cs.getDouble("x");
            warp.y = cs.getDouble("y");
            warp.z = cs.getDouble("z");
            warp.yaw = (float) cs.getDouble("yaw");
            warp.pitch = (float) cs.getDouble("pitch");
            warp.cost = cs.getInt("cost", 100);
            warp.creator = cs.getString("creator", "server");
            warps.put(key, warp);
        }
    }

    public void saveWarps() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, Warp> entry : warps.entrySet()) {
            Warp warp = entry.getValue();
            String path = "warps." + warp.name;
            config.set(path + ".display-name", warp.displayName);
            config.set(path + ".world", warp.world);
            config.set(path + ".x", warp.x);
            config.set(path + ".y", warp.y);
            config.set(path + ".z", warp.z);
            config.set(path + ".yaw", warp.yaw);
            config.set(path + ".pitch", warp.pitch);
            config.set(path + ".cost", warp.cost);
            config.set(path + ".creator", warp.creator);
        }

        try {
            config.save(warpsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save warps: " + e.getMessage());
        }
    }

    private void loadHomes() {
        playerHomes.clear();
        if (!homesFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(homesFile);
        ConfigurationSection section = config.getConfigurationSection("players");
        if (section == null) return;

        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection homesSection = section.getConfigurationSection(uuidStr);
                if (homesSection == null) continue;

                List<Home> homes = new ArrayList<>();
                for (String homeName : homesSection.getKeys(false)) {
                    ConfigurationSection cs = homesSection.getConfigurationSection(homeName);
                    if (cs == null) continue;

                    Home home = new Home();
                    home.name = homeName;
                    home.world = cs.getString("world", "world");
                    home.x = cs.getDouble("x");
                    home.y = cs.getDouble("y");
                    home.z = cs.getDouble("z");
                    home.yaw = (float) cs.getDouble("yaw");
                    home.pitch = (float) cs.getDouble("pitch");
                    homes.add(home);
                }
                playerHomes.put(uuid, homes);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveHomes() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, List<Home>> entry : playerHomes.entrySet()) {
            String uuidStr = entry.getKey().toString();
            int index = 0;
            for (Home home : entry.getValue()) {
                String path = "players." + uuidStr + "." + home.name;
                config.set(path + ".world", home.world);
                config.set(path + ".x", home.x);
                config.set(path + ".y", home.y);
                config.set(path + ".z", home.z);
                config.set(path + ".yaw", home.yaw);
                config.set(path + ".pitch", home.pitch);
                index++;
            }
        }

        try {
            config.save(homesFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save homes: " + e.getMessage());
        }
    }

    public boolean setHome(Player player, String name) {
        UUID uuid = player.getUniqueId();
        List<Home> homes = playerHomes.computeIfAbsent(uuid, k -> new ArrayList<>());

        for (Home home : homes) {
            if (home.name.equalsIgnoreCase(name)) {
                Location loc = player.getLocation();
                home.world = loc.getWorld().getName();
                home.x = loc.getX();
                home.y = loc.getY();
                home.z = loc.getZ();
                home.yaw = loc.getYaw();
                home.pitch = loc.getPitch();
                saveHomes();
                player.sendMessage(ChatColor.GREEN + "Home '" + name + "' updated!");
                return true;
            }
        }

        if (homes.size() >= MAX_HOMES) {
            player.sendMessage(ChatColor.RED + "You can only have " + MAX_HOMES + " homes!");
            return false;
        }

        Home home = new Home();
        home.name = name;
        Location loc = player.getLocation();
        home.world = loc.getWorld().getName();
        home.x = loc.getX();
        home.y = loc.getY();
        home.z = loc.getZ();
        home.yaw = loc.getYaw();
        home.pitch = loc.getPitch();
        homes.add(home);
        saveHomes();
        player.sendMessage(ChatColor.GREEN + "Home '" + name + "' set!");
        return true;
    }

    public boolean deleteHome(Player player, String name) {
        List<Home> homes = playerHomes.get(player.getUniqueId());
        if (homes == null) return false;

        Iterator<Home> it = homes.iterator();
        while (it.hasNext()) {
            if (it.next().name.equalsIgnoreCase(name)) {
                it.remove();
                saveHomes();
                player.sendMessage(ChatColor.GREEN + "Home '" + name + "' deleted!");
                return true;
            }
        }
        return false;
    }

    public List<Home> getHomes(UUID uuid) {
        return playerHomes.getOrDefault(uuid, Collections.emptyList());
    }

    public boolean teleportHome(Player player, String name) {
        List<Home> homes = getHomes(player.getUniqueId());
        for (Home home : homes) {
            if (home.name.equalsIgnoreCase(name)) {
                org.bukkit.World world = Bukkit.getWorld(home.world);
                if (world == null) {
                    player.sendMessage(ChatColor.RED + "Home world '" + home.world + "' not found!");
                    return false;
                }
                Location loc = new Location(world, home.x, home.y, home.z, home.yaw, home.pitch);
                startTeleport(player, loc, "home '" + name + "'");
                return true;
            }
        }
        player.sendMessage(ChatColor.RED + "Home '" + name + "' not found!");
        return false;
    }

    public boolean setWarp(Player player, String name, int cost) {
        if (warps.containsKey(name)) {
            player.sendMessage(ChatColor.RED + "Warp '" + name + "' already exists!");
            return false;
        }

        if (!player.hasPermission("generators.admin")) {
            if (!plugin.getCoinManager().hasCoins(player, cost)) {
                player.sendMessage(ChatColor.RED + "Not enough coins! Need " + cost + " coins.");
                return false;
            }
        }

        Warp warp = new Warp();
        warp.name = name;
        warp.displayName = name;
        Location loc = player.getLocation();
        if (loc.getWorld() == null) {
            player.sendMessage(ChatColor.RED + "Cannot set warp here — world not found!");
            return false;
        }
        warp.world = loc.getWorld().getName();
        warp.x = loc.getX();
        warp.y = loc.getY();
        warp.z = loc.getZ();
        warp.yaw = loc.getYaw();
        warp.pitch = loc.getPitch();
        warp.cost = cost;
        warp.creator = player.getName();
        warps.put(name, warp);
        saveWarps();

        if (!player.hasPermission("generators.admin")) {
            plugin.getCoinManager().removeCoins(player, cost);
        }

        player.sendMessage(ChatColor.GREEN + "Warp '" + name + "' created! (-" + cost + " coins)");
        return true;
    }

    public boolean deleteWarp(String name) {
        if (warps.remove(name) != null) {
            saveWarps();
            return true;
        }
        return false;
    }

    public boolean teleportWarp(Player player, String name) {
        Warp warp = warps.get(name);
        if (warp == null) {
            player.sendMessage(ChatColor.RED + "Warp '" + name + "' not found!");
            return false;
        }

        org.bukkit.World world = Bukkit.getWorld(warp.world);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Warp world '" + warp.world + "' not found!");
            return false;
        }

        if (warp.cost > 0 && !player.hasPermission("generators.admin")) {
            if (!plugin.getCoinManager().hasCoins(player, warp.cost)) {
                player.sendMessage(ChatColor.RED + "Not enough coins! Need " + warp.cost + " coins.");
                return false;
            }
            plugin.getCoinManager().removeCoins(player, warp.cost);
        }

        Location loc = new Location(world, warp.x, warp.y, warp.z, warp.yaw, warp.pitch);
        startTeleport(player, loc, "warp '" + name + "'");

        if (warp.cost > 0 && !player.hasPermission("generators.admin")) {
            final int cost = warp.cost;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Player p = Bukkit.getPlayer(player.getUniqueId());
                if (p == null || !p.isOnline()) {
                    plugin.getCoinManager().addCoins(player, cost);
                }
            }, TELEPORT_DELAY + 5);
        }

        return true;
    }

    public Map<String, Warp> getWarps() {
        return warps;
    }

    public void saveLastLocation(Player player) {
        lastLocations.put(player.getUniqueId(), player.getLocation());
    }

    public boolean teleportBack(Player player) {
        Location last = lastLocations.remove(player.getUniqueId());
        if (last == null) {
            player.sendMessage(ChatColor.RED + "No previous location!");
            return false;
        }
        player.teleport(last);
        player.sendMessage(ChatColor.GREEN + "Returned to last location!");
        return true;
    }

    public void sendTPA(Player sender, Player target) {
        if (sender.equals(target)) {
            sender.sendMessage(ChatColor.RED + "You cannot TPA to yourself!");
            return;
        }

        tpaTarget.put(target.getUniqueId(), sender.getUniqueId());
        tpaRequests.add(target.getUniqueId());

        sender.sendMessage(ChatColor.GREEN + "TPA request sent to " + target.getName() + "!");
        target.sendMessage(ChatColor.GREEN + sender.getName() + " wants to teleport to you!");
        target.sendMessage(ChatColor.YELLOW + "Type /tpaccept or /tpdeny, or click in the GUI.");
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (tpaRequests.remove(target.getUniqueId())) {
                tpaTarget.remove(target.getUniqueId());
                sender.sendMessage(ChatColor.RED + "TPA request expired.");
                target.sendMessage(ChatColor.RED + "TPA request from " + sender.getName() + " expired.");
            }
        }, TPA_EXPIRE);
    }

    public void sendTPAHere(Player sender, Player target) {
        if (sender.equals(target)) {
            sender.sendMessage(ChatColor.RED + "You cannot TPA here yourself!");
            return;
        }

        tpaHereTarget.put(target.getUniqueId(), sender.getUniqueId());
        tpaHereRequests.add(target.getUniqueId());

        sender.sendMessage(ChatColor.GREEN + "TPAHere request sent to " + target.getName() + "!");
        target.sendMessage(ChatColor.GREEN + sender.getName() + " wants you to teleport to them!");
        target.sendMessage(ChatColor.YELLOW + "Type /tpaccept or /tpdeny, or click in the GUI.");
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (tpaHereRequests.remove(target.getUniqueId())) {
                tpaHereTarget.remove(target.getUniqueId());
                sender.sendMessage(ChatColor.RED + "TPAHere request expired.");
                target.sendMessage(ChatColor.RED + "TPAHere request from " + sender.getName() + " expired.");
            }
        }, TPA_EXPIRE);
    }

    public boolean acceptTPA(Player player) {
        UUID senderUUID = tpaTarget.remove(player.getUniqueId());
        if (senderUUID != null) {
            tpaRequests.remove(player.getUniqueId());
            Player sender = Bukkit.getPlayer(senderUUID);
            if (sender != null && sender.isOnline()) {
                startTeleport(sender, player.getLocation(), player.getName());
                sender.sendMessage(ChatColor.GREEN + "Teleporting to " + player.getName() + "...");
                player.sendMessage(ChatColor.GREEN + "Accepting " + sender.getName() + "'s TPA.");
                return true;
            }
            player.sendMessage(ChatColor.RED + "That player is no longer online!");
            return false;
        }

        UUID hereSenderUUID = tpaHereTarget.remove(player.getUniqueId());
        if (hereSenderUUID != null) {
            tpaHereRequests.remove(player.getUniqueId());
            Player sender = Bukkit.getPlayer(hereSenderUUID);
            if (sender != null && sender.isOnline()) {
                startTeleport(player, sender.getLocation(), sender.getName());
                player.sendMessage(ChatColor.GREEN + "Teleporting to " + sender.getName() + "...");
                sender.sendMessage(ChatColor.GREEN + player.getName() + " accepted your TPAHere.");
                return true;
            }
            player.sendMessage(ChatColor.RED + "That player is no longer online!");
            return false;
        }

        player.sendMessage(ChatColor.RED + "No pending TPA request!");
        return false;
    }

    public boolean denyTPA(Player player) {
        UUID senderUUID = tpaTarget.remove(player.getUniqueId());
        if (senderUUID != null) {
            tpaRequests.remove(player.getUniqueId());
            Player sender = Bukkit.getPlayer(senderUUID);
            if (sender != null && sender.isOnline()) {
                sender.sendMessage(ChatColor.RED + player.getName() + " denied your TPA request.");
            }
            player.sendMessage(ChatColor.YELLOW + "TPA denied.");
            return true;
        }

        UUID hereSenderUUID = tpaHereTarget.remove(player.getUniqueId());
        if (hereSenderUUID != null) {
            tpaHereRequests.remove(player.getUniqueId());
            Player sender = Bukkit.getPlayer(hereSenderUUID);
            if (sender != null && sender.isOnline()) {
                sender.sendMessage(ChatColor.RED + player.getName() + " denied your TPAHere request.");
            }
            player.sendMessage(ChatColor.YELLOW + "TPAHere denied.");
            return true;
        }

        player.sendMessage(ChatColor.RED + "No pending TPA request!");
        return false;
    }

    private void startTeleport(Player player, Location target, String name) {
        saveLastLocation(player);

        player.sendMessage(ChatColor.YELLOW + "Teleporting to " + name + " in 2 seconds... Don't move!");

        final UUID uuid = player.getUniqueId();
        final Location startLoc = player.getLocation();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) return;

            if (p.getWorld().equals(startLoc.getWorld()) &&
                    p.getLocation().distance(startLoc) > 0.5) {
                p.sendMessage(ChatColor.RED + "Teleport cancelled! You moved.");
                return;
            }

            p.teleport(target);
            p.sendMessage(ChatColor.GREEN + "Teleported to " + name + "!");
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        }, TELEPORT_DELAY);
    }

    public void openWarpGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "WARP_MENU");

        gui.setItem(4, createItem(Material.COMPASS,
                ChatColor.GOLD + "" + ChatColor.BOLD + "WARPS & HOMES"));

        gui.setItem(10, createItem(Material.RED_BED,
                ChatColor.GREEN + "My Homes",
                "", "§7Teleport to your homes",
                "§7" + getHomes(player.getUniqueId()).size() + "/" + MAX_HOMES + " homes set"));

        gui.setItem(12, createItem(Material.ENDER_PEARL,
                ChatColor.AQUA + "Server Warps",
                "", "§7Teleport to server warps"));

        gui.setItem(14, createItem(Material.PLAYER_HEAD,
                ChatColor.YELLOW + "TPA",
                "", "§7Send teleport request"));

        gui.setItem(16, createItem(Material.BOOK,
                ChatColor.WHITE + "Back",
                "", "§7Return to last location"));

        gui.setItem(22, createItem(Material.BARRIER, ChatColor.RED + "Close"));

        player.openInventory(gui);
    }

    public void openHomesGUI(Player player) {
        List<Home> homes = getHomes(player.getUniqueId());
        Inventory gui = Bukkit.createInventory(null, 27, "HOMES_MENU");

        gui.setItem(4, createItem(Material.RED_BED,
                ChatColor.GREEN + "" + ChatColor.BOLD + "MY HOMES",
                "", "§7" + homes.size() + "/" + MAX_HOMES + " homes"));

        int slot = 10;
        for (Home home : homes) {
            if (slot >= 17) break;
            gui.setItem(slot, createItem(Material.RED_BED,
                    ChatColor.GREEN + home.name,
                    "", "§7World: §f" + home.world,
                    "§7Click to teleport"));
            slot++;
        }

        if (homes.size() < MAX_HOMES) {
            gui.setItem(16, createItem(Material.EMERALD,
                    ChatColor.AQUA + "Set Home Here",
                    "", "§7Click to set new home"));
        }

        gui.setItem(22, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));

        player.openInventory(gui);
    }

    public void openWarpsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "SERVER_WARPS");

        gui.setItem(4, createItem(Material.COMPASS,
                ChatColor.GOLD + "" + ChatColor.BOLD + "SERVER WARPS"));

        int slot = 10;
        for (Map.Entry<String, Warp> entry : warps.entrySet()) {
            if (slot >= 17) break;
            Warp warp = entry.getValue();
            gui.setItem(slot, createItem(Material.ENDER_PEARL,
                    ChatColor.AQUA + warp.displayName,
                    "", "§7Cost: §6" + warp.cost + " coins",
                    "§7Click to teleport"));
            slot++;
        }

        gui.setItem(22, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));

        player.openInventory(gui);
    }

    public void openTPAGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "TPA_MENU");

        gui.setItem(4, createItem(Material.PLAYER_HEAD,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "TELEPORT REQUEST"));

        int slot = 10;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            if (slot >= 17) break;

            gui.setItem(slot, createItem(Material.PLAYER_HEAD,
                    ChatColor.GREEN + online.getName(),
                    "", "§7World: §f" + online.getWorld().getName(),
                    "§7Click to send TPA"));
            slot++;
        }

        if (slot == 10) {
            gui.setItem(13, createItem(Material.BARRIER,
                    ChatColor.RED + "No players online"));
        }

        gui.setItem(22, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));

        player.openInventory(gui);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            setHome(player, "home");
            player.sendMessage(ChatColor.GREEN + "Welcome! Your first home has been set at spawn!");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastLocations.remove(uuid);
        tpaRequests.remove(uuid);
        tpaHereRequests.remove(uuid);
        tpaTarget.remove(uuid);
        tpaHereTarget.remove(uuid);
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

    public static class Warp {
        public String name;
        public String displayName;
        public String world;
        public double x, y, z;
        public float yaw, pitch;
        public int cost;
        public String creator;
    }

    public static class Home {
        public String name;
        public String world;
        public double x, y, z;
        public float yaw, pitch;
    }
}
