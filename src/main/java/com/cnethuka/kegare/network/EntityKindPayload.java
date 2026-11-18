package com.cnethuka.kegare.network;

import com.cnethuka.kegare.Kegare;
import com.cnethuka.kegare.platform.net.IPayloadContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What a Kegare entity really is, sent just before its spawn packet (DESIGN §3.5). The spawn packet itself names a vanilla stand-in
 * type: a protocol translator between the server and an older client (ViaBackwards) has to know an entity's type to translate its
 * data, and it knows no modded type (seen: "Could not find EntityTypes26_3 type id 161", then the client failed to decode
 * set_entity_data and was dropped). The stand-in has the same base class as the real entity, so its data translates the same way.
 * The client puts the real type back before it creates the entity.
 */
public record EntityKindPayload(int entityId, Identifier kind) implements CustomPacketPayload {
    public static final Type<EntityKindPayload> TYPE = new Type<>(Kegare.id("entity_kind"));
    public static final StreamCodec<FriendlyByteBuf, EntityKindPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.entityId());
                buf.writeIdentifier(p.kind());
            },
            buf -> new EntityKindPayload(buf.readVarInt(), buf.readIdentifier()));

    /** Client: entity id to the real type, from the payload until the spawn packet that follows it. */
    private static final Map<Integer, EntityType<?>> PENDING = new ConcurrentHashMap<>();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Server: whether this entity is announced through a stand-in (every {@code kegare:} entity type). */
    public static boolean applies(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals(Kegare.MODID);
    }

    public static EntityKindPayload of(Entity entity) {
        return new EntityKindPayload(entity.getId(), BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    /** Server: the same spawn packet, naming a vanilla type of the same base class. */
    public static ClientboundAddEntityPacket standIn(Entity entity, ClientboundAddEntityPacket packet) {
        EntityType<?> type = entity instanceof LivingEntity ? EntityType.ARMOR_STAND
                : entity instanceof ThrowableItemProjectile ? EntityType.SNOWBALL : EntityType.MARKER;
        return new ClientboundAddEntityPacket(packet.getId(), packet.getUUID(), packet.getX(), packet.getY(), packet.getZ(), packet.getXRot(),
                packet.getYRot(), type, packet.getData(), packet.getMovement(), packet.getYHeadRot());
    }

    public static void handle(EntityKindPayload payload, IPayloadContext context) {
        BuiltInRegistries.ENTITY_TYPE.getOptional(payload.kind())
                .filter(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals(Kegare.MODID))
                .ifPresent(type -> PENDING.put(payload.entityId(), type));
        if (PENDING.size() > 4096) PENDING.clear(); // spawn packets that never came
    }

    /** Client: the real type of the entity a spawn packet is about, once. */
    @Nullable
    public static EntityType<?> take(int entityId) {
        return PENDING.remove(entityId);
    }

    public static void clear() {
        PENDING.clear();
    }
}
