package com.generator.pvp;

import com.generator.GeneratorPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PvPStatsManager {

    private final GeneratorPlugin plugin;
    private final File statsFile;
    private final Map<UUID, PvPStats> stats = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> killStreaks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> deathStreaks = new ConcurrentHashMap<>();

    public PvPStatsManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "pvp_stats.yml");
        loadStats();
    }

    private void loadStats() {
        stats.clear();
        if (!statsFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(statsFile);
        ConfigurationSection section = config.getConfigurationSection("stats");
        if (section == null) return;

        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection cs = section.getConfigurationSection(uuidStr);
                if (cs == null) continue;

                PvPStats stat = new PvPStats();
                stat.uuid = uuid;
                stat.kills = cs.getInt("kills", 0);
                stat.deaths = cs.getInt("deaths", 0);
                stat.assists = cs.getInt("assists", 0);
                stat.elo = cs.getInt("elo", 1000);
                stat.highestElo = cs.getInt("highest-elo", 1000);
                stat.wins = cs.getInt("wins", 0);
                stat.losses = cs.getInt("losses", 0);
                stat.killStreak = cs.getInt("kill-streak", 0);
                stat.deathStreak = cs.getInt("death-streak", 0);
                stat.highestKillStreak = cs.getInt("highest-kill-streak", 0);
                stat.totalDamageDealt = cs.getDouble("total-damage-dealt", 0);
                stat.totalDamageTaken = cs.getDouble("total-damage-taken", 0);
                stat.headshots = cs.getInt("headshots", 0);
                stat.firstBloods = cs.getInt("first-bloods", 0);
                stat.gamesPlayed = cs.getInt("games-played", 0);
                stat.lastPlayed = cs.getLong("last-played", 0);

                stats.put(uuid, stat);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveStats() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, PvPStats> entry : stats.entrySet()) {
            PvPStats stat = entry.getValue();
            String path = "stats." + stat.uuid.toString();
            config.set(path + ".kills", stat.kills);
            config.set(path + ".deaths", stat.deaths);
            config.set(path + ".assists", stat.assists);
            config.set(path + ".elo", stat.elo);
            config.set(path + ".highest-elo", stat.highestElo);
            config.set(path + ".wins", stat.wins);
            config.set(path + ".losses", stat.losses);
            config.set(path + ".kill-streak", stat.killStreak);
            config.set(path + ".death-streak", stat.deathStreak);
            config.set(path + ".highest-kill-streak", stat.highestKillStreak);
            config.set(path + ".total-damage-dealt", stat.totalDamageDealt);
            config.set(path + ".total-damage-taken", stat.totalDamageTaken);
            config.set(path + ".headshots", stat.headshots);
            config.set(path + ".first-bloods", stat.firstBloods);
            config.set(path + ".games-played", stat.gamesPlayed);
            config.set(path + ".last-played", stat.lastPlayed);
        }

        try {
            config.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save pvp_stats.yml: " + e.getMessage());
        }
    }

    public PvPStats getStats(UUID uuid) {
        return stats.computeIfAbsent(uuid, k -> {
            PvPStats s = new PvPStats();
            s.uuid = k;
            s.elo = 1000;
            s.highestElo = 1000;
            return s;
        });
    }

    public void recordKill(UUID killer, UUID victim) {
        PvPStats killerStats = getStats(killer);
        PvPStats victimStats = getStats(victim);

        killerStats.kills++;
        killerStats.lastPlayed = System.currentTimeMillis();
        victimStats.deaths++;
        victimStats.lastPlayed = System.currentTimeMillis();

        int streak = killStreaks.getOrDefault(killer, 0) + 1;
        killStreaks.put(killer, streak);
        killerStats.killStreak = streak;
        if (streak > killerStats.highestKillStreak) {
            killerStats.highestKillStreak = streak;
        }

        deathStreaks.put(killer, 0);
        killerStats.deathStreak = 0;

        int deathStreak = deathStreaks.getOrDefault(victim, 0) + 1;
        deathStreaks.put(victim, deathStreak);
        victimStats.deathStreak = deathStreak;

        killStreaks.put(victim, 0);
        victimStats.killStreak = 0;

        updateElo(killer, victim);
    }

    public void recordDeath(UUID victim) {
        PvPStats victimStats = getStats(victim);
        victimStats.deaths++;
        victimStats.lastPlayed = System.currentTimeMillis();

        int deathStreak = deathStreaks.getOrDefault(victim, 0) + 1;
        deathStreaks.put(victim, deathStreak);
        victimStats.deathStreak = deathStreak;

        killStreaks.put(victim, 0);
        victimStats.killStreak = 0;
    }

    public void recordAssist(UUID assistant) {
        PvPStats stats = getStats(assistant);
        stats.assists++;
        stats.lastPlayed = System.currentTimeMillis();
    }

    public void recordWin(UUID winner, UUID loser) {
        PvPStats winnerStats = getStats(winner);
        PvPStats loserStats = getStats(loser);
        winnerStats.wins++;
        winnerStats.gamesPlayed++;
        winnerStats.lastPlayed = System.currentTimeMillis();
        loserStats.losses++;
        loserStats.gamesPlayed++;
        loserStats.lastPlayed = System.currentTimeMillis();
    }

    public void recordHeadshot(UUID shooter) {
        getStats(shooter).headshots++;
    }

    public void recordFirstBlood(UUID killer) {
        getStats(killer).firstBloods++;
    }

    public void recordDamage(UUID dealer, UUID receiver, double damage) {
        getStats(dealer).totalDamageDealt += damage;
        getStats(receiver).totalDamageTaken += damage;
    }

    private void updateElo(UUID winner, UUID loser) {
        PvPStats winnerStats = getStats(winner);
        PvPStats loserStats = getStats(loser);

        int k = 32;
        double expectedWinner = 1.0 / (1.0 + Math.pow(10, (loserStats.elo - winnerStats.elo) / 400.0));
        double expectedLoser = 1.0 / (1.0 + Math.pow(10, (winnerStats.elo - loserStats.elo) / 400.0));

        winnerStats.elo = (int) Math.max(0, winnerStats.elo + k * (1 - expectedWinner));
        loserStats.elo = (int) Math.max(0, loserStats.elo + k * (0 - expectedLoser));

        if (winnerStats.elo > winnerStats.highestElo) {
            winnerStats.highestElo = winnerStats.elo;
        }
    }

    public void resetSeason() {
        for (PvPStats stat : stats.values()) {
            stat.seasonWins = stat.wins;
            stat.seasonLosses = stat.losses;
            stat.seasonKills = stat.kills;
            stat.seasonDeaths = stat.deaths;
            stat.seasonElo = stat.elo;
            stat.wins = 0;
            stat.losses = 0;
            stat.kills = 0;
            stat.deaths = 0;
            stat.elo = 1000;
            stat.killStreak = 0;
            stat.deathStreak = 0;
        }
        killStreaks.clear();
        deathStreaks.clear();
        saveStats();
    }

    public List<PvPStats> getTopPlayers(int limit) {
        return stats.values().stream()
                .sorted(Comparator.comparingInt((PvPStats s) -> s.elo).reversed())
                .limit(limit)
                .toList();
    }

    public List<PvPStats> getTopKillers(int limit) {
        return stats.values().stream()
                .sorted(Comparator.comparingInt((PvPStats s) -> s.kills).reversed())
                .limit(limit)
                .toList();
    }

    public List<PvPStats> getTopWinners(int limit) {
        return stats.values().stream()
                .sorted(Comparator.comparingInt((PvPStats s) -> s.wins).reversed())
                .limit(limit)
                .toList();
    }

    public List<PvPStats> getSeasonTopPlayers(int limit) {
        return stats.values().stream()
                .sorted(Comparator.comparingInt((PvPStats s) -> s.seasonElo).reversed())
                .limit(limit)
                .toList();
    }

    public static class PvPStats {
        public UUID uuid;
        public int kills = 0;
        public int deaths = 0;
        public int assists = 0;
        public int elo = 1000;
        public int highestElo = 1000;
        public int wins = 0;
        public int losses = 0;
        public int killStreak = 0;
        public int deathStreak = 0;
        public int highestKillStreak = 0;
        public double totalDamageDealt = 0;
        public double totalDamageTaken = 0;
        public int headshots = 0;
        public int firstBloods = 0;
        public int gamesPlayed = 0;
        public long lastPlayed = 0;
        public int seasonWins = 0;
        public int seasonLosses = 0;
        public int seasonKills = 0;
        public int seasonDeaths = 0;
        public int seasonElo = 1000;

        public double getKDR() {
            return deaths == 0 ? kills : (double) kills / deaths;
        }

        public double getWinRate() {
            return gamesPlayed == 0 ? 0 : (double) wins / gamesPlayed * 100;
        }

        public double getSeasonKDR() {
            return seasonDeaths == 0 ? seasonKills : (double) seasonKills / seasonDeaths;
        }
    }
}
