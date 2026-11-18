package com.cnethuka.kegare.data;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * {@code ./gradlew runData} (an alias of Fabric's {@code runDatagen}) writes everything into {@code src/generated/resources}. Registered under the
 * {@code fabric-datagen} entrypoint in fabric.mod.json. The client providers (language, item models, sounds) run in the same pass because the
 * run configuration is a client one.
 */
public final class KegareDataGen implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();

        // Assets
        pack.addProvider(KegareLanguageProvider::new);
        pack.addProvider(KegareItemModelProvider::new);
        pack.addProvider(KegareSoundProvider::new);

        // Data
        pack.addProvider(KegareItemTagsProvider::new);
        pack.addProvider(KegareRecipeProvider::new);
        pack.addProvider(KegareAdvancementProvider::new);
    }
}
