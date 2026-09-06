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

public class SeasonManager {

    private final GeneratorPlugin plugin;
    private final File seasonFile;
    private final Map<Integer, ServerSeason> seasons = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, SeasonPlayerData>> playerData = new ConcurrentHashMap<>();
    private volatile ServerSeason currentSeason;
    private int nextSeasonId = 1;

    public SeasonManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
        this.seasonFile = new File(plugin.getDataFolder(), "seasons.yml");
        loadSeasons();
        loadPlayerData();
    }

    private void loadSeasons() {
        seasons.clear();
        if (!seasonFile.exists()) {
            createDefaultSeasons();
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(seasonFile);
        nextSeasonId = config.getInt("next-season-id", 1);

        ConfigurationSection section = config.getConfigurationSection("seasons");
        if (section == null) {
            createDefaultSeasons();
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                int id = Integer.parseInt(key);
                ConfigurationSection cs = section.getConfigurationSection(key);
                if (cs == null) continue;

                ServerSeason season = new ServerSeason();
                season.id = id;
                season.name = cs.getString("name", "Season " + id);
                season.theme = cs.getString("theme", "default");
                season.startTime = cs.getLong("start-time", 0);
                season.endTime = cs.getLong("end-time", 0);
                season.isActive = cs.getBoolean("is-active", false);
                season.description = cs.getString("description", "");
                season.rewardMultiplier = cs.getDouble("reward-multiplier", 1.0);
                season.specialBlocks = cs.getStringList("special-blocks");
                season.questBonus = cs.getInt("quest-bonus", 10);

                seasons.put(id, season);
                if (season.isActive && System.currentTimeMillis() < season.endTime) {
                    currentSeason = season;
                }
            } catch (NumberFormatException ignored) {}
        }

        if (currentSeason == null) {
            activateNextSeason();
        }
    }

    private void createDefaultSeasons() {
        addSeason("Spring Bloom", "spring", 1.2, Arrays.asList("GRASS_BLOCK", "FLOWER_POT", "SAPLING"), "New beginnings! 20% bonus to all rewards!");
        addSeason("Summer Heat", "summer", 1.5, Arrays.asList("SAND", "CACTUS", "MELON"), "Hot summer! 50% bonus to all rewards!");
        addSeason("Autumn Harvest", "autumn", 1.3, Arrays.asList("PUMPKIN", "HAY_BLOCK", "BROWN_MUSHROOM"), "Harvest season! 30% bonus to all rewards!");
        addSeason("Winter Frost", "winter", 2.0, Arrays.asList("SNOW_BLOCK", "ICE", "PACKED_ICE"), "Double rewards all season!");
        saveSeasons();
        activateNextSeason();
    }

    private void addSeason(String name, String theme, double multiplier, List<String> blocks, String description) {
        ServerSeason season = new ServerSeason();
        season.id = nextSeasonId++;
        season.name = name;
        season.theme = theme;
        season.startTime = 0;
        season.endTime = 0;
        season.isActive = false;
        season.description = description;
        season.rewardMultiplier = multiplier;
        season.specialBlocks = blocks;
        season.questBonus = (int) ((multiplier - 1.0) * 100);
        seasons.put(season.id, season);
    }

    public void saveSeasons() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("next-season-id", nextSeasonId);

        for (Map.Entry<Integer, ServerSeason> entry : seasons.entrySet()) {
            ServerSeason season = entry.getValue();
            String path = "seasons." + season.id;
            config.set(path + ".name", season.name);
            config.set(path + ".theme", season.theme);
            config.set(path + ".start-time", season.startTime);
            config.set(path + ".end-time", season.endTime);
            config.set(path + ".is-active", season.isActive);
            config.set(path + ".description", season.description);
            config.set(path + ".reward-multiplier", season.rewardMultiplier);
            config.set(path + ".special-blocks", season.specialBlocks);
            config.set(path + ".quest-bonus", season.questBonus);
        }

        try {
            config.save(seasonFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save seasons.yml: " + e.getMessage());
        }
    }

    private void loadPlayerData() {
        playerData.clear();
        File dataFile = new File(plugin.getDataFolder(), "season_data.yml");
        if (!dataFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = config.getConfigurationSection("players");
        if (section == null) return;

        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection seasonSection = section.getConfigurationSection(uuidStr);
                if (seasonSection == null) continue;

                for (String seasonIdStr : seasonSection.getKeys(false)) {
                    try {
                        int seasonId = Integer.parseInt(seasonIdStr);
                        ConfigurationSection cs = seasonSection.getConfigurationSection(seasonIdStr);
                        if (cs == null) continue;

                        SeasonPlayerData data = new SeasonPlayerData();
                        data.seasonId = seasonId;
                        data.xpEarned.set(cs.getInt("xp-earned", 0));
                        data.questsCompleted.set(cs.getInt("quests-completed", 0));
                        data.blocksBroken.set(cs.getInt("blocks-broken", 0));
                        data.mobsKilled.set(cs.getInt("mobs-killed", 0));
                        data.rewardsClaimed = cs.getBoolean("rewards-claimed", false);

                        playerData.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(seasonId, data);
                    } catch (NumberFormatException ignored) {}
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void savePlayerData() {
        File dataFile = new File(plugin.getDataFolder(), "season_data.yml");
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, Map<Integer, SeasonPlayerData>> entry : playerData.entrySet()) {
            String uuidPath = "players." + entry.getKey().toString();
            for (Map.Entry<Integer, SeasonPlayerData> dataEntry : entry.getValue().entrySet()) {
                SeasonPlayerData data = dataEntry.getValue();
                String path = uuidPath + "." + data.seasonId;
                config.set(path + ".xp-earned", data.xpEarned.get());
                config.set(path + ".quests-completed", data.questsCompleted.get());
                config.set(path + ".blocks-broken", data.blocksBroken.get());
                config.set(path + ".mobs-killed", data.mobsKilled.get());
                config.set(path + ".rewards-claimed", data.rewardsClaimed);
            }
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save season_data.yml: " + e.getMessage());
        }
    }

    public void activateNextSeason() {
        if (currentSeason != null) {
            currentSeason.isActive = false;
        }

        List<ServerSeason> inactive = seasons.values().stream()
                .filter(s -> !s.isActive)
                .sorted(Comparator.comparingInt(s -> s.id))
                .toList();

        if (inactive.isEmpty()) {
            nextSeasonId = 1;
            createDefaultSeasons();
            inactive = new ArrayList<>(seasons.values());
        }

        ServerSeason next = inactive.get(0);
        next.isActive = true;
        next.startTime = System.currentTimeMillis();
        next.endTime = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000);

        currentSeason = next;
        saveSeasons();

        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "NEW SEASON: " + next.name + "!");
        Bukkit.broadcastMessage(ChatColor.YELLOW + next.description);
        Bukkit.broadcastMessage(ChatColor.AQUA + "Duration: 7 days | Reward Multiplier: x" + next.rewardMultiplier);
    }

    public void addXP(UUID uuid, int amount) {
        if (currentSeason == null) return;

        SeasonPlayerData data = playerData.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(currentSeason.id, k -> new SeasonPlayerData());
        data.seasonId = currentSeason.id;
        data.xpEarned.addAndGet(amount);
    }

    public void addBlockBroken(UUID uuid) {
        if (currentSeason == null) return;

        SeasonPlayerData data = playerData.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(currentSeason.id, k -> new SeasonPlayerData());
        data.blocksBroken.incrementAndGet();
        addXP(uuid, 1);
    }

    public void addMobKilled(UUID uuid) {
        if (currentSeason == null) return;

        SeasonPlayerData data = playerData.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(currentSeason.id, k -> new SeasonPlayerData());
        data.mobsKilled.incrementAndGet();
        addXP(uuid, 5);
    }

    public void completeQuest(UUID uuid) {
        if (currentSeason == null) return;

        SeasonPlayerData data = playerData.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(currentSeason.id, k -> new SeasonPlayerData());
        data.questsCompleted.incrementAndGet();
        addXP(uuid, currentSeason.questBonus);
    }

    public SeasonPlayerData getPlayerData(UUID uuid) {
        if (currentSeason == null) return new SeasonPlayerData();
        return playerData.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(currentSeason.id, k -> new SeasonPlayerData());
    }

    public boolean claimReward(UUID uuid) {
        SeasonPlayerData data = getPlayerData(uuid);
        if (data.rewardsClaimed) return false;

        data.rewardsClaimed = true;
        savePlayerData();
        return true;
    }

    public ServerSeason getCurrentSeason() {
        return currentSeason;
    }

    public ServerSeason getSeason(int id) {
        return seasons.get(id);
    }

    public Collection<ServerSeason> getAllSeasons() {
        return seasons.values();
    }

    public long getTimeRemaining() {
        if (currentSeason == null) return 0;
        return Math.max(0, currentSeason.endTime - System.currentTimeMillis());
    }

    public String getTimeRemainingFormatted() {
        long remaining = getTimeRemaining();
        if (remaining <= 0) return "Season ended";

        long days = remaining / (24 * 60 * 60 * 1000);
        long hours = (remaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);

        if (days > 0) return days + "d " + hours + "h";
        return hours + "h";
    }

    public List<SeasonPlayerData> getSeasonLeaderboard() {
        if (currentSeason == null) return new ArrayList<>();
        int seasonId = currentSeason.id;
        return playerData.values().stream()
                .filter(data -> data.containsKey(seasonId))
                .map(data -> data.get(seasonId))
                .sorted(Comparator.comparingInt((SeasonPlayerData d) -> d.xpEarned.get()).reversed())
                .toList();
    }

    public void openSeasonGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "SEASON_GUI");
        int slot = 0;

        for (ServerSeason season : seasons.values()) {
            if (slot >= 26) break;
            Material mat = season.isActive ? Material.NETHER_STAR : Material.GRAY_WOOL;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + season.name + (season.isActive ? " §a[ACTIVE]" : ""));
                meta.setLore(Arrays.asList(
                        "",
                        "§7" + season.description,
                        "",
                        "§7Multiplier: §ax" + season.rewardMultiplier,
                        "§7Quest Bonus: §a+" + season.questBonus + "%",
                        "§7Duration: §f7 days"
                ));
                item.setItemMeta(meta);
            }
            gui.setItem(slot, item);
            slot++;
            if (slot == 17) slot = 19;
        }

        SeasonPlayerData data = getPlayerData(player.getUniqueId());
        gui.setItem(22, createItem(Material.EXPERIENCE_BOTTLE,
                ChatColor.AQUA + "Your Season Stats",
                "",
                "§7XP Earned: §f" + data.getXpEarned(),
                "§7Quests Completed: §f" + data.getQuestsCompleted(),
                "§7Blocks Broken: §f" + data.getBlocksBroken(),
                "§7Mobs Killed: §f" + data.getMobsKilled(),
                "§7Rewards Claimed: " + (data.rewardsClaimed ? "§aYes" : "§cNo")));

        gui.setItem(26, createItem(Material.BARRIER, ChatColor.RED + "Close"));
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

    public static class ServerSeason {
        public int id;
        public String name;
        public String theme;
        public long startTime;
        public long endTime;
        public boolean isActive;
        public String description;
        public double rewardMultiplier;
        public List<String> specialBlocks = new ArrayList<>();
        public int questBonus = 10;
    }

    public static class SeasonPlayerData {
        public int seasonId;
        public AtomicInteger xpEarned = new AtomicInteger(0);
        public AtomicInteger questsCompleted = new AtomicInteger(0);
        public AtomicInteger blocksBroken = new AtomicInteger(0);
        public AtomicInteger mobsKilled = new AtomicInteger(0);
        public volatile boolean rewardsClaimed = false;

        public int getXpEarned() { return xpEarned.get(); }
        public int getQuestsCompleted() { return questsCompleted.get(); }
        public int getBlocksBroken() { return blocksBroken.get(); }
        public int getMobsKilled() { return mobsKilled.get(); }
    }
}
