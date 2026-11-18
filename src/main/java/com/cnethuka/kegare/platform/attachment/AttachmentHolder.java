package com.cnethuka.kegare.platform.attachment;

import net.minecraft.nbt.CompoundTag;

/** Where attachments sit in an entity's NBT. */
public final class AttachmentHolder {
    /** Fabric's key ({@code AttachmentTarget.NBT_ATTACHMENT_KEY}). */
    public static final String ATTACHMENTS_NBT_KEY = "fabric:attachments";
    /** NeoForge's key: a world made by the NeoForge build has its attachments here. */
    public static final String NEOFORGE_NBT_KEY = "neoforge:attachments";

    private AttachmentHolder() {}

    /**
     * Moves a NeoForge file's attachments to where Fabric reads them. Entries already under the Fabric key win. Other mods' entries come along
     * and are skipped (with a log line) by Fabric if it doesn't know them. Returns whether anything moved.
     */
    public static boolean migrateNeoForgeKey(CompoundTag file) {
        if (!file.contains(NEOFORGE_NBT_KEY)) return false;
        CompoundTag old = file.getCompoundOrEmpty(NEOFORGE_NBT_KEY);
        file.remove(NEOFORGE_NBT_KEY);
        CompoundTag now = file.getCompoundOrEmpty(ATTACHMENTS_NBT_KEY);
        boolean moved = false;
        for (String key : old.keySet()) {
            if (now.contains(key)) continue;
            now.put(key, old.get(key));
            moved = true;
        }
        if (moved) file.put(ATTACHMENTS_NBT_KEY, now);
        return moved;
    }
}
