package com.cnethuka.kegare.mixin.client;

import com.cnethuka.kegare.innate_technique.time_manipulation.client.TimeStopClient;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * An entity frozen in a Standstill is drawn at the end of its last tick every frame (TimeStopClient, DESIGN §3.26): its tick is skipped,
 * so anything drawn from its age plus the partial tick would otherwise shiver back and forth each tick.
 * <p>
 * 1.21.11 extracts an entity's render state with the frame's partial tick in {@code LevelRenderer.extractEntity(Entity, float)}, which
 * hands it to the dispatcher; that call's second argument is the one replaced.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @ModifyArg(method = "extractEntity", index = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;extractEntity(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"))
    private float kegare$frozenPartialTick(Entity entity, float partialTick) {
        return TimeStopClient.partialTick(entity, partialTick);
    }
}
