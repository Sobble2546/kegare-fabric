package com.cnethuka.kegare.platform.registry;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.registry.FabricRegistry;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

/** Minecraft 26.x (unobfuscated). Names checked against the 26.3 jars of registry-sync 8.0.5 and creative-tab 5.0.22, not compiled: the registry spellings that change with the version, in one place. */
public final class RegistryCompat {
    private RegistryCompat() {}

    /**
     * A new Minecraft registry for Kegare's own content. Not SYNCED: clients never get its contents over the wire (every Kegare payload names
     * entries by id, never by raw number), and a matching Kegare version is checked by the hello instead.
     */
    public static <T> MappedRegistry<T> createRegistry(ResourceKey<Registry<T>> key) {
        return FabricRegistryBuilder.create(key).buildAndRegister();
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
        return FabricCreativeModeTab.builder();
    }
}
