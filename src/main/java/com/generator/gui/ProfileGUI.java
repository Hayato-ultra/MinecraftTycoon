package com.generator.gui;

import com.generator.GeneratorPlugin;
import com.generator.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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

import java.text.SimpleDateFormat;
import java.util.*;

public class ProfileGUI implements Listener {

    private final GeneratorPlugin plugin;
    private final Set<Inventory> managedInventories = Collections.synchronizedSet(new HashSet<>());
    private final Map<Inventory, String> inventoryTitles = new HashMap<>();

    public ProfileGUI(GeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    public void openProfile(Player player) {
        openProfile(player, player);
    }

    public void openProfile(Player viewer, Player target) {
        Inventory gui = createManaged(45, "PROFILE:" + target.getUniqueId().toString(), viewer);

        PlayerProfile profile = plugin.getProfileManager().getProfile(target.getUniqueId());
        if (profile == null) {
            profile = new PlayerProfile(target.getUniqueId(), target.getName());
        }

        // Player Head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(target);
            skullMeta.setDisplayName("§6§l" + target.getName());
            skullMeta.setLore(Arrays.asList(
                    "§7Click to view more info",
                    "§7UUID: " + target.getUniqueId().toString().substring(0, 8) + "..."
            ));
            head.setItemMeta(skullMeta);
        }
        gui.setItem(4, head);

        // Stats
        gui.setItem(10, createItem(Material.EXPERIENCE_BOTTLE,
                "§aLevel: " + profile.getLevel(),
                "§7Your current level"));

        gui.setItem(11, createItem(Material.GOLD_INGOT,
                "§6Coins Earned: " + String.format("%,d", profile.getTotalCoinsEarned()),
                "§7Total coins earned"));

        gui.setItem(12, createItem(Material.DIAMOND,
                "§bPlaytime: " + formatPlaytime(profile.getTotalPlaytimeMinutes()),
                "§7Total time played"));

        // Generator Stats
        gui.setItem(14, createItem(Material.FURNACE,
                "§eGenerators Placed: " + profile.getGeneratorsPlaced(),
                "§7Total generators placed"));

        gui.setItem(15, createItem(Material.HOPPER,
                "§eGenerators Collected: " + profile.getGeneratorsCollected(),
                "§7Total collections"));

        gui.setItem(16, createItem(Material.ANVIL,
                "§eGenerators Upgraded: " + profile.getGeneratorsUpgraded(),
                "§7Total upgrades"));

        // Block Stats
        gui.setItem(19, createItem(Material.STONE,
                "§7Blocks Broken: " + String.format("%,d", profile.getBlocksBroken()),
                "§7Total blocks mined"));

        gui.setItem(20, createItem(Material.OAK_PLANKS,
                "§7Blocks Placed: " + String.format("%,d", profile.getBlocksPlaced()),
                "§7Total blocks placed"));

        // Join Info
        gui.setItem(22, createItem(Material.CLOCK,
                "§7First Join: " + formatDate(profile.getFirstJoin()),
                "§7Last Join: " + formatDate(profile.getLastJoin())));

        // Actions
        gui.setItem(31, createItem(Material.RED_BED,
                "§cSet Home",
                "§7Click to set your home"));

        gui.setItem(32, createItem(Material.COMPASS,
                "§eTeleport Home",
                "§7Click to teleport to your home"));

        // Close
        gui.setItem(40, createItem(Material.BARRIER,
                "§cClose"));

        viewer.openInventory(gui);
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
        if (!title.startsWith("PROFILE:")) return;

        try {
            UUID targetUuid = UUID.fromString(title.substring(8));
            Player target = Bukkit.getPlayer(targetUuid);

            if (item.getType() == Material.RED_BED) {
                player.performCommand("sethome");
                player.closeInventory();
            } else if (item.getType() == Material.COMPASS) {
                player.performCommand("home");
                player.closeInventory();
            } else if (item.getType() == Material.BARRIER) {
                player.closeInventory();
            }
        } catch (Exception e) {
            player.closeInventory();
        }
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

    private String formatPlaytime(int minutes) {
        if (minutes < 60) return minutes + "m";
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (hours < 24) return hours + "h " + mins + "m";
        int days = hours / 24;
        int hrs = hours % 24;
        return days + "d " + hrs + "h " + mins + "m";
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
        return sdf.format(new Date(timestamp));
    }
}
