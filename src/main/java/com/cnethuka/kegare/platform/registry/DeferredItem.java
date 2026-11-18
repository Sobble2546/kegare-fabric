package com.cnethuka.kegare.platform.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

/** NeoForge's DeferredItem: a registered item that is also an {@link ItemLike}. */
public class DeferredItem<T extends Item> extends DeferredHolder<Item, T> implements ItemLike {
    public DeferredItem(ResourceKey<Item> key) {
        super(key);
    }

    @Override
    public Item asItem() {
        return get();
    }
}
