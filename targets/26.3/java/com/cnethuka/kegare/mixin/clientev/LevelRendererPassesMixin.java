package com.cnethuka.kegare.mixin.clientev;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 26.x: nothing to add. The 1.21.11 file of this name adds frame-graph passes after particles and weather; on 26.x every level stage is
 * posted from Fabric's COLLECT_SUBMITS ({@code platform.client.LevelStages}). The class stays so the mixin config lists the same names
 * on every target.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererPassesMixin {
}
