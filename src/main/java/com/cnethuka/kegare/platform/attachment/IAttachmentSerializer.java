package com.cnethuka.kegare.platform.attachment;

import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;

/**
 * NeoForge's hand-written attachment serializer, kept for {@code PossessionData}. {@link AttachmentType.Builder#serialize(IAttachmentSerializer)}
 * wraps it in a Codec, which is what Fabric stores. Through that wrapper {@code holder} and {@code provider} are always null, so a serializer
 * must not use them (the only one Kegare has doesn't).
 */
public interface IAttachmentSerializer<S extends Tag, T> {
    T read(@Nullable IAttachmentHolder holder, S tag, @Nullable HolderLookup.Provider provider);

    /** The tag to store, or null to store nothing. */
    @Nullable
    S write(T attachment, @Nullable HolderLookup.Provider provider);
}
