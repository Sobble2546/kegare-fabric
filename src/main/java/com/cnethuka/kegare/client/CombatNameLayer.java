package com.cnethuka.kegare.client;

import com.cnethuka.kegare.Kegare;
import com.cnethuka.kegare.api.ability.Ability;
import com.cnethuka.kegare.network.SyncPayload;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The selected ability's name, shown for 2 s after picking it, above hearts/food/armour (drawn after them, so it
 * clears whatever they used). Only while the combat panel is up.
 */
public final class CombatNameLayer implements HudElement {
    public static final Identifier ID = Kegare.id("combat_name");
    /** Draw nothing; they carry no height, so the registry's height for them is the top of the left or right status bars. */
    public static final Identifier PROBE_LEFT = Kegare.id("combat_name_probe_left");
    public static final Identifier PROBE_RIGHT = Kegare.id("combat_name_probe_right");
    private static final int NAME_TICKS = 40;

    private static int lastSelected = -1;
    private static long selectedAt;

    @Override
    public void render(GuiGraphics g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        SyncPayload data = ClientData.get();
        if (!CombatPanelLayer.shown() || data == null || mc.level == null) return;
        long now = mc.level.getGameTime();
        int selected = data.selectedSlot();
        if (selected != lastSelected) {
            lastSelected = selected;
            selectedAt = now;
        }
        List<Ability> abilities = ClientLoadout.abilities();
        long age = now - selectedAt;
        if (age >= NAME_TICKS || selected < 0 || selected >= abilities.size()) return;
        int alpha = age < NAME_TICKS - 10 ? 255 : (int) (255 * (NAME_TICKS - age) / 10f);
        if (alpha <= 8) return;
        Ability ability = abilities.get(selected);
        Component name = ability != null ? ability.displayName() : Component.translatable("hud.kegare.empty_slot");
        int y = g.guiHeight() - stackTop() - 10;
        g.drawCenteredString(mc.font, name, g.guiWidth() / 2, y, (alpha << 24) | 0xFFFFFF);
    }

    /**
     * How far above the bottom of the screen the tallest of the left (hearts, armour) and right (food, air) status bars ends. In
     * Creative no bar is drawn, so the stack ends where the panel puts it, as {@code Gui.leftHeight} used to say.
     */
    static int stackTop() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null && !mc.gameMode.canHurtPlayer()) return 39 + CombatPanelLayer.extraHeight();
        try {
            return registryStackTop();
        } catch (IllegalStateException | IllegalArgumentException notReady) {
            return 39 + CombatPanelLayer.extraHeight() + 10; // the registry is not ready or has no camera player: assume one row
        }
    }

    /**
     * How far Fabric itself lifts the action bar and the held item's name this frame: the registry's tallest stack beyond two rows
     * (20 px), 0 when the registry is not ready. Fabric's formula, mirrored here so {@link HudClearance} can undo it. The registry's
     * providers are static ("a bar exists"), so this does not look at Creative.
     * Only providers resolved before the probes are seen, so a third-party status bar in a root after the info bar is not counted.
     */
    static int fabricTextLift() {
        try {
            return Math.max(0, registryStackTop() - 39 - 20);
        } catch (IllegalStateException | IllegalArgumentException notReady) {
            return 0;
        }
    }

    private static int registryStackTop() {
        return Math.max(HudStatusBarHeightRegistry.getHeight(PROBE_LEFT), HudStatusBarHeightRegistry.getHeight(PROBE_RIGHT));
    }
}
