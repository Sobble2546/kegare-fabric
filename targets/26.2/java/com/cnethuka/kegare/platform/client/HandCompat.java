package com.cnethuka.kegare.platform.client;

import com.cnethuka.kegare.mixin.client.ItemInHandRendererInvoker;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Draws the vanilla first-person arm from inside a RenderHandEvent listener (the per-version seam; 26.x has its own file). */
public final class HandCompat {
    private HandCompat() {}

    public static void renderArm(LocalPlayer who, float partialTick, float pitch, InteractionHand hand, float swing, ItemStack stack, float equip,
                                 PoseStack pose, SubmitNodeCollector collector, int light) {
        ((ItemInHandRendererInvoker) Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer()).kegare$submitArmWithItem(who, partialTick,
                pitch, hand, swing, stack, equip, pose, collector, light);
    }
}
