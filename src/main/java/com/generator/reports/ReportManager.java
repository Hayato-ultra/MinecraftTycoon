package com.generator.reports;

import com.generator.GeneratorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReportManager implements Listener {

    private final GeneratorPlugin plugin;
    private final Set<Inventory> managedInventories = Collections.synchronizedSet(new HashSet<>());
    private final Map<Inventory, String> inventoryTitles = new HashMap<>();
    private final List<Report> reports = new CopyOnWriteArrayList<>();
    private final File reportsFile;

    public ReportManager(GeneratorPlugin plugin) {
        this.plugin = plugin;
        this.reportsFile = new File(plugin.getDataFolder(), "reports.yml");
        loadReports();
    }

    public void createReport(Player reporter, Player reported, String reason) {
        Report report = new Report(
                UUID.randomUUID(),
                reporter.getUniqueId(),
                reporter.getName(),
                reported.getUniqueId(),
                reported.getName(),
                reason,
                System.currentTimeMillis(),
                "OPEN"
        );
        reports.add(report);
        saveReports();

        reporter.sendMessage(ChatColor.GREEN + "Report submitted! ID: " + report.id.toString().substring(0, 8));
        reporter.playSound(reporter.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        // Notify online admins
        for (Player admin : Bukkit.getOnlinePlayers()) {
            if (admin.hasPermission("generators.admin") && !admin.equals(reporter)) {
                admin.sendMessage(ChatColor.RED + "[REPORT] " + reporter.getName() + " reported " + reported.getName() + ": " + reason);
            }
        }
    }

    public void openReportGUI(Player player) {
        Inventory gui = createManaged(54, "REPORTS_LIST", player);

        gui.setItem(4, createItem(Material.BOOK,
                ChatColor.RED + "" + ChatColor.BOLD + "REPORTS"));

        int slot = 10;
        for (Report report : reports) {
            if (slot >= 44) break;
            if (!report.status.equals("OPEN")) continue;

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "Report: " + report.reportedName);
                meta.setLore(Arrays.asList(
                        "",
                        "§7Reporter: §f" + report.reporterName,
                        "§7Reason: §f" + report.reason,
                        "§7Date: §f" + formatDate(report.timestamp),
                        "§7ID: §f" + report.id.toString().substring(0, 8),
                        "",
                        "§eClick to manage"));
                item.setItemMeta(meta);
            }
            gui.setItem(slot, item);
            slot++;
        }

        if (slot == 10) {
            gui.setItem(13, createItem(Material.BARRIER,
                    ChatColor.GREEN + "No open reports"));
        }

        gui.setItem(49, createItem(Material.BARRIER, ChatColor.RED + "Close"));

        player.openInventory(gui);
    }

    public void openReportPlayerGUI(Player reporter) {
        Inventory gui = createManaged(27, "REPORT_PLAYER", reporter);

        gui.setItem(4, createItem(Material.BOOK,
                ChatColor.RED + "" + ChatColor.BOLD + "REPORT PLAYER"));

        int slot = 10;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(reporter)) continue;
            if (slot >= 17) break;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(online);
                meta.setDisplayName(ChatColor.GREEN + online.getName());
                meta.setLore(Arrays.asList(
                        "",
                        "§7Click to report",
                        "§7for rule breaking"));
                head.setItemMeta(meta);
            }
            gui.setItem(slot, head);
            slot++;
        }

        gui.setItem(22, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));

        reporter.openInventory(gui);
    }

    private void openReasonGUI(Player reporter, Player reported) {
        Inventory gui = createManaged(27, "REPORT_REASON:" + reported.getUniqueId(), reporter);

        gui.setItem(4, createItem(Material.BOOK,
                ChatColor.RED + "Report " + reported.getName()));

        gui.setItem(10, createItem(Material.IRON_SWORD,
                ChatColor.RED + "Hacking/Cheating",
                "§7Using hacks or exploits"));

        gui.setItem(11, createItem(Material.FIRE_CHARGE,
                ChatColor.RED + "Griefing",
                "§7Destroying builds"));

        gui.setItem(12, createItem(Material.OAK_SIGN,
                ChatColor.RED + "Spamming",
                "§7Excessive chat messages"));

        gui.setItem(13, createItem(Material.BARRIER,
                ChatColor.RED + "Inappropriate Content",
                "§7Offensive language or builds"));

        gui.setItem(14, createItem(Material.GOLD_INGOT,
                ChatColor.RED + "Scamming",
                "§7Trading scams"));

        gui.setItem(15, createItem(Material.RED_BED,
                ChatColor.RED + "Other",
                "§7Other rule violation"));

        gui.setItem(22, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));

        reporter.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory topInv = event.getView().getTopInventory();
        if (topInv == null || !managedInventories.contains(topInv)) return;

        if (event.getRawSlot() >= topInv.getSize()) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        event.setCancelled(true);

        String title = inventoryTitles.getOrDefault(topInv, "");

        try {
            if (title.equals("REPORTS_LIST")) {
                handleReportsListClick(player, item);
            } else if (title.equals("REPORT_PLAYER")) {
                handleReportPlayerClick(player, item);
            } else if (title.startsWith("REPORT_REASON:")) {
                handleReasonClick(player, item, title);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("ReportManager error: " + e.getMessage());
            player.closeInventory();
        }
    }

    private void handleReportsListClick(Player player, ItemStack item) {
        if (item.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (item.getType() == Material.PAPER && item.getItemMeta() != null) {
            if (!player.hasPermission("generators.admin")) {
                player.sendMessage(ChatColor.RED + "No permission!");
                player.closeInventory();
                return;
            }
            String reportId = null;
            for (String line : item.getItemMeta().getLore()) {
                String stripped = ChatColor.stripColor(line);
                if (stripped.startsWith("ID: ")) {
                    reportId = stripped.substring(4);
                    break;
                }
            }

            if (reportId != null) {
                for (Report report : reports) {
                    if (report.id.toString().startsWith(reportId) && report.status.equals("OPEN")) {
                        report.status = "RESOLVED";
                        saveReports();
                        player.sendMessage(ChatColor.GREEN + "Report resolved!");
                        openReportGUI(player);
                        break;
                    }
                }
            }
        }
    }

    private void handleReportPlayerClick(Player player, ItemStack item) {
        if (item.getType() == Material.ARROW) {
            player.closeInventory();
            return;
        }

        if (item.getType() == Material.PLAYER_HEAD && item.getItemMeta() != null) {
            String reportedName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            Player reported = Bukkit.getPlayer(reportedName);
            if (reported != null) {
                openReasonGUI(player, reported);
            }
        }
    }

    private void handleReasonClick(Player player, ItemStack item, String title) {
        if (item.getType() == Material.ARROW) {
            openReportPlayerGUI(player);
            return;
        }

        if (item.getType() == Material.BARRIER && item.getItemMeta() != null) {
            String reportedUuid = title.substring("REPORT_REASON:".length());
            Player reported = Bukkit.getPlayer(UUID.fromString(reportedUuid));
            if (reported != null) {
                String reason = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                createReport(player, reported, reason);
            }
            player.closeInventory();
            return;
        }

        if (item.getItemMeta() != null) {
            String reportedUuid = title.substring("REPORT_REASON:".length());
            Player reported = Bukkit.getPlayer(UUID.fromString(reportedUuid));
            if (reported != null) {
                String reason = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                createReport(player, reported, reason);
            }
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        if (topInv != null && managedInventories.contains(topInv)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        if (topInv != null) {
            managedInventories.remove(topInv);
            inventoryTitles.remove(topInv);
        }
    }

    private void loadReports() {
        if (!reportsFile.exists()) return;
        try {
            org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(reportsFile);
            List<Map<?, ?>> list = config.getMapList("reports");
            long sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
            for (Map<?, ?> map : list) {
                long timestamp = ((Number) map.get("timestamp")).longValue();
                String status = (String) map.get("status");
                if ("RESOLVED".equals(status) && timestamp < sevenDaysAgo) {
                    continue;
                }
                Report report = new Report(
                        UUID.fromString((String) map.get("id")),
                        UUID.fromString((String) map.get("reporter-uuid")),
                        (String) map.get("reporter-name"),
                        UUID.fromString((String) map.get("reported-uuid")),
                        (String) map.get("reported-name"),
                        (String) map.get("reason"),
                        timestamp,
                        status
                );
                reports.add(report);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load reports: " + e.getMessage());
        }
    }

    public void saveReports() {
        try {
            org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
            List<Map<String, Object>> list = new ArrayList<>();
            for (Report report : reports) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", report.id.toString());
                map.put("reporter-uuid", report.reporterUUID.toString());
                map.put("reporter-name", report.reporterName);
                map.put("reported-uuid", report.reportedUUID.toString());
                map.put("reported-name", report.reportedName);
                map.put("reason", report.reason);
                map.put("timestamp", report.timestamp);
                map.put("status", report.status);
                list.add(map);
            }
            config.set("reports", list);
            config.save(reportsFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save reports: " + e.getMessage());
        }
    }

    private Inventory createManaged(int size, String title, Player player) {
        Inventory gui = Bukkit.createInventory(null, size, title);
        managedInventories.add(gui);
        inventoryTitles.put(gui, title);
        return gui;
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm");
        return sdf.format(new Date(timestamp));
    }

    public static class Report {
        public final UUID id;
        public final UUID reporterUUID;
        public final String reporterName;
        public final UUID reportedUUID;
        public final String reportedName;
        public final String reason;
        public final long timestamp;
        public String status;

        public Report(UUID id, UUID reporterUUID, String reporterName, UUID reportedUUID, String reportedName,
                      String reason, long timestamp, String status) {
            this.id = id;
            this.reporterUUID = reporterUUID;
            this.reporterName = reporterName;
            this.reportedUUID = reportedUUID;
            this.reportedName = reportedName;
            this.reason = reason;
            this.timestamp = timestamp;
            this.status = status;
        }
    }
}
