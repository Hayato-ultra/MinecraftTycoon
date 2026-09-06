package com.generator.economy;

import com.generator.GeneratorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;

public class EconomyHook {

    private final GeneratorPlugin plugin;
    private Object economy;

    public EconomyHook(GeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found! Economy features disabled.");
            return false;
        }

        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(economyClass);
            if (rsp == null) {
                plugin.getLogger().warning("No economy provider found!");
                return false;
            }
            economy = rsp.getProvider();
            Method getName = economyClass.getMethod("getName");
            plugin.getLogger().info("Economy hooked: " + getName.invoke(economy));
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to hook Vault economy: " + e.getMessage());
            return false;
        }
    }

    public boolean hasEconomy() {
        return economy != null;
    }

    public double getBalance(Player player) {
        if (!hasEconomy()) return 0;
        try {
            // Try getBalance(OfflinePlayer) first — most common Vault signature
            Method m = economy.getClass().getMethod("getBalance", OfflinePlayer.class);
            Object result = m.invoke(economy, (OfflinePlayer) player);
            return result instanceof Number ? ((Number) result).doubleValue() : 0;
        } catch (NoSuchMethodException e) {
            try {
                // Fallback: try getBalance(String)
                Method m = economy.getClass().getMethod("getBalance", String.class);
                Object result = m.invoke(economy, player.getName());
                return result instanceof Number ? ((Number) result).doubleValue() : 0;
            } catch (Exception e2) {
                plugin.getLogger().warning("Failed to get balance: " + e2.getMessage());
                return 0;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get balance: " + e.getMessage());
            return 0;
        }
    }

    public boolean hasEnough(Player player, double amount) {
        return getBalance(player) >= amount;
    }

    public boolean withdraw(Player player, double amount) {
        if (!hasEconomy()) return false;
        if (!hasEnough(player, amount)) return false;
        try {
            // Try withdrawPlayer(OfflinePlayer, double)
            Method m = economy.getClass().getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
            m.invoke(economy, (OfflinePlayer) player, amount);
            return true;
        } catch (NoSuchMethodException e) {
            try {
                // Fallback: try withdrawPlayer(String, double)
                Method m = economy.getClass().getMethod("withdrawPlayer", String.class, double.class);
                m.invoke(economy, player.getName(), amount);
                return true;
            } catch (Exception e2) {
                plugin.getLogger().warning("Failed to withdraw: " + e2.getMessage());
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to withdraw: " + e.getMessage());
            return false;
        }
    }

    public void deposit(Player player, double amount) {
        if (!hasEconomy()) return;
        try {
            // Try depositPlayer(OfflinePlayer, double)
            Method m = economy.getClass().getMethod("depositPlayer", OfflinePlayer.class, double.class);
            m.invoke(economy, (OfflinePlayer) player, amount);
        } catch (NoSuchMethodException e) {
            try {
                // Fallback: try depositPlayer(String, double)
                Method m = economy.getClass().getMethod("depositPlayer", String.class, double.class);
                m.invoke(economy, player.getName(), amount);
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    public String format(double amount) {
        if (hasEconomy()) {
            try {
                Method m = economy.getClass().getMethod("format", double.class);
                Object result = m.invoke(economy, amount);
                if (result instanceof String) return (String) result;
            } catch (Exception ignored) {}
        }
        return "$" + String.format("%,.2f", amount);
    }
}
