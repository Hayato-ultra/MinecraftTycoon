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

public class KitManager implements Listener {

    private final GeneratorPlugin plugin;
    private final Set<Inventory> managedInventories = Collections.synchronizedSet(new HashSet<>());
    private final Map<Inventory, String> inventoryTitles = Collections.synchronizedMap(new HashMap<>());
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public KitManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    public void openKitGUI(Player player) {
        Inventory gui = createManaged(45, "KITS_MENU", player);

        gui.setItem(4, createItem(Material.CHEST,
                ChatColor.GOLD + "" + ChatColor.BOLD + "KITS"));

        gui.setItem(10, createItem(Material.WOODEN_PICKAXE,
                ChatColor.GREEN + "Starter Kit",
                "", "§7Contains basic tools and food",
                "§7Wooden pickaxe, axe, sword",
                "§716 bread, 8 torches",
                "", cooldownText(player, "starter")));

        gui.setItem(12, createItem(Material.IRON_PICKAXE,
                ChatColor.AQUA + "Miner Kit",
                "", "§7Mining supplies",
                "§7Iron pickaxe, 32 torches",
                "§716 bread, 8 iron ingots",
                "", cooldownText(player, "miner")));

        gui.setItem(14, createItem(Material.IRON_CHESTPLATE,
                ChatColor.BLUE + "Warrior Kit",
                "", "§7Combat gear",
                "§7Iron armor set, iron sword",
                "§716 cooked beef, shield",
                "", cooldownText(player, "warrior")));

        gui.setItem(16, createItem(Material.DIAMOND_PICKAXE,
                ChatColor.LIGHT_PURPLE + "Pro Kit",
                "", "§7Advanced tools",
                "§7Diamond pickaxe, axe, shovel",
                "§732 bread, 16 golden apples",
                "", cooldownText(player, "pro")));

        gui.setItem(22, createItem(Material.NETHERITE_CHESTPLATE,
                ChatColor.RED + "Elite Kit",
                "", "§7End-game gear",
                "§7Netherite armor, netherite sword",
                "§764 golden apples, elytra",
                "", cooldownText(player, "elite")));

        gui.setItem(31, createItem(Material.ENDER_PEARL,
                ChatColor.DARK_PURPLE + "Teleport Kit",
                "", "§7Travel supplies",
                "§732 ender pearls, compass",
                "§716 bread, rockets",
                "", cooldownText(player, "teleport")));

        gui.setItem(33, createItem(Material.REDSTONE,
                ChatColor.RED + "Redstone Kit",
                "", "§7Redstone components",
                "§764 redstone, 32 repeaters",
                "§716 comparators, 8 pistons",
                "", cooldownText(player, "redstone")));

        gui.setItem(40, createItem(Material.BARRIER, ChatColor.RED + "Close"));

        player.openInventory(gui);
    }

    private String cooldownText(Player player, String kit) {
        long remaining = getCooldownRemaining(player, kit);
        if (remaining <= 0) return "§a§lAVAILABLE";
        int hours = (int) (remaining / 3600000);
        int minutes = (int) ((remaining % 3600000) / 60000);
        return "§c§lCooldown: " + hours + "h " + minutes + "m";
    }

    private long getCooldownRemaining(Player player, String kit) {
        Map<String, Long> playerCooldowns = cooldowns.getOrDefault(player.getUniqueId(), Collections.emptyMap());
        long lastClaimed = playerCooldowns.getOrDefault(kit, 0L);
        long cooldown = getCooldownMs(kit);
        long remaining = lastClaimed + cooldown - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    private long getCooldownMs(String kit) {
        return switch (kit) {
            case "starter" -> 0;
            case "miner" -> 3600000;
            case "warrior" -> 3600000;
            case "pro" -> 7200000;
            case "elite" -> 21600000;
            case "teleport" -> 7200000;
            case "redstone" -> 3600000;
            default -> 3600000;
        };
    }

    private void claimKit(Player player, String kit) {
        long remaining = getCooldownRemaining(player, kit);
        if (remaining > 0) {
            int hours = (int) (remaining / 3600000);
            int minutes = (int) ((remaining % 3600000) / 60000);
            player.sendMessage(ChatColor.RED + "Kit on cooldown! Wait " + hours + "h " + minutes + "m");
            return;
        }

        List<ItemStack> items = getKitItems(kit);
        for (ItemStack item : items) {
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            if (!overflow.isEmpty()) {
                for (ItemStack drop : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }

        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(kit, System.currentTimeMillis());
        player.sendMessage(ChatColor.GREEN + "Received " + ChatColor.GOLD + kit.toUpperCase() + " Kit" + ChatColor.GREEN + "!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        openKitGUI(player);
    }

    private List<ItemStack> getKitItems(String kit) {
        List<ItemStack> items = new ArrayList<>();
        switch (kit) {
            case "starter" -> {
                items.add(new ItemStack(Material.WOODEN_PICKAXE));
                items.add(new ItemStack(Material.WOODEN_AXE));
                items.add(new ItemStack(Material.WOODEN_SWORD));
                items.add(createItem(Material.BREAD, "§7Bread", 16));
                items.add(createItem(Material.TORCH, "§7Torch", 8));
            }
            case "miner" -> {
                items.add(new ItemStack(Material.IRON_PICKAXE));
                items.add(createItem(Material.TORCH, "§7Torch", 32));
                items.add(createItem(Material.BREAD, "§7Bread", 16));
                items.add(createItem(Material.IRON_INGOT, "§7Iron Ingot", 8));
            }
            case "warrior" -> {
                items.add(new ItemStack(Material.IRON_HELMET));
                items.add(new ItemStack(Material.IRON_CHESTPLATE));
                items.add(new ItemStack(Material.IRON_LEGGINGS));
                items.add(new ItemStack(Material.IRON_BOOTS));
                items.add(new ItemStack(Material.IRON_SWORD));
                items.add(new ItemStack(Material.SHIELD));
                items.add(createItem(Material.COOKED_BEEF, "§7Cooked Beef", 16));
            }
            case "pro" -> {
                items.add(new ItemStack(Material.DIAMOND_PICKAXE));
                items.add(new ItemStack(Material.DIAMOND_AXE));
                items.add(new ItemStack(Material.DIAMOND_SHOVEL));
                items.add(createItem(Material.BREAD, "§7Bread", 32));
                items.add(createItem(Material.GOLDEN_APPLE, "§7Golden Apple", 16));
            }
            case "elite" -> {
                items.add(new ItemStack(Material.NETHERITE_HELMET));
                items.add(new ItemStack(Material.NETHERITE_CHESTPLATE));
                items.add(new ItemStack(Material.NETHERITE_LEGGINGS));
                items.add(new ItemStack(Material.NETHERITE_BOOTS));
                items.add(new ItemStack(Material.NETHERITE_SWORD));
                items.add(new ItemStack(Material.ELYTRA));
                items.add(createItem(Material.GOLDEN_APPLE, "§7Golden Apple", 64));
            }
            case "teleport" -> {
                items.add(createItem(Material.ENDER_PEARL, "§7Ender Pearl", 32));
                items.add(new ItemStack(Material.COMPASS));
                items.add(createItem(Material.BREAD, "§7Bread", 16));
                items.add(createItem(Material.FIREWORK_ROCKET, "§7Rocket", 16));
            }
            case "redstone" -> {
                items.add(createItem(Material.REDSTONE, "§7Redstone", 64));
                items.add(createItem(Material.REPEATER, "§7Repeater", 32));
                items.add(createItem(Material.COMPARATOR, "§7Comparator", 16));
                items.add(createItem(Material.PISTON, "§7Piston", 8));
                items.add(createItem(Material.STICKY_PISTON, "§7Sticky Piston", 4));
            }
        }
        return items;
    }

    private ItemStack createItem(Material material, String name, int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
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
            if (title.equals("KITS_MENU")) {
                handleKitClick(player, item);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("KitManager error: " + e.getMessage());
            player.closeInventory();
        }
    }

    private void handleKitClick(Player player, ItemStack item) {
        Material type = item.getType();

        if (type == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        String kit = switch (type) {
            case WOODEN_PICKAXE -> "starter";
            case IRON_PICKAXE -> "miner";
            case IRON_CHESTPLATE -> "warrior";
            case DIAMOND_PICKAXE -> "pro";
            case NETHERITE_CHESTPLATE -> "elite";
            case ENDER_PEARL -> "teleport";
            case REDSTONE -> "redstone";
            default -> null;
        };

        if (kit != null) {
            claimKit(player, kit);
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
        cooldowns.remove(event.getPlayer().getUniqueId());
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
