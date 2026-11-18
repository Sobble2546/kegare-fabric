package com.cnethuka.kegare.platform.registry;

import net.minecraft.core.Holder;

/**
 * Minecraft 26.x: {@link Holder} is sealed (only Holder.Reference and Holder.Direct), so a DeferredHolder cannot BE one here, unlike on
 * 1.21.11. It still has {@link DeferredHolder#holder()}; code that needs a {@code Holder<T>} calls that, which works on every target.
 * (Checked by compiling against the 26.3 jar: "class is not allowed to extend sealed class: Holder".)
 */
abstract class HolderDelegate<R> {
    /** The real holder, once registered. */
    protected abstract Holder<R> delegate();

    public abstract R value();

    public abstract boolean isBound();
}
