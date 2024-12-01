package org.Little100.anvil_Fixer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class Anvil_Fixer extends JavaPlugin implements CommandExecutor {

    private PluginConfig config;
    private PlayerAnvilInteractionListener listener;

    @Override
    public void onEnable() {
        initializeConfig();
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml", false);
            saveDefaultConfig();  // 只在第一次启动时保存默认配置
        }
        File mdFile = new File(getDataFolder(), "config.md");
        if (!mdFile.exists()) {
            saveResource("config.md", false);
            saveDefaultConfig();  // 只在第一次启动时保存默认配置
        }
        super.reloadConfig();
        config = new PluginConfig(this);
        if (config == null) {
            getLogger().severe("Failed to load config file. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);  // 禁用插件
            return;
        } else {
            getLogger().info("Config loaded successfully.");
        }

        listener = new PlayerAnvilInteractionListener(this, getConfig(), getLogger());
        getServer().getPluginManager().registerEvents(listener, this);

        if (getCommand("anvil_fixer_english") != null) {
            getCommand("anvil_fixer_english").setExecutor(this);
        } else {
            getLogger().severe("Failed to register command 'anvil_fixer_english'. Please check plugin.yml!");
        }

        getLogger().info("" +
                "   __ _ _   _   _          _  ___   ___  \n" +
                "  / /(_) |_| |_| | ___    / |/ _ \\ / _ \\ \n" +
                " / / | | __| __| |/ _ \\   | | | | | | | |\n" +
                "/ /__| | |_| |_| |  __/   | | |_| | |_| |\n" +
                "\\____/_|\\__|\\__|_|\\___|___|_|\\___/ \\___/ \n" +
                "                     |_____|             ");
    }

    private void initializeConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        reloadConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("anvil_fixer_english")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.spigot().sendMessage(listener.getUnsupportedItemMessageEnglish());
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("anvil_fixer") || command.getName().equalsIgnoreCase("AF")) {
            if (!sender.hasPermission("anvil_fixer.reload") && !sender.hasPermission("AF.reload")) {
                sender.sendMessage("§c你没有权限使用此命令");
                return true;
            }
            if (command.getName().equalsIgnoreCase("anvil_fixer") || command.getName().equalsIgnoreCase("AF")) {
                if (!sender.hasPermission("anvil_fixer.reload") && !sender.hasPermission("AF.reload")) {
                    sender.sendMessage("§c你没有权限使用此命令");
                    return true;
                }
                if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                    // 重新加载配置
                    config.loadConfig();

                    // 更新监听器的配置
                    listener.updateConfig(getConfig());

                    sender.sendMessage("§aAnvil_Fixer 配置文件已重新加载！");
                    return true;
                } else {
                    sender.sendMessage("§c使用方法: /anvil_fixer reload 或 /AF reload");
                    return true;
                }
            }
            return false;
        }

        return false;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}