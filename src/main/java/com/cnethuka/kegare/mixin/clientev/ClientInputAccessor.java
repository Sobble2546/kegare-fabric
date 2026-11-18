package com.cnethuka.kegare.mixin.clientev;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** {@code moveVector} is protected; MovementInputUpdateEvent writes it back. */
@Mixin(ClientInput.class)
public interface ClientInputAccessor {
    @Accessor("moveVector")
    void kegare$setMoveVector(Vec2 moveVector);
}
