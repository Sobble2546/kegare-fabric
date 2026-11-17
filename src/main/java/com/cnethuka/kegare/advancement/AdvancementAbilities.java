package com.cnethuka.kegare.advancement;

import com.cnethuka.kegare.api.ability.Ability;
import com.cnethuka.kegare.innate_technique.puppet_manipulation.PuppetAdvancements;
import com.cnethuka.kegare.innate_technique.puppet_manipulation.PuppetManipulation;
import com.cnethuka.kegare.innate_technique.rime.Rime;
import com.cnethuka.kegare.innate_technique.rime.RimeAdvancements;
import com.cnethuka.kegare.innate_technique.swap.Swap;
import com.cnethuka.kegare.innate_technique.swap.SwapAdvancements;
import com.cnethuka.kegare.innate_technique.time_manipulation.TimeAdvancements;
import com.cnethuka.kegare.innate_technique.time_manipulation.TimeManipulation;
import com.cnethuka.kegare.innatetechnique.change.ChangeSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Which abilities each advancement in Kegare's technique tabs depends on (DESIGN §3.34). When an addon replaces or tweaks one of
 * them, the advancement is turned off: never awarded again, and whoever already earned it keeps it. One that lists none is about the
 * technique as a whole (freezing, any swap) and stays on. Every advancement in a Kegare technique tab must be listed here, even with
 * none: {@code AdvancementAbilitiesTest} checks.
 */
public final class AdvancementAbilities {
    private static volatile Map<ResourceLocation, List<ResourceLocation>> map;

    private AdvancementAbilities() {}

    /** Every listed advancement and the abilities it depends on. */
    public static Map<ResourceLocation, List<ResourceLocation>> all() {
        Map<ResourceLocation, List<ResourceLocation>> m = map;
        if (m == null) map = m = build();
        return m;
    }

    private static Map<ResourceLocation, List<ResourceLocation>> build() {
        Map<ResourceLocation, List<ResourceLocation>> m = new LinkedHashMap<>();
        // Rime
        put(m, RimeAdvancements.COLD_OPEN, Rime.FROST_SHARD);
        put(m, RimeAdvancements.FULL_CHARGE, Rime.GLACIAL_LANCE);
        put(m, RimeAdvancements.HOME_FIELD);
        put(m, RimeAdvancements.SHATTER);
        put(m, RimeAdvancements.BRAIN_FREEZE);
        put(m, RimeAdvancements.THIN_ICE, Rime.RIME_SHELL);
        put(m, RimeAdvancements.COLD_BLOODED);
        put(m, RimeAdvancements.DEEP_FREEZE);
        put(m, RimeAdvancements.LONG_WAY_FROM_HOME);
        put(m, RimeAdvancements.SPLINTER, Rime.SPLINTER);
        put(m, RimeAdvancements.SLIP_N_SLIDE, Rime.ICE_PATH);
        put(m, RimeAdvancements.SKY_HIGH, Rime.GLACIAL_SPIRE, Rime.FROST_SHARD);
        put(m, RimeAdvancements.SHATTERED_PRESSURE, Rime.SHATTER_SHELL);
        // Swap
        put(m, SwapAdvancements.SWITCHEROO, Swap.POSITIONS);
        put(m, SwapAdvancements.NOT_ALLOWED);
        put(m, SwapAdvancements.RETURN_TO_SENDER, Swap.SEND_OFF);
        put(m, SwapAdvancements.WRONG_TARGET, Swap.AWARENESS);
        put(m, SwapAdvancements.TWO_OF_YOU, Swap.MIMIC);
        put(m, SwapAdvancements.FALL_GUY);
        put(m, SwapAdvancements.ROUND_TRIP, Swap.SWITCHBACK);
        put(m, SwapAdvancements.IDENTITY_CRISIS, Swap.TOTAL_SWAP);
        put(m, SwapAdvancements.NEWTONS_THIRD, Swap.MOMENTUM_EXCHANGE);
        // Puppet Manipulation
        put(m, PuppetAdvancements.PULLING_STRINGS, PuppetManipulation.PUPPET_CREATION);
        put(m, PuppetAdvancements.OUT_OF_BODY, PuppetManipulation.POSSESS);
        put(m, PuppetAdvancements.KURU_KURU, PuppetManipulation.PUPPET_CREATION);
        put(m, PuppetAdvancements.NOBODY_HOME, PuppetManipulation.POSSESS);
        put(m, PuppetAdvancements.DECOY, PuppetManipulation.PUPPET_CREATION);
        put(m, PuppetAdvancements.WHOS_REAL, PuppetManipulation.STAND_IN);
        put(m, PuppetAdvancements.CURIOSITY_KILLED, PuppetManipulation.CURIO_BURST);
        put(m, PuppetAdvancements.LITTLE_ME, PuppetManipulation.POSSESS);
        put(m, PuppetAdvancements.GET_OVER_HERE, PuppetManipulation.STRINGS);
        put(m, PuppetAdvancements.TIED_UP, PuppetManipulation.STRINGS);
        put(m, PuppetAdvancements.WALKIES, PuppetManipulation.ZIP_LEASH);
        put(m, PuppetAdvancements.STRINGS_ATTACHED, PuppetManipulation.STRINGS, PuppetManipulation.CURIO_BURST);
        // Time Manipulation
        put(m, TimeAdvancements.THE_WORLD_STOOD_STILL, TimeManipulation.STANDSTILL);
        put(m, TimeAdvancements.OVERCLOCK, TimeManipulation.ACCELERATE);
        put(m, TimeAdvancements.SECOND_CHANCE, TimeManipulation.POINT_MAKER);
        put(m, TimeAdvancements.SLOW_MOTION, TimeManipulation.DECELERATE);
        put(m, TimeAdvancements.NOT_SO_FAST, TimeManipulation.REWIND);
        put(m, TimeAdvancements.FROZEN_IN_FLIGHT, TimeManipulation.STASIS_FIELD);
        put(m, TimeAdvancements.SAME_CLOCK, TimeManipulation.STANDSTILL);
        put(m, TimeAdvancements.PRE_PLANNED, TimeManipulation.STANDSTILL);
        return Map.copyOf(m);
    }

    private static void put(Map<ResourceLocation, List<ResourceLocation>> m, ResourceLocation advancement, Ability... abilities) {
        if (m.put(advancement, Arrays.stream(abilities).map(Ability::id).toList()) != null) throw new IllegalStateException("Listed twice: " + advancement);
    }

    /** Whether this advancement is turned off because an ability it depends on is in {@code changed}. */
    public static boolean isTurnedOff(ResourceLocation advancement, Set<ResourceLocation> changed) {
        if (changed.isEmpty()) return false;
        List<ResourceLocation> needs = all().get(advancement);
        if (needs == null) return false;
        for (ResourceLocation ability : needs) if (changed.contains(ability)) return true;
        return false;
    }

    /** Whether this advancement is turned off by the game's changes. */
    public static boolean isTurnedOff(ResourceLocation advancement) {
        return isTurnedOff(advancement, ChangeSet.GLOBAL.changed());
    }

    /** Every advancement {@code changed} turns off. */
    public static List<ResourceLocation> turnedOff(Collection<ResourceLocation> changed) {
        Set<ResourceLocation> set = Set.copyOf(changed);
        List<ResourceLocation> out = new ArrayList<>();
        for (ResourceLocation advancement : all().keySet()) if (isTurnedOff(advancement, set)) out.add(advancement);
        return out;
    }
}
