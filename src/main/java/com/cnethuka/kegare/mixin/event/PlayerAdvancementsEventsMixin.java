package com.cnethuka.kegare.mixin.event;

import com.cnethuka.kegare.platform.NeoForge;
import com.cnethuka.kegare.platform.event.entity.player.AdvancementEvent;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** An advancement earned: right after its rewards are granted, which happens exactly once, when its last criterion is. */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsEventsMixin {
    @Shadow private ServerPlayer player;

    @Inject(method = "award", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/advancements/AdvancementRewards;grant(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void kegare$earned(AdvancementHolder advancement, String criterion, CallbackInfoReturnable<Boolean> cir) {
        NeoForge.EVENT_BUS.post(new AdvancementEvent.AdvancementEarnEvent(player, advancement));
    }
}
