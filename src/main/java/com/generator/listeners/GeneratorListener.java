package com.generator.listeners;

import com.generator.GeneratorPlugin;
import com.generator.config.ConfigManager;
import com.generator.generator.Generator;
import com.generator.generator.GeneratorManager;
import com.generator.profile.PlayerProfile;
import com.generator.util.IslandRoleManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GeneratorListener implements Listener {

    private final GeneratorPlugin plugin;

    public GeneratorListener(GeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        // FIRST: Check if the clicked block IS a generator — always handle this
        if (clickedBlock != null) {
            Generator gen = plugin.getGeneratorManager().getGeneratorAt(clickedBlock.getLocation());
            if (gen != null) {
                event.setCancelled(true);

                if (!IslandRoleManager.hasMinRole(player, gen.getLocation(), IslandRoleManager.Role.MEMBER)) {
                    IslandRoleManager.Role role = IslandRoleManager.getPlayerRole(player, gen.getLocation());
                    player.sendMessage(ChatColor.RED + "You need at least MEMBER role to collect! (Your role: " + role.name + ")");
                    return;
                }

                if (gen.getStoredAmount() > 0) {
                    plugin.getGeneratorManager().collectGenerator(gen, player);
                } else {
                    int interval = gen.getInterval(plugin.getConfigManager());
                    player.sendMessage(ChatColor.YELLOW + "Generating... Next item in " + interval + " seconds");
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                }

                plugin.getGUIManager().openGeneratorDetail(player, gen);
                return;
            }
        }

        // SECOND: Check if player is holding a generator item — handle placement
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand == null || itemInHand.getType() == Material.AIR) return;

        String typeId = getGeneratorTypeId(itemInHand);
        if (typeId == null) return;

        event.setCancelled(true);

        if (clickedBlock == null) {
            player.sendMessage(ChatColor.RED + "Look at a block to place the generator!");
            return;
        }

        Block placeBlock = clickedBlock.getRelative(event.getBlockFace());

        if (!IslandRoleManager.hasMinRole(player, placeBlock.getLocation(), IslandRoleManager.Role.COOP)) {
            IslandRoleManager.Role role = IslandRoleManager.getPlayerRole(player, placeBlock.getLocation());
            player.sendMessage(ChatColor.RED + "You need at least COOP role to place generators! (Your role: " + role.name + ")");
            return;
        }

        if (!placeBlock.getType().isAir() && placeBlock.getType() != Material.WATER && placeBlock.getType() != Material.LAVA) {
            player.sendMessage(ChatColor.RED + "Cannot place generator here!");
            return;
        }

        if (!plugin.getGeneratorManager().canPlaceGenerator(player)) {
            player.sendMessage(ChatColor.RED + "You have reached the maximum number of generators!");
            return;
        }

        if (plugin.getGeneratorManager().getGeneratorAt(placeBlock.getLocation()) != null) {
            player.sendMessage(ChatColor.RED + "A generator is already here!");
            return;
        }

        ConfigManager configManager = plugin.getConfigManager();
        ConfigManager.GeneratorTypeData typeData = configManager.getGeneratorType(typeId);
        if (typeData == null) return;

        placeBlock.setType(typeData.material);

        Generator gen = plugin.getGeneratorManager().createGenerator(player, typeId, placeBlock.getLocation());
        if (gen == null) {
            placeBlock.setType(Material.AIR);
            player.sendMessage(ChatColor.RED + "Failed to create generator!");
            return;
        }

        // Spawn glowing item frame on top of the generator
        spawnGeneratorDisplay(placeBlock.getLocation(), typeData);

        itemInHand.setAmount(itemInHand.getAmount() - 1);

        player.sendMessage(ChatColor.GREEN + "Generator placed!");
        player.playSound(placeBlock.getLocation(), Sound.BLOCK_METAL_PLACE, 1.0f, 1.0f);

        PlayerProfile profile = plugin.getProfileManager().getProfileIfLoaded(player.getUniqueId());
        if (profile != null) {
            profile.addGeneratorsPlaced();
        }
    }

    private void spawnGeneratorDisplay(Location location, ConfigManager.GeneratorTypeData typeData) {
        Location frameLoc = location.clone().add(0.5, 1.001, 0.5);

        GlowItemFrame frame = (GlowItemFrame) location.getWorld().spawnEntity(frameLoc, EntityType.GLOW_ITEM_FRAME);
        frame.setFacingDirection(org.bukkit.block.BlockFace.UP);
        frame.setVisible(false);
        frame.setFixed(true);

        ItemStack displayItem = new ItemStack(typeData.output);
        ItemMeta displayMeta = displayItem.getItemMeta();
        if (displayMeta != null) {
            displayMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', typeData.name));
            displayItem.setItemMeta(displayMeta);
        }
        frame.setItem(displayItem);
    }

    private void removeGeneratorDisplay(Location location) {
        Location frameLoc = location.clone().add(0.5, 1.001, 0.5);
        for (org.bukkit.entity.Entity entity : location.getWorld().getNearbyEntities(frameLoc, 0.5, 0.5, 0.5)) {
            if (entity instanceof GlowItemFrame || entity instanceof ItemFrame) {
                entity.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack itemInHand = event.getPlayer().getInventory().getItemInMainHand();
        if (itemInHand != null && getGeneratorTypeId(itemInHand) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        Generator gen = plugin.getGeneratorManager().getGeneratorAt(block.getLocation());
        if (gen == null) return;

        if (!IslandRoleManager.hasMinRole(player, gen.getLocation(), IslandRoleManager.Role.OWNER)) {
            IslandRoleManager.Role role = IslandRoleManager.getPlayerRole(player, gen.getLocation());
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You need OWNER role to break generators! (Your role: " + role.name + ")");
            return;
        }

        event.setCancelled(true);

        plugin.getGeneratorManager().removeGenerator(gen.getId());

        removeGeneratorDisplay(block.getLocation());

        ItemStack generatorItem = createGeneratorItem(gen.getType());
        HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(generatorItem);

        if (!remaining.isEmpty()) {
            for (ItemStack drop : remaining.values()) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
            }
        }

        block.setType(Material.AIR);

        player.sendMessage(ChatColor.GREEN + "Generator removed! Added back to your inventory.");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
    }

    private String getGeneratorTypeId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getLore() == null) return null;

        for (String line : meta.getLore()) {
            String stripped = ChatColor.stripColor(line);
            if (stripped != null && stripped.startsWith("Type: ")) {
                return stripped.substring(6).trim();
            }
        }
        return null;
    }

    public static ItemStack createGeneratorItem(String type) {
        ConfigManager configManager =
            GeneratorPlugin.getInstance().getConfigManager();
        ConfigManager.GeneratorTypeData typeData = configManager.getGeneratorType(type);
        if (typeData == null) return new ItemStack(Material.STONE);

        ItemStack item = new ItemStack(typeData.material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', typeData.name));

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Type: " + ChatColor.WHITE + type);
            lore.add(ChatColor.GRAY + "Interval: " + ChatColor.WHITE + typeData.interval + "s");
            lore.add(ChatColor.GRAY + "Storage: " + ChatColor.WHITE + typeData.storage);
            lore.add("");
            lore.add(ChatColor.YELLOW + "Right-click a block to place");

            for (String descLine : typeData.description) {
                lore.add(ChatColor.translateAlternateColorCodes('&', descLine));
            }

            meta.setLore(lore);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getLocation().getWorld() != null &&
                event.getLocation().getWorld().getName().equals("lobby")) {
            event.setCancelled(true);
        }
    }
}
