package cn.ureyes.tpx.commands;

import cn.ureyes.tpx.Tpx;
import cn.ureyes.tpx.managers.ConfigManager;
import cn.ureyes.tpx.managers.TeleportManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class TeleportCommand implements CommandExecutor, TabCompleter {
    private final Tpx plugin;
    private final ConfigManager configManager;
    private final TeleportManager teleportManager;
    private final List<String> subCommands = Arrays.asList("accept", "reject", "help");

    public TeleportCommand(Tpx plugin, ConfigManager configManager, TeleportManager teleportManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.teleportManager = teleportManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getPrefix() + "§c只有玩家可以使用此命令！");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0) {
            // .cy xxxxx
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "accept":
                    teleportManager.acceptTeleportRequest(player);
                    return true;
                case "reject":
                    teleportManager.rejectTeleportRequest(player);
                    return true;
                case "help":
                    sendHelpMessage(player);
                    return true;
            }
        }

        //handle tp event
        if (args.length == 1) {
            String targetName = args[0];
            Player target = Bukkit.getPlayer(targetName);

            if (target == null || !target.isOnline()) {
                player.sendMessage(configManager.getMessage("player-not-found", "player", targetName));
                return true;
            }

            teleportManager.sendTeleportRequest(player, target);
            return true;
        }


        sendHelpMessage(player);
        return true;
    }

    //help
    private void sendHelpMessage(Player player) {
        player.sendMessage(configManager.getPrefix() + "§e===== §f传送命令帮助 §e=====");
        player.sendMessage(configManager.getPrefix() + "§f/chuansong <玩家名> §7- §f发送传送请求");
        player.sendMessage(configManager.getPrefix() + "§f/cs <玩家名> §7- §f发送传送请求");
        player.sendMessage(configManager.getPrefix() + "§f/cs accept §7- §f接受传送请求");
        player.sendMessage(configManager.getPrefix() + "§f/cs reject §7- §f拒绝传送请求");
        player.sendMessage(configManager.getPrefix() + "§f/cs help §7- §f显示此帮助信息");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();


            completions.addAll(subCommands);
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()));

            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
