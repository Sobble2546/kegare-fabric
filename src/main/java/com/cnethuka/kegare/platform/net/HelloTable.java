package com.cnethuka.kegare.platform.net;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** The server's side of the id table: every Kegare entry of the registries Fabric syncs and Via cannot translate for us. */
final class HelloTable {
    static final String NAMESPACE = "kegare";

    private HelloTable() {}

    /** Entity types, items and sound events whose namespace is Kegare's, with the raw id this server numbers them with. */
    static List<KegareHello.Entry> entries() {
        List<KegareHello.Entry> out = new ArrayList<>();
        collect(BuiltInRegistries.ENTITY_TYPE, out);
        collect(BuiltInRegistries.ITEM, out);
        collect(BuiltInRegistries.SOUND_EVENT, out);
        return List.copyOf(out);
    }

    private static <T> void collect(Registry<T> registry, List<KegareHello.Entry> out) {
        Identifier registryId = registry.key().identifier();
        for (Identifier name : registry.keySet()) {
            if (!NAMESPACE.equals(name.getNamespace())) continue;
            out.add(new KegareHello.Entry(registryId, name, registry.getId(registry.getValue(name))));
        }
    }
}
