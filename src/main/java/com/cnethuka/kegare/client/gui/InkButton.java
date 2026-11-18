package com.cnethuka.kegare.client.gui;

import com.cnethuka.kegare.Kegare;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * A vanilla button (same input, focus and narration) drawn with Kegare's inked sprites instead of the grey stone:
 * cursed iron normally, gold-lit when hovered or focused, dull when inactive.
 */
public final class InkButton extends Button {
    private static final Identifier NORMAL = Kegare.id("button");
    private static final Identifier HIGHLIGHTED = Kegare.id("button_highlighted");
    private static final Identifier DISABLED = Kegare.id("button_disabled");

    private static final int TEXT = 0xE6DDF0;
    private static final int TEXT_HIGHLIGHTED = 0xFFE9A8;
    private static final int TEXT_DISABLED = 0x6B6276;

    public InkButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean lit = active && isHoveredOrFocused();
        g.blitSprite(RenderPipelines.GUI_TEXTURED, !active ? DISABLED : lit ? HIGHLIGHTED : NORMAL, getX(), getY(), getWidth(), getHeight(),
                GuiDraw.fade(alpha));
        int rgb = !active ? TEXT_DISABLED : lit ? TEXT_HIGHLIGHTED : TEXT;
        g.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2,
                rgb | Mth.ceil(alpha * 255f) << 24);
    }
}
