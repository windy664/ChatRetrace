package org.windy.chatRetrace;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.*;

public class ChatRetrace extends JavaPlugin implements CommandExecutor {

    private static ChatRetrace instance;

    public static ChatRetrace getInstance() {
        return instance;
    }

    // 全局聊天记录缓存（最多保存最近500条）
    private final LinkedList<ChatMessage> chatHistory = new LinkedList<>();

    // 每个命令发送者的开始时间
    private final Map<CommandSender, Instant> startTimeMap = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        this.getCommand("chatretrace").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(), this);
        getLogger().info("ChatRetrace 插件已启用");
    }

    @Override
    public void onDisable() {
        getLogger().info("ChatRetrace 插件已卸载");
    }

    public void recordChat(String playerName, String message) {
        chatHistory.add(new ChatMessage(playerName, message, Instant.now()));
        if (chatHistory.size() > 500) {
            chatHistory.removeFirst();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("say")) {
            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            Instant now = Instant.now();
            Instant lastTime = startTimeMap.getOrDefault(sender, now.minusSeconds(10));
            startTimeMap.put(sender, now); // 更新时间戳为这次

            List<ChatMessage> matched = new ArrayList<>();
            for (ChatMessage msg : chatHistory) {
                if (!msg.timestamp.isBefore(lastTime) && msg.timestamp.isBefore(now)) {
                    matched.add(msg);
                }
            }

            if (!matched.isEmpty()) {
                matched.stream().limit(20).forEach(msg ->
                        sender.sendMessage("🌏玩家 | " + msg.player + "💬: " + msg.message));
            }
            Bukkit.broadcastMessage("🐧§cQQ群 §7 | §f群成员💬：" + message);
            return true;
        }

        sender.sendMessage("§c用法: /chatretrace say <内容>");
        return true;
    }


    public record ChatMessage(String player, String message, Instant timestamp) {}
}
