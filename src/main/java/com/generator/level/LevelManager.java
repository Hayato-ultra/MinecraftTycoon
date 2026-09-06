package com.generator.level;

import com.generator.GeneratorPlugin;
import com.generator.profile.PlayerProfile;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LevelManager implements Listener {

    private final GeneratorPlugin plugin;
    private final Map<UUID, Integer> playerXp = new ConcurrentHashMap<>();

    public LevelManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    public int getXp(Player player) {
        return playerXp.getOrDefault(player.getUniqueId(), 0);
    }

    public int getLevel(PlayerProfile profile) {
        return profile.getLevel();
    }

    public int getXpForNextLevel(int currentLevel) {
        return plugin.getConfigManager().getLevelXp(currentLevel);
    }

    public int getXpProgress(PlayerProfile profile) {
        int currentLevelXp = plugin.getConfigManager().getLevelXp(profile.getLevel() - 1);
        return currentLevelXp;
    }

    public void addXp(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        PlayerProfile profile = plugin.getProfileManager().getProfileIfLoaded(uuid);
        if (profile == null) return;

        int currentXp = playerXp.getOrDefault(uuid, 0);
        int newXp = currentXp + amount;
        playerXp.put(uuid, newXp);

        int requiredXp = getXpForNextLevel(profile.getLevel());
        while (newXp >= requiredXp && profile.getLevel() < 10) {
            newXp -= requiredXp;
            profile.setLevel(profile.getLevel() + 1);
            playerXp.put(uuid, newXp);

            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "LEVEL UP! " +
                    ChatColor.GREEN + "You are now level " + ChatColor.GOLD + profile.getLevel() + ChatColor.GREEN + "!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            requiredXp = getXpForNextLevel(profile.getLevel());
        }

        updateXpBar(player, profile);
    }

    public void updateXpBar(Player player, PlayerProfile profile) {
        int currentXp = playerXp.getOrDefault(player.getUniqueId(), 0);
        int requiredXp = getXpForNextLevel(profile.getLevel());

        float progress = (float) currentXp / requiredXp;
        player.setExp(Math.min(progress, 1.0f));
        player.setLevel(profile.getLevel());
    }

    public void loadXp(Player player) {
        PlayerProfile profile = plugin.getProfileManager().getProfileIfLoaded(player.getUniqueId());
        if (profile != null) {
            updateXpBar(player, profile);
        }
    }

    public void unloadPlayer(UUID uuid) {
        playerXp.remove(uuid);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        int xp = plugin.getConfigManager().getXpSource("block-break");
        if (xp > 0) {
            addXp(player, xp);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        int xp = plugin.getConfigManager().getXpSource("block-place");
        if (xp > 0) {
            addXp(player, xp);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        Player player = event.getEntity().getKiller();

        if (event.getEntity().getType() == org.bukkit.entity.EntityType.PLAYER) {
            int xp = plugin.getConfigManager().getXpSource("player-kill");
            if (xp > 0) addXp(player, xp);
        } else {
            int xp = plugin.getConfigManager().getXpSource("mob-kill");
            if (xp > 0) addXp(player, xp);
        }
    }
}
