package com.generator.gui;

import com.generator.GeneratorPlugin;
import com.generator.config.ConfigManager;
import com.generator.economy.EconomyHook;
import com.generator.generator.Generator;
import com.generator.generator.GeneratorManager;
import com.generator.listeners.GeneratorListener;
import com.generator.util.IslandRoleManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GUIManager implements Listener {

    private final GeneratorPlugin plugin;
    private final Map<UUID, Generator> viewingGenerator = new HashMap<>();
    private final Map<UUID, String> viewingCategory = new HashMap<>();
    private final Set<Inventory> managedInventories = Collections.synchronizedSet(new HashSet<>());
    private final Map<Inventory, String> inventoryTitles = new HashMap<>();

    public GUIManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    private Inventory openManaged(int size, String title, Player player) {
        Inventory gui = Bukkit.createInventory(null, size, title);
        managedInventories.add(gui);
        inventoryTitles.put(gui, title);
        return gui;
    }

    private String getTitle(Inventory inv) {
        return inventoryTitles.getOrDefault(inv, "");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory topInv = event.getView().getTopInventory();
        if (topInv == null) return;
        if (!managedInventories.contains(topInv)) return;

        // Only cancel clicks in the GUI (top inventory), not the player's own inventory
        if (event.getRawSlot() >= topInv.getSize()) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        event.setCancelled(true);

        String title = getTitle(topInv);

        try {
            if (title.equals("MAIN")) {
                handleMainMenuClick(player, item);
            } else if (title.equals("SHOP")) {
                handleShopMenuClick(player, item);
            } else if (title.equals("MY")) {
                handleMyGeneratorsClick(player, item);
            } else if (title.startsWith("CAT:")) {
                handleCategoryMenuClick(player, item, title.substring(4));
            } else if (title.startsWith("GEN:")) {
                handleGeneratorDetailClick(player, item);
            } else if (title.equals("UPGRADE")) {
                handleUpgradeClick(player, item);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("GUI error for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
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

    // ==================== MAIN MENU ====================

    public void openMainMenu(Player player) {
        viewingCategory.remove(player.getUniqueId());
        viewingGenerator.remove(player.getUniqueId());

        Inventory gui = openManaged(45, "MAIN", player);

        ConfigManager configManager = plugin.getConfigManager();
        IslandRoleManager.Role role = IslandRoleManager.getPlayerRole(player, player.getLocation());

        gui.setItem(4, createItem(Material.NETHER_STAR, ChatColor.GOLD + "" + ChatColor.BOLD + "GENERATORS",
                ChatColor.GRAY + "Browse and manage your generators",
                "",
                ChatColor.AQUA + "Island Role: " + ChatColor.WHITE + role.name.toUpperCase(),
                ChatColor.YELLOW + "Total: " + ChatColor.WHITE + plugin.getGeneratorManager().getTotalGeneratorCount()));

        int slot = 10;
        for (ConfigManager.CategoryData category : configManager.getAllCategories()) {
            if (slot >= 17) break;
            List<ConfigManager.GeneratorTypeData> gens = configManager.getGeneratorsByCategory(category.id);
            gui.setItem(slot, createItem(category.material,
                    ChatColor.translateAlternateColorCodes('&', category.name),
                    ChatColor.translateAlternateColorCodes('&', category.description),
                    ChatColor.GRAY + "Generators: " + ChatColor.WHITE + gens.size(),
                    "",
                    ChatColor.YELLOW + "Click to browse"));
            slot++;
            if (slot == 14) slot = 16;
        }

        gui.setItem(28, createItem(Material.CHEST, ChatColor.AQUA + "My Generators",
                ChatColor.GRAY + "View all your placed generators",
                ChatColor.GRAY + "Count: " + ChatColor.WHITE + plugin.getGeneratorManager().getPlayerGeneratorCount(player.getUniqueId())));

        gui.setItem(30, createItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "Generator Shop",
                ChatColor.GRAY + "Purchase new generators"));

        gui.setItem(32, createItem(Material.REDSTONE, ChatColor.RED + "Collect All",
                ChatColor.GRAY + "Collect from all generators"));

        gui.setItem(40, createItem(Material.BARRIER, ChatColor.RED + "Close"));

        gui.setItem(44, createItem(Material.PAPER, ChatColor.YELLOW + "Info",
                ChatColor.GRAY + "Place a generator on your island",
                ChatColor.GRAY + "Right-click to collect",
                ChatColor.GRAY + "Break to remove"));

        player.openInventory(gui);
    }

    private void handleMainMenuClick(Player player, ItemStack item) {
        Material type = item.getType();
        if (type == Material.BARRIER) {
            player.closeInventory();
            return;
        }
        if (type == Material.CHEST) {
            openMyGenerators(player);
            return;
        }
        if (type == Material.EMERALD_BLOCK) {
            openShopMenu(player);
            return;
        }
        if (type == Material.REDSTONE) {
            plugin.getGeneratorManager().collectAllGenerators(player);
            openMainMenu(player);
            return;
        }
        for (ConfigManager.CategoryData category : plugin.getConfigManager().getAllCategories()) {
            if (type == category.material) {
                openCategoryMenu(player, category.id);
                return;
            }
        }
    }

    // ==================== CATEGORY MENU ====================

    public void openCategoryMenu(Player player, String categoryId) {
        viewingCategory.put(player.getUniqueId(), categoryId);

        ConfigManager configManager = plugin.getConfigManager();
        ConfigManager.CategoryData category = configManager.getCategory(categoryId);
        if (category == null) {
            openMainMenu(player);
            return;
        }

        Inventory gui = openManaged(45, "CAT:" + categoryId, player);

        gui.setItem(4, createItem(category.material,
                ChatColor.translateAlternateColorCodes('&', category.name)));

        EconomyHook economy = plugin.getEconomyHook();

        List<ConfigManager.GeneratorTypeData> generators = configManager.getGeneratorsByCategory(categoryId);
        int slot = 10;
        for (ConfigManager.GeneratorTypeData gen : generators) {
            if (slot >= 44) break;

            boolean canAfford = economy.hasEconomy() && economy.hasEnough(player, gen.price);
            boolean islandLevelMet = player.hasPermission("generators.admin") ||
                    getIslandLevel(player) >= gen.requiredIslandLevel;

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Price: " + (canAfford ? ChatColor.GREEN : ChatColor.RED) +
                    economy.format(gen.price));
            lore.add(ChatColor.GRAY + "Output: " + ChatColor.WHITE + gen.amount + "x " +
                    gen.output.name().replace("_", " ").toLowerCase());
            lore.add(ChatColor.GRAY + "Interval: " + ChatColor.WHITE + gen.interval + "s");
            lore.add(ChatColor.GRAY + "Storage: " + ChatColor.WHITE + gen.storage);
            if (gen.requiredIslandLevel > 0) {
                lore.add(ChatColor.GRAY + "Required Island Level: " +
                        (islandLevelMet ? ChatColor.GREEN : ChatColor.RED) + gen.requiredIslandLevel);
            }
            lore.add("");
            if (!canAfford) lore.add(ChatColor.RED + "Not enough coins!");
            else lore.add(ChatColor.YELLOW + "Click to purchase");

            gui.setItem(slot, createItem(gen.material,
                    ChatColor.translateAlternateColorCodes('&', gen.name),
                    lore.toArray(new String[0])));
            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        gui.setItem(40, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));
        player.openInventory(gui);
    }

    private void handleCategoryMenuClick(Player player, ItemStack item, String catId) {
        if (item.getType() == Material.ARROW) {
            openMainMenu(player);
            return;
        }

        ConfigManager configManager = plugin.getConfigManager();
        for (ConfigManager.GeneratorTypeData gen : configManager.getGeneratorsByCategory(catId)) {
            if (item.getType() == gen.material) {
                handlePurchase(player, gen.id);
                openCategoryMenu(player, catId);
                return;
            }
        }
    }

    // ==================== SHOP MENU ====================

    public void openShopMenu(Player player) {
        viewingCategory.remove(player.getUniqueId());
        viewingGenerator.remove(player.getUniqueId());

        Inventory gui = openManaged(54, "SHOP", player);

        gui.setItem(4, createItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "" + ChatColor.BOLD + "GENERATOR SHOP",
                ChatColor.GRAY + "Purchase generators with coins"));

        ConfigManager configManager = plugin.getConfigManager();
        EconomyHook economy = plugin.getEconomyHook();

        int slot = 10;
        for (ConfigManager.GeneratorTypeData gen : configManager.getAllGeneratorTypes()) {
            if (slot >= 44) break;

            boolean canAfford = economy.hasEconomy() && economy.hasEnough(player, gen.price);
            boolean islandLevelMet = player.hasPermission("generators.admin") ||
                    getIslandLevel(player) >= gen.requiredIslandLevel;
            boolean canPlace = plugin.getGeneratorManager().canPlaceGenerator(player);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Price: " + (canAfford ? ChatColor.GREEN : ChatColor.RED) +
                    economy.format(gen.price));
            lore.add(ChatColor.GRAY + "Output: " + ChatColor.WHITE + gen.amount + "x " +
                    gen.output.name().replace("_", " ").toLowerCase());
            lore.add(ChatColor.GRAY + "Interval: " + ChatColor.WHITE + gen.interval + "s");
            lore.add(ChatColor.GRAY + "Storage: " + ChatColor.WHITE + gen.storage);
            if (gen.requiredIslandLevel > 0) {
                lore.add(ChatColor.GRAY + "Island Level: " +
                        (islandLevelMet ? ChatColor.GREEN : ChatColor.RED) + gen.requiredIslandLevel);
            }
            lore.add("");
            if (!canAfford) lore.add(ChatColor.RED + "Not enough coins!");
            else if (!islandLevelMet) lore.add(ChatColor.RED + "Island level too low!");
            else if (!canPlace) lore.add(ChatColor.RED + "Max generators reached!");
            else lore.add(ChatColor.YELLOW + "Click to buy");

            gui.setItem(slot, createItem(gen.material,
                    ChatColor.translateAlternateColorCodes('&', gen.name),
                    lore.toArray(new String[0])));
            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        gui.setItem(45, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));
        gui.setItem(49, createItem(Material.PAPER, ChatColor.YELLOW + "Balance",
                ChatColor.GRAY + economy.format(economy.getBalance(player))));

        player.openInventory(gui);
    }

    private void handleShopMenuClick(Player player, ItemStack item) {
        if (item.getType() == Material.ARROW) {
            openMainMenu(player);
            return;
        }

        for (ConfigManager.GeneratorTypeData gen : plugin.getConfigManager().getAllGeneratorTypes()) {
            if (item.getType() == gen.material) {
                handlePurchase(player, gen.id);
                openShopMenu(player);
                return;
            }
        }
    }

    // ==================== MY GENERATORS ====================

    public void openMyGenerators(Player player) {
        viewingGenerator.remove(player.getUniqueId());

        Inventory gui = openManaged(54, "MY", player);

        List<Generator> generators = plugin.getGeneratorManager().getPlayerGenerators(player.getUniqueId());

        gui.setItem(4, createItem(Material.CHEST, ChatColor.AQUA + "" + ChatColor.BOLD + "MY GENERATORS",
                ChatColor.GRAY + "Total: " + ChatColor.WHITE + generators.size()));

        ConfigManager configManager = plugin.getConfigManager();
        int slot = 10;
        for (Generator gen : generators) {
            if (slot >= 44) break;

            ConfigManager.GeneratorTypeData typeData = configManager.getGeneratorType(gen.getType());
            if (typeData == null) continue;

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Level: " + ChatColor.WHITE + gen.getLevel() + "/" + gen.getMaxLevel(configManager));
            lore.add(ChatColor.GRAY + "Stored: " + ChatColor.WHITE + gen.getStoredAmount() + "/" + gen.getMaxStorage(configManager));
            lore.add(ChatColor.GRAY + "Location: " + ChatColor.WHITE + gen.getLocationString());
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to view details");

            gui.setItem(slot, createItem(typeData.material,
                    ChatColor.translateAlternateColorCodes('&', typeData.name),
                    lore.toArray(new String[0])));
            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        if (generators.isEmpty()) {
            gui.setItem(22, createItem(Material.BARRIER, ChatColor.RED + "No Generators",
                    ChatColor.GRAY + "Purchase generators from the shop"));
        }

        gui.setItem(45, createItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "Collect All",
                ChatColor.GRAY + "Collect from all generators"));
        gui.setItem(49, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));

        player.openInventory(gui);
    }

    private void handleMyGeneratorsClick(Player player, ItemStack item) {
        if (item.getType() == Material.ARROW) {
            openMainMenu(player);
            return;
        }

        if (item.getType() == Material.EMERALD_BLOCK) {
            plugin.getGeneratorManager().collectAllGenerators(player);
            openMyGenerators(player);
            return;
        }

        List<Generator> generators = plugin.getGeneratorManager().getPlayerGenerators(player.getUniqueId());
        for (Generator gen : generators) {
            ConfigManager.GeneratorTypeData typeData = plugin.getConfigManager().getGeneratorType(gen.getType());
            if (typeData != null && item.getType() == typeData.material) {
                openGeneratorDetail(player, gen);
                return;
            }
        }
    }

    // ==================== GENERATOR DETAIL ====================

    public void openGeneratorDetail(Player player, Generator gen) {
        viewingGenerator.put(player.getUniqueId(), gen);

        ConfigManager configManager = plugin.getConfigManager();
        ConfigManager.GeneratorTypeData typeData = configManager.getGeneratorType(gen.getType());
        if (typeData == null) {
            openMyGenerators(player);
            return;
        }

        Inventory gui = openManaged(45, "GEN:" + gen.getType(), player);

        gui.setItem(4, createItem(typeData.material,
                ChatColor.translateAlternateColorCodes('&', typeData.name)));

        gui.setItem(10, createItem(Material.PAPER, ChatColor.AQUA + "Info",
                ChatColor.GRAY + "Type: " + ChatColor.WHITE + gen.getType(),
                ChatColor.GRAY + "Level: " + ChatColor.WHITE + gen.getLevel() + "/" + gen.getMaxLevel(configManager),
                ChatColor.GRAY + "Output: " + ChatColor.WHITE + typeData.amount + "x " +
                        typeData.output.name().replace("_", " ").toLowerCase(),
                ChatColor.GRAY + "Interval: " + ChatColor.WHITE + gen.getInterval(configManager) + "s"));

        gui.setItem(12, createItem(Material.CHEST, ChatColor.GOLD + "Storage",
                ChatColor.GRAY + "Stored: " + ChatColor.WHITE + gen.getStoredAmount() + "/" + gen.getMaxStorage(configManager),
                ChatColor.GRAY + "Free Space: " + ChatColor.WHITE + gen.getFreeSpace(configManager)));

        long timeSinceLastGen = System.currentTimeMillis() - gen.getLastGeneration();
        int interval = gen.getInterval(configManager) * 1000;
        long nextIn = Math.max(0, interval - timeSinceLastGen) / 1000;

        gui.setItem(14, createItem(Material.CLOCK, ChatColor.YELLOW + "Timer",
                ChatColor.GRAY + "Next item: " + ChatColor.WHITE + (gen.isFull(configManager) ? "FULL" : nextIn + "s")));

        gui.setItem(28, createItem(Material.LIME_WOOL, ChatColor.GREEN + "COLLECT",
                ChatColor.GRAY + "Collect " + gen.getStoredAmount() + " items"));

        gui.setItem(30, createItem(Material.ANVIL, ChatColor.AQUA + "UPGRADE",
                ChatColor.GRAY + "Level: " + gen.getLevel() + " -> " + (gen.getLevel() + 1),
                ChatColor.GRAY + "Cost: " + plugin.getEconomyHook().format(configManager.getUpgradeCost(gen.getLevel()))));

        gui.setItem(32, createItem(Material.BARRIER, ChatColor.RED + "REMOVE",
                ChatColor.GRAY + "Remove this generator",
                ChatColor.GRAY + "Generator will return to inventory"));

        gui.setItem(40, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));

        player.openInventory(gui);
    }

    private void handleGeneratorDetailClick(Player player, ItemStack item) {
        Generator gen = viewingGenerator.get(player.getUniqueId());
        if (gen == null) {
            player.sendMessage(ChatColor.RED + "Generator not found! Opening menu...");
            openMainMenu(player);
            return;
        }

        Material type = item.getType();
        if (type == Material.ARROW) {
            openMyGenerators(player);
            return;
        }

        if (type == Material.LIME_WOOL) {
            if (!IslandRoleManager.hasMinRole(player, gen.getLocation(), IslandRoleManager.Role.MEMBER)) {
                IslandRoleManager.Role role = IslandRoleManager.getPlayerRole(player, gen.getLocation());
                player.sendMessage(ChatColor.RED + "You need at least MEMBER role to collect! (Your role: " + role.name + ")");
                return;
            }
            plugin.getGeneratorManager().collectGenerator(gen, player);
            openGeneratorDetail(player, gen);
            return;
        }

        if (type == Material.ANVIL) {
            if (gen.isMaxLevel(plugin.getConfigManager())) {
                player.sendMessage(ChatColor.RED + "Generator is already at max level!");
                return;
            }
            if (!IslandRoleManager.hasMinRole(player, gen.getLocation(), IslandRoleManager.Role.COOP)) {
                IslandRoleManager.Role role = IslandRoleManager.getPlayerRole(player, gen.getLocation());
                player.sendMessage(ChatColor.RED + "You need at least COOP role to upgrade generators! (Your role: " + role.name + ")");
                return;
            }
            openUpgradeConfirm(player, gen);
            return;
        }

        if (type == Material.BARRIER) {
            if (!IslandRoleManager.hasMinRole(player, gen.getLocation(), IslandRoleManager.Role.OWNER)) {
                IslandRoleManager.Role role = IslandRoleManager.getPlayerRole(player, gen.getLocation());
                player.sendMessage(ChatColor.RED + "You need OWNER role to remove generators! (Your role: " + role.name + ")");
                return;
            }
            plugin.getGeneratorManager().removeGenerator(gen.getId());
            ItemStack generatorItem = GeneratorListener.createGeneratorItem(gen.getType());
            player.getInventory().addItem(generatorItem);
            player.sendMessage(ChatColor.GREEN + "Generator removed!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            openMyGenerators(player);
            return;
        }
    }

    // ==================== UPGRADE ====================

    public void openUpgradeConfirm(Player player, Generator gen) {
        viewingGenerator.put(player.getUniqueId(), gen);

        ConfigManager configManager = plugin.getConfigManager();
        ConfigManager.GeneratorTypeData typeData = configManager.getGeneratorType(gen.getType());
        if (typeData == null) {
            openMyGenerators(player);
            return;
        }

        Inventory gui = openManaged(27, "UPGRADE", player);

        gui.setItem(13, createItem(typeData.material,
                ChatColor.translateAlternateColorCodes('&', typeData.name)));

        gui.setItem(10, createItem(Material.PAPER, ChatColor.AQUA + "Current",
                ChatColor.GRAY + "Level: " + ChatColor.WHITE + gen.getLevel(),
                ChatColor.GRAY + "Interval: " + ChatColor.WHITE + gen.getInterval(configManager) + "s",
                ChatColor.GRAY + "Storage: " + ChatColor.WHITE + gen.getMaxStorage(configManager)));

        int nextLevel = gen.getLevel() + 1;
        int[] nextStats = configManager.getLevelStats(nextLevel);

        gui.setItem(16, createItem(Material.DIAMOND, ChatColor.GREEN + "Next",
                ChatColor.GRAY + "Level: " + ChatColor.WHITE + nextLevel,
                ChatColor.GRAY + "Interval: " + ChatColor.WHITE + nextStats[0] + "s",
                ChatColor.GRAY + "Storage: " + ChatColor.WHITE + nextStats[1]));

        gui.setItem(18, createItem(Material.LIME_WOOL, ChatColor.GREEN + "CONFIRM UPGRADE",
                ChatColor.GRAY + "Cost: " + ChatColor.WHITE + plugin.getEconomyHook().format(configManager.getUpgradeCost(gen.getLevel()))));

        gui.setItem(26, createItem(Material.RED_WOOL, ChatColor.RED + "CANCEL"));

        player.openInventory(gui);
    }

    private void handleUpgradeClick(Player player, ItemStack item) {
        Generator gen = viewingGenerator.get(player.getUniqueId());
        if (gen == null) {
            openMainMenu(player);
            return;
        }

        if (item.getType() == Material.LIME_WOOL) {
            if (!IslandRoleManager.hasMinRole(player, gen.getLocation(), IslandRoleManager.Role.COOP)) {
                IslandRoleManager.Role role = IslandRoleManager.getPlayerRole(player, gen.getLocation());
                player.sendMessage(ChatColor.RED + "You need at least COOP role to upgrade! (Your role: " + role.name + ")");
                openMainMenu(player);
                return;
            }

            ConfigManager configManager = plugin.getConfigManager();
            double cost = configManager.getUpgradeCost(gen.getLevel());

            if (!plugin.getEconomyHook().hasEnough(player, cost)) {
                player.sendMessage(ChatColor.RED + "Not enough coins! Need " + plugin.getEconomyHook().format(cost));
                openGeneratorDetail(player, gen);
                return;
            }

            plugin.getEconomyHook().withdraw(player, cost);
            gen.setLevel(gen.getLevel() + 1);
            gen.markDirty();

            player.sendMessage(ChatColor.GREEN + "Generator upgraded to level " + gen.getLevel() + "!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            openGeneratorDetail(player, gen);
            return;
        }

        if (item.getType() == Material.RED_WOOL) {
            openGeneratorDetail(player, gen);
            return;
        }
    }

    // ==================== PURCHASE LOGIC ====================

    public void handlePurchase(Player player, String typeId) {
        ConfigManager configManager = plugin.getConfigManager();
        ConfigManager.GeneratorTypeData typeData = configManager.getGeneratorType(typeId);
        if (typeData == null) return;

        if (!IslandRoleManager.hasMinRole(player, player.getLocation(), IslandRoleManager.Role.COOP)) {
            IslandRoleManager.Role role = IslandRoleManager.getPlayerRole(player, player.getLocation());
            player.sendMessage(ChatColor.RED + "You need at least COOP role to purchase generators! (Your role: " + role.name + ")");
            return;
        }

        EconomyHook economy = plugin.getEconomyHook();

        if (!economy.hasEconomy()) {
            player.sendMessage(ChatColor.RED + "Economy not available!");
            return;
        }

        if (!economy.hasEnough(player, typeData.price)) {
            player.sendMessage(ChatColor.RED + "Not enough coins! Need " + economy.format(typeData.price));
            return;
        }

        if (!plugin.getGeneratorManager().canPlaceGenerator(player)) {
            player.sendMessage(ChatColor.RED + "You have reached the maximum number of generators!");
            return;
        }

        if (!player.hasPermission("generators.admin")) {
            int islandLevel = getIslandLevel(player);
            if (islandLevel < typeData.requiredIslandLevel) {
                player.sendMessage(ChatColor.RED + "Your island level is too low! Need level " +
                        typeData.requiredIslandLevel + ", you have " + islandLevel);
                return;
            }
        }

        ItemStack generatorItem = GeneratorListener.createGeneratorItem(typeId);

        if (!economy.withdraw(player, typeData.price)) {
            player.sendMessage(ChatColor.RED + "Failed to withdraw coins!");
            return;
        }

        HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(generatorItem);

        if (!remaining.isEmpty()) {
            economy.deposit(player, typeData.price);
            player.sendMessage(ChatColor.RED + "Inventory full! Coins refunded.");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "Purchased " +
                ChatColor.translateAlternateColorCodes('&', typeData.name) +
                ChatColor.GREEN + " for " + economy.format(typeData.price));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    // ==================== UTILS ====================

    private int getIslandLevel(Player player) {
        try {
            Class<?> bentoBoxClass = Class.forName("world.bentobox.bentobox.BentoBox");
            Object bentoBox = bentoBoxClass.getMethod("getInstance").invoke(null);
            Object islandsManager = bentoBoxClass.getMethod("getIslands").invoke(bentoBox);
            Object island = islandsManager.getClass().getMethod("getIslandAt", org.bukkit.Location.class)
                    .invoke(islandsManager, player.getLocation());
            if (island != null) {
                Object level = island.getClass().getMethod("getLevel").invoke(island);
                return level instanceof Number ? ((Number) level).intValue() : 0;
            }
        } catch (Exception ignored) {}
        return 0;
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
