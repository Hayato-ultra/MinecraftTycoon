package com.generator.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class ConfigManager {

    private final FileConfiguration config;
    private final Map<String, GeneratorTypeData> generatorTypes = new LinkedHashMap<>();
    private final Map<String, CategoryData> categories = new LinkedHashMap<>();
    private final Map<Integer, Double> upgradeCosts = new TreeMap<>();
    private final Map<Integer, int[]> levelStats = new TreeMap<>();
    private static final int[] DEFAULT_LEVEL_STATS = {60, 16};
    private final Map<String, Integer> sellPrices = new HashMap<>();
    private final Map<Integer, Map<String, Object>> dailyRewards = new TreeMap<>();
    private final Map<Integer, Integer> levelXp = new TreeMap<>();
    private final Map<String, Integer> xpSources = new HashMap<>();
    private List<String> pvpWorlds = new ArrayList<>();

    public ConfigManager(FileConfiguration config) {
        this.config = config;
        load();
    }

    public void load() {
        generatorTypes.clear();
        categories.clear();
        upgradeCosts.clear();
        levelStats.clear();
        sellPrices.clear();
        dailyRewards.clear();
        levelXp.clear();
        xpSources.clear();
        pvpWorlds = config.getStringList("settings.pvp-worlds");
        if (pvpWorlds.isEmpty()) {
            pvpWorlds = Arrays.asList("pvp", "P_V_P");
        }

        ConfigurationSection genSection = config.getConfigurationSection("generators");
        if (genSection != null) {
            for (String key : genSection.getKeys(false)) {
                ConfigurationSection gen = genSection.getConfigurationSection(key);
                if (gen == null) continue;

                GeneratorTypeData data = new GeneratorTypeData();
                data.id = key;
                data.name = gen.getString("name", key);
                data.material = Material.matchMaterial(gen.getString("material", "STONE"));
                data.output = Material.matchMaterial(gen.getString("output", "STONE"));
                if (data.material == null) data.material = Material.STONE;
                if (data.output == null) data.output = Material.STONE;
                data.interval = gen.getInt("interval", 60);
                data.amount = gen.getInt("amount", 1);
                data.storage = gen.getInt("storage", 16);
                data.maxLevel = gen.getInt("max-level", 10);
                data.price = gen.getDouble("price", 0);
                data.category = gen.getString("category", "special");
                data.requiredIslandLevel = gen.getInt("required-island-level", 0);
                data.description = gen.getStringList("description");
                data.isMoneyGenerator = gen.getBoolean("is-money-generator", false);
                data.isSellChest = gen.getBoolean("is-sell-chest", false);
                data.moneyPerItem = Math.max(0, gen.getInt("money-per-item", 0));

                generatorTypes.put(key, data);
            }
        }

        ConfigurationSection catSection = config.getConfigurationSection("categories");
        if (catSection != null) {
            for (String key : catSection.getKeys(false)) {
                ConfigurationSection cat = catSection.getConfigurationSection(key);
                if (cat == null) continue;

                CategoryData data = new CategoryData();
                data.id = key;
                data.name = cat.getString("name", key);
                data.material = Material.matchMaterial(cat.getString("material", "STONE"));
                if (data.material == null) data.material = Material.CHEST;
                data.description = cat.getString("description", "");
                categories.put(key, data);
            }
        }

        ConfigurationSection upgradeSection = config.getConfigurationSection("upgrade-costs");
        if (upgradeSection != null) {
            for (String key : upgradeSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    upgradeCosts.put(level, upgradeSection.getDouble(key));
                } catch (NumberFormatException ignored) {}
            }
        }

        ConfigurationSection levelSection = config.getConfigurationSection("level-stats");
        if (levelSection != null) {
            for (String key : levelSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    List<Integer> stats = levelSection.getIntegerList(key);
                    if (stats.size() >= 2) {
                        levelStats.put(level, new int[]{stats.get(0), stats.get(1)});
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        ConfigurationSection sellSection = config.getConfigurationSection("sell-prices");
        if (sellSection != null) {
            for (String key : sellSection.getKeys(false)) {
                sellPrices.put(key, sellSection.getInt(key));
            }
        }

        ConfigurationSection dailySection = config.getConfigurationSection("daily-rewards.rewards");
        if (dailySection != null) {
            for (String key : dailySection.getKeys(false)) {
                try {
                    int day = Integer.parseInt(key);
                    ConfigurationSection reward = dailySection.getConfigurationSection(key);
                    if (reward != null) {
                        Map<String, Object> rewardData = new HashMap<>();
                        rewardData.put("coins", reward.getInt("coins", 0));
                        rewardData.put("item", reward.getString("item", ""));
                        rewardData.put("item-amount", reward.getInt("item-amount", 1));
                        dailyRewards.put(day, rewardData);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        ConfigurationSection xpSection = config.getConfigurationSection("level-xp");
        if (xpSection != null) {
            for (String key : xpSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    levelXp.put(level, xpSection.getInt(key));
                } catch (NumberFormatException ignored) {}
            }
        }

        ConfigurationSection xpSourcesSection = config.getConfigurationSection("xp-sources");
        if (xpSourcesSection != null) {
            for (String key : xpSourcesSection.getKeys(false)) {
                xpSources.put(key, xpSourcesSection.getInt(key));
            }
        }
    }

    public void reload() {
        load();
    }

    public Map<String, GeneratorTypeData> getGeneratorTypes() { return generatorTypes; }
    public GeneratorTypeData getGeneratorType(String id) { return generatorTypes.get(id); }
    public Collection<GeneratorTypeData> getAllGeneratorTypes() { return generatorTypes.values(); }
    public Map<String, CategoryData> getCategories() { return categories; }
    public CategoryData getCategory(String id) { return categories.get(id); }
    public Collection<CategoryData> getAllCategories() { return categories.values(); }
    public double getUpgradeCost(int level) { return upgradeCosts.getOrDefault(level, Double.MAX_VALUE); }
    public int[] getLevelStats(int level) { return levelStats.getOrDefault(level, DEFAULT_LEVEL_STATS); }
    public int getMaxGeneratorsPerPlayer() { return config.getInt("settings.max-generators-per-player", 50); }
    public boolean isOfflineGenerationEnabled() { return config.getBoolean("settings.offline-generation", true); }
    public int getCheckInterval() { return config.getInt("settings.check-interval", 1); }
    public boolean isHologramEnabled() { return config.getBoolean("settings.hologram-enabled", true); }
    public double getHologramHeight() { return config.getDouble("settings.hologram-height", 1.8); }
    public String getDatabaseType() { return config.getString("settings.database-type", "sqlite"); }
    public Map<String, Integer> getSellPrices() { return sellPrices; }
    public int getSellPrice(String material) { return sellPrices.getOrDefault(material, 0); }
    public Map<Integer, Map<String, Object>> getDailyRewards() { return dailyRewards; }
    public int getLevelXp(int level) { return levelXp.getOrDefault(level, 100); }
    public int getXpSource(String source) { return xpSources.getOrDefault(source, 0); }
    public List<String> getPvpWorlds() { return pvpWorlds; }
    public boolean isPvpWorld(String worldName) { return pvpWorlds.contains(worldName); }
    public boolean isMoneyGenerator(String generatorId) {
        GeneratorTypeData data = generatorTypes.get(generatorId);
        return data != null && data.isMoneyGenerator;
    }
    public boolean isSellChest(String generatorId) {
        GeneratorTypeData data = generatorTypes.get(generatorId);
        return data != null && data.isSellChest;
    }

    public List<GeneratorTypeData> getGeneratorsByCategory(String category) {
        List<GeneratorTypeData> result = new ArrayList<>();
        for (GeneratorTypeData data : generatorTypes.values()) {
            if (data.category.equals(category)) {
                result.add(data);
            }
        }
        return result;
    }

    public static class GeneratorTypeData {
        public String id;
        public String name;
        public Material material;
        public Material output;
        public int interval;
        public int amount;
        public int storage;
        public int maxLevel;
        public double price;
        public String category;
        public int requiredIslandLevel;
        public List<String> description;
        public boolean isMoneyGenerator;
        public boolean isSellChest;
        public int moneyPerItem;
    }

    public static class CategoryData {
        public String id;
        public String name;
        public Material material;
        public String description;
    }
}
