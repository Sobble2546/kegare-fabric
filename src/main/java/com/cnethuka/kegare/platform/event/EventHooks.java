package com.cnethuka.kegare.platform.event;

import com.cnethuka.kegare.platform.NeoForge;
import com.cnethuka.kegare.platform.event.entity.EntityTeleportEvent;
import net.minecraft.world.entity.LivingEntity;

/** The few posting helpers NeoForge code (and Kegare's tests) call by name. The mixins post through these too, so tests exercise the same path. */
public final class EventHooks {
    private EventHooks() {}

    /** An enderman-like entity teleports itself to the position; the returned event says whether a listener cancelled it. */
    public static EntityTeleportEvent.EnderEntity onEnderTeleport(LivingEntity entity, double x, double y, double z) {
        return NeoForge.EVENT_BUS.post(new EntityTeleportEvent.EnderEntity(entity, x, y, z));
    }
}
