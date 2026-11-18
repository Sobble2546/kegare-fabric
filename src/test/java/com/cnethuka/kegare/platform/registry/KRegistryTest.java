package com.cnethuka.kegare.platform.registry;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.Test;

/** A declared Minecraft registry is only a handle until it is bound; using it earlier says what is wrong. */
class KRegistryTest {
    private static final ResourceKey<Registry<String>> KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("kegaretest", "unbound"));

    @Test
    void aDeclaredRegistryUsedBeforeItIsMadeSaysSo() {
        KRegistry<String> registry = KRegistry.declare(KEY);
        Identifier id = Identifier.fromNamespaceAndPath("kegaretest", "x");
        for (Runnable use : new Runnable[] {() -> registry.get(id), () -> registry.registry(), registry::iterator, registry::stream,
                () -> registry.containsKey(id), registry::values}) {
            IllegalStateException e = assertThrows(IllegalStateException.class, use::run);
            assertTrue(e.getMessage().contains("kegaretest:unbound") && e.getMessage().contains("init()"), e.getMessage());
        }
    }

    @Test
    void aStandaloneRegistryCannotBeBound() {
        assertThrows(UnsupportedOperationException.class, () ->
                KRegistry.standalone(ResourceKey.<String>createRegistryKey(Identifier.fromNamespaceAndPath("kegaretest", "map"))).bind());
    }
}
