package cn.ureyes.tpx.utils;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;


public class MessageUtils {

    /**
     * 发送可点击的传送请求消息
     * @param target 接收者
     * @param message 消息内容
     * @param acceptText 接受按钮文本
     * @param rejectText 拒绝按钮文本
     * @param acceptHover 接受按钮悬停文本
     * @param rejectHover 拒绝按钮悬停文本
     */
    public static void sendClickableRequest(Player target, String message, 
                                           String acceptText, String rejectText,
                                           String acceptHover, String rejectHover) {
        TextComponent baseMessage = new TextComponent(message);
        
        //accept
        TextComponent acceptButton = new TextComponent(" " + acceptText + " ");
        acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cs accept"));
        acceptButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new ComponentBuilder(acceptHover).create()));
        
        //reject
        TextComponent rejectButton = new TextComponent(" " + rejectText + " ");
        rejectButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cs reject"));
        rejectButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new ComponentBuilder(rejectHover).create()));
        
        //combine
        baseMessage.addExtra(acceptButton);
        baseMessage.addExtra(rejectButton);
        
        //sned
        target.spigot().sendMessage(baseMessage);
    }
}
