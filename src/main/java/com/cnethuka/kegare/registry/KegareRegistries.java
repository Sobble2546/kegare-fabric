package com.cnethuka.kegare.registry;

import com.cnethuka.kegare.api.technique.InnateTechnique;
import com.cnethuka.kegare.api.trait.Trait;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * Custom registries. Innate techniques and traits are registry entries, so addons (or later Kegare
 * content) plug in with a plain {@code DeferredRegister} and no changes to the base. Their keys are public in
 * {@link com.cnethuka.kegare.api.KegareApi}.
 * Both registries sync to clients, so the HUD can resolve names and ability lists by ID.
 */
public final class KegareRegistries {
    public static final ResourceKey<Registry<InnateTechnique>> INNATE_TECHNIQUE_KEY = com.cnethuka.kegare.api.KegareApi.INNATE_TECHNIQUES;
    public static final ResourceKey<Registry<Trait>> TRAIT_KEY = com.cnethuka.kegare.api.KegareApi.TRAITS;

    /** Once it's complete, addons' changes are applied to every technique in one place (DESIGN §3.34). */
    public static final Registry<InnateTechnique> INNATE_TECHNIQUE = new RegistryBuilder<>(INNATE_TECHNIQUE_KEY).sync(true)
            .onBake(com.cnethuka.kegare.innatetechnique.change.TechniqueViews::bake).create();
    public static final Registry<Trait> TRAIT = new RegistryBuilder<>(TRAIT_KEY).sync(true).create();

    private KegareRegistries() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(NewRegistryEvent.class, event -> {
            event.register(INNATE_TECHNIQUE);
            event.register(TRAIT);
        });
    }
}
