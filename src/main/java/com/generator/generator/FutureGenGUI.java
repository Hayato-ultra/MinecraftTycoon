package com.generator.generator;

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

import java.util.*;

public class FutureGenGUI implements Listener {

    private final GeneratorPlugin plugin;
    private final Set<Inventory> managedInventories = Collections.synchronizedSet(new HashSet<>());
    private final Map<Inventory, String> inventoryTitles = Collections.synchronizedMap(new HashMap<>());

    public FutureGenGUI(GeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    public void openFutureGenMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54,
                net.kyori.adventure.text.Component.text("Future Generators"));
        managedInventories.add(gui);
        inventoryTitles.put(gui, "FUTURE_MAIN");

        gui.setItem(10, createItem(Material.BLAZE_POWDER, ChatColor.GOLD + "Fusion",
                "", "§7Combine generators to create", "§7more powerful versions!", "", "§eClick to view recipes"));
        gui.setItem(12, createItem(Material.NETHER_STAR, ChatColor.AQUA + "Evolution",
                "", "§7Evolve your generators through", "§7stages for massive bonuses!", "", "§eClick to view tree"));
        gui.setItem(14, createItem(Material.ENCHANTED_GOLDEN_APPLE, ChatColor.LIGHT_PURPLE + "Rare & Secret",
                "", "§7Discover secret generators", "§7with unique abilities!", "", "§eClick to browse"));
        gui.setItem(16, createItem(Material.EXPERIENCE_BOTTLE, ChatColor.GREEN + "Mastery",
                "", "§7Level up generators through", "§7use for permanent bonuses!", "", "§eClick to view mastery"));
        gui.setItem(22, createItem(Material.ANVIL, ChatColor.YELLOW + "Specializations",
                "", "§7Specialize generators for", "§7specific production bonuses!", "", "§eClick to specialize"));
        gui.setItem(31, createItem(Material.BEACON, ChatColor.RED + "Advanced Production",
                "", "§7View all active multipliers", "§7and bonuses on your generators!", "", "§eClick to view stats"));

        gui.setItem(49, createItem(Material.BARRIER, ChatColor.RED + "Close"));

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    public void openFusionGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54,
                net.kyori.adventure.text.Component.text("Fusion Recipes"));
        managedInventories.add(gui);
        inventoryTitles.put(gui, "FUSION");

        FutureGenManager futureGen = plugin.getFutureGenManager();
        Map<String, FutureGenManager.FusionRecipe> recipes = futureGen.getFusionRecipes();

        int slot = 10;
        for (FutureGenManager.FusionRecipe recipe : recipes.values()) {
            if (slot > 44) break;
            if (slot % 9 == 8) slot += 2;

            boolean canFuse = futureGen.canFuse(player, recipe.id);
            ChatColor color = canFuse ? ChatColor.GREEN : ChatColor.RED;

            gui.setItem(slot, createItem(recipe.resultMaterial,
                    color + recipe.name,
                    "",
                    "§7" + recipe.description,
                    "",
                    "§7Ingredients:",
                    "§8- §f" + recipe.ingredient1Amount + "x " + recipe.ingredient1,
                    "§8- §f" + recipe.ingredient2Amount + "x " + recipe.ingredient2,
                    "§7Cost: §f" + (int) recipe.coinCost + " coins",
                    recipe.requiredPrestige > 0 ? "§7Required Prestige: §f" + recipe.requiredPrestige : "§7Required Prestige: §fNone",
                    "",
                    canFuse ? "§a§lCLICK TO FUSE" : "§c§lCANNOT FUSE"
            ));
            slot++;
        }

        gui.setItem(49, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));
        player.openInventory(gui);
    }

    public void openEvolutionGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54,
                net.kyori.adventure.text.Component.text("Evolution Tree"));
        managedInventories.add(gui);
        inventoryTitles.put(gui, "EVOLUTION");

        FutureGenManager futureGen = plugin.getFutureGenManager();
        Map<String, FutureGenManager.EvolutionData> tree = futureGen.getEvolutionTree();

        int slot = 10;
        for (Map.Entry<String, FutureGenManager.EvolutionData> entry : tree.entrySet()) {
            if (slot > 44) break;
            if (slot % 9 == 8) slot += 2;

            String genType = entry.getKey();
            FutureGenManager.EvolutionData evo = entry.getValue();
            int currentStage = futureGen.getEvolutionStage(player.getUniqueId(), genType);
            boolean canEvolve = futureGen.canEvolve(player, genType);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Current Stage: §f" + currentStage + "/" + evo.stages.size());
            lore.add("");

            for (FutureGenManager.EvolutionData.EvolutionStage stage : evo.stages) {
                ChatColor stageColor = stage.stage <= currentStage ? ChatColor.GREEN :
                        (stage.stage == currentStage + 1 && canEvolve ? ChatColor.YELLOW : ChatColor.GRAY);
                lore.add(stageColor + "Stage " + stage.stage + ": " + stage.ability);
                if (stage.stage > currentStage) {
                    lore.add("  §8- §7Mastery Required: §f" + stage.requiredMastery);
                    lore.add("  §8- §7Cost: §f" + (int) stage.coinCost + " coins");
                } else {
                    lore.add("  §8- §aUNLOCKED");
                }
            }

            gui.setItem(slot, createItem(Material.NETHER_STAR,
                    ChatColor.AQUA + genType.substring(0, 1).toUpperCase() + genType.substring(1) + " Evolution",
                    lore.toArray(new String[0])));
            slot++;
        }

        gui.setItem(49, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));
        player.openInventory(gui);
    }

    public void openSecretsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54,
                net.kyori.adventure.text.Component.text("Secret Generators"));
        managedInventories.add(gui);
        inventoryTitles.put(gui, "SECRETS");

        FutureGenManager futureGen = plugin.getFutureGenManager();
        Map<String, FutureGenManager.SecretGenData> secrets = futureGen.getSecretGenerators();

        int slot = 10;
        for (FutureGenManager.SecretGenData secret : secrets.values()) {
            if (slot > 44) break;
            if (slot % 9 == 8) slot += 2;

            boolean unlocked = futureGen.isSecretUnlocked(player.getUniqueId(), secret.id);
            boolean canUnlock = futureGen.canUnlockSecret(player, secret.id);

            gui.setItem(slot, createItem(secret.material,
                    (unlocked ? "§a§l" : "§5§l") + secret.name,
                    secret.description.toArray(new String[0])));
            slot++;
        }

        gui.setItem(49, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));
        player.openInventory(gui);
    }

    public void openMasteryGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54,
                net.kyori.adventure.text.Component.text("Generator Mastery"));
        managedInventories.add(gui);
        inventoryTitles.put(gui, "MASTERY");

        FutureGenManager futureGen = plugin.getFutureGenManager();

        gui.setItem(10, createItem(Material.STONE, ChatColor.WHITE + "Basic Generator",
                "", "§7Mastery: §f" + futureGen.getPlayerMastery(player.getUniqueId(), "basic") + "/100",
                "§7Level: §f" + futureGen.getMasteryLevel(player.getUniqueId(), "basic"),
                "§7Bonus: §a+" + ((int)(futureGen.getMasteryBonus(player.getUniqueId(), "basic") * 100 - 100)) + "%"));
        gui.setItem(11, createItem(Material.IRON_ORE, ChatColor.GRAY + "Advanced Generator",
                "", "§7Mastery: §f" + futureGen.getPlayerMastery(player.getUniqueId(), "advanced") + "/100",
                "§7Level: §f" + futureGen.getMasteryLevel(player.getUniqueId(), "advanced"),
                "§7Bonus: §a+" + ((int)(futureGen.getMasteryBonus(player.getUniqueId(), "advanced") * 100 - 100)) + "%"));
        gui.setItem(12, createItem(Material.DIAMOND_ORE, ChatColor.AQUA + "Diamond Generator",
                "", "§7Mastery: §f" + futureGen.getPlayerMastery(player.getUniqueId(), "diamond") + "/100",
                "§7Level: §f" + futureGen.getMasteryLevel(player.getUniqueId(), "diamond"),
                "§7Bonus: §a+" + ((int)(futureGen.getMasteryBonus(player.getUniqueId(), "diamond") * 100 - 100)) + "%"));
        gui.setItem(13, createItem(Material.NETHERITE_BLOCK, ChatColor.DARK_RED + "Netherite Generator",
                "", "§7Mastery: §f" + futureGen.getPlayerMastery(player.getUniqueId(), "netherite") + "/100",
                "§7Level: §f" + futureGen.getMasteryLevel(player.getUniqueId(), "netherite"),
                "§7Bonus: §a+" + ((int)(futureGen.getMasteryBonus(player.getUniqueId(), "netherite") * 100 - 100)) + "%"));
        gui.setItem(14, createItem(Material.GOLD_ORE, ChatColor.GOLD + "Gold Generator",
                "", "§7Mastery: §f" + futureGen.getPlayerMastery(player.getUniqueId(), "gold") + "/100",
                "§7Level: §f" + futureGen.getMasteryLevel(player.getUniqueId(), "gold"),
                "§7Bonus: §a+" + ((int)(futureGen.getMasteryBonus(player.getUniqueId(), "gold") * 100 - 100)) + "%"));
        gui.setItem(15, createItem(Material.LAPIS_ORE, ChatColor.BLUE + "Lapis Generator",
                "", "§7Mastery: §f" + futureGen.getPlayerMastery(player.getUniqueId(), "lapis") + "/100",
                "§7Level: §f" + futureGen.getMasteryLevel(player.getUniqueId(), "lapis"),
                "§7Bonus: §a+" + ((int)(futureGen.getMasteryBonus(player.getUniqueId(), "lapis") * 100 - 100)) + "%"));
        gui.setItem(16, createItem(Material.REDSTONE_ORE, ChatColor.RED + "Redstone Generator",
                "", "§7Mastery: §f" + futureGen.getPlayerMastery(player.getUniqueId(), "redstone") + "/100",
                "§7Level: §f" + futureGen.getMasteryLevel(player.getUniqueId(), "redstone"),
                "§7Bonus: §a+" + ((int)(futureGen.getMasteryBonus(player.getUniqueId(), "redstone") * 100 - 100)) + "%"));
        gui.setItem(17, createItem(Material.EMERALD_ORE, ChatColor.GREEN + "Emerald Generator",
                "", "§7Mastery: §f" + futureGen.getPlayerMastery(player.getUniqueId(), "emerald") + "/100",
                "§7Level: §f" + futureGen.getMasteryLevel(player.getUniqueId(), "emerald"),
                "§7Bonus: §a+" + ((int)(futureGen.getMasteryBonus(player.getUniqueId(), "emerald") * 100 - 100)) + "%"));

        gui.setItem(31, createItem(Material.PAPER, ChatColor.YELLOW + "Mastery Info",
                "", "§7Mastery levels are earned by",
                "§7collecting from generators.",
                "§7Each level gives +5% production bonus.",
                "§7100 mastery = 1 level"));

        gui.setItem(49, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));
        player.openInventory(gui);
    }

    public void openSpecializationGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54,
                net.kyori.adventure.text.Component.text("Specializations"));
        managedInventories.add(gui);
        inventoryTitles.put(gui, "SPECIALIZATION");

        FutureGenManager futureGen = plugin.getFutureGenManager();

        String[] generators = {"basic", "advanced", "diamond", "netherite", "gold", "lapis", "redstone", "emerald"};
        int slot = 10;
        for (String gen : generators) {
            if (slot > 44) break;
            if (slot % 9 == 8) slot += 2;

            String currentSpec = futureGen.getSpecialization(player.getUniqueId(), gen);
            String specDisplay = currentSpec != null ?
                    currentSpec.substring(0, 1).toUpperCase() + currentSpec.substring(1) : "None";

            gui.setItem(slot, createItem(Material.ANVIL,
                    ChatColor.AQUA + gen.substring(0, 1).toUpperCase() + gen.substring(1),
                    "",
                    "§7Current Specialization: §f" + specDisplay,
                    "",
                    "§eClick to change specialization"));
            slot++;
        }

        gui.setItem(49, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));
        player.openInventory(gui);
    }

    public void openSpecializationSelectGUI(Player player, String generatorType) {
        Inventory gui = Bukkit.createInventory(null, 27,
                net.kyori.adventure.text.Component.text("Select Specialization: " + generatorType));
        managedInventories.add(gui);
        inventoryTitles.put(gui, "SPEC_SELECT:" + generatorType);

        FutureGenManager futureGen = plugin.getFutureGenManager();
        String currentSpec = futureGen.getSpecialization(player.getUniqueId(), generatorType);

        gui.setItem(10, createItem(Material.FEATHER,
                ("speed".equals(currentSpec) ? "§a" : "§f") + "Speed Specialization",
                "", "§7+25% production speed", "§7Faster resource generation"));
        gui.setItem(13, createItem(Material.DIAMOND,
                ("quantity".equals(currentSpec) ? "§a" : "§f") + "Quantity Specialization",
                "", "§7+50% resource output", "§7More resources per cycle"));
        gui.setItem(16, createItem(Material.CHEST,
                ("storage".equals(currentSpec) ? "§a" : "§f") + "Storage Specialization",
                "", "§7+100% storage capacity", "§7Store more before collecting"));

        gui.setItem(22, createItem(Material.BARRIER, ChatColor.RED + "Remove Specialization"));

        gui.setItem(18, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));
        player.openInventory(gui);
    }

    public void openProductionStatsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54,
                net.kyori.adventure.text.Component.text("Production Stats"));
        managedInventories.add(gui);
        inventoryTitles.put(gui, "PRODUCTION_STATS");

        FutureGenManager futureGen = plugin.getFutureGenManager();
        List<com.generator.generator.Generator> gens = plugin.getGeneratorManager().getPlayerGenerators(player.getUniqueId());

        int slot = 10;
        for (com.generator.generator.Generator gen : gens) {
            if (slot > 44) break;
            if (slot % 9 == 8) slot += 2;

            double mult = futureGen.getProductionMultiplier(player.getUniqueId(), gen.getType());
            int evoStage = futureGen.getEvolutionStage(player.getUniqueId(), gen.getType());
            int mastery = futureGen.getMasteryLevel(player.getUniqueId(), gen.getType());
            String spec = futureGen.getSpecialization(player.getUniqueId(), gen.getType());

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Production Multiplier: §a" + String.format("%.1f", mult) + "x");
            lore.add("§7Evolution Stage: §f" + evoStage);
            lore.add("§7Mastery Level: §f" + mastery);
            lore.add("§7Specialization: §f" + (spec != null ? spec : "None"));

            gui.setItem(slot, createItem(gen.getOutputMaterial(plugin.getConfigManager()),
                    ChatColor.GREEN + gen.getType() + " Generator",
                    lore.toArray(new String[0])));
            slot++;
        }

        gui.setItem(49, createItem(Material.ARROW, ChatColor.YELLOW + "Back"));
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory == null) return;
        String title = inventoryTitles.get(topInventory);
        if (title == null) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= topInventory.getSize()) return;

        switch (title) {
            case "FUTURE_MAIN" -> {
                switch (slot) {
                    case 10 -> openFusionGUI(player);
                    case 12 -> openEvolutionGUI(player);
                    case 14 -> openSecretsGUI(player);
                    case 16 -> openMasteryGUI(player);
                    case 22 -> openSpecializationGUI(player);
                    case 31 -> openProductionStatsGUI(player);
                    case 49 -> player.closeInventory();
                }
            }
            case "FUSION" -> {
                if (slot == 49) openFutureGenMenu(player);
                else if (slot >= 10 && slot <= 44) {
                    FutureGenManager futureGen = plugin.getFutureGenManager();
                    int recipeIndex = 0;
                    for (FutureGenManager.FusionRecipe recipe : futureGen.getFusionRecipes().values()) {
                        if (recipeIndex == slot - 10 || (recipeIndex == slot - 11 && slot % 9 == 0)) {
                            if (futureGen.canFuse(player, recipe.id)) {
                                futureGen.fuse(player, recipe.id);
                                openFusionGUI(player);
                            } else {
                                player.sendMessage(ChatColor.RED + "You cannot fuse this recipe!");
                            }
                            break;
                        }
                        recipeIndex++;
                    }
                }
            }
            case "EVOLUTION" -> {
                if (slot == 49) openFutureGenMenu(player);
                else if (slot >= 10 && slot <= 44) {
                    FutureGenManager futureGen = plugin.getFutureGenManager();
                    int evoIndex = 0;
                    for (String genType : futureGen.getEvolutionTree().keySet()) {
                        if (evoIndex == slot - 10 || (evoIndex == slot - 11 && slot % 9 == 0)) {
                            if (futureGen.canEvolve(player, genType)) {
                                futureGen.evolve(player, genType);
                                openEvolutionGUI(player);
                            } else {
                                player.sendMessage(ChatColor.RED + "You cannot evolve this generator yet!");
                            }
                            break;
                        }
                        evoIndex++;
                    }
                }
            }
            case "SECRETS" -> {
                if (slot == 49) openFutureGenMenu(player);
                else if (slot >= 10 && slot <= 44) {
                    FutureGenManager futureGen = plugin.getFutureGenManager();
                    int secretIndex = 0;
                    for (FutureGenManager.SecretGenData secret : futureGen.getSecretGenerators().values()) {
                        if (secretIndex == slot - 10 || (secretIndex == slot - 11 && slot % 9 == 0)) {
                            if (futureGen.isSecretUnlocked(player.getUniqueId(), secret.id)) {
                                player.sendMessage(ChatColor.GREEN + "You already have the " + secret.name + "!");
                            } else if (futureGen.canUnlockSecret(player, secret.id)) {
                                futureGen.unlockSecret(player, secret.id);
                                openSecretsGUI(player);
                            } else {
                                player.sendMessage(ChatColor.RED + "You cannot unlock this secret generator yet!");
                            }
                            break;
                        }
                        secretIndex++;
                    }
                }
            }
            case "MASTERY" -> {
                if (slot == 49) openFutureGenMenu(player);
            }
            case "SPECIALIZATION" -> {
                if (slot == 49) openFutureGenMenu(player);
                else if (slot >= 10 && slot <= 17) {
                    String[] generators = {"basic", "advanced", "diamond", "netherite", "gold", "lapis", "redstone", "emerald"};
                    int genIndex = slot - 10;
                    if (genIndex < generators.length) {
                        openSpecializationSelectGUI(player, generators[genIndex]);
                    }
                }
            }
            default -> {
                if (title.startsWith("SPEC_SELECT:")) {
                    String genType = title.substring("SPEC_SELECT:".length());
                    FutureGenManager futureGen = plugin.getFutureGenManager();
                    switch (slot) {
                        case 10 -> {
                            futureGen.setSpecialization(player.getUniqueId(), genType, "speed");
                            player.sendMessage(ChatColor.GREEN + "Set " + genType + " to Speed specialization!");
                            openSpecializationGUI(player);
                        }
                        case 13 -> {
                            futureGen.setSpecialization(player.getUniqueId(), genType, "quantity");
                            player.sendMessage(ChatColor.GREEN + "Set " + genType + " to Quantity specialization!");
                            openSpecializationGUI(player);
                        }
                        case 16 -> {
                            futureGen.setSpecialization(player.getUniqueId(), genType, "storage");
                            player.sendMessage(ChatColor.GREEN + "Set " + genType + " to Storage specialization!");
                            openSpecializationGUI(player);
                        }
                        case 22 -> {
                            futureGen.setSpecialization(player.getUniqueId(), genType, null);
                            player.sendMessage(ChatColor.YELLOW + "Removed specialization for " + genType + "!");
                            openSpecializationGUI(player);
                        }
                        case 18 -> openSpecializationGUI(player);
                    }
                }
            }
            case "PRODUCTION_STATS" -> {
                if (slot == 49) openFutureGenMenu(player);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (inventoryTitles.containsValue(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        managedInventories.remove(event.getInventory());
        inventoryTitles.remove(event.getInventory());
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
}
