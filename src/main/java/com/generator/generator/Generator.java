package com.generator.generator;

import com.generator.config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.UUID;

public class Generator {

    private final UUID id;
    private final UUID ownerUUID;
    private final String islandId;
    private final String type;
    private final Location location;
    private int level;
    private int storedAmount;
    private long lastGeneration;
    private volatile boolean dirty;

    public Generator(UUID id, UUID ownerUUID, String islandId, String type, Location location,
                     int level, int storedAmount, long lastGeneration) {
        this.id = id;
        this.ownerUUID = ownerUUID;
        this.islandId = islandId;
        this.type = type;
        this.location = location;
        this.level = level;
        this.storedAmount = storedAmount;
        this.lastGeneration = lastGeneration;
        this.dirty = false;
    }

    public UUID getId() { return id; }
    public UUID getOwnerUUID() { return ownerUUID; }
    public String getIslandId() { return islandId; }
    public String getType() { return type; }
    public Location getLocation() { return location; }
    public int getLevel() { return level; }
    public int getStoredAmount() { return storedAmount; }
    public long getLastGeneration() { return lastGeneration; }
    public boolean isDirty() { return dirty; }

    public void setLevel(int level) { this.level = level; this.dirty = true; }
    public void setStoredAmount(int storedAmount) { this.storedAmount = storedAmount; this.dirty = true; }
    public void setLastGeneration(long lastGeneration) { this.lastGeneration = lastGeneration; this.dirty = true; }
    public void markClean() { this.dirty = false; }
    public void markDirty() { this.dirty = true; }

    public int getMaxStorage(ConfigManager configManager) {
        return configManager.getLevelStats(level)[1];
    }

    public int getInterval(ConfigManager configManager) {
        return configManager.getLevelStats(level)[0];
    }

    public Material getOutputMaterial(ConfigManager configManager) {
        ConfigManager.GeneratorTypeData typeData = configManager.getGeneratorType(type);
        return typeData != null ? typeData.output : Material.STONE;
    }

    public int getOutputAmount(ConfigManager configManager) {
        ConfigManager.GeneratorTypeData typeData = configManager.getGeneratorType(type);
        return typeData != null ? typeData.amount : 1;
    }

    public boolean isFull(ConfigManager configManager) {
        return storedAmount >= getMaxStorage(configManager);
    }

    public boolean canAdd(int amount, ConfigManager configManager) {
        return storedAmount + amount <= getMaxStorage(configManager);
    }

    public int getFreeSpace(ConfigManager configManager) {
        return Math.max(0, getMaxStorage(configManager) - storedAmount);
    }

    public int getMaxLevel(ConfigManager configManager) {
        ConfigManager.GeneratorTypeData typeData = configManager.getGeneratorType(type);
        return typeData != null ? typeData.maxLevel : 10;
    }

    public boolean isMaxLevel(ConfigManager configManager) {
        return level >= getMaxLevel(configManager);
    }

    public String getLocationString() {
        if (location == null) return "unknown";
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    public static Generator fromLocation(UUID id, UUID ownerUUID, String islandId, String type,
                                          Location location, int level, int storedAmount, long lastGeneration) {
        return new Generator(id, ownerUUID, islandId, type, location, level, storedAmount, lastGeneration);
    }
}
