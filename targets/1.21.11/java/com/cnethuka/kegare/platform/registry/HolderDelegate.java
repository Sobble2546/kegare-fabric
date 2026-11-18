package com.cnethuka.kegare.platform.registry;

import com.mojang.datafixers.util.Either;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

/**
 * Minecraft 1.21.11: a {@link Holder} that forwards everything to {@link #delegate()} (the real holder, once registered). It is the one place
 * that lists Holder's methods, which change between Minecraft versions (26.x adds components), so the shared DeferredHolder doesn't.
 */
abstract class HolderDelegate<R> implements Holder<R> {
    protected abstract Holder<R> delegate();

    @Override
    public R value() {
        return delegate().value();
    }

    @Override
    public boolean isBound() {
        return delegate().isBound();
    }

    @Override
    public boolean is(Identifier id) {
        return delegate().is(id);
    }

    @Override
    public boolean is(ResourceKey<R> key) {
        return delegate().is(key);
    }

    @Override
    public boolean is(Predicate<ResourceKey<R>> predicate) {
        return delegate().is(predicate);
    }

    @Override
    public boolean is(TagKey<R> tag) {
        return delegate().is(tag);
    }

    @Override
    public boolean is(Holder<R> other) {
        return delegate().is(other);
    }

    @Override
    public Stream<TagKey<R>> tags() {
        return delegate().tags();
    }

    @Override
    public Either<ResourceKey<R>, R> unwrap() {
        return delegate().unwrap();
    }

    @Override
    public Optional<ResourceKey<R>> unwrapKey() {
        return delegate().unwrapKey();
    }

    @Override
    public Kind kind() {
        return delegate().kind();
    }

    @Override
    public boolean canSerializeIn(HolderOwner<R> owner) {
        return delegate().canSerializeIn(owner);
    }
}
