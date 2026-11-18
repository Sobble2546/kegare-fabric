package com.cnethuka.kegare.innate_technique.rime;

import com.cnethuka.kegare.Kegare;
import com.cnethuka.kegare.advancement.KegareAdvancements;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Rime's advancement tab (root {@code kegare:innate_technique/rime}, unlocked on first rolling Rime). Every one is
 * hidden and silent in chat (they would reveal who has Rime), uses minecraft:impossible and is awarded from code.
 * PvP ones count player targets only.
 */
public final class RimeAdvancements {
    public static final Identifier ROOT = KegareAdvancements.innateTechniqueRoot(Kegare.id("rime"));
    public static final Identifier SHATTER = id("shatter");
    public static final Identifier COLD_OPEN = id("cold_open");
    public static final Identifier BRAIN_FREEZE = id("brain_freeze");
    public static final Identifier FULL_CHARGE = id("full_charge");
    public static final Identifier THIN_ICE = id("thin_ice");
    public static final Identifier COLD_BLOODED = id("cold_blooded");
    public static final Identifier DEEP_FREEZE = id("deep_freeze");
    public static final Identifier LONG_WAY_FROM_HOME = id("long_way_from_home");
    public static final Identifier HOME_FIELD = id("home_field");
    // Specials
    public static final Identifier SPLINTER = id("splinter");
    public static final Identifier SKY_HIGH = id("sky_high");
    public static final Identifier SLIP_N_SLIDE = id("slip_n_slide");
    public static final Identifier SHATTERED_PRESSURE = id("shattered_pressure");

    /** Slip 'n Slide: total blocks slid with Ice Path (kept across sessions). */
    public static final double SLIP_N_SLIDE_BLOCKS = 100.0;
    /** Shattered Pressure: players hit by one Shatter Shell. */
    public static final int SHATTERED_PRESSURE_PLAYERS = 3;

    /** Thin Ice: total damage Rime Shell has blocked (kept across sessions). */
    public static final double THIN_ICE_DAMAGE = 100.0;
    /** Deep Freeze: this many freezes of the same player... */
    public static final int DEEP_FREEZE_COUNT = 3;
    /** ...within this many ticks (60 seconds). */
    public static final long DEEP_FREEZE_WINDOW_TICKS = 60 * 20;

    private RimeAdvancements() {}

    private static Identifier id(String name) {
        return Kegare.id("innate_technique/rime/" + name);
    }

    /** Only a player who currently holds Rime earns these (so the tab never opens for anyone else). */
    public static void award(ServerPlayer player, Identifier id) {
        if (Rime.holds(player)) KegareAdvancements.award(player, id);
    }
}
