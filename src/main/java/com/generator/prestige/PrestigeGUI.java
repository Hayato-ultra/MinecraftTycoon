package com.generator.prestige;

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

public class PrestigeGUI implements Listener {

    private final GeneratorPlugin plugin;
    private static final Set<Inventory> managedInventories = Collections.synchronizedSet(new HashSet<>());
    private static final Map<Inventory, String> inventoryTitles = Collections.synchronizedMap(new HashMap<>());

    private static final String PRESTIGE_MAIN = "PRESTIGE_MAIN";
    private static final String PRESTIGE_CONFIRM = "PRESTIGE_CONFIRM";
    private static final String PRESTIGE_SHOP = "PRESTIGE_SHOP";
    private static final String PRESTIGE_INFO = "PRESTIGE_INFO";

    public PrestigeGUI(GeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    public void openPrestigeMain(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, PRESTIGE_MAIN);
        managedInventories.add(gui);
        inventoryTitles.put(gui, PRESTIGE_MAIN);

        int level = plugin.getPrestigeManager().getPrestigeLevel(player.getUniqueId());
        int coins = plugin.getPrestigeManager().getPrestigeCoins(player.getUniqueId());
        double coinMult = plugin.getPrestigeManager().getCoinMultiplier(player.getUniqueId());
        double speedMult = plugin.getPrestigeManager().getGenerationSpeedMultiplier(player.getUniqueId());
        int extraHomes = plugin.getPrestigeManager().getExtraHomes(player.getUniqueId());

        gui.setItem(4, createItem(Material.NETHER_STAR,
                ChatColor.GOLD + "" + ChatColor.BOLD + "PRESTIGE SYSTEM",
                "",
                "§7Current Prestige: §6#" + level,
                "§7Prestige Coins: §6" + String.format("%,d", coins),
                "",
                "§7Permanent Bonuses:",
                "§a+§f" + String.format("%.0f%%", (coinMult - 1) * 100) + " §7coin multiplier",
                "§a+§f" + String.format("%.0f%%", (speedMult - 1) * 100) + " §7generation speed",
                "§a+" + extraHomes + " §7extra homes"));

        boolean canPrestige = plugin.getPrestigeManager().canPrestige(player);
        String failReason = plugin.getPrestigeManager().getPrestigeFailReason(player);

        if (canPrestige) {
            gui.setItem(20, createItem(Material.DRAGON_EGG,
                    ChatColor.GREEN + "" + ChatColor.BOLD + "PRESTIGE NOW",
                    "",
                    "§7Reset progress for permanent bonuses!",
                    "§7Gain §6" + String.format("%,d", 100 + (level + 1) * 50) + " §7prestige coins"));
        } else {
            gui.setItem(20, createItem(Material.BARRIER,
                    ChatColor.RED + "Cannot Prestige",
                    "",
                    "§c" + failReason));
        }

        gui.setItem(22, createItem(Material.EMERALD,
                ChatColor.AQUA + "Prestige Shop",
                "", "§7Spend prestige coins on upgrades"));

        gui.setItem(24, createItem(Material.BOOK,
                ChatColor.YELLOW + "Prestige Info",
                "", "§7View all bonuses and requirements"));

        gui.setItem(40, createItem(Material.BARRIER, ChatColor.RED + "Close"));

        player.openInventory(gui);
    }

    private void openPrestigeConfirm(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, PRESTIGE_CONFIRM);
        managedInventories.add(gui);
        inventoryTitles.put(gui, PRESTIGE_CONFIRM);

        int level = plugin.getPrestigeManager().getPrestigeLevel(player.getUniqueId());

        gui.setItem(13, createItem(Material.DRAGON_EGG,
                ChatColor.RED + "" + ChatColor.BOLD + "ARE YOU SURE?",
                "",
                "§7You are about to prestige to §6#" + (level + 1),
                "",
                "§c§lThis will reset:",
                "§7- All generators",
                "§7- All coins",
                "",
                "§a§lYou will gain:",
                "§7- Permanent coin multiplier: §a+" + String.format("%.0f%%", 10.0) + "",
                "§7- Permanent speed bonus: §a+" + String.format("%.0f%%", 5.0) + "",
                "§7- §6" + String.format("%,d", 100 + (level + 1) * 50) + " §7prestige coins"));

        gui.setItem(11, createItem(Material.GREEN_STAINED_GLASS_PANE,
                ChatColor.GREEN + "CONFIRM PRESTIGE"));

        gui.setItem(15, createItem(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "CANCEL"));

        player.openInventory(gui);
    }

    public void openPrestigeShop(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, PRESTIGE_SHOP);
        managedInventories.add(gui);
        inventoryTitles.put(gui, PRESTIGE_SHOP);

        UUID uuid = player.getUniqueId();
        int coins = plugin.getPrestigeManager().getPrestigeCoins(uuid);

        gui.setItem(4, createItem(Material.EMERALD_BLOCK,
                ChatColor.GOLD + "" + ChatColor.BOLD + "PRESTIGE SHOP",
                "", "§7Your Prestige Coins: §6" + String.format("%,d", coins)));

        String[][] bonuses = {
                {"coin_multiplier", "Coin Multiplier", "EMERALD", "+5% coin earnings per level"},
                {"speed_boost", "Speed Boost", "REDSTONE", "+5% generation speed per level"},
                {"extra_homes", "Extra Homes", "RED_BED", "+1 home per 5 prestige levels"},
                {"bonus_xp", "Bonus XP", "EXPERIENCE_BOTTLE", "+10% XP gain per level"},
                {"bonus_chest", "Bonus Chest", "CHEST", "+1 bonus chest slot per level"}
        };

        int slot = 10;
        for (String[] bonus : bonuses) {
            if (slot >= 17) break;

            Material mat = Material.matchMaterial(bonus[2]);
            if (mat == null) mat = Material.PAPER;

            int currentLevel = plugin.getPrestigeManager().getBonusLevel(uuid, bonus[0]);
            int cost = plugin.getPrestigeManager().getBonusCost(uuid, bonus[0]);
            boolean canBuy = coins >= cost;

            ChatColor color = canBuy ? ChatColor.GREEN : ChatColor.RED;

            gui.setItem(slot, createItem(mat,
                    color + bonus[1],
                    "",
                    "§7" + bonus[3],
                    "§7Level: §f" + currentLevel,
                    "§7Cost: " + (canBuy ? "§a" : "§c") + String.format("%,d", cost) + " prestige coins",
                    "",
                    canBuy ? "§eClick to purchase" : "§cNot enough coins"));
            slot++;
        }

        gui.setItem(40, createItem(Material.BARRIER, ChatColor.RED + "Close"));
        player.openInventory(gui);
    }

    public void openPrestigeInfo(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, PRESTIGE_INFO);
        managedInventories.add(gui);
        inventoryTitles.put(gui, PRESTIGE_INFO);

        UUID uuid = player.getUniqueId();
        int level = plugin.getPrestigeManager().getPrestigeLevel(uuid);
        double coinMult = plugin.getPrestigeManager().getCoinMultiplier(uuid);
        double speedMult = plugin.getPrestigeManager().getGenerationSpeedMultiplier(uuid);
        int extraHomes = plugin.getPrestigeManager().getExtraHomes(uuid);

        gui.setItem(4, createItem(Material.BOOK,
                ChatColor.GOLD + "" + ChatColor.BOLD + "PRESTIGE INFO"));

        gui.setItem(10, createItem(Material.NETHER_STAR,
                ChatColor.GREEN + "Current Bonuses",
                "",
                "§7Prestige Level: §6#" + level,
                "",
                "§aCoin Multiplier: §f" + String.format("%.0f%%", (coinMult - 1) * 100),
                "§aGeneration Speed: §f" + String.format("%.0f%%", (speedMult - 1) * 100),
                "§aExtra Homes: §f+" + extraHomes));

        gui.setItem(12, createItem(Material.PAPER,
                ChatColor.YELLOW + "Requirements",
                "",
                "§7Island Level: §6≥ 50",
                "§7Coins: §6≥ 1,000,000"));

        gui.setItem(14, createItem(Material.DIAMOND,
                ChatColor.AQUA + "Benefits",
                "",
                "§7+10% coin multiplier per level",
                "§7+5% generation speed per level",
                "§7+100 prestige coins per prestige",
                "§7+50 bonus coins per prestige level"));

        gui.setItem(16, createItem(Material.EMERALD,
                ChatColor.LIGHT_PURPLE + "Prestige Levels",
                "",
                "§7Max Level: §f100",
                "§7Current: §6#" + level,
                "§7Remaining: §f" + (100 - level)));

        List<Map.Entry<UUID, Integer>> top = plugin.getPrestigeManager().getTopPrestige(5);
        StringBuilder topText = new StringBuilder();
        for (int i = 0; i < top.size(); i++) {
            UUID topUuid = top.get(i).getKey();
            int topLevel = top.get(i).getValue();
            String name = Bukkit.getOfflinePlayer(topUuid).getName();
            if (name == null) name = topUuid.toString().substring(0, 8);
            topText.append("§7").append(i + 1).append(". ").append(name).append(": §6#").append(topLevel);
            if (i < top.size() - 1) topText.append("\n");
        }

        gui.setItem(31, createItem(Material.GOLD_BLOCK,
                ChatColor.GOLD + "Leaderboard",
                topText.toString().split("\n")));

        gui.setItem(40, createItem(Material.BARRIER, ChatColor.RED + "Close"));
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
            case PRESTIGE_MAIN -> {
                switch (slot) {
                    case 20 -> {
                        if (plugin.getPrestigeManager().canPrestige(player)) {
                            openPrestigeConfirm(player);
                        }
                    }
                    case 22 -> openPrestigeShop(player);
                    case 24 -> openPrestigeInfo(player);
                    case 40 -> player.closeInventory();
                }
            }
            case PRESTIGE_CONFIRM -> {
                switch (slot) {
                    case 11 -> {
                        if (plugin.getPrestigeManager().prestige(player)) {
                            player.closeInventory();
                            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                            int newLevel = plugin.getPrestigeManager().getPrestigeLevel(player.getUniqueId());
                            int coins = plugin.getPrestigeManager().getPrestigeCoins(player.getUniqueId());
                            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "PRESTIGE #" + newLevel + "!");
                            player.sendMessage(ChatColor.GREEN + "You gained permanent bonuses and " +
                                    String.format("%,d", coins) + " prestige coins!");
                            Bukkit.broadcastMessage(ChatColor.GOLD + player.getName() + " just prestiged to #" + newLevel + "!");
                        }
                    }
                    case 15 -> openPrestigeMain(player);
                }
            }
            case PRESTIGE_SHOP -> {
                if (slot == 40) {
                    player.closeInventory();
                    return;
                }

                String[] bonusIds = {"coin_multiplier", "speed_boost", "extra_homes", "bonus_xp", "bonus_chest"};
                int bonusIndex = slot - 10;
                if (bonusIndex >= 0 && bonusIndex < bonusIds.length) {
                    String bonusId = bonusIds[bonusIndex];
                    if (plugin.getPrestigeManager().purchaseBonus(player, bonusId)) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                        player.sendMessage(ChatColor.GREEN + "Purchased bonus: " + bonusId + "!");
                        openPrestigeShop(player);
                    } else {
                        player.sendMessage(ChatColor.RED + "Not enough prestige coins!");
                    }
                }
            }
            case PRESTIGE_INFO -> {
                if (slot == 40) player.closeInventory();
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
