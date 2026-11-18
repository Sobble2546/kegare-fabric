package com.cnethuka.kegare.platform.attachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class AttachmentHolderTest {
    @Test
    void theKeysAreFabricsAndNeoForges() {
        assertEquals("fabric:attachments", AttachmentHolder.ATTACHMENTS_NBT_KEY);
        assertEquals("neoforge:attachments", AttachmentHolder.NEOFORGE_NBT_KEY);
    }

    @Test
    void aNeoForgeFilesAttachmentsMoveToTheFabricKey() {
        CompoundTag file = new CompoundTag();
        CompoundTag old = new CompoundTag();
        CompoundTag malice = new CompoundTag();
        malice.putInt("capacity", 40);
        old.put("kegare:malice", malice);
        old.putString("curios:inventory", "kept");
        file.put("neoforge:attachments", old);
        file.putString("Dimension", "minecraft:overworld");

        assertTrue(AttachmentHolder.migrateNeoForgeKey(file));
        assertFalse(file.contains("neoforge:attachments"));
        CompoundTag now = file.getCompoundOrEmpty("fabric:attachments");
        assertEquals(40, now.getCompoundOrEmpty("kegare:malice").getIntOr("capacity", 0));
        assertEquals("kept", now.getStringOr("curios:inventory", ""));
        assertEquals("minecraft:overworld", file.getStringOr("Dimension", ""), "the rest of the file is left alone");
    }

    @Test
    void anEntryAlreadyUnderTheFabricKeyWins() {
        CompoundTag file = new CompoundTag();
        CompoundTag fabric = new CompoundTag();
        fabric.putString("kegare:roll", "fabric");
        file.put("fabric:attachments", fabric);
        CompoundTag neo = new CompoundTag();
        neo.putString("kegare:roll", "neoforge");
        neo.putString("kegare:grade", "neoforge");
        file.put("neoforge:attachments", neo);

        assertTrue(AttachmentHolder.migrateNeoForgeKey(file));
        CompoundTag now = file.getCompoundOrEmpty("fabric:attachments");
        assertEquals("fabric", now.getStringOr("kegare:roll", ""));
        assertEquals("neoforge", now.getStringOr("kegare:grade", ""));
        assertFalse(file.contains("neoforge:attachments"));
    }

    @Test
    void aFileWithoutTheNeoForgeKeyIsUntouched() {
        CompoundTag file = new CompoundTag();
        file.put("fabric:attachments", new CompoundTag());
        assertFalse(AttachmentHolder.migrateNeoForgeKey(file));
        assertTrue(file.contains("fabric:attachments"));
    }
}
