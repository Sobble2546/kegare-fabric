package com.cnethuka.kegare.platform.registry;

import com.cnethuka.kegare.platform.attachment.AttachmentType;
import com.cnethuka.kegare.platform.attachment.AttachmentTypeRegistry;

/** The only NeoForge registry Kegare reads: its attachment types. */
public final class NeoForgeRegistries {
    public static final KRegistry<AttachmentType<?>> ATTACHMENT_TYPES = new AttachmentTypeRegistry();

    private NeoForgeRegistries() {}
}
