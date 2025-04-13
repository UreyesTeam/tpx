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
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        
        // 初始化传送管理器
        teleportManager = new TeleportManager(this, configManager);
        
        // 注册命令
        registerCommands();
        
        getLogger().info("TPX传送插件已启用！");
    }

    @Override
    public void onDisable() {
        // 清理传送请求
        if (teleportManager != null) {
            teleportManager.clearRequests();
        }
        
        getLogger().info("TPX传送插件已禁用！");
    }
    
    /**
     * 注册命令
     */
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
    
    /**
     * 获取配置管理器
     * @return 配置管理器
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * 获取传送管理器
     * @return 传送管理器
     */
    public TeleportManager getTeleportManager() {
        return teleportManager;
    }
}
