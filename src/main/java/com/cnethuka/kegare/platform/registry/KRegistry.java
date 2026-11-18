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
 *   <li>{@link #declare}, then {@link #bind}: a real Minecraft registry made with Fabric's registry builder (never synced to clients, see
 *       {@link RegistryCompat}), which {@link #registry()} gives. Declaring only makes the handle; the Minecraft registry is made by
 *       {@code bind()}, which the owner calls explicitly during mod initialisation, before registries freeze. Using the handle before that
 *       throws an exception that says so. Once bound the registry is read without locking (a frozen Minecraft registry does not change);</li>
 *   <li>{@link #standalone}: just a map, for things Minecraft has no registry for (attachment types). Its map is guarded by its own lock.</li>
 * </ul>
 */
public class KRegistry<T> implements Iterable<T> {
    private static final Map<Identifier, KRegistry<?>> BY_KEY = new ConcurrentHashMap<>();

    private final ResourceKey<Registry<T>> key;
    /** True for a map-only registry; false for one that is (or will be) backed by a Minecraft registry. */
    private final boolean standalone;
    @Nullable
    private volatile Registry<T> backing;
    private final Map<ResourceKey<T>, T> own = new LinkedHashMap<>();

    protected KRegistry(ResourceKey<Registry<T>> key, @Nullable Registry<T> backing) {
        this.key = key;
        this.standalone = backing == null;
        this.backing = backing;
    }

    private KRegistry(ResourceKey<Registry<T>> key) {
        this.key = key;
        this.standalone = false;
    }

    /** The handle of a Minecraft registry of Kegare's own, not made yet: call {@link #bind} during mod initialisation. */
    public static <T> KRegistry<T> declare(ResourceKey<Registry<T>> key) {
        KRegistry<T> out = new KRegistry<>(key);
        BY_KEY.put(key.identifier(), out);
        return out;
    }

    /** Makes and registers the Minecraft registry behind a {@link #declare}d handle. Before registries freeze. Does nothing if already bound. */
    public synchronized void bind() {
        if (standalone) throw new UnsupportedOperationException(key + " is a standalone registry");
        if (backing == null) backing = RegistryCompat.createRegistry(key);
    }

    public static <T> KRegistry<T> standalone(ResourceKey<Registry<T>> key) {
        return new KRegistry<>(key, null);
    }

    /** The Kegare registry declared with {@link #declare} for this key, if any (it may not be bound yet). */
    @SuppressWarnings("unchecked")
    @Nullable
    static <T> KRegistry<T> byKey(ResourceKey<? extends Registry<T>> key) {
        return (KRegistry<T>) BY_KEY.get(key.identifier());
    }

    public ResourceKey<Registry<T>> key() {
        return key;
    }

    /** The Minecraft registry behind a bound one. Throws for a {@link #standalone} one, and for one not bound yet. */
    public Registry<T> registry() {
        if (standalone) throw new UnsupportedOperationException(key + " is not a Minecraft registry");
        return bound();
    }

    @Nullable
    Registry<T> backing() {
        return standalone ? null : bound();
    }

    private Registry<T> bound() {
        Registry<T> b = backing;
        if (b == null) {
            throw new IllegalStateException("The registry " + key.identifier() + " is used before it was made: its owner's init() must run first, "
                    + "during mod initialisation (it was probably touched too early)");
        }
        return b;
    }

    /** Adds a value to a standalone registry. */
    public void put(ResourceKey<T> entryKey, T value) {
        if (!standalone) throw new UnsupportedOperationException("Register into " + key + " through Registry.register");
        synchronized (own) {
            if (own.containsKey(entryKey)) throw new IllegalStateException("Duplicate registration: " + entryKey);
            own.put(entryKey, value);
        }
        onPut(entryKey, value);
    }

    /** Called when a value is added to a standalone registry. */
    protected void onPut(ResourceKey<T> entryKey, T value) {}

    /** The value with this id, or null. */
    @Nullable
    public T get(Identifier id) {
        if (!standalone) return bound().getValue(id);
        synchronized (own) {
            return own.get(ResourceKey.create(key, id));
        }
    }

    @Nullable
    public Identifier getKey(T value) {
        if (!standalone) return bound().getKey(value);
        synchronized (own) {
            for (Map.Entry<ResourceKey<T>, T> e : own.entrySet()) {
                if (e.getValue() == value) return e.getKey().identifier();
            }
        }
        return null;
    }

    public boolean containsKey(Identifier id) {
        if (!standalone) return bound().containsKey(id);
        synchronized (own) {
            return own.containsKey(ResourceKey.create(key, id));
        }
    }

    public Set<Identifier> keySet() {
        if (!standalone) return bound().keySet();
        Set<Identifier> out = new java.util.LinkedHashSet<>();
        synchronized (own) {
            own.keySet().forEach(k -> out.add(k.identifier()));
        }
        return Collections.unmodifiableSet(out);
    }

    /** Entries in registration order (a snapshot for a standalone registry). */
    public Set<Map.Entry<ResourceKey<T>, T>> entrySet() {
        if (!standalone) return bound().entrySet();
        Set<Map.Entry<ResourceKey<T>, T>> out = new java.util.LinkedHashSet<>();
        synchronized (own) {
            own.forEach((k, v) -> out.add(new AbstractMap.SimpleImmutableEntry<>(k, v)));
        }
        return Collections.unmodifiableSet(out);
    }

    public Collection<T> values() {
        if (!standalone) return bound().stream().toList();
        synchronized (own) {
            return java.util.List.copyOf(own.values());
        }
    }

    /** A stream of the values: over the Minecraft registry itself when backed (no copy), over a snapshot otherwise. */
    public Stream<T> stream() {
        return standalone ? values().stream() : bound().stream();
    }

    /** Iterates the Minecraft registry itself when backed (no copy per loop), a snapshot of the map otherwise. */
    @Override
    public Iterator<T> iterator() {
        return standalone ? values().iterator() : bound().iterator();
    }

    public int size() {
        if (!standalone) return bound().keySet().size();
        synchronized (own) {
            return own.size();
        }
    }
}
