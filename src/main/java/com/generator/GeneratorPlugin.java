package com.generator;

import com.generator.commands.AdminCommand;
import com.generator.commands.GeneratorCommand;
import com.generator.config.ConfigManager;
import com.generator.economy.EconomyHook;
import com.generator.economy.CoinManager;
import com.generator.generator.GeneratorManager;
import com.generator.gui.GUIManager;
import com.generator.gui.MenuManager;
import com.generator.gui.ProfileGUI;
import com.generator.gui.TeleportItem;
import com.generator.gui.AdminDashboard;
import com.generator.level.LevelManager;
import com.generator.reports.ReportManager;
import com.generator.profile.ProfileManager;
import com.generator.listeners.ProfileListener;
import com.generator.listeners.GeneratorListener;
import com.generator.shop.ShopManager;
import com.generator.shop.KitManager;
import com.generator.shop.TradeManager;
import com.generator.grief.AntiGriefManager;
import com.generator.grief.ClaimManager;
import com.generator.pvp.ArenaManager;
import com.generator.pvp.PvPStatsManager;
import com.generator.pvp.PvPRankManager;
import com.generator.pvp.PvPSeasonManager;
import com.generator.pvp.PvPTournamentManager;
import com.generator.pvp.PvPLeaderboardGUI;
import com.generator.warp.WarpManager;
import com.generator.prestige.PrestigeManager;
import com.generator.prestige.PrestigeGUI;
import com.generator.oneblock.OneBlockManager;
import com.generator.oneblock.OneBlockQuestManager;
import com.generator.oneblock.OneBlockEventManager;
import com.generator.oneblock.OneBlockGUI;
import com.generator.events.EventManager;
import com.generator.events.SeasonManager;
import com.generator.events.SeasonalQuestManager;
import com.generator.events.SeasonalLeaderboardGUI;
import com.generator.events.SeasonalRewardManager;
import com.generator.smp.SpawnManager;
import com.generator.smp.CommunityManager;
import com.generator.smp.PlayerShopManager;
import com.generator.smp.SMPEventManager;
import com.generator.storage.DatabaseManager;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class GeneratorPlugin extends JavaPlugin {

    private static GeneratorPlugin instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private GeneratorManager generatorManager;
    private EconomyHook economyHook;
    private GUIManager guiManager;
    private MenuManager menuManager;
    private ProfileManager profileManager;
    private ProfileGUI profileGUI;
    private TeleportItem teleportItem;
    private CoinManager coinManager;
    private LevelManager levelManager;
    private AdminDashboard adminDashboard;
    private ReportManager reportManager;
    private ShopManager shopManager;
    private KitManager kitManager;
    private TradeManager tradeManager;
    private AntiGriefManager antiGriefManager;
    private ClaimManager claimManager;
    private ArenaManager arenaManager;
    private PvPStatsManager pvpStatsManager;
    private PvPRankManager pvpRankManager;
    private PvPSeasonManager pvpSeasonManager;
    private PvPTournamentManager pvpTournamentManager;
    private PvPLeaderboardGUI pvpLeaderboardGUI;
    private WarpManager warpManager;
    private PrestigeManager prestigeManager;
    private PrestigeGUI prestigeGUI;
    private OneBlockManager oneBlockManager;
    private OneBlockQuestManager oneBlockQuestManager;
    private OneBlockEventManager oneBlockEventManager;
    private OneBlockGUI oneBlockGUI;
    private EventManager eventManager;
    private SeasonManager seasonManager;
    private SeasonalQuestManager seasonalQuestManager;
    private SeasonalLeaderboardGUI seasonalLeaderboardGUI;
    private SeasonalRewardManager seasonalRewardManager;
    private SpawnManager spawnManager;
    private CommunityManager communityManager;
    private PlayerShopManager playerShopManager;
    private SMPEventManager smpEventManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        configManager = new ConfigManager(getConfig());

        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        generatorManager = new GeneratorManager(this);

        economyHook = new EconomyHook(this);
        economyHook.setup();

        guiManager = new GUIManager(this);

        menuManager = new MenuManager(this);

        profileManager = new ProfileManager(this);

        profileGUI = new ProfileGUI(this);

        teleportItem = new TeleportItem(this);

        coinManager = new CoinManager(this);

        levelManager = new LevelManager(this);

        adminDashboard = new AdminDashboard(this);

        reportManager = new ReportManager(this);

        shopManager = new ShopManager(this);

        kitManager = new KitManager(this);

        tradeManager = new TradeManager(this);

        antiGriefManager = new AntiGriefManager(this);

        claimManager = new ClaimManager(this);

        arenaManager = new ArenaManager(this);
        pvpStatsManager = new PvPStatsManager(this);
        pvpRankManager = new PvPRankManager();
        pvpSeasonManager = new PvPSeasonManager(this, pvpStatsManager);
        pvpTournamentManager = new PvPTournamentManager(this, pvpStatsManager);
        pvpLeaderboardGUI = new PvPLeaderboardGUI(this, pvpStatsManager, pvpRankManager, pvpSeasonManager);

        warpManager = new WarpManager(this);

        prestigeManager = new PrestigeManager(this);
        prestigeGUI = new PrestigeGUI(this);

        oneBlockManager = new OneBlockManager(this);
        oneBlockQuestManager = new OneBlockQuestManager(this, oneBlockManager);
        oneBlockEventManager = new OneBlockEventManager(this, oneBlockManager);
        oneBlockGUI = new OneBlockGUI(this, oneBlockManager, oneBlockQuestManager, oneBlockEventManager);

        eventManager = new EventManager(this);
        seasonManager = new SeasonManager(this);
        seasonalQuestManager = new SeasonalQuestManager(this, seasonManager);
        seasonalLeaderboardGUI = new SeasonalLeaderboardGUI(this, seasonManager, eventManager, seasonalQuestManager);
        seasonalRewardManager = new SeasonalRewardManager(this, seasonManager);

        spawnManager = new SpawnManager(this);
        communityManager = new CommunityManager(this);
        playerShopManager = new PlayerShopManager(this);
        smpEventManager = new SMPEventManager(this);

        getServer().getPluginManager().registerEvents(new GeneratorListener(this), this);
        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(menuManager, this);
        getServer().getPluginManager().registerEvents(new ProfileListener(this), this);
        getServer().getPluginManager().registerEvents(profileGUI, this);
        getServer().getPluginManager().registerEvents(teleportItem, this);
        getServer().getPluginManager().registerEvents(coinManager, this);
        getServer().getPluginManager().registerEvents(levelManager, this);
        getServer().getPluginManager().registerEvents(adminDashboard, this);
        getServer().getPluginManager().registerEvents(reportManager, this);
        getServer().getPluginManager().registerEvents(shopManager, this);
        getServer().getPluginManager().registerEvents(kitManager, this);
        getServer().getPluginManager().registerEvents(tradeManager, this);
        getServer().getPluginManager().registerEvents(antiGriefManager, this);
        getServer().getPluginManager().registerEvents(claimManager, this);
        getServer().getPluginManager().registerEvents(arenaManager, this);
        getServer().getPluginManager().registerEvents(pvpTournamentManager, this);
        getServer().getPluginManager().registerEvents(pvpLeaderboardGUI, this);
        getServer().getPluginManager().registerEvents(warpManager, this);
        getServer().getPluginManager().registerEvents(prestigeGUI, this);
        getServer().getPluginManager().registerEvents(oneBlockManager, this);
        getServer().getPluginManager().registerEvents(oneBlockQuestManager, this);
        getServer().getPluginManager().registerEvents(oneBlockEventManager, this);
        getServer().getPluginManager().registerEvents(oneBlockGUI, this);
        getServer().getPluginManager().registerEvents(eventManager, this);
        getServer().getPluginManager().registerEvents(seasonalLeaderboardGUI, this);
        getServer().getPluginManager().registerEvents(spawnManager, this);
        getServer().getPluginManager().registerEvents(communityManager, this);
        getServer().getPluginManager().registerEvents(playerShopManager, this);
        getServer().getPluginManager().registerEvents(smpEventManager, this);

        getCommand("generator").setExecutor(new GeneratorCommand(this));
        getCommand("generatorshop").setExecutor(new GeneratorCommand(this));
        getCommand("generatoradmin").setExecutor(new AdminCommand(this));
        getCommand("generatoradmin").setTabCompleter((CommandSender sender, Command command, String alias, String[] args) -> {
            if (args.length == 1) {
                return java.util.List.of("give", "giveall", "reload", "setlevel", "inspect", "remove", "givecoins", "givexp");
            }
            return java.util.Collections.emptyList();
        });

        getCommand("menu").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                menuManager.openMainMenu(player);
            }
            return true;
        });

        getCommand("mode").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                menuManager.openModeSelector(player);
            }
            return true;
        });

        getCommand("profile").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                profileGUI.openProfile(player);
            }
            return true;
        });

        getCommand("sell").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                coinManager.sellInventory(player);
            }
            return true;
        });

        getCommand("daily").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                coinManager.claimDaily(player);
            }
            return true;
        });

        getCommand("coins").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                int coins = coinManager.getCoins(player);
                player.sendMessage(org.bukkit.ChatColor.GOLD + "Your balance: " + coins + " coins");
            }
            return true;
        });

        getCommand("admin").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                adminDashboard.openDashboard(player);
            }
            return true;
        });

        getCommand("report").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                reportManager.openReportPlayerGUI(player);
            }
            return true;
        });

        getCommand("reports").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                reportManager.openReportGUI(player);
            }
            return true;
        });

        getCommand("shop").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                shopManager.openShop(player);
            }
            return true;
        });

        getCommand("kit").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                kitManager.openKitGUI(player);
            }
            return true;
        });

        getCommand("trade").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                if (args.length == 0) {
                    player.sendMessage(org.bukkit.ChatColor.YELLOW + "Usage: /trade <player> | /trade accept | /trade decline");
                    return true;
                }
                String sub = args[0].toLowerCase();
                if (sub.equals("accept")) {
                    tradeManager.acceptTrade(player);
                } else if (sub.equals("decline")) {
                    tradeManager.declineTrade(player);
                } else {
                    org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(args[0]);
                    if (target == null) {
                        player.sendMessage(org.bukkit.ChatColor.RED + "Player not found!");
                        return true;
                    }
                    tradeManager.sendTradeRequest(player, target);
                }
            }
            return true;
        });

        getCommand("claim").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                if (args.length == 0) {
                    claimManager.openClaimGUI(player);
                    return true;
                }
                String sub = args[0].toLowerCase();
                switch (sub) {
                    case "create" -> {
                        claimManager.enterClaimMode(player);
                        player.closeInventory();
                    }
                    case "remove" -> {
                        if (args.length < 2) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Usage: /claim remove <name>");
                            return true;
                        }
                        String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                        if (claimManager.removeClaim(player.getUniqueId(), name)) {
                            player.sendMessage(org.bukkit.ChatColor.GREEN + "Claim removed!");
                        } else {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Claim not found!");
                        }
                    }
                    case "list" -> {
                        java.util.List<com.generator.grief.ClaimManager.Claim> claims = claimManager.getPlayerClaims(player.getUniqueId());
                        if (claims.isEmpty()) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "You have no claims!");
                        } else {
                            player.sendMessage(org.bukkit.ChatColor.GREEN + "=== YOUR CLAIMS ===");
                            for (com.generator.grief.ClaimManager.Claim claim : claims) {
                                player.sendMessage(org.bukkit.ChatColor.YELLOW + claim.name + ": " +
                                        "(" + claim.min.getBlockX() + "," + claim.min.getBlockY() + "," + claim.min.getBlockZ() + ") - " +
                                        "(" + claim.max.getBlockX() + "," + claim.max.getBlockY() + "," + claim.max.getBlockZ() + ")");
                            }
                        }
                    }
                    case "trust" -> {
                        if (args.length < 3) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Usage: /claim trust <player> <claim>");
                            return true;
                        }
                        org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(args[1]);
                        if (target == null) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Player not found!");
                            return true;
                        }
                        String claimName = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                        if (claimManager.trustPlayer(player.getUniqueId(), target.getUniqueId(), claimName)) {
                            player.sendMessage(org.bukkit.ChatColor.GREEN + target.getName() + " trusted in " + claimName + "!");
                        } else {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Claim not found!");
                        }
                    }
                    case "untrust" -> {
                        if (args.length < 3) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Usage: /claim untrust <player> <claim>");
                            return true;
                        }
                        org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(args[1]);
                        if (target == null) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Player not found!");
                            return true;
                        }
                        String claimName = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                        if (claimManager.untrustPlayer(player.getUniqueId(), target.getUniqueId(), claimName)) {
                            player.sendMessage(org.bukkit.ChatColor.GREEN + target.getName() + " untrusted from " + claimName + "!");
                        } else {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Claim not found!");
                        }
                    }
                    case "cancel" -> {
                        claimManager.exitClaimMode(player);
                    }
                    case "info" -> {
                        com.generator.grief.ClaimManager.Claim claim = claimManager.getClaimAt(player.getLocation());
                        if (claim == null) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "You are not in a claim!");
                        } else {
                            player.sendMessage(org.bukkit.ChatColor.GREEN + "Claim: " + claim.name);
                            player.sendMessage(org.bukkit.ChatColor.YELLOW + "Owner: " + org.bukkit.Bukkit.getOfflinePlayer(claim.owner).getName());
                            player.sendMessage(org.bukkit.ChatColor.YELLOW + "Trusted: " + claim.trusted.size() + " players");
                        }
                    }
                    default -> player.sendMessage(org.bukkit.ChatColor.RED + "Usage: /claim [create|remove|list|trust|untrust|info|cancel]");
                }
            }
            return true;
        });

        getCommand("claim").setTabCompleter(claimManager);

        getCommand("antigrief").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                antiGriefManager.toggleAntiGrief(player);
            }
            return true;
        });

        getCommand("pvp").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                if (args.length == 0) {
                    arenaManager.openArenaGUI(player);
                    return true;
                }
                String sub = args[0].toLowerCase();
                switch (sub) {
                    case "join" -> {
                        if (args.length < 2) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Usage: /pvp join <arena>");
                            return true;
                        }
                        arenaManager.joinArena(player, args[1]);
                    }
                    case "leave" -> {
                        arenaManager.leaveArena(player);
                    }
                    case "leaderboard", "lb" -> {
                        pvpLeaderboardGUI.openLeaderboard(player, args.length >= 2 ? args[1] : "elo");
                    }
                    case "stats" -> {
                        org.bukkit.entity.Player target = args.length >= 2 ? org.bukkit.Bukkit.getPlayer(args[1]) : player;
                        if (target == null) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Player not found!");
                            return true;
                        }
                        pvpLeaderboardGUI.openStatsGUI(player, target);
                    }
                    case "rank" -> {
                        PvPStatsManager.PvPStats stats = pvpStatsManager.getStats(player.getUniqueId());
                        PvPRankManager.PvPRank rank = pvpRankManager.getRank(stats.elo);
                        player.sendMessage(org.bukkit.ChatColor.GREEN + "=== PVP RANK ===");
                        player.sendMessage(org.bukkit.ChatColor.YELLOW + "Rank: " + rank.chatColor + rank.name);
                        player.sendMessage(org.bukkit.ChatColor.YELLOW + "ELO: " + stats.elo);
                        player.sendMessage(org.bukkit.ChatColor.YELLOW + "Progress: " + pvpRankManager.getProgressBar(stats.elo));
                        player.sendMessage(org.bukkit.ChatColor.YELLOW + "Season: " + pvpSeasonManager.getCurrentSeason().name);
                        player.sendMessage(org.bukkit.ChatColor.YELLOW + "Time Left: " + pvpSeasonManager.getTimeRemainingFormatted());
                    }
                    case "season" -> {
                        PvPSeasonManager.PvPSeason season = pvpSeasonManager.getCurrentSeason();
                        player.sendMessage(org.bukkit.ChatColor.GREEN + "=== CURRENT SEASON ===");
                        player.sendMessage(org.bukkit.ChatColor.YELLOW + "Name: " + season.name);
                        player.sendMessage(org.bukkit.ChatColor.YELLOW + "Time Left: " + pvpSeasonManager.getTimeRemainingFormatted());
                        player.sendMessage(org.bukkit.ChatColor.YELLOW + "Rewards: First " + season.rewardCoins + " coins");
                    }
                    case "tournament" -> {
                        if (args.length < 2) {
                            pvpTournamentManager.openTournamentGUI(player);
                            return true;
                        }
                        String tourneySub = args[1].toLowerCase();
                        switch (tourneySub) {
                            case "create" -> {
                                if (args.length < 6) {
                                    player.sendMessage(org.bukkit.ChatColor.RED + "Usage: /pvp tournament create <name> <type> <maxPlayers> <entryFee> <reward>");
                                    return true;
                                }
                                int maxPlayers;
                                int entryFee;
                                int reward;
                                try {
                                    maxPlayers = Integer.parseInt(args[3]);
                                    entryFee = Integer.parseInt(args[4]);
                                    reward = Integer.parseInt(args[5]);
                                } catch (NumberFormatException e) {
                                    player.sendMessage(org.bukkit.ChatColor.RED + "Invalid numbers!");
                                    return true;
                                }
                                pvpTournamentManager.createTournament(args[2], "ffa", maxPlayers, entryFee, reward);
                            }
                            case "join" -> {
                                if (args.length < 3) {
                                    player.sendMessage(org.bukkit.ChatColor.RED + "Usage: /pvp tournament join <name>");
                                    return true;
                                }
                                pvpTournamentManager.joinTournament(player, args[2]);
                            }
                            case "leave" -> {
                                pvpTournamentManager.leaveTournament(player);
                            }
                            case "start" -> {
                                if (args.length < 3 || !sender.hasPermission("generators.admin")) {
                                    player.sendMessage(org.bukkit.ChatColor.RED + "Usage: /pvp tournament start <name>");
                                    return true;
                                }
                                pvpTournamentManager.startTournament(args[2]);
                            }
                            default -> pvpTournamentManager.openTournamentGUI(player);
                        }
                    }
                    case "list" -> {
                        player.sendMessage(org.bukkit.ChatColor.GREEN + "=== ARENAS ===");
                        for (com.generator.pvp.ArenaManager.Arena arena : arenaManager.getArenas()) {
                            player.sendMessage(org.bukkit.ChatColor.YELLOW + arena.displayName +
                                    " §7(" + arena.type + ") §f- " + (arena.enabled ? "§aEnabled" : "§cDisabled"));
                        }
                    }
                    default -> player.sendMessage(org.bukkit.ChatColor.RED + "Usage: /pvp [join|leave|leaderboard|stats|rank|season|tournament|list]");
                }
            }
            return true;
        });

        getCommand("pvp").setTabCompleter((sender, command, alias, args) -> {
            if (args.length == 1) {
                return java.util.List.of("join", "leave", "leaderboard", "stats", "rank", "season", "tournament", "list");
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
                return arenaManager.getArenas().stream()
                        .map(a -> a.name)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("leaderboard")) {
                return java.util.List.of("elo", "kills", "wins", "season");
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("tournament")) {
                return java.util.List.of("create", "join", "leave", "start");
            }
            return java.util.Collections.emptyList();
        });

        getCommand("warp").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                if (args.length == 0) {
                    warpManager.openWarpGUI(player);
                    return true;
                }
                String sub = args[0].toLowerCase();
                switch (sub) {
                    case "set" -> {
                        if (args.length < 2) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Usage: /warp set <name> [cost]");
                            return true;
                        }
                        int cost;
                        try {
                            cost = args.length >= 3 ? Integer.parseInt(args[2]) : 100;
                        } catch (NumberFormatException e) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Invalid cost number!");
                            return true;
                        }
                        if (cost < 0) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Cost must be 0 or more!");
                            return true;
                        }
                        warpManager.setWarp(player, args[1], cost);
                    }
                    case "delete", "del" -> {
                        if (args.length < 2) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Usage: /warp delete <name>");
                            return true;
                        }
                        if (warpManager.deleteWarp(args[1])) {
                            player.sendMessage(org.bukkit.ChatColor.GREEN + "Warp deleted!");
                        } else {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Warp not found!");
                        }
                    }
                    case "list" -> {
                        player.sendMessage(org.bukkit.ChatColor.GREEN + "=== WARPS ===");
                        for (java.util.Map.Entry<String, com.generator.warp.WarpManager.Warp> entry : warpManager.getWarps().entrySet()) {
                            com.generator.warp.WarpManager.Warp warp = entry.getValue();
                            player.sendMessage(org.bukkit.ChatColor.YELLOW + warp.displayName +
                                    " §7(Cost: §6" + warp.cost + " §7coins)");
                        }
                    }
                    default -> {
                        warpManager.teleportWarp(player, sub);
                    }
                }
            }
            return true;
        });

        getCommand("home").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                if (args.length == 0) {
                    warpManager.openHomesGUI(player);
                    return true;
                }
                String sub = args[0].toLowerCase();
                switch (sub) {
                    case "set" -> {
                        String name = args.length >= 2 ? args[1] : "home";
                        warpManager.setHome(player, name);
                    }
                    case "delete", "del" -> {
                        if (args.length < 2) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Usage: /home delete <name>");
                            return true;
                        }
                        warpManager.deleteHome(player, args[1]);
                    }
                    case "list" -> {
                        java.util.List<com.generator.warp.WarpManager.Home> homes = warpManager.getHomes(player.getUniqueId());
                        if (homes.isEmpty()) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "You have no homes!");
                        } else {
                            player.sendMessage(org.bukkit.ChatColor.GREEN + "=== YOUR HOMES ===");
                            for (com.generator.warp.WarpManager.Home home : homes) {
                                player.sendMessage(org.bukkit.ChatColor.YELLOW + home.name + " §7(" + home.world + ")");
                            }
                        }
                    }
                    default -> {
                        warpManager.teleportHome(player, sub);
                    }
                }
            }
            return true;
        });

        getCommand("tpa").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                if (args.length == 0) {
                    warpManager.openTPAGUI(player);
                    return true;
                }
                String sub = args[0].toLowerCase();
                switch (sub) {
                    case "accept" -> warpManager.acceptTPA(player);
                    case "deny" -> warpManager.denyTPA(player);
                    default -> {
                        org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(args[0]);
                        if (target == null) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Player not found!");
                            return true;
                        }
                        warpManager.sendTPA(player, target);
                    }
                }
            }
            return true;
        });

        getCommand("tpahere").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                if (args.length == 0) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Usage: /tpahere <player>");
                    return true;
                }
                org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(args[0]);
                if (target == null) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Player not found!");
                    return true;
                }
                warpManager.sendTPAHere(player, target);
            }
            return true;
        });

        getCommand("prestige").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                prestigeGUI.openPrestigeMain(player);
            }
            return true;
        });

        getCommand("oneblock").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                if (args.length == 0) {
                    oneBlockGUI.openMainMenu(player);
                    return true;
                }
                String sub = args[0].toLowerCase();
                switch (sub) {
                    case "info" -> {
                        OneBlockManager.OneBlockProgress progress = oneBlockManager.getProgress(player.getUniqueId());
                        String currentPhase = oneBlockManager.getCurrentPhaseName(player.getUniqueId());
                        int phaseProgress = oneBlockManager.getPhaseProgress(player.getUniqueId());
                        player.sendMessage(org.bukkit.ChatColor.GREEN + "=== ONEBLOCK INFO ===");
                        player.sendMessage(org.bukkit.ChatColor.YELLOW + "Current Phase: " + currentPhase);
                        player.sendMessage(org.bukkit.ChatColor.YELLOW + "Phase Progress: " + phaseProgress + "%");
                        player.sendMessage(org.bukkit.ChatColor.YELLOW + "Total Blocks: " + progress.totalBroken);
                        player.sendMessage(org.bukkit.ChatColor.YELLOW + "Phases Completed: " + progress.phasesCompleted);
                    }
                    case "phases" -> oneBlockGUI.openPhasesMenu(player);
                    case "quests" -> oneBlockGUI.openQuestsMenu(player);
                    case "events" -> oneBlockGUI.openEventsMenu(player);
                    case "leaderboard" -> oneBlockGUI.openLeaderboardMenu(player);
                    case "reset" -> {
                        if (sender.hasPermission("generators.admin")) {
                            if (args.length >= 2) {
                                org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(args[1]);
                                if (target != null) {
                                    oneBlockManager.resetPlayer(target.getUniqueId());
                                    player.sendMessage(org.bukkit.ChatColor.GREEN + "Reset OneBlock progress for " + target.getName());
                                } else {
                                    player.sendMessage(org.bukkit.ChatColor.RED + "Player not found!");
                                }
                            }
                        }
                    }
                    default -> oneBlockGUI.openMainMenu(player);
                }
            }
            return true;
        });

        getCommand("back").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                spawnManager.goBack(player);
            }
            return true;
        });

        getCommand("hat").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                ItemStack helmet = player.getInventory().getHelmet();
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) {
                    player.sendMessage(org.bukkit.ChatColor.RED + "Hold an item to wear as a hat!");
                    return true;
                }
                player.getInventory().setHelmet(hand);
                player.getInventory().setItemInMainHand(helmet);
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Hat equipped!");
            }
            return true;
        });

        getCommand("near").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                int radius = args.length > 0 ? Integer.parseInt(args[0]) : 50;
                List<Player> nearby = new ArrayList<>();
                for (Player p : player.getWorld().getPlayers()) {
                    if (p != player && p.getLocation().distance(player.getLocation()) <= radius) {
                        nearby.add(p);
                    }
                }
                if (nearby.isEmpty()) {
                    player.sendMessage(org.bukkit.ChatColor.YELLOW + "No players within " + radius + " blocks.");
                } else {
                    player.sendMessage(org.bukkit.ChatColor.GREEN + "=== PLAYERS NEARBY ===");
                    for (Player p : nearby) {
                        int dist = (int) p.getLocation().distance(player.getLocation());
                        player.sendMessage(org.bukkit.ChatColor.YELLOW + p.getName() + " - " + dist + " blocks");
                    }
                }
            }
            return true;
        });

        getCommand("fly").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                if (player.hasPermission("generators.fly")) {
                    if (player.getAllowFlight()) {
                        player.setAllowFlight(false);
                        player.setFlying(false);
                        player.sendMessage(org.bukkit.ChatColor.YELLOW + "Fly mode disabled.");
                    } else {
                        player.setAllowFlight(true);
                        player.sendMessage(org.bukkit.ChatColor.GREEN + "Fly mode enabled.");
                    }
                } else {
                    player.sendMessage(org.bukkit.ChatColor.RED + "You don't have permission to fly!");
                }
            }
            return true;
        });

        getCommand("spawn").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                if (args.length == 0) {
                    spawnManager.teleportToDefaultSpawn(player);
                    return true;
                }
                String sub = args[0].toLowerCase();
                if (sub.equals("set") && args.length >= 2 && sender.hasPermission("generators.admin")) {
                    spawnManager.setSpawn(args[1], player.getLocation());
                    player.sendMessage(org.bukkit.ChatColor.GREEN + "Spawn set: " + args[1]);
                } else if (sub.equals("remove") && args.length >= 2 && sender.hasPermission("generators.admin")) {
                    if (spawnManager.removeSpawn(args[1])) {
                        player.sendMessage(org.bukkit.ChatColor.GREEN + "Spawn removed: " + args[1]);
                    } else {
                        player.sendMessage(org.bukkit.ChatColor.RED + "Spawn not found!");
                    }
                } else if (sub.equals("list") && sender.hasPermission("generators.admin")) {
                    player.sendMessage(org.bukkit.ChatColor.GREEN + "=== SPAWNS ===");
                    for (Map.Entry<String, org.bukkit.Location> entry : spawnManager.getAllSpawns().entrySet()) {
                        org.bukkit.Location loc = entry.getValue();
                        player.sendMessage(org.bukkit.ChatColor.YELLOW + entry.getKey() + ": " +
                                loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")");
                    }
                } else {
                    spawnManager.teleportToSpawn(player, sub);
                }
            } else if (sender.hasPermission("generators.admin") && args.length >= 3 && args[0].equalsIgnoreCase("set")) {
                org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(args[1]);
                if (target != null) {
                    spawnManager.setSpawn(args[2], target.getLocation());
                    sender.sendMessage(org.bukkit.ChatColor.GREEN + "Spawn set: " + args[2]);
                } else {
                    sender.sendMessage(org.bukkit.ChatColor.RED + "Player not found!");
                }
            }
            return true;
        });

        getCommand("community").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                if (args.length == 0) {
                    openCommunityGUI(player);
                    return true;
                }
                String sub = args[0].toLowerCase();
                switch (sub) {
                    case "list" -> {
                        player.sendMessage(org.bukkit.ChatColor.GREEN + "=== COMMUNITY AREAS ===");
                        for (CommunityManager.CommunityArea area : communityManager.getAllAreas()) {
                            boolean unlocked = communityManager.canAccess(player, area.id);
                            String status = unlocked ? "§a[Unlocked]" : "§c[Locked]";
                            player.sendMessage(org.bukkit.ChatColor.YELLOW + area.displayName + " " + status +
                                    " §7- " + area.description);
                        }
                    }
                    case "unlock" -> {
                        if (args.length < 2) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Usage: /community unlock <area>");
                            return true;
                        }
                        communityManager.unlockArea(player, args[1]);
                    }
                    case "tp", "teleport" -> {
                        if (args.length < 2) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Usage: /community tp <area>");
                            return true;
                        }
                        communityManager.teleportToArea(player, args[1]);
                    }
                    default -> openCommunityGUI(player);
                }
            }
            return true;
        });

        getCommand("community").setTabCompleter((sender, command, alias, args) -> {
            if (args.length == 1) return java.util.List.of("list", "unlock", "tp");
            if (args.length == 2 && (args[0].equalsIgnoreCase("unlock") || args[0].equalsIgnoreCase("tp"))) {
                return communityManager.getAllAreas().stream()
                        .map(a -> a.id)
                        .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            }
            return java.util.Collections.emptyList();
        });

        getCommand("playershop").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                if (args.length == 0) {
                    openPlayerShopListGUI(player);
                    return true;
                }
                String sub = args[0].toLowerCase();
                switch (sub) {
                    case "create" -> {
                        if (args.length < 4) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Usage: /playershop create <name> <material> <buyPrice> [sellPrice]");
                            return true;
                        }
                        Material mat = Material.matchMaterial(args[2].toUpperCase());
                        if (mat == null) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Invalid material: " + args[2]);
                            return true;
                        }
                        int buyPrice;
                        try {
                            buyPrice = Integer.parseInt(args[3]);
                        } catch (NumberFormatException e) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Invalid buy price!");
                            return true;
                        }
                        int sellPrice = args.length >= 5 ? Integer.parseInt(args[4]) : 0;
                        playerShopManager.createShop(player, args[1], mat, buyPrice, sellPrice);
                    }
                    case "delete" -> {
                        if (args.length < 2) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Usage: /playershop delete <shopId>");
                            return true;
                        }
                        playerShopManager.deleteShop(player, args[1]);
                    }
                    case "list" -> {
                        List<PlayerShopManager.PlayerShop> myShops = playerShopManager.getPlayerShops(player.getUniqueId());
                        if (myShops.isEmpty()) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "You have no shops!");
                        } else {
                            player.sendMessage(org.bukkit.ChatColor.GREEN + "=== YOUR SHOPS ===");
                            for (PlayerShopManager.PlayerShop shop : myShops) {
                                player.sendMessage(org.bukkit.ChatColor.YELLOW + shop.name + " §7(" + shop.id + ") §f- " + shop.itemName);
                            }
                        }
                    }
                    default -> openPlayerShopListGUI(player);
                }
            }
            return true;
        });

        getCommand("smp").setExecutor((sender, command, label, args) -> {
            if (args.length == 0) {
                sender.sendMessage(org.bukkit.ChatColor.YELLOW + "Usage: /smp event <type>");
                return true;
            }
            String sub = args[0].toLowerCase();
            if (sub.equals("event") && args.length >= 2 && sender.hasPermission("generators.admin")) {
                String type = args[1].toLowerCase();
                if (smpEventManager.canStartEvent(type)) {
                    smpEventManager.startEvent(type);
                    sender.sendMessage(org.bukkit.ChatColor.GREEN + "Started event: " + type);
                } else {
                    sender.sendMessage(org.bukkit.ChatColor.RED + "Event on cooldown or already active!");
                }
            } else if (sub.equals("list")) {
                sender.sendMessage(org.bukkit.ChatColor.GREEN + "=== ACTIVE EVENTS ===");
                for (SMPEventManager.SMPEvent event : smpEventManager.getAllActiveEvents()) {
                    sender.sendMessage(org.bukkit.ChatColor.YELLOW + event.displayName + " §7(" + event.type + ")");
                }
            } else if (sub.equals("stop") && args.length >= 2 && sender.hasPermission("generators.admin")) {
                smpEventManager.endEvent(args[1]);
                sender.sendMessage(org.bukkit.ChatColor.GREEN + "Event stopped.");
            } else {
                sender.sendMessage(org.bukkit.ChatColor.RED + "Usage: /smp event <type> | /smp list | /smp stop <id>");
            }
            return true;
        });

        getCommand("events").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                if (args.length == 0) {
                    eventManager.openEventGUI(player);
                    return true;
                }
                String sub = args[0].toLowerCase();
                switch (sub) {
                    case "create" -> {
                        if (args.length < 6 || !sender.hasPermission("generators.admin")) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Usage: /events create <name> <type> <duration> <reward> <required>");
                            return true;
                        }
                        long duration;
                        int reward;
                        int required;
                        try {
                            duration = Long.parseLong(args[3]) * 60000;
                            reward = Integer.parseInt(args[4]);
                            required = Integer.parseInt(args[5]);
                        } catch (NumberFormatException e) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Invalid numbers!");
                            return true;
                        }
                        eventManager.createEvent(args[1], args[2], duration, reward, required);
                        player.sendMessage(org.bukkit.ChatColor.GREEN + "Event created!");
                    }
                    case "stop" -> {
                        if (args.length < 2 || !sender.hasPermission("generators.admin")) {
                            player.sendMessage(org.bukkit.ChatColor.RED + "Usage: /events stop <id>");
                            return true;
                        }
                        eventManager.endEvent(args[1]);
                        player.sendMessage(org.bukkit.ChatColor.GREEN + "Event stopped.");
                    }
                    case "list" -> {
                        eventManager.openEventGUI(player);
                    }
                    default -> eventManager.openEventGUI(player);
                }
            }
            return true;
        });

        getCommand("events").setTabCompleter((sender, command, alias, args) -> {
            if (args.length == 1) return java.util.List.of("create", "stop", "list");
            return java.util.Collections.emptyList();
        });

        getCommand("season").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                if (args.length == 0) {
                    seasonManager.openSeasonGUI(player);
                    return true;
                }
                String sub = args[0].toLowerCase();
                switch (sub) {
                    case "info" -> seasonalLeaderboardGUI.openSeasonInfo(player);
                    case "leaderboard", "lb" -> seasonalLeaderboardGUI.openLeaderboard(player, args.length >= 2 ? args[1] : "xp");
                    case "quests" -> seasonalQuestManager.openQuestGUI(player);
                    case "rewards" -> seasonalRewardManager.openRewardGUI(player);
                    case "claim" -> seasonalRewardManager.claimSeasonRewards(player);
                    case "next" -> {
                        if (sender.hasPermission("generators.admin")) {
                            seasonManager.activateNextSeason();
                            player.sendMessage(org.bukkit.ChatColor.GREEN + "Activated next season!");
                        }
                    }
                    default -> seasonManager.openSeasonGUI(player);
                }
            }
            return true;
        });

        getCommand("season").setTabCompleter((sender, command, alias, args) -> {
            if (args.length == 1) return java.util.List.of("info", "leaderboard", "quests", "rewards", "claim", "next");
            return java.util.Collections.emptyList();
        });

        getServer().getScheduler().runTaskLater(this, () -> {
            generatorManager.loadAll();
            generatorManager.enable();
        }, 10L);

        getLogger().info("GeneratorPlugin enabled! Loaded " + configManager.getGeneratorTypes().size() + " generator types.");
    }

    @Override
    public void onDisable() {
        instance = null;
        if (generatorManager != null) {
            generatorManager.saveAll();
            generatorManager.disable();
        }
        if (profileManager != null) {
            profileManager.saveAll();
        }
        if (reportManager != null) {
            reportManager.saveReports();
        }
        if (arenaManager != null) {
            arenaManager.saveArenas();
            arenaManager.saveStats();
        }
        if (pvpStatsManager != null) {
            pvpStatsManager.saveStats();
        }
        if (pvpSeasonManager != null) {
            pvpSeasonManager.saveSeasons();
        }
        if (eventManager != null) {
            eventManager.saveEvents();
        }
        if (seasonManager != null) {
            seasonManager.saveSeasons();
            seasonManager.savePlayerData();
        }
        if (seasonalQuestManager != null) {
            seasonalQuestManager.saveQuests();
            seasonalQuestManager.saveProgress();
        }
        if (warpManager != null) {
            warpManager.saveWarps();
            warpManager.saveHomes();
        }
        if (claimManager != null) {
            claimManager.saveClaims();
        }
        if (prestigeManager != null) {
            prestigeManager.save();
        }
        if (oneBlockManager != null) {
            oneBlockManager.savePhases();
            oneBlockManager.saveProgress();
        }
        if (oneBlockQuestManager != null) {
            oneBlockQuestManager.saveQuests();
            oneBlockQuestManager.saveCompletedQuests();
        }
        if (communityManager != null) {
            communityManager.saveAreas();
            communityManager.saveUnlocked();
        }
        if (playerShopManager != null) {
            playerShopManager.saveShops();
        }
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("GeneratorPlugin disabled!");
    }

    public static GeneratorPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public GeneratorManager getGeneratorManager() {
        return generatorManager;
    }

    public EconomyHook getEconomyHook() {
        return economyHook;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public ProfileGUI getProfileGUI() {
        return profileGUI;
    }

    public TeleportItem getTeleportItem() {
        return teleportItem;
    }

    public CoinManager getCoinManager() {
        return coinManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public AdminDashboard getAdminDashboard() {
        return adminDashboard;
    }

    public ReportManager getReportManager() {
        return reportManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public TradeManager getTradeManager() {
        return tradeManager;
    }

    public AntiGriefManager getAntiGriefManager() {
        return antiGriefManager;
    }

    public ClaimManager getClaimManager() {
        return claimManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public PvPStatsManager getPvPStatsManager() {
        return pvpStatsManager;
    }

    public PvPRankManager getPvPRankManager() {
        return pvpRankManager;
    }

    public PvPSeasonManager getPvPSeasonManager() {
        return pvpSeasonManager;
    }

    public PvPTournamentManager getPvPTournamentManager() {
        return pvpTournamentManager;
    }

    public PvPLeaderboardGUI getPvPLeaderboardGUI() {
        return pvpLeaderboardGUI;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public SeasonManager getSeasonManager() {
        return seasonManager;
    }

    public SeasonalQuestManager getSeasonalQuestManager() {
        return seasonalQuestManager;
    }

    public SeasonalLeaderboardGUI getSeasonalLeaderboardGUI() {
        return seasonalLeaderboardGUI;
    }

    public SeasonalRewardManager getSeasonalRewardManager() {
        return seasonalRewardManager;
    }

    public WarpManager getWarpManager() {
        return warpManager;
    }

    public PrestigeManager getPrestigeManager() {
        return prestigeManager;
    }

    public PrestigeGUI getPrestigeGUI() {
        return prestigeGUI;
    }

    public OneBlockManager getOneBlockManager() {
        return oneBlockManager;
    }

    public OneBlockQuestManager getOneBlockQuestManager() {
        return oneBlockQuestManager;
    }

    public OneBlockEventManager getOneBlockEventManager() {
        return oneBlockEventManager;
    }

    public OneBlockGUI getOneBlockGUI() {
        return oneBlockGUI;
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public CommunityManager getCommunityManager() {
        return communityManager;
    }

    public PlayerShopManager getPlayerShopManager() {
        return playerShopManager;
    }

    public SMPEventManager getSMPEventManager() {
        return smpEventManager;
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(java.util.Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void openCommunityGUI(Player player) {
        org.bukkit.inventory.Inventory gui = org.bukkit.Bukkit.createInventory(null, 27, "COMMUNITY_MENU");

        int slot = 10;
        for (CommunityManager.CommunityArea area : communityManager.getAllAreas()) {
            if (slot >= 17) break;
            boolean unlocked = communityManager.canAccess(player, area.id);
            org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(area.material != null ? area.material : org.bukkit.Material.CHEST);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(org.bukkit.ChatColor.GOLD + area.displayName);
                meta.setLore(java.util.Arrays.asList(
                        "",
                        "§7" + area.description,
                        "",
                        unlocked ? "§aClick to teleport" : "§cLocked - Click to unlock",
                        unlocked ? "" : "§7Cost: §6" + area.requiredCoins + " coins"
                ));
                item.setItemMeta(meta);
            }
            gui.setItem(slot, item);
            slot++;
            if (slot == 17) slot = 19;
        }

        gui.setItem(22, createItem(org.bukkit.Material.BARRIER, org.bukkit.ChatColor.RED + "Close"));

        player.openInventory(gui);
    }

    private void openPlayerShopListGUI(Player player) {
        org.bukkit.inventory.Inventory gui = org.bukkit.Bukkit.createInventory(null, 54, "PLAYER_SHOPS_LIST");

        List<PlayerShopManager.PlayerShop> shops = playerShopManager.getAllShops();
        int slot = 0;
        for (PlayerShopManager.PlayerShop shop : shops) {
            if (slot >= 45) break;
            org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(shop.item != null ? shop.item : org.bukkit.Material.CHEST);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(org.bukkit.ChatColor.GOLD + shop.name);
                meta.setLore(java.util.Arrays.asList(
                        "",
                        "§7Owner: §f" + org.bukkit.Bukkit.getOfflinePlayer(shop.owner).getName(),
                        "§7Item: §f" + shop.itemName,
                        "§7Buy: §6" + shop.buyPrice + " §7coins",
                        "§7Sell: §6" + shop.sellPrice + " §7coins",
                        "§7Stock: §f" + shop.stock + "/" + shop.maxStock
                ));
                item.setItemMeta(meta);
            }
            gui.setItem(slot, item);
            slot++;
        }

        gui.setItem(53, createItem(org.bukkit.Material.BARRIER, org.bukkit.ChatColor.RED + "Close"));

        player.openInventory(gui);
    }
}
