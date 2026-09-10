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

import java.util.*;

public class MenuManager implements Listener {

    private final GeneratorPlugin plugin;
    private final Set<Inventory> managedInventories = Collections.synchronizedSet(new HashSet<>());
    private final Map<Inventory, String> inventoryTitles = Collections.synchronizedMap(new HashMap<>());

    public MenuManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Inventory gui = createManaged(45, "SERVER_MENU", player);

        gui.setItem(4, createItem(Material.NETHER_STAR,
                ChatColor.GOLD + "" + ChatColor.BOLD + "SERVER MENU",
                ChatColor.GRAY + "Navigate to game modes and features"));

        // Game Modes
        gui.setItem(10, createItem(Material.GRASS_BLOCK,
                ChatColor.GREEN + "SkyBlock",
                ChatColor.GRAY + "Build your island empire",
                ChatColor.GRAY + "Earn coins, buy generators",
                "",
                ChatColor.YELLOW + "Click to join"));

        gui.setItem(11, createItem(Material.GRAVEL,
                ChatColor.AQUA + "OneBlock",
                ChatColor.GRAY + "Start from a single block",
                ChatColor.GRAY + "Progress through phases",
                "",
                ChatColor.YELLOW + "Click to join"));

        gui.setItem(12, createItem(Material.OAK_LOG,
                ChatColor.WHITE + "SMP",
                ChatColor.GRAY + "Survival Multiplayer",
                ChatColor.GRAY + "Free-build with friends",
                "",
                ChatColor.YELLOW + "Click to join"));

        List<String> pvpWorlds = plugin.getConfigManager().getPvpWorlds();
        int pvpSlot = 14;
        Material[] pvpMats = {Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.GOLDEN_SWORD, Material.STONE_SWORD};
        for (int i = 0; i < pvpWorlds.size() && pvpSlot <= 16; i++) {
            Material mat = i < pvpMats.length ? pvpMats[i] : Material.IRON_SWORD;
            gui.setItem(pvpSlot, createItem(mat,
                    ChatColor.RED + "PvP - " + pvpWorlds.get(i),
                    ChatColor.GRAY + "PvP arena world",
                    "",
                    ChatColor.YELLOW + "Click to join"));
            pvpSlot++;
        }

        // Info & Social
        gui.setItem(29, createItem(Material.BOOK,
                ChatColor.YELLOW + "Quests",
                ChatColor.GRAY + "View and track quests"));

        gui.setItem(30, createItem(Material.CHEST,
                ChatColor.GOLD + "Rewards",
                ChatColor.GRAY + "Claim daily rewards"));

        gui.setItem(31, createItem(Material.PLAYER_HEAD,
                ChatColor.AQUA + "Profile",
                ChatColor.GRAY + "View your stats"));

        gui.setItem(32, createItem(Material.REDSTONE,
                ChatColor.GREEN + "Leaderboard",
                ChatColor.GRAY + "Top players"));

        gui.setItem(33, createItem(Material.EMERALD,
                ChatColor.GREEN + "Shop",
                ChatColor.GRAY + "Buy items and kits"));

        // Utilities
        gui.setItem(37, createItem(Material.COMPASS,
                ChatColor.YELLOW + "Warps",
                ChatColor.GRAY + "Teleport to locations"));

        gui.setItem(38, createItem(Material.PAPER,
                ChatColor.WHITE + "Rules",
                ChatColor.GRAY + "Server rules"));

        gui.setItem(39, createItem(Material.RED_BED,
                ChatColor.RED + "Set Home",
                ChatColor.GRAY + "Set your home location"));

        gui.setItem(40, createItem(Material.ARROW,
                ChatColor.YELLOW + "Back to Spawn"));

        gui.setItem(44, createItem(Material.BARRIER,
                ChatColor.RED + "Close"));

        player.openInventory(gui);
    }

    public void openModeSelector(Player player) {
        Inventory gui = createManaged(27, "MODE_SELECT", player);

        gui.setItem(4, createItem(Material.NETHER_STAR,
                ChatColor.GOLD + "" + ChatColor.BOLD + "SELECT MODE"));

        gui.setItem(11, createItem(Material.GRASS_BLOCK,
                ChatColor.GREEN + "SkyBlock",
                ChatColor.GRAY + "Island tycoon"));

        gui.setItem(13, createItem(Material.GRAVEL,
                ChatColor.AQUA + "OneBlock",
                ChatColor.GRAY + "One block challenge"));

        gui.setItem(15, createItem(Material.OAK_LOG,
                ChatColor.WHITE + "SMP",
                ChatColor.GRAY + "Free survival"));

        gui.setItem(22, createItem(Material.BARRIER,
                ChatColor.RED + "Close"));

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory topInv = event.getView().getTopInventory();
        if (topInv == null || !managedInventories.contains(topInv)) return;

        if (event.getRawSlot() >= topInv.getSize()) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        event.setCancelled(true);

        String title = inventoryTitles.getOrDefault(topInv, "");

        try {
            switch (title) {
                case "SERVER_MENU" -> handleMainMenuClick(player, item);
                case "MODE_SELECT" -> handleModeSelectClick(player, item);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Menu error for " + player.getName() + ": " + e.getMessage());
            player.closeInventory();
        }
    }

    private void handleMainMenuClick(Player player, ItemStack item) {
        Material type = item.getType();

        // Game Modes
        if (type == Material.GRASS_BLOCK) {
            teleportToWorld(player, "lobby");
            return;
        }
        if (type == Material.GRAVEL) {
            teleportToWorld(player, "oneblock_world");
            return;
        }
        if (type == Material.OAK_LOG) {
            teleportToWorld(player, "smp");
            return;
        }
        if (type.name().contains("SWORD") && item.getItemMeta() != null &&
                item.getItemMeta().getDisplayName().contains("PvP - ")) {
            String worldName = ChatColor.stripColor(item.getItemMeta().getDisplayName()).replace("PvP - ", "");
            teleportToWorld(player, worldName);
            return;
        }

        // Utilities
        if (type == Material.ARROW) {
            teleportToWorld(player, "lobby");
            return;
        }
        if (type == Material.BARRIER) {
            player.closeInventory();
            return;
        }
        if (type == Material.COMPASS) {
            player.sendMessage(ChatColor.YELLOW + "Use /warp to see available warps");
            player.closeInventory();
            return;
        }
        if (type == Material.PAPER) {
            sendRules(player);
            player.closeInventory();
            return;
        }
        if (type == Material.RED_BED) {
            player.performCommand("sethome");
            player.closeInventory();
            return;
        }
        if (type == Material.PLAYER_HEAD) {
            player.performCommand("profile");
            player.closeInventory();
            return;
        }
        if (type == Material.EMERALD) {
            plugin.getShopManager().openShop(player);
            return;
        }
        if (type == Material.CHEST) {
            player.performCommand("daily");
            player.closeInventory();
            return;
        }
        if (type == Material.BOOK) {
            player.sendMessage(ChatColor.YELLOW + "Quests coming soon!");
            player.closeInventory();
            return;
        }
        if (type == Material.REDSTONE) {
            player.sendMessage(ChatColor.YELLOW + "Leaderboard coming soon!");
            player.closeInventory();
            return;
        }
    }

    private void handleModeSelectClick(Player player, ItemStack item) {
        Material type = item.getType();

        if (type == Material.GRASS_BLOCK) {
            teleportToWorld(player, "lobby");
            return;
        }
        if (type == Material.GRAVEL) {
            teleportToWorld(player, "oneblock_world");
            return;
        }
        if (type == Material.OAK_LOG) {
            teleportToWorld(player, "smp");
            return;
        }
        if (type == Material.BARRIER) {
            player.closeInventory();
        }
    }

    private void teleportToWorld(Player player, String worldName) {
        player.closeInventory();
        player.performCommand("mvtp " + worldName);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }

    private void sendRules(Player player) {
        player.sendMessage(ChatColor.GREEN + "=== SERVER RULES ===");
        player.sendMessage(ChatColor.YELLOW + "1. " + ChatColor.WHITE + "Be respectful to all players");
        player.sendMessage(ChatColor.YELLOW + "2. " + ChatColor.WHITE + "No griefing or stealing");
        player.sendMessage(ChatColor.YELLOW + "3. " + ChatColor.WHITE + "No hacking or exploits");
        player.sendMessage(ChatColor.YELLOW + "4. " + ChatColor.WHITE + "No spamming or advertising");
        player.sendMessage(ChatColor.YELLOW + "5. " + ChatColor.WHITE + "Have fun!");
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
