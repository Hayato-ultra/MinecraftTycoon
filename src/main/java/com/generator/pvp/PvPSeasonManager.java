package com.generator.pvp;

import com.generator.GeneratorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PvPSeasonManager {

    private final GeneratorPlugin plugin;
    private final PvPStatsManager statsManager;
    private final File seasonFile;
    private PvPSeason currentSeason;
    private final Map<Integer, PvPSeason> seasons = new ConcurrentHashMap<>();
    private int nextSeasonId = 1;

    private static final long SEASON_DURATION = 30L * 24 * 60 * 60 * 1000;

    public PvPSeasonManager(GeneratorPlugin plugin, PvPStatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.seasonFile = new File(plugin.getDataFolder(), "pvp_seasons.yml");
        loadSeasons();
        startSeasonCheck();
    }

    private void loadSeasons() {
        seasons.clear();
        if (!seasonFile.exists()) {
            startNewSeason();
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(seasonFile);
        nextSeasonId = config.getInt("next-season-id", 1);

        ConfigurationSection section = config.getConfigurationSection("seasons");
        if (section == null) {
            startNewSeason();
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                int id = Integer.parseInt(key);
                ConfigurationSection cs = section.getConfigurationSection(key);
                if (cs == null) continue;

                PvPSeason season = new PvPSeason();
                season.id = id;
                season.name = cs.getString("name", "Season " + id);
                season.startTime = cs.getLong("start-time", 0);
                season.endTime = cs.getLong("end-time", 0);
                season.isActive = cs.getBoolean("is-active", false);
                season.rewardCoins = cs.getInt("reward-coins", 1000);
                season.rewardKits = cs.getStringList("reward-kits");
                season.topPlayers = cs.getStringList("top-players");

                seasons.put(id, season);
                if (season.isActive) {
                    currentSeason = season;
                }
            } catch (NumberFormatException ignored) {}
        }

        if (currentSeason == null) {
            startNewSeason();
        }
    }

    public void saveSeasons() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("next-season-id", nextSeasonId);

        for (Map.Entry<Integer, PvPSeason> entry : seasons.entrySet()) {
            PvPSeason season = entry.getValue();
            String path = "seasons." + season.id;
            config.set(path + ".name", season.name);
            config.set(path + ".start-time", season.startTime);
            config.set(path + ".end-time", season.endTime);
            config.set(path + ".is-active", season.isActive);
            config.set(path + ".reward-coins", season.rewardCoins);
            config.set(path + ".reward-kits", season.rewardKits);
            config.set(path + ".top-players", season.topPlayers);
        }

        try {
            config.save(seasonFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save pvp_seasons.yml: " + e.getMessage());
        }
    }

    private void startNewSeason() {
        if (currentSeason != null) {
            endSeason(currentSeason);
        }

        PvPSeason season = new PvPSeason();
        season.id = nextSeasonId++;
        season.name = "Season " + season.id;
        season.startTime = System.currentTimeMillis();
        season.endTime = System.currentTimeMillis() + SEASON_DURATION;
        season.isActive = true;
        season.rewardCoins = 1000 + (season.id * 500);

        currentSeason = season;
        seasons.put(season.id, season);

        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "PvP SEASON " + season.id + " HAS BEGUN!");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "Duration: 30 days");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "Rewards for top players!");
        Bukkit.broadcastMessage(ChatColor.AQUA + "First place: " + season.rewardCoins + " coins");
        Bukkit.broadcastMessage(ChatColor.AQUA + "Second place: " + (season.rewardCoins / 2) + " coins");
        Bukkit.broadcastMessage(ChatColor.AQUA + "Third place: " + (season.rewardCoins / 4) + " coins");

        saveSeasons();
    }

    private void endSeason(PvPSeason season) {
        season.isActive = false;
        List<PvPStatsManager.PvPStats> topPlayers = statsManager.getSeasonTopPlayers(10);
        List<String> topNames = new ArrayList<>();

        for (int i = 0; i < topPlayers.size(); i++) {
            PvPStatsManager.PvPStats stats = topPlayers.get(i);
            Player player = Bukkit.getPlayer(stats.uuid);
            String name = player != null ? player.getName() : Bukkit.getOfflinePlayer(stats.uuid).getName();
            topNames.add(name);

            if (player != null) {
                int reward = season.rewardCoins / (i + 1);
                plugin.getCoinManager().addCoins(player, reward);
                player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "SEASON REWARD!");
                player.sendMessage(ChatColor.YELLOW + "You placed #" + (i + 1) + " in PvP Season " + season.id + "!");
                player.sendMessage(ChatColor.GREEN + "Reward: " + reward + " coins!");
            }
        }

        season.topPlayers = topNames;

        Bukkit.broadcastMessage(ChatColor.GOLD + "PvP Season " + season.id + " has ended!");
        if (!topNames.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Top players:");
            for (int i = 0; i < Math.min(3, topNames.size()); i++) {
                Bukkit.broadcastMessage(ChatColor.AQUA + "#" + (i + 1) + ": " + topNames.get(i));
            }
        }

        statsManager.resetSeason();
        saveSeasons();
    }

    private void startSeasonCheck() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (currentSeason != null && System.currentTimeMillis() >= currentSeason.endTime) {
                startNewSeason();
            }
        }, 20L * 60, 20L * 60);
    }

    public PvPSeason getCurrentSeason() {
        return currentSeason;
    }

    public PvPSeason getSeason(int id) {
        return seasons.get(id);
    }

    public Collection<PvPSeason> getAllSeasons() {
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
        long minutes = (remaining % (60 * 60 * 1000)) / (60 * 1000);

        if (days > 0) return days + "d " + hours + "h " + minutes + "m";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    public static class PvPSeason {
        public int id;
        public String name;
        public long startTime;
        public long endTime;
        public boolean isActive;
        public int rewardCoins;
        public List<String> rewardKits = new ArrayList<>();
        public List<String> topPlayers = new ArrayList<>();
    }
}
