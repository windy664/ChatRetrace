package org.windy.chatRetrace;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        ChatRetrace plugin = ChatRetrace.getInstance();
        plugin.recordChat(event.getPlayer().getName(), event.getMessage());
    }
}
