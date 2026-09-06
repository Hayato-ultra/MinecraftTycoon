package com.generator.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IslandRoleManager {

    private static final int ROLE_OWNER = 4;
    private static final int ROLE_COOP = 3;
    private static final int ROLE_MEMBER = 2;
    private static final int ROLE_VISITOR = 1;
    private static final int ROLE_NONE = 0;

    private static final Map<String, CacheEntry> ROLE_CACHE = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 30000;

    private static class CacheEntry {
        final int level;
        final long timestamp;

        CacheEntry(int level) {
            this.level = level;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
        }
    }

    public enum Role {
        OWNER("owner", ROLE_OWNER),
        COOP("coop", ROLE_COOP),
        MEMBER("member", ROLE_MEMBER),
        VISITOR("visitor", ROLE_VISITOR),
        NONE("none", ROLE_NONE);

        public final String name;
        public final int level;

        Role(String name, int level) {
            this.name = name;
            this.level = level;
        }
    }

    public static Role getPlayerRole(Player player, Location location) {
        if (location == null || location.getWorld() == null) return Role.NONE;

        String cacheKey = player.getUniqueId() + ":" + location.getWorld().getName() +
                ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();

        CacheEntry cached = ROLE_CACHE.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return getRoleByLevel(cached.level);
        }

        int level = queryBentoBoxRole(player, location);
        ROLE_CACHE.put(cacheKey, new CacheEntry(level));

        // Periodic cleanup every 100 entries
        if (ROLE_CACHE.size() > 100) {
            ROLE_CACHE.entrySet().removeIf(e -> e.getValue().isExpired());
        }

        return getRoleByLevel(level);
    }

    public static boolean hasMinRole(Player player, Location location, Role minRole) {
        if (player.hasPermission("generators.admin")) return true;
        Role role = getPlayerRole(player, location);
        return role.level >= minRole.level;
    }

    private static Role getRoleByLevel(int level) {
        for (Role role : Role.values()) {
            if (role.level == level) return role;
        }
        return Role.NONE;
    }

    private static int queryBentoBoxRole(Player player, Location location) {
        try {
            Class<?> bentoBoxClass = Class.forName("world.bentobox.bentobox.BentoBox");
            Object bentoBox = bentoBoxClass.getMethod("getInstance").invoke(null);
            Object islandsManager = bentoBoxClass.getMethod("getIslands").invoke(bentoBox);

            Object island = islandsManager.getClass().getMethod("getIslandAt", Location.class)
                    .invoke(islandsManager, location);
            if (island == null) return ROLE_NONE;

            UUID uuid = player.getUniqueId();

            // Check owner
            Object owner = island.getClass().getMethod("getOwner").invoke(island);
            if (owner != null && owner.equals(uuid)) return ROLE_OWNER;

            // Check coops
            Object coops = island.getClass().getMethod("getCoops").invoke(island);
            if (coops != null && coops instanceof java.util.Set<?> coopSet && coopSet.contains(uuid)) {
                return ROLE_COOP;
            }

            // Check trusteds (treated as coop in some BentoBox versions)
            try {
                Object trusteds = island.getClass().getMethod("getTrusteds").invoke(island);
                if (trusteds != null && trusteds instanceof java.util.Set<?> trustedSet && trustedSet.contains(uuid)) {
                    return ROLE_COOP;
                }
            } catch (NoSuchMethodException ignored) {}

            // Check members
            Object members = island.getClass().getMethod("getMembers").invoke(island);
            if (members != null && members instanceof java.util.Set<?> memberSet && memberSet.contains(uuid)) {
                return ROLE_MEMBER;
            }

            // Check if visitor (on the island but not a member)
            Object islandBounds = island.getClass().getMethod("getBounds").invoke(island);
            if (islandBounds != null) {
                // If they're in the island world and the island exists, they're at least a visitor
                if (location.getWorld().getName().equals(
                        island.getClass().getMethod("getWorld").invoke(island).toString())) {
                    return ROLE_VISITOR;
                }
            }

        } catch (Exception ignored) {}
        return ROLE_NONE;
    }
}
