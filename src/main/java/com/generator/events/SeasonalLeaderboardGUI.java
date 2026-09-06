package com.generator.events;

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

public class SeasonalLeaderboardGUI implements Listener {

    private final GeneratorPlugin plugin;
    private final SeasonManager seasonManager;
    private final EventManager eventManager;
    private final SeasonalQuestManager questManager;
    private final Set<Inventory> managedInventories = Collections.synchronizedSet(new HashSet<>());
    private final Map<Inventory, String> inventoryTitles = Collections.synchronizedMap(new HashMap<>());

    public SeasonalLeaderboardGUI(GeneratorPlugin plugin, SeasonManager seasonManager,
                                  EventManager eventManager, SeasonalQuestManager questManager) {
        this.plugin = plugin;
        this.seasonManager = seasonManager;
        this.eventManager = eventManager;
        this.questManager = questManager;
    }

    public void openLeaderboard(Player player, String type) {
        Inventory gui = Bukkit.createInventory(null, 54, "SEASON_LEADERBOARD:" + type);
        managedInventories.add(gui);
        inventoryTitles.put(gui, "SEASON_LEADERBOARD:" + type);

        List<SeasonManager.SeasonPlayerData> topPlayers = seasonManager.getSeasonLeaderboard();

        int slot = 0;
        for (SeasonManager.SeasonPlayerData data : topPlayers) {
            if (slot >= 45) break;

            Material mat;
            if (slot == 0) mat = Material.NETHERITE_BLOCK;
            else if (slot == 1) mat = Material.DIAMOND_BLOCK;
            else if (slot == 2) mat = Material.GOLD_BLOCK;
            else mat = Material.IRON_BLOCK;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "#" + (slot + 1) + " Player");

                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§7XP Earned: §f" + data.xpEarned);
                lore.add("§7Quests Completed: §f" + data.questsCompleted);
                lore.add("§7Blocks Broken: §f" + data.blocksBroken);
                lore.add("§7Mobs Killed: §f" + data.mobsKilled);
                lore.add("");
                lore.add("§7Status: " + (data.rewardsClaimed ? "§aRewards Claimed" : "§eRewards Available"));

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
                meta.setLore(Arrays.asList("", "§7Play this season to see rankings!"));
                empty.setItemMeta(meta);
            }
            gui.setItem(22, empty);
        }

        gui.setItem(45, createItem(Material.ARROW, ChatColor.YELLOW + "← Season"));
        gui.setItem(46, createItem(Material.ARROW, ChatColor.YELLOW + "← Events"));
        gui.setItem(47, createItem(Material.ARROW, ChatColor.YELLOW + "← Quests"));
        gui.setItem(53, createItem(Material.BARRIER, ChatColor.RED + "Close"));

        player.openInventory(gui);
    }

    public void openSeasonInfo(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "SEASON_INFO");

        SeasonManager.ServerSeason season = seasonManager.getCurrentSeason();
        if (season != null) {
            gui.setItem(4, createItem(Material.NETHER_STAR,
                    ChatColor.GOLD + season.name,
                    "",
                    "§7" + season.description,
                    "",
                    "§7Multiplier: §ax" + season.rewardMultiplier,
                    "§7Quest Bonus: §a+" + season.questBonus + "%",
                    "§7Time Left: §f" + seasonManager.getTimeRemainingFormatted()));

            SeasonManager.SeasonPlayerData data = seasonManager.getPlayerData(player.getUniqueId());
            gui.setItem(13, createItem(Material.EXPERIENCE_BOTTLE,
                    ChatColor.AQUA + "Your Season Progress",
                    "",
                    "§7XP Earned: §f" + data.xpEarned,
                    "§7Quests Completed: §f" + data.questsCompleted,
                    "§7Blocks Broken: §f" + data.blocksBroken,
                    "§7Mobs Killed: §f" + data.mobsKilled,
                    "",
                    data.rewardsClaimed ? "§aRewards Claimed" : "§eRewards Available"));
        } else {
            gui.setItem(13, createItem(Material.BARRIER, ChatColor.RED + "No active season"));
        }

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

        if (title.startsWith("SEASON_LEADERBOARD:")) {
            switch (slot) {
                case 45 -> openLeaderboard(player, "xp");
                case 46 -> openLeaderboard(player, "quests");
                case 47 -> openLeaderboard(player, "blocks");
                case 53 -> player.closeInventory();
            }
        } else if (title.equals("SEASON_INFO")) {
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
