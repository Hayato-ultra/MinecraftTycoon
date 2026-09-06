package com.generator.gui;

import com.generator.GeneratorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.*;

public class AdminDashboard implements Listener {

    private final GeneratorPlugin plugin;
    private final Set<Inventory> managedInventories = Collections.synchronizedSet(new HashSet<>());
    private final Map<Inventory, String> inventoryTitles = new HashMap<>();

    public AdminDashboard(GeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    public void openDashboard(Player player) {
        if (!player.hasPermission("generators.admin")) {
            player.sendMessage(ChatColor.RED + "No permission!");
            return;
        }

        Inventory gui = createManaged(54, "ADMIN_DASH", player);

        gui.setItem(4, createItem(Material.NETHER_STAR,
                ChatColor.RED + "" + ChatColor.BOLD + "ADMIN DASHBOARD"));

        gui.setItem(10, createItem(Material.FURNACE,
                ChatColor.GOLD + "Generator Stats",
                "",
                "§7Total generators: §f" + plugin.getGeneratorManager().getTotalGeneratorCount()));

        gui.setItem(11, createItem(Material.PLAYER_HEAD,
                ChatColor.AQUA + "Player Stats",
                "",
                "§7Online players: §f" + Bukkit.getOnlinePlayers().size()));

        gui.setItem(12, createItem(Material.DIAMOND,
                ChatColor.GREEN + "Economy",
                "",
                "§7Total money in system",
                "§7Use /coins to check"));

        gui.setItem(13, createItem(Material.COMMAND_BLOCK,
                ChatColor.LIGHT_PURPLE + "Commands",
                "",
                "§7Quick admin commands"));

        gui.setItem(14, createItem(Material.REDSTONE,
                ChatColor.RED + "Server Info",
                "",
                "§7Server status and info"));

        gui.setItem(15, createItem(Material.BOOK,
                ChatColor.YELLOW + "Logs",
                "",
                "§7Recent server logs"));

        gui.setItem(16, createItem(Material.BARRIER,
                ChatColor.DARK_RED + "Danger Zone",
                "",
                "§7Dangerous admin actions"));

        // Quick actions
        gui.setItem(28, createItem(Material.WRITABLE_BOOK,
                ChatColor.GREEN + "Give Generator",
                "",
                "§7Right-click to give a generator"));

        gui.setItem(29, createItem(Material.EMERALD,
                ChatColor.GREEN + "Give Coins",
                "",
                "§7Right-click to give coins"));

        gui.setItem(30, createItem(Material.EXPERIENCE_BOTTLE,
                ChatColor.GREEN + "Give XP",
                "",
                "§7Right-click to give XP"));

        gui.setItem(31, createItem(Material.PAPER,
                ChatColor.GREEN + "Broadcast",
                "",
                "§7Right-click to broadcast message"));

        gui.setItem(32, createItem(Material.RED_BED,
                ChatColor.GREEN + "Set Spawn",
                "",
                "§7Right-click to set spawn"));

        gui.setItem(33, createItem(Material.WITHER_SKELETON_SKULL,
                ChatColor.RED + "Kill All Mobs",
                "",
                "§7Right-click to kill all mobs"));

        gui.setItem(34, createItem(Material.LAVA_BUCKET,
                ChatColor.RED + "Clear Drops",
                "",
                "§7Right-click to clear dropped items"));

        gui.setItem(49, createItem(Material.BARRIER,
                ChatColor.RED + "Close"));

        player.openInventory(gui);
    }

    private void openPlayerList(Player admin) {
        Inventory gui = createManaged(54, "ADMIN_PLAYERS", admin);

        gui.setItem(4, createItem(Material.PLAYER_HEAD,
                ChatColor.AQUA + "" + ChatColor.BOLD + "ONLINE PLAYERS"));

        int slot = 10;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= 44) break;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(online);
                meta.setDisplayName(ChatColor.GREEN + online.getName());
                meta.setLore(Arrays.asList(
                        "",
                        "§7World: §f" + online.getWorld().getName(),
                        "§7Health: §c" + (int) online.getHealth() + "/" + (int) online.getMaxHealth(),
                        "§7Level: §e" + online.getLevel(),
                        "",
                        "§eClick to manage"));
                head.setItemMeta(meta);
            }
            gui.setItem(slot, head);
            slot++;
        }

        gui.setItem(49, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));
        admin.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!player.hasPermission("generators.admin")) return;

        Inventory topInv = event.getView().getTopInventory();
        if (topInv == null || !managedInventories.contains(topInv)) return;

        if (event.getRawSlot() >= topInv.getSize()) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        event.setCancelled(true);

        String title = inventoryTitles.getOrDefault(topInv, "");

        try {
            switch (title) {
                case "ADMIN_DASH" -> handleDashboardClick(player, item);
                case "ADMIN_PLAYERS" -> handlePlayerListClick(player, item);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("AdminDashboard error: " + e.getMessage());
            player.closeInventory();
        }
    }

    private void handleDashboardClick(Player player, ItemStack item) {
        Material type = item.getType();

        if (type == Material.FURNACE) {
            player.performCommand("generatoradmin list");
            player.closeInventory();
        } else if (type == Material.PLAYER_HEAD) {
            openPlayerList(player);
        } else if (type == Material.DIAMOND) {
            player.performCommand("coins");
            player.closeInventory();
        } else if (type == Material.COMMAND_BLOCK) {
            sendCommandHelp(player);
            player.closeInventory();
        } else if (type == Material.REDSTONE) {
            sendServerInfo(player);
            player.closeInventory();
        } else if (type == Material.BOOK) {
            player.performCommand("gc");
            player.closeInventory();
        } else if (type == Material.BARRIER) {
            player.closeInventory();
        } else if (type == Material.WRITABLE_BOOK) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Usage: /generatoradmin give <player> <type>");
        } else if (type == Material.EMERALD) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Usage: /generatoradmin givecoins <player> <amount>");
        } else if (type == Material.EXPERIENCE_BOTTLE) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Usage: /generatoradmin givexp <player> <amount>");
        } else if (type == Material.PAPER) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Usage: /broadcast <message>");
        } else if (type == Material.RED_BED) {
            player.performCommand("setspawn");
            player.closeInventory();
        } else if (type == Material.WITHER_SKELETON_SKULL) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=!player]");
            player.sendMessage(ChatColor.GREEN + "Killed all mobs!");
            player.closeInventory();
        } else if (type == Material.LAVA_BUCKET) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=item]");
            player.sendMessage(ChatColor.GREEN + "Cleared all dropped items!");
            player.closeInventory();
        }
    }

    private void handlePlayerListClick(Player player, ItemStack item) {
        if (item.getType() == Material.ARROW) {
            openDashboard(player);
            return;
        }

        if (item.getType() == Material.PLAYER_HEAD && item.getItemMeta() != null) {
            String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            Player target = Bukkit.getPlayer(name);
            if (target != null) {
                player.closeInventory();
                player.teleport(target.getLocation());
                player.sendMessage(ChatColor.GREEN + "Teleported to " + name);
            }
        }
    }

    private void sendCommandHelp(Player player) {
        player.sendMessage(ChatColor.GREEN + "=== ADMIN COMMANDS ===");
        player.sendMessage(ChatColor.YELLOW + "/generatoradmin give <player> <type> §7- Give generator");
        player.sendMessage(ChatColor.YELLOW + "/generatoradmin giveall <type> §7- Give to all online");
        player.sendMessage(ChatColor.YELLOW + "/generatoradmin reload §7- Reload config");
        player.sendMessage(ChatColor.YELLOW + "/generatoradmin setlevel <player> <level> §7- Set level");
        player.sendMessage(ChatColor.YELLOW + "/generatoradmin inspect <player> §7- View player info");
        player.sendMessage(ChatColor.YELLOW + "/generatoradmin remove <id> §7- Remove generator");
    }

    private void sendServerInfo(Player player) {
        player.sendMessage(ChatColor.GREEN + "=== SERVER INFO ===");
        player.sendMessage(ChatColor.YELLOW + "Players: §f" + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
        player.sendMessage(ChatColor.YELLOW + "Generators: §f" + plugin.getGeneratorManager().getTotalGeneratorCount());
        player.sendMessage(ChatColor.YELLOW + "Worlds: §f" + Bukkit.getWorlds().size());
        player.sendMessage(ChatColor.YELLOW + "TPS: §f" + String.format("%.1f", Bukkit.getServer().getTPS()[0]));
        player.sendMessage(ChatColor.YELLOW + "Uptime: §f" + formatUptime());
    }

    private String formatUptime() {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        long hours = uptime / 3600000;
        long minutes = (uptime % 3600000) / 60000;
        return hours + "h " + minutes + "m";
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        if (topInv != null && managedInventories.contains(topInv)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        if (topInv != null) {
            managedInventories.remove(topInv);
            inventoryTitles.remove(topInv);
        }
    }

    private Inventory createManaged(int size, String title, Player player) {
        Inventory gui = Bukkit.createInventory(null, size, title);
        managedInventories.add(gui);
        inventoryTitles.put(gui, title);
        return gui;
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
