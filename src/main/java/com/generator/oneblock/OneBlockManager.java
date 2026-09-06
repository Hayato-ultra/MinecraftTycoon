package com.generator.oneblock;

import com.generator.GeneratorPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OneBlockManager implements Listener {

    private final GeneratorPlugin plugin;
    private final File oneBlockFile;
    private final Map<String, OneBlockPhase> phases = new LinkedHashMap<>();
    private final Map<UUID, OneBlockProgress> playerProgress = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastBreakTime = new ConcurrentHashMap<>();

    private static final String ONEBLOCK_WORLD = "oneblock_world";
    private static final int SPAWN_X = 0;
    private static final int SPAWN_Y = 64;
    private static final int SPAWN_Z = 0;
    private static final long BREAK_COOLDOWN = 500;

    public OneBlockManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
        this.oneBlockFile = new File(plugin.getDataFolder(), "oneblock.yml");
        loadPhases();
        loadProgress();
    }

    private void loadPhases() {
        phases.clear();
        if (!oneBlockFile.exists()) {
            createDefaultPhases();
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(oneBlockFile);
        ConfigurationSection phaseSection = config.getConfigurationSection("phases");
        if (phaseSection == null) {
            createDefaultPhases();
            return;
        }

        for (String key : phaseSection.getKeys(false)) {
            ConfigurationSection cs = phaseSection.getConfigurationSection(key);
            if (cs == null) continue;

            OneBlockPhase phase = new OneBlockPhase();
            phase.id = key;
            phase.displayName = cs.getString("display-name", key);
            phase.material = Material.matchMaterial(cs.getString("material", "STONE"));
            phase.blocksRequired = cs.getInt("blocks-required", 100);
            phase.rewardCoins = cs.getInt("reward-coins", 50);
            phase.rewardItem = cs.getString("reward-item", "");
            phase.rewardAmount = cs.getInt("reward-amount", 1);
            phase.nextPhase = cs.getString("next-phase", "");
            phase.description = cs.getString("description", "");

            if (phase.material != null) {
                phases.put(key, phase);
            }
        }

        if (phases.isEmpty()) {
            createDefaultPhases();
        }
    }

    private void createDefaultPhases() {
        phases.clear();

        addPhase("dirt", "Dirt Plains", "DIRT", 50, 25, "", 1, "grass", "Start your journey on a dirt island");
        addPhase("grass", "Grass Plains", "GRASS_BLOCK", 100, 50, "", 1, "stone", "The island grows greener");
        addPhase("stone", "Stone Mountain", "STONE", 150, 75, "COBBLESTONE", 16, "wood", "Mine deep into the mountain");
        addPhase("wood", "Forest Island", "OAK_LOG", 200, 100, "OAK_PLANKS", 32, "iron", "Build your first shelter");
        addPhase("iron", "Iron Deposit", "IRON_ORE", 250, 150, "IRON_INGOT", 8, "gold", "Forge your first tools");
        addPhase("gold", "Gold Vein", "GOLD_ORE", 300, 200, "GOLD_INGOT", 8, "diamond", "Strike gold!");
        addPhase("diamond", "Diamond Depth", "DIAMOND_ORE", 400, 300, "DIAMOND", 4, "emerald", "The rarest finds");
        addPhase("emerald", "Emerald Peak", "EMERALD_ORE", 500, 400, "EMERALD", 4, "nether", "A mountain of wealth");
        addPhase("nether", "Nether Gate", "NETHERRACK", 600, 500, "BLAZE_ROD", 4, "end", "Enter the nether dimension");
        addPhase("end", "End Island", "END_STONE", 800, 750, "ENDER_PEARL", 8, "crystal", "The final frontier");
        addPhase("crystal", "Crystal Peak", "AMETHYST_BLOCK", 1000, 1000, "NETHERITE_INGOT", 2, "obsidian", "Crystalline perfection");
        addPhase("obsidian", "Obsidian Fortress", "OBSIDIAN", 1500, 1500, "NETHERITE_BLOCK", 1, "bedrock", "Unbreakable defenses");
        addPhase("bedrock", "Bedrock Summit", "BEDROCK", 2000, 2000, "DRAGON_EGG", 1, "", "The ultimate achievement!");

        savePhases();
    }

    private void addPhase(String id, String name, String material, int blocksRequired,
                          int rewardCoins, String rewardItem, int rewardAmount,
                          String nextPhase, String description) {
        OneBlockPhase phase = new OneBlockPhase();
        phase.id = id;
        phase.displayName = name;
        phase.material = Material.matchMaterial(material);
        phase.blocksRequired = blocksRequired;
        phase.rewardCoins = rewardCoins;
        phase.rewardItem = rewardItem;
        phase.rewardAmount = rewardAmount;
        phase.nextPhase = nextPhase;
        phase.description = description;
        phases.put(id, phase);
    }

    public void savePhases() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, OneBlockPhase> entry : phases.entrySet()) {
            OneBlockPhase phase = entry.getValue();
            String path = "phases." + phase.id;
            config.set(path + ".display-name", phase.displayName);
            config.set(path + ".material", phase.material != null ? phase.material.name() : "STONE");
            config.set(path + ".blocks-required", phase.blocksRequired);
            config.set(path + ".reward-coins", phase.rewardCoins);
            config.set(path + ".reward-item", phase.rewardItem);
            config.set(path + ".reward-amount", phase.rewardAmount);
            config.set(path + ".next-phase", phase.nextPhase);
            config.set(path + ".description", phase.description);
        }

        try {
            config.save(oneBlockFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save oneblock.yml: " + e.getMessage());
        }
    }

    public void saveProgress() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, OneBlockProgress> entry : playerProgress.entrySet()) {
            String path = "progress." + entry.getKey().toString();
            OneBlockProgress progress = entry.getValue();
            config.set(path + ".current-phase", progress.currentPhase);
            config.set(path + ".blocks-broken", progress.blocksBroken);
            config.set(path + ".total-broken", progress.totalBroken);
            config.set(path + ".phases-completed", progress.phasesCompleted);
        }

        try {
            YamlConfiguration existing = YamlConfiguration.loadConfiguration(oneBlockFile);
            existing.getConfigurationSection("progress");
            for (String key : existing.getKeys(true)) {
                if (key.startsWith("progress.")) {
                    existing.set(key, null);
                }
            }
            config.getConfigurationSection("progress");
            for (String key : config.getKeys(true)) {
                if (key.startsWith("progress.")) {
                    existing.set(key, config.get(key));
                }
            }
            existing.save(oneBlockFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save oneblock progress: " + e.getMessage());
        }
    }

    private void loadProgress() {
        playerProgress.clear();
        if (!oneBlockFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(oneBlockFile);
        ConfigurationSection progressSection = config.getConfigurationSection("progress");
        if (progressSection == null) return;

        for (String uuidStr : progressSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection cs = progressSection.getConfigurationSection(uuidStr);
                if (cs == null) continue;

                OneBlockProgress progress = new OneBlockProgress();
                progress.currentPhase = cs.getString("current-phase", "dirt");
                progress.blocksBroken = cs.getInt("blocks-broken", 0);
                progress.totalBroken = cs.getInt("total-broken", 0);
                progress.phasesCompleted = cs.getInt("phases-completed", 0);
                playerProgress.put(uuid, progress);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public boolean isOneBlockWorld(String worldName) {
        return ONEBLOCK_WORLD.equalsIgnoreCase(worldName);
    }

    public boolean isOneBlockLocation(Location loc) {
        if (!isOneBlockWorld(loc.getWorld().getName())) return false;
        return loc.getBlockX() == SPAWN_X && loc.getBlockZ() == SPAWN_Z;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location loc = block.getLocation();

        if (!isOneBlockWorld(loc.getWorld().getName())) return;
        if (!isOneBlockLocation(loc)) return;

        long now = System.currentTimeMillis();
        Long lastBreak = lastBreakTime.get(player.getUniqueId());
        if (lastBreak != null && (now - lastBreak) < BREAK_COOLDOWN) {
            event.setCancelled(true);
            return;
        }
        lastBreakTime.put(player.getUniqueId(), now);

        OneBlockProgress progress = getProgress(player.getUniqueId());
        OneBlockPhase currentPhase = phases.get(progress.currentPhase);
        if (currentPhase == null) return;

        event.setCancelled(false);
        progress.blocksBroken++;
        progress.totalBroken++;

        if (progress.blocksBroken >= currentPhase.blocksRequired) {
            completePhase(player, progress, currentPhase);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Location spawnLoc = new Location(
                    loc.getWorld(),
                    SPAWN_X, SPAWN_Y, SPAWN_Z
            );
            Block spawnBlock = spawnLoc.getBlock();
            spawnBlock.setType(getNextBlockMaterial(currentPhase));
        }, 1L);
    }

    private Material getNextBlockMaterial(OneBlockPhase phase) {
        if (phase.nextPhase != null && !phase.nextPhase.isEmpty()) {
            OneBlockPhase next = phases.get(phase.nextPhase);
            if (next != null && next.material != null) {
                return next.material;
            }
        }
        return phase.material != null ? phase.material : Material.STONE;
    }

    private void completePhase(Player player, OneBlockProgress progress, OneBlockPhase phase) {
        progress.blocksBroken = 0;
        progress.phasesCompleted++;

        plugin.getCoinManager().addCoins(player, phase.rewardCoins);
        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "PHASE COMPLETE: " + phase.displayName + "!");
        player.sendMessage(ChatColor.YELLOW + "+" + phase.rewardCoins + " coins!");

        if (!phase.rewardItem.isEmpty()) {
            Material rewardMat = Material.matchMaterial(phase.rewardItem);
            if (rewardMat != null) {
                ItemStack reward = new ItemStack(rewardMat, phase.rewardAmount);
                player.getInventory().addItem(reward);
                player.sendMessage(ChatColor.AQUA + "Reward: " + phase.rewardAmount + " " + phase.displayName + " items!");
            }
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        if (!phase.nextPhase.isEmpty() && phases.containsKey(phase.nextPhase)) {
            progress.currentPhase = phase.nextPhase;
            OneBlockPhase nextPhase = phases.get(phase.nextPhase);
            player.sendMessage(ChatColor.GOLD + "Next phase: " + nextPhase.displayName);
            player.sendMessage(ChatColor.GRAY + "" + nextPhase.description);
        } else {
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You have completed all OneBlock phases!");
        }

        Bukkit.broadcastMessage(ChatColor.GOLD + player.getName() + " completed OneBlock phase: " + phase.displayName + "!");
    }

    public OneBlockProgress getProgress(UUID uuid) {
        return playerProgress.computeIfAbsent(uuid, k -> {
            OneBlockProgress p = new OneBlockProgress();
            p.currentPhase = "dirt";
            p.blocksBroken = 0;
            p.totalBroken = 0;
            p.phasesCompleted = 0;
            return p;
        });
    }

    public OneBlockPhase getPhase(String id) {
        return phases.get(id);
    }

    public Collection<OneBlockPhase> getAllPhases() {
        return phases.values();
    }

    public String getCurrentPhaseName(UUID uuid) {
        OneBlockProgress progress = getProgress(uuid);
        OneBlockPhase phase = phases.get(progress.currentPhase);
        return phase != null ? phase.displayName : "Unknown";
    }

    public int getPhaseProgress(UUID uuid) {
        OneBlockProgress progress = getProgress(uuid);
        OneBlockPhase phase = phases.get(progress.currentPhase);
        if (phase == null) return 0;
        return (int) ((double) progress.blocksBroken / phase.blocksRequired * 100);
    }

    public void resetPlayer(UUID uuid) {
        playerProgress.remove(uuid);
    }

    public Map<UUID, OneBlockProgress> getAllProgress() {
        return Collections.unmodifiableMap(playerProgress);
    }

    public List<Map.Entry<UUID, OneBlockProgress>> getTopPlayers(int limit) {
        return playerProgress.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().totalBroken, a.getValue().totalBroken))
                .limit(limit)
                .toList();
    }

    public static class OneBlockPhase {
        public String id;
        public String displayName;
        public Material material;
        public int blocksRequired;
        public int rewardCoins;
        public String rewardItem;
        public int rewardAmount;
        public String nextPhase;
        public String description;
    }

    public static class OneBlockProgress {
        public String currentPhase = "dirt";
        public int blocksBroken = 0;
        public int totalBroken = 0;
        public int phasesCompleted = 0;
    }
}
