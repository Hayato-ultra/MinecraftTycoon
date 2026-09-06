package com.generator.oneblock;

import com.generator.GeneratorPlugin;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OneBlockQuestManager implements Listener {

    private final GeneratorPlugin plugin;
    private final OneBlockManager oneBlockManager;
    private final File questFile;
    private final Map<String, OneBlockQuest> quests = new LinkedHashMap<>();
    private final Map<UUID, Set<String>> completedQuests = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Integer>> questProgress = new ConcurrentHashMap<>();

    public OneBlockQuestManager(GeneratorPlugin plugin, OneBlockManager oneBlockManager) {
        this.plugin = plugin;
        this.oneBlockManager = oneBlockManager;
        this.questFile = new File(plugin.getDataFolder(), "oneblock_quests.yml");
        loadQuests();
        loadCompletedQuests();
    }

    private void loadQuests() {
        quests.clear();
        if (!questFile.exists()) {
            createDefaultQuests();
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(questFile);
        ConfigurationSection questSection = config.getConfigurationSection("quests");
        if (questSection == null) {
            createDefaultQuests();
            return;
        }

        for (String key : questSection.getKeys(false)) {
            ConfigurationSection cs = questSection.getConfigurationSection(key);
            if (cs == null) continue;

            OneBlockQuest quest = new OneBlockQuest();
            quest.id = key;
            quest.displayName = cs.getString("display-name", key);
            quest.description = cs.getString("description", "");
            quest.type = cs.getString("type", "blocks");
            quest.target = cs.getString("target", "STONE");
            quest.requiredAmount = cs.getInt("required-amount", 100);
            quest.rewardCoins = cs.getInt("reward-coins", 100);
            quest.rewardItem = cs.getString("reward-item", "");
            quest.rewardAmount = cs.getInt("reward-amount", 1);
            quest.requiredPhase = cs.getString("required-phase", "");
            quest.repeatable = cs.getBoolean("repeatable", false);

            quests.put(key, quest);
        }

        if (quests.isEmpty()) {
            createDefaultQuests();
        }
    }

    private void createDefaultQuests() {
        quests.clear();

        addQuest("mine-stone", "Stone Collector", "Mine 100 stone blocks", "blocks", "STONE", 100, 50, "", 1, "dirt", true);
        addQuest("mine-dirt", "Dirt Digger", "Mine 50 dirt blocks", "blocks", "DIRT", 50, 25, "", 1, "dirt", true);
        addQuest("mine-wood", "Wood Chopper", "Mine 30 logs", "blocks", "OAK_LOG", 30, 75, "OAK_PLANKS", 16, "wood", true);
        addQuest("mine-iron", "Iron Miner", "Mine 20 iron ore", "blocks", "IRON_ORE", 20, 100, "IRON_INGOT", 8, "iron", true);
        addQuest("mine-diamond", "Diamond Hunter", "Mine 5 diamond ore", "blocks", "DIAMOND_ORE", 5, 250, "DIAMOND", 2, "diamond", true);
        addQuest("kill-zombie", "Zombie Slayer", "Kill 10 zombies", "kill", "ZOMBIE", 10, 75, "ROTTEN_FLESH", 16, "", true);
        addQuest("kill-skeleton", "Skeleton Archer", "Kill 10 skeletons", "kill", "SKELETON", 10, 75, "BONE", 16, "", true);
        addQuest("kill-creeper", "Creeper Hunter", "Kill 5 creepers", "kill", "CREEPER", 5, 100, "GUNPOWDER", 16, "", true);
        addQuest("craft-tools", "Tool Maker", "Craft 10 tools", "craft", "TOOL", 10, 150, "IRON_INGOT", 8, "iron", true);
        addQuest("craft-armor", "Armorsmith", "Craft 5 armor pieces", "craft", "ARMOR", 5, 200, "DIAMOND", 2, "diamond", true);
        addQuest("complete-phases", "Phase Master", "Complete 5 phases", "phases", "ANY", 5, 500, "NETHERITE_INGOT", 2, "", false);
        addQuest("total-blocks", "Block Breaker", "Break 1000 total blocks", "total-blocks", "ANY", 1000, 300, "EMERALD", 4, "", false);

        saveQuests();
    }

    private void addQuest(String id, String name, String desc, String type, String target,
                          int required, int rewardCoins, String rewardItem, int rewardAmount,
                          String requiredPhase, boolean repeatable) {
        OneBlockQuest quest = new OneBlockQuest();
        quest.id = id;
        quest.displayName = name;
        quest.description = desc;
        quest.type = type;
        quest.target = target;
        quest.requiredAmount = required;
        quest.rewardCoins = rewardCoins;
        quest.rewardItem = rewardItem;
        quest.rewardAmount = rewardAmount;
        quest.requiredPhase = requiredPhase;
        quest.repeatable = repeatable;
        quests.put(id, quest);
    }

    public void saveQuests() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, OneBlockQuest> entry : quests.entrySet()) {
            OneBlockQuest quest = entry.getValue();
            String path = "quests." + quest.id;
            config.set(path + ".display-name", quest.displayName);
            config.set(path + ".description", quest.description);
            config.set(path + ".type", quest.type);
            config.set(path + ".target", quest.target);
            config.set(path + ".required-amount", quest.requiredAmount);
            config.set(path + ".reward-coins", quest.rewardCoins);
            config.set(path + ".reward-item", quest.rewardItem);
            config.set(path + ".reward-amount", quest.rewardAmount);
            config.set(path + ".required-phase", quest.requiredPhase);
            config.set(path + ".repeatable", quest.repeatable);
        }

        try {
            config.save(questFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save oneblock_quests.yml: " + e.getMessage());
        }
    }

    public void saveCompletedQuests() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, Set<String>> entry : completedQuests.entrySet()) {
            config.set("completed." + entry.getKey().toString(), new ArrayList<>(entry.getValue()));
        }

        for (Map.Entry<UUID, Map<String, Integer>> entry : questProgress.entrySet()) {
            for (Map.Entry<String, Integer> progress : entry.getValue().entrySet()) {
                config.set("progress." + entry.getKey().toString() + "." + progress.getKey(), progress.getValue());
            }
        }

        try {
            YamlConfiguration existing = YamlConfiguration.loadConfiguration(questFile);
            existing.set("completed", config.get("completed"));
            existing.set("progress", config.get("progress"));
            existing.save(questFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save oneblock quest progress: " + e.getMessage());
        }
    }

    private void loadCompletedQuests() {
        completedQuests.clear();
        questProgress.clear();
        if (!questFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(questFile);

        ConfigurationSection completedSection = config.getConfigurationSection("completed");
        if (completedSection != null) {
            for (String uuidStr : completedSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    List<String> quests = completedSection.getStringList(uuidStr);
                    completedQuests.put(uuid, new HashSet<>(quests));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        ConfigurationSection progressSection = config.getConfigurationSection("progress");
        if (progressSection != null) {
            for (String uuidStr : progressSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    ConfigurationSection cs = progressSection.getConfigurationSection(uuidStr);
                    if (cs == null) continue;

                    Map<String, Integer> progress = new HashMap<>();
                    for (String questId : cs.getKeys(false)) {
                        progress.put(questId, cs.getInt(questId, 0));
                    }
                    questProgress.put(uuid, progress);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!oneBlockManager.isOneBlockWorld(player.getWorld().getName())) return;

        updateQuestProgress(player, "blocks", event.getBlock().getType().name(), 1);
        updateQuestProgress(player, "total-blocks", "ANY", 1);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!oneBlockManager.isOneBlockWorld(killer.getWorld().getName())) return;

        String entityType = event.getEntityType().name();
        updateQuestProgress(killer, "kill", entityType, 1);
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!oneBlockManager.isOneBlockWorld(player.getWorld().getName())) return;

        String itemName = event.getRecipe().getResult().getType().name();
        if (itemName.contains("HELMET") || itemName.contains("CHESTPLATE") ||
                itemName.contains("LEGGINGS") || itemName.contains("BOOTS")) {
            updateQuestProgress(player, "craft", "ARMOR", 1);
        } else if (itemName.contains("SWORD") || itemName.contains("PICKAXE") ||
                itemName.contains("AXE") || itemName.contains("SHOVEL") || itemName.contains("HOE")) {
            updateQuestProgress(player, "craft", "TOOL", 1);
        }
    }

    private void updateQuestProgress(Player player, String type, String target, int amount) {
        UUID uuid = player.getUniqueId();
        Map<String, Integer> progress = questProgress.computeIfAbsent(uuid, k -> new HashMap<>());

        for (OneBlockQuest quest : quests.values()) {
            if (!quest.type.equals(type)) continue;
            if (completedQuests.getOrDefault(uuid, Collections.emptySet()).contains(quest.id) && !quest.repeatable) continue;

            if (!quest.requiredPhase.isEmpty()) {
                OneBlockManager.OneBlockProgress obProgress = oneBlockManager.getProgress(uuid);
                if (!isPhaseUnlocked(quest.requiredPhase, obProgress.currentPhase)) continue;
            }

            if (!quest.target.equals("ANY") && !target.equals(quest.target) &&
                    !isTypeMatch(quest.target, target)) continue;

            int current = progress.getOrDefault(quest.id, 0) + amount;
            progress.put(quest.id, current);

            if (current >= quest.requiredAmount) {
                completeQuest(player, quest);
            }
        }
    }

    private boolean isTypeMatch(String target, String actual) {
        if (target.equals("TOOL")) {
            return actual.contains("SWORD") || actual.contains("PICKAXE") ||
                    actual.contains("AXE") || actual.contains("SHOVEL") || actual.contains("HOE");
        }
        if (target.equals("ARMOR")) {
            return actual.contains("HELMET") || actual.contains("CHESTPLATE") ||
                    actual.contains("LEGGINGS") || actual.contains("BOOTS");
        }
        return false;
    }

    private boolean isPhaseUnlocked(String requiredPhase, String currentPhase) {
        List<String> phaseOrder = List.of("dirt", "grass", "stone", "wood", "iron", "gold",
                "diamond", "emerald", "nether", "end", "crystal", "obsidian", "bedrock");
        int requiredIndex = phaseOrder.indexOf(requiredPhase);
        int currentIndex = phaseOrder.indexOf(currentPhase);
        return currentIndex >= requiredIndex;
    }

    private void completeQuest(Player player, OneBlockQuest quest) {
        UUID uuid = player.getUniqueId();
        completedQuests.computeIfAbsent(uuid, k -> new HashSet<>()).add(quest.id);

        plugin.getCoinManager().addCoins(player, quest.rewardCoins);
        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "QUEST COMPLETE: " + quest.displayName + "!");
        player.sendMessage(ChatColor.YELLOW + "+" + quest.rewardCoins + " coins!");

        if (!quest.rewardItem.isEmpty()) {
            Material rewardMat = Material.matchMaterial(quest.rewardItem);
            if (rewardMat != null) {
                ItemStack reward = new ItemStack(rewardMat, quest.rewardAmount);
                player.getInventory().addItem(reward);
                player.sendMessage(ChatColor.AQUA + "Reward: " + quest.rewardAmount + " " + rewardMat.name());
            }
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    public boolean isQuestCompleted(UUID uuid, String questId) {
        return completedQuests.getOrDefault(uuid, Collections.emptySet()).contains(questId);
    }

    public int getQuestProgress(UUID uuid, String questId) {
        return questProgress.getOrDefault(uuid, Collections.emptyMap()).getOrDefault(questId, 0);
    }

    public Set<String> getCompletedQuests(UUID uuid) {
        return completedQuests.getOrDefault(uuid, Collections.emptySet());
    }

    public Collection<OneBlockQuest> getAllQuests() {
        return quests.values();
    }

    public OneBlockQuest getQuest(String id) {
        return quests.get(id);
    }

    public static class OneBlockQuest {
        public String id;
        public String displayName;
        public String description;
        public String type;
        public String target;
        public int requiredAmount;
        public int rewardCoins;
        public String rewardItem;
        public int rewardAmount;
        public String requiredPhase;
        public boolean repeatable;
    }
}
