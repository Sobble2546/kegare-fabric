package com.cnethuka.kegare.client.gui;

import com.cnethuka.kegare.Kegare;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

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
        // Hidden while it fades in or out: the GUI text path draws alpha under 4/255 as fully opaque, which would pop the label for a frame.
        if (alpha < 0.02f) return;
        int rgb = !active ? TEXT_DISABLED : lit ? TEXT_HIGHLIGHTED : TEXT;
        // Vanilla's label path: clipped to the button and scrolled when too long; the collector fades it with the widget's alpha.
        // The colour is the base style, so a message that sets its own colour still wins, as with the old drawCenteredString.
        renderScrollingStringOverContents(g.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE),
                Component.empty().withColor(rgb).append(getMessage()), 2);
    }
}
