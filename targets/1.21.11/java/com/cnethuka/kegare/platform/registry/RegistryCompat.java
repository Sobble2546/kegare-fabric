package com.cnethuka.kegare.platform.registry;

import net.fabricmc.fabric.api.event.registry.FabricRegistry;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

/** Minecraft 1.21.11 / Fabric API 0.141: the registry spellings that change with the version, in one place. */
public final class RegistryCompat {
    private RegistryCompat() {}

    /**
     * A new Minecraft registry for Kegare's own content. Not SYNCED: clients never get its contents over the wire (every Kegare payload names
     * entries by id, never by raw number), and a matching Kegare version is checked by the hello instead.
     */
    public static <T> MappedRegistry<T> createRegistry(ResourceKey<Registry<T>> key) {
        return FabricRegistryBuilder.createSimple(key).buildAndRegister();
    }

    /** A registry of the game's, by key. */
    @SuppressWarnings("unchecked")
    public static <T> Registry<T> lookup(ResourceKey<? extends Registry<T>> key) {
        Registry<?> found = BuiltInRegistries.REGISTRY.getValue(key.identifier());
        if (found == null) throw new IllegalStateException("No registry " + key.identifier());
        return (Registry<T>) found;
    }

    public static void addAlias(Registry<?> registry, Identifier from, Identifier to) {
        ((FabricRegistry) registry).addAlias(from, to);
    }

    /** A creative tab builder that Fabric places after the vanilla tabs. */
    public static CreativeModeTab.Builder tabBuilder() {
        return net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder();
    }
}
