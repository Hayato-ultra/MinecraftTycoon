package com.generator.profile;

import org.bukkit.Location;

import java.util.UUID;

public class PlayerProfile {

    private final UUID uuid;
    private String displayName;
    private long firstJoin;
    private long lastJoin;
    private int totalPlaytimeMinutes;
    private int level;
    private int totalCoinsEarned;
    private int generatorsPlaced;
    private int generatorsCollected;
    private int generatorsUpgraded;
    private int blocksBroken;
    private int blocksPlaced;
    private Location home;

    public PlayerProfile(UUID uuid, String displayName) {
        this.uuid = uuid;
        this.displayName = displayName;
        this.firstJoin = System.currentTimeMillis();
        this.lastJoin = System.currentTimeMillis();
        this.totalPlaytimeMinutes = 0;
        this.level = 1;
        this.totalCoinsEarned = 0;
        this.generatorsPlaced = 0;
        this.generatorsCollected = 0;
        this.generatorsUpgraded = 0;
        this.blocksBroken = 0;
        this.blocksPlaced = 0;
        this.home = null;
    }

    public UUID getUuid() { return uuid; }
    public String getDisplayName() { return displayName; }
    public long getFirstJoin() { return firstJoin; }
    public long getLastJoin() { return lastJoin; }
    public int getTotalPlaytimeMinutes() { return totalPlaytimeMinutes; }
    public int getLevel() { return level; }
    public int getTotalCoinsEarned() { return totalCoinsEarned; }
    public int getGeneratorsPlaced() { return generatorsPlaced; }
    public int getGeneratorsCollected() { return generatorsCollected; }
    public int getGeneratorsUpgraded() { return generatorsUpgraded; }
    public int getBlocksBroken() { return blocksBroken; }
    public int getBlocksPlaced() { return blocksPlaced; }
    public Location getHome() { return home; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setFirstJoin(long firstJoin) { this.firstJoin = firstJoin; }
    public void setLastJoin(long lastJoin) { this.lastJoin = lastJoin; }
    public void setTotalPlaytimeMinutes(int totalPlaytimeMinutes) { this.totalPlaytimeMinutes = totalPlaytimeMinutes; }
    public void setLevel(int level) { this.level = level; }
    public void setTotalCoinsEarned(int totalCoinsEarned) { this.totalCoinsEarned = totalCoinsEarned; }
    public void setGeneratorsPlaced(int generatorsPlaced) { this.generatorsPlaced = generatorsPlaced; }
    public void setGeneratorsCollected(int generatorsCollected) { this.generatorsCollected = generatorsCollected; }
    public void setGeneratorsUpgraded(int generatorsUpgraded) { this.generatorsUpgraded = generatorsUpgraded; }
    public void setBlocksBroken(int blocksBroken) { this.blocksBroken = blocksBroken; }
    public void setBlocksPlaced(int blocksPlaced) { this.blocksPlaced = blocksPlaced; }
    public void setHome(Location home) { this.home = home; }

    public void addCoinsEarned(int amount) { this.totalCoinsEarned += amount; }
    public void addGeneratorsPlaced() { this.generatorsPlaced++; }
    public void addGeneratorsCollected() { this.generatorsCollected++; }
    public void addGeneratorsUpgraded() { this.generatorsUpgraded++; }
    public void addBlocksBroken() { this.blocksBroken++; }
    public void addBlocksPlaced() { this.blocksPlaced++; }
}
