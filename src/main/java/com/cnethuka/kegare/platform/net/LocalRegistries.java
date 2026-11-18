package com.cnethuka.kegare.platform.net;

import java.util.OptionalInt;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** This game's registries, for {@link IdRemap}. */
final class LocalRegistries implements IdRemap.Local {
    @Override
    public OptionalInt idOf(Identifier registry, Identifier name) {
        Registry<?> found = BuiltInRegistries.REGISTRY.getValue(registry);
        return found == null ? OptionalInt.empty() : idIn(found, name);
    }

    private static <T> OptionalInt idIn(Registry<T> registry, Identifier name) {
        // getValue on a defaulted registry answers its default for an unknown name, so ask first
        return registry.containsKey(name) ? OptionalInt.of(registry.getId(registry.getValue(name))) : OptionalInt.empty();
    }

    @Override
    public int foreignCount(Identifier registry) {
        Registry<?> found = BuiltInRegistries.REGISTRY.getValue(registry);
        if (found == null) return 0;
        int n = 0;
        for (Identifier name : found.keySet()) if (!HelloTable.NAMESPACE.equals(name.getNamespace())) n++;
        return n;
    }
}
