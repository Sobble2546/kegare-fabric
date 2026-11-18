package com.cnethuka.kegare.timing;

import com.cnethuka.kegare.Kegare;
import net.minecraft.server.level.ServerPlayer;
import com.cnethuka.kegare.platform.bus.EventPriority;
import com.cnethuka.kegare.platform.bus.SubscribeEvent;
import com.cnethuka.kegare.platform.bus.EventBusSubscriber;
import com.cnethuka.kegare.platform.Players;
import com.cnethuka.kegare.platform.event.entity.player.AttackEntityEvent;
import com.cnethuka.kegare.platform.event.entity.player.PlayerEvent;
import com.cnethuka.kegare.platform.event.server.ServerStoppedEvent;
import com.cnethuka.kegare.platform.event.tick.PlayerTickEvent;

/** Where sub-tick timing meets the game (DESIGN §3.29): the clock pings, and the melee attack's time. */
@EventBusSubscriber(modid = Kegare.MODID)
public final class TimingEvents {
    private TimingEvents() {}

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && !Players.isFake(player)) ClockSync.tick(player);
    }

    /** First, so everything after it can ask when the swing was. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SubTick.mark(player, TimedInput.ATTACK);
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SubTick.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    static void onStopped(ServerStoppedEvent event) {
        SubTick.clear();
    }
}
