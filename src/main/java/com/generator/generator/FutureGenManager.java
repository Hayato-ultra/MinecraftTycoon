package com.generator.generator;

import com.generator.GeneratorPlugin;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FutureGenManager {

    private final GeneratorPlugin plugin;
    private final Map<String, FusionRecipe> fusionRecipes = new LinkedHashMap<>();
    private final Map<String, EvolutionData> evolutionTree = new LinkedHashMap<>();
    private final Map<String, SecretGenData> secretGenerators = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Integer>> masteryData = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, String>> specializations = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Boolean>> unlockedSecrets = new ConcurrentHashMap<>();

    private File dataFile;
    private FileConfiguration dataConfig;

    public FutureGenManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
        loadDataFile();
        loadDefaults();
        load();
    }

    // === FUSION ===

    public static class FusionRecipe {
        public String id;
        public String name;
        public String resultType;
        public String ingredient1;
        public String ingredient2;
        public int ingredient1Amount;
        public int ingredient2Amount;
        public double coinCost;
        public int requiredPrestige;
        public Material resultMaterial;
        public String description;

        public FusionRecipe(String id, String name, String resultType, String ingredient1, String ingredient2,
                           int amount1, int amount2, double coinCost, int requiredPrestige, Material resultMaterial, String description) {
            this.id = id;
            this.name = name;
            this.resultType = resultType;
            this.ingredient1 = ingredient1;
            this.ingredient2 = ingredient2;
            this.ingredient1Amount = amount1;
            this.ingredient2Amount = amount2;
            this.coinCost = coinCost;
            this.requiredPrestige = requiredPrestige;
            this.resultMaterial = resultMaterial;
            this.description = description;
        }
    }

    public boolean canFuse(Player player, String recipeId) {
        FusionRecipe recipe = fusionRecipes.get(recipeId);
        if (recipe == null) return false;

        if (plugin.getPrestigeManager() != null) {
            int prestige = plugin.getPrestigeManager().getPrestigeLevel(player.getUniqueId());
            if (prestige < recipe.requiredPrestige) return false;
        }

        if (plugin.getCoinManager().getCoins(player) < recipe.coinCost) return false;

        int ing1Count = 0;
        int ing2Count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            String itemId = item.getType().name();
            if (itemId.equals(recipe.ingredient1)) ing1Count += item.getAmount();
            if (itemId.equals(recipe.ingredient2)) ing2Count += item.getAmount();
        }

        return ing1Count >= recipe.ingredient1Amount && ing2Count >= recipe.ingredient2Amount;
    }

    public boolean fuse(Player player, String recipeId) {
        if (!canFuse(player, recipeId)) return false;

        FusionRecipe recipe = fusionRecipes.get(recipeId);

        plugin.getCoinManager().removeCoins(player, (int) recipe.coinCost);

        removeItems(player, recipe.ingredient1, recipe.ingredient1Amount);
        removeItems(player, recipe.ingredient2, recipe.ingredient2Amount);

        GeneratorManager genManager = plugin.getGeneratorManager();
        Generator gen = genManager.createGenerator(player, recipe.resultType, player.getLocation());

        if (gen != null) {
            player.sendMessage(ChatColor.GREEN + "Fused " + ChatColor.GOLD + recipe.name + ChatColor.GREEN + "!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            return true;
        }
        return false;
    }

    public Map<String, FusionRecipe> getFusionRecipes() { return fusionRecipes; }
    public FusionRecipe getFusionRecipe(String id) { return fusionRecipes.get(id); }

    // === EVOLUTION ===

    public static class EvolutionData {
        public String generatorType;
        public List<EvolutionStage> stages = new ArrayList<>();

        public static class EvolutionStage {
            public int stage;
            public int requiredMastery;
            public double coinCost;
            public double multiplier;
            public Material outputOverride;
            public int intervalReduction;
            public String ability;
            public String abilityDescription;

            public EvolutionStage(int stage, int requiredMastery, double coinCost, double multiplier,
                                 Material outputOverride, int intervalReduction, String ability, String abilityDescription) {
                this.stage = stage;
                this.requiredMastery = requiredMastery;
                this.coinCost = coinCost;
                this.multiplier = multiplier;
                this.outputOverride = outputOverride;
                this.intervalReduction = intervalReduction;
                this.ability = ability;
                this.abilityDescription = abilityDescription;
            }
        }
    }

    public int getEvolutionStage(UUID playerUUID, String generatorType) {
        Map<String, String> specs = specializations.getOrDefault(playerUUID, Collections.emptyMap());
        String stageStr = specs.get(generatorType + "_evolution");
        if (stageStr == null) return 0;
        try {
            return Integer.parseInt(stageStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public boolean canEvolve(Player player, String generatorType) {
        EvolutionData evo = evolutionTree.get(generatorType);
        if (evo == null || evo.stages.isEmpty()) return false;

        int currentStage = getEvolutionStage(player.getUniqueId(), generatorType);
        int nextStage = currentStage + 1;
        if (nextStage > evo.stages.size()) return false;

        EvolutionData.EvolutionStage stage = evo.stages.get(nextStage - 1);
        int mastery = getPlayerMastery(player.getUniqueId(), generatorType);

        if (mastery < stage.requiredMastery) return false;
        if (plugin.getCoinManager().getCoins(player) < stage.coinCost) return false;

        return true;
    }

    public boolean evolve(Player player, String generatorType) {
        if (!canEvolve(player, generatorType)) return false;

        EvolutionData evo = evolutionTree.get(generatorType);
        int currentStage = getEvolutionStage(player.getUniqueId(), generatorType);
        EvolutionData.EvolutionStage stage = evo.stages.get(currentStage);

        plugin.getCoinManager().removeCoins(player, (int) stage.coinCost);

        specializations.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(generatorType + "_evolution", String.valueOf(currentStage + 1));

        player.sendMessage(ChatColor.GREEN + "Evolved to " + ChatColor.GOLD + stage.ability + ChatColor.GREEN + "!");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.5f);

        save();
        return true;
    }

    public double getEvolutionMultiplier(UUID playerUUID, String generatorType) {
        int stage = getEvolutionStage(playerUUID, generatorType);
        if (stage == 0) return 1.0;

        EvolutionData evo = evolutionTree.get(generatorType);
        if (evo == null || stage > evo.stages.size()) return 1.0;

        return evo.stages.get(stage - 1).multiplier;
    }

    public int getEvolutionIntervalReduction(UUID playerUUID, String generatorType) {
        int stage = getEvolutionStage(playerUUID, generatorType);
        if (stage == 0) return 0;

        EvolutionData evo = evolutionTree.get(generatorType);
        if (evo == null || stage > evo.stages.size()) return 0;

        return evo.stages.get(stage - 1).intervalReduction;
    }

    // === RARE & SECRET GENERATORS ===

    public static class SecretGenData {
        public String id;
        public String name;
        public String requiredPermission;
        public int requiredPrestige;
        public int requiredMasteryLevel;
        public double coinCost;
        public Material material;
        public Material output;
        public int interval;
        public int amount;
        public int storage;
        public double productionMultiplier;
        public String specialAbility;
        public List<String> description;
        public boolean discovered;

        public SecretGenData(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public boolean isSecretUnlocked(UUID playerUUID, String secretId) {
        return unlockedSecrets.getOrDefault(playerUUID, Collections.emptyMap())
                .getOrDefault(secretId, false);
    }

    public boolean canUnlockSecret(Player player, String secretId) {
        SecretGenData secret = secretGenerators.get(secretId);
        if (secret == null) return false;
        if (isSecretUnlocked(player.getUniqueId(), secretId)) return false;

        if (secret.requiredPermission != null && !secret.requiredPermission.isEmpty()) {
            if (!player.hasPermission(secret.requiredPermission)) return false;
        }

        if (plugin.getPrestigeManager() != null) {
            int prestige = plugin.getPrestigeManager().getPrestigeLevel(player.getUniqueId());
            if (prestige < secret.requiredPrestige) return false;
        }

        int mastery = getPlayerMastery(player.getUniqueId(), secret.id);
        if (mastery < secret.requiredMasteryLevel) return false;

        if (plugin.getCoinManager().getCoins(player) < secret.coinCost) return false;

        return true;
    }

    public boolean unlockSecret(Player player, String secretId) {
        if (!canUnlockSecret(player, secretId)) return false;

        SecretGenData secret = secretGenerators.get(secretId);
        plugin.getCoinManager().removeCoins(player, (int) secret.coinCost);

        unlockedSecrets.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(secretId, true);

        player.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "SECRET UNLOCKED: " + ChatColor.GOLD + secret.name + "!");
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        save();
        return true;
    }

    // === MASTERY ===

    public int getPlayerMastery(UUID playerUUID, String generatorType) {
        return masteryData.getOrDefault(playerUUID, Collections.emptyMap())
                .getOrDefault(generatorType, 0);
    }

    public void addMastery(UUID playerUUID, String generatorType, int amount) {
        masteryData.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>())
                .merge(generatorType, amount, Integer::sum);
    }

    public int getMasteryLevel(UUID playerUUID, String generatorType) {
        int mastery = getPlayerMastery(playerUUID, generatorType);
        return mastery / 100;
    }

    public int getMasteryProgress(UUID playerUUID, String generatorType) {
        int mastery = getPlayerMastery(playerUUID, generatorType);
        return mastery % 100;
    }

    public double getMasteryBonus(UUID playerUUID, String generatorType) {
        int level = getMasteryLevel(playerUUID, generatorType);
        return 1.0 + (level * 0.05);
    }

    // === SPECIALIZATIONS ===

    public String getSpecialization(UUID playerUUID, String generatorType) {
        return specializations.getOrDefault(playerUUID, Collections.emptyMap())
                .get(generatorType + "_spec");
    }

    public boolean setSpecialization(UUID playerUUID, String generatorType, String spec) {
        specializations.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>())
                .put(generatorType + "_spec", spec);
        save();
        return true;
    }

    // === ADVANCED PRODUCTION ===

    public double getProductionMultiplier(UUID playerUUID, String generatorType) {
        double mult = 1.0;
        mult *= getEvolutionMultiplier(playerUUID, generatorType);
        mult *= getMasteryBonus(playerUUID, generatorType);

        String spec = getSpecialization(playerUUID, generatorType);
        if (spec != null) {
            switch (spec) {
                case "speed" -> mult *= 1.25;
                case "quantity" -> mult *= 1.5;
                case "luck" -> mult *= 1.0;
            }
        }

        SecretGenData secret = secretGenerators.get(generatorType);
        if (secret != null && isSecretUnlocked(playerUUID, generatorType)) {
            mult *= secret.productionMultiplier;
        }

        return mult;
    }

    public int getAdjustedInterval(UUID playerUUID, String generatorType, int baseInterval) {
        int reduction = getEvolutionIntervalReduction(playerUUID, generatorType);
        String spec = getSpecialization(playerUUID, generatorType);
        if ("speed".equals(spec)) {
            reduction += 10;
        }
        return Math.max(1, baseInterval - reduction);
    }

    // === PERSISTENCE ===

    private void loadDataFile() {
        dataFile = new File(plugin.getDataFolder(), "future_generators.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create future_generators.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void loadDefaults() {
        fusionRecipes.put("stone_to_iron", new FusionRecipe(
                "stone_to_iron", "Stone-Iron Fusion", "advanced",
                "COBBLESTONE", "COAL", 64, 32, 5000, 0,
                Material.IRON_ORE, "Fuse cobblestone and coal to create an advanced generator"
        ));
        fusionRecipes.put("iron_to_gold", new FusionRecipe(
                "iron_to_gold", "Iron-Gold Fusion", "ultimate",
                "IRON_INGOT", "REDSTONE", 32, 16, 15000, 2,
                Material.GOLD_ORE, "Fuse iron and redstone for an ultimate generator"
        ));
        fusionRecipes.put("diamond_to_netherite", new FusionRecipe(
                "diamond_to_netherite", "Diamond-Netherite Fusion", "netherite",
                "DIAMOND", "BLAZE_POWDER", 16, 8, 50000, 5,
                Material.ANCIENT_DEBRIS, "The ultimate fusion for netherite generators"
        ));
        fusionRecipes.put("coal_to_charcoal", new FusionRecipe(
                "coal_to_charcoal", "Coal-Charcoal Fusion", "charcoal",
                "COAL", "STICK", 32, 16, 1000, 0,
                Material.CHARCOAL, "Basic fusion for charcoal production"
        ));
        fusionRecipes.put("redstone_to_lapis", new FusionRecipe(
                "redstone_to_lapis", "Redstone-Lapis Fusion", "lapis",
                "REDSTONE", "GLOWSTONE_DUST", 24, 12, 8000, 1,
                Material.LAPIS_LAZULI, "Combine redstone and glowstone for lapis"
        ));

        addEvolution("basic", new EvolutionData.EvolutionStage(1, 100, 2000, 1.25, null, 5, "Speed Boost", "25% faster production"));
        addEvolution("basic", new EvolutionData.EvolutionStage(2, 300, 8000, 1.5, null, 10, "Double Output", "50% more output"));
        addEvolution("basic", new EvolutionData.EvolutionStage(3, 600, 25000, 2.0, null, 15, "Triple Threat", "Double output + speed"));

        addEvolution("advanced", new EvolutionData.EvolutionStage(1, 200, 5000, 1.3, null, 8, "Enhanced Processing", "30% faster + more output"));
        addEvolution("advanced", new EvolutionData.EvolutionStage(2, 500, 20000, 1.75, null, 12, "Quantum Boost", "75% more output"));

        addEvolution("diamond", new EvolutionData.EvolutionStage(1, 400, 15000, 1.5, null, 10, "Crystal Focus", "50% production bonus"));
        addEvolution("diamond", new EvolutionData.EvolutionStage(2, 800, 60000, 2.5, null, 20, "Diamond Core", "150% production bonus"));

        addEvolution("netherite", new EvolutionData.EvolutionStage(1, 600, 40000, 2.0, null, 15, "Nether Infusion", "Double production"));
        addEvolution("netherite", new EvolutionData.EvolutionStage(2, 1000, 150000, 3.0, null, 25, "Ancient Power", "Triple production"));

        SecretGenData luckyGen = new SecretGenData("lucky_generator", "Lucky Generator");
        luckyGen.requiredPrestige = 3;
        luckyGen.requiredMasteryLevel = 5;
        luckyGen.coinCost = 100000;
        luckyGen.material = Material.NETHER_STAR;
        luckyGen.output = Material.DIAMOND;
        luckyGen.interval = 30;
        luckyGen.amount = 1;
        luckyGen.storage = 64;
        luckyGen.productionMultiplier = 2.0;
        luckyGen.specialAbility = "lucky_drops";
        luckyGen.description = Arrays.asList("§5§lSECRET: Lucky Generator", "", "§7Produces rare drops with", "§7a chance for bonus items!", "", "§eAbility: §aLucky Drops");
        secretGenerators.put("lucky_generator", luckyGen);

        SecretGenData quantumGen = new SecretGenData("quantum_generator", "Quantum Generator");
        quantumGen.requiredPrestige = 7;
        quantumGen.requiredMasteryLevel = 10;
        quantumGen.coinCost = 500000;
        quantumGen.material = Material.END_CRYSTAL;
        quantumGen.output = Material.EMERALD;
        quantumGen.interval = 15;
        quantumGen.amount = 2;
        quantumGen.storage = 128;
        quantumGen.productionMultiplier = 3.0;
        quantumGen.specialAbility = "quantum_tunnel";
        quantumGen.description = Arrays.asList("§5§lSECRET: Quantum Generator", "", "§7Harnesses quantum tunneling", "§7for massive output!", "", "§eAbility: §aQuantum Tunnel");
        secretGenerators.put("quantum_generator", quantumGen);

        SecretGenData voidGen = new SecretGenData("void_generator", "Void Generator");
        voidGen.requiredPrestige = 10;
        voidGen.requiredMasteryLevel = 15;
        voidGen.coinCost = 2000000;
        voidGen.material = Material.DRAGON_EGG;
        voidGen.output = Material.NETHERITE_INGOT;
        voidGen.interval = 60;
        voidGen.amount = 1;
        voidGen.storage = 32;
        voidGen.productionMultiplier = 5.0;
        voidGen.specialAbility = "void_extract";
        voidGen.description = Arrays.asList("§5§lSECRET: Void Generator", "", "§7Extracts materials from", "§7the void itself!", "", "§eAbility: §aVoid Extract");
        secretGenerators.put("void_generator", voidGen);

        SecretGenData timeGen = new SecretGenData("time_generator", "Time Generator");
        timeGen.requiredPrestige = 15;
        timeGen.requiredMasteryLevel = 20;
        timeGen.coinCost = 10000000;
        timeGen.material = Material.CLOCK;
        timeGen.output = Material.NETHER_STAR;
        timeGen.interval = 120;
        timeGen.amount = 1;
        timeGen.storage = 16;
        timeGen.productionMultiplier = 10.0;
        timeGen.specialAbility = "time_warp";
        timeGen.description = Arrays.asList("§5§lSECRET: Time Generator", "", "§7Manipulates time to produce", "§7the rarest materials!", "", "§eAbility: §aTime Warp");
        secretGenerators.put("time_generator", timeGen);
    }

    private void addEvolution(String generatorType, EvolutionData.EvolutionStage stage) {
        evolutionTree.computeIfAbsent(generatorType, k -> new EvolutionData()).stages.add(stage);
    }

    private void save() {
        try {
            // Save mastery
            for (Map.Entry<UUID, Map<String, Integer>> entry : masteryData.entrySet()) {
                String path = "mastery." + entry.getKey();
                for (Map.Entry<String, Integer> mEntry : entry.getValue().entrySet()) {
                    dataConfig.set(path + "." + mEntry.getKey(), mEntry.getValue());
                }
            }

            // Save specializations
            for (Map.Entry<UUID, Map<String, String>> entry : specializations.entrySet()) {
                String path = "specializations." + entry.getKey();
                for (Map.Entry<String, String> sEntry : entry.getValue().entrySet()) {
                    dataConfig.set(path + "." + sEntry.getKey(), sEntry.getValue());
                }
            }

            // Save unlocked secrets
            for (Map.Entry<UUID, Map<String, Boolean>> entry : unlockedSecrets.entrySet()) {
                String path = "unlocked-secrets." + entry.getKey();
                for (Map.Entry<String, Boolean> sEntry : entry.getValue().entrySet()) {
                    dataConfig.set(path + "." + sEntry.getKey(), sEntry.getValue());
                }
            }

            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save future_generators.yml: " + e.getMessage());
        }
    }

    private void load() {
        // Load mastery
        ConfigurationSection masterySection = dataConfig.getConfigurationSection("mastery");
        if (masterySection != null) {
            for (String uuidStr : masterySection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    ConfigurationSection playerSection = masterySection.getConfigurationSection(uuidStr);
                    if (playerSection != null) {
                        Map<String, Integer> playerMastery = new ConcurrentHashMap<>();
                        for (String genType : playerSection.getKeys(false)) {
                            playerMastery.put(genType, playerSection.getInt(genType));
                        }
                        masteryData.put(uuid, playerMastery);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Load specializations
        ConfigurationSection specSection = dataConfig.getConfigurationSection("specializations");
        if (specSection != null) {
            for (String uuidStr : specSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    ConfigurationSection playerSection = specSection.getConfigurationSection(uuidStr);
                    if (playerSection != null) {
                        Map<String, String> playerSpecs = new ConcurrentHashMap<>();
                        for (String key : playerSection.getKeys(false)) {
                            playerSpecs.put(key, playerSection.getString(key));
                        }
                        specializations.put(uuid, playerSpecs);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Load unlocked secrets
        ConfigurationSection secretSection = dataConfig.getConfigurationSection("unlocked-secrets");
        if (secretSection != null) {
            for (String uuidStr : secretSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    ConfigurationSection playerSection = secretSection.getConfigurationSection(uuidStr);
                    if (playerSection != null) {
                        Map<String, Boolean> playerSecrets = new ConcurrentHashMap<>();
                        for (String key : playerSection.getKeys(false)) {
                            playerSecrets.put(key, playerSection.getBoolean(key));
                        }
                        unlockedSecrets.put(uuid, playerSecrets);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }

        plugin.getLogger().info("Loaded future generator data: " + masteryData.size() + " players with mastery.");
    }

    public void saveAll() {
        save();
    }

    private void removeItems(Player player, String materialName, int amount) {
        Material mat = Material.matchMaterial(materialName);
        if (mat == null) return;

        int remaining = amount;
        for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == mat) {
                int take = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - take);
                remaining -= take;
            }
        }
    }

    public Map<String, EvolutionData> getEvolutionTree() { return evolutionTree; }
    public EvolutionData getEvolutionData(String generatorType) { return evolutionTree.get(generatorType); }
    public Map<String, SecretGenData> getSecretGenerators() { return secretGenerators; }
    public SecretGenData getSecretGenerator(String id) { return secretGenerators.get(id); }
}
