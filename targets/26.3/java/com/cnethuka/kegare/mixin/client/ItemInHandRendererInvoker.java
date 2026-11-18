package com.cnethuka.kegare.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 26.x: the first-person hand renderer was renamed and draws from two render states instead of the player (same class name as on 1.21.11 for the mixin config). */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public interface ItemInHandRendererInvoker {
    @Invoker("submitArmWithItem")
    void kegare$submitArmWithItem(PlayerRenderState player, FirstPersonHandsAndItemsRenderState hands, float partialTicks, float pitch, InteractionHand hand,
            float swingProgress, ItemStack stack, float equippedProgress, PoseStack poseStack, SubmitNodeCollector collector, int combinedLight);
}
