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

    // Only the handles are made when this class is first used (so touching a key or a handle early is harmless); init() makes the Minecraft
    // registries behind them. A handle used before init() throws and says so, instead of failing somewhere else with a buried cause.
    public static final KRegistry<InnateTechnique> INNATE_TECHNIQUE = KRegistry.declare(INNATE_TECHNIQUE_KEY);
    public static final KRegistry<Trait> TRAIT = KRegistry.declare(TRAIT_KEY);

    private KegareRegistries() {}

    /** Makes both registries. Must run during mod initialisation (before registries freeze), before any DeferredRegister for them is applied. */
    public static void init() {
        INNATE_TECHNIQUE.bind();
        TRAIT.bind();
    }

    /** Kept for the old call; the registries are made in static initialisation. */
    public static void register(IEventBus modBus) {
        init();
    }
}
