package com.cnethuka.kegare.registry;

import com.cnethuka.kegare.Kegare;
import com.cnethuka.kegare.innatetechnique.InnateTechnique;
import com.cnethuka.kegare.platform.bus.IEventBus;
import com.cnethuka.kegare.platform.registry.KRegistry;
import com.cnethuka.kegare.trait.Trait;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * Custom registries. Innate techniques and traits are registry entries, so addons (or later Kegare
 * content) plug in with a plain {@code DeferredRegister} and no changes to the base.
 * Neither is synced to clients: every Kegare payload names an entry by its id, so the HUD resolves names and ability lists from the
 * client's own copy, and the hello (DESIGN §3.5) makes sure both sides run the same Kegare version.
 */
public final class KegareRegistries {
    public static final ResourceKey<Registry<InnateTechnique>> INNATE_TECHNIQUE_KEY = ResourceKey.createRegistryKey(Kegare.id("innate_technique"));
    public static final ResourceKey<Registry<Trait>> TRAIT_KEY = ResourceKey.createRegistryKey(Kegare.id("trait"));

    // Made when this class is first used; the first thing mod initialisation does is call init(), so that is before registries freeze.
    public static final KRegistry<InnateTechnique> INNATE_TECHNIQUE = KRegistry.create(INNATE_TECHNIQUE_KEY);
    public static final KRegistry<Trait> TRAIT = KRegistry.create(TRAIT_KEY);

    private KegareRegistries() {}

    /** Makes sure both registries exist. Must run during mod initialisation, before any DeferredRegister for them is applied. */
    public static void init() {}

    /** Kept for the old call; the registries are made in static initialisation. */
    public static void register(IEventBus modBus) {
        init();
    }
}
