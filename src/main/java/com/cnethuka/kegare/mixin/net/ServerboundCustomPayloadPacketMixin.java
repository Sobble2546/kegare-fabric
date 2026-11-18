package com.cnethuka.kegare.mixin.net;

import com.cnethuka.kegare.platform.net.NetworkThread;
import com.cnethuka.kegare.platform.net.PlatformNetHooks;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sub-tick timing's C2S payload runs on the connection's thread, as the packet arrives (DESIGN §3.29). Fabric only ever runs handlers on
 * the main thread, so the packet's own {@code handle}, which the network thread calls before any listener sees it, is hooked instead:
 * it is the earliest point and does not depend on how Fabric's listener mixins order themselves. Every other payload carries on.
 */
@Mixin(ServerboundCustomPayloadPacket.class)
public abstract class ServerboundCustomPayloadPacketMixin {
    @Inject(method = "handle(Lnet/minecraft/network/protocol/common/ServerCommonPacketListener;)V", at = @At("HEAD"), cancellable = true)
    private void kegare$networkThread(ServerCommonPacketListener listener, CallbackInfo ci) {
        if (NetworkThread.isServerPayload(((ServerboundCustomPayloadPacket) (Object) this).payload())
                && PlatformNetHooks.handleServerPayload(listener, ((ServerboundCustomPayloadPacket) (Object) this).payload())) {
            ci.cancel();
        }
    }
}
