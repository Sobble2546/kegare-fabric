package com.cnethuka.kegare.api.change;

import com.cnethuka.kegare.api.ability.Ability;
import com.cnethuka.kegare.innatetechnique.change.ChangeSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModLoadingContext;

/**
 * Changes to innate techniques that already exist, Kegare's or another addon's: add an ability, replace one, or tweak one. Register
 * them from your mod's constructor; once the technique registry is complete they're applied in one place, to every technique, and
 * registering more throws.
 * <p>
 * Every change that can't apply stops the game at startup with a message naming it: two addons replacing the same ability, an
 * unknown technique or ability, a replacement that doesn't fit the slot. Tweaks from several addons stack, in registration order.
 * <p>
 * <b>Replacing or tweaking an ability turns off Kegare's advancements that depend on it</b> (they're never awarded again; anyone who
 * already earned one keeps it). Adding an ability changes nothing that exists, so it turns nothing off. Your addon can ship its own
 * advancements instead (see {@link com.cnethuka.kegare.api.KegareApi#techniqueTab}).
 * <p>
 * Clients must have the same changes as the server: a player whose addons differ is refused when joining, with a message saying so.
 */
public final class TechniqueChanges {
    private TechniqueChanges() {}

    /**
     * Adds an ordinary ability (one with a number key) at the end of a technique's list. Abilities already there keep their
     * Refinement unlock levels; this one unlocks at {@code unlockLevel}.
     */
    public static void add(ResourceLocation technique, Ability ability, int unlockLevel) {
        ChangeSet.GLOBAL.add(technique, null, ability, unlockLevel, null, by());
    }

    /** {@link #add(ResourceLocation, Ability, int)} for an ability with a special: the special unlocks at {@code specialUnlockLevel}. */
    public static void add(ResourceLocation technique, Ability ability, int unlockLevel, int specialUnlockLevel) {
        ChangeSet.GLOBAL.add(technique, null, ability, unlockLevel, specialUnlockLevel, by());
    }

    /**
     * Adds an ordinary ability right after {@code after} (an ability already in that technique's list). Several added after the
     * same one keep the order they were registered in.
     */
    public static void addAfter(ResourceLocation technique, ResourceLocation after, Ability ability, int unlockLevel) {
        ChangeSet.GLOBAL.add(technique, after, ability, unlockLevel, null, by());
    }

    /** {@link #addAfter(ResourceLocation, ResourceLocation, Ability, int)} for an ability with a special. */
    public static void addAfter(ResourceLocation technique, ResourceLocation after, Ability ability, int unlockLevel, int specialUnlockLevel) {
        ChangeSet.GLOBAL.add(technique, after, ability, unlockLevel, specialUnlockLevel, by());
    }

    /**
     * Replaces the ability with the same ID as {@code replacement}, wherever it is (an ordinary ability, a special or an ultimate,
     * Kegare's or an addon's). It keeps that ability's slot, unlock level and cooldown, so loadouts and saved data keep working. A
     * special must be replaced by a special of the same base; anything else by a non-special. Its own {@code special()} is the special
     * players get (a new special ID unlocks at the level the old special did).
     */
    public static void replace(Ability replacement) {
        ChangeSet.GLOBAL.replace(replacement, by());
    }

    /** Changes the numbers or behaviour of the ability with this ID (an ordinary ability, a special or an ultimate). */
    public static void tweak(ResourceLocation ability, AbilityTweak tweak) {
        ChangeSet.GLOBAL.tweak(ability, tweak, by());
    }

    /** Whether an addon replaced or tweaked the ability with this ID. */
    public static boolean isChanged(ResourceLocation ability) {
        return ChangeSet.GLOBAL.isChanged(ability);
    }

    /** Every ability ID an addon replaced or tweaked. */
    public static Set<ResourceLocation> changedAbilities() {
        return ChangeSet.GLOBAL.changed();
    }

    /** The mod registering the change, for the messages that name it. */
    private static String by() {
        return ModLoadingContext.get().getActiveNamespace();
    }
}
