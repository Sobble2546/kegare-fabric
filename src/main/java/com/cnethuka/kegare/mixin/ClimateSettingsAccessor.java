package com.cnethuka.kegare.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A biome's rainfall: vanilla 1.21.11 has no public getter for it. Read only; reach it through {@link BiomeClimateAccessor}. */
@Mixin(targets = "net.minecraft.world.level.biome.Biome$ClimateSettings")
public interface ClimateSettingsAccessor {
    @Accessor("downfall")
    float kegare$downfall();
}
