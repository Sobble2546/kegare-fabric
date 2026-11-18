package com.cnethuka.kegare.mixin.net.client;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The connection of a configuration or play listener of the client: a protected field of their shared base class. */
@Mixin(ClientCommonPacketListenerImpl.class)
public interface ClientCommonPacketListenerImplAccessor {
    @Accessor("connection")
    Connection kegare$connection();
}
