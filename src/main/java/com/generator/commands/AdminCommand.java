package com.generator.commands;

import com.generator.GeneratorPlugin;
import com.generator.generator.Generator;
import com.generator.listeners.GeneratorListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final GeneratorPlugin plugin;

    public AdminCommand(GeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("generators.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "giveall" -> handleGiveAll(sender, args);
            case "reload" -> handleReload(sender);
            case "setlevel" -> handleSetLevel(sender, args);
            case "inspect" -> handleInspect(sender, args);
            case "remove" -> handleRemove(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /generatoradmin give <player> <type> [amount]");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return;
        }

        String type = args[2];
        if (plugin.getConfigManager().getGeneratorType(type) == null) {
            sender.sendMessage(ChatColor.RED + "Invalid generator type: " + type);
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                amount = Math.max(1, Math.min(amount, 64));
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount!");
                return;
            }
        }

        ItemStack item = GeneratorListener.createGeneratorItem(type);
        item.setAmount(amount);
        HashMap<Integer, ItemStack> overflow = target.getInventory().addItem(item);

        if (!overflow.isEmpty()) {
            for (ItemStack drop : overflow.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), drop);
            }
            sender.sendMessage(ChatColor.YELLOW + "Some items dropped on ground (inventory full)!");
        }

        sender.sendMessage(ChatColor.GREEN + "Gave " + amount + "x " + type + " generator to " + target.getName());
        target.sendMessage(ChatColor.GREEN + "You received " + amount + "x " + type + " generator!");
    }

    private void handleGiveAll(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /generatoradmin giveall <type>");
            return;
        }

        String type = args[1];
        if (plugin.getConfigManager().getGeneratorType(type) == null) {
            sender.sendMessage(ChatColor.RED + "Invalid generator type: " + type);
            return;
        }

        ItemStack item = GeneratorListener.createGeneratorItem(type);
        int count = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().addItem(item.clone());
            player.sendMessage(ChatColor.GREEN + "You received a " + type + " generator!");
            count++;
        }

        sender.sendMessage(ChatColor.GREEN + "Gave " + type + " generator to " + count + " players!");
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getConfigManager().reload();
        sender.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
    }

    private void handleSetLevel(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /generatoradmin setlevel <player> <type> <level>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return;
        }

        String type = args[2];
        if (plugin.getConfigManager().getGeneratorType(type) == null) {
            sender.sendMessage(ChatColor.RED + "Invalid generator type: " + type);
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid level!");
            return;
        }

        if (level < 1 || level > 10) {
            sender.sendMessage(ChatColor.RED + "Level must be between 1 and 10!");
            return;
        }

        List<Generator> generators = plugin.getGeneratorManager().getPlayerGenerators(target.getUniqueId());
        boolean found = false;

        for (Generator gen : generators) {
            if (gen.getType().equals(type)) {
                gen.setLevel(level);
                gen.markDirty();
                found = true;
                break;
            }
        }

        if (found) {
            sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s " + type + " generator to level " + level);
        } else {
            sender.sendMessage(ChatColor.RED + "No " + type + " generator found for " + target.getName());
        }
    }

    private void handleInspect(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /generatoradmin inspect [player]");
            return;
        }

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return;
        }

        List<Generator> generators = plugin.getGeneratorManager().getPlayerGenerators(target.getUniqueId());
        sender.sendMessage(ChatColor.GREEN + "--- " + target.getName() + "'s Generators (" + generators.size() + ") ---");

        for (Generator gen : generators) {
            sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + gen.getType() +
                    ChatColor.GRAY + " Lv." + gen.getLevel() +
                    ChatColor.GRAY + " | Stored: " + ChatColor.WHITE + gen.getStoredAmount() +
                    ChatColor.GRAY + " | " + ChatColor.WHITE + gen.getLocationString());
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /generatoradmin remove <player|all>");
            return;
        }

        if (args[1].equalsIgnoreCase("all")) {
            int count = plugin.getGeneratorManager().getTotalGeneratorCount();
            for (Generator gen : new ArrayList<>(plugin.getGeneratorManager().getAllGenerators())) {
                plugin.getGeneratorManager().removeGenerator(gen.getId());
            }
            sender.sendMessage(ChatColor.GREEN + "Removed all " + count + " generators!");
        } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return;
            }

            List<Generator> generators = new ArrayList<>(plugin.getGeneratorManager().getPlayerGenerators(target.getUniqueId()));
            for (Generator gen : generators) {
                plugin.getGeneratorManager().removeGenerator(gen.getId());
            }
            sender.sendMessage(ChatColor.GREEN + "Removed " + generators.size() + " generators from " + target.getName());
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "--- Generator Admin Commands ---");
        sender.sendMessage(ChatColor.YELLOW + "/generatoradmin give <player> <type> [amount]" + ChatColor.GRAY + " - Give generator");
        sender.sendMessage(ChatColor.YELLOW + "/generatoradmin giveall <type>" + ChatColor.GRAY + " - Give to all online");
        sender.sendMessage(ChatColor.YELLOW + "/generatoradmin reload" + ChatColor.GRAY + " - Reload config");
        sender.sendMessage(ChatColor.YELLOW + "/generatoradmin setlevel <player> <type> <level>" + ChatColor.GRAY + " - Set level");
        sender.sendMessage(ChatColor.YELLOW + "/generatoradmin inspect [player]" + ChatColor.GRAY + " - Inspect generators");
        sender.sendMessage(ChatColor.YELLOW + "/generatoradmin remove <player|all>" + ChatColor.GRAY + " - Remove generators");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("generators.admin")) return completions;

        if (args.length == 1) {
            completions.add("give");
            completions.add("giveall");
            completions.add("reload");
            completions.add("setlevel");
            completions.add("inspect");
            completions.add("remove");
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "give", "setlevel", "inspect", "remove" -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                    if (args[0].equalsIgnoreCase("remove")) {
                        completions.add("all");
                    }
                }
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "give", "giveall", "setlevel" -> {
                    for (String type : plugin.getConfigManager().getGeneratorTypes().keySet()) {
                        completions.add(type);
                    }
                }
            }
        }

        return completions;
    }
}
