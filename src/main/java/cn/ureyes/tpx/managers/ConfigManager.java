package cn.ureyes.tpx.managers;

import cn.ureyes.tpx.Tpx;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 配置管理器，负责加载和管理配置文件
 * 此类方法由ChatGPT生成 仅供参考
 */
public class ConfigManager {
    private final Tpx plugin;
    private FileConfiguration config;
    private String prefix;

    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public ConfigManager(Tpx plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * 加载配置文件
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        prefix = colorize(config.getString("prefix", "&eTheUrEyesServer &f&l>>> "));
    }

    /**
     * 获取消息前缀
     * @return 消息前缀
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * 获取配置文件中的消息并替换变量
     * @param path 配置路径
     * @param replacements 变量替换，格式为 {"变量名", "替换值"}
     * @return 格式化后的消息
     */
    public String getMessage(String path, String... replacements) {
        String message = config.getString("messages." + path, "消息未配置: " + path);
        message = prefix + message;
        
        // 替换变量
        if (replacements != null && replacements.length > 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                if (i + 1 < replacements.length) {
                    message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
                }
            }
        }
        
        return colorize(message);
    }

    /**
     * 获取配置文件中的消息（不添加前缀）并替换变量
     * @param path 配置路径
     * @param replacements 变量替换，格式为 {"变量名", "替换值"}
     * @return 格式化后的消息
     */
    public String getRawMessage(String path, String... replacements) {
        String message = config.getString("messages." + path, "消息未配置: " + path);
        
        // 替换变量
        if (replacements != null && replacements.length > 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                if (i + 1 < replacements.length) {
                    message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
                }
            }
        }
        
        return colorize(message);
    }

    /**
     * 获取传送请求超时时间（秒）
     * @return 超时时间
     */
    public int getRequestTimeout() {
        return config.getInt("settings.request-timeout", 60);
    }

    /**
     * 获取传送请求冷却时间（秒）
     * @return 冷却时间
     */
    public int getRequestCooldown() {
        return config.getInt("settings.request-cooldown", 30);
    }

    /**
     * 是否启用点击接受/拒绝按钮
     * @return 是否启用
     */
    public boolean isClickButtonsEnabled() {
        return config.getBoolean("settings.enable-click-buttons", true);
    }

    /**
     * 获取传送延迟时间（秒）
     * @return 延迟时间
     */
    public int getTeleportDelay() {
        return config.getInt("settings.teleport-delay", 3);
    }

    /**
     * 将颜色代码转换为Minecraft颜色
     * @param text 包含颜色代码的文本
     * @return 转换后的文本
     */
    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
