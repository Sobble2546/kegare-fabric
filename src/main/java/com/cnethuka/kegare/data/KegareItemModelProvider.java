package com.cnethuka.kegare.data;

import com.cnethuka.kegare.registry.KegareItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;

/**
 * Flat item models. Since 1.21.4 this also writes each item's {@code assets/kegare/items/<id>.json} definition next to the
 * {@code models/item/<id>.json} model.
 */
public final class KegareItemModelProvider extends FabricModelProvider {
    public KegareItemModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators generators) {}

    @Override
    public void generateItemModels(ItemModelGenerators generators) {
        generators.generateFlatItem(KegareItems.PURIFICATION_TALISMAN.get(), ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(KegareItems.BLINDFOLD.get(), ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(KegareItems.SUNGLASSES.get(), ModelTemplates.FLAT_ITEM);
        // Rime's projectiles in flight
        generators.generateFlatItem(com.cnethuka.kegare.innate_technique.rime.RimeRegistry.FROST_SHARD_ITEM.get(), ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(com.cnethuka.kegare.innate_technique.rime.RimeRegistry.GLACIAL_LANCE_ITEM.get(), ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(com.cnethuka.kegare.innate_technique.swap.SwapRegistry.ANCHOR_ITEM.get(), ModelTemplates.FLAT_ITEM);
    }
}
