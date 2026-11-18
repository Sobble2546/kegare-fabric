package com.cnethuka.kegare.platform.client.gui;

import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

/**
 * The vanilla HUD layers by their NeoForge names. 1.21.11 draws the experience bar and the jump bar through one "info bar"
 * (vanilla's contextual bar), so {@link #EXPERIENCE_BAR} and {@link #JUMP_METER} are the same layer, and cancelling it hides both.
 */
public final class VanillaGuiLayers {
    private VanillaGuiLayers() {}

    public static final Identifier CROSSHAIR = VanillaHudElements.CROSSHAIR;
    public static final Identifier HOTBAR = VanillaHudElements.HOTBAR;
    public static final Identifier EXPERIENCE_BAR = VanillaHudElements.INFO_BAR;
    public static final Identifier JUMP_METER = VanillaHudElements.INFO_BAR;
    public static final Identifier EXPERIENCE_LEVEL = VanillaHudElements.EXPERIENCE_LEVEL;
    public static final Identifier SELECTED_ITEM_NAME = VanillaHudElements.HELD_ITEM_TOOLTIP;
    public static final Identifier OVERLAY_MESSAGE = VanillaHudElements.OVERLAY_MESSAGE;
    public static final Identifier CHAT = VanillaHudElements.CHAT;

    /** The layers that get {@code RenderGuiLayerEvent.Pre/Post}: the ones Kegare's listeners care about. */
    public static final List<Identifier> WRAPPED = List.of(HOTBAR, EXPERIENCE_BAR, EXPERIENCE_LEVEL, SELECTED_ITEM_NAME, OVERLAY_MESSAGE, CHAT);
}
