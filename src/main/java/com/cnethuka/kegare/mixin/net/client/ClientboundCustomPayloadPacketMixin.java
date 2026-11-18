package com.cnethuka.kegare.mixin.net.client;

import com.cnethuka.kegare.platform.net.NetworkThread;
import com.cnethuka.kegare.platform.net.PlatformNetClientHooks;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The client's twin of {@code ServerboundCustomPayloadPacketMixin}: sub-tick timing's ping is answered on the connection's thread. */
@Mixin(ClientboundCustomPayloadPacket.class)
public abstract class ClientboundCustomPayloadPacketMixin {
    @Inject(method = "handle(Lnet/minecraft/network/protocol/common/ClientCommonPacketListener;)V", at = @At("HEAD"), cancellable = true)
    private void kegare$networkThread(ClientCommonPacketListener listener, CallbackInfo ci) {
        if (NetworkThread.isClientPayload(((ClientboundCustomPayloadPacket) (Object) this).payload())
                && PlatformNetClientHooks.handleClientPayload(listener, ((ClientboundCustomPayloadPacket) (Object) this).payload())) {
            ci.cancel();
        }
    }
}
