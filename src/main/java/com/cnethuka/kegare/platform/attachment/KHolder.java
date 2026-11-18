package com.cnethuka.kegare.platform.attachment;

import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * NeoForge's {@code getData/hasData/setData/removeData} on anything that holds attachments, over Fabric's attachment API. Injected into
 * {@code Entity} and {@code Level} (fabric.mod.json "loom:injected_interfaces" so call sites compile, and the mixins
 * {@code EntityHolderMixin}/{@code LevelHolderMixin} so it exists at runtime), so {@code player.getData(KegareAttachments.MALICE)} keeps working.
 * <p>
 * Every method takes either the type or the {@code Supplier} a {@code DeferredRegister} returns.
 */
public interface KHolder extends IAttachmentHolder {
    private AttachmentTarget target() {
        return (AttachmentTarget) this;
    }

    /** The value, created from the type's default and stored when there is none yet (NeoForge's getData). */
    default <T> T getData(AttachmentType<T> type) {
        return target().getAttachedOrCreate(type.fabric());
    }

    default <T> T getData(Supplier<? extends AttachmentType<T>> type) {
        return getData(type.get());
    }

    default boolean hasData(AttachmentType<?> type) {
        return target().hasAttached(type.fabric());
    }

    default boolean hasData(Supplier<? extends AttachmentType<?>> type) {
        return hasData(type.get());
    }

    /** Returns what was there before (null if nothing). */
    @Nullable
    default <T> T setData(AttachmentType<T> type, T value) {
        return target().setAttached(type.fabric(), value);
    }

    @Nullable
    default <T> T setData(Supplier<? extends AttachmentType<T>> type, T value) {
        return setData(type.get(), value);
    }

    @Nullable
    default <T> T removeData(AttachmentType<T> type) {
        return target().removeAttached(type.fabric());
    }

    @Nullable
    default <T> T removeData(Supplier<? extends AttachmentType<T>> type) {
        return removeData(type.get());
    }

    /**
     * Every saved attachment this holds, as NBT under its id (NeoForge's serializeAttachments, and the same inner shape as the
     * {@code fabric:attachments} compound in the entity's file). Null when there is none.
     */
    @Nullable
    default CompoundTag serializeAttachments(HolderLookup.Provider registries) {
        CompoundTag out = new CompoundTag();
        for (AttachmentType<?> type : Attachments.all()) {
            if (!type.isSaved() || !hasData(type)) continue;
            net.minecraft.nbt.Tag tag = Attachments.encodeValue(this, type, registries);
            if (tag != null) out.put(type.id().toString(), tag);
        }
        return out.isEmpty() ? null : out;
    }
}
