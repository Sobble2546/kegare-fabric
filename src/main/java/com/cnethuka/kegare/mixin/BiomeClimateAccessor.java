package com.cnethuka.kegare.mixin;

import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.Coerce;

/** A biome's climate record ({@code Biome.ClimateSettings} is package-private in 1.21.11, so it comes out as an Object): see {@link ClimateSettingsAccessor}. */
@Mixin(Biome.class)
public interface BiomeClimateAccessor {
    @Coerce
    @Accessor("climateSettings")
    Object kegare$climateSettings();
}
