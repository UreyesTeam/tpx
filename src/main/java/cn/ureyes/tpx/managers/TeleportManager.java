package cn.ureyes.tpx.managers;

import cn.ureyes.tpx.Tpx;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 传送请求管理器，负责管理传送请求
 */
public class TeleportManager {
    private final Tpx plugin;
    private final ConfigManager configManager;
    
    // 存储传送请求：key=接收者UUID, value=发送者UUID
    private final Map<UUID, UUID> teleportRequests = new ConcurrentHashMap<>();
    
    // 存储请求过期任务：key=接收者UUID, value=任务ID
    private final Map<UUID, BukkitTask> requestTasks = new ConcurrentHashMap<>();
    
    // 存储冷却时间：key=玩家UUID, value=冷却结束时间
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    /**
     * 构造函数
     * @param plugin 插件实例
     * @param configManager 配置管理器
     */
    public TeleportManager(Tpx plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * 发送传送请求
     * @param sender 发送者
     * @param target 接收者
     * @return 是否成功发送请求
     */
    public boolean sendTeleportRequest(Player sender, Player target) {
        UUID senderUUID = sender.getUniqueId();
        UUID targetUUID = target.getUniqueId();
        
        // 检查是否是自己
        if (senderUUID.equals(targetUUID)) {
            sender.sendMessage(configManager.getMessage("self-teleport"));
            return false;
        }
        
        // 检查冷却时间
        if (isOnCooldown(senderUUID)) {
            int remainingTime = getRemainingCooldown(senderUUID);
            sender.sendMessage(configManager.getMessage("cooldown-active", "time", String.valueOf(remainingTime)));
            return false;
        }
        
        // 检查是否已经有待处理的请求
        if (teleportRequests.containsValue(senderUUID) && teleportRequests.get(targetUUID) == senderUUID) {
            sender.sendMessage(configManager.getMessage("already-sent-request", "player", target.getName()));
            return false;
        }
        
        // 保存请求
        teleportRequests.put(targetUUID, senderUUID);
        
        // 设置冷却时间
        setCooldown(senderUUID);
        
        // 发送消息给请求者
        sender.sendMessage(configManager.getMessage("request-sent", "player", target.getName()));
        
        // 发送消息给接收者
        if (configManager.isClickButtonsEnabled()) {
            // 使用点击按钮发送消息（将在实现clickable interface时完成）
            sendClickableRequest(target, sender);
        } else {
            target.sendMessage(configManager.getMessage("request-received", "player", sender.getName()));
        }
        
        // 设置请求过期任务
        int timeout = configManager.getRequestTimeout();
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (teleportRequests.containsKey(targetUUID) && teleportRequests.get(targetUUID).equals(senderUUID)) {
                teleportRequests.remove(targetUUID);
                requestTasks.remove(targetUUID);
                
                // 通知双方请求已过期
                Player senderPlayer = Bukkit.getPlayer(senderUUID);
                Player targetPlayer = Bukkit.getPlayer(targetUUID);
                
                if (senderPlayer != null && senderPlayer.isOnline()) {
                    senderPlayer.sendMessage(configManager.getMessage("request-sent-expired", "player", target.getName()));
                }
                
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    targetPlayer.sendMessage(configManager.getMessage("request-received-expired", "player", sender.getName()));
                }
            }
        }, timeout * 20L);
        
        requestTasks.put(targetUUID, task);
        
        return true;
    }

    /**
     * 接受传送请求
     * @param target 接收者
     * @return 是否成功接受请求
     */
    public boolean acceptTeleportRequest(Player target) {
        UUID targetUUID = target.getUniqueId();
        
        // 检查是否有待处理的请求
        if (!teleportRequests.containsKey(targetUUID)) {
            target.sendMessage(configManager.getMessage("no-pending-requests"));
            return false;
        }
        
        UUID senderUUID = teleportRequests.get(targetUUID);
        Player sender = Bukkit.getPlayer(senderUUID);
        
        // 检查发送者是否在线
        if (sender == null || !sender.isOnline()) {
            teleportRequests.remove(targetUUID);
            cancelRequestTask(targetUUID);
            target.sendMessage(configManager.getMessage("player-offline", "player", senderUUID.toString()));
            return false;
        }
        
        // 移除请求
        teleportRequests.remove(targetUUID);
        cancelRequestTask(targetUUID);
        
        // 发送消息
        target.sendMessage(configManager.getMessage("request-received-accepted", "player", sender.getName()));
        sender.sendMessage(configManager.getMessage("request-sent-accepted", "player", target.getName()));
        
        // 执行传送
        teleportPlayer(sender, target);
        
        return true;
    }

    /**
     * 拒绝传送请求
     * @param target 接收者
     * @return 是否成功拒绝请求
     */
    public boolean rejectTeleportRequest(Player target) {
        UUID targetUUID = target.getUniqueId();
        
        // 检查是否有待处理的请求
        if (!teleportRequests.containsKey(targetUUID)) {
            target.sendMessage(configManager.getMessage("no-pending-requests"));
            return false;
        }
        
        UUID senderUUID = teleportRequests.get(targetUUID);
        Player sender = Bukkit.getPlayer(senderUUID);
        
        // 移除请求
        teleportRequests.remove(targetUUID);
        cancelRequestTask(targetUUID);
        
        // 发送消息
        target.sendMessage(configManager.getMessage("request-received-rejected", "player", 
                sender != null ? sender.getName() : senderUUID.toString()));
        
        if (sender != null && sender.isOnline()) {
            sender.sendMessage(configManager.getMessage("request-sent-rejected", "player", target.getName()));
        }
        
        return true;
    }

    /**
     * 执行传送
     * @param sender 发送者（被传送的玩家）
     * @param target 接收者（目标位置的玩家）
     */
    private void teleportPlayer(Player sender, Player target) {
        int delay = configManager.getTeleportDelay();
        
        if (delay <= 0) {
            // 立即传送
            sender.teleport(target.getLocation());
            sender.sendMessage(configManager.getMessage("teleport-success-message"));
        } else {
            // 延迟传送
            for (int i = delay; i > 0; i--) {
                final int time = i;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (sender.isOnline() && target.isOnline()) {
                        sender.sendMessage(configManager.getMessage("teleport-countdown-message", "time", String.valueOf(time)));
                    }
                }, (delay - i) * 20L);
            }
            
            // 执行传送
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (sender.isOnline() && target.isOnline()) {
                    sender.teleport(target.getLocation());
                    sender.sendMessage(configManager.getMessage("teleport-success-message"));
                }
            }, delay * 20L);
        }
    }

    /**
     * 发送可点击的传送请求
     * @param target 接收者
     * @param sender 发送者
     */
    private void sendClickableRequest(Player target, Player sender) {
        String message = configManager.getMessage("request-received", "player", sender.getName());
        String acceptText = configManager.getRawMessage("click-accept");
        String rejectText = configManager.getRawMessage("click-reject");
        String acceptHover = configManager.getRawMessage("hover-accept");
        String rejectHover = configManager.getRawMessage("hover-reject");
        
        cn.ureyes.tpx.utils.MessageUtils.sendClickableRequest(
            target, message, acceptText, rejectText, acceptHover, rejectHover
        );
    }

    /**
     * 取消请求过期任务
     * @param targetUUID 接收者UUID
     */
    private void cancelRequestTask(UUID targetUUID) {
        BukkitTask task = requestTasks.remove(targetUUID);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * 设置冷却时间
     * @param playerUUID 玩家UUID
     */
    private void setCooldown(UUID playerUUID) {
        cooldowns.put(playerUUID, System.currentTimeMillis() + (configManager.getRequestCooldown() * 1000L));
    }

    /**
     * 检查是否在冷却中
     * @param playerUUID 玩家UUID
     * @return 是否在冷却中
     */
    private boolean isOnCooldown(UUID playerUUID) {
        if (!cooldowns.containsKey(playerUUID)) {
            return false;
        }
        
        return System.currentTimeMillis() < cooldowns.get(playerUUID);
    }

    /**
     * 获取剩余冷却时间（秒）
     * @param playerUUID 玩家UUID
     * @return 剩余冷却时间
     */
    private int getRemainingCooldown(UUID playerUUID) {
        if (!cooldowns.containsKey(playerUUID)) {
            return 0;
        }
        
        long remaining = cooldowns.get(playerUUID) - System.currentTimeMillis();
        return Math.max(0, (int) (remaining / 1000));
    }

    /**
     * 获取发送者UUID
     * @param targetUUID 接收者UUID
     * @return 发送者UUID，如果没有请求则返回null
     */
    public UUID getSender(UUID targetUUID) {
        return teleportRequests.get(targetUUID);
    }

    /**
     * 清理所有请求
     */
    public void clearRequests() {
        teleportRequests.clear();
        requestTasks.values().forEach(BukkitTask::cancel);
        requestTasks.clear();
    }
}
