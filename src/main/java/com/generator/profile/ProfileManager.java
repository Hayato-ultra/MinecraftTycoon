package com.generator.profile;

import com.generator.GeneratorPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProfileManager {

    private final GeneratorPlugin plugin;
    private final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();
    private final File profilesDir;

    public ProfileManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
        this.profilesDir = new File(plugin.getDataFolder(), "profiles");
        if (!profilesDir.exists()) {
            profilesDir.mkdirs();
        }
    }

    public PlayerProfile getProfile(UUID uuid) {
        return profiles.computeIfAbsent(uuid, this::loadProfile);
    }

    public PlayerProfile createProfile(UUID uuid, String name) {
        PlayerProfile profile = new PlayerProfile(uuid, name);
        profiles.put(uuid, profile);
        saveProfile(profile);
        return profile;
    }

    public PlayerProfile getProfileIfLoaded(UUID uuid) {
        return profiles.get(uuid);
    }

    public void saveProfile(PlayerProfile profile) {
        if (profile == null) return;
        File file = new File(profilesDir, profile.getUuid() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("uuid", profile.getUuid().toString());
        config.set("display-name", profile.getDisplayName());
        config.set("first-join", profile.getFirstJoin());
        config.set("last-join", profile.getLastJoin());
        config.set("total-playtime-minutes", profile.getTotalPlaytimeMinutes());
        config.set("level", profile.getLevel());
        config.set("total-coins-earned", profile.getTotalCoinsEarned());
        config.set("generators-placed", profile.getGeneratorsPlaced());
        config.set("generators-collected", profile.getGeneratorsCollected());
        config.set("generators-upgraded", profile.getGeneratorsUpgraded());
        config.set("blocks-broken", profile.getBlocksBroken());
        config.set("blocks-placed", profile.getBlocksPlaced());

        if (profile.getHome() != null) {
            config.set("home.world", profile.getHome().getWorld().getName());
            config.set("home.x", profile.getHome().getX());
            config.set("home.y", profile.getHome().getY());
            config.set("home.z", profile.getHome().getZ());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save profile for " + profile.getDisplayName() + ": " + e.getMessage());
        }
    }

    private PlayerProfile loadProfile(UUID uuid) {
        File file = new File(profilesDir, uuid + ".yml");
        if (!file.exists()) {
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        PlayerProfile profile = new PlayerProfile(uuid, config.getString("display-name", "Unknown"));

        profile.setFirstJoin(config.getLong("first-join", System.currentTimeMillis()));
        profile.setLastJoin(config.getLong("last-join", System.currentTimeMillis()));
        profile.setTotalPlaytimeMinutes(config.getInt("total-playtime-minutes", 0));
        profile.setLevel(config.getInt("level", 1));
        profile.setTotalCoinsEarned(config.getInt("total-coins-earned", 0));
        profile.setGeneratorsPlaced(config.getInt("generators-placed", 0));
        profile.setGeneratorsCollected(config.getInt("generators-collected", 0));
        profile.setGeneratorsUpgraded(config.getInt("generators-upgraded", 0));
        profile.setBlocksBroken(config.getInt("blocks-broken", 0));
        profile.setBlocksPlaced(config.getInt("blocks-placed", 0));

        if (config.contains("home.world")) {
            String worldName = config.getString("home.world");
            if (worldName != null && plugin.getServer().getWorld(worldName) != null) {
                profile.setHome(new org.bukkit.Location(
                        plugin.getServer().getWorld(worldName),
                        config.getDouble("home.x"),
                        config.getDouble("home.y"),
                        config.getDouble("home.z")
                ));
            }
        }

        return profile;
    }

    public void saveAll() {
        for (PlayerProfile profile : profiles.values()) {
            saveProfile(profile);
        }
        plugin.getLogger().info("Saved " + profiles.size() + " player profiles.");
    }

    public void unloadProfile(UUID uuid) {
        PlayerProfile profile = profiles.remove(uuid);
        if (profile != null) {
            saveProfile(profile);
        }
    }
}
