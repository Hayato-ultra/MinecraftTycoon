package com.generator.gui;

import com.generator.GeneratorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class TeleportItem implements Listener {

    private static final int SLOT = 8;
    private static final String ITEM_NAME = ChatColor.AQUA + "" + ChatColor.BOLD + "Teleporter";
    private static final String LORE_KEY = "§7§k TELEPORT_COMPASS";

    private final GeneratorPlugin plugin;
    private final Set<Inventory> managedInventories = Collections.synchronizedSet(new HashSet<>());
    private final Map<Inventory, String> inventoryTitles = new HashMap<>();

    public TeleportItem(GeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    public void giveTeleportItem(Player player) {
        ItemStack existing = player.getInventory().getItem(SLOT);
        if (isTeleportItem(existing)) return;
        if (existing != null && existing.getType() != Material.AIR) {
            player.getInventory().addItem(existing);
        }
        ItemStack item = createTeleportItem();
        player.getInventory().setItem(SLOT, item);
    }

    private ItemStack createTeleportItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ITEM_NAME);
            meta.setLore(Arrays.asList(
                    LORE_KEY,
                    "",
                    "§7Right-click to open",
                    "§7Teleport menu",
                    "",
                    "§eTeleport between worlds",
                    "§eJoin friends' islands"
            ));
            meta.setCustomModelData(9999);
            meta.setUnbreakable(true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isTeleportItem(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getLore() == null) return false;
        return meta.getLore().contains(LORE_KEY);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            ItemStack current = player.getInventory().getItem(SLOT);
            if (!isTeleportItem(current)) {
                giveTeleportItem(player);
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ItemStack current = player.getInventory().getItem(SLOT);
            if (!isTeleportItem(current)) {
                giveTeleportItem(player);
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ItemStack current = player.getInventory().getItem(SLOT);
            if (!isTeleportItem(current)) {
                giveTeleportItem(player);
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getPlayer().getInventory().getItemInMainHand() != null &&
                isTeleportItem(event.getPlayer().getInventory().getItemInMainHand())) {
            if (event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isTeleportItem(item)) return;

        event.setCancelled(true);

        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            openTeleportMenu(player);
        }
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        if (isTeleportItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot drop the Teleporter!");
        }
    }

    @EventHandler
    public void onTeleporterItemMove(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory() != player.getInventory()) return;
        if (event.getSlot() == SLOT) {
            ItemStack clicked = event.getCurrentItem();
            if (isTeleportItem(clicked)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onTeleporterItemDrag(InventoryDragEvent event) {
        if (event.getRawSlots().contains(SLOT) && event.getView().getBottomInventory() == event.getInventory()) {
            if (event.getCursor() != null && isTeleportItem(event.getCursor())) {
                event.setCancelled(true);
            }
        }
    }

    public void openTeleportMenu(Player player) {
        Inventory gui = createManaged(45, "TELEPORT_MENU", player);

        gui.setItem(4, createItem(Material.NETHER_STAR,
                ChatColor.AQUA + "" + ChatColor.BOLD + "TELEPORT"));

        gui.setItem(10, createItem(Material.GRASS_BLOCK,
                ChatColor.GREEN + "SkyBlock",
                "",
                "§7Visit islands & hub",
                "§7Click to open SkyBlock menu"));

        gui.setItem(11, createItem(Material.GRAVEL,
                ChatColor.AQUA + "OneBlock",
                "",
                "§7One block challenge",
                "§7Click to teleport"));

        gui.setItem(12, createItem(Material.OAK_LOG,
                ChatColor.WHITE + "SMP World",
                "",
                "§7Free survival",
                "§7Click to teleport"));

        List<String> pvpWorlds = plugin.getConfigManager().getPvpWorlds();
        int pvpSlot = 14;
        Material[] pvpMats = {Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.GOLDEN_SWORD, Material.STONE_SWORD};
        for (int i = 0; i < pvpWorlds.size() && pvpSlot <= 16; i++) {
            Material mat = i < pvpMats.length ? pvpMats[i] : Material.IRON_SWORD;
            gui.setItem(pvpSlot, createItem(mat,
                    ChatColor.RED + "PvP - " + pvpWorlds.get(i),
                    "",
                    "§7PvP arena world",
                    "§7Click to teleport"));
            pvpSlot++;
        }

        gui.setItem(16, createItem(Material.OBSIDIAN,
                ChatColor.DARK_PURPLE + "Nether Hub",
                "",
                "§7Access nether portals",
                "§7Click to teleport"));

        gui.setItem(28, createItem(Material.PLAYER_HEAD,
                ChatColor.YELLOW + "Friends",
                "",
                "§7Visit friends' islands",
                "§7Click to open friends list"));

        gui.setItem(29, createItem(Material.RED_BED,
                ChatColor.GREEN + "My Home",
                "",
                "§7Teleport to your /home",
                "§7Click to teleport"));

        gui.setItem(30, createItem(Material.COMPASS,
                ChatColor.GOLD + "Warps",
                "",
                "§7Server warp points",
                "§7Click to view warps"));

        gui.setItem(31, createItem(Material.ENDER_PEARL,
                ChatColor.LIGHT_PURPLE + "Last Location",
                "",
                "§7Return to previous location",
                "§7Click to teleport"));

        gui.setItem(40, createItem(Material.BARRIER,
                ChatColor.RED + "Close"));

        player.openInventory(gui);
    }

    public void openSkyBlockMenu(Player player) {
        Inventory gui = createManaged(45, "SKYBLOCK_MENU", player);

        gui.setItem(4, createItem(Material.GRASS_BLOCK,
                ChatColor.GREEN + "" + ChatColor.BOLD + "SKYBLOCK"));

        gui.setItem(10, createItem(Material.BEACON,
                ChatColor.AQUA + "SkyBlock Hub",
                "",
                "§7Visit the SkyBlock spawn",
                "§7Central hub area",
                "§7Click to teleport"));

        gui.setItem(12, createItem(Material.RED_BED,
                ChatColor.GREEN + "My Island",
                "",
                "§7Go to your own island",
                "§7Click to teleport"));

        gui.setItem(14, createItem(Material.ENDER_EYE,
                ChatColor.LIGHT_PURPLE + "Visit Islands",
                "",
                "§7Visit other players' islands",
                "§7Click to see online players"));

        gui.setItem(16, createItem(Material.PAINTING,
                ChatColor.GOLD + "Island Top",
                "",
                "§7Visit the top islands",
                "§7Highest rated islands"));

        gui.setItem(28, createItem(Material.BOOK,
                ChatColor.YELLOW + "Island Settings",
                "",
                "§7Manage your island",
                "§7Trust, settings, info"));

        gui.setItem(31, createItem(Material.BARRIER,
                ChatColor.RED + "Close"));

        gui.setItem(36, createItem(Material.ARROW,
                ChatColor.YELLOW + "Back to Teleport Menu"));

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    private void openVisitIslands(Player player) {
        Inventory gui = createManaged(54, "VISIT_ISLANDS", player);

        gui.setItem(4, createItem(Material.ENDER_EYE,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "VISIT ISLANDS"));

        int slot = 10;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) continue;
            if (slot >= 44) break;
            if (slot % 9 == 8) slot += 2;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = head.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + online.getName());
                meta.setLore(java.util.Arrays.asList(
                        "",
                        "§7Click to visit island",
                        "§7World: " + (online.getWorld() != null ? online.getWorld().getName() : "unknown")
                ));
                head.setItemMeta(meta);
            }
            gui.setItem(slot, head);
            slot++;
        }

        if (slot == 10) {
            gui.setItem(22, createItem(Material.BARRIER,
                    ChatColor.RED + "No players online to visit"));
        }

        gui.setItem(49, createItem(Material.ARROW,
                ChatColor.YELLOW + "Back"));

        player.openInventory(gui);
    }

    private void openIslandSettings(Player player) {
        Inventory gui = createManaged(27, "ISLAND_SETTINGS", player);

        gui.setItem(4, createItem(Material.BOOK,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "ISLAND SETTINGS"));

        gui.setItem(10, createItem(Material.PLAYER_HEAD,
                ChatColor.GREEN + "Trust Player",
                "",
                "§7Allow a player to build",
                "§7on your island"));

        gui.setItem(12, createItem(Material.BARRIER,
                ChatColor.RED + "Untrust Player",
                "",
                "§7Remove a player's access",
                "§7from your island"));

        gui.setItem(14, createItem(Material.PAPER,
                ChatColor.AQUA + "Island Info",
                "",
                "§7View island level",
                "§7Members & settings"));

        gui.setItem(16, createItem(Material.ARROW,
                ChatColor.YELLOW + "Back"));

        player.openInventory(gui);
    }

    private void openFriendsList(Player player) {
        Inventory gui = createManaged(27, "FRIENDS_LIST", player);

        gui.setItem(4, createItem(Material.PLAYER_HEAD,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "FRIENDS"));

        int slot = 10;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) continue;
            if (slot >= 17) break;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = head.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + online.getName());
                meta.setLore(Arrays.asList(
                        "",
                        "§7World: §f" + online.getWorld().getName(),
                        "§7Click to teleport",
                        "",
                        "§e" + online.getGameMode().name()
                ));
                head.setItemMeta(meta);
            }
            gui.setItem(slot, head);
            slot++;
        }

        if (slot == 10) {
            gui.setItem(13, createItem(Material.BARRIER,
                    ChatColor.RED + "No friends online",
                    "",
                    "§7Invite friends to join the server!"));
        }

        gui.setItem(22, createItem(Material.ARROW,
                ChatColor.YELLOW + "Back to Teleport Menu"));

        player.openInventory(gui);
    }

    private void openWarpsList(Player player) {
        Inventory gui = createManaged(27, "WARPS_LIST", player);

        gui.setItem(4, createItem(Material.COMPASS,
                ChatColor.GOLD + "" + ChatColor.BOLD + "WARPS"));

        gui.setItem(10, createItem(Material.DIAMOND,
                ChatColor.AQUA + "Spawn",
                "",
                "§7Server spawn point"));

        gui.setItem(11, createItem(Material.GOLD_INGOT,
                ChatColor.GOLD + "Shop",
                "",
                "§7Main shop"));

        gui.setItem(12, createItem(Material.ENCHANTING_TABLE,
                ChatColor.LIGHT_PURPLE + "Enchant",
                "",
                "§7Enchanting area"));

        gui.setItem(13, createItem(Material.ANVIL,
                ChatColor.GRAY + "Anvil",
                "",
                "§7Anvil area"));

        gui.setItem(14, createItem(Material.SMITHING_TABLE,
                ChatColor.RED + "Smith",
                "",
                "§7Smithing area"));

        gui.setItem(15, createItem(Material.ENDER_CHEST,
                ChatColor.DARK_PURPLE + "Ender Chest",
                "",
                "§7Public ender chest"));

        gui.setItem(16, createItem(Material.WHEAT,
                ChatColor.GREEN + "Farm",
                "",
                "§7Community farm"));

        gui.setItem(22, createItem(Material.ARROW,
                ChatColor.YELLOW + "Back to Teleport Menu"));

        player.openInventory(gui);
    }

    @EventHandler
    public void onGuiInventoryClick(InventoryClickEvent event) {
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
                case "TELEPORT_MENU" -> handleTeleportMenuClick(player, item);
                case "SKYBLOCK_MENU" -> handleSkyBlockMenuClick(player, item);
                case "VISIT_ISLANDS" -> handleVisitIslandsClick(player, item);
                case "ISLAND_SETTINGS" -> handleIslandSettingsClick(player, item);
                case "FRIENDS_LIST" -> handleFriendsListClick(player, item);
                case "WARPS_LIST" -> handleWarpsListClick(player, item);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("TeleportItem GUI error: " + e.getMessage());
            player.closeInventory();
        }
    }

    private void handleTeleportMenuClick(Player player, ItemStack item) {
        Material type = item.getType();

        if (type == Material.GRASS_BLOCK) {
            openSkyBlockMenu(player);
        } else if (type == Material.GRAVEL) {
            teleportTo(player, "oneblock_world");
        } else         if (type == Material.OAK_LOG) {
            teleportTo(player, "smp");
        } else if (type.name().contains("SWORD") && item.getItemMeta() != null &&
                item.getItemMeta().getDisplayName().contains("PvP - ")) {
            String worldName = ChatColor.stripColor(item.getItemMeta().getDisplayName()).replace("PvP - ", "");
            teleportTo(player, worldName);
        } else if (type == Material.OBSIDIAN) {
            teleportTo(player, "world_nether");
        } else if (type == Material.PLAYER_HEAD) {
            openFriendsList(player);
        } else if (type == Material.RED_BED) {
            player.closeInventory();
            player.performCommand("home");
        } else if (type == Material.COMPASS) {
            openWarpsList(player);
        } else if (type == Material.ENDER_PEARL) {
            player.closeInventory();
            player.performCommand("back");
        } else if (type == Material.BARRIER) {
            player.closeInventory();
        }
    }

    private void handleFriendsListClick(Player player, ItemStack item) {
        if (item.getType() == Material.ARROW) {
            openTeleportMenu(player);
            return;
        }
        if (item.getType() == Material.PLAYER_HEAD && item.getItemMeta() != null) {
            String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            Player target = Bukkit.getPlayer(name);
            if (target != null && target.isOnline()) {
                player.closeInventory();
                player.teleport(target.getLocation());
                player.sendMessage(ChatColor.GREEN + "Teleported to " + name + "!");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            } else {
                player.sendMessage(ChatColor.RED + name + " is not online!");
            }
        }
    }

    private void handleSkyBlockMenuClick(Player player, ItemStack item) {
        Material type = item.getType();

        if (type == Material.BEACON) {
            teleportTo(player, "bskyblock_world");
        } else if (type == Material.RED_BED) {
            player.closeInventory();
            player.performCommand("home");
        } else if (type == Material.ENDER_EYE) {
            openVisitIslands(player);
        } else if (type == Material.PAINTING) {
            player.closeInventory();
            player.sendMessage(ChatColor.GOLD + "Island Top coming soon!");
        } else if (type == Material.BOOK) {
            openIslandSettings(player);
        } else if (type == Material.BARRIER) {
            player.closeInventory();
        } else if (type == Material.ARROW) {
            openTeleportMenu(player);
        }
    }

    private void handleVisitIslandsClick(Player player, ItemStack item) {
        if (item.getType() == Material.ARROW) {
            openSkyBlockMenu(player);
            return;
        }
        if (item.getType() == Material.PLAYER_HEAD && item.getItemMeta() != null) {
            String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            Player target = Bukkit.getPlayer(name);
            if (target != null && target.isOnline()) {
                player.closeInventory();
                player.teleport(target.getLocation());
                player.sendMessage(ChatColor.GREEN + "Visiting " + name + "'s island!");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            } else {
                player.sendMessage(ChatColor.RED + name + " is not online!");
            }
        }
    }

    private void handleIslandSettingsClick(Player player, ItemStack item) {
        Material type = item.getType();

        if (type == Material.ARROW) {
            openSkyBlockMenu(player);
        } else if (type == Material.PLAYER_HEAD) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Use /island trust <player> to trust someone");
        } else if (type == Material.BARRIER) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Use /island untrust <player> to untrust someone");
        } else if (type == Material.PAPER) {
            player.closeInventory();
            player.performCommand("island info");
        }
    }

    private void handleWarpsListClick(Player player, ItemStack item) {
        if (item.getType() == Material.ARROW) {
            openTeleportMenu(player);
            return;
        }

        switch (item.getType()) {
            case DIAMOND -> teleportTo(player, "lobby");
            case GOLD_INGOT -> teleportTo(player, "lobby");
            case ENCHANTING_TABLE -> teleportTo(player, "lobby");
            case ANVIL -> teleportTo(player, "lobby");
            case SMITHING_TABLE -> teleportTo(player, "lobby");
            case ENDER_CHEST -> teleportTo(player, "lobby");
            case WHEAT -> teleportTo(player, "lobby");
            default -> {}
        }
    }

    private void teleportTo(Player player, String worldName) {
        if (worldName == null || worldName.isEmpty() || !worldName.matches("^[a-zA-Z0-9_-]+$")) {
            player.sendMessage(ChatColor.RED + "Invalid world name!");
            return;
        }
        if (Bukkit.getWorld(worldName) == null) {
            player.sendMessage(ChatColor.RED + "World '" + worldName + "' not found!");
            return;
        }
        player.closeInventory();
        player.performCommand("mvtp " + worldName);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }

    @EventHandler
    public void onGuiInventoryDrag(InventoryDragEvent event) {
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
