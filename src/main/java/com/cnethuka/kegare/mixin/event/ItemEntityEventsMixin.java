package com.cnethuka.kegare.mixin.event;

import com.cnethuka.kegare.platform.NeoForge;
import com.cnethuka.kegare.platform.TriState;
import com.cnethuka.kegare.platform.event.entity.player.ItemEntityPickupEvent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A player touching a dropped item; FALSE from a listener leaves it where it is. (TRUE forcing a pickup is not supported: nothing needs it.) */
@Mixin(ItemEntity.class)
public abstract class ItemEntityEventsMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void kegare$pickup(Player player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.level().isClientSide()) return;
        if (NeoForge.EVENT_BUS.post(new ItemEntityPickupEvent.Pre(player, self)).canPickup() == TriState.FALSE) ci.cancel();
    }
}
