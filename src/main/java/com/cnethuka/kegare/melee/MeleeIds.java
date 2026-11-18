package com.cnethuka.kegare.melee;

import com.cnethuka.kegare.Kegare;
import java.util.List;
import net.minecraft.resources.Identifier;

/** Animations ({@code player_animations/melee.json}), effects and loop keys. */
public final class MeleeIds {
    public static final Identifier ANIM_DASH_FORWARD = Kegare.id("dash_forward");
    public static final Identifier ANIM_DASH_BACK = Kegare.id("dash_back");
    public static final Identifier ANIM_DASH_LEFT = Kegare.id("dash_left");
    public static final Identifier ANIM_DASH_RIGHT = Kegare.id("dash_right");
    public static final Identifier ANIM_PARRY_GUARD = Kegare.id("parry_guard");
    public static final Identifier ANIM_PARRY_PERFECT = Kegare.id("parry_perfect");
    public static final Identifier ANIM_PARRY_RECOVERY = Kegare.id("parry_recovery");
    public static final Identifier ANIM_STAGGER = Kegare.id("stagger");
    public static final Identifier ANIM_REINFORCED_STRIKE = Kegare.id("reinforced_strike");
    /** Every animation of the layer. */
    public static final List<Identifier> ANIMATIONS = List.of(ANIM_DASH_FORWARD, ANIM_DASH_BACK, ANIM_DASH_LEFT, ANIM_DASH_RIGHT,
            ANIM_PARRY_GUARD, ANIM_PARRY_PERFECT, ANIM_PARRY_RECOVERY, ANIM_STAGGER, ANIM_REINFORCED_STRIKE);

    public static final Identifier FX_DASH_BURST = Kegare.id("melee/dash_burst");
    public static final Identifier FX_DASH_TRAIL = Kegare.id("melee/dash_trail");
    /** What the dasher sees instead in first person (the client swaps them). */
    public static final Identifier FX_DASH_TRAIL_SELF = Kegare.id("melee/dash_trail_self");
    public static final Identifier FX_PARRY_SPARK = Kegare.id("melee/parry_spark");
    /** What the defender sees instead in first person (the client swaps them). */
    public static final Identifier FX_PARRY_SPARK_SELF = Kegare.id("melee/parry_spark_self");
    public static final Identifier FX_GUARD_HIT = Kegare.id("melee/guard_hit");
    public static final Identifier FX_GUARD_HIT_SELF = Kegare.id("melee/guard_hit_self");
    public static final Identifier FX_STRIKE_IMPACT = Kegare.id("melee/strike_impact");
    /** Malice let go when a Strike lands, in the glow's color, on top of the impact. */
    public static final Identifier FX_STRIKE_BURST = Kegare.id("melee/strike_burst");

    static final String GUARD_LOOP = "kegare:melee_guard";
    /** The perfect-parry reaction's length: the guard pose comes back after it if they're still holding. */
    public static final int PARRY_PERFECT_TICKS = 9;

    private MeleeIds() {}
}
