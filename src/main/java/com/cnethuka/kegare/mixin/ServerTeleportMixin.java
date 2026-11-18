package com.cnethuka.kegare.mixin;

import com.cnethuka.kegare.severed.Severed;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every time the server puts a player somewhere (a command, Rewind, a swap, an ender pearl), Severed's Blur is told: a position the
 * server chose isn't movement for its speed check to put back (DESIGN §3.27).
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerTeleportMixin {
    @Shadow
    public ServerPlayer player;

    /** 1.21.11: the (x, y, z, yaw, pitch) overload delegates to this one, so every server-chosen position passes here. */
    @Inject(method = "teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V", at = @At("HEAD"))
    private void kegare$movedByServer(PositionMoveRotation to, Set<Relative> relative, CallbackInfo ci) {
        Severed.movedByServer(player);
    }
}
