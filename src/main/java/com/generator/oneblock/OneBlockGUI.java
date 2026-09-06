package com.generator.oneblock;

import com.generator.GeneratorPlugin;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class OneBlockGUI implements Listener {

    private final GeneratorPlugin plugin;
    private final OneBlockManager oneBlockManager;
    private final OneBlockQuestManager questManager;
    private final OneBlockEventManager eventManager;
    private static final Set<Inventory> managedInventories = Collections.synchronizedSet(new HashSet<>());
    private static final Map<Inventory, String> inventoryTitles = Collections.synchronizedMap(new HashMap<>());

    private static final String MAIN_MENU = "ONEBLOCK_MAIN";
    private static final String PHASES_MENU = "ONEBLOCK_PHASES";
    private static final String QUESTS_MENU = "ONEBLOCK_QUESTS";
    private static final String EVENTS_MENU = "ONEBLOCK_EVENTS";
    private static final String LEADERBOARD_MENU = "ONEBLOCK_LEADERBOARD";

    public OneBlockGUI(GeneratorPlugin plugin, OneBlockManager oneBlockManager,
                       OneBlockQuestManager questManager, OneBlockEventManager eventManager) {
        this.plugin = plugin;
        this.oneBlockManager = oneBlockManager;
        this.questManager = questManager;
        this.eventManager = eventManager;
    }

    public void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, MAIN_MENU);
        managedInventories.add(gui);
        inventoryTitles.put(gui, MAIN_MENU);

        OneBlockManager.OneBlockProgress progress = oneBlockManager.getProgress(player.getUniqueId());
        String currentPhase = oneBlockManager.getCurrentPhaseName(player.getUniqueId());
        int phaseProgress = oneBlockManager.getPhaseProgress(player.getUniqueId());
        int totalBroken = progress.totalBroken;
        int phasesCompleted = progress.phasesCompleted;

        gui.setItem(4, createItem(Material.GRASS_BLOCK,
                ChatColor.GREEN + "" + ChatColor.BOLD + "ONEBLOCK",
                "",
                "§7Current Phase: §6" + currentPhase,
                "§7Phase Progress: §a" + phaseProgress + "%",
                "§7Total Blocks: §f" + String.format("%,d", totalBroken),
                "§7Phases Completed: §a" + phasesCompleted));

        gui.setItem(11, createItem(Material.BOOK,
                ChatColor.AQUA + "Phases",
                "", "§7View all phases and progress"));

        gui.setItem(13, createItem(Material.WRITABLE_BOOK,
                ChatColor.YELLOW + "Quests",
                "", "§7Complete quests for rewards"));

        gui.setItem(15, createItem(Material.NETHER_STAR,
                ChatColor.GOLD + "Events",
                "", "§7View seasonal events"));

        gui.setItem(31, createItem(Material.COMPARATOR,
                ChatColor.LIGHT_PURPLE + "Leaderboard",
                "", "§7Top OneBlock players"));

        gui.setItem(40, createItem(Material.BARRIER, ChatColor.RED + "Close"));

        player.openInventory(gui);
    }

    public void openPhasesMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, PHASES_MENU);
        managedInventories.add(gui);
        inventoryTitles.put(gui, PHASES_MENU);

        gui.setItem(4, createItem(Material.NETHER_STAR,
                ChatColor.GOLD + "" + ChatColor.BOLD + "ONEBLOCK PHASES"));

        Collection<OneBlockManager.OneBlockPhase> phases = oneBlockManager.getAllPhases();
        int slot = 10;
        for (OneBlockManager.OneBlockPhase phase : phases) {
            if (slot >= 44) break;

            OneBlockManager.OneBlockProgress progress = oneBlockManager.getProgress(player.getUniqueId());
            boolean isCurrent = progress.currentPhase.equals(phase.id);
            boolean isCompleted = isPhaseCompleted(progress, phase.id);

            ChatColor color = isCompleted ? ChatColor.GREEN : (isCurrent ? ChatColor.YELLOW : ChatColor.GRAY);

            gui.setItem(slot, createItem(phase.material != null ? phase.material : Material.STONE,
                    color + phase.displayName,
                    "",
                    "§7" + phase.description,
                    "§7Blocks Required: §f" + phase.blocksRequired,
                    "§7Reward: §6" + phase.rewardCoins + " coins",
                    isCurrent ? "§a► CURRENT PHASE" : (isCompleted ? "§a✓ COMPLETED" : "")));
            slot++;
        }

        gui.setItem(49, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));
        player.openInventory(gui);
    }

    private boolean isPhaseCompleted(OneBlockManager.OneBlockProgress progress, String phaseId) {
        List<String> phaseOrder = List.of("dirt", "grass", "stone", "wood", "iron", "gold",
                "diamond", "emerald", "nether", "end", "crystal", "obsidian", "bedrock");
        int phaseIndex = phaseOrder.indexOf(phaseId);
        int currentIndex = phaseOrder.indexOf(progress.currentPhase);
        return currentIndex > phaseIndex;
    }

    public void openQuestsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, QUESTS_MENU);
        managedInventories.add(gui);
        inventoryTitles.put(gui, QUESTS_MENU);

        gui.setItem(4, createItem(Material.WRITABLE_BOOK,
                ChatColor.GOLD + "" + ChatColor.BOLD + "ONEBLOCK QUESTS"));

        Collection<OneBlockQuestManager.OneBlockQuest> quests = questManager.getAllQuests();
        int slot = 10;
        for (OneBlockQuestManager.OneBlockQuest quest : quests) {
            if (slot >= 44) break;

            boolean completed = questManager.isQuestCompleted(player.getUniqueId(), quest.id);
            int progress = questManager.getQuestProgress(player.getUniqueId(), quest.id);

            ChatColor color = completed ? ChatColor.GREEN : ChatColor.YELLOW;
            double percent = Math.min(100, (double) progress / quest.requiredAmount * 100);

            List<String> lore = new ArrayList<>();
            lore.add("§7" + quest.description);
            lore.add("§7Progress: §f" + progress + "/" + quest.requiredAmount + " §7(" + String.format("%.0f", percent) + "%)");
            lore.add("§7Reward: §6" + quest.rewardCoins + " coins");
            if (!quest.rewardItem.isEmpty()) {
                lore.add("§7Bonus: §a" + quest.rewardAmount + " " + quest.rewardItem);
            }
            lore.add("");
            lore.add(completed ? "§a✓ COMPLETED" : "§eIn Progress");

            Material mat = Material.PAPER;
            gui.setItem(slot, createItem(mat, color + quest.displayName, lore.toArray(new String[0])));
            slot++;
        }

        gui.setItem(49, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));
        player.openInventory(gui);
    }

    public void openEventsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, EVENTS_MENU);
        managedInventories.add(gui);
        inventoryTitles.put(gui, EVENTS_MENU);

        gui.setItem(4, createItem(Material.NETHER_STAR,
                ChatColor.GOLD + "" + ChatColor.BOLD + "SEASONAL EVENTS"));

        OneBlockEventManager.SeasonalEvent activeEvent = eventManager.getActiveEvent();
        if (activeEvent != null) {
            int progress = eventManager.getEventProgress(player.getUniqueId(), activeEvent.id);
            double percent = Math.min(100, (double) progress / activeEvent.requiredBlocks * 100);

            gui.setItem(13, createItem(activeEvent.material != null ? activeEvent.material : Material.PUMPKIN,
                    ChatColor.GREEN + "" + ChatColor.BOLD + activeEvent.displayName,
                    "",
                    "§7" + activeEvent.description,
                    "§7Progress: §f" + progress + "/" + activeEvent.requiredBlocks + " §7(" + String.format("%.0f", percent) + "%)",
                    "§7Bonus Multiplier: §a" + activeEvent.bonusMultiplier + "x",
                    "§7Bonus Coins: §6" + activeEvent.bonusCoins,
                    "",
                    "§aACTIVE NOW!"));
        } else {
            gui.setItem(13, createItem(Material.BARRIER,
                    ChatColor.RED + "No Active Event",
                    "",
                    "§7Check back during seasonal events!",
                    "§7Events: Halloween, Winter, Spring, Summer, New Year"));
        }

        gui.setItem(22, createItem(Material.BARRIER, ChatColor.RED + "Close"));
        player.openInventory(gui);
    }

    public void openLeaderboardMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, LEADERBOARD_MENU);
        managedInventories.add(gui);
        inventoryTitles.put(gui, LEADERBOARD_MENU);

        gui.setItem(4, createItem(Material.GOLD_BLOCK,
                ChatColor.GOLD + "" + ChatColor.BOLD + "ONEBLOCK LEADERBOARD"));

        List<Map.Entry<UUID, OneBlockManager.OneBlockProgress>> topPlayers = oneBlockManager.getTopPlayers(5);
        int slot = 10;
        for (int i = 0; i < topPlayers.size(); i++) {
            Map.Entry<UUID, OneBlockManager.OneBlockProgress> entry = topPlayers.get(i);
            UUID uuid = entry.getKey();
            OneBlockManager.OneBlockProgress progress = entry.getValue();
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name == null) name = uuid.toString().substring(0, 8);

            Material mat;
            switch (i) {
                case 0 -> mat = Material.NETHERITE_INGOT;
                case 1 -> mat = Material.DIAMOND;
                case 2 -> mat = Material.IRON_INGOT;
                default -> mat = Material.GOLD_NUGGET;
            }

            gui.setItem(slot, createItem(mat,
                    ChatColor.GOLD + "#" + (i + 1) + " " + name,
                    "",
                    "§7Total Broken: §f" + String.format("%,d", progress.totalBroken),
                    "§7Phases Completed: §a" + progress.phasesCompleted,
                    "§7Current Phase: §6" + oneBlockManager.getCurrentPhaseName(uuid)));
            slot++;
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

        switch (title) {
            case MAIN_MENU -> {
                switch (slot) {
                    case 11 -> openPhasesMenu(player);
                    case 13 -> openQuestsMenu(player);
                    case 15 -> openEventsMenu(player);
                    case 31 -> openLeaderboardMenu(player);
                    case 40 -> player.closeInventory();
                }
            }
            case PHASES_MENU -> {
                if (slot == 49) openMainMenu(player);
            }
            case QUESTS_MENU -> {
                if (slot == 49) openMainMenu(player);
            }
            case EVENTS_MENU -> {
                if (slot == 22) player.closeInventory();
            }
            case LEADERBOARD_MENU -> {
                if (slot == 22) player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        managedInventories.remove(inv);
        inventoryTitles.remove(inv);
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
