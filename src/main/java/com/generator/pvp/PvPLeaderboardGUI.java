package com.generator.pvp;

import com.generator.GeneratorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class PvPLeaderboardGUI implements Listener {

    private final GeneratorPlugin plugin;
    private final PvPStatsManager statsManager;
    private final PvPRankManager rankManager;
    private final PvPSeasonManager seasonManager;
    private final Set<Inventory> managedInventories = Collections.synchronizedSet(new HashSet<>());
    private final Map<Inventory, String> inventoryTitles = Collections.synchronizedMap(new HashMap<>());

    public PvPLeaderboardGUI(GeneratorPlugin plugin, PvPStatsManager statsManager,
                             PvPRankManager rankManager, PvPSeasonManager seasonManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.rankManager = rankManager;
        this.seasonManager = seasonManager;
    }

    public void openLeaderboard(Player player, String type) {
        Inventory gui = Bukkit.createInventory(null, 54, "PVP_LEADERBOARD:" + type);
        managedInventories.add(gui);
        inventoryTitles.put(gui, "PVP_LEADERBOARD:" + type);

        List<PvPStatsManager.PvPStats> topPlayers = switch (type.toLowerCase()) {
            case "kills" -> statsManager.getTopKillers(45);
            case "wins" -> statsManager.getTopWinners(45);
            case "season" -> statsManager.getSeasonTopPlayers(45);
            default -> statsManager.getTopPlayers(45);
        };

        int slot = 0;
        for (PvPStatsManager.PvPStats stats : topPlayers) {
            if (slot >= 45) break;

            PvPStatsManager.PvPStats fullStats = statsManager.getStats(stats.uuid);
            PvPRankManager.PvPRank rank = rankManager.getRank(fullStats.elo);

            Material mat;
            if (slot == 0) mat = Material.NETHERITE_BLOCK;
            else if (slot == 1) mat = Material.DIAMOND_BLOCK;
            else if (slot == 2) mat = Material.GOLD_BLOCK;
            else mat = Material.IRON_BLOCK;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String name = Bukkit.getOfflinePlayer(stats.uuid).getName();
                meta.setDisplayName(rank.color + "#" + (slot + 1) + " " + name);

                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§7Rank: " + rank.chatColor + rank.name);
                lore.add("§7ELO: §f" + fullStats.elo);

                switch (type.toLowerCase()) {
                    case "kills" -> {
                        lore.add("§7Kills: §f" + fullStats.kills);
                        lore.add("§7Deaths: §f" + fullStats.deaths);
                        lore.add("§7KDR: §f" + String.format("%.2f", fullStats.getKDR()));
                    }
                    case "wins" -> {
                        lore.add("§7Wins: §f" + fullStats.wins);
                        lore.add("§7Losses: §f" + fullStats.losses);
                        lore.add("§7Win Rate: §f" + String.format("%.1f%%", fullStats.getWinRate()));
                    }
                    case "season" -> {
                        lore.add("§7Season ELO: §f" + fullStats.seasonElo);
                        lore.add("§7Season Kills: §f" + fullStats.seasonKills);
                        lore.add("§7Season KDR: §f" + String.format("%.2f", fullStats.getSeasonKDR()));
                    }
                    default -> {
                        lore.add("§7ELO: §f" + fullStats.elo);
                        lore.add("§7Highest ELO: §f" + fullStats.highestElo);
                        lore.add("§7Progress: " + rankManager.getProgressBar(fullStats.elo));
                    }
                }

                lore.add("");
                lore.add("§7Games Played: §f" + fullStats.gamesPlayed);
                lore.add("§7Kill Streak: §f" + fullStats.highestKillStreak);

                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            gui.setItem(slot, item);
            slot++;
        }

        if (topPlayers.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "No data yet");
                meta.setLore(Arrays.asList("", "§7Play some PvP to see rankings!"));
                empty.setItemMeta(meta);
            }
            gui.setItem(22, empty);
        }

        gui.setItem(45, createItem(Material.ARROW, ChatColor.YELLOW + "← ELO"));
        gui.setItem(46, createItem(Material.ARROW, ChatColor.YELLOW + "← Kills"));
        gui.setItem(47, createItem(Material.ARROW, ChatColor.YELLOW + "← Wins"));
        gui.setItem(48, createItem(Material.ARROW, ChatColor.YELLOW + "← Season"));
        gui.setItem(53, createItem(Material.BARRIER, ChatColor.RED + "Close"));

        player.openInventory(gui);
    }

    public void openStatsGUI(Player player, Player target) {
        Inventory gui = Bukkit.createInventory(null, 27, "PVP_STATS:" + target.getUniqueId());
        managedInventories.add(gui);
        inventoryTitles.put(gui, "PVP_STATS:" + target.getUniqueId());

        PvPStatsManager.PvPStats stats = statsManager.getStats(target.getUniqueId());
        PvPRankManager.PvPRank rank = rankManager.getRank(stats.elo);

        gui.setItem(4, createItem(rank.icon,
                rank.chatColor + target.getName() + "'s Stats",
                "",
                "§7Rank: " + rank.chatColor + rank.name,
                "§7ELO: §f" + stats.elo,
                "§7Highest ELO: §f" + stats.highestElo,
                "§7Progress: " + rankManager.getProgressBar(stats.elo)));

        gui.setItem(10, createItem(Material.IRON_SWORD,
                ChatColor.RED + "Combat Stats",
                "",
                "§7Kills: §f" + stats.kills,
                "§7Deaths: §f" + stats.deaths,
                "§7KDR: §f" + String.format("%.2f", stats.getKDR()),
                "§7Assists: §f" + stats.assists));

        gui.setItem(12, createItem(Material.GOLD_INGOT,
                ChatColor.GOLD + "Win/Loss",
                "",
                "§7Wins: §f" + stats.wins,
                "§7Losses: §f" + stats.losses,
                "§7Win Rate: §f" + String.format("%.1f%%", stats.getWinRate()),
                "§7Games: §f" + stats.gamesPlayed));

        gui.setItem(14, createItem(Material.DIAMOND,
                ChatColor.AQUA + "Streaks",
                "",
                "§7Current Kill Streak: §f" + stats.killStreak,
                "§7Highest Kill Streak: §f" + stats.highestKillStreak,
                "§7Death Streak: §f" + stats.deathStreak));

        gui.setItem(16, createItem(Material.BOW,
                ChatColor.GREEN + "Special",
                "",
                "§7Headshots: §f" + stats.headshots,
                "§7First Bloods: §f" + stats.firstBloods,
                "§7Total Damage: §f" + String.format("%.1f", stats.totalDamageDealt),
                "§7Damage Taken: §f" + String.format("%.1f", stats.totalDamageTaken)));

        gui.setItem(22, createItem(Material.BARRIER, ChatColor.RED + "Close"));

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory == null) return;
        String title = inventoryTitles.get(topInventory);
        if (title == null) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= topInventory.getSize()) return;

        if (title.startsWith("PVP_LEADERBOARD:")) {
            String currentType = title.substring(16);
            switch (slot) {
                case 45 -> openLeaderboard(player, "elo");
                case 46 -> openLeaderboard(player, "kills");
                case 47 -> openLeaderboard(player, "wins");
                case 48 -> openLeaderboard(player, "season");
                case 53 -> player.closeInventory();
            }
        } else if (title.startsWith("PVP_STATS:")) {
            if (slot == 22) player.closeInventory();
        }
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
}
