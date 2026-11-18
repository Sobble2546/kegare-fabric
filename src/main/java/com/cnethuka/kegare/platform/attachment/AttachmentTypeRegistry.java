package com.cnethuka.kegare.platform.attachment;

import com.cnethuka.kegare.platform.registry.KRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Every attachment type, by id (NeoForge's {@code NeoForgeRegistries.ATTACHMENT_TYPES}; Fabric's own attachment registry is not a Minecraft
 * registry). Putting a type here is what gives it its id and creates the Fabric type.
 */
public final class AttachmentTypeRegistry extends KRegistry<AttachmentType<?>> {
    public static final ResourceKey<Registry<AttachmentType<?>>> KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("kegare", "platform/attachment_type"));

    /** Every type, in registration order: rebuilt when one is added (at startup), so reading it costs nothing. */
    private volatile java.util.List<AttachmentType<?>> all = java.util.List.of();

    public AttachmentTypeRegistry() {
        super(KEY, null);
    }

    @Override
    protected void onPut(ResourceKey<AttachmentType<?>> entryKey, AttachmentType<?> value) {
        value.bind(entryKey.identifier());
        synchronized (this) {
            all = java.util.List.copyOf(values());
        }
    }

    /** Every registered type, in registration order (an immutable snapshot; no copy per call). */
    java.util.List<AttachmentType<?>> all() {
        return all;
    }
}
