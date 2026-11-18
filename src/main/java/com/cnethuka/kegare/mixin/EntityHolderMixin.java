package com.cnethuka.kegare.mixin;

import com.cnethuka.kegare.platform.attachment.KHolder;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

/** Makes {@link KHolder} real on every entity (fabric.mod.json injects it for the compiler). */
@Mixin(Entity.class)
abstract class EntityHolderMixin implements KHolder {}
