package org.Little100.anvil_Fixer;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;
import java.util.logging.Logger;

public class PlayerAnvilInteractionListener implements Listener {

    private Anvil_Fixer plugin;
    private FileConfiguration config;
    private final Logger logger;
    private Map<UUID, Long> lastInteractTime = new HashMap<>();

    // 存储玩家上次修复的时间
    private final Map<Player, Long> cooldownMap = new HashMap<>();

    // 如果要设置冷却时间，请修改这里 单位毫秒
    private static final long COOLDOWN_TIME = 0L;

    public PlayerAnvilInteractionListener(Anvil_Fixer plugin,FileConfiguration config, Logger logger) {
        this.plugin = plugin;
        this.config = config;
        this.logger = logger;
        logFullConfiguration();
        initializeConfig();
    }

    private void initializeConfig() {
        boolean isEnabled = config.getBoolean("Anvil_Fixer.enable", true);
    }

    public void updateConfig(FileConfiguration newConfig) {
        this.config = newConfig;
    }

    private void logFullConfiguration() {
        ConfigurationSection itemsSection = config.getConfigurationSection("Anvil_Fixer.Item");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // 防止触发双次判定
        if (lastInteractTime.containsKey(playerId)) {
            long lastTiome = lastInteractTime.get(playerId);
            if (currentTime - lastTiome < 50) {
                event.setCancelled(true);
                return;
            }
        }

        lastInteractTime.put(playerId, currentTime);

        Block block = event.getClickedBlock();
        ItemStack item = player.getInventory().getItemInMainHand();

        // 判断冷却时间
        if (block != null && isAnvil(block.getType())) {
            // 判断冷却时间
            if (isInCooldown(player)) {
                player.sendMessage(ChatColor.RED + "请等待冷却时间再进行修复！");
                return; // 如果在冷却时间内，停止继续执行
            }
        }

        // 设置玩家当前时间为修复时间
        setCooldown(player);

        if (block == null || !isAnvil(block.getType()) || !player.isSneaking()) {
            return;
        }

        if (item.getType() == Material.AIR) {
            return; // 除去空手的情况
        }

        // 获取完整的物品ID
        String itemId = "minecraft:" + item.getType().name().toLowerCase(); // 构建完整的物品ID

        // 修改后的isValidRepairItem调用，使用完整的ID
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

        // 使用完整的物品ID来获取项配置
        ConfigurationSection itemConfig = getItemConfig(itemId);

        if (itemConfig == null) {
            player.sendMessage("§c物品配置未找到，无法修复。(item config not found, can not repair.)");
            return; // 处理未找到配置的情况
        }

        double chance = itemConfig.getDouble("chance", 0.0);
        int maxLevel = itemConfig.getInt("Fix_level.Max.level", 1);
        int minLevel = itemConfig.getInt("Fix_level.Min.level", 1);

        if (maxLevel <= 0 || minLevel <= 0) {
            player.sendMessage("§c修复等级配置无效，无法修复铁砧。(repair level config is invalid, can not repair anvil.)");
            return;
        }

        double maxLevelChance = itemConfig.getDouble("Fix_level.Max.chance", 0.0);
        double minLevelChance = itemConfig.getDouble("Fix_level.Min.chance", 0.0);


        Random random = new Random();
        double roll = random.nextDouble();

        boolean repairSuccessful = roll < chance;
        boolean levelCheckPassed = false;

        if (repairSuccessful) {
            double levelRoll = random.nextDouble();
            int repairAmount = 0;
            if (levelRoll < maxLevelChance) {
                repairAmount = maxLevel;
            } else if (levelRoll < maxLevelChance + minLevelChance) {
                repairAmount = minLevel;
            }

            int targetLevel = Math.max(0, currentLevel - repairAmount);
            levelCheckPassed = targetLevel < currentLevel;

            if (levelCheckPassed) {
                String currentName = currentLevel == 2 ? "损坏的铁砧(damaged anvil)" : "开裂的铁砧(chipped anvil)";
                String targetName = targetLevel == 1 ? "开裂的铁砧(chipped anvil)" : "完整的铁砧(anvil)";
                int repairLevel = currentLevel - targetLevel;

                player.sendMessage("§a修复成功！铁砧从状态 " + currentName + " 修复到状态 " + targetName + " 修复了 " + repairLevel + " 级!");
                repairAnvil(block, targetLevel);
                item.setAmount(item.getAmount() - 1);
                successRepair(player, Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
            }
        } else {
            player.sendMessage("§e修复失败，概率未达到要求。(repair failed due to initial chance check)");
            item.setAmount(item.getAmount() - 1);
            failRepair(player, Sound.BLOCK_ANVIL_DESTROY, 1.0f, 1.0f);
        }
    }

    // 检查玩家是否在冷却时间内
    private boolean isInCooldown(Player player) {
        long lastCooldown = cooldownMap.getOrDefault(player, 0L);
        return (System.currentTimeMillis() - lastCooldown) < COOLDOWN_TIME;
    }

    // 设置玩家的冷却时间
    private void setCooldown(Player player) {
        cooldownMap.put(player, System.currentTimeMillis());
    }

    private ConfigurationSection getItemConfig(String itemId) {
        ConfigurationSection itemsSection = config.getConfigurationSection("Anvil_Fixer.Item");
        if (itemsSection == null) {
            return null;
        }

        for (String key : itemsSection.getKeys(false)) {
            String id = itemsSection.getString(key + ".id");
            if (id != null && id.equalsIgnoreCase(itemId)) {
                ConfigurationSection itemConfig = itemsSection.getConfigurationSection(key);
                if (itemConfig == null) {
                    return null;
                }

                if (!itemConfig.contains("Fix_level.Max.level") || !itemConfig.contains("Fix_level.Min.level")) {
                    return null;
                }

                return itemConfig; // 返回找到的配置
            }
        }

        return null;
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
        String itemIdToCheck = "minecraft:" + material.name().toLowerCase();  // 形成完整的ID

        ConfigurationSection itemsSection = config.getConfigurationSection("Anvil_Fixer.Item");

        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                String id = itemsSection.getString(key + ".id");
                boolean enabled = itemsSection.getBoolean(key + ".enable");
                // 比较 ID 和启用状态
                if (id != null && id.equalsIgnoreCase(itemIdToCheck) && enabled) {
                    return true;
                }
            }
        }

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
                    item.addExtra("\n");
                    item.addExtra("[");
                    item.addExtra(ChatColor.DARK_GREEN + name + ChatColor.RESET);
                    item.addExtra("][");
                    item.addExtra(ChatColor.GRAY + "id:" + id + ChatColor.RESET);
                    item.addExtra("][");
                    item.addExtra(ChatColor.AQUA + "概率:" + String.format("%.2f", chance * 100) + "%" + ChatColor.RESET);
                    item.addExtra("]");
                    item.addExtra("\n");

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
                    item.addExtra("\n");
                    item.addExtra(ChatColor.DARK_GREEN + name + ChatColor.RESET);
                    item.addExtra("[");
                    item.addExtra(ChatColor.GRAY + "id:" + id + ChatColor.RESET);
                    item.addExtra("][");
                    item.addExtra(ChatColor.AQUA + "Chance:" + String.format("%.2f", chance * 100) + "%" + ChatColor.RESET);
                    item.addExtra("]");
                    item.addExtra("\n");

                    supportedItems.add(item);
                }
            }
        }
        return supportedItems;
    }

    private static final int ITEMS_PER_PAGE = 5; // 每页显示的物品数量

    private List<TextComponent> getSupportedItemsForPage(int page) {
        List<TextComponent> supportedItems = new ArrayList<>();
        ConfigurationSection itemsSection = config.getConfigurationSection("Anvil_Fixer.Item");
        if (itemsSection != null) {
            List<TextComponent> allItems = new ArrayList<>();

            // 获取所有支持的物品
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null && itemSection.getBoolean("enable", false)) {
                    String name = itemSection.getString("tips.name", "");
                    String id = itemSection.getString("id", "");
                    double chance = itemSection.getDouble("chance", 0.0);

                    TextComponent item = new TextComponent();
                    item.addExtra("\n");
                    item.addExtra("[" + ChatColor.DARK_GREEN + name + ChatColor.RESET + "] ");
                    item.addExtra("[" + ChatColor.GRAY + "id:" + id + ChatColor.RESET + "] ");
                    item.addExtra("[" + ChatColor.AQUA + "Chance:" + String.format("%.2f", chance * 100) + "%" + ChatColor.RESET + "]");
                    item.addExtra("\n");

                    allItems.add(item);
                }
            }

            // 计算分页
            int startIndex = (page - 1) * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allItems.size());

            // 获取当前页的数据
            for (int i = startIndex; i < endIndex; i++) {
                supportedItems.add(allItems.get(i));
            }
        }

        return supportedItems;
    }

    private void sendPagination(Player player, int currentPage) {
        List<TextComponent> supportedItems = getSupportedItemsForPage(currentPage);

        // 显示物品信息
        for (TextComponent item : supportedItems) {
            player.spigot().sendMessage(item);
        }

        // 添加分页按钮
        TextComponent previousPage = new TextComponent("[上一页](Previous page)");
        previousPage.setColor(ChatColor.YELLOW);
        previousPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/anvil_fixer_page " + (currentPage - 1)));
        previousPage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("点击查看上一页").color(ChatColor.GRAY).create()));

        TextComponent nextPage = new TextComponent("[下一页](Next page)");
        nextPage.setColor(ChatColor.YELLOW);
        nextPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/anvil_fixer_page " + (currentPage + 1)));
        nextPage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("点击查看下一页").color(ChatColor.GRAY).create()));

        player.spigot().sendMessage(previousPage);
        player.spigot().sendMessage(nextPage);
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage();

        if (command.startsWith("/anvil_fixer_page")) {
            String[] args = command.split(" ");
            if (args.length < 2) {
                return;
            }

            try {
                int page = Integer.parseInt(args[1]);
                if (page < 1) {
                    page = 1; // 页面最小为1
                }

                sendPagination(event.getPlayer(), page);
            } catch (NumberFormatException e) {
                event.getPlayer().sendMessage(ChatColor.RED + "无效的页面号码！");
            }
        }
    }


    private static void failRepair(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, volume, pitch);
    }

    private static void successRepair(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, volume, pitch);
    }
}
