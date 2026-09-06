package com.generator.generator;

import com.generator.GeneratorPlugin;
import com.generator.config.ConfigManager;
import com.generator.profile.PlayerProfile;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GeneratorManager {

    private final GeneratorPlugin plugin;
    private final Map<UUID, Generator> generators = new ConcurrentHashMap<>();
    private final Map<String, UUID> locationIndex = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> playerGenerators = new ConcurrentHashMap<>();
    private BukkitRunnable schedulerTask;

    public GeneratorManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    public static String locationKey(Location loc) {
        if (loc == null || loc.getWorld() == null) return "unknown:0:0:0";
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    public void enable() {
        startScheduler();
    }

    public void disable() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
        }
    }

    public void loadAll() {
        generators.clear();
        locationIndex.clear();
        playerGenerators.clear();

        List<Generator> loaded = plugin.getDatabaseManager().loadAllGenerators();
        for (Generator gen : loaded) {
            calculateOfflineGeneration(gen);
            registerGenerator(gen);
        }
        plugin.getLogger().info("Loaded " + generators.size() + " generators.");
    }

    public void saveAll() {
        for (Generator gen : generators.values()) {
            if (gen.isDirty()) {
                plugin.getDatabaseManager().saveGenerator(gen);
                gen.markClean();
            }
        }
    }

    public Generator createGenerator(Player owner, String type, Location location) {
        ConfigManager configManager = plugin.getConfigManager();
        ConfigManager.GeneratorTypeData typeData = configManager.getGeneratorType(type);
        if (typeData == null) return null;

        UUID id = UUID.randomUUID();
        String islandId = getIslandId(owner, location);

        Generator gen = Generator.fromLocation(id, owner.getUniqueId(), islandId, type,
                location.clone(), 1, 0, System.currentTimeMillis());

        registerGenerator(gen);
        plugin.getDatabaseManager().saveGenerator(gen);

        return gen;
    }

    public void registerGenerator(Generator gen) {
        generators.put(gen.getId(), gen);
        if (gen.getLocation() != null) {
            locationIndex.put(locationKey(gen.getLocation()), gen.getId());
        }
        playerGenerators.computeIfAbsent(gen.getOwnerUUID(), k -> new HashSet<>()).add(gen.getId());
    }

    public void removeGenerator(UUID generatorId) {
        Generator gen = generators.remove(generatorId);
        if (gen != null) {
            if (gen.getLocation() != null) {
                locationIndex.remove(locationKey(gen.getLocation()));
            }
            Set<UUID> playerGens = playerGenerators.get(gen.getOwnerUUID());
            if (playerGens != null) {
                playerGens.remove(generatorId);
                if (playerGens.isEmpty()) {
                    playerGenerators.remove(gen.getOwnerUUID());
                }
            }
            plugin.getDatabaseManager().deleteGenerator(generatorId);
        }
    }

    public Generator getGeneratorAt(Location location) {
        UUID id = locationIndex.get(locationKey(location));
        return id != null ? generators.get(id) : null;
    }

    public Generator getGenerator(UUID id) {
        return generators.get(id);
    }

    public List<Generator> getPlayerGenerators(UUID playerUUID) {
        Set<UUID> ids = playerGenerators.getOrDefault(playerUUID, Collections.emptySet());
        List<Generator> result = new ArrayList<>();
        for (UUID id : ids) {
            Generator gen = generators.get(id);
            if (gen != null) result.add(gen);
        }
        return result;
    }

    public int getPlayerGeneratorCount(UUID playerUUID) {
        return playerGenerators.getOrDefault(playerUUID, Collections.emptySet()).size();
    }

    public void resetGenerators(Player player) {
        UUID playerUUID = player.getUniqueId();
        Set<UUID> ids = new HashSet<>(playerGenerators.getOrDefault(playerUUID, Collections.emptySet()));

        for (UUID id : ids) {
            Generator gen = generators.get(id);
            if (gen == null) continue;

            Location loc = gen.getLocation();
            if (loc != null) {
                org.bukkit.block.Block block = loc.getBlock();
                block.setType(Material.AIR);
            }

            generators.remove(id);
            locationIndex.remove(locationKey(loc));
            plugin.getDatabaseManager().deleteGenerator(id);
        }

        playerGenerators.remove(playerUUID);
    }

    public Collection<Generator> getAllGenerators() {
        return generators.values();
    }

    public int getTotalGeneratorCount() {
        return generators.size();
    }

    private void startScheduler() {
        schedulerTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        };
        schedulerTask.runTaskTimer(plugin, 20L, 20L);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        ConfigManager configManager = plugin.getConfigManager();

        for (Generator gen : generators.values()) {
            if (gen.getLocation() == null || gen.getLocation().getWorld() == null) continue;
            if (gen.isFull(configManager)) continue;

            int interval = gen.getInterval(configManager) * 1000;
            if (plugin.getFutureGenManager() != null) {
                interval = plugin.getFutureGenManager().getAdjustedInterval(
                        gen.getOwnerUUID(), gen.getType(), interval) * 1000;
            }
            if (now - gen.getLastGeneration() >= interval) {
                int amount = gen.getOutputAmount(configManager);
                if (plugin.getFutureGenManager() != null) {
                    double mult = plugin.getFutureGenManager().getProductionMultiplier(
                            gen.getOwnerUUID(), gen.getType());
                    amount = (int) Math.ceil(amount * mult);
                }
                if (gen.canAdd(amount, configManager)) {
                    gen.setStoredAmount(gen.getStoredAmount() + amount);
                    gen.setLastGeneration(now);
                    gen.markDirty();

                    Location loc = gen.getLocation();
                    Player owner = Bukkit.getPlayer(gen.getOwnerUUID());
                    if (owner != null && owner.isOnline() && owner.getWorld().equals(loc.getWorld())) {
                        if (owner.getLocation().distanceSquared(loc) < 100) {
                            owner.playSound(loc, Sound.ENTITY_ITEM_PICKUP, 0.3f, 1.5f);
                        }
                    }
                } else {
                    gen.setLastGeneration(now);
                }
            }
        }
    }

    public boolean collectGenerator(Generator gen, Player player) {
        if (gen.getStoredAmount() <= 0) return false;

        ConfigManager configManager = plugin.getConfigManager();
        ConfigManager.GeneratorTypeData typeData = configManager.getGeneratorType(gen.getType());

        if (typeData != null && typeData.isMoneyGenerator) {
            int coins = gen.getStoredAmount() * typeData.moneyPerItem;
            plugin.getCoinManager().addCoins(player, coins);
            gen.setStoredAmount(0);
            gen.markDirty();
            player.sendMessage(ChatColor.GREEN + "Collected " + ChatColor.GOLD + coins + " coins" + ChatColor.GREEN + "!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

            PlayerProfile profile = plugin.getProfileManager().getProfileIfLoaded(player.getUniqueId());
            if (profile != null) {
                profile.addGeneratorsCollected();
                profile.addCoinsEarned(coins);
            }

            plugin.getLevelManager().addXp(player, plugin.getConfigManager().getXpSource("generator-collect"));
            if (plugin.getFutureGenManager() != null) {
                plugin.getFutureGenManager().addMastery(player.getUniqueId(), gen.getType(), 1);
            }
            return true;
        }

        Material outputMaterial = gen.getOutputMaterial(configManager);
        int amount = gen.getStoredAmount();

        ItemStack itemStack = new ItemStack(outputMaterial, Math.min(amount, outputMaterial.getMaxStackSize()));
        HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(itemStack);

        int collected = amount;
        if (!remaining.isEmpty()) {
            collected = amount - remaining.values().stream().mapToInt(ItemStack::getAmount).sum();
        }

        if (collected <= 0) {
            player.sendMessage(ChatColor.RED + "Your inventory is full!");
            return false;
        }

        gen.setStoredAmount(gen.getStoredAmount() - collected);
        gen.markDirty();

        if (collected < amount) {
            player.sendMessage(ChatColor.YELLOW + "Collected " + collected + " " +
                    outputMaterial.name().replace("_", " ").toLowerCase() +
                    " (inventory was partially full)");
        } else {
            player.sendMessage(ChatColor.GREEN + "Collected " + collected + " " +
                    outputMaterial.name().replace("_", " ").toLowerCase());
        }

        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);

        PlayerProfile profile = plugin.getProfileManager().getProfileIfLoaded(player.getUniqueId());
        if (profile != null) {
            profile.addGeneratorsCollected();
        }

        if (plugin.getFutureGenManager() != null) {
            plugin.getFutureGenManager().addMastery(player.getUniqueId(), gen.getType(), 1);
        }

        return true;
    }

    public boolean collectAllGenerators(Player player) {
        List<Generator> gens = getPlayerGenerators(player.getUniqueId());
        int totalCollected = 0;
        int totalCoins = 0;

        for (Generator gen : gens) {
            if (gen.getStoredAmount() <= 0) continue;

            ConfigManager configManager = plugin.getConfigManager();
            ConfigManager.GeneratorTypeData typeData = configManager.getGeneratorType(gen.getType());

            if (typeData != null && typeData.isMoneyGenerator) {
                int coins = gen.getStoredAmount() * typeData.moneyPerItem;
                totalCoins += coins;
                gen.setStoredAmount(0);
                gen.markDirty();
                continue;
            }

            Material outputMaterial = gen.getOutputMaterial(configManager);
            int amount = gen.getStoredAmount();

            int spaceAvailable = 0;
            for (int i = 0; i < 36; i++) {
                ItemStack slot = player.getInventory().getItem(i);
                if (slot == null) {
                    spaceAvailable += outputMaterial.getMaxStackSize();
                } else if (slot.getType() == outputMaterial) {
                    spaceAvailable += slot.getMaxStackSize() - slot.getAmount();
                }
            }

            int toCollect = Math.min(amount, spaceAvailable);
            if (toCollect <= 0) continue;

            int remaining = toCollect;
            while (remaining > 0) {
                int stackSize = Math.min(remaining, outputMaterial.getMaxStackSize());
                ItemStack itemStack = new ItemStack(outputMaterial, stackSize);
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(itemStack);
                int actuallyAdded = stackSize;
                if (!overflow.isEmpty()) {
                    for (ItemStack drop : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                    actuallyAdded -= overflow.values().stream().mapToInt(ItemStack::getAmount).sum();
                }
                remaining -= actuallyAdded;
            }

            gen.setStoredAmount(gen.getStoredAmount() - toCollect);
            gen.markDirty();
            totalCollected += toCollect;
        }

        if (totalCoins > 0) {
            plugin.getCoinManager().addCoins(player, totalCoins);
            player.sendMessage(ChatColor.GREEN + "Collected " + ChatColor.GOLD + totalCoins + " coins" + ChatColor.GREEN + " from money generators!");
        }

        if (totalCollected > 0) {
            player.sendMessage(ChatColor.GREEN + "Collected " + totalCollected + " items from all generators!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            PlayerProfile profile = plugin.getProfileManager().getProfileIfLoaded(player.getUniqueId());
            if (profile != null) {
                profile.addGeneratorsCollected();
            }

            return true;
        } else if (totalCoins <= 0) {
            player.sendMessage(ChatColor.YELLOW + "No items to collect.");
            return false;
        }
        return true;
    }

    public boolean canPlaceGenerator(Player player) {
        ConfigManager configManager = plugin.getConfigManager();
        int max = configManager.getMaxGeneratorsPerPlayer();
        return getPlayerGeneratorCount(player.getUniqueId()) < max;
    }

    public void calculateOfflineGeneration(Generator gen) {
        if (!plugin.getConfigManager().isOfflineGenerationEnabled()) return;
        if (gen.getLocation() == null) return;

        long now = System.currentTimeMillis();
        long elapsed = now - gen.getLastGeneration();
        ConfigManager configManager = plugin.getConfigManager();

        int interval = gen.getInterval(configManager) * 1000;
        int amount = gen.getOutputAmount(configManager);
        int maxStorage = gen.getMaxStorage(configManager);

        long productions = elapsed / interval;
        if (productions <= 0) return;

        int totalGenerated = (int) (productions * amount);
        int newStored = Math.min(gen.getStoredAmount() + totalGenerated, maxStorage);

        if (newStored != gen.getStoredAmount()) {
            gen.setStoredAmount(newStored);
            gen.setLastGeneration(now);
            gen.markDirty();
        }
    }

    private String getIslandId(Player player, Location location) {
        try {
            Class<?> bentoBoxClass = Class.forName("world.bentobox.bentobox.BentoBox");
            Object bentoBox = bentoBoxClass.getMethod("getInstance").invoke(null);
            Object islandsManager = bentoBoxClass.getMethod("getIslands").invoke(bentoBox);
            Object island = islandsManager.getClass().getMethod("getIslandAt", Location.class)
                    .invoke(islandsManager, location);
            if (island != null) {
                Object uniqueId = island.getClass().getMethod("getUniqueId").invoke(island);
                return uniqueId != null ? uniqueId.toString() : "unknown";
            }
        } catch (Exception ignored) {
            // BentoBox not available or error
        }
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockZ();
    }
}
