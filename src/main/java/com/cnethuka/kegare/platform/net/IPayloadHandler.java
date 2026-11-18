package com.cnethuka.kegare.platform.net;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** A payload's handler. Kegare passes {@code Payload::handle}. */
@FunctionalInterface
public interface IPayloadHandler<T extends CustomPacketPayload> {
    void handle(T payload, IPayloadContext context);
}
