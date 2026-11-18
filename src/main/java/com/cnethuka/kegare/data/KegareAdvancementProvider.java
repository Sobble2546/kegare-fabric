package com.cnethuka.kegare.data;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;

/** Writes {@link KegareAdvancementGenerator}'s advancements (all of Kegare's, secret-identity rule enforced there). */
public final class KegareAdvancementProvider extends FabricAdvancementProvider {
    public KegareAdvancementProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    public void generateAdvancement(HolderLookup.Provider registries, Consumer<AdvancementHolder> consumer) {
        new KegareAdvancementGenerator().generate(registries, consumer);
    }
}
