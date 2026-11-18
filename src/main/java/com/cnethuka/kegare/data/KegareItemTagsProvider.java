package com.cnethuka.kegare.data;

import com.cnethuka.kegare.registry.KegareItems;
import com.cnethuka.kegare.registry.KegareTags;
import com.cnethuka.kegare.sixeyes.Eyewear;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class KegareItemTagsProvider extends FabricTagProvider.ItemTagProvider {
    /** Trinkets' eyes slot takes what this tag holds (the slot itself: data/trinkets/slots/head/eyes.json). */
    public static final TagKey<Item> EYES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("trinkets", Eyewear.SLOT_ID));

    public KegareItemTagsProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        valueLookupBuilder(KegareTags.TALISMANS).add(KegareItems.PURIFICATION_TALISMAN.get());
        valueLookupBuilder(EYES).add(KegareItems.BLINDFOLD.get(), KegareItems.SUNGLASSES.get());
    }
}
