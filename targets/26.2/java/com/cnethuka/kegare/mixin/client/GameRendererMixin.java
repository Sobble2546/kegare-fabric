package com.cnethuka.kegare.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 26.x: nothing yet. The 1.21.11 file of this name runs the full-screen effects (ScreenFx) between the level and the HUD; those are
 * not ported to 26.x (client.visual.FullscreenFx for this target), so there is nothing to hook. The class keeps the mixin config's names.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
}
