package com.cnethuka.kegare.mixin;

import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The spawn packet's type, so the client can put a Kegare entity's real type back (network.EntityKindPayload). */
@Mixin(ClientboundAddEntityPacket.class)
public interface AddEntityPacketAccessor {
    @Mutable
    @Accessor("type")
    void kegare$setType(EntityType<?> type);
}
