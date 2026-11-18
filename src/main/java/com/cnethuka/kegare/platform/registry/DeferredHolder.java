package com.cnethuka.kegare.platform.registry;

import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * NeoForge's DeferredHolder: a registry entry that exists before the registry is filled. {@link #getId()} and {@link #getKey()} work at
 * once; {@link #get()} and {@link #value()} (and every {@link Holder} method) only after the {@link DeferredRegister} was applied.
 * It is a {@code Supplier<T>}, and on 1.21.11 also a {@code Holder<R>} (via {@link HolderDelegate}, the one class that follows the Minecraft
 * version). On 26.x Holder is sealed, so nothing can implement it: code that needs a {@code Holder} must call {@link #holder()}, which works
 * on every target. Use that for any new code.
 */
public class DeferredHolder<R, T extends R> extends HolderDelegate<R> implements Supplier<T> {
    private final ResourceKey<R> key;
    @Nullable
    private volatile Holder<R> bound;
    @Nullable
    private volatile T value;

    public DeferredHolder(ResourceKey<R> key) {
        this.key = key;
    }

    public static <R, T extends R> DeferredHolder<R, T> create(ResourceKey<R> key) {
        return new DeferredHolder<>(key);
    }

    public Identifier getId() {
        return key.identifier();
    }

    public ResourceKey<R> getKey() {
        return key;
    }

    /** Called when the entry is registered. */
    void bind(T value, Holder<R> holder) {
        this.value = value;
        this.bound = holder;
    }

    /** The registered holder itself (a Holder.Reference). Throws before registration. */
    public Holder<R> holder() {
        return delegate();
    }

    /** A holder that carries the value itself instead of pointing into the registry, so the value, not a registry number, is what a packet holds. */
    public Holder<R> directHolder() {
        return Holder.<R>direct(value());
    }

    @Override
    public boolean isBound() {
        return bound != null;
    }

    @Override
    public T get() {
        return value();
    }

    @Override
    public T value() {
        T out = value;
        if (out == null) throw new NullPointerException("Trying to access unbound value: " + key);
        return out;
    }

    @Override
    protected Holder<R> delegate() {
        Holder<R> out = bound;
        if (out == null) throw new NullPointerException("Trying to access unbound holder: " + key);
        return out;
    }

    @Override
    public String toString() {
        return "DeferredHolder{" + key + "}";
    }
}
