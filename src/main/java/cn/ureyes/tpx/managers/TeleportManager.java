package cn.ureyes.tpx.managers;

import cn.ureyes.tpx.Tpx;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class TeleportManager {
    private final Tpx plugin;
    private final ConfigManager configManager;
    
    //存储传送请求 key=接收者UUID value=发送者UUID
    private final Map<UUID, UUID> teleportRequests = new ConcurrentHashMap<>();
    
    //存储请求过期任务 key=接收者UUID value=任务ID
    private final Map<UUID, BukkitTask> requestTasks = new ConcurrentHashMap<>();
    
    //存储冷却时间 key=玩家UUID value=冷却结束时间
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public TeleportManager(Tpx plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    //发送传送请求
    public boolean sendTeleportRequest(Player sender, Player target) {
        UUID senderUUID = sender.getUniqueId();
        UUID targetUUID = target.getUniqueId();
        
        //self check
        if (senderUUID.equals(targetUUID)) {
            sender.sendMessage(configManager.getMessage("self-teleport"));
            return false;
        }
        
        //calmdown check
        if (isOnCooldown(senderUUID)) {
            int remainingTime = getRemainingCooldown(senderUUID);
            sender.sendMessage(configManager.getMessage("cooldown-active", "time", String.valueOf(remainingTime)));
            return false;
        }
        
        //检查现有的请求是否重复
        if (teleportRequests.containsValue(senderUUID) && teleportRequests.get(targetUUID) == senderUUID) {
            sender.sendMessage(configManager.getMessage("already-sent-request", "player", target.getName()));
            return false;
        }

        teleportRequests.put(targetUUID, senderUUID);
        setCooldown(senderUUID);
        sender.sendMessage(configManager.getMessage("request-sent", "player", target.getName()));
        
        //send to
        if (configManager.isClickButtonsEnabled()) {
            sendClickableRequest(target, sender);
        } else {
            target.sendMessage(configManager.getMessage("request-received", "player", sender.getName()));
        }
        
        //set task
        int timeout = configManager.getRequestTimeout();
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (teleportRequests.containsKey(targetUUID) && teleportRequests.get(targetUUID).equals(senderUUID)) {
                teleportRequests.remove(targetUUID);
                requestTasks.remove(targetUUID);
                

                Player senderPlayer = Bukkit.getPlayer(senderUUID);
                Player targetPlayer = Bukkit.getPlayer(targetUUID);

                //执行通知
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

    //接受传送
    public boolean acceptTeleportRequest(Player target) {
        UUID targetUUID = target.getUniqueId();
        

        if (!teleportRequests.containsKey(targetUUID)) {
            target.sendMessage(configManager.getMessage("no-pending-requests"));
            return false;
        }
        
        UUID senderUUID = teleportRequests.get(targetUUID);
        Player sender = Bukkit.getPlayer(senderUUID);
        

        if (sender == null || !sender.isOnline()) {
            teleportRequests.remove(targetUUID);
            cancelRequestTask(targetUUID);
            target.sendMessage(configManager.getMessage("player-offline", "player", senderUUID.toString()));
            return false;
        }
        

        teleportRequests.remove(targetUUID);
        cancelRequestTask(targetUUID);
        
        //send
        target.sendMessage(configManager.getMessage("request-received-accepted", "player", sender.getName()));
        sender.sendMessage(configManager.getMessage("request-sent-accepted", "player", target.getName()));

        teleportPlayer(sender, target);
        
        return true;
    }

    //拒绝了传送
    public boolean rejectTeleportRequest(Player target) {
        UUID targetUUID = target.getUniqueId();
        
        // 检查是否有待处理的请求
        if (!teleportRequests.containsKey(targetUUID)) {
            target.sendMessage(configManager.getMessage("no-pending-requests"));
            return false;
        }
        
        UUID senderUUID = teleportRequests.get(targetUUID);
        Player sender = Bukkit.getPlayer(senderUUID);
        

        teleportRequests.remove(targetUUID);
        cancelRequestTask(targetUUID);

        target.sendMessage(configManager.getMessage("request-received-rejected", "player", 
                sender != null ? sender.getName() : senderUUID.toString()));
        
        if (sender != null && sender.isOnline()) {
            sender.sendMessage(configManager.getMessage("request-sent-rejected", "player", target.getName()));
        }
        
        return true;
    }


    private void teleportPlayer(Player sender, Player target) {
        int delay = configManager.getTeleportDelay();
        
        if (delay <= 0) {
            sender.teleport(target.getLocation());
            sender.sendMessage(configManager.getMessage("teleport-success-message"));
        } else {
            //delay
            for (int i = delay; i > 0; i--) {
                final int time = i;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (sender.isOnline() && target.isOnline()) {
                        sender.sendMessage(configManager.getMessage("teleport-countdown-message", "time", String.valueOf(time)));
                    }
                }, (delay - i) * 20L);
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (sender.isOnline() && target.isOnline()) {
                    sender.teleport(target.getLocation());
                    sender.sendMessage(configManager.getMessage("teleport-success-message"));
                }
            }, delay * 20L);
        }
    }


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


    private void cancelRequestTask(UUID targetUUID) {
        BukkitTask task = requestTasks.remove(targetUUID);
        if (task != null) {
            task.cancel();
        }
    }

    private void setCooldown(UUID playerUUID) {
        cooldowns.put(playerUUID, System.currentTimeMillis() + (configManager.getRequestCooldown() * 1000L));
    }

    private boolean isOnCooldown(UUID playerUUID) {
        if (!cooldowns.containsKey(playerUUID)) {
            return false;
        }
        
        return System.currentTimeMillis() < cooldowns.get(playerUUID);
    }

    private int getRemainingCooldown(UUID playerUUID) {
        if (!cooldowns.containsKey(playerUUID)) {
            return 0;
        }
        
        long remaining = cooldowns.get(playerUUID) - System.currentTimeMillis();
        return Math.max(0, (int) (remaining / 1000));
    }



    public void clearRequests() {
        teleportRequests.clear();
        requestTasks.values().forEach(BukkitTask::cancel);
        requestTasks.clear();
    }
}
