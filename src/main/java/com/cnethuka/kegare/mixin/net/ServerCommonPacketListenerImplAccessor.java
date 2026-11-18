package com.cnethuka.kegare.mixin.net;

import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The connection and server of a configuration or play listener: both are protected fields of the shared base class. */
@Mixin(ServerCommonPacketListenerImpl.class)
public interface ServerCommonPacketListenerImplAccessor {
    @Accessor("connection")
    Connection kegare$connection();

    @Accessor("server")
    MinecraftServer kegare$server();
}
