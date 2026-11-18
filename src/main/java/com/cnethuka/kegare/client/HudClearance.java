package com.cnethuka.kegare.client;

import com.cnethuka.kegare.Kegare;
import net.minecraft.resources.Identifier;
import com.cnethuka.kegare.platform.Dist;
import com.cnethuka.kegare.platform.bus.EventPriority;
import com.cnethuka.kegare.platform.bus.SubscribeEvent;
import com.cnethuka.kegare.platform.bus.EventBusSubscriber;
import com.cnethuka.kegare.platform.client.event.RenderGuiLayerEvent;
import com.cnethuka.kegare.platform.client.gui.VanillaGuiLayers;
import com.cnethuka.kegare.platform.net.ClientNet;

/**
 * While the combat panel is up: hides the vanilla hotbar, XP bar and level (the panel takes their place; the hotbar is
 * drawn as a column on the left) and moves the vanilla text above the hotbar (action-bar messages, the held item's name) and chat up by
 * as much as the panel raised hearts and food (chat also moves right, clear of the hotbar column). The open chat screen draws the chat
 * itself, outside the HUD layer, so {@code ChatScreenMixin} applies the same shift there.
 * <p>
 * The two texts: as soon as the panel registers a status bar height ({@link KegareClient}), Fabric lifts them above the tallest status stack
 * beyond two rows, which vanilla never does (armour on a horse, absorption), on any server. Off a Kegare server this layer undoes that lift
 * ({@link CombatNameLayer#fabricTextLift}); on one, with the panel up, it adds what Fabric's lift leaves short of the panel's height.
 */
@EventBusSubscriber(modid = Kegare.MODID, value = Dist.CLIENT)
public final class HudClearance {
    private static boolean shifted;

    private HudClearance() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    static void hide(RenderGuiLayerEvent.Pre event) {
        if (!CombatPanelLayer.shown()) return;
        Identifier layer = event.getName();
        if (layer.equals(VanillaGuiLayers.HOTBAR) || layer.equals(VanillaGuiLayers.EXPERIENCE_BAR)
                || layer.equals(VanillaGuiLayers.EXPERIENCE_LEVEL) || layer.equals(VanillaGuiLayers.JUMP_METER)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void pre(RenderGuiLayerEvent.Pre event) {
        if (event.isCanceled()) return;
        Identifier layer = event.getName();
        float dx = 0, dy;
        if (layer.equals(VanillaGuiLayers.CHAT)) {
            if (!CombatPanelLayer.shown()) return;
            dx = chatShiftX();
            dy = -chatLift();
        } else if (layer.equals(VanillaGuiLayers.OVERLAY_MESSAGE) || layer.equals(VanillaGuiLayers.SELECTED_ITEM_NAME)) {
            dy = textShift();
        } else {
            return;
        }
        if (dx == 0 && dy == 0) return;
        event.getGuiGraphics().pose().pushMatrix();
        event.getGuiGraphics().pose().translate(dx, dy);
        shifted = true;
    }

    @SubscribeEvent
    static void post(RenderGuiLayerEvent.Post event) {
        if (shifted && moves(event.getName())) {
            event.getGuiGraphics().pose().popMatrix();
            shifted = false;
        }
    }

    private static boolean moves(Identifier layer) {
        return layer.equals(VanillaGuiLayers.OVERLAY_MESSAGE) || layer.equals(VanillaGuiLayers.SELECTED_ITEM_NAME)
                || layer.equals(VanillaGuiLayers.CHAT);
    }

    /**
     * Where the action-bar message and the held item's name go, relative to where Fabric already put them (positive is down). Off a Kegare
     * server: undo Fabric's lift, so they sit where vanilla puts them. On one, with the panel up: whatever is still missing to rise by the
     * panel's height (Fabric's lift counts toward it, and if the stack is taller than the panel's rise Fabric's higher lift stays, so the
     * texts keep clear of the stack). Panel down: nothing.
     */
    private static int textShift() {
        int fabricLift = CombatNameLayer.fabricTextLift();
        if (!ClientNet.isActive()) return fabricLift;
        if (!CombatPanelLayer.shown()) return 0;
        return -Math.max(0, CombatPanelLayer.extraHeight() - fabricLift);
    }

    /** How far chat moves right while the panel is up: clear of the hotbar column. Also used by the open chat screen ({@code ChatScreenMixin}). */
    public static int chatShiftX() {
        return CombatHotbarLayer.WIDTH + 2;
    }

    /** How far chat moves up while the panel is up (the panel raised hearts and food by as much). */
    public static int chatLift() {
        return CombatPanelLayer.extraHeight();
    }
}
