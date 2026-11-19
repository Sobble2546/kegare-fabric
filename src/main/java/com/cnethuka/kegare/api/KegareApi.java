package com.cnethuka.kegare.api;

import com.cnethuka.kegare.api.ability.Ability;
import com.cnethuka.kegare.api.technique.InnateTechnique;
import com.cnethuka.kegare.api.trait.Trait;
import com.cnethuka.kegare.loadout.SharedAbilities;
import com.cnethuka.kegare.loadout.SharedAbilityService;
import com.cnethuka.kegare.registry.KegareRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Where an addon starts: Kegare's registries and its shared abilities. See {@code docs/addon-api.md} for a walkthrough.
 * <p>
 * New innate techniques and traits are ordinary registry entries: register them with
 * {@code Registry.register(KegareApi.innateTechniques(), id, technique)} (or {@link #traits()}) from your mod's {@code onInitialize}.
 * Everything here runs on the logical server unless it says otherwise. The registries are not synced to clients: install your addon
 * on both sides (a client whose techniques or changes differ from the server's is turned away when joining).
 */
public final class KegareApi {
    /** Kegare's mod ID: declare a dependency on it ({@code "depends": {"kegare": "*"}}) in your {@code fabric.mod.json}. */
    public static final String MOD_ID = "kegare";

    /** The innate technique registry ({@code kegare:innate_technique}). */
    public static final ResourceKey<Registry<InnateTechnique>> INNATE_TECHNIQUES =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(MOD_ID, "innate_technique"));

    /** The trait registry ({@code kegare:trait}). */
    public static final ResourceKey<Registry<Trait>> TRAITS =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(MOD_ID, "trait"));

    private KegareApi() {}

    /**
     * Every registered innate technique, Kegare's and every addon's. Safe on both sides. Fabric runs mods' initialisers in no set
     * order, so the registry is made here if Kegare's own initialiser hasn't run yet.
     */
    public static Registry<InnateTechnique> innateTechniques() {
        KegareRegistries.INNATE_TECHNIQUE.bind();
        return KegareRegistries.INNATE_TECHNIQUE.registry();
    }

    /** Every registered trait, Kegare's and every addon's. Safe on both sides; made here if need be, as {@link #innateTechniques()}. */
    public static Registry<Trait> traits() {
        KegareRegistries.TRAIT.bind();
        return KegareRegistries.TRAIT.registry();
    }

    /** True for a technique Kegare itself ships; false for one an addon registered. */
    public static boolean isKegares(InnateTechnique technique) {
        return MOD_ID.equals(technique.id().getNamespace());
    }

    /**
     * Registers a shared ability (one in the Malice or Melee tab, not tied to any technique: {@link Ability#category()}). Players
     * unlock it once, with {@link #unlockSharedAbility}, and keep it through rerolls. Call from your mod's {@code onInitialize}; the
     * order of registration is the order on the Sorcerer screen. Its ID must be unique.
     */
    public static <T extends Ability> T registerSharedAbility(T ability) {
        return SharedAbilities.register(ability);
    }

    /** Like {@link #registerSharedAbility}, but every Malice user has it from the start, with nothing to unlock. */
    public static <T extends Ability> T registerSharedAbilityForEveryone(T ability) {
        return SharedAbilities.registerForEveryone(ability);
    }

    /** Unlocks a shared ability for this player (server only). False if they already had it or can't have it (Severed). */
    public static boolean unlockSharedAbility(ServerPlayer player, Ability ability) {
        return SharedAbilityService.unlock(player, ability);
    }

    /**
     * The root of a technique's advancement tab ({@code <namespace>:innate_technique/<path>}): parent your own advancements to it to
     * put them in that tab. Kegare's techniques have one; an addon's has one if the addon ships it.
     */
    public static Identifier techniqueTab(Identifier technique) {
        return com.cnethuka.kegare.advancement.KegareAdvancements.innateTechniqueRoot(technique);
    }

    /** Awards an advancement (every criterion), if it exists and isn't done yet. Server only. */
    public static void awardAdvancement(ServerPlayer player, Identifier advancement) {
        com.cnethuka.kegare.advancement.KegareAdvancements.awardIfPresent(player, advancement);
    }

    /** Whether this player has unlocked this shared ability (or has it as everyone does). */
    public static boolean hasSharedAbility(ServerPlayer player, Ability ability) {
        return SharedAbilityService.isUnlocked(player, ability.id());
    }
}
