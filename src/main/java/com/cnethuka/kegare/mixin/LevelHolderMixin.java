package com.cnethuka.kegare.mixin;

import com.cnethuka.kegare.platform.attachment.KHolder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/** Makes {@link KHolder} real on every level (fabric.mod.json injects it for the compiler). */
@Mixin(Level.class)
abstract class LevelHolderMixin implements KHolder {}
