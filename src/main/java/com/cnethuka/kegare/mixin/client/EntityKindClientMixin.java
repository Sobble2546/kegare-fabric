package com.cnethuka.kegare.mixin.client;

import com.cnethuka.kegare.mixin.AddEntityPacketAccessor;
import com.cnethuka.kegare.network.EntityKindPayload;
import com.cnethuka.kegare.platform.net.ClientNet;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A Kegare entity arrives as a vanilla stand-in type; its real type came just before (network.EntityKindPayload). Only on a Kegare server. */
@Mixin(ClientPacketListener.class)
public abstract class EntityKindClientMixin {
    @Inject(method = "createEntityFromPacket", at = @At("HEAD"))
    private void kegare$realType(ClientboundAddEntityPacket packet, CallbackInfoReturnable<?> cir) {
        if (!ClientNet.isActive()) return;
        EntityType<?> real = EntityKindPayload.take(packet.getId());
        if (real != null) ((AddEntityPacketAccessor) packet).kegare$setType(real);
    }
}
