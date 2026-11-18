package com.cnethuka.kegare.platform.client;

import com.cnethuka.kegare.mixin.client.ItemInHandRendererInvoker;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** Draws the vanilla first-person arm from inside a RenderHandEvent listener. 26.x needs the frame's render states, kept while the event is posted. */
public final class HandCompat {
    private HandCompat() {}

    @Nullable
    private static PlayerRenderState player;
    @Nullable
    private static FirstPersonHandsAndItemsRenderState hands;

    public static void states(@Nullable PlayerRenderState playerState, @Nullable FirstPersonHandsAndItemsRenderState handsState) {
        player = playerState;
        hands = handsState;
    }

    public static void renderArm(LocalPlayer who, float partialTick, float pitch, InteractionHand hand, float swing, ItemStack stack, float equip,
                                 PoseStack pose, SubmitNodeCollector collector, int light) {
        if (player == null || hands == null) return; // not inside the hand pass
        ((ItemInHandRendererInvoker) Minecraft.getInstance().gameRenderer.firstPersonHandsAndItemsRenderer).kegare$submitArmWithItem(player, hands, partialTick,
                pitch, hand, swing, stack, equip, pose, collector, light);
    }
}
