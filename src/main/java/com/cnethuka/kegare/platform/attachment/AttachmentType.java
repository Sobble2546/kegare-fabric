package com.cnethuka.kegare.platform.attachment;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;

/**
 * NeoForge's AttachmentType over Fabric's. Built without an id (as in NeoForge) and given one when its
 * {@code DeferredRegister<AttachmentType<?>>} is applied ({@link AttachmentTypeRegistry}), which is when the Fabric type exists.
 * <p>
 * Nothing here touches Fabric until {@link #fabric()} is called, so the codec and copy logic can be tested without a game.
 * Fabric's own {@code copyOnDeath} is left OFF on purpose: Fabric copies after {@code ServerPlayerEvents.COPY_FROM}, so Kegare's
 * "clone" handlers would see empty data and Fabric would then overwrite their edits. {@link Attachments#copyOnDeath} does the copy instead.
 */
public final class AttachmentType<T> {
    private final Supplier<T> factory;
    @Nullable
    private final Codec<T> codec;
    @Nullable
    private final IAttachmentSerializer<Tag, T> serializer;
    private final boolean copyOnDeath;
    @Nullable
    private Identifier id;
    @Nullable
    private net.fabricmc.fabric.api.attachment.v1.AttachmentType<T> fabric;

    private AttachmentType(Supplier<T> factory, @Nullable Codec<T> codec, @Nullable IAttachmentSerializer<Tag, T> serializer, boolean copyOnDeath) {
        this.factory = factory;
        this.codec = codec;
        this.serializer = serializer;
        this.copyOnDeath = copyOnDeath;
    }

    public static <T> Builder<T> builder(Supplier<T> factory) {
        return new Builder<>(factory);
    }

    /** The id it was registered under. Throws before registration. */
    public Identifier id() {
        if (id == null) throw new IllegalStateException("Attachment type not registered yet");
        return id;
    }

    public boolean isRegistered() {
        return id != null;
    }

    /** Is it stored with its holder? (Runtime-only types have no codec.) */
    public boolean isSaved() {
        return codec != null;
    }

    public boolean copyOnDeath() {
        return copyOnDeath;
    }

    public T create() {
        return factory.get();
    }

    /** The codec Fabric stores it with, or null for a runtime-only type. */
    @Nullable
    public Codec<T> codec() {
        return codec;
    }

    /** The same thing as NBT in and out, NeoForge style; null for a runtime-only type. */
    @Nullable
    public IAttachmentSerializer<Tag, T> serializer() {
        return serializer;
    }

    /** The Fabric type. Only exists once registered. */
    public net.fabricmc.fabric.api.attachment.v1.AttachmentType<T> fabric() {
        if (fabric == null) throw new IllegalStateException("Attachment type not registered yet" + (id == null ? "" : ": " + id));
        return fabric;
    }

    /** Called by {@link AttachmentTypeRegistry} when the type is registered under {@code id}. Idempotent for the same id. */
    synchronized void bind(Identifier id) {
        if (this.id != null) {
            if (!this.id.equals(id)) throw new IllegalStateException("Attachment type already registered as " + this.id + ", not " + id);
            return;
        }
        this.id = id;
        AttachmentRegistry.Builder<T> builder = AttachmentRegistry.<T>builder().initializer(factory);
        if (codec != null) builder.persistent(codec);
        this.fabric = builder.buildAndRegister(id);
    }

    // ---------------------------------------------------------------- NBT

    private static DynamicOps<Tag> ops(@Nullable HolderLookup.Provider registries) {
        return registries == null ? NbtOps.INSTANCE : RegistryOps.create(NbtOps.INSTANCE, registries);
    }

    /** What to store for this value, or null for nothing. Throws on a codec error. */
    @Nullable
    public Tag encode(T value, @Nullable HolderLookup.Provider registries) {
        if (serializer != null) return serializer.write(value, registries);
        if (codec == null) return null;
        return codec.encodeStart(ops(registries), value).getOrThrow(msg -> new IllegalStateException("Couldn't encode " + describe() + ": " + msg));
    }

    /** The value stored as this tag. Throws on a bad tag. */
    public T decode(Tag tag, @Nullable HolderLookup.Provider registries) {
        if (serializer != null) return serializer.read(null, tag, registries);
        if (codec == null) throw new IllegalStateException(describe() + " is not saved");
        return codec.parse(ops(registries), tag).getOrThrow(msg -> new IllegalStateException("Couldn't decode " + describe() + ": " + msg));
    }

    /** A copy, through the codec like NeoForge's default copy handler. Null when the type has nothing to copy (runtime-only, or encodes to nothing). */
    @Nullable
    public T copy(T value, @Nullable HolderLookup.Provider registries) {
        Tag tag = encode(value, registries);
        return tag == null ? null : decode(tag, registries);
    }

    private String describe() {
        return id == null ? "an attachment" : "attachment " + id;
    }

    @Override
    public String toString() {
        return "AttachmentType[" + (id == null ? "unregistered" : id) + "]";
    }

    // ---------------------------------------------------------------- builder

    public static final class Builder<T> {
        private final Supplier<T> factory;
        @Nullable
        private Codec<T> codec;
        @Nullable
        private IAttachmentSerializer<Tag, T> serializer;
        private boolean copyOnDeath;

        private Builder(Supplier<T> factory) {
            this.factory = factory;
        }

        public Builder<T> serialize(Codec<T> codec) {
            this.codec = codec;
            this.serializer = null;
            return this;
        }

        /** A NeoForge-style serializer, run through a Codec so Fabric can store it. Writing null stores an empty compound, which reads back as it was. */
        @SuppressWarnings("unchecked")
        public Builder<T> serialize(IAttachmentSerializer<? extends Tag, T> serializer) {
            this.serializer = (IAttachmentSerializer<Tag, T>) serializer;
            this.codec = codecOf(this.serializer);
            return this;
        }

        /** Kept when the holder dies and is rebuilt (the player's respawn); see {@link Attachments#copyOnDeath}. */
        public Builder<T> copyOnDeath() {
            this.copyOnDeath = true;
            return this;
        }

        public AttachmentType<T> build() {
            IAttachmentSerializer<Tag, T> out = serializer;
            if (out == null && codec != null) out = serializerOf(codec);
            return new AttachmentType<>(factory, codec, out, copyOnDeath);
        }
    }

    // ---------------------------------------------------------------- codec <-> serializer

    /** A serializer over a codec: NBT in and out. */
    static <T> IAttachmentSerializer<Tag, T> serializerOf(Codec<T> codec) {
        return new IAttachmentSerializer<>() {
            @Override
            public T read(@Nullable IAttachmentHolder holder, Tag tag, @Nullable HolderLookup.Provider provider) {
                return codec.parse(ops(provider), tag).getOrThrow(msg -> new IllegalStateException("Couldn't decode attachment: " + msg));
            }

            @Nullable
            @Override
            public Tag write(T attachment, @Nullable HolderLookup.Provider provider) {
                return codec.encodeStart(ops(provider), attachment).getOrThrow(msg -> new IllegalStateException("Couldn't encode attachment: " + msg));
            }
        };
    }

    /** A codec over a serializer. Whatever the ops, it goes through NBT. A null write becomes an empty compound; a tag the serializer rejects is an error result. */
    static <T> Codec<T> codecOf(IAttachmentSerializer<Tag, T> serializer) {
        return new Codec<>() {
            @Override
            public <U> DataResult<Pair<T, U>> decode(DynamicOps<U> ops, U input) {
                try {
                    Tag tag = ops.convertTo(NbtOps.INSTANCE, input);
                    return DataResult.success(Pair.of(serializer.read(null, tag, null), ops.empty()));
                } catch (RuntimeException e) {
                    return DataResult.error(() -> "Attachment serializer failed: " + e);
                }
            }

            @Override
            public <U> DataResult<U> encode(T input, DynamicOps<U> ops, U prefix) {
                try {
                    Tag tag = serializer.write(input, null);
                    if (tag == null) tag = new CompoundTag();
                    return ops.mergeToPrimitive(prefix, NbtOps.INSTANCE.convertTo(ops, tag));
                } catch (RuntimeException e) {
                    return DataResult.error(() -> "Attachment serializer failed: " + e);
                }
            }

            @Override
            public String toString() {
                return "SerializerCodec";
            }
        };
    }
}
