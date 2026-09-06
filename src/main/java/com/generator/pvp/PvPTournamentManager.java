package com.generator.pvp;

import com.generator.GeneratorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PvPTournamentManager implements Listener {

    private final GeneratorPlugin plugin;
    private final PvPStatsManager statsManager;
    private final Map<String, Tournament> tournaments = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerTournaments = new ConcurrentHashMap<>();
    private final Set<Inventory> managedInventories = Collections.synchronizedSet(new HashSet<>());
    private final Map<Inventory, String> inventoryTitles = Collections.synchronizedMap(new HashMap<>());

    public PvPTournamentManager(GeneratorPlugin plugin, PvPStatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
    }

    public boolean createTournament(String name, String type, int maxPlayers, int entryFee, int rewardCoins) {
        if (tournaments.containsKey(name)) return false;

        Tournament tournament = new Tournament();
        tournament.name = name;
        tournament.type = type;
        tournament.maxPlayers = maxPlayers;
        tournament.entryFee = entryFee;
        tournament.rewardCoins = rewardCoins;
        tournament.status = "registering";
        tournament.startTime = System.currentTimeMillis() + 300000;

        tournaments.put(name, tournament);
        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "TOURNAMENT CREATED: " + name + "!");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "Type: " + type + " | Players: " + maxPlayers);
        Bukkit.broadcastMessage(ChatColor.YELLOW + "Entry Fee: " + entryFee + " coins | Reward: " + rewardCoins + " coins");
        Bukkit.broadcastMessage(ChatColor.AQUA + "Join with /pvp join " + name);
        return true;
    }

    public boolean joinTournament(Player player, String name) {
        Tournament tournament = tournaments.get(name);
        if (tournament == null) {
            player.sendMessage(ChatColor.RED + "Tournament not found!");
            return false;
        }

        if (!tournament.status.equals("registering")) {
            player.sendMessage(ChatColor.RED + "Tournament is not accepting registrations!");
            return false;
        }

        if (tournament.players.size() >= tournament.maxPlayers) {
            player.sendMessage(ChatColor.RED + "Tournament is full!");
            return false;
        }

        if (playerTournaments.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already in a tournament!");
            return false;
        }

        if (tournament.entryFee > 0 && !plugin.getCoinManager().hasCoins(player, tournament.entryFee)) {
            player.sendMessage(ChatColor.RED + "Not enough coins! Need " + tournament.entryFee);
            return false;
        }

        if (tournament.entryFee > 0) {
            plugin.getCoinManager().removeCoins(player, tournament.entryFee);
        }

        tournament.players.add(player.getUniqueId());
        tournament.alive.add(player.getUniqueId());
        playerTournaments.put(player.getUniqueId(), name);

        player.sendMessage(ChatColor.GREEN + "Joined tournament: " + name + "!");
        Bukkit.broadcastMessage(ChatColor.YELLOW + player.getName() + " joined " + name + " (" + tournament.players.size() + "/" + tournament.maxPlayers + ")");

        if (tournament.players.size() >= tournament.maxPlayers) {
            startTournament(name);
        }
        return true;
    }

    public boolean leaveTournament(Player player) {
        String name = playerTournaments.remove(player.getUniqueId());
        if (name == null) {
            player.sendMessage(ChatColor.RED + "You are not in a tournament!");
            return false;
        }

        Tournament tournament = tournaments.get(name);
        if (tournament == null) return false;

        tournament.players.remove(player.getUniqueId());
        tournament.alive.remove(player.getUniqueId());

        player.sendMessage(ChatColor.YELLOW + "Left tournament: " + name);

        if (tournament.status.equals("active")) {
            checkTournamentEnd(name);
        }
        return true;
    }

    public void startTournament(String name) {
        Tournament tournament = tournaments.get(name);
        if (tournament == null || !tournament.status.equals("registering")) return;

        tournament.status = "active";
        tournament.startTime = System.currentTimeMillis();

        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "TOURNAMENT " + name + " HAS STARTED!");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "" + tournament.players.size() + " players competing!");

        for (UUID uuid : tournament.players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(ChatColor.GREEN + "Tournament started! Fight!");
                teleportToArena(player, tournament.type);
            }
        }
    }

    private void teleportToArena(Player player, String type) {
        switch (type.toLowerCase()) {
            case "ffa" -> player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            case "duels" -> player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            default -> player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        String victimTournament = playerTournaments.get(victim.getUniqueId());
        if (victimTournament == null) return;

        Tournament tournament = tournaments.get(victimTournament);
        if (tournament == null || !tournament.status.equals("active")) return;

        tournament.alive.remove(victim.getUniqueId());

        if (killer != null) {
            String killerTournament = playerTournaments.get(killer.getUniqueId());
            if (victimTournament.equals(killerTournament)) {
                tournament.eliminations.put(killer.getUniqueId(), tournament.eliminations.getOrDefault(killer.getUniqueId(), 0) + 1);
                killer.sendMessage(ChatColor.GREEN + "Eliminated " + victim.getName() + "! (" + tournament.alive.size() + " remaining)");
            }
        }

        victim.sendMessage(ChatColor.RED + "You have been eliminated from " + victimTournament + "!");
        playerTournaments.remove(victim.getUniqueId());

        checkTournamentEnd(victimTournament);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String name = playerTournaments.remove(player.getUniqueId());
        if (name == null) return;

        Tournament tournament = tournaments.get(name);
        if (tournament != null) {
            tournament.players.remove(player.getUniqueId());
            tournament.alive.remove(player.getUniqueId());
            checkTournamentEnd(name);
        }
    }

    private void checkTournamentEnd(String name) {
        Tournament tournament = tournaments.get(name);
        if (tournament == null || !tournament.status.equals("active")) return;

        if (tournament.alive.size() <= 1) {
            endTournament(name);
        }
    }

    private void endTournament(String name) {
        Tournament tournament = tournaments.remove(name);
        if (tournament == null) return;

        tournament.status = "ended";

        UUID winnerId = tournament.alive.isEmpty() ? tournament.players.get(tournament.players.size() - 1) : tournament.alive.get(0);
        Player winner = Bukkit.getPlayer(winnerId);

        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "TOURNAMENT " + name + " HAS ENDED!");
        if (winner != null) {
            plugin.getCoinManager().addCoins(winner, tournament.rewardCoins);
            Bukkit.broadcastMessage(ChatColor.GREEN + "Winner: " + winner.getName() + "! +" + tournament.rewardCoins + " coins!");
            winner.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "YOU WON THE TOURNAMENT! +" + tournament.rewardCoins + " coins!");
        } else {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Winner: " + Bukkit.getOfflinePlayer(winnerId).getName());
        }

        Bukkit.broadcastMessage(ChatColor.YELLOW + "Kill Leader: " + getKillLeader(tournament));
    }

    private String getKillLeader(Tournament tournament) {
        UUID topKiller = null;
        int topKills = 0;
        for (Map.Entry<UUID, Integer> entry : tournament.eliminations.entrySet()) {
            if (entry.getValue() > topKills) {
                topKills = entry.getValue();
                topKiller = entry.getKey();
            }
        }
        if (topKiller == null) return "None";
        Player player = Bukkit.getPlayer(topKiller);
        return (player != null ? player.getName() : Bukkit.getOfflinePlayer(topKiller).getName()) + " (" + topKills + " kills)";
    }

    public void openTournamentGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "PVP_TOURNAMENTS");
        managedInventories.add(gui);
        inventoryTitles.put(gui, "PVP_TOURNAMENTS");

        int slot = 0;
        for (Tournament tournament : tournaments.values()) {
            if (slot >= 26) break;
            Material mat = tournament.status.equals("registering") ? Material.GREEN_BED : Material.RED_BED;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + tournament.name);
                meta.setLore(Arrays.asList(
                        "",
                        "§7Type: §f" + tournament.type,
                        "§7Players: §f" + tournament.players.size() + "/" + tournament.maxPlayers,
                        "§7Entry: §6" + tournament.entryFee + " §7coins",
                        "§7Reward: §6" + tournament.rewardCoins + " §7coins",
                        "§7Status: " + (tournament.status.equals("registering") ? "§aRegistering" : "§cActive"),
                        "",
                        tournament.status.equals("registering") ? "§aClick to join" : "§cIn progress"
                ));
                item.setItemMeta(meta);
            }
            gui.setItem(slot, item);
            slot++;
        }

        if (tournaments.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "No tournaments available");
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

    public Tournament getTournament(String name) {
        return tournaments.get(name);
    }

    public Collection<Tournament> getAllTournaments() {
        return tournaments.values();
    }

    public String getPlayerTournament(UUID uuid) {
        return playerTournaments.get(uuid);
    }

    public static class Tournament {
        public String name;
        public String type;
        public int maxPlayers;
        public int entryFee;
        public int rewardCoins;
        public String status;
        public long startTime;
        public List<UUID> players = new ArrayList<>();
        public List<UUID> alive = new ArrayList<>();
        public Map<UUID, Integer> eliminations = new HashMap<>();
    }
}
