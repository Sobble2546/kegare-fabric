package com.cnethuka.kegare.platform.registry;

import com.cnethuka.kegare.platform.bus.IEventBus;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/**
 * NeoForge's DeferredRegister, kept call-compatible: declare entries in static fields, then {@link #register(IEventBus)} once.
 * NeoForge fills the registry later from a RegisterEvent; Fabric registries are open during mod initialisation, so
 * {@code register(IEventBus)} registers everything right there, in declaration order. The bus is not used. Calling it again does nothing.
 * <p>
 * Entries land in a Minecraft registry (vanilla ones, or Kegare's own, bound by {@link KRegistry#bind}) or in a {@link KRegistry#standalone}
 * map (attachment types).
 */
public class DeferredRegister<T> {
    private final ResourceKey<? extends Registry<T>> registryKey;
    @Nullable
    private final KRegistry<T> standalone;
    private final String namespace;
    private final Map<Identifier, Entry<T>> entries = new LinkedHashMap<>();
    private final List<Identifier[]> aliases = new ArrayList<>();
    private boolean applied;

    private record Entry<T>(DeferredHolder<T, ? extends T> holder, Function<ResourceKey<T>, ? extends T> factory) {}

    protected DeferredRegister(ResourceKey<? extends Registry<T>> registryKey, @Nullable KRegistry<T> standalone, String namespace) {
        this.registryKey = registryKey;
        this.standalone = standalone;
        this.namespace = namespace;
    }

    public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        return new DeferredRegister<>(registryKey, null, namespace);
    }

    /** Into a registry of Kegare's own making ({@link NeoForgeRegistries#ATTACHMENT_TYPES}). */
    public static <T> DeferredRegister<T> create(KRegistry<T> registry, String namespace) {
        return new DeferredRegister<>(registry.key(), registry, namespace);
    }

    public static Items createItems(String namespace) {
        return new Items(namespace);
    }

    protected final String namespace() {
        return namespace;
    }

    protected final ResourceKey<? extends Registry<T>> registryKey() {
        return registryKey;
    }

    @SuppressWarnings("unchecked")
    protected final ResourceKey<T> entryKey(String name) {
        return (ResourceKey<T>) ResourceKey.create(registryKey, Identifier.fromNamespaceAndPath(namespace, name));
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Supplier<? extends I> factory) {
        return registerKeyed(name, key -> factory.get());
    }

    /** Like {@link #register} for entries that need their own key to be built (EntityType.Builder#build takes one). */
    public <I extends T> DeferredHolder<T, I> registerKeyed(String name, Function<ResourceKey<T>, ? extends I> factory) {
        DeferredHolder<T, I> holder = new DeferredHolder<>(entryKey(name));
        return add(holder, factory);
    }

    protected final <I extends T, H extends DeferredHolder<T, I>> H add(H holder, Function<ResourceKey<T>, ? extends I> factory) {
        if (applied) throw new IllegalStateException("Registered " + holder.getKey() + " after " + registryKey + " was filled");
        if (entries.putIfAbsent(holder.getId(), new Entry<>(holder, factory)) != null) throw new IllegalArgumentException("Duplicate registration " + holder.getId());
        return holder;
    }

    /** Old id to new id, for saves that still have the old one. Applied when the entries are. */
    public void addAlias(Identifier from, Identifier to) {
        aliases.add(new Identifier[] {from, to});
    }

    /** Everything declared so far, in order. */
    public Collection<DeferredHolder<T, ? extends T>> getEntries() {
        List<DeferredHolder<T, ? extends T>> out = new ArrayList<>();
        entries.values().forEach(e -> out.add(e.holder()));
        return Collections.unmodifiableList(out);
    }

    /** Fills the registry with what was declared. {@code bus} is accepted for NeoForge's signature and not used. */
    public synchronized void register(IEventBus bus) {
        if (applied) return;
        applied = true;
        Registry<T> backing = null;
        if (standalone == null) {
            KRegistry<T> own = KRegistry.byKey(registryKey);
            backing = own != null ? own.backing() : RegistryCompat.lookup(registryKey);
        }
        for (Entry<T> entry : entries.values()) apply(entry, backing);
        for (Identifier[] alias : aliases) {
            if (backing == null) throw new IllegalStateException("Aliases need a Minecraft registry: " + registryKey);
            RegistryCompat.addAlias(backing, alias[0], alias[1]);
        }
    }

    @SuppressWarnings("unchecked")
    private <I extends T> void apply(Entry<T> entry, @Nullable Registry<T> backing) {
        DeferredHolder<T, I> holder = (DeferredHolder<T, I>) entry.holder();
        ResourceKey<T> key = holder.getKey();
        I value = (I) entry.factory().apply(key);
        Holder<T> registered;
        if (standalone != null) {
            standalone.put(key, value);
            registered = Holder.direct(value);
        } else {
            registered = Registry.registerForHolder(backing, key, value);
        }
        holder.bind(value, registered);
    }

    /** NeoForge's item register: sets each item's id on its properties first (every item needs one since 1.21.2). */
    public static class Items extends DeferredRegister<Item> {
        protected Items(String namespace) {
            super(net.minecraft.core.registries.Registries.ITEM, null, namespace);
        }

        public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> factory, Item.Properties properties) {
            return add(new DeferredItem<I>(entryKey(name)), key -> factory.apply(properties.setId(key)));
        }

        public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> factory) {
            return registerItem(name, factory, new Item.Properties());
        }

        public DeferredItem<Item> registerSimpleItem(String name, Item.Properties properties) {
            return registerItem(name, Item::new, properties);
        }

        public DeferredItem<Item> registerSimpleItem(String name) {
            return registerSimpleItem(name, new Item.Properties());
        }
    }
}
