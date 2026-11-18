package com.cnethuka.kegare.platform.attachment;

import com.cnethuka.kegare.platform.registry.NeoForgeRegistries;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;

/** Attachment helpers that are not on the holder. */
public final class Attachments {
    private Attachments() {}

    /** Every registered attachment type, in registration order. */
    public static List<AttachmentType<?>> all() {
        return List.copyOf(NeoForgeRegistries.ATTACHMENT_TYPES.values());
    }

    /**
     * NeoForge's attachment copy before {@code PlayerEvent.Clone}, for when the game rebuilds a player (respawn after death, coming back from
     * the End). <b>Call it first thing from the Clone event's source</b>, {@code ServerPlayerEvents.COPY_FROM}, before any handler reads the new
     * player: Fabric's own copy comes after that event and would overwrite what the handlers set.
     * <p>
     * <b>{@code wasDeath} is the opposite of Fabric's {@code alive}</b> ({@code wasDeath = !alive}).
     * <ul>
     *   <li>Death: the {@code copyOnDeath} types are copied through their codec, so the new player shares nothing with the old one and
     *       anything not saved starts fresh, as in NeoForge. Fabric's own transfer then does nothing, because the platform leaves Fabric's
     *       {@code copyOnDeath} flag off.</li>
     *   <li>Anything else (End return; Fabric's dimension-change transfer behaves the same way): every attachment, runtime-only ones
     *       included, is handed over <i>by reference</i>, which is exactly what Fabric does right after. Doing it here first means handlers see the
     *       data, and what they change in place survives Fabric's own pass (it re-sets the same objects). Known gap: a value a handler
     *       replaces with {@code setData} is put back to the old object by Fabric's pass, so Clone handlers must edit in place.</li>
     * </ul>
     */
    public static void copyOnDeath(Entity oldEntity, Entity newEntity, boolean wasDeath) {
        HolderLookup.Provider registries = newEntity.registryAccess();
        for (AttachmentType<?> type : all()) {
            if (wasDeath) {
                if (type.isSaved() && type.copyOnDeath()) copyThroughCodec(oldEntity, newEntity, type, registries);
            } else {
                copyReference(oldEntity, newEntity, type);
            }
        }
    }

    private static <T> void copyThroughCodec(Entity from, Entity to, AttachmentType<T> type, HolderLookup.Provider registries) {
        if (!from.hasData(type)) return;
        T copy = type.copy(from.getData(type), registries);
        if (copy != null) to.setData(type, copy);
    }

    private static <T> void copyReference(Entity from, Entity to, AttachmentType<T> type) {
        if (from.hasData(type)) to.setData(type, from.getData(type));
    }

    /** The saved form of one attachment on a holder, or null for nothing. */
    @Nullable
    static <T> Tag encodeValue(KHolder holder, AttachmentType<T> type, HolderLookup.Provider registries) {
        return type.encode(holder.getData(type), registries);
    }
}
