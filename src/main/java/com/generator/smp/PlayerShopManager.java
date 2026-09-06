package com.generator.smp;

import com.generator.GeneratorPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerShopManager implements Listener {

    private final GeneratorPlugin plugin;
    private final File shopFile;
    private final Map<String, PlayerShop> shops = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> playerShops = new ConcurrentHashMap<>();
    private static final Set<Inventory> managedInventories = Collections.synchronizedSet(new HashSet<>());
    private static final Map<Inventory, String> inventoryTitles = Collections.synchronizedMap(new HashMap<>());

    private static final int MAX_SHOPS_PER_PLAYER = 3;
    private static final int SHOP_CREATE_COST = 1000;

    public PlayerShopManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
        this.shopFile = new File(plugin.getDataFolder(), "player_shops.yml");
        loadShops();
    }

    private void loadShops() {
        shops.clear();
        playerShops.clear();
        if (!shopFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(shopFile);
        ConfigurationSection section = config.getConfigurationSection("shops");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection cs = section.getConfigurationSection(key);
            if (cs == null) continue;

            PlayerShop shop = new PlayerShop();
            shop.id = key;
            shop.name = cs.getString("name", key);
            shop.owner = UUID.fromString(cs.getString("owner", ""));
            shop.world = cs.getString("world", "world");
            shop.x = cs.getInt("x");
            shop.y = cs.getInt("y");
            shop.z = cs.getInt("z");
            shop.itemName = cs.getString("item", "STONE");
            shop.buyPrice = cs.getInt("buy-price", 0);
            shop.sellPrice = cs.getInt("sell-price", 0);
            shop.stock = cs.getInt("stock", 0);
            shop.maxStock = cs.getInt("max-stock", 64);

            Material mat = Material.matchMaterial(shop.itemName);
            if (mat != null) {
                shop.item = mat;
                shops.put(key, shop);
                playerShops.computeIfAbsent(shop.owner, k -> new HashSet<>()).add(key);
            }
        }
    }

    public void saveShops() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, PlayerShop> entry : shops.entrySet()) {
            PlayerShop shop = entry.getValue();
            String path = "shops." + shop.id;
            config.set(path + ".name", shop.name);
            config.set(path + ".owner", shop.owner.toString());
            config.set(path + ".world", shop.world);
            config.set(path + ".x", shop.x);
            config.set(path + ".y", shop.y);
            config.set(path + ".z", shop.z);
            config.set(path + ".item", shop.itemName);
            config.set(path + ".buy-price", shop.buyPrice);
            config.set(path + ".sell-price", shop.sellPrice);
            config.set(path + ".stock", shop.stock);
            config.set(path + ".max-stock", shop.maxStock);
        }

        try {
            config.save(shopFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player_shops.yml: " + e.getMessage());
        }
    }

    public boolean createShop(Player player, String name, Material item, int buyPrice, int sellPrice) {
        UUID uuid = player.getUniqueId();
        Set<String> existing = playerShops.getOrDefault(uuid, Collections.emptySet());
        if (existing.size() >= MAX_SHOPS_PER_PLAYER) {
            player.sendMessage(ChatColor.RED + "You can only have " + MAX_SHOPS_PER_PLAYER + " shops!");
            return false;
        }

        if (!plugin.getCoinManager().hasCoins(player, SHOP_CREATE_COST)) {
            player.sendMessage(ChatColor.RED + "Not enough coins! Need " + SHOP_CREATE_COST + " to create a shop.");
            return false;
        }

        Block block = player.getTargetBlockExact(5);
        if (block == null || block.getType() != Material.CHEST) {
            player.sendMessage(ChatColor.RED + "Look at a chest to create a shop!");
            return false;
        }

        String shopId = uuid.toString().substring(0, 8) + "_" + name.toLowerCase().replace(" ", "_");

        PlayerShop shop = new PlayerShop();
        shop.id = shopId;
        shop.name = name;
        shop.owner = uuid;
        shop.world = block.getWorld().getName();
        shop.x = block.getX();
        shop.y = block.getY();
        shop.z = block.getZ();
        shop.item = item;
        shop.itemName = item.name();
        shop.buyPrice = buyPrice;
        shop.sellPrice = sellPrice;
        shop.stock = 0;
        shop.maxStock = 64;

        shops.put(shopId, shop);
        playerShops.computeIfAbsent(uuid, k -> new HashSet<>()).add(shopId);
        plugin.getCoinManager().removeCoins(player, SHOP_CREATE_COST);
        saveShops();

        player.sendMessage(ChatColor.GREEN + "Shop created: " + name);
        player.sendMessage(ChatColor.YELLOW + "Buy: " + buyPrice + " coins | Sell: " + sellPrice + " coins");
        return true;
    }

    public boolean deleteShop(Player player, String shopId) {
        PlayerShop shop = shops.get(shopId);
        if (shop == null) return false;
        if (!shop.owner.equals(player.getUniqueId())) return false;

        shops.remove(shopId);
        playerShops.getOrDefault(player.getUniqueId(), Collections.emptySet()).remove(shopId);
        saveShops();

        player.sendMessage(ChatColor.GREEN + "Shop deleted!");
        return true;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) return;

        String shopId = getShopAt(block.getLocation());
        if (shopId == null) return;

        PlayerShop shop = shops.get(shopId);
        if (shop == null) return;

        event.setCancelled(true);
        openShopGUI(player, shop);
    }

    private void openShopGUI(Player player, PlayerShop shop) {
        Inventory gui = Bukkit.createInventory(null, 27, "SHOP:" + shop.id);
        managedInventories.add(gui);
        inventoryTitles.put(gui, "SHOP:" + shop.id);

        gui.setItem(4, createItem(shop.item,
                ChatColor.GOLD + shop.name,
                "",
                "§7Owner: §f" + Bukkit.getOfflinePlayer(shop.owner).getName(),
                "§7Item: §f" + shop.itemName,
                "",
                "§7Stock: §f" + shop.stock + "/" + shop.maxStock));

        if (shop.buyPrice > 0) {
            gui.setItem(11, createItem(Material.GREEN_STAINED_GLASS_PANE,
                    ChatColor.GREEN + "Buy for " + shop.buyPrice + " coins",
                    "", "§7Click to buy 1 " + shop.itemName));
        }

        if (shop.sellPrice > 0) {
            gui.setItem(15, createItem(Material.RED_STAINED_GLASS_PANE,
                    ChatColor.RED + "Sell for " + shop.sellPrice + " coins",
                    "", "§7Click to sell 1 " + shop.itemName));
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
        if (title == null || !title.startsWith("SHOP:")) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= topInventory.getSize()) return;

        String shopId = title.substring(5);
        PlayerShop shop = shops.get(shopId);
        if (shop == null) return;

        switch (slot) {
            case 11 -> {
                if (shop.buyPrice > 0 && shop.stock > 0) {
                    if (plugin.getCoinManager().hasCoins(player, shop.buyPrice)) {
                        plugin.getCoinManager().removeCoins(player, shop.buyPrice);
                        player.getInventory().addItem(new ItemStack(shop.item, 1));
                        shop.stock--;
                        saveShops();
                        player.sendMessage(ChatColor.GREEN + "Bought 1 " + shop.itemName + " for " + shop.buyPrice + " coins!");
                        openShopGUI(player, shop);
                    } else {
                        player.sendMessage(ChatColor.RED + "Not enough coins!");
                    }
                }
            }
            case 15 -> {
                if (shop.sellPrice > 0) {
                    ItemStack item = new ItemStack(shop.item);
                    if (player.getInventory().containsAtLeast(item, 1)) {
                        player.getInventory().removeItem(item);
                        plugin.getCoinManager().addCoins(player, shop.sellPrice);
                        shop.stock++;
                        saveShops();
                        player.sendMessage(ChatColor.GREEN + "Sold 1 " + shop.itemName + " for " + shop.sellPrice + " coins!");
                        openShopGUI(player, shop);
                    } else {
                        player.sendMessage(ChatColor.RED + "You don't have any " + shop.itemName + "!");
                    }
                }
            }
            case 22 -> player.closeInventory();
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST) return;

        String shopId = getShopAt(block.getLocation());
        if (shopId == null) return;

        PlayerShop shop = shops.get(shopId);
        if (shop == null) return;

        if (!shop.owner.equals(player.getUniqueId()) && !player.hasPermission("generators.admin")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot break this shop chest!");
        } else {
            shops.remove(shopId);
            playerShops.getOrDefault(shop.owner, Collections.emptySet()).remove(shopId);
            saveShops();
            player.sendMessage(ChatColor.YELLOW + "Shop destroyed!");
        }
    }

    private String getShopAt(Location loc) {
        for (Map.Entry<String, PlayerShop> entry : shops.entrySet()) {
            PlayerShop shop = entry.getValue();
            if (shop.world.equals(loc.getWorld().getName()) &&
                    shop.x == loc.getBlockX() && shop.y == loc.getBlockY() && shop.z == loc.getBlockZ()) {
                return shop.id;
            }
        }
        return null;
    }

    public List<PlayerShop> getPlayerShops(UUID uuid) {
        Set<String> shopIds = playerShops.getOrDefault(uuid, Collections.emptySet());
        List<PlayerShop> result = new ArrayList<>();
        for (String id : shopIds) {
            PlayerShop shop = shops.get(id);
            if (shop != null) result.add(shop);
        }
        return result;
    }

    public List<PlayerShop> getAllShops() {
        return new ArrayList<>(shops.values());
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

    public static class PlayerShop {
        public String id;
        public String name;
        public UUID owner;
        public String world;
        public int x, y, z;
        public Material item;
        public String itemName;
        public int buyPrice;
        public int sellPrice;
        public int stock;
        public int maxStock;
    }
}
