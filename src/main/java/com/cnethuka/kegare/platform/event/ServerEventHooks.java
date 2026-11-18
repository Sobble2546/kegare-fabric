package com.cnethuka.kegare.platform.event;

import com.cnethuka.kegare.platform.NeoForge;
import com.cnethuka.kegare.platform.bus.IEventBus;
import com.cnethuka.kegare.platform.event.entity.EntityAttributeCreationEvent;
import com.cnethuka.kegare.platform.event.entity.player.PlayerEvent;
import com.cnethuka.kegare.platform.event.entity.player.PlayerInteractEvent;
import com.cnethuka.kegare.platform.event.level.BlockEvent;
import com.cnethuka.kegare.platform.event.server.ServerAboutToStartEvent;
import com.cnethuka.kegare.platform.event.server.ServerStartedEvent;
import com.cnethuka.kegare.platform.event.server.ServerStoppedEvent;
import com.cnethuka.kegare.platform.event.tick.LevelTickEvent;
import com.cnethuka.kegare.platform.event.tick.ServerTickEvent;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Fabric callbacks that fire NeoForge-shaped events on {@link NeoForge#EVENT_BUS}. The events Fabric has no usable callback for
 * (damage, death, knockback, item use, attack, ...) are fired by the mixins in {@code kegare-events.mixins.json}.
 * {@link #init} runs once, early in mod initialisation; {@link #fireAttributeCreation} once after the entity types exist.
 */
public final class ServerEventHooks {
    private ServerEventHooks() {}

    private static boolean initialised;

    /** The respawned player whose Fabric "changed world" callback NeoForge has no event for (see the respawn comment below). */
    private static ServerPlayer respawning;
    /** On while Fabric's block-break callback is what asked: the bridge below must not ask Fabric again. */
    private static final ThreadLocal<Boolean> IN_FABRIC_BREAK = ThreadLocal.withInitial(() -> false);
    /** On while the bridge asks Fabric's block-break callbacks for a Kegare-made BreakEvent: our own callback must stay out of it. */
    private static final ThreadLocal<Boolean> IN_BRIDGE = ThreadLocal.withInitial(() -> false);

    public static synchronized void init() {
        if (initialised) return;
        initialised = true;

        // ---- ticks and lifecycle ----
        ServerTickEvents.START_SERVER_TICK.register(server -> NeoForge.EVENT_BUS.post(new ServerTickEvent.Pre(server)));
        ServerTickEvents.END_SERVER_TICK.register(server -> NeoForge.EVENT_BUS.post(new ServerTickEvent.Post(server)));
        ServerTickEvents.START_WORLD_TICK.register(level -> NeoForge.EVENT_BUS.post(new LevelTickEvent.Pre(level)));
        ServerTickEvents.END_WORLD_TICK.register(level -> NeoForge.EVENT_BUS.post(new LevelTickEvent.Post(level)));
        ServerLifecycleEvents.SERVER_STARTING.register(server -> NeoForge.EVENT_BUS.post(new ServerAboutToStartEvent(server)));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> NeoForge.EVENT_BUS.post(new ServerStartedEvent(server)));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            respawning = null;
            NeoForge.EVENT_BUS.post(new ServerStoppedEvent(server));
        });

        // ---- commands and chat ----
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
                NeoForge.EVENT_BUS.post(new RegisterCommandsEvent(dispatcher, selection, buildContext)));
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) ->
                NeoForge.EVENT_BUS.post(new ServerChatEvent(sender, message.signedContent(), message)));

        // ---- players coming and going ----
        ServerPlayerEvents.JOIN.register(player -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player)));
        ServerPlayerEvents.LEAVE.register(player -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player)));
        // Fabric also reports a respawn into another dimension as a world change; NeoForge's PlayerChangedDimensionEvent is for portals and
        // the like only. COPY_FROM is the start of a respawn (the new player is the one the world-change callback will name), AFTER_RESPAWN the end.
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            respawning = newPlayer;
            NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(newPlayer, oldPlayer, !alive));
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            respawning = null;
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(newPlayer, alive));
        });
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            if (player == respawning) return;
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(player, origin.dimension(), destination.dimension()));
        });
        // Fabric names the tracked entity first; NeoForge's event is the player's (getEntity) with the tracked one as the target.
        EntityTrackingEvents.START_TRACKING.register((tracked, player) -> NeoForge.EVENT_BUS.post(new PlayerEvent.StartTracking(player, tracked)));

        // ---- interaction: any cancel is a FAIL (nothing happens, nothing is used) ----
        UseBlockCallback.EVENT.register((player, level, hand, hit) ->
                NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, hand, hit.getBlockPos(), hit)).isCanceled()
                        ? InteractionResult.FAIL : InteractionResult.PASS);
        UseItemCallback.EVENT.register((player, level, hand) ->
                NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(player, hand)).isCanceled()
                        ? InteractionResult.FAIL : InteractionResult.PASS);
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
                NeoForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickBlock(player, pos, direction)).isCanceled()
                        ? InteractionResult.FAIL : InteractionResult.PASS);
        // One Fabric callback for Neo's EntityInteract and EntityInteractSpecific: Kegare treats them alike, so both are posted.
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            boolean plain = NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player, hand, entity)).isCanceled();
            Vec3 local = hit == null ? Vec3.ZERO : hit.getLocation().subtract(entity.position());
            boolean specific = NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteractSpecific(player, hand, entity, local)).isCanceled();
            return plain || specific ? InteractionResult.FAIL : InteractionResult.PASS;
        });

        // ---- block breaking, both ways ----
        // A real break: Fabric asks its callbacks, ours posts Neo's BreakEvent for Kegare's listeners (and the bridge below keeps out).
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (IN_BRIDGE.get() || !(player instanceof ServerPlayer)) return true;
            IN_FABRIC_BREAK.set(true);
            try {
                return !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled();
            } finally {
                IN_FABRIC_BREAK.set(false);
            }
        });
        // A break Kegare makes itself (BattleDamage posts BreakEvent straight to the bus): ask Fabric's callbacks too, where claim and
        // protection mods listen. LOWEST so Kegare's own listeners go first, and it is skipped once one cancelled.
        NeoForge.EVENT_BUS.addListener(com.cnethuka.kegare.platform.bus.EventPriority.LOWEST, false, BlockEvent.BreakEvent.class, event -> {
            if (IN_FABRIC_BREAK.get() || !(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof net.minecraft.world.level.Level level)) return;
            IN_BRIDGE.set(true);
            try {
                boolean allowed = PlayerBlockBreakEvents.BEFORE.invoker()
                        .beforeBlockBreak(level, player, event.getPos(), event.getState(), level.getBlockEntity(event.getPos()));
                if (!allowed) event.setCanceled(true);
            } finally {
                IN_BRIDGE.set(false);
            }
        });
    }

    /** Posts {@link EntityAttributeCreationEvent} on {@code bus} (the mod bus the registering code listens on). Call once, after entity types exist. */
    public static void fireAttributeCreation(IEventBus bus) {
        bus.post(new EntityAttributeCreationEvent());
    }
}
