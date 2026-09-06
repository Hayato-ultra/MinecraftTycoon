package com.generator.shop;

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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class TradeManager implements Listener {

    private final GeneratorPlugin plugin;
    private final Set<Inventory> managedInventories = Collections.synchronizedSet(new HashSet<>());
    private final Map<Inventory, String> inventoryTitles = Collections.synchronizedMap(new HashMap<>());
    private final Map<UUID, TradeRequest> pendingRequests = new HashMap<>();
    private final Map<UUID, Inventory> playerTradeInventories = new HashMap<>();

    public TradeManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendTradeRequest(Player sender, Player target) {
        if (sender.equals(target)) {
            sender.sendMessage(ChatColor.RED + "You cannot trade with yourself!");
            return;
        }

        pendingRequests.put(target.getUniqueId(), new TradeRequest(sender.getUniqueId(), System.currentTimeMillis()));
        sender.sendMessage(ChatColor.GREEN + "Trade request sent to " + target.getName() + "!");
        target.sendMessage(ChatColor.GREEN + sender.getName() + " wants to trade with you!");
        target.sendMessage(ChatColor.YELLOW + "Type /trade accept to accept, or /trade decline to decline.");
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
    }

    public void acceptTrade(Player player) {
        TradeRequest request = pendingRequests.remove(player.getUniqueId());
        if (request == null) {
            player.sendMessage(ChatColor.RED + "No pending trade request!");
            return;
        }

        Player sender = Bukkit.getPlayer(request.senderUUID);
        if (sender == null || !sender.isOnline()) {
            player.sendMessage(ChatColor.RED + "That player is no longer online!");
            return;
        }

        long elapsed = System.currentTimeMillis() - request.timestamp;
        if (elapsed > 120000) {
            player.sendMessage(ChatColor.RED + "Trade request expired!");
            return;
        }

        openTradeGUI(sender, player);
    }

    public void declineTrade(Player player) {
        TradeRequest request = pendingRequests.remove(player.getUniqueId());
        if (request == null) {
            player.sendMessage(ChatColor.RED + "No pending trade request!");
            return;
        }

        Player sender = Bukkit.getPlayer(request.senderUUID);
        if (sender != null && sender.isOnline()) {
            sender.sendMessage(ChatColor.RED + player.getName() + " declined your trade request.");
        }
        player.sendMessage(ChatColor.YELLOW + "Trade declined.");
    }

    private void openTradeGUI(Player player1, Player player2) {
        Inventory gui1 = createManaged(54, "TRADE", player1);
        Inventory gui2 = createManaged(54, "TRADE", player2);

        playerTradeInventories.put(player1.getUniqueId(), gui1);
        playerTradeInventories.put(player2.getUniqueId(), gui2);

        gui1.setItem(4, createItem(Material.EMERALD,
                ChatColor.GOLD + "" + ChatColor.BOLD + "TRADE",
                "", "§7" + player1.getName() + " <-> " + player2.getName(),
                "", "§7Your offer: slots 0-3", "§7Their offer: slots 5-8"));

        gui1.setItem(0, createItem(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "Your Offer (1-4)"));
        gui1.setItem(1, createItem(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "Your Offer (2-4)"));
        gui1.setItem(2, createItem(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "Your Offer (3-4)"));
        gui1.setItem(3, createItem(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "Your Offer (4-4)"));

        gui1.setItem(5, createItem(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "Their Offer (1-4)"));
        gui1.setItem(6, createItem(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "Their Offer (2-4)"));
        gui1.setItem(7, createItem(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "Their Offer (3-4)"));
        gui1.setItem(8, createItem(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "Their Offer (4-4)"));

        gui1.setItem(13, createItem(Material.NETHER_STAR,
                ChatColor.YELLOW + "CONFIRM TRADE",
                "", "§7Place items, then click to confirm"));

        gui1.setItem(49, createItem(Material.BARRIER,
                ChatColor.RED + "Cancel Trade"));

        gui2.setItem(4, createItem(Material.EMERALD,
                ChatColor.GOLD + "" + ChatColor.BOLD + "TRADE",
                "", "§7" + player1.getName() + " <-> " + player2.getName(),
                "", "§7Your offer: slots 5-8", "§7Their offer: slots 0-3"));

        gui2.setItem(0, createItem(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "Their Offer (1-4)"));
        gui2.setItem(1, createItem(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "Their Offer (2-4)"));
        gui2.setItem(2, createItem(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "Their Offer (3-4)"));
        gui2.setItem(3, createItem(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "Their Offer (4-4)"));

        gui2.setItem(5, createItem(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "Your Offer (1-4)"));
        gui2.setItem(6, createItem(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "Your Offer (2-4)"));
        gui2.setItem(7, createItem(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "Your Offer (3-4)"));
        gui2.setItem(8, createItem(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "Your Offer (4-4)"));

        gui2.setItem(13, createItem(Material.NETHER_STAR,
                ChatColor.YELLOW + "CONFIRM TRADE",
                "", "§7Place items, then click to confirm"));

        gui2.setItem(49, createItem(Material.BARRIER,
                ChatColor.RED + "Cancel Trade"));

        player1.openInventory(gui1);
        player2.openInventory(gui2);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory topInv = event.getView().getTopInventory();
        if (topInv == null || !managedInventories.contains(topInv)) return;

        String title = inventoryTitles.getOrDefault(topInv, "");
        if (!title.equals("TRADE")) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot >= topInv.getSize()) return;

        try {
            if (slot == 49) {
                player.closeInventory();
                player.sendMessage(ChatColor.RED + "Trade cancelled.");
                return;
            }

            if (slot == 13) {
                player.sendMessage(ChatColor.YELLOW + "Trade confirm coming soon! Place items in your offer slots.");
                return;
            }

            if (slot >= 0 && slot <= 8 && slot != 4) {
                event.setCancelled(false);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("TradeManager error: " + e.getMessage());
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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        pendingRequests.remove(uuid);
        Iterator<Map.Entry<UUID, TradeRequest>> it = pendingRequests.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().senderUUID.equals(uuid)) {
                it.remove();
            }
        }
        Inventory inv = playerTradeInventories.remove(uuid);
        if (inv != null) {
            managedInventories.remove(inv);
            inventoryTitles.remove(inv);
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

    private static class TradeRequest {
        final UUID senderUUID;
        final long timestamp;

        TradeRequest(UUID senderUUID, long timestamp) {
            this.senderUUID = senderUUID;
            this.timestamp = timestamp;
        }
    }
}
