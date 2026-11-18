package com.cnethuka.kegare.database;

import com.cnethuka.kegare.Kegare;
import net.minecraft.server.level.ServerPlayer;
import com.cnethuka.kegare.platform.bus.EventPriority;
import com.cnethuka.kegare.platform.bus.SubscribeEvent;
import com.cnethuka.kegare.platform.bus.EventBusSubscriber;
import net.fabricmc.fabric.api.entity.FakePlayer;
import com.cnethuka.kegare.platform.event.entity.player.PlayerEvent;
import com.cnethuka.kegare.platform.event.tick.ServerTickEvent;

/** The game's side of the storage (DESIGN §3.33): saving on the interval and on the way out. Does nothing with it off. */
@EventBusSubscriber(modid = Kegare.MODID)
public final class DatabaseEvents {
    private DatabaseEvents() {}

    /** Last, so a birth or anything else at login goes in straight away. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        KegareDatabase database = KegareDatabase.current();
        if (database != null && event.getEntity() instanceof ServerPlayer player && !(com.cnethuka.kegare.platform.Players.isFakeBody(player))) database.snapshot(player);
    }

    /** Last, after the casts have stopped and everything else has tidied up. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        KegareDatabase database = KegareDatabase.current();
        if (database != null && event.getEntity() instanceof ServerPlayer player && !(com.cnethuka.kegare.platform.Players.isFakeBody(player))) database.logout(player);
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        KegareDatabase database = KegareDatabase.current();
        if (database == null) return;
        long tick = event.getServer().getTickCount();
        if (tick % 20 == 0) database.expireLoaded();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (database.dueThisTick(player.getUUID(), tick)) database.snapshot(player);
        }
    }
}
