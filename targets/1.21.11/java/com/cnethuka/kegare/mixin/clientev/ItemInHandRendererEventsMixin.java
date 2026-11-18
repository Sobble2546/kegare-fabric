package com.cnethuka.kegare.mixin.clientev;

import com.cnethuka.kegare.platform.client.ClientEventHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * RenderHandEvent: wraps the two calls {@code renderHandsWithItems} makes to draw a hand, so a listener can cancel one or draw its
 * own. The wrapped calls (and not renderArmWithItem itself) so that a listener calling the vanilla renderer through
 * {@code ItemInHandRendererInvoker} does not fire the event again.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererEventsMixin {
    @WrapOperation(method = "renderHandsWithItems",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    private void kegare$hand(ItemInHandRenderer self, AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swing,
                             ItemStack stack, float equip, PoseStack pose, SubmitNodeCollector collector, int light, Operation<Void> original) {
        if (ClientEventHooks.renderHand(hand, partialTick, pitch, swing, stack, equip, pose, collector, light)) return;
        original.call(self, player, partialTick, pitch, hand, swing, stack, equip, pose, collector, light);
    }
}
