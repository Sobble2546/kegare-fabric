package com.cnethuka.kegare.platform.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.Test;

/** DeferredRegister over a standalone registry (a plain map), so no game is needed. */
class DeferredRegisterTest {
    private static ResourceKey<Registry<String>> key(String name) {
        return ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("kegaretest", name));
    }

    @Test
    void idsWorkBeforeRegistrationAndValuesAfter() {
        KRegistry<String> registry = KRegistry.standalone(key("a"));
        DeferredRegister<String> register = DeferredRegister.create(registry, "kegaretest");
        DeferredHolder<String, String> one = register.register("one", () -> "1");

        assertEquals(Identifier.fromNamespaceAndPath("kegaretest", "one"), one.getId());
        assertEquals("one", one.getKey().identifier().getPath());
        assertFalse(one.isBound());
        assertThrows(NullPointerException.class, one::get);
        assertThrows(NullPointerException.class, one::value);
        assertNull(registry.get(one.getId()));

        register.register(null);
        assertTrue(one.isBound());
        assertEquals("1", one.get());
        assertEquals("1", one.value());
        assertEquals("1", registry.get(one.getId()));
        assertEquals(one.getId(), registry.getKey("1"));
        assertTrue(registry.containsKey(one.getId()));
    }

    @Test
    void entriesAreRegisteredInDeclarationOrderAndOnlyOnce() {
        KRegistry<String> registry = KRegistry.standalone(key("b"));
        DeferredRegister<String> register = DeferredRegister.create(registry, "kegaretest");
        AtomicInteger made = new AtomicInteger();
        register.register("z", () -> "z" + made.incrementAndGet());
        register.register("a", () -> "a" + made.incrementAndGet());
        register.register("m", () -> "m" + made.incrementAndGet());

        register.register(null);
        register.register(null);
        assertEquals(3, made.get(), "a second register(bus) does nothing");
        assertEquals(List.of("z1", "a2", "m3"), registry.stream().toList());
        assertEquals(List.of("z", "a", "m"), register.getEntries().stream().map(h -> h.getId().getPath()).toList());
        assertEquals(3, registry.size());
    }

    @Test
    void aDuplicateNameOrALateEntryIsRefused() {
        KRegistry<String> registry = KRegistry.standalone(key("c"));
        DeferredRegister<String> register = DeferredRegister.create(registry, "kegaretest");
        register.register("x", () -> "x");
        assertThrows(IllegalArgumentException.class, () -> register.register("x", () -> "again"));
        register.register(null);
        assertThrows(IllegalStateException.class, () -> register.register("late", () -> "late"));
    }

    @Test
    void theHolderCanBeUsedAsASupplierAndItsKeyFactoryGetsItsKey() {
        KRegistry<String> registry = KRegistry.standalone(key("d"));
        DeferredRegister<String> register = DeferredRegister.create(registry, "kegaretest");
        DeferredHolder<String, String> keyed = register.registerKeyed("named", k -> "made for " + k.identifier());
        java.util.function.Supplier<String> asSupplier = keyed;
        register.register(null);
        assertEquals("made for kegaretest:named", asSupplier.get());
        assertSame(keyed.value(), keyed.get());
    }

    @Test
    void holderGivesARealHolderAndDirectHolderOneThatCarriesTheValue() {
        KRegistry<String> registry = KRegistry.standalone(key("f"));
        DeferredRegister<String> register = DeferredRegister.create(registry, "kegaretest");
        DeferredHolder<String, String> entry = register.register("h", () -> "value");
        assertThrows(NullPointerException.class, entry::holder);
        register.register(null);
        assertEquals("value", entry.holder().value());
        assertEquals("value", entry.directHolder().value());
        assertEquals(net.minecraft.core.Holder.Kind.DIRECT, entry.directHolder().kind());
    }

    @Test
    void aStandaloneRegistryRefusesDuplicatesAndKnowsItsKeys() {
        KRegistry<String> registry = KRegistry.standalone(key("e"));
        ResourceKey<String> k = ResourceKey.create(registry.key(), Identifier.fromNamespaceAndPath("kegaretest", "k"));
        registry.put(k, "v");
        assertThrows(IllegalStateException.class, () -> registry.put(k, "w"));
        assertEquals(java.util.Set.of(k.identifier()), registry.keySet());
        assertEquals("v", registry.entrySet().iterator().next().getValue());
        assertThrows(UnsupportedOperationException.class, registry::registry);
    }
}
