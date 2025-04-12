package org.windy.chatRetrace;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ChatRetrace extends JavaPlugin implements TabExecutor, Listener {

    private static final String LOGS_DIR = "plugins/TrChat/logs/";
    private final Map<CommandSender, List<String>> collectingChats = new HashMap<>();
    private final Map<CommandSender, Instant> collectionStartTime = new HashMap<>();

    @Override
    public void onEnable() {
        getCommand("chatretrace").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("§a[ChatRetrace] 插件已启用。");
    }

    @Override
    public void onDisable() {
        getLogger().info("§c[ChatRetrace] 插件已关闭。");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            // /chatretrace <行数>
            return handleReadLog(sender, args[0]);
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("say")) {
            // /chatretrace say <内容>
            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            Bukkit.broadcastMessage("§e[广播] §f" + message);

            Instant startTime = Instant.now();
            collectingChats.put(sender, new ArrayList<>());
            collectionStartTime.put(sender, startTime);

            // 调度10秒后返回信息
            Bukkit.getScheduler().runTaskLater(this, () -> {
                List<String> messages = collectingChats.remove(sender);
                collectionStartTime.remove(sender);

                if (messages == null || messages.isEmpty()) {
                    sender.sendMessage("§7[ChatRetrace] 10秒内无人聊天。");
                } else {
                    sender.sendMessage("§a===== 10秒内聊天记录 =====");
                    for (String msg : messages) {
                        sender.sendMessage("§7" + msg);
                    }
                }
            }, 20L * 10); // 10秒 = 200 tick

            return true;
        }

        sender.sendMessage("§c用法: /chatretrace <行数> 或 /chatretrace say <内容>");
        return true;
    }

    private boolean handleReadLog(CommandSender sender, String lineArg) {
        int lines;
        try {
            lines = Integer.parseInt(lineArg);
            if (lines <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage("§c请输入一个大于0的整数行数。");
            return true;
        }

        LocalDate today = LocalDate.now();
        String fileName = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".txt";
        Path filePath = new File(LOGS_DIR, fileName).toPath();

        if (!Files.exists(filePath)) {
            sender.sendMessage("§c日志文件不存在：" + filePath.getFileName());
            return true;
        }

        try {
            List<String> allLines = Files.readAllLines(filePath);
            int fromIndex = Math.max(0, allLines.size() - lines);
            List<String> recentLines = allLines.subList(fromIndex, allLines.size());

            sender.sendMessage("§a===== 最近 " + recentLines.size() + " 条聊天记录 =====");
            for (String line : recentLines) {
                sender.sendMessage("§7" + line);
            }
        } catch (IOException e) {
            sender.sendMessage("§c读取日志文件失败。");
            e.printStackTrace();
        }

        return true;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Instant now = Instant.now();
        for (Map.Entry<CommandSender, Instant> entry : collectionStartTime.entrySet()) {
            if (now.isAfter(entry.getValue())) {
                collectingChats.get(entry.getKey()).add(
                        String.format("%s: %s", event.getPlayer().getName(), event.getMessage())
                );
            }
        }
    }
}
