package com.generator.prestige;

import com.generator.GeneratorPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PrestigeManager {

    private final GeneratorPlugin plugin;
    private final File prestigeFile;
    private final Map<UUID, Integer> prestigeLevels = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> prestigeCoins = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Integer>> purchasedBonuses = new ConcurrentHashMap<>();

    private static final int MAX_PRESTIGE_LEVEL = 100;
    private static final double COIN_MULTIPLIER_PER_LEVEL = 0.10;
    private static final double GENERATION_SPEED_PER_LEVEL = 0.05;
    private static final int EXTRA_HOMES_PER_5_LEVELS = 1;

    private static final int REQUIREMENT_ISLAND_LEVEL = 50;
    private static final int REQUIREMENT_COINS = 1000000;
    private static final int REQUIREMENT_MIN_GENERATORS = 1;
    private static final int PRESTIGE_COINS_REWARD = 100;
    private static final int COIN_BONUS_PER_LEVEL = 50;

    public PrestigeManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
        this.prestigeFile = new File(plugin.getDataFolder(), "prestige.yml");
        load();
    }

    public void load() {
        prestigeLevels.clear();
        prestigeCoins.clear();
        purchasedBonuses.clear();
        if (!prestigeFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(prestigeFile);
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) return;

        for (String uuidStr : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection cs = players.getConfigurationSection(uuidStr);
                if (cs == null) continue;

                prestigeLevels.put(uuid, cs.getInt("level", 0));
                prestigeCoins.put(uuid, cs.getInt("prestige-coins", 0));

                Map<String, Integer> bonuses = new HashMap<>();
                ConfigurationSection bonusSection = cs.getConfigurationSection("bonuses");
                if (bonusSection != null) {
                    for (String key : bonusSection.getKeys(false)) {
                        bonuses.put(key, bonusSection.getInt(key, 0));
                    }
                }
                purchasedBonuses.put(uuid, bonuses);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();

        for (UUID uuid : prestigeLevels.keySet()) {
            String path = "players." + uuid.toString();
            config.set(path + ".level", prestigeLevels.getOrDefault(uuid, 0));
            config.set(path + ".prestige-coins", prestigeCoins.getOrDefault(uuid, 0));

            Map<String, Integer> bonuses = purchasedBonuses.getOrDefault(uuid, Collections.emptyMap());
            for (Map.Entry<String, Integer> entry : bonuses.entrySet()) {
                config.set(path + ".bonuses." + entry.getKey(), entry.getValue());
            }
        }

        try {
            config.save(prestigeFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save prestige.yml: " + e.getMessage());
        }
    }

    public int getPrestigeLevel(UUID uuid) {
        return prestigeLevels.getOrDefault(uuid, 0);
    }

    public int getPrestigeCoins(UUID uuid) {
        return prestigeCoins.getOrDefault(uuid, 0);
    }

    public int getBonusLevel(UUID uuid, String bonusId) {
        return purchasedBonuses.getOrDefault(uuid, Collections.emptyMap()).getOrDefault(bonusId, 0);
    }

    public boolean canPrestige(Player player) {
        if (getPrestigeLevel(player.getUniqueId()) >= MAX_PRESTIGE_LEVEL) return false;

        int islandLevel = getIslandLevel(player);
        if (islandLevel < REQUIREMENT_ISLAND_LEVEL) return false;

        int coins = plugin.getCoinManager().getCoins(player);
        if (coins < REQUIREMENT_COINS) return false;

        return true;
    }

    public String getPrestigeFailReason(Player player) {
        int level = getPrestigeLevel(player.getUniqueId());
        if (level >= MAX_PRESTIGE_LEVEL) return "Max prestige level reached!";

        int islandLevel = getIslandLevel(player);
        if (islandLevel < REQUIREMENT_ISLAND_LEVEL) {
            return "Need island level " + REQUIREMENT_ISLAND_LEVEL + " (you: " + islandLevel + ")";
        }

        int coins = plugin.getCoinManager().getCoins(player);
        if (coins < REQUIREMENT_COINS) {
            return "Need " + String.format("%,d", REQUIREMENT_COINS) + " coins (you: " + String.format("%,d", coins) + ")";
        }

        return null;
    }

    public boolean prestige(Player player) {
        String failReason = getPrestigeFailReason(player);
        if (failReason != null) return false;

        UUID uuid = player.getUniqueId();
        int currentLevel = getPrestigeLevel(uuid);

        plugin.getGeneratorManager().resetGenerators(player);

        plugin.getCoinManager().setCoins(player, 0);

        int newLevel = currentLevel + 1;
        prestigeLevels.put(uuid, newLevel);

        int coinsEarned = PRESTIGE_COINS_REWARD + (newLevel * COIN_BONUS_PER_LEVEL);
        prestigeCoins.merge(uuid, coinsEarned, Integer::sum);

        save();
        return true;
    }

    public double getCoinMultiplier(UUID uuid) {
        int level = getPrestigeLevel(uuid);
        int bonus = getBonusLevel(uuid, "coin_multiplier");
        return 1.0 + (level * COIN_MULTIPLIER_PER_LEVEL) + (bonus * 0.05);
    }

    public double getGenerationSpeedMultiplier(UUID uuid) {
        int level = getPrestigeLevel(uuid);
        int bonus = getBonusLevel(uuid, "speed_boost");
        return 1.0 + (level * GENERATION_SPEED_PER_LEVEL) + (bonus * 0.05);
    }

    public int getExtraHomes(UUID uuid) {
        int level = getPrestigeLevel(uuid);
        return level / EXTRA_HOMES_PER_5_LEVELS;
    }

    public List<String> getAvailableBonuses() {
        List<String> bonuses = new ArrayList<>();
        bonuses.add("coin_multiplier");
        bonuses.add("speed_boost");
        bonuses.add("extra_homes");
        bonuses.add("bonus_xp");
        bonuses.add("bonus_chest");
        return bonuses;
    }

    public int getBonusCost(UUID uuid, String bonusId) {
        int currentLevel = getBonusLevel(uuid, bonusId);
        return 50 + (currentLevel * 25);
    }

    public boolean purchaseBonus(Player player, String bonusId) {
        UUID uuid = player.getUniqueId();
        int cost = getBonusCost(uuid, bonusId);
        int coins = getPrestigeCoins(uuid);

        if (coins < cost) return false;

        prestigeCoins.merge(uuid, -cost, Integer::sum);
        purchasedBonuses.computeIfAbsent(uuid, k -> new HashMap<>()).merge(bonusId, 1, Integer::sum);

        save();
        return true;
    }

    public Map<UUID, Integer> getAllPrestigeLevels() {
        return Collections.unmodifiableMap(prestigeLevels);
    }

    public List<Map.Entry<UUID, Integer>> getTopPrestige(int limit) {
        return prestigeLevels.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .toList();
    }

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
}
