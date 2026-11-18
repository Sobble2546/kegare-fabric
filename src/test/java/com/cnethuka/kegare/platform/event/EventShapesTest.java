package com.cnethuka.kegare.platform.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cnethuka.kegare.platform.TriState;
import com.cnethuka.kegare.platform.bus.EventBus;
import com.cnethuka.kegare.platform.bus.EventPriority;
import com.cnethuka.kegare.platform.bus.IEventBus;
import com.cnethuka.kegare.platform.damage.DamageContainer;
import com.cnethuka.kegare.platform.event.entity.living.LivingDamageEvent;
import com.cnethuka.kegare.platform.event.entity.living.LivingEntityUseItemEvent;
import com.cnethuka.kegare.platform.event.entity.living.LivingIncomingDamageEvent;
import com.cnethuka.kegare.platform.event.entity.living.LivingKnockBackEvent;
import com.cnethuka.kegare.platform.event.entity.player.CriticalHitEvent;
import com.cnethuka.kegare.platform.event.entity.player.ItemEntityPickupEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The events' own behaviour (the entities are null: only state and the bus are exercised). */
class EventShapesTest {
    @Test
    void incomingDamageIsChangedInTheOrderOfPriorityAndTheContainerFollows() {
        IEventBus bus = new EventBus();
        DamageContainer container = new DamageContainer(null, 10f);
        // Order matters (Strength before the Closing Line, Fists then Spark then Zone): each sees what the one before left.
        bus.addListener(EventPriority.LOWEST, (LivingIncomingDamageEvent e) -> e.setAmount(e.getAmount() * 0.5f));
        bus.addListener(EventPriority.NORMAL, (LivingIncomingDamageEvent e) -> e.setAmount(e.getAmount() + 4f));
        bus.addListener(EventPriority.LOW, (LivingIncomingDamageEvent e) -> e.setInvulnerabilityTicks(0));
        LivingIncomingDamageEvent event = bus.post(new LivingIncomingDamageEvent(null, container));
        assertEquals(7f, event.getAmount());
        assertEquals(7f, container.getNewDamage(), "the container is what the pipeline reads back");
        assertEquals(0, container.getPostAttackInvulnerabilityTicks());
        assertFalse(event.isCanceled());
    }

    @Test
    void aCancelledIncomingHitReachesNoOneBelowTheCanceller() {
        IEventBus bus = new EventBus();
        List<String> seen = new ArrayList<>();
        bus.addListener(EventPriority.HIGHEST, (LivingIncomingDamageEvent e) -> {
            seen.add("time stops");
            e.setCanceled(true);
        });
        bus.addListener(EventPriority.NORMAL, (LivingIncomingDamageEvent e) -> seen.add("fists"));
        LivingIncomingDamageEvent event = bus.post(new LivingIncomingDamageEvent(null, new DamageContainer(null, 3f)));
        assertTrue(event.isCanceled());
        assertEquals(List.of("time stops"), seen);
    }

    @Test
    void preChangesTheContainerAndPostReportsWhatWasRemoved() {
        DamageContainer container = new DamageContainer(null, 10f);
        LivingDamageEvent.Pre pre = new LivingDamageEvent.Pre(null, container);
        pre.setNewDamage(pre.getNewDamage() * 0.7f);
        assertEquals(7f, container.getNewDamage(), 1e-6);
        assertEquals(10f, pre.getOriginalDamage());
        assertEquals(2f, new LivingDamageEvent.Post(null, container, 2f).getNewDamage(), "absorption took the rest");
        assertEquals(7f, new LivingDamageEvent.Post(null, container).getNewDamage(), 1e-6);
    }

    @Test
    void knockbackCarriesItsStrengthAndCanBeCancelled() {
        IEventBus bus = new EventBus();
        bus.addListener(EventPriority.HIGHEST, (LivingKnockBackEvent e) -> e.setCanceled(e.getStrength() > 1.0));
        assertTrue(bus.post(new LivingKnockBackEvent(null, 1.5, 0.0, 1.0)).isCanceled());
        assertFalse(bus.post(new LivingKnockBackEvent(null, 0.4, 0.0, 1.0)).isCanceled());
    }

    @Test
    void usingAnItemTickLetsAListenerShortenTheTimeLeft() {
        IEventBus bus = new EventBus();
        bus.addListener((LivingEntityUseItemEvent.Tick e) -> e.setDuration(e.getDuration() - 1));
        LivingEntityUseItemEvent.Tick tick = bus.post(new LivingEntityUseItemEvent.Tick(null, null, 32));
        assertEquals(31, tick.getDuration());
    }

    @Test
    void startUsingIsCancellableButTickIsNot() {
        assertTrue(((Object) new LivingEntityUseItemEvent.Start(null, null, 1)) instanceof com.cnethuka.kegare.platform.bus.ICancellableEvent);
        assertFalse(((Object) new LivingEntityUseItemEvent.Tick(null, null, 1)) instanceof com.cnethuka.kegare.platform.bus.ICancellableEvent);
    }

    @Test
    void aPickupIsLeftToTheGameUntilAListenerSaysOtherwise() {
        ItemEntityPickupEvent.Pre pre = new ItemEntityPickupEvent.Pre(null, null);
        assertSame(TriState.DEFAULT, pre.canPickup());
        pre.setCanPickup(TriState.FALSE);
        assertTrue(pre.canPickup().isFalse());
    }

    @Test
    void aCriticalHitIsVanillaUnlessAListenerChangesIt() {
        CriticalHitEvent event = new CriticalHitEvent(null, null, 1.5f, true);
        assertTrue(event.isCriticalHit());
        event.setCriticalHit(false);
        assertFalse(event.isCriticalHit());
        assertTrue(event.isVanillaCritical());
    }

    @Test
    void triStateReadsLikeTheNeoForgeOne() {
        assertTrue(TriState.TRUE.isTrue());
        assertTrue(TriState.DEFAULT.isDefault());
        assertSame(TriState.FALSE, TriState.from(false));
        assertTrue(TriState.DEFAULT.orElse(true));
        assertFalse(TriState.FALSE.orElse(true));
    }
}
