package com.generator.events;

import com.generator.GeneratorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SeasonalQuestManager {

    private final GeneratorPlugin plugin;
    private final SeasonManager seasonManager;
    private final File questFile;
    private final Map<String, SeasonalQuest> allQuests = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, QuestProgress>> playerProgress = new ConcurrentHashMap<>();

    public SeasonalQuestManager(GeneratorPlugin plugin, SeasonManager seasonManager) {
        this.plugin = plugin;
        this.seasonManager = seasonManager;
        this.questFile = new File(plugin.getDataFolder(), "seasonal_quests.yml");
        loadQuests();
        loadProgress();
        createSeasonQuests();
    }

    private void loadQuests() {
        allQuests.clear();
        if (!questFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(questFile);
        ConfigurationSection section = config.getConfigurationSection("quests");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection cs = section.getConfigurationSection(key);
            if (cs == null) continue;

            SeasonalQuest quest = new SeasonalQuest();
            quest.id = key;
            quest.name = cs.getString("name", key);
            quest.description = cs.getString("description", "");
            quest.type = cs.getString("type", "blocks");
            quest.target = cs.getString("target", "STONE");
            quest.requiredAmount = cs.getInt("required-amount", 100);
            quest.rewardCoins = cs.getInt("reward-coins", 50);
            quest.rewardXP = cs.getInt("reward-xp", 100);
            quest.seasonTheme = cs.getString("season-theme", "all");
            quest.icon = cs.getString("icon", "STONE");

            allQuests.put(key, quest);
        }
    }

    public void saveQuests() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, SeasonalQuest> entry : allQuests.entrySet()) {
            SeasonalQuest quest = entry.getValue();
            String path = "quests." + quest.id;
            config.set(path + ".name", quest.name);
            config.set(path + ".description", quest.description);
            config.set(path + ".type", quest.type);
            config.set(path + ".target", quest.target);
            config.set(path + ".required-amount", quest.requiredAmount);
            config.set(path + ".reward-coins", quest.rewardCoins);
            config.set(path + ".reward-xp", quest.rewardXP);
            config.set(path + ".season-theme", quest.seasonTheme);
            config.set(path + ".icon", quest.icon);
        }

        try {
            config.save(questFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save seasonal_quests.yml: " + e.getMessage());
        }
    }

    private void loadProgress() {
        playerProgress.clear();
        File progressFile = new File(plugin.getDataFolder(), "quest_progress.yml");
        if (!progressFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(progressFile);
        ConfigurationSection section = config.getConfigurationSection("progress");
        if (section == null) return;

        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection questSection = section.getConfigurationSection(uuidStr);
                if (questSection == null) continue;

                for (String questId : questSection.getKeys(false)) {
                    ConfigurationSection cs = questSection.getConfigurationSection(questId);
                    if (cs == null) continue;

                    QuestProgress progress = new QuestProgress();
                    progress.questId = questId;
                    progress.currentAmount.set(cs.getInt("current-amount", 0));
                    progress.completed = cs.getBoolean("completed", false);
                    progress.claimed = cs.getBoolean("claimed", false);

                    playerProgress.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(questId, progress);
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveProgress() {
        File progressFile = new File(plugin.getDataFolder(), "quest_progress.yml");
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, Map<String, QuestProgress>> entry : playerProgress.entrySet()) {
            String uuidPath = "progress." + entry.getKey().toString();
            for (Map.Entry<String, QuestProgress> questEntry : entry.getValue().entrySet()) {
                QuestProgress progress = questEntry.getValue();
                config.set(uuidPath + "." + progress.questId + ".current-amount", progress.currentAmount.get());
                config.set(uuidPath + "." + progress.questId + ".completed", progress.completed);
                config.set(uuidPath + "." + progress.questId + ".claimed", progress.claimed);
            }
        }

        try {
            config.save(progressFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save quest_progress.yml: " + e.getMessage());
        }
    }

    private void createSeasonQuests() {
        if (seasonManager.getCurrentSeason() == null) return;

        String theme = seasonManager.getCurrentSeason().theme;
        if (!allQuests.containsKey(theme + "_mining")) {
            addQuest(theme + "_mining", "Season Mining", "Mine blocks this season", "blocks", theme.equals("winter") ? "SNOW_BLOCK" : "STONE", 200, 100, 200, theme);
            addQuest(theme + "_killing", "Season Hunting", "Kill mobs this season", "mobs", "ZOMBIE", 50, 150, 300, theme);
            addQuest(theme + "_farming", "Season Farming", "Harvest crops this season", "blocks", "WHEAT", 100, 75, 150, theme);
            addQuest(theme + "_exploring", "Season Explorer", "Break unique blocks", "blocks", "DIAMOND_ORE", 10, 200, 500, theme);
            saveQuests();
        }
    }

    private void addQuest(String id, String name, String desc, String type, String target, int required, int coins, int xp, String theme) {
        SeasonalQuest quest = new SeasonalQuest();
        quest.id = id;
        quest.name = name;
        quest.description = desc;
        quest.type = type;
        quest.target = target;
        quest.requiredAmount = required;
        quest.rewardCoins = coins;
        quest.rewardXP = xp;
        quest.seasonTheme = theme;
        quest.icon = target;
        allQuests.put(id, quest);
    }

    public void onBlockBreak(Player player, Material block) {
        for (SeasonalQuest quest : allQuests.values()) {
            if (quest.type.equals("blocks") && quest.target.equals(block.name())) {
                updateProgress(player, quest.id, 1);
            }
        }
    }

    public void onMobKill(Player player, String mobType) {
        for (SeasonalQuest quest : allQuests.values()) {
            if (quest.type.equals("mobs") && quest.target.equalsIgnoreCase(mobType)) {
                updateProgress(player, quest.id, 1);
            }
        }
    }

    public void onItemCraft(Player player, Material item) {
        for (SeasonalQuest quest : allQuests.values()) {
            if (quest.type.equals("craft") && quest.target.equals(item.name())) {
                updateProgress(player, quest.id, 1);
            }
        }
    }

    private void updateProgress(Player player, String questId, int amount) {
        SeasonalQuest quest = allQuests.get(questId);
        if (quest == null) return;

        QuestProgress progress = playerProgress.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(questId, k -> new QuestProgress());
        progress.questId = questId;

        if (progress.completed) return;

        progress.currentAmount.addAndGet(amount);
        seasonManager.addXP(player.getUniqueId(), amount);

        int current = progress.currentAmount.get();
        if (current >= quest.requiredAmount) {
            progress.completed = true;
            player.sendMessage(ChatColor.GOLD + "Quest completed: " + quest.name + "!");
            player.sendMessage(ChatColor.YELLOW + "Reward: " + quest.rewardCoins + " coins, " + quest.rewardXP + " XP");
        } else if (quest.requiredAmount >= 4 && current % (quest.requiredAmount / 4) == 0) {
            int percent = (int) ((double) current / quest.requiredAmount * 100);
            player.sendMessage(ChatColor.AQUA + quest.name + " progress: " + percent + "%");
        }
    }

    public boolean claimReward(Player player, String questId) {
        QuestProgress progress = playerProgress.getOrDefault(player.getUniqueId(), Collections.emptyMap()).get(questId);
        SeasonalQuest quest = allQuests.get(questId);
        if (progress == null || quest == null || !progress.completed || progress.claimed) return false;

        progress.claimed = true;
        plugin.getCoinManager().addCoins(player, quest.rewardCoins);
        seasonManager.completeQuest(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Claimed " + quest.rewardCoins + " coins for " + quest.name + "!");
        saveProgress();
        return true;
    }

    public List<SeasonalQuest> getSeasonQuests() {
        String theme = seasonManager.getCurrentSeason() != null ? seasonManager.getCurrentSeason().theme : "all";
        return allQuests.values().stream()
                .filter(q -> q.seasonTheme.equals(theme) || q.seasonTheme.equals("all"))
                .toList();
    }

    public QuestProgress getPlayerProgress(UUID uuid, String questId) {
        return playerProgress.getOrDefault(uuid, Collections.emptyMap()).get(questId);
    }

    public void openQuestGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "SEASONAL_QUESTS");
        List<SeasonalQuest> quests = getSeasonQuests();

        int slot = 0;
        for (SeasonalQuest quest : quests) {
            if (slot >= 26) break;
            QuestProgress progress = getPlayerProgress(player.getUniqueId(), quest.id);
            int current = progress != null ? progress.getCurrentAmount() : 0;
            boolean completed = progress != null && progress.completed;
            boolean claimed = progress != null && progress.claimed;

            Material mat = claimed ? Material.LIME_STAINED_GLASS_PANE :
                    completed ? Material.YELLOW_STAINED_GLASS_PANE :
                            Material.matchMaterial(quest.icon) != null ? Material.matchMaterial(quest.icon) : Material.PAPER;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + quest.name);
                meta.setLore(Arrays.asList(
                        "",
                        "§7" + quest.description,
                        "",
                        "§7Progress: §f" + current + "/" + quest.requiredAmount,
                        "§7Reward: §6" + quest.rewardCoins + " coins",
                        "§7XP: §a" + quest.rewardXP,
                        "",
                        claimed ? "§aClaimed" : completed ? "§eClick to claim" : "§cIn progress"
                ));
                item.setItemMeta(meta);
            }
            gui.setItem(slot, item);
            slot++;
            if (slot == 17) slot = 19;
        }

        gui.setItem(22, createItem(Material.BARRIER, ChatColor.RED + "Close"));
        player.openInventory(gui);
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

    public static class SeasonalQuest {
        public String id;
        public String name;
        public String description;
        public String type;
        public String target;
        public int requiredAmount;
        public int rewardCoins;
        public int rewardXP;
        public String seasonTheme;
        public String icon;
    }

    public static class QuestProgress {
        public String questId;
        public AtomicInteger currentAmount = new AtomicInteger(0);
        public volatile boolean completed = false;
        public volatile boolean claimed = false;

        public int getCurrentAmount() { return currentAmount.get(); }
    }
}
