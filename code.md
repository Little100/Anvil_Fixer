package org.Little100.anvil_Fixer;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class PlayerAnvilInteractionListener implements Listener {

    private final FileConfiguration config;
    private final Logger logger;

    public PlayerAnvilInteractionListener(FileConfiguration config, Logger logger) {
        this.config = config;
        this.logger = logger;
        logFullConfiguration();
    }

    private void logFullConfiguration() {
        ConfigurationSection itemsSection = config.getConfigurationSection("Anvil_Fixer.Item");
        if (itemsSection == null) {
            logger.severe("Anvil_Fixer.Item section is missing in the config file.");
        } else {
            logger.info("Anvil_Fixer.Item section loaded successfully.");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null || !isAnvil(block.getType()) || !player.isSneaking()) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            return; // 除去空手的情况
        }

        if (!isValidRepairItem(item.getType())) {
            sendUnsupportedItemMessage(player);
            return;
        }

        Material anvilType = block.getType();
        int currentLevel = getAnvilStateLevel(anvilType);

        if (currentLevel <= 0 || currentLevel > 2) {
            player.sendMessage("§c满级铁砧不能被修复！(Max level anvil can not be repaired.)");
            return;
        }

        String itemName = item.getType().name().toLowerCase();
        ConfigurationSection itemConfig = getItemConfig(itemName);

        if (itemConfig == null) {
            logger.warning("No configuration found for item: " + itemName);
            player.sendMessage("§c该物品的配置不存在，无法修复铁砧。");
            return;
        }

        double chance = itemConfig.getDouble("chance", 0.0);
        int maxLevel = itemConfig.getInt("Fix_level.Max.level", 1);
        int minLevel = itemConfig.getInt("Fix_level.Min.level", 1);

        if (maxLevel <= 0 || minLevel <= 0) {
            logger.severe("Fix level configuration is invalid for item: " + itemName);
            player.sendMessage("§c修复等级配置无效，无法修复铁砧。");
            return;
        }

        double maxLevelChance = itemConfig.getDouble("Fix_level.Max.chance", 0.0);
        double minLevelChance = itemConfig.getDouble("Fix_level.Min.chance", 0.0);

        logger.info("Item: " + itemName + ", Chance: " + chance + ", MaxLevel: " + maxLevel + ", MinLevel: " + minLevel);
        logger.info("Current anvil level: " + currentLevel);

        Random random = new Random();
        double roll = random.nextDouble();
        logger.info("Random roll: " + roll + ", Required chance: " + chance);

        if (roll < chance) {
            double levelRoll = random.nextDouble();
            logger.info("Level roll: " + levelRoll);
            int repairAmount;
            if (levelRoll < maxLevelChance) {
                repairAmount = maxLevel;
                logger.info("Attempting max level repair: " + repairAmount);
            } else if (levelRoll < maxLevelChance + minLevelChance) {
                repairAmount = minLevel;
                logger.info("Attempting min level repair: " + repairAmount);
            } else {
                logger.info("Repair failed due to level chance");
                player.sendMessage("§e修复失败，概率未达到要求。");
                return;
            }

            int targetLevel = Math.max(0, currentLevel - repairAmount);
            logger.info("Target level after repair: " + targetLevel);

            if (targetLevel < currentLevel) {
                logger.info("Repair successful. New level: " + targetLevel);
                player.sendMessage("§a修复成功！铁砧从状态 " + currentLevel + " 修复到状态 " + targetLevel);
                repairAnvil(block, targetLevel);
                item.setAmount(item.getAmount() - 1);
            } else {
                logger.info("Repair failed. Target level not lower than current level");
                player.sendMessage("§e修复失败，修复等级不足。");
            }
        } else {
            logger.info("Repair failed due to initial chance check");
            player.sendMessage("§e修复失败，概率未达到要求。");
        }
    }

    private ConfigurationSection getItemConfig(String itemName) {
        ConfigurationSection itemsSection = config.getConfigurationSection("Anvil_Fixer.Item");
        if (itemsSection == null) {
            logger.severe("Anvil_Fixer.Item section is missing from config.");
            return null;
        }

        itemName = itemName.toLowerCase();
        logger.info("Looking for item config: " + itemName); // Log itemName

        ConfigurationSection itemConfig = itemsSection.getConfigurationSection(itemName);
        if (itemConfig == null) {
            logger.warning("No configuration found for item: " + itemName);
            return null;
        }

        // Check for missing configuration items
        if (!itemConfig.contains("Fix_level.Max.level") || !itemConfig.contains("Fix_level.Min.level")) {
            logger.severe("Missing required configuration for item: " + itemName);
            return null;
        }

        return itemConfig;
    }

    private boolean isAnvil(Material material) {
        return material == Material.ANVIL || material == Material.CHIPPED_ANVIL || material == Material.DAMAGED_ANVIL;
    }

    private int getAnvilStateLevel(Material anvilType) {
        switch (anvilType) {
            case ANVIL: return 0;
            case CHIPPED_ANVIL: return 1;
            case DAMAGED_ANVIL: return 2;
            default: return -1;
        }
    }

    private boolean isValidRepairItem(Material material) {
        String itemId = material.name().toLowerCase();
        logger.info("Checking if " + itemId + " is a valid repair item");
        ConfigurationSection itemsSection = config.getConfigurationSection("Anvil_Fixer.Item");

        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null && itemSection.getBoolean("enable", false)) {
                    String id = itemSection.getString("id");
                    logger.info("Checking against config item: " + key + ", ID: " + id + ", Enabled: " + itemSection.getBoolean("enable", false));

                    if (id != null && id.equalsIgnoreCase("minecraft:" + itemId) && itemSection.getBoolean("enable", false)) {
                        logger.info(itemId + " is a valid repair item");
                        return true;
                    }
                }
            }
        }
        logger.info(itemId + " is not a valid repair item");
        return false;
    }

    private void repairAnvil(Block block, int targetLevel) {
        Material newMaterial;
        switch (targetLevel) {
            case 0: newMaterial = Material.ANVIL; break;
            case 1: newMaterial = Material.CHIPPED_ANVIL; break;
            case 2: newMaterial = Material.DAMAGED_ANVIL; break;
            default: return;
        }
        block.setType(newMaterial);
    }

    private void sendUnsupportedItemMessage(Player player) {
        player.spigot().sendMessage(getUnsupportedItemMessage());

        TextComponent englishOption = new TextComponent("[Do you need to read the English version? Click here.]");
        englishOption.setColor(ChatColor.YELLOW);
        englishOption.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/anvil_fixer_english"));
        englishOption.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Click to see the English version").color(ChatColor.GRAY).create()));

        player.spigot().sendMessage(englishOption);
    }

    private TextComponent getUnsupportedItemMessage() {
        TextComponent message = new TextComponent(ChatColor.DARK_RED + "您手中的物品不支持修复铁砧！" + ChatColor.RESET + "支持的物品有");
        List<TextComponent> supportedItems = getSupportedItems();
        for (int i = 0; i < supportedItems.size(); i++) {
            message.addExtra(supportedItems.get(i));
            if (i < supportedItems.size() - 1) {
                message.addExtra(", ");
            }
        }
        return message;
    }

    private List<TextComponent> getSupportedItems() {
        List<TextComponent> supportedItems = new ArrayList<>();
        ConfigurationSection itemsSection = config.getConfigurationSection("Anvil_Fixer.Item");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null && itemSection.getBoolean("enable", false)) {
                    String name = itemSection.getString("tips.name", "");
                    String id = itemSection.getString("id", "");
                    double chance = itemSection.getDouble("chance", 0.0);

                    TextComponent item = new TextComponent();
                    item.addExtra("[");
                    item.addExtra(ChatColor.DARK_GREEN + name + ChatColor.RESET);
                    item.addExtra("][");
                    item.addExtra(ChatColor.GRAY + "id:" + id + ChatColor.RESET);
                    item.addExtra("][");
                    item.addExtra(ChatColor.AQUA + "概率:" + String.format("%.2f", chance * 100) + "%" + ChatColor.RESET);
                    item.addExtra("]");

                    supportedItems.add(item);
                }
            }
        }
        return supportedItems;
    }

    public TextComponent getUnsupportedItemMessageEnglish() {
        TextComponent message = new TextComponent(ChatColor.DARK_RED + "The item in your hand cannot be used to repair anvils!" + ChatColor.RESET + " Supported items are: ");
        List<TextComponent> supportedItems = getSupportedItemsEnglish();
        for (int i = 0; i < supportedItems.size(); i++) {
            message.addExtra(supportedItems.get(i));
            if (i < supportedItems.size() - 1) {
                message.addExtra(", ");
            }
        }
        return message;
    }

    private List<TextComponent> getSupportedItemsEnglish() {
        List<TextComponent> supportedItems = new ArrayList<>();
        ConfigurationSection itemsSection = config.getConfigurationSection("Anvil_Fixer.Item");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null && itemSection.getBoolean("enable", false)) {
                    String name = itemSection.getString("tips.name", "");
                    String id = itemSection.getString("id", "");
                    double chance = itemSection.getDouble("chance", 0.0);

                    TextComponent item = new TextComponent();
                    item.addExtra(ChatColor.DARK_GREEN + name + ChatColor.RESET);
                    item.addExtra("[");
                    item.addExtra(ChatColor.GRAY + "id:" + id + ChatColor.RESET);
                    item.addExtra("][");
                    item.addExtra(ChatColor.AQUA + "Chance:" + String.format("%.2f", chance * 100) + "%" + ChatColor.RESET);
                    item.addExtra("]");

                    supportedItems.add(item);
                }
            }
        }
        return supportedItems;
    }
}