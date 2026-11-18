package com.cnethuka.kegare.client.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

/**
 * Textured quads on the HUD that {@link GuiGraphicsExtractor#blit} can't do: the texture window as floats (a frame of a sheet), a tint with alpha,
 * and a choice of linear or pixel-sharp filtering (the texture's own sampler is what blit uses, and a smooth one is set per draw here
 * instead of by a metadata file). The element is queued on the HUD's render state the way blit queues its own, with the current 2D pose.
 */
public final class GuiBlit {
    private GuiBlit() {}

    /**
     * @param argb tint and alpha, multiplied into the texture ({@code 0xFFFFFFFF} leaves it as it is)
     */
    public static void blit(GuiGraphicsExtractor g, Identifier texture, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1,
                            int argb, boolean linear) {
        AbstractTexture t = Minecraft.getInstance().getTextureManager().getTexture(texture);
        FilterMode filter = linear ? FilterMode.LINEAR : FilterMode.NEAREST;
        g.guiRenderState.addBlitToCurrentLayer(new BlitRenderState(RenderPipelines.GUI_TEXTURED,
                TextureSetup.singleTexture(t.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(filter)),
                new Matrix3x2f(g.pose()), x0, y0, x1, y1, u0, u1, v0, v1, argb, g.scissorStack.peek()));
    }
}
