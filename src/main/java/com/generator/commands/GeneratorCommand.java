package com.generator.commands;

import com.generator.GeneratorPlugin;
import com.generator.generator.Generator;
import com.generator.listeners.GeneratorListener;
import com.generator.util.IslandRoleManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GeneratorCommand implements CommandExecutor, TabCompleter {

    private final GeneratorPlugin plugin;

    public GeneratorCommand(GeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (args.length == 0) {
            plugin.getGUIManager().openMainMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "shop" -> {
                plugin.getGUIManager().openShopMenu(player);
                return true;
            }
            case "list" -> {
                listGenerators(player);
                return true;
            }
            case "collect" -> {
                if (args.length > 1 && args[1].equalsIgnoreCase("all")) {
                    if (!IslandRoleManager.hasMinRole(player, player.getLocation(), IslandRoleManager.Role.MEMBER)) {
                        IslandRoleManager.Role role = IslandRoleManager.getPlayerRole(player, player.getLocation());
                        player.sendMessage(ChatColor.RED + "You need at least MEMBER role to collect! (Your role: " + role.name + ")");
                        return true;
                    }
                    plugin.getGeneratorManager().collectAllGenerators(player);
                } else {
                    plugin.getGUIManager().openMyGenerators(player);
                }
                return true;
            }
            case "help" -> {
                sendHelp(player);
                return true;
            }
            default -> {
                plugin.getGUIManager().openMainMenu(player);
                return true;
            }
        }
    }

    private void listGenerators(Player player) {
        List<Generator> generators = plugin.getGeneratorManager().getPlayerGenerators(player.getUniqueId());
        IslandRoleManager.Role role = IslandRoleManager.getPlayerRole(player, player.getLocation());

        player.sendMessage(ChatColor.GREEN + "--- Your Generators (" + generators.size() + ") ---");
        player.sendMessage(ChatColor.AQUA + "Island Role: " + ChatColor.WHITE + role.name.toUpperCase());

        if (generators.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "You have no generators.");
            return;
        }

        for (Generator gen : generators) {
            player.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + gen.getType() +
                    ChatColor.GRAY + " Lv." + gen.getLevel() +
                    ChatColor.GRAY + " | Stored: " + ChatColor.WHITE + gen.getStoredAmount() +
                    ChatColor.GRAY + " | " + ChatColor.WHITE + gen.getLocationString());
        }
    }

    private void sendHelp(Player player) {
        IslandRoleManager.Role role = IslandRoleManager.getPlayerRole(player, player.getLocation());
        player.sendMessage(ChatColor.GREEN + "--- Generator Commands ---");
        player.sendMessage(ChatColor.AQUA + "Your Island Role: " + ChatColor.WHITE + role.name.toUpperCase());
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "/generator " + ChatColor.GRAY + "- Open generator menu");
        player.sendMessage(ChatColor.YELLOW + "/generator shop " + ChatColor.GRAY + "- Browse generator shop");
        player.sendMessage(ChatColor.YELLOW + "/generator list " + ChatColor.GRAY + "- List your generators");
        player.sendMessage(ChatColor.YELLOW + "/generator collect all " + ChatColor.GRAY + "- Collect from all generators");
        player.sendMessage(ChatColor.YELLOW + "/generator help " + ChatColor.GRAY + "- Show this help");
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "Role Permissions:");
        player.sendMessage(ChatColor.GREEN + "  Owner " + ChatColor.GRAY + "- Place, Collect, Upgrade, Remove");
        player.sendMessage(ChatColor.AQUA + "  Coop " + ChatColor.GRAY + "- Place, Collect, Upgrade");
        player.sendMessage(ChatColor.YELLOW + "  Member " + ChatColor.GRAY + "- Collect only");
        player.sendMessage(ChatColor.RED + "  Visitor " + ChatColor.GRAY + "- No generator access");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("shop");
            completions.add("list");
            completions.add("collect");
            completions.add("help");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("collect")) {
            completions.add("all");
        }
        return completions;
    }
}
