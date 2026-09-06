package com.generator.grief;

import com.generator.GeneratorPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class AntiGriefManager implements Listener {

    private final GeneratorPlugin plugin;
    private final Map<UUID, Boolean> antiGriefToggle = new HashMap<>();
    private final Set<Material> protectedBlocks = new HashSet<>();

    public AntiGriefManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
        initProtectedBlocks();
    }

    private void initProtectedBlocks() {
        protectedBlocks.addAll(Arrays.asList(
                Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST,
                Material.BARREL, Material.SHULKER_BOX, Material.WHITE_SHULKER_BOX,
                Material.ORANGE_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX,
                Material.LIGHT_BLUE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX,
                Material.LIME_SHULKER_BOX, Material.PINK_SHULKER_BOX,
                Material.GRAY_SHULKER_BOX, Material.LIGHT_GRAY_SHULKER_BOX,
                Material.CYAN_SHULKER_BOX, Material.PURPLE_SHULKER_BOX,
                Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX,
                Material.GREEN_SHULKER_BOX, Material.RED_SHULKER_BOX,
                Material.BLACK_SHULKER_BOX,
                Material.BEDROCK, Material.OBSIDIAN, Material.CRYING_OBSIDIAN,
                Material.RESPAWN_ANCHOR, Material.NETHER_PORTAL,
                Material.END_PORTAL, Material.END_PORTAL_FRAME,
                Material.BEACON, Material.CONDUIT,
                Material.SPAWNER, Material.INFESTED_STONE,
                Material.INFESTED_COBBLESTONE, Material.INFESTED_STONE_BRICKS,
                Material.INFESTED_MOSSY_STONE_BRICKS, Material.INFESTED_CRACKED_STONE_BRICKS,
                Material.INFESTED_DEEPSLATE
        ));
    }

    public boolean isProtected(Player player, org.bukkit.block.Block block) {
        if (player.hasPermission("generators.admin")) return false;
        if (isAntiGriefEnabled(player)) return false;

        if (protectedBlocks.contains(block.getType())) return true;

        if (plugin.getConfigManager().isPvpWorld(player.getWorld().getName())) return false;

        return false;
    }

    public boolean canBuild(Player player, org.bukkit.block.Block block) {
        if (player.hasPermission("generators.admin")) return true;
        if (isAntiGriefEnabled(player)) return true;
        if (plugin.getConfigManager().isPvpWorld(player.getWorld().getName())) return true;

        return false;
    }

    public boolean canDamage(Player attacker, Player victim) {
        if (attacker.hasPermission("generators.admin")) return true;

        if (!plugin.getConfigManager().isPvpWorld(attacker.getWorld().getName())) {
            attacker.sendMessage(ChatColor.RED + "PvP is disabled in this world!");
            return false;
        }

        return true;
    }

    public boolean canOpenInventory(Player player, org.bukkit.block.Block block) {
        if (player.hasPermission("generators.admin")) return true;
        if (isAntiGriefEnabled(player)) return true;
        if (plugin.getConfigManager().isPvpWorld(player.getWorld().getName())) return true;

        return false;
    }

    public boolean canExplode(org.bukkit.block.Block block) {
        if (block.getType() == Material.BEDROCK || block.getType() == Material.OBSIDIAN ||
                block.getType() == Material.END_PORTAL || block.getType() == Material.END_PORTAL_FRAME) {
            return false;
        }
        return true;
    }

    public boolean canPickup(Player player, org.bukkit.entity.Entity entity) {
        if (player.hasPermission("generators.admin")) return true;
        if (isAntiGriefEnabled(player)) return true;
        if (plugin.getConfigManager().isPvpWorld(player.getWorld().getName())) return true;

        return false;
    }

    public boolean canEmptyBucket(Player player) {
        if (player.hasPermission("generators.admin")) return true;
        if (isAntiGriefEnabled(player)) return true;
        if (plugin.getConfigManager().isPvpWorld(player.getWorld().getName())) return true;

        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (isProtected(player, event.getBlock())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "This block is protected!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!canBuild(player, event.getBlock())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot build here!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        if (!canDamage(attacker, victim)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getInventory().getHolder() instanceof org.bukkit.block.Container) {
            org.bukkit.block.Container container = (org.bukkit.block.Container) event.getInventory().getHolder();
            if (!canOpenInventory(player, container.getBlock())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "This container is protected!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<org.bukkit.block.Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            org.bukkit.block.Block block = it.next();
            if (!canExplode(block)) {
                it.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (event.getClickedBlock() == null) return;

        if (protectedBlocks.contains(event.getClickedBlock().getType())) {
            if (!canOpenInventory(player, event.getClickedBlock())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "This block is protected!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (!canEmptyBucket(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot place liquids here!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        antiGriefToggle.remove(event.getPlayer().getUniqueId());
    }

    public boolean isAntiGriefEnabled(Player player) {
        return antiGriefToggle.getOrDefault(player.getUniqueId(), true);
    }

    public void toggleAntiGrief(Player player) {
        boolean current = isAntiGriefEnabled(player);
        antiGriefToggle.put(player.getUniqueId(), !current);
        player.sendMessage(ChatColor.GREEN + "Anti-grief " + (!current ? "enabled" : "disabled") + "!");
    }
}
