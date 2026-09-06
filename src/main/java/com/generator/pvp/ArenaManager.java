package com.generator.pvp;

import com.generator.GeneratorPlugin;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArenaManager implements Listener {

    private final GeneratorPlugin plugin;
    private final File arenaFile;
    private final File statsFile;
    private final Map<String, Arena> arenas = new HashMap<>();
    private final Map<UUID, PlayerStats> playerStats = new HashMap<>();
    private final Map<UUID, String> playerArena = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    private final Map<UUID, Float> savedHealth = new HashMap<>();
    private final Map<UUID, Integer> savedFood = new HashMap<>();
    private final Map<UUID, Collection<PotionEffect>> savedEffects = new HashMap<>();
    private final Set<UUID> inArena = new HashSet<>();

    public ArenaManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
        this.arenaFile = new File(plugin.getDataFolder(), "arenas.yml");
        this.statsFile = new File(plugin.getDataFolder(), "pvp_stats.yml");
        loadArenas();
        loadStats();
    }

    private void loadArenas() {
        arenas.clear();
        if (!arenaFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(arenaFile);
        ConfigurationSection section = config.getConfigurationSection("arenas");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection cs = section.getConfigurationSection(key);
            if (cs == null) continue;

            Arena arena = new Arena();
            arena.name = key;
            arena.displayName = cs.getString("display-name", key);
            arena.type = cs.getString("type", "FFA");
            arena.world = cs.getString("world", "pvp");
            arena.lobbyX = cs.getDouble("lobby-x");
            arena.lobbyY = cs.getDouble("lobby-y");
            arena.lobbyZ = cs.getDouble("lobby-z");
            arena.spawnX = cs.getDouble("spawn-x");
            arena.spawnY = cs.getDouble("spawn-y");
            arena.spawnZ = cs.getDouble("spawn-z");
            arena.maxPlayers = cs.getInt("max-players", 8);
            arena.minPlayers = cs.getInt("min-players", 2);
            arena.killReward = cs.getInt("kill-reward", 100);
            arena.winReward = cs.getInt("win-reward", 500);
            arena.kitName = cs.getString("kit", "basic");
            arena.enabled = cs.getBoolean("enabled", true);
            arenas.put(key, arena);
        }
    }

    public void saveArenas() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, Arena> entry : arenas.entrySet()) {
            Arena arena = entry.getValue();
            String path = "arenas." + arena.name;
            config.set(path + ".display-name", arena.displayName);
            config.set(path + ".type", arena.type);
            config.set(path + ".world", arena.world);
            config.set(path + ".lobby-x", arena.lobbyX);
            config.set(path + ".lobby-y", arena.lobbyY);
            config.set(path + ".lobby-z", arena.lobbyZ);
            config.set(path + ".spawn-x", arena.spawnX);
            config.set(path + ".spawn-y", arena.spawnY);
            config.set(path + ".spawn-z", arena.spawnZ);
            config.set(path + ".max-players", arena.maxPlayers);
            config.set(path + ".min-players", arena.minPlayers);
            config.set(path + ".kill-reward", arena.killReward);
            config.set(path + ".win-reward", arena.winReward);
            config.set(path + ".kit", arena.kitName);
            config.set(path + ".enabled", arena.enabled);
        }

        try {
            config.save(arenaFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save arenas: " + e.getMessage());
        }
    }

    private void loadStats() {
        playerStats.clear();
        if (!statsFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(statsFile);
        ConfigurationSection section = config.getConfigurationSection("players");
        if (section == null) return;

        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection cs = section.getConfigurationSection(uuidStr);
                if (cs == null) continue;

                PlayerStats stats = new PlayerStats();
                stats.wins = cs.getInt("wins", 0);
                stats.losses = cs.getInt("losses", 0);
                stats.kills = cs.getInt("kills", 0);
                stats.deaths = cs.getInt("deaths", 0);
                stats.elo = cs.getInt("elo", 1000);
                stats.killStreak = cs.getInt("kill-streak", 0);
                stats.bestKillStreak = cs.getInt("best-kill-streak", 0);
                stats.rank = calculateRank(stats.elo);
                playerStats.put(uuid, stats);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveStats() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            String uuidStr = entry.getKey().toString();
            PlayerStats stats = entry.getValue();
            config.set("players." + uuidStr + ".wins", stats.wins);
            config.set("players." + uuidStr + ".losses", stats.losses);
            config.set("players." + uuidStr + ".kills", stats.kills);
            config.set("players." + uuidStr + ".deaths", stats.deaths);
            config.set("players." + uuidStr + ".elo", stats.elo);
            config.set("players." + uuidStr + ".kill-streak", stats.killStreak);
            config.set("players." + uuidStr + ".best-kill-streak", stats.bestKillStreak);
        }

        try {
            config.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save PvP stats: " + e.getMessage());
        }
    }

    public String calculateRank(int elo) {
        if (elo >= 2000) return "Diamond";
        if (elo >= 1500) return "Gold";
        if (elo >= 1200) return "Silver";
        return "Bronze";
    }

    public PlayerStats getStats(UUID uuid) {
        return playerStats.computeIfAbsent(uuid, k -> new PlayerStats());
    }

    public List<PlayerStats> getTopPlayers(int limit) {
        return playerStats.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().elo, a.getValue().elo))
                .limit(limit)
                .map(e -> e.getValue())
                .toList();
    }

    public List<Map.Entry<UUID, PlayerStats>> getTopEntries(int limit) {
        return playerStats.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().elo, a.getValue().elo))
                .limit(limit)
                .toList();
    }

    public Arena getArena(String name) {
        return arenas.get(name);
    }

    public Collection<Arena> getArenas() {
        return arenas.values();
    }

    public boolean isInArena(Player player) {
        return inArena.contains(player.getUniqueId());
    }

    public String getPlayerArena(Player player) {
        return playerArena.get(player.getUniqueId());
    }

    public boolean joinArena(Player player, String arenaName) {
        Arena arena = arenas.get(arenaName);
        if (arena == null || !arena.enabled) {
            player.sendMessage(ChatColor.RED + "Arena not found or disabled!");
            return false;
        }

        if (inArena.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already in an arena!");
            return false;
        }

        savePlayerState(player);
        inArena.add(player.getUniqueId());
        playerArena.put(player.getUniqueId(), arenaName);

        org.bukkit.World world = Bukkit.getWorld(arena.world);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Arena world '" + arena.world + "' not found!");
            inArena.remove(player.getUniqueId());
            playerArena.remove(player.getUniqueId());
            return false;
        }

        Location lobby = new Location(world, arena.lobbyX, arena.lobbyY, arena.lobbyZ);
        player.teleport(lobby);
        applyKit(player, arena.kitName);

        player.sendMessage(ChatColor.GREEN + "Joined " + arena.displayName + "!");
        player.sendMessage(ChatColor.YELLOW + "Waiting for players... (" + getArenaPlayerCount(arenaName) + "/" + arena.maxPlayers + ")");

        broadcastToArena(arenaName, ChatColor.YELLOW + player.getName() + " joined! (" + getArenaPlayerCount(arenaName) + "/" + arena.maxPlayers + ")");

        if (getArenaPlayerCount(arenaName) >= arena.minPlayers) {
            startCountdown(arenaName);
        }

        return true;
    }

    public void leaveArena(Player player) {
        if (!inArena.contains(player.getUniqueId())) return;

        String arenaName = playerArena.remove(player.getUniqueId());
        inArena.remove(player.getUniqueId());

        restorePlayerState(player);

        player.sendMessage(ChatColor.YELLOW + "Left the arena.");

        if (arenaName != null) {
            broadcastToArena(arenaName, ChatColor.RED + player.getName() + " left the arena.");
        }
    }

    private int getArenaPlayerCount(String arenaName) {
        int count = 0;
        for (String arena : playerArena.values()) {
            if (arena.equals(arenaName)) count++;
        }
        return count;
    }

    private List<Player> getArenaPlayers(String arenaName) {
        List<Player> players = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : playerArena.entrySet()) {
            if (entry.getValue().equals(arenaName)) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) players.add(player);
            }
        }
        return players;
    }

    private void startCountdown(String arenaName) {
        Arena arena = arenas.get(arenaName);
        if (arena == null) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (getArenaPlayerCount(arenaName) < arena.minPlayers) {
                broadcastToArena(arenaName, ChatColor.RED + "Not enough players. Countdown cancelled.");
                return;
            }
            broadcastToArena(arenaName, ChatColor.GREEN + "Game starting in 5 seconds!");
            for (int i = 5; i > 0; i--) {
                final int sec = i;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (getArenaPlayerCount(arenaName) < arena.minPlayers) {
                        broadcastToArena(arenaName, ChatColor.RED + "Not enough players. Countdown cancelled.");
                        return;
                    }
                    broadcastToArena(arenaName, ChatColor.YELLOW + "Starting in " + sec + "...");
                }, (5 - sec) * 20L);
            }
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                startMatch(arenaName);
            }, 5 * 20L);
        }, 10L);
    }

    private void startMatch(String arenaName) {
        Arena arena = arenas.get(arenaName);
        if (arena == null) return;

        List<Player> players = getArenaPlayers(arenaName);
        if (players.isEmpty()) return;

        org.bukkit.World world = Bukkit.getWorld(arena.world);
        if (world == null) {
            broadcastToArena(arenaName, ChatColor.RED + "Arena world '" + arena.world + "' not found!");
            for (Player p : players) leaveArena(p);
            return;
        }

        broadcastToArena(arenaName, ChatColor.GREEN + "" + ChatColor.BOLD + "GAME STARTED!");
        broadcastToArena(arenaName, ChatColor.YELLOW + "Mode: " + arena.type);

        for (Player player : players) {
            Location spawn = new Location(world, arena.spawnX, arena.spawnY, arena.spawnZ);
            player.teleport(spawn);
            player.setGameMode(GameMode.SURVIVAL);
            AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHealth != null) {
                player.setHealth(maxHealth.getValue());
            }
            player.setFoodLevel(20);
        }
    }

    private void savePlayerState(Player player) {
        UUID uuid = player.getUniqueId();
        savedInventories.put(uuid, player.getInventory().getContents());
        savedArmor.put(uuid, player.getInventory().getArmorContents());
        savedHealth.put(uuid, (float) player.getHealth());
        savedFood.put(uuid, player.getFoodLevel());
        savedEffects.put(uuid, player.getActivePotionEffects());

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
    }

    private void restorePlayerState(Player player) {
        UUID uuid = player.getUniqueId();

        player.getInventory().setContents(savedInventories.getOrDefault(uuid, new ItemStack[0]));
        player.getInventory().setArmorContents(savedArmor.getOrDefault(uuid, new ItemStack[0]));

        float health = savedHealth.getOrDefault(uuid, 20f);
        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            player.setHealth(Math.min(health, maxHealth.getValue()));
        }

        player.setFoodLevel(savedFood.getOrDefault(uuid, 20));

        Collection<PotionEffect> effects = savedEffects.getOrDefault(uuid, Collections.emptyList());
        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }

        savedInventories.remove(uuid);
        savedArmor.remove(uuid);
        savedHealth.remove(uuid);
        savedFood.remove(uuid);
        savedEffects.remove(uuid);
    }

    public void applyKit(Player player, String kitName) {
        player.getInventory().clear();

        switch (kitName.toLowerCase()) {
            case "basic" -> {
                player.getInventory().addItem(new ItemStack(Material.STONE_SWORD));
                player.getInventory().addItem(new ItemStack(Material.BOW));
                player.getInventory().addItem(new ItemStack(Material.ARROW, 32));
                player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));
                player.getInventory().setHelmet(new ItemStack(Material.LEATHER_HELMET));
                player.getInventory().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
                player.getInventory().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
                player.getInventory().setBoots(new ItemStack(Material.LEATHER_BOOTS));
            }
            case "diamond" -> {
                player.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD));
                player.getInventory().addItem(new ItemStack(Material.BOW));
                player.getInventory().addItem(new ItemStack(Material.ARROW, 64));
                player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 3));
                player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            }
            case "netherite" -> {
                player.getInventory().addItem(new ItemStack(Material.NETHERITE_SWORD));
                player.getInventory().addItem(new ItemStack(Material.BOW));
                player.getInventory().addItem(new ItemStack(Material.ARROW, 64));
                player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 5));
                player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 4));
                player.getInventory().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
                player.getInventory().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
                player.getInventory().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
                player.getInventory().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 600, 0));
            }
            default -> {
                player.getInventory().addItem(new ItemStack(Material.STONE_SWORD));
                player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 8));
                player.getInventory().setHelmet(new ItemStack(Material.LEATHER_HELMET));
                player.getInventory().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
                player.getInventory().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
                player.getInventory().setBoots(new ItemStack(Material.LEATHER_BOOTS));
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!isInArena(victim)) return;

        Player killer = victim.getKiller();
        String arenaName = playerArena.get(victim.getUniqueId());
        Arena arena = arenas.get(arenaName);
        if (arena == null) return;

        event.getDrops().clear();

        if (killer != null && isInArena(killer)) {
            PlayerStats killerStats = getStats(killer.getUniqueId());
            killerStats.kills++;
            killerStats.killStreak++;
            if (killerStats.killStreak > killerStats.bestKillStreak) {
                killerStats.bestKillStreak = killerStats.killStreak;
            }
            killerStats.elo += arena.killReward / 10;
            killerStats.rank = calculateRank(killerStats.elo);

            plugin.getCoinManager().addCoins(killer, arena.killReward);
            killer.sendMessage(ChatColor.GREEN + "+" + arena.killReward + " coins for kill!");

            broadcastToArena(arenaName, ChatColor.RED + victim.getName() + " was killed by " + killer.getName() + "!");
        }

        PlayerStats victimStats = getStats(victim.getUniqueId());
        victimStats.deaths++;
        victimStats.losses++;
        victimStats.killStreak = 0;
        victimStats.elo -= arena.killReward / 15;
        if (victimStats.elo < 0) victimStats.elo = 0;
        victimStats.rank = calculateRank(victimStats.elo);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            org.bukkit.World world = Bukkit.getWorld(arena.world);
            if (world == null) return;
            Location lobby = new Location(world, arena.lobbyX, arena.lobbyY, arena.lobbyZ);
            victim.teleport(lobby);
            applyKit(victim, arena.kitName);
        }, 5L);

        checkArenaEnd(arenaName);
    }

    private void checkArenaEnd(String arenaName) {
        List<Player> alive = getArenaPlayers(arenaName).stream()
                .filter(p -> p.getHealth() > 0)
                .toList();

        Arena arena = arenas.get(arenaName);
        if (arena == null) return;

        if (alive.size() <= 1 && getArenaPlayerCount(arenaName) >= arena.minPlayers) {
            if (alive.size() == 1) {
                Player winner = alive.get(0);
                PlayerStats winnerStats = getStats(winner.getUniqueId());
                winnerStats.wins++;
                winnerStats.elo += arena.winReward / 5;
                winnerStats.rank = calculateRank(winnerStats.elo);

                plugin.getCoinManager().addCoins(winner, arena.winReward);

                broadcastToArena(arenaName, ChatColor.GREEN + "" + ChatColor.BOLD + winner.getName() + " WINS! +" + arena.winReward + " coins!");
                winner.playSound(winner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }

            List<Player> toRemove = new ArrayList<>(getArenaPlayers(arenaName));
            for (Player player : toRemove) {
                leaveArena(player);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        String attackerArena = playerArena.get(attacker.getUniqueId());
        String victimArena = playerArena.get(victim.getUniqueId());

        boolean attackerIn = attackerArena != null;
        boolean victimIn = victimArena != null;

        if (attackerIn && victimIn) {
            if (!attackerArena.equals(victimArena)) {
                event.setCancelled(true);
            }
        } else if (attackerIn && !victimIn) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        leaveArena(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        getStats(event.getPlayer().getUniqueId());
    }

    private void broadcastToArena(String arenaName, String message) {
        for (Player player : getArenaPlayers(arenaName)) {
            player.sendMessage(message);
        }
    }

    public void openArenaGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "PVP_ARENA");

        gui.setItem(4, createItem(Material.NETHER_STAR,
                ChatColor.RED + "" + ChatColor.BOLD + "PVP ARENAS"));

        int slot = 10;
        for (Arena arena : arenas.values()) {
            if (!arena.enabled) continue;
            if (slot >= 17) break;

            Material mat = arena.type.equals("FFA") ? Material.IRON_SWORD : Material.DIAMOND_SWORD;
            int players = getArenaPlayerCount(arena.name);

            gui.setItem(slot, createItem(mat,
                    ChatColor.GREEN + arena.displayName,
                    "",
                    "§7Type: §f" + arena.type,
                    "§7Players: §f" + players + "/" + arena.maxPlayers,
                    "§7Kit: §f" + arena.kitName,
                    "§7Kill Reward: §6" + arena.killReward + " coins",
                    "§7Win Reward: §6" + arena.winReward + " coins",
                    "",
                    "§eClick to join"));
            slot++;
        }

        gui.setItem(18, createItem(Material.BARRIER, ChatColor.RED + "Close"));

        player.openInventory(gui);
    }

    public void openLeaderboardGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "PVP_LEADERBOARD");

        gui.setItem(4, createItem(Material.GOLD_INGOT,
                ChatColor.GOLD + "" + ChatColor.BOLD + "PVP LEADERBOARD"));

        List<Map.Entry<UUID, PlayerStats>> topEntries = getTopEntries(8);
        int slot = 10;
        for (int i = 0; i < topEntries.size(); i++) {
            Map.Entry<UUID, PlayerStats> entry = topEntries.get(i);
            UUID uuid = entry.getKey();
            PlayerStats stats = entry.getValue();
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name == null) name = uuid.toString().substring(0, 8);

            ChatColor rankColor = switch (stats.rank) {
                case "Diamond" -> ChatColor.AQUA;
                case "Gold" -> ChatColor.GOLD;
                case "Silver" -> ChatColor.GRAY;
                default -> ChatColor.YELLOW;
            };

            gui.setItem(slot, createItem(Material.PLAYER_HEAD,
                    rankColor + "#" + (i + 1) + " " + name,
                    "",
                    "§7Rank: " + rankColor + stats.rank,
                    "§7ELO: §f" + stats.elo,
                    "§7Wins: §a" + stats.wins + " §7Losses: §c" + stats.losses,
                    "§7Kills: §a" + stats.kills + " §7Deaths: §c" + stats.deaths,
                    "§7Best Streak: §6" + stats.bestKillStreak));
            slot++;
        }

        gui.setItem(22, createItem(Material.BARRIER, ChatColor.RED + "Close"));

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

    public static class Arena {
        public String name;
        public String displayName;
        public String type;
        public String world;
        public double lobbyX, lobbyY, lobbyZ;
        public double spawnX, spawnY, spawnZ;
        public int maxPlayers;
        public int minPlayers;
        public int killReward;
        public int winReward;
        public String kitName;
        public boolean enabled;
    }

    public static class PlayerStats {
        public int wins = 0;
        public int losses = 0;
        public int kills = 0;
        public int deaths = 0;
        public int elo = 1000;
        public int killStreak = 0;
        public int bestKillStreak = 0;
        public String rank = "Bronze";
    }
}
