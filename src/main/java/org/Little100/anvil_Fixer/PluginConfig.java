package org.Little100.anvil_Fixer;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PluginConfig {
    private Anvil_Fixer plugin;
    private FileConfiguration config;
    private boolean enabled;

    // 添加更多配置项，反映插件的实际配置需求
    private List<RepairItemConfig> repairItems;

    private Random random;

    public PluginConfig(Anvil_Fixer plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.repairItems = new ArrayList<>();
        loadConfig();
    }

    public void loadConfig() {
        // 确保默认配置文件存在
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        // 重新获取最新的配置
        this.config = plugin.getConfig();

        // 加载基本设置
        this.enabled = config.getBoolean("Anvil_Fixer.enable", true);

        // 加载可修复物品列表
        loadRepairItems();
    }

    private void loadRepairItems() {
        // 清空之前的配置
        this.repairItems.clear();

        // 从配置文件加载可修复物品
        ConfigurationSection itemsSection = config.getConfigurationSection("Anvil_Fixer.Item");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemConfig = itemsSection.getConfigurationSection(key);
                if (itemConfig != null && itemConfig.getBoolean("enable", false)) {
                    RepairItemConfig item = new RepairItemConfig(
                            itemConfig.getString("id"),
                            itemConfig.getString("tips.name"),
                            itemConfig.getDouble("chance", 0.0),
                            itemConfig.getConfigurationSection("Fix_level")
                    );
                    repairItems.add(item);
                }
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<RepairItemConfig> getRepairItems() {
        return repairItems;
    }

    // 内部类，用于存储每个可修复物品的详细配置
    public static class RepairItemConfig {
        private String id;
        private String name;
        private double chance;
        private ConfigurationSection fixLevelConfig;

        public RepairItemConfig(String id, String name, double chance, ConfigurationSection fixLevelConfig) {
            this.id = id;
            this.name = name;
            this.chance = chance;
            this.fixLevelConfig = fixLevelConfig;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public double getChance() {
            return chance;
        }

        public ConfigurationSection getFixLevelConfig() {
            return fixLevelConfig;
        }
    }

    // 可以添加其他实用方法
    public String translateColorCodes(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}