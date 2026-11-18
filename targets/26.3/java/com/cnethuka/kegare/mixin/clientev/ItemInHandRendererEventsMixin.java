package com.cnethuka.kegare.mixin.clientev;

import com.cnethuka.kegare.platform.client.ClientEventHooks;
import com.cnethuka.kegare.platform.client.HandCompat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** RenderHandEvent on 26.x: around each arm the first-person renderer submits. The two render states are kept for {@link HandCompat}. UNVERIFIED in game. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class ItemInHandRendererEventsMixin {
    @WrapOperation(method = "submitHandsWithItems",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;submitArmWithItem(Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    private void kegare$hand(FirstPersonHandsAndItemsRenderer self, PlayerRenderState player, FirstPersonHandsAndItemsRenderState hands, float partialTick, float pitch,
                             InteractionHand hand, float swing, ItemStack stack, float equip, PoseStack pose, SubmitNodeCollector collector, int light,
                             Operation<Void> original) {
        HandCompat.states(player, hands);
        try {
            if (ClientEventHooks.renderHand(hand, partialTick, pitch, swing, stack, equip, pose, collector, light)) return;
        } finally {
            HandCompat.states(null, null);
        }
        original.call(self, player, hands, partialTick, pitch, hand, swing, stack, equip, pose, collector, light);
    }
}
