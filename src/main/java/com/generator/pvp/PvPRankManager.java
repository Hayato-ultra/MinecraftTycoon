package com.generator.pvp;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.*;

public class PvPRankManager {

    private static final List<PvPRank> RANKS = new ArrayList<>();

    static {
        RANKS.add(new PvPRank("Unranked", 0, ChatColor.GRAY, Material.STONE, "§7"));
        RANKS.add(new PvPRank("Bronze", 1, ChatColor.GOLD, Material.COPPER_INGOT, "§6"));
        RANKS.add(new PvPRank("Silver", 2, ChatColor.WHITE, Material.IRON_INGOT, "§f"));
        RANKS.add(new PvPRank("Gold", 3, ChatColor.YELLOW, Material.GOLD_INGOT, "§e"));
        RANKS.add(new PvPRank("Platinum", 4, ChatColor.AQUA, Material.DIAMOND, "§b"));
        RANKS.add(new PvPRank("Diamond", 5, ChatColor.LIGHT_PURPLE, Material.DIAMOND_BLOCK, "§d"));
        RANKS.add(new PvPRank("Master", 6, ChatColor.RED, Material.NETHERITE_INGOT, "§c"));
        RANKS.add(new PvPRank("Grandmaster", 7, ChatColor.DARK_RED, Material.NETHERITE_BLOCK, "§4"));
        RANKS.add(new PvPRank("Legend", 8, ChatColor.GOLD, Material.GOLD_BLOCK, "§6"));
        RANKS.add(new PvPRank("Mythic", 9, ChatColor.LIGHT_PURPLE, Material.EMERALD_BLOCK, "§5"));
        RANKS.add(new PvPRank("Champion", 10, ChatColor.GREEN, Material.NETHER_STAR, "§a"));
    }

    public PvPRank getRank(int elo) {
        PvPRank rank = RANKS.get(0);
        for (PvPRank r : RANKS) {
            if (elo >= r.minElo) {
                rank = r;
            }
        }
        return rank;
    }

    public PvPRank getRankByName(String name) {
        for (PvPRank rank : RANKS) {
            if (rank.name.equalsIgnoreCase(name)) {
                return rank;
            }
        }
        return RANKS.get(0);
    }

    public List<PvPRank> getAllRanks() {
        return Collections.unmodifiableList(RANKS);
    }

    public int getProgress(int elo) {
        PvPRank current = getRank(elo);
        int currentIdx = RANKS.indexOf(current);
        if (currentIdx >= RANKS.size() - 1) return 100;

        int currentMin = current.minElo;
        int nextMin = RANKS.get(currentIdx + 1).minElo;
        int range = nextMin - currentMin;
        int progress = elo - currentMin;
        return Math.min(100, (int) ((double) progress / range * 100));
    }

    public String getProgressBar(int elo) {
        int progress = getProgress(elo);
        int filled = progress / 5;
        int empty = 20 - filled;
        StringBuilder bar = new StringBuilder("§7[");
        PvPRank rank = getRank(elo);
        for (int i = 0; i < filled; i++) bar.append(rank.color).append("█");
        for (int i = 0; i < empty; i++) bar.append("§8█");
        bar.append("§7]");
        return bar.toString();
    }

    public static class PvPRank {
        public final String name;
        public final int tier;
        public final ChatColor chatColor;
        public final Material icon;
        public final String color;
        public final int minElo;

        public PvPRank(String name, int tier, ChatColor chatColor, Material icon, String color) {
            this.name = name;
            this.tier = tier;
            this.chatColor = chatColor;
            this.icon = icon;
            this.color = color;
            this.minElo = tier == 0 ? 0 : 100 + (tier - 1) * 100;
        }
    }
}
