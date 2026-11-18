package com.cnethuka.kegare.innate_technique.time_manipulation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import com.cnethuka.kegare.platform.bus.EventPriority;
import com.cnethuka.kegare.platform.bus.ICancellableEvent;
import com.cnethuka.kegare.platform.NeoForge;
import com.cnethuka.kegare.platform.event.entity.EntityJoinLevelEvent;
import com.cnethuka.kegare.platform.event.entity.EntityTravelToDimensionEvent;
import com.cnethuka.kegare.platform.event.entity.ProjectileImpactEvent;
import com.cnethuka.kegare.platform.event.entity.living.LivingDeathEvent;
import com.cnethuka.kegare.platform.event.entity.living.LivingEntityUseItemEvent;
import com.cnethuka.kegare.platform.event.entity.living.LivingIncomingDamageEvent;
import com.cnethuka.kegare.platform.event.entity.player.AttackEntityEvent;
import com.cnethuka.kegare.platform.event.entity.player.PlayerEvent;
import com.cnethuka.kegare.platform.event.entity.player.PlayerInteractEvent;
import com.cnethuka.kegare.platform.event.level.BlockEvent;
import com.cnethuka.kegare.platform.event.server.ServerStoppedEvent;
import com.cnethuka.kegare.platform.event.tick.EntityTickEvent;
import com.cnethuka.kegare.platform.event.tick.LevelTickEvent;
import com.cnethuka.kegare.platform.event.tick.PlayerTickEvent;
import com.cnethuka.kegare.platform.event.tick.ServerTickEvent;

/**
 * Time Manipulation's server hooks: Point Maker's marks, the position history Rewind reads, Stasis Fields, Standstill's stops and the
 * advancements' timers; and everything they need cleared on death, logout, a change of dimension and the server stopping.
 */
final class TimeEvents {
    private TimeEvents() {}

    static void init() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> PositionHistory.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post e) -> {
            if (e.getLevel() instanceof ServerLevel level) {
                StasisFields.tick(level);
                TimeStops.tick(level);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                PointMarks.tick(p);
                TimeAdvancements.tick(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(AccelerateAbility::onUseTick); // both sides, so client item use keeps step with the server
        NeoForge.EVENT_BUS.addListener(TimeStops::onEntityTick);
        NeoForge.EVENT_BUS.addListener(TimeStops::onJoin);
        NeoForge.EVENT_BUS.addListener(TimeStops::onImpact);
        // HIGHEST: damage to the frozen is held at its full amount, and no other listener sees it until it lands.
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, TimeStops::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, TimeStops::onKnockBack);
        NeoForge.EVENT_BUS.addListener(TimeStops::onEnderTeleport);

        // A frozen player does nothing.
        NeoForge.EVENT_BUS.addListener((AttackEntityEvent e) -> refuse(e.getEntity(), e));
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> refuse(e.getEntity(), e));
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem e) -> refuse(e.getEntity(), e));
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.LeftClickBlock e) -> refuse(e.getEntity(), e));
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract e) -> refuse(e.getEntity(), e));
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteractSpecific e) -> refuse(e.getEntity(), e));
        NeoForge.EVENT_BUS.addListener((LivingEntityUseItemEvent.Start e) -> refuse(e.getEntity(), e));
        NeoForge.EVENT_BUS.addListener((BlockEvent.BreakEvent e) -> refuse(e.getPlayer(), e));

        NeoForge.EVENT_BUS.addListener((LivingDeathEvent e) -> {
            LivingEntity entity = e.getEntity();
            if (entity.level().isClientSide()) return;
            PositionHistory.forget(entity);
            if (entity instanceof ServerPlayer p) {
                PointMarks.clear(p);
                TimeAdvancements.forget(p.getUUID());
                TimeStops.onDied(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (!(e.getEntity() instanceof ServerPlayer p)) return;
            PointMarks.forget(p.getUUID());
            PositionHistory.forget(p);
            TimeAdvancements.forget(p.getUUID());
            TimeStops.onLeaving(p); // before they're removed: a frozen player takes what was held for them
            AccelerateAbility.forget(p.getUUID());
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) TimeStops.sendTo(p);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (!(e.getEntity() instanceof ServerPlayer p)) return;
            TimeStops.onLeaving(p); // a caster who's gone ends it
            TimeStops.sendTo(p);
        });
        // LOWEST, and only if the trip wasn't cancelled: a frozen player sent to another level first takes what was held for them,
        // where they were held.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (EntityTravelToDimensionEvent e) -> {
            if (!e.isCanceled() && e.getEntity() instanceof ServerPlayer p) TimeStops.onTravel(p);
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            PointMarks.clearAll();
            PositionHistory.clear();
            StasisFields.clear();
            TimeStops.clear();
            TimeAdvancements.clear();
        });
    }

    private static void refuse(Entity who, ICancellableEvent event) {
        if (who instanceof ServerPlayer p && TimeStops.frozen(p)) event.setCanceled(true);
    }
}
