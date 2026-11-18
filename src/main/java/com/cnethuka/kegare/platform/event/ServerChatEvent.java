package com.cnethuka.kegare.platform.event;

import com.cnethuka.kegare.platform.bus.Event;
import com.cnethuka.kegare.platform.bus.ICancellableEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;

/** A player's chat message, before it is broadcast. */
public final class ServerChatEvent extends Event implements ICancellableEvent {
    private final ServerPlayer player;
    private final String rawText;
    private final PlayerChatMessage message;

    public ServerChatEvent(ServerPlayer player, String rawText, PlayerChatMessage message) {
        this.player = player;
        this.rawText = rawText;
        this.message = message;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public String getRawText() {
        return rawText;
    }

    public String getUsername() {
        return player.getGameProfile().name();
    }

    public PlayerChatMessage getMessage() {
        return message;
    }

    public Component getMessageComponent() {
        return message.decoratedContent();
    }
}
