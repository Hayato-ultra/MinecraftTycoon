package com.generator.shop;

import com.generator.GeneratorPlugin;
import com.generator.economy.CoinManager;
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

public class ShopManager implements Listener {

    private final GeneratorPlugin plugin;
    private final Set<Inventory> managedInventories = Collections.synchronizedSet(new HashSet<>());
    private final Map<Inventory, String> inventoryTitles = Collections.synchronizedMap(new HashMap<>());

    private static final Map<String, List<ShopItem>> SHOP_CATEGORIES = new LinkedHashMap<>();

    static {
        SHOP_CATEGORIES.put("blocks", Arrays.asList(
                new ShopItem(Material.COBBLESTONE, 2, 1),
                new ShopItem(Material.STONE, 4, 2),
                new ShopItem(Material.DEEPSLATE, 4, 2),
                new ShopItem(Material.GRANITE, 3, 1),
                new ShopItem(Material.DIORITE, 3, 1),
                new ShopItem(Material.ANDESITE, 3, 1),
                new ShopItem(Material.SAND, 5, 2),
                new ShopItem(Material.RED_SAND, 6, 3),
                new ShopItem(Material.GRAVEL, 3, 1),
                new ShopItem(Material.CLAY_BALL, 8, 4),
                new ShopItem(Material.DIRT, 1, 0),
                new ShopItem(Material.GRASS_BLOCK, 2, 1),
                new ShopItem(Material.OAK_LOG, 6, 3),
                new ShopItem(Material.BIRCH_LOG, 6, 3),
                new ShopItem(Material.SPRUCE_LOG, 8, 4),
                new ShopItem(Material.ACACIA_LOG, 6, 3),
                new ShopItem(Material.BONE_MEAL, 2, 1)
        ));

        SHOP_CATEGORIES.put("ores", Arrays.asList(
                new ShopItem(Material.COAL, 8, 4),
                new ShopItem(Material.IRON_INGOT, 20, 10),
                new ShopItem(Material.COPPER_INGOT, 12, 6),
                new ShopItem(Material.GOLD_INGOT, 35, 18),
                new ShopItem(Material.REDSTONE, 10, 5),
                new ShopItem(Material.LAPIS_LAZULI, 12, 6),
                new ShopItem(Material.DIAMOND, 80, 40),
                new ShopItem(Material.EMERALD, 60, 30),
                new ShopItem(Material.NETHERITE_INGOT, 500, 250)
        ));

        SHOP_CATEGORIES.put("food", Arrays.asList(
                new ShopItem(Material.BREAD, 5, 2),
                new ShopItem(Material.COOKED_BEEF, 12, 6),
                new ShopItem(Material.COOKED_PORKCHOP, 10, 5),
                new ShopItem(Material.COOKED_CHICKEN, 8, 4),
                new ShopItem(Material.COOKED_MUTTON, 10, 5),
                new ShopItem(Material.COOKED_COD, 8, 4),
                new ShopItem(Material.COOKED_SALMON, 10, 5),
                new ShopItem(Material.GOLDEN_APPLE, 200, 100),
                new ShopItem(Material.CHORUS_FRUIT, 30, 15),
                new ShopItem(Material.ENCHANTED_GOLDEN_APPLE, 2000, 1000)
        ));

        SHOP_CATEGORIES.put("tools", Arrays.asList(
                new ShopItem(Material.WOODEN_PICKAXE, 10, 5),
                new ShopItem(Material.STONE_PICKAXE, 25, 12),
                new ShopItem(Material.IRON_PICKAXE, 100, 50),
                new ShopItem(Material.DIAMOND_PICKAXE, 500, 250),
                new ShopItem(Material.NETHERITE_PICKAXE, 3000, 1500),
                new ShopItem(Material.WOODEN_AXE, 10, 5),
                new ShopItem(Material.STONE_AXE, 25, 12),
                new ShopItem(Material.IRON_AXE, 100, 50),
                new ShopItem(Material.DIAMOND_AXE, 500, 250),
                new ShopItem(Material.WOODEN_SHOVEL, 5, 2),
                new ShopItem(Material.STONE_SHOVEL, 12, 6),
                new ShopItem(Material.IRON_SHOVEL, 50, 25),
                new ShopItem(Material.DIAMOND_SHOVEL, 250, 125)
        ));

        SHOP_CATEGORIES.put("armor", Arrays.asList(
                new ShopItem(Material.LEATHER_HELMET, 30, 15),
                new ShopItem(Material.LEATHER_CHESTPLATE, 50, 25),
                new ShopItem(Material.LEATHER_LEGGINGS, 40, 20),
                new ShopItem(Material.LEATHER_BOOTS, 25, 12),
                new ShopItem(Material.IRON_HELMET, 150, 75),
                new ShopItem(Material.IRON_CHESTPLATE, 250, 125),
                new ShopItem(Material.IRON_LEGGINGS, 200, 100),
                new ShopItem(Material.IRON_BOOTS, 125, 62),
                new ShopItem(Material.DIAMOND_HELMET, 500, 250),
                new ShopItem(Material.DIAMOND_CHESTPLATE, 800, 400),
                new ShopItem(Material.DIAMOND_LEGGINGS, 650, 325),
                new ShopItem(Material.DIAMOND_BOOTS, 400, 200)
        ));

        SHOP_CATEGORIES.put("redstone", Arrays.asList(
                new ShopItem(Material.REDSTONE, 10, 5),
                new ShopItem(Material.REDSTONE_TORCH, 8, 4),
                new ShopItem(Material.REPEATER, 15, 8),
                new ShopItem(Material.COMPARATOR, 20, 10),
                new ShopItem(Material.PISTON, 25, 12),
                new ShopItem(Material.STICKY_PISTON, 40, 20),
                new ShopItem(Material.OBSERVER, 30, 15),
                new ShopItem(Material.HOPPER, 50, 25),
                new ShopItem(Material.DROPPER, 20, 10),
                new ShopItem(Material.DISPENSER, 25, 12),
                new ShopItem(Material.TNT, 40, 20)
        ));

        SHOP_CATEGORIES.put("decoration", Arrays.asList(
                new ShopItem(Material.TORCH, 2, 1),
                new ShopItem(Material.GLOWSTONE, 15, 8),
                new ShopItem(Material.SEA_LANTERN, 20, 10),
                new ShopItem(Material.SHROOMLIGHT, 12, 6),
                new ShopItem(Material.JACK_O_LANTERN, 10, 5),
                new ShopItem(Material.LANTERN, 15, 8),
                new ShopItem(Material.SOUL_LANTERN, 15, 8),
                new ShopItem(Material.BOOKSHELF, 30, 15),
                new ShopItem(Material.PAINTING, 10, 5),
                new ShopItem(Material.ITEM_FRAME, 15, 8),
                new ShopItem(Material.FLOWER_POT, 8, 4),
                new ShopItem(Material.RED_BED, 25, 12)
        ));

        SHOP_CATEGORIES.put("special", Arrays.asList(
                new ShopItem(Material.ENDER_PEARL, 50, 25),
                new ShopItem(Material.ENDER_EYE, 60, 30),
                new ShopItem(Material.ELYTRA, 5000, 2500),
                new ShopItem(Material.SHULKER_SHELL, 200, 100),
                new ShopItem(Material.TOTEM_OF_UNDYING, 3000, 1500),
                new ShopItem(Material.NETHER_STAR, 2000, 1000),
                new ShopItem(Material.DRAGON_EGG, 10000, 5000),
                new ShopItem(Material.BEACON, 3000, 1500),
                new ShopItem(Material.SPONGE, 100, 50)
        ));
    }

    public ShopManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    public void openShop(Player player) {
        openShopCategory(player, "main");
    }

    private void openShopCategory(Player player, String category) {
        if (category.equals("main")) {
            openShopMain(player);
        } else {
            openShopItems(player, category);
        }
    }

    private void openShopMain(Player player) {
        Inventory gui = createManaged(54, "SHOP_MAIN", player);

        gui.setItem(4, createItem(Material.EMERALD,
                ChatColor.GREEN + "" + ChatColor.BOLD + "SHOP",
                "",
                "§7Buy items with coins"));

        gui.setItem(10, createItem(Material.COBBLESTONE,
                ChatColor.WHITE + "Blocks",
                "", "§7Building blocks"));

        gui.setItem(11, createItem(Material.DIAMOND_ORE,
                ChatColor.AQUA + "Ores", "", "§7Ore materials"));

        gui.setItem(12, createItem(Material.COOKED_BEEF,
                ChatColor.GOLD + "Food", "", "§7Edible items"));

        gui.setItem(13, createItem(Material.IRON_PICKAXE,
                ChatColor.GRAY + "Tools", "", "§7Pickaxes, axes, shovels"));

        gui.setItem(14, createItem(Material.IRON_CHESTPLATE,
                ChatColor.BLUE + "Armor", "", "§7Protective gear"));

        gui.setItem(15, createItem(Material.REDSTONE,
                ChatColor.RED + "Redstone", "", "§7Redstone components"));

        gui.setItem(16, createItem(Material.TORCH,
                ChatColor.YELLOW + "Decoration", "", "§7Decorative blocks"));

        gui.setItem(22, createItem(Material.NETHER_STAR,
                ChatColor.LIGHT_PURPLE + "Special", "", "§7Rare items"));

        gui.setItem(29, createItem(Material.GOLD_INGOT,
                ChatColor.GOLD + "Sell Items",
                "", "§7Sell your items for coins"));

        gui.setItem(31, createItem(Material.CHEST,
                ChatColor.GREEN + "Admin Shop",
                "", "§7Free items (admin)"));

        gui.setItem(33, createItem(Material.CHEST,
                ChatColor.AQUA + "Kits",
                "", "§7Starter & claim kits"));

        gui.setItem(49, createItem(Material.BARRIER,
                ChatColor.RED + "Close"));

        player.openInventory(gui);
    }

    private void openShopItems(Player player, String category) {
        List<ShopItem> items = SHOP_CATEGORIES.get(category);
        if (items == null) return;

        int size = ((items.size() / 9) + 2) * 9;
        size = Math.min(size, 54);
        Inventory gui = createManaged(size, "SHOP_CAT:" + category, player);

        gui.setItem(4, createItem(Material.EMERALD,
                ChatColor.GREEN + category.toUpperCase() + " SHOP"));

        int slot = 9;
        for (ShopItem shopItem : items) {
            if (slot >= size - 9) break;

            ItemStack display = new ItemStack(shopItem.material);
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + shopItem.material.name().replace("_", " "));
                meta.setLore(Arrays.asList(
                        "",
                        "§7Buy Price: §6" + shopItem.buyPrice + " coins",
                        "§7Sell Price: §a" + shopItem.sellPrice + " coins",
                        "",
                        "§eLeft-click: Buy 1",
                        "§eShift-left-click: Buy 16",
                        "§eRight-click: Sell 1",
                        "§eShift-right-click: Sell all"
                ));
                display.setItemMeta(meta);
            }
            gui.setItem(slot, display);
            slot++;
        }

        gui.setItem(size - 5, createItem(Material.ARROW, ChatColor.YELLOW + "Back to Shop"));

        player.openInventory(gui);
    }

    public void openAdminShop(Player player) {
        if (!player.hasPermission("generators.admin")) {
            player.sendMessage(ChatColor.RED + "No permission!");
            return;
        }

        Inventory gui = createManaged(54, "ADMIN_SHOP", player);

        gui.setItem(4, createItem(Material.NETHER_STAR,
                ChatColor.RED + "" + ChatColor.BOLD + "ADMIN SHOP",
                "", "§7All items are FREE"));

        List<Material> adminItems = Arrays.asList(
                Material.COBBLESTONE, Material.STONE, Material.DEEPSLATE,
                Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND,
                Material.EMERALD, Material.NETHERITE_INGOT,
                Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
                Material.IRON_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
                Material.IRON_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE,
                Material.ENDER_PEARL, Material.ELYTRA, Material.TOTEM_OF_UNDYING,
                Material.NETHER_STAR, Material.BEACON, Material.SHULKER_SHELL,
                Material.REDSTONE, Material.REPEATER, Material.COMPARATOR,
                Material.TORCH, Material.GLOWSTONE, Material.SEA_LANTERN,
                Material.BREAD, Material.COOKED_BEEF, Material.GOLDEN_APPLE,
                Material.TNT, Material.HOPPER, Material.CHEST
        );

        int slot = 9;
        for (Material mat : adminItems) {
            if (slot >= 45) break;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + mat.name().replace("_", " "));
                meta.setLore(Arrays.asList("", "§7FREE - Click to get 64"));
                item.setItemMeta(meta);
            }
            gui.setItem(slot, item);
            slot++;
        }

        gui.setItem(49, createItem(Material.BARRIER, ChatColor.RED + "Close"));

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
            if (title.equals("SHOP_MAIN")) {
                handleShopMainClick(player, item);
            } else if (title.startsWith("SHOP_CAT:")) {
                handleShopItemClick(player, event, title.substring(9));
            } else if (title.equals("ADMIN_SHOP")) {
                handleAdminShopClick(player, item);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("ShopManager error: " + e.getMessage());
            player.closeInventory();
        }
    }

    private void handleShopMainClick(Player player, ItemStack item) {
        Material type = item.getType();

        if (type == Material.COBBLESTONE) openShopItems(player, "blocks");
        else if (type == Material.DIAMOND_ORE || type == Material.DIAMOND) openShopItems(player, "ores");
        else if (type == Material.COOKED_BEEF) openShopItems(player, "food");
        else if (type == Material.IRON_PICKAXE) openShopItems(player, "tools");
        else if (type == Material.IRON_CHESTPLATE) openShopItems(player, "armor");
        else if (type == Material.REDSTONE) openShopItems(player, "redstone");
        else if (type == Material.TORCH) openShopItems(player, "decoration");
        else if (type == Material.NETHER_STAR) openShopItems(player, "special");
        else if (type == Material.GOLD_INGOT) {
            plugin.getCoinManager().sellInventory(player);
            player.closeInventory();
        }
        else if (type == Material.CHEST && item.getItemMeta() != null &&
                item.getItemMeta().getDisplayName().contains("Admin Shop")) {
            openAdminShop(player);
        }
        else if (type == Material.CHEST && item.getItemMeta() != null &&
                item.getItemMeta().getDisplayName().contains("Kits")) {
            plugin.getKitManager().openKitGUI(player);
        }
        else if (type == Material.BARRIER) player.closeInventory();
    }

    private void handleShopItemClick(Player player, InventoryClickEvent event, String category) {
        if (event.getCurrentItem().getType() == Material.ARROW) {
            openShopMain(player);
            return;
        }

        Material clickedType = event.getCurrentItem().getType();
        List<ShopItem> items = SHOP_CATEGORIES.get(category);
        if (items == null) return;

        ShopItem shopItem = null;
        for (ShopItem si : items) {
            if (si.material == clickedType) {
                shopItem = si;
                break;
            }
        }
        if (shopItem == null) return;

        CoinManager coins = plugin.getCoinManager();
        boolean shift = event.isShiftClick();

        if (player.getOpenInventory().getCursor().getType() != Material.AIR) {
            player.sendMessage(ChatColor.RED + "Clear your cursor first!");
            return;
        }

        if (event.isLeftClick()) {
            int amount = shift ? 16 : 1;
            int cost = shopItem.buyPrice * amount;
            if (!coins.hasCoins(player, cost)) {
                player.sendMessage(ChatColor.RED + "Not enough coins! Need " + cost + " coins.");
                return;
            }
            coins.removeCoins(player, cost);
            ItemStack purchase = new ItemStack(clickedType, amount);
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(purchase);
            if (!overflow.isEmpty()) {
                for (ItemStack drop : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                long refund = overflow.values().stream().mapToLong(ItemStack::getAmount).sum() * (long) shopItem.buyPrice;
                coins.addCoins(player, (int) Math.min(refund, Integer.MAX_VALUE));
                player.sendMessage(ChatColor.YELLOW + "Some items dropped on ground!");
            }
            player.sendMessage(ChatColor.GREEN + "Bought " + amount + " " + clickedType.name().replace("_", " ") + " for " + cost + " coins!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        } else {
            int amount = shift ? countItem(player, clickedType) : 1;
            if (amount <= 0) {
                player.sendMessage(ChatColor.RED + "No items to sell!");
                return;
            }
            int removed = removeItem(player, clickedType, amount);
            int value = shopItem.sellPrice * removed;
            coins.addCoins(player, value);
            player.sendMessage(ChatColor.GREEN + "Sold " + removed + " " + clickedType.name().replace("_", " ") + " for " + value + " coins!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        }
    }

    private void handleAdminShopClick(Player player, ItemStack item) {
        if (item.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (item.getType() != Material.AIR && item.getType() != Material.EMERALD) {
            ItemStack free = new ItemStack(item.getType(), 64);
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(free);
            if (!overflow.isEmpty()) {
                for (ItemStack drop : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
            player.sendMessage(ChatColor.GREEN + "Received 64x " + item.getType().name().replace("_", " ") + "!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    private int countItem(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private int removeItem(Player player, Material material, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == material) {
                int remove = Math.min(remaining, item.getAmount());
                item.setAmount(item.getAmount() - remove);
                remaining -= remove;
                if (item.getAmount() <= 0) {
                    player.getInventory().setItem(i, null);
                }
            }
        }
        return amount - remaining;
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

    public static class ShopItem {
        public final Material material;
        public final int buyPrice;
        public final int sellPrice;

        public ShopItem(Material material, int buyPrice, int sellPrice) {
            this.material = material;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
        }
    }
}
