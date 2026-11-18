package com.cnethuka.kegare.mixin;

import com.cnethuka.kegare.platform.net.ItemWire;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * The item stack network codec (ItemStack's first anonymous class, the one behind OPTIONAL_STREAM_CODEC, which the others call): Kegare
 * items go out as marked paper and come back as themselves ({@link ItemWire}).
 */
@Mixin(targets = "net.minecraft.world.item.ItemStack$1")
public abstract class ItemStackWireMixin {
    @ModifyVariable(method = "encode(Lnet/minecraft/network/RegistryFriendlyByteBuf;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), argsOnly = true)
    private ItemStack kegare$toWire(ItemStack stack) {
        return ItemWire.toWire(stack);
    }

    @ModifyReturnValue(method = "decode(Lnet/minecraft/network/RegistryFriendlyByteBuf;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private ItemStack kegare$fromWire(ItemStack stack) {
        return ItemWire.fromWire(stack);
    }
}
