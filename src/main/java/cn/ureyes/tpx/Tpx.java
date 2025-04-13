package cn.ureyes.tpx;

import cn.ureyes.tpx.commands.TeleportCommand;
import cn.ureyes.tpx.managers.ConfigManager;
import cn.ureyes.tpx.managers.TeleportManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class Tpx extends JavaPlugin {
    private ConfigManager configManager;
    private TeleportManager teleportManager;

    @Override
    public void onEnable() {
        //init
        configManager = new ConfigManager(this);
        teleportManager = new TeleportManager(this, configManager);
        registerCommands();


        getLogger().info("传送插件已启用！");
    }

    @Override
    public void onDisable() {
        if (teleportManager != null) {
            teleportManager.clearRequests();
        }
        
        getLogger().info("传送插件已禁用！");
    }
    

    private void registerCommands() {
        try {
            TeleportCommand teleportCommand = new TeleportCommand(this, configManager, teleportManager);
            getCommand("chuansong").setExecutor(teleportCommand);
            getCommand("chuansong").setTabCompleter(teleportCommand);
            getCommand("cs").setExecutor(teleportCommand);
            getCommand("cs").setTabCompleter(teleportCommand);
            
            getLogger().info("命令注册成功！");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "注册命令时发生错误: " + e.getMessage(), e);
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
    public TeleportManager getTeleportManager() {
        return teleportManager;
    }
}
