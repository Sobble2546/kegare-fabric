package com.cnethuka.kegare.platform.net;

import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.network.ConfigurationTask;

/**
 * NeoForge's configuration task that sends payloads rather than packets. Register it with
 * {@link RegisterConfigurationTasksEvent#register}; its payloads must be registered with
 * {@link PayloadRegistrar#configurationToClient}. It finishes when a handler calls {@link IPayloadContext#finishCurrentTask}.
 */
public interface ICustomConfigurationTask extends ConfigurationTask {
    void run(Consumer<CustomPacketPayload> sender);

    @Override
    default void start(Consumer<Packet<?>> sender) {
        run(payload -> sender.accept(VersionCompat.createConfigS2CPacket(payload)));
    }
}
