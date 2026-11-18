package com.cnethuka.kegare.platform.event.entity;

import com.cnethuka.kegare.platform.bus.Event;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

/** Entity types register their default attributes. Posted once, after the entity types are registered (see ServerEventHooks.fireAttributeCreation). */
public final class EntityAttributeCreationEvent extends Event {
    public void put(EntityType<? extends LivingEntity> type, AttributeSupplier attributes) {
        FabricDefaultAttributeRegistry.register(type, attributes);
    }
}
