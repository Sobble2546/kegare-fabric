package com.cnethuka.kegare.data;

import com.cnethuka.kegare.Kegare;
import com.cnethuka.kegare.sound.KegareSounds;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.datagen.v1.builder.SoundTypeBuilder;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricSoundsProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.HolderLookup;

/** {@code assets/kegare/sounds.json}, one entry per {@link KegareSounds#ALL}; runData fails if a file is missing. */
public final class KegareSoundProvider extends FabricSoundsProvider {
    public KegareSoundProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void configure(HolderLookup.Provider registries, SoundExporter exporter) {
        for (KegareSounds.Sound s : KegareSounds.ALL) {
            // Fabric has no ExistingFileHelper, so look the .ogg up on the classpath (the main resources) ourselves.
            String path = "/assets/" + Kegare.MODID + "/sounds/" + s.file() + ".ogg";
            if (KegareSoundProvider.class.getResource(path) == null) {
                throw new IllegalStateException(s.name() + ": the sound file " + path + " isn't there (add the .ogg)");
            }
            exporter.add(Kegare.id(s.name()), SoundTypeBuilder.of().subtitle(s.subtitle()).sound(SoundTypeBuilder.EntryBuilder.ofFile(Kegare.id(s.file()))));
        }
    }

    @Override
    public String getName() {
        return "Kegare sounds";
    }
}
