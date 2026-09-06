package com.generator.events;

import com.generator.GeneratorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SeasonalRewardManager {

    private final GeneratorPlugin plugin;
    private final SeasonManager seasonManager;
    private final Set<Inventory> managedInventories = Collections.synchronizedSet(new HashSet<>());
    private final Map<Inventory, String> inventoryTitles = Collections.synchronizedMap(new HashMap<>());

    public SeasonalRewardManager(GeneratorPlugin plugin, SeasonManager seasonManager) {
        this.plugin = plugin;
        this.seasonManager = seasonManager;
    }

    public void claimSeasonRewards(Player player) {
        SeasonManager.SeasonPlayerData data = seasonManager.getPlayerData(player.getUniqueId());
        if (data.rewardsClaimed) {
            player.sendMessage(ChatColor.RED + "You already claimed your season rewards!");
            return;
        }

        int tier = getRewardTier(data);
        if (tier == 0) {
            player.sendMessage(ChatColor.RED + "You need at least 100 XP to claim season rewards!");
            return;
        }

        SeasonReward reward = getReward(tier);
        if (reward == null) return;

        seasonManager.claimReward(player.getUniqueId());
        plugin.getCoinManager().addCoins(player, reward.coins);

        for (ItemStack item : reward.items) {
            player.getInventory().addItem(item);
        }

        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "SEASON REWARDS CLAIMED!");
        player.sendMessage(ChatColor.YELLOW + "Tier: " + reward.tierName);
        player.sendMessage(ChatColor.GREEN + "+" + reward.coins + " coins");
        player.sendMessage(ChatColor.GREEN + "+" + reward.items.size() + " items");
    }

    private int getRewardTier(SeasonManager.SeasonPlayerData data) {
        if (data.xpEarned >= 5000) return 5;
        if (data.xpEarned >= 2500) return 4;
        if (data.xpEarned >= 1000) return 3;
        if (data.xpEarned >= 500) return 2;
        if (data.xpEarned >= 100) return 1;
        return 0;
    }

    private SeasonReward getReward(int tier) {
        SeasonReward reward = new SeasonReward();
        reward.tier = tier;

        switch (tier) {
            case 1 -> {
                reward.tierName = "Bronze";
                reward.coins = 100;
                reward.items = Arrays.asList(
                        new ItemStack(Material.IRON_INGOT, 16),
                        new ItemStack(Material.BREAD, 32)
                );
            }
            case 2 -> {
                reward.tierName = "Silver";
                reward.coins = 250;
                reward.items = Arrays.asList(
                        new ItemStack(Material.DIAMOND, 4),
                        new ItemStack(Material.GOLD_INGOT, 32),
                        new ItemStack(Material.ENDER_PEARL, 2)
                );
            }
            case 3 -> {
                reward.tierName = "Gold";
                reward.coins = 500;
                reward.items = Arrays.asList(
                        new ItemStack(Material.DIAMOND, 8),
                        new ItemStack(Material.EMERALD, 16),
                        new ItemStack(Material.NETHER_STAR, 1)
                );
            }
            case 4 -> {
                reward.tierName = "Platinum";
                reward.coins = 1000;
                reward.items = Arrays.asList(
                        new ItemStack(Material.DIAMOND_BLOCK, 2),
                        new ItemStack(Material.NETHERITE_INGOT, 4),
                        new ItemStack(Material.ELYTRA, 1)
                );
            }
            case 5 -> {
                reward.tierName = "Diamond";
                reward.coins = 2500;
                reward.items = Arrays.asList(
                        new ItemStack(Material.NETHERITE_BLOCK, 1),
                        new ItemStack(Material.DIAMOND_BLOCK, 4),
                        new ItemStack(Material.ELYTRA, 1),
                        new ItemStack(Material.SHULKER_BOX, 1),
                        new ItemStack(Material.TOTEM_OF_UNDYING, 1)
                );
            }
        }
        return reward;
    }

    public void openRewardGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "SEASON_REWARDS");
        managedInventories.add(gui);
        inventoryTitles.put(gui, "SEASON_REWARDS");

        SeasonManager.SeasonPlayerData data = seasonManager.getPlayerData(player.getUniqueId());
        int tier = getRewardTier(data);

        gui.setItem(4, createItem(Material.NETHER_STAR,
                ChatColor.GOLD + "Season Rewards",
                "",
                "§7Your XP: §f" + data.xpEarned,
                "§7Your Tier: §f" + (tier == 0 ? "None" : getTierName(tier)),
                "",
                "§7Tier 1 (100 XP): 100 coins",
                "§7Tier 2 (500 XP): 250 coins + diamonds",
                "§7Tier 3 (1000 XP): 500 coins + nether star",
                "§7Tier 4 (2500 XP): 1000 coins + elytra",
                "§7Tier 5 (5000 XP): 2500 coins + everything"));

        for (int i = 1; i <= 5; i++) {
            boolean unlocked = tier >= i;
            Material mat = unlocked ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "Tier " + i + ": " + getTierName(i));
                meta.setLore(Arrays.asList(
                        "",
                        unlocked ? "§aUnlocked!" : "§cLocked",
                        "",
                        "§7Reward: §6" + getTierCoins(i) + " coins"
                ));
                item.setItemMeta(meta);
            }
            gui.setItem(10 + (i - 1), item);
        }

        gui.setItem(22, createItem(Material.BARRIER, ChatColor.RED + "Close"));
        player.openInventory(gui);
    }

    private String getTierName(int tier) {
        return switch (tier) {
            case 1 -> "Bronze";
            case 2 -> "Silver";
            case 3 -> "Gold";
            case 4 -> "Platinum";
            case 5 -> "Diamond";
            default -> "None";
        };
    }

    private int getTierCoins(int tier) {
        return switch (tier) {
            case 1 -> 100;
            case 2 -> 250;
            case 3 -> 500;
            case 4 -> 1000;
            case 5 -> 2500;
            default -> 0;
        };
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

    public static class SeasonReward {
        public int tier;
        public String tierName;
        public int coins;
        public List<ItemStack> items;
    }
}
