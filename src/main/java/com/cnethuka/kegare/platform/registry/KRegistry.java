package com.cnethuka.kegare.platform.registry;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * A registry as Kegare's code reads it, shaped like NeoForge's {@code Registry}: {@code get(id)} returns the value (or null), not an Optional
 * of a holder as Minecraft's does since 1.21.2. Two kinds:
 * <ul>
 *   <li>{@link #create}: a real Minecraft registry made with Fabric's registry builder (never synced to clients, see {@link RegistryCompat}),
 *       which {@link #registry()} gives;</li>
 *   <li>{@link #standalone}: just a map, for things Minecraft has no registry for (attachment types).</li>
 * </ul>
 */
public class KRegistry<T> implements Iterable<T> {
    private static final Map<Identifier, KRegistry<?>> BY_KEY = new ConcurrentHashMap<>();

    private final ResourceKey<Registry<T>> key;
    @Nullable
    private final Registry<T> backing;
    private final Map<ResourceKey<T>, T> own = new LinkedHashMap<>();

    protected KRegistry(ResourceKey<Registry<T>> key, @Nullable Registry<T> backing) {
        this.key = key;
        this.backing = backing;
    }

    /** Makes and registers a Minecraft registry. Call during mod initialisation, before registries freeze. */
    public static <T> KRegistry<T> create(ResourceKey<Registry<T>> key) {
        KRegistry<T> out = new KRegistry<>(key, RegistryCompat.createRegistry(key));
        BY_KEY.put(key.identifier(), out);
        return out;
    }

    public static <T> KRegistry<T> standalone(ResourceKey<Registry<T>> key) {
        return new KRegistry<>(key, null);
    }

    /** The Kegare registry made with {@link #create} for this key, if any. */
    @SuppressWarnings("unchecked")
    @Nullable
    static <T> KRegistry<T> byKey(ResourceKey<? extends Registry<T>> key) {
        return (KRegistry<T>) BY_KEY.get(key.identifier());
    }

    public ResourceKey<Registry<T>> key() {
        return key;
    }

    /** The Minecraft registry behind it. Throws for a {@link #standalone} one. */
    public Registry<T> registry() {
        if (backing == null) throw new UnsupportedOperationException(key + " is not a Minecraft registry");
        return backing;
    }

    @Nullable
    Registry<T> backing() {
        return backing;
    }

    /** Adds a value to a standalone registry. */
    public synchronized void put(ResourceKey<T> entryKey, T value) {
        if (backing != null) throw new UnsupportedOperationException("Register into " + key + " through Registry.register");
        if (own.containsKey(entryKey)) throw new IllegalStateException("Duplicate registration: " + entryKey);
        own.put(entryKey, value);
        onPut(entryKey, value);
    }

    /** Called when a value is added to a standalone registry. */
    protected void onPut(ResourceKey<T> entryKey, T value) {}

    /** The value with this id, or null. */
    @Nullable
    public synchronized T get(Identifier id) {
        if (backing != null) return backing.getValue(id);
        return own.get(ResourceKey.create(key, id));
    }

    @Nullable
    public synchronized Identifier getKey(T value) {
        if (backing != null) return backing.getKey(value);
        for (Map.Entry<ResourceKey<T>, T> e : own.entrySet()) {
            if (e.getValue() == value) return e.getKey().identifier();
        }
        return null;
    }

    public synchronized boolean containsKey(Identifier id) {
        if (backing != null) return backing.containsKey(id);
        return own.containsKey(ResourceKey.create(key, id));
    }

    public synchronized Set<Identifier> keySet() {
        if (backing != null) return backing.keySet();
        Set<Identifier> out = new java.util.LinkedHashSet<>();
        own.keySet().forEach(k -> out.add(k.identifier()));
        return Collections.unmodifiableSet(out);
    }

    /** Entries in registration order (a snapshot). */
    public synchronized Set<Map.Entry<ResourceKey<T>, T>> entrySet() {
        if (backing != null) return backing.entrySet();
        Set<Map.Entry<ResourceKey<T>, T>> out = new java.util.LinkedHashSet<>();
        own.forEach((k, v) -> out.add(new AbstractMap.SimpleImmutableEntry<>(k, v)));
        return Collections.unmodifiableSet(out);
    }

    public synchronized Collection<T> values() {
        if (backing != null) return backing.stream().toList();
        return java.util.List.copyOf(own.values());
    }

    public Stream<T> stream() {
        return values().stream();
    }

    @Override
    public Iterator<T> iterator() {
        return values().iterator();
    }

    public synchronized int size() {
        return backing != null ? backing.keySet().size() : own.size();
    }
}
