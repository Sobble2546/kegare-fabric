package com.cnethuka.kegare.platform.damage;

import com.cnethuka.kegare.platform.NeoForge;
import com.cnethuka.kegare.platform.event.entity.living.LivingDamageEvent;
import com.cnethuka.kegare.platform.event.entity.living.LivingIncomingDamageEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * The two-phase damage events (DESIGN §3.24 relies on them; map section 1), fired from mixins into {@code LivingEntity.hurtServer} and
 * {@code actuallyHurt} (and {@code Player.actuallyHurt}, which does not call super):
 * <ol>
 *   <li>{@link #begin} when {@code hurtServer} starts: a {@link DamageContainer} goes on this thread's stack ({@link #end} pops it);</li>
 *   <li>{@link #incoming} once the early outs are past: {@link LivingIncomingDamageEvent}; its amount is what the rest of the hit uses
 *       ({@link #amount}), and the hurt cooldown it starts is {@link #cooldown};</li>
 *   <li>{@link #beginActuallyHurt} / {@link #afterDefences} / {@link #endActuallyHurt}: {@link LivingDamageEvent.Pre} after armor, effects
 *       and enchantments, {@link LivingDamageEvent.Post} after absorption and the health.</li>
 * </ol>
 * The server thread only (the stack is per thread anyway). Nothing here may live in a mixin package: mixins call into it.
 */
public final class DamagePipeline {
    private DamagePipeline() {}

    /** One hurt on the stack. */
    public static final class Frame {
        final LivingEntity entity;
        final DamageContainer container;
        final boolean synthetic;
        boolean preFired;
        float absorptionBefore;

        Frame(LivingEntity entity, DamageContainer container, boolean synthetic) {
            this.entity = entity;
            this.container = container;
            this.synthetic = synthetic;
        }

        public DamageContainer container() {
            return container;
        }
    }

    private static final ThreadLocal<Deque<Frame>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private static Frame top(LivingEntity entity) {
        Frame frame = STACK.get().peek();
        return frame != null && frame.entity == entity ? frame : null;
    }

    // ---- hurtServer ----

    public static Frame begin(LivingEntity entity, DamageSource source, float amount) {
        Frame frame = new Frame(entity, new DamageContainer(source, amount), false);
        STACK.get().push(frame);
        return frame;
    }

    public static void end(Frame frame) {
        Deque<Frame> stack = STACK.get();
        if (!stack.contains(frame)) return;
        // Normally the top; if a listener threw, anything above it is stale.
        while (!stack.isEmpty()) {
            if (stack.pop() == frame) return;
        }
    }

    /** Fires the incoming event. True: a listener cancelled it, so the hit does nothing. */
    public static boolean incoming(LivingEntity entity) {
        Frame frame = top(entity);
        if (frame == null) return false;
        return NeoForge.EVENT_BUS.post(new LivingIncomingDamageEvent(entity, frame.container)).isCanceled();
    }

    /** The amount the hit goes on with: the container's, which listeners of the incoming event may have changed. */
    public static float amount(LivingEntity entity, float vanilla) {
        Frame frame = top(entity);
        return frame == null ? vanilla : frame.container.getNewDamage();
    }

    /** The hurt cooldown a hit that isn't inside a cooldown starts (vanilla: 20). */
    public static int cooldown(LivingEntity entity, int vanilla) {
        Frame frame = top(entity);
        return frame == null ? vanilla : frame.container.getPostAttackInvulnerabilityTicks();
    }

    // ---- actuallyHurt ----

    /**
     * Called at the top of {@code actuallyHurt}. Inside a {@code hurtServer} the container is that hit's; called on its own (a mod
     * calling it directly) it gets one of its own.
     */
    public static Frame beginActuallyHurt(LivingEntity entity, DamageSource source, float amount) {
        Frame frame = top(entity);
        if (frame == null) {
            frame = new Frame(entity, new DamageContainer(source, amount), true);
            STACK.get().push(frame);
        }
        frame.preFired = false;
        frame.container.enteredActuallyHurt(amount);
        return frame;
    }

    /** After armor, effects and enchantments, before absorption: fires the Pre event and returns the amount to go on with. */
    public static float afterDefences(LivingEntity entity, float amount) {
        Frame frame = top(entity);
        if (frame == null) return amount;
        frame.container.defencesDone(amount);
        frame.container.setNewDamage(amount);
        frame.absorptionBefore = entity.getAbsorptionAmount();
        frame.preFired = true;
        NeoForge.EVENT_BUS.post(new LivingDamageEvent.Pre(entity, frame.container));
        return frame.container.getNewDamage();
    }

    /** After {@code actuallyHurt} ran to its end: the Post event, if the Pre one fired (an invulnerable target fires neither). */
    public static void endActuallyHurt(Frame frame, boolean completed) {
        try {
            if (completed && frame.preFired) {
                float pre = frame.container.getNewDamage();
                float absorbed = Math.min(frame.absorptionBefore, Math.max(pre, 0f));
                frame.container.absorbed(absorbed);
                float removed = Math.max(pre - absorbed, 0f);
                NeoForge.EVENT_BUS.post(new LivingDamageEvent.Post(frame.entity, frame.container, removed));
            }
        } finally {
            frame.preFired = false;
            if (frame.synthetic) end(frame);
        }
    }
}
