package com.generator.grief;

import com.generator.GeneratorPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ClaimManager implements Listener, TabCompleter {

    private final GeneratorPlugin plugin;
    private final File claimFile;
    private final Map<UUID, List<Claim>> playerClaims = new HashMap<>();
    private final Map<UUID, Location> claimStart = new HashMap<>();
    private final Map<UUID, Boolean> claimMode = new HashMap<>();
    private final Set<Inventory> managedInventories = Collections.synchronizedSet(new HashSet<>());
    private final Map<Inventory, String> inventoryTitles = Collections.synchronizedMap(new HashMap<>());

    private static final int MAX_CLAIMS_PER_PLAYER = 5;
    private static final int MIN_CLAIM_SIZE = 5;
    private static final int MAX_CLAIM_SIZE = 100;

    public ClaimManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
        this.claimFile = new File(plugin.getDataFolder(), "claims.yml");
        loadClaims();
    }

    public void loadClaims() {
        playerClaims.clear();
        if (!claimFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(claimFile);
        ConfigurationSection playersSection = config.getConfigurationSection("players");
        if (playersSection == null) return;

        for (String uuidStr : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection claimsSection = playersSection.getConfigurationSection(uuidStr);
                if (claimsSection == null) continue;

                List<Claim> claims = new ArrayList<>();
                ConfigurationSection listSection = claimsSection.getConfigurationSection("claims");
                if (listSection != null) {
                    for (String key : listSection.getKeys(false)) {
                        ConfigurationSection cs = listSection.getConfigurationSection(key);
                        if (cs == null) continue;

                        String worldName = cs.getString("world", "world");
                        org.bukkit.World world = Bukkit.getWorld(worldName);
                        if (world == null) continue;

                        Location min = new Location(
                                world,
                                cs.getInt("minX"),
                                cs.getInt("minY"),
                                cs.getInt("minZ")
                        );
                        Location max = new Location(
                                world,
                                cs.getInt("maxX"),
                                cs.getInt("maxY"),
                                cs.getInt("maxZ")
                        );
                        String name = cs.getString("name", "Claim");
                        long created = cs.getLong("created", System.currentTimeMillis());
                        claims.add(new Claim(uuid, name, min, max, created));
                    }
                }
                playerClaims.put(uuid, claims);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveClaims() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, List<Claim>> entry : playerClaims.entrySet()) {
            String uuidStr = entry.getKey().toString();
            int index = 0;
            for (Claim claim : entry.getValue()) {
                String path = "players." + uuidStr + ".claims." + index;
                config.set(path + ".name", claim.name);
                config.set(path + ".world", claim.min.getWorld() != null ? claim.min.getWorld().getName() : "world");
                config.set(path + ".minX", claim.min.getBlockX());
                config.set(path + ".minY", claim.min.getBlockY());
                config.set(path + ".minZ", claim.min.getBlockZ());
                config.set(path + ".maxX", claim.max.getBlockX());
                config.set(path + ".maxY", claim.max.getBlockY());
                config.set(path + ".maxZ", claim.max.getBlockZ());
                config.set(path + ".created", claim.created);
                index++;
            }
        }

        try {
            config.save(claimFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save claims: " + e.getMessage());
        }
    }

    public int getClaimCount(UUID uuid) {
        return playerClaims.getOrDefault(uuid, Collections.emptyList()).size();
    }

    public List<Claim> getPlayerClaims(UUID uuid) {
        return playerClaims.getOrDefault(uuid, Collections.emptyList());
    }

    public boolean canClaim(UUID uuid) {
        return getClaimCount(uuid) < MAX_CLAIMS_PER_PLAYER;
    }

    public Claim getClaimAt(Location loc) {
        for (List<Claim> claims : playerClaims.values()) {
            for (Claim claim : claims) {
                if (claim.contains(loc)) return claim;
            }
        }
        return null;
    }

    public boolean isOwner(UUID uuid, Location loc) {
        Claim claim = getClaimAt(loc);
        return claim != null && claim.owner.equals(uuid);
    }

    public boolean canAccess(UUID uuid, Location loc) {
        Claim claim = getClaimAt(loc);
        if (claim == null) return true;
        if (claim.owner.equals(uuid)) return true;
        if (claim.trusted.contains(uuid)) return true;
        return false;
    }

    public boolean createClaim(UUID uuid, String name, Location loc1, Location loc2) {
        if (!canClaim(uuid)) return false;

        int minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;

        if (sizeX < MIN_CLAIM_SIZE || sizeY < MIN_CLAIM_SIZE || sizeZ < MIN_CLAIM_SIZE) return false;
        if (sizeX > MAX_CLAIM_SIZE || sizeY > MAX_CLAIM_SIZE || sizeZ > MAX_CLAIM_SIZE) return false;

        Location min = new Location(loc1.getWorld(), minX, minY, minZ);
        Location max = new Location(loc1.getWorld(), maxX, maxY, maxZ);

        Claim newClaim = new Claim(uuid, name, min, max, System.currentTimeMillis());

        for (List<Claim> claims : playerClaims.values()) {
            for (Claim existing : claims) {
                if (existing.overlaps(newClaim)) return false;
            }
        }

        playerClaims.computeIfAbsent(uuid, k -> new ArrayList<>()).add(newClaim);
        saveClaims();
        return true;
    }

    public boolean removeClaim(UUID uuid, String name) {
        List<Claim> claims = playerClaims.get(uuid);
        if (claims == null) return false;

        Iterator<Claim> it = claims.iterator();
        while (it.hasNext()) {
            Claim claim = it.next();
            if (claim.name.equalsIgnoreCase(name)) {
                it.remove();
                saveClaims();
                return true;
            }
        }
        return false;
    }

    public boolean trustPlayer(UUID owner, UUID trusted, String claimName) {
        List<Claim> claims = playerClaims.get(owner);
        if (claims == null) return false;

        for (Claim claim : claims) {
            if (claim.name.equalsIgnoreCase(claimName)) {
                claim.trusted.add(trusted);
                saveClaims();
                return true;
            }
        }
        return false;
    }

    public boolean untrustPlayer(UUID owner, UUID trusted, String claimName) {
        List<Claim> claims = playerClaims.get(owner);
        if (claims == null) return false;

        for (Claim claim : claims) {
            if (claim.name.equalsIgnoreCase(claimName)) {
                claim.trusted.remove(trusted);
                saveClaims();
                return true;
            }
        }
        return false;
    }

    public void enterClaimMode(Player player) {
        claimMode.put(player.getUniqueId(), true);
        player.sendMessage(ChatColor.GREEN + "Claim mode ON! Left-click two corners to define your claim.");
        player.sendMessage(ChatColor.YELLOW + "Type /claim cancel to cancel.");
    }

    public void exitClaimMode(Player player) {
        claimMode.remove(player.getUniqueId());
        claimStart.remove(player.getUniqueId());
        player.sendMessage(ChatColor.YELLOW + "Claim mode OFF.");
    }

    public boolean isInClaimMode(Player player) {
        return claimMode.getOrDefault(player.getUniqueId(), false);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isInClaimMode(player)) return;
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        event.setCancelled(true);

        if (claimStart.containsKey(player.getUniqueId())) {
            Location loc1 = claimStart.remove(player.getUniqueId());
            Location loc2 = event.getClickedBlock().getLocation();

            String name = "Claim_" + (getClaimCount(player.getUniqueId()) + 1);
            if (createClaim(player.getUniqueId(), name, loc1, loc2)) {
                player.sendMessage(ChatColor.GREEN + "Claim '" + name + "' created!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            } else {
                player.sendMessage(ChatColor.RED + "Cannot create claim here! Check size (5-100) and overlaps.");
            }
            exitClaimMode(player);
        } else {
            claimStart.put(player.getUniqueId(), event.getClickedBlock().getLocation());
            player.sendMessage(ChatColor.GREEN + "First corner set! Now click the opposite corner.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        claimMode.remove(event.getPlayer().getUniqueId());
        claimStart.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        claimMode.remove(uuid);
        claimStart.remove(uuid);
    }

    public void openClaimGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "CLAIM_MENU");
        managedInventories.add(gui);
        inventoryTitles.put(gui, "CLAIM_MENU");

        gui.setItem(4, createItem(Material.GRASS_BLOCK,
                ChatColor.GREEN + "" + ChatColor.BOLD + "CLAIMS",
                "", "§7Your claims: §f" + getClaimCount(player.getUniqueId()) + "/" + MAX_CLAIMS_PER_PLAYER));

        gui.setItem(10, createItem(Material.DIAMOND,
                ChatColor.AQUA + "Create Claim",
                "", "§7Left-click two corners"));

        gui.setItem(12, createItem(Material.REDSTONE,
                ChatColor.RED + "Remove Claim",
                "", "§7Remove a claim"));

        gui.setItem(14, createItem(Material.BOOK,
                ChatColor.YELLOW + "Trust Player",
                "", "§7Allow player to build"));

        gui.setItem(16, createItem(Material.PAPER,
                ChatColor.WHITE + "Claim List",
                "", "§7View your claims"));

        gui.setItem(22, createItem(Material.BARRIER,
                ChatColor.RED + "Close"));

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory topInv = event.getView().getTopInventory();
        if (topInv == null || !managedInventories.contains(topInv)) return;

        if (event.getRawSlot() >= topInv.getSize()) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        event.setCancelled(true);

        String title = inventoryTitles.getOrDefault(topInv, "");

        try {
            if (title.equals("CLAIM_MENU")) {
                handleClaimMenuClick(player, item);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("ClaimManager error: " + e.getMessage());
            player.closeInventory();
        }
    }

    private void handleClaimMenuClick(Player player, ItemStack item) {
        Material type = item.getType();

        if (type == Material.DIAMOND) {
            player.closeInventory();
            enterClaimMode(player);
        } else if (type == Material.REDSTONE) {
            player.closeInventory();
            List<Claim> claims = playerClaims.get(player.getUniqueId());
            if (claims == null || claims.isEmpty()) {
                player.sendMessage(ChatColor.RED + "You have no claims!");
                return;
            }
            player.sendMessage(ChatColor.YELLOW + "Type /claim remove <name> to remove a claim.");
        } else if (type == Material.BOOK) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Type /claim trust <player> <claim> to trust a player.");
        } else if (type == Material.PAPER) {
            player.closeInventory();
            List<Claim> claims = playerClaims.get(player.getUniqueId());
            if (claims == null || claims.isEmpty()) {
                player.sendMessage(ChatColor.RED + "You have no claims!");
                return;
            }
            player.sendMessage(ChatColor.GREEN + "=== YOUR CLAIMS ===");
            for (Claim claim : claims) {
                player.sendMessage(ChatColor.YELLOW + claim.name + ": " +
                        "(" + claim.min.getBlockX() + "," + claim.min.getBlockY() + "," + claim.min.getBlockZ() + ") - " +
                        "(" + claim.max.getBlockX() + "," + claim.max.getBlockY() + "," + claim.max.getBlockZ() + ")");
            }
        } else if (type == Material.BARRIER) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        if (topInv != null && managedInventories.contains(topInv)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        if (topInv != null) {
            managedInventories.remove(topInv);
            inventoryTitles.remove(topInv);
        }
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();
        if (args.length == 1) {
            return Arrays.asList("create", "remove", "list", "trust", "untrust", "info");
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "remove", "trust", "untrust", "info" -> {
                    List<Claim> claims = playerClaims.get(player.getUniqueId());
                    if (claims != null) {
                        return claims.stream()
                                .map(c -> c.name)
                                .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                                .toList();
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    public static class Claim {
        public final UUID owner;
        public final String name;
        public final Location min;
        public final Location max;
        public final long created;
        public final Set<UUID> trusted = new HashSet<>();

        public Claim(UUID owner, String name, Location min, Location max, long created) {
            this.owner = owner;
            this.name = name;
            this.min = min;
            this.max = max;
            this.created = created;
        }

        public boolean contains(Location loc) {
            if (loc.getWorld() == null || min.getWorld() == null) return false;
            if (!loc.getWorld().equals(min.getWorld())) return false;
            return loc.getBlockX() >= min.getBlockX() && loc.getBlockX() <= max.getBlockX() &&
                    loc.getBlockY() >= min.getBlockY() && loc.getBlockY() <= max.getBlockY() &&
                    loc.getBlockZ() >= min.getBlockZ() && loc.getBlockZ() <= max.getBlockZ();
        }

        public boolean overlaps(Claim other) {
            if (min.getWorld() == null || other.min.getWorld() == null) return false;
            if (!min.getWorld().equals(other.min.getWorld())) return false;
            return min.getBlockX() <= other.max.getBlockX() && max.getBlockX() >= other.min.getBlockX() &&
                    min.getBlockY() <= other.max.getBlockY() && max.getBlockY() >= other.min.getBlockY() &&
                    min.getBlockZ() <= other.max.getBlockZ() && max.getBlockZ() >= other.min.getBlockZ();
        }
    }
}
