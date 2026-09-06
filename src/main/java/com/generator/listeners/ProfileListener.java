package com.generator.listeners;

import com.generator.GeneratorPlugin;
import com.generator.profile.PlayerProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ProfileListener implements Listener {

    private final GeneratorPlugin plugin;

    public ProfileListener(GeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());

        if (profile == null) {
            profile = plugin.getProfileManager().createProfile(player.getUniqueId(), player.getName());
        }

        profile.setLastJoin(System.currentTimeMillis());

        plugin.getTeleportItem().giveTeleportItem(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getProfileManager().unloadProfile(player.getUniqueId());
        plugin.getLevelManager().unloadPlayer(player.getUniqueId());
    }
}
