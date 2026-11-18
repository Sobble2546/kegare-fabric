package com.cnethuka.kegare.mixin.clientev;

import com.cnethuka.kegare.client.CombatPanelLayer;
import com.cnethuka.kegare.client.HudClearance;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Keeps the chat where {@link HudClearance} puts it (up and right of the hotbar column) while the chat box is open too. While it is
 * focused, 1.21.11's HUD skips the chat and {@code ChatScreen.render} draws it itself, so the HUD layer's shift never applies.
 * Only the chat is moved: the edit box is drawn by the screen and stays at the bottom.
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    /** Vanilla un-does the pose for the mouse (hover text) by itself, so only the drawing is moved. */
    @WrapOperation(method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V"))
    private void kegare$shiftChat(ChatComponent chat, GuiGraphicsExtractor g, Font font, int tick, int mouseX, int mouseY, ChatComponent.DisplayMode focused, boolean insertion,
                                  Operation<Void> original) {
        if (!CombatPanelLayer.shown()) {
            original.call(chat, g, font, tick, mouseX, mouseY, focused, insertion);
            return;
        }
        g.pose().pushMatrix();
        g.pose().translate(HudClearance.chatShiftX(), -HudClearance.chatLift());
        try {
            original.call(chat, g, font, tick, mouseX, mouseY, focused, insertion);
        } finally {
            g.pose().popMatrix();
        }
    }

    /**
     * Finding the clicked component runs without a pose, on the click's own coordinates, so it gets the position in the chat's own space.
     * {@code x()} and {@code y()} are read in {@code mouseClicked} only for that lookup.
     */
    @WrapOperation(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/MouseButtonEvent;x()D"))
    private double kegare$clickX(MouseButtonEvent event, Operation<Double> original) {
        return original.call(event) - (CombatPanelLayer.shown() ? HudClearance.chatShiftX() : 0);
    }

    @WrapOperation(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/MouseButtonEvent;y()D"))
    private double kegare$clickY(MouseButtonEvent event, Operation<Double> original) {
        return original.call(event) + (CombatPanelLayer.shown() ? HudClearance.chatLift() : 0);
    }
}
