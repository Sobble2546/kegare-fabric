package com.cnethuka.kegare.platform.damage;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.damagesource.DamageSource;

/**
 * NeoForge's damage bookkeeping for one hit, cut to what Kegare reads: the source, the amount through the pipeline, the hurt cooldown
 * the hit starts, and how much the defences took off ({@link #getReduction}).
 */
public class DamageContainer {
    public enum Reduction { ARMOR, ENCHANTMENTS, MOB_EFFECTS, ABSORPTION, INVULNERABILITY }

    private final DamageSource source;
    private final float originalDamage;
    private float newDamage;
    private int postAttackInvulnerabilityTicks = 20;
    private float amountAtActuallyHurt = Float.NaN;
    private final Map<Reduction, Float> reductions = new EnumMap<>(Reduction.class);

    public DamageContainer(DamageSource source, float originalDamage) {
        this.source = source;
        this.originalDamage = originalDamage;
        this.newDamage = originalDamage;
    }

    public DamageSource getSource() {
        return source;
    }

    public float getOriginalDamage() {
        return originalDamage;
    }

    public float getNewDamage() {
        return newDamage;
    }

    public void setNewDamage(float newDamage) {
        this.newDamage = newDamage;
    }

    /** The hurt cooldown (invulnerable ticks) the hit starts, 20 unless a listener changed it. */
    public int getPostAttackInvulnerabilityTicks() {
        return postAttackInvulnerabilityTicks;
    }

    public void setPostAttackInvulnerabilityTicks(int ticks) {
        this.postAttackInvulnerabilityTicks = ticks;
    }

    /** How much of the hit {@code reduction} took off. Armor, enchantments and effects are reported together under ARMOR. */
    public float getReduction(Reduction reduction) {
        return reductions.getOrDefault(reduction, 0f);
    }

    /** The amount that entered {@code actuallyHurt}, before armor and effects; NaN if it has not got that far. */
    public float getAmountAtActuallyHurt() {
        return amountAtActuallyHurt;
    }

    // ---- set by the damage pipeline (platform.damage.DamagePipeline); not part of NeoForge's API ----

    void enteredActuallyHurt(float amount) {
        amountAtActuallyHurt = amount;
        reductions.clear();
    }

    /** The defences took {@code amountAtActuallyHurt - afterDefences}; the listeners of the Pre event see {@code afterDefences}. */
    void defencesDone(float afterDefences) {
        if (!Float.isNaN(amountAtActuallyHurt)) reductions.put(Reduction.ARMOR, Math.max(0f, amountAtActuallyHurt - afterDefences));
    }

    void absorbed(float amount) {
        reductions.put(Reduction.ABSORPTION, amount);
    }
}
