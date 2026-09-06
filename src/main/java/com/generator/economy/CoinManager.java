package com.generator.economy;

import com.generator.GeneratorPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CoinManager implements Listener {

    private final GeneratorPlugin plugin;
    private final Set<UUID> dailyClaimed = ConcurrentHashMap.newKeySet();
    private long lastDailyReset = 0;

    public CoinManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    private void checkDailyReset() {
        long now = System.currentTimeMillis();
        long today = now / 86400000L;
        if (today != lastDailyReset) {
            dailyClaimed.clear();
            lastDailyReset = today;
        }
    }

    public int getCoins(Player player) {
        EconomyHook econ = plugin.getEconomyHook();
        if (econ.hasEconomy()) {
            return (int) econ.getBalance(player);
        }
        return 0;
    }

    public boolean addCoins(Player player, int amount) {
        if (amount <= 0) return false;
        EconomyHook econ = plugin.getEconomyHook();
        if (econ.hasEconomy()) {
            double multiplier = plugin.getPrestigeManager() != null ?
                    plugin.getPrestigeManager().getCoinMultiplier(player.getUniqueId()) : 1.0;
            int finalAmount = (int) (amount * multiplier);
            econ.deposit(player, finalAmount);
            return true;
        }
        return false;
    }

    public boolean removeCoins(Player player, int amount) {
        if (amount <= 0) return false;
        EconomyHook econ = plugin.getEconomyHook();
        if (econ.hasEconomy()) {
            return econ.withdraw(player, amount);
        }
        return false;
    }

    public boolean hasCoins(Player player, int amount) {
        EconomyHook econ = plugin.getEconomyHook();
        if (econ.hasEconomy()) {
            return econ.getBalance(player) >= amount;
        }
        return false;
    }

    public boolean setCoins(Player player, int amount) {
        EconomyHook econ = plugin.getEconomyHook();
        if (econ.hasEconomy()) {
            econ.withdraw(player, econ.getBalance(player));
            if (amount > 0) econ.deposit(player, amount);
            return true;
        }
        return false;
    }

    public int getSellPrice(ItemStack item) {
        Map<String, Integer> sellPrices = plugin.getConfigManager().getSellPrices();
        String materialName = item.getType().name();
        return sellPrices.getOrDefault(materialName, 0);
    }

    public int sellInventory(Player player) {
        int totalValue = 0;
        int itemsSold = 0;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null) continue;
            if (isSpecialItem(item)) continue;

            int price = getSellPrice(item);
            if (price <= 0) continue;

            int value = Math.min((int) ((long) price * item.getAmount()), 1000000);
            totalValue += value;
            itemsSold += item.getAmount();
            player.getInventory().setItem(i, null);
        }

        if (totalValue > 0) {
            addCoins(player, totalValue);
            player.sendMessage(ChatColor.GREEN + "Sold " + itemsSold + " items for " + ChatColor.GOLD + totalValue + " coins" + ChatColor.GREEN + "!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        } else {
            player.sendMessage(ChatColor.YELLOW + "No items to sell.");
        }

        return totalValue;
    }

    public int sellItem(Player player, ItemStack item) {
        if (isSpecialItem(item)) {
            player.sendMessage(ChatColor.RED + "You cannot sell this item!");
            return 0;
        }

        int price = getSellPrice(item);
        if (price <= 0) {
            player.sendMessage(ChatColor.RED + "This item cannot be sold.");
            return 0;
        }

        int value = Math.min((int) ((long) price * item.getAmount()), 1000000);
        addCoins(player, value);
        player.getInventory().remove(item);
        player.sendMessage(ChatColor.GREEN + "Sold " + item.getAmount() + " " +
                item.getType().name().replace("_", " ").toLowerCase() +
                " for " + ChatColor.GOLD + value + " coins" + ChatColor.GREEN + "!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        return value;
    }

    public boolean canClaimDaily(Player player) {
        checkDailyReset();
        return !dailyClaimed.contains(player.getUniqueId());
    }

    public void claimDaily(Player player) {
        checkDailyReset();
        UUID uuid = player.getUniqueId();
        if (dailyClaimed.contains(uuid)) {
            player.sendMessage(ChatColor.RED + "You already claimed your daily reward today!");
            return;
        }

        Map<Integer, Map<String, Object>> rewards = plugin.getConfigManager().getDailyRewards();
        if (rewards.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Daily rewards are disabled.");
            return;
        }

        int day = (int) ((System.currentTimeMillis() / 86400000L) % 7) + 1;
        Map<String, Object> reward = rewards.get(day);
        if (reward == null) {
            player.sendMessage(ChatColor.RED + "No reward available for day " + day);
            return;
        }

        int coins = ((Number) reward.getOrDefault("coins", 0)).intValue();
        String item = (String) reward.getOrDefault("item", "");
        int itemAmount = ((Number) reward.getOrDefault("item-amount", 1)).intValue();

        if (coins > 0) {
            addCoins(player, coins);
            player.sendMessage(ChatColor.GREEN + "Received " + ChatColor.GOLD + coins + " coins" + ChatColor.GREEN + "!");
        }

        if (!item.isEmpty()) {
            try {
                org.bukkit.Material mat = org.bukkit.Material.valueOf(item);
                ItemStack rewardItem = new ItemStack(mat, itemAmount);
                HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(rewardItem);
                if (!remaining.isEmpty()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), rewardItem);
                }
                player.sendMessage(ChatColor.GREEN + "Received " + ChatColor.GOLD + itemAmount + " " +
                        mat.name().replace("_", " ").toLowerCase() + ChatColor.GREEN + "!");
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid daily reward item: " + item);
            }
        }

        dailyClaimed.add(uuid);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    private boolean isSpecialItem(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta() != null) {
            if (item.getItemMeta().hasCustomModelData()) return true;
            if (item.getItemMeta().hasDisplayName()) {
                String name = item.getItemMeta().getDisplayName();
                if (name.contains("Teleporter") || name.contains("Generator")) return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory topInv = event.getView().getTopInventory();
        if (topInv == null) return;

        String title = event.getView().getTitle();
        if (title == null) return;

        if (title.equals("SELL_CHEST")) {
            event.setCancelled(true);
            if (event.getRawSlot() < topInv.getSize()) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null) {
                    sellItem(player, clicked);
                }
            }
        }
    }
}
