package com.cnethuka.kegare.client;

import com.cnethuka.kegare.Kegare;
import net.minecraft.resources.Identifier;
import com.cnethuka.kegare.platform.Dist;
import com.cnethuka.kegare.platform.bus.EventPriority;
import com.cnethuka.kegare.platform.bus.SubscribeEvent;
import com.cnethuka.kegare.platform.bus.EventBusSubscriber;
import com.cnethuka.kegare.platform.client.event.RenderGuiLayerEvent;
import com.cnethuka.kegare.platform.client.gui.VanillaGuiLayers;

/**
 * While the combat panel is up: hides the vanilla hotbar, XP bar and level (the panel takes their place; the hotbar is
 * drawn as a column on the left) and moves the vanilla text above the hotbar (action-bar messages, the held item's name) and chat up by
 * as much as the panel raised hearts and food (chat also moves right, clear of the hotbar column). Fabric already lifts the two texts
 * above the tallest status bar once the panel has registered its height ({@link KegareClient}), but only once that stack is taller than
 * two rows; {@link #textLift} adds what is still missing so they rise by the full amount, as on NeoForge.
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
        if (!moves(event.getName()) || event.isCanceled() || !CombatPanelLayer.shown()) return;
        event.getGuiGraphics().pose().pushMatrix();
        boolean chat = event.getName().equals(VanillaGuiLayers.CHAT);
        event.getGuiGraphics().pose().translate(chat ? CombatHotbarLayer.WIDTH + 2 : 0, -(chat ? CombatPanelLayer.extraHeight() : textLift()));
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
     * What the action-bar message and the held item's name still have to rise by, on top of Fabric's own lift. Fabric moves them up by the
     * height of the tallest status stack beyond two rows (20 px); the panel wants them {@link CombatPanelLayer#extraHeight} above where
     * vanilla puts them, whatever the stack. The part of the stack above the 39 px baseline is what Fabric measures.
     */
    private static int textLift() {
        int fabricLift = Math.max(0, CombatNameLayer.stackTop() - 39 - 20);
        return Math.max(0, CombatPanelLayer.extraHeight() - fabricLift);
    }
}
