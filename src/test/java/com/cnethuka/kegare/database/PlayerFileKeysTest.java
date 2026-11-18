package com.cnethuka.kegare.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

/** How the player-file helpers treat Fabric's and NeoForge's attachment keys (no game needed). */
class PlayerFileKeysTest {
    private static CompoundTag malice(int capacity) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("capacity", capacity);
        return tag;
    }

    @Test
    void rowsAreReadFromEitherKeyAndTheFabricOneWins() {
        CompoundTag file = new CompoundTag();
        CompoundTag neo = new CompoundTag();
        neo.put("kegare:malice", malice(10));
        neo.put("kegare:roll", malice(1));
        neo.put("curios:inventory", new CompoundTag());
        file.put("neoforge:attachments", neo);
        CompoundTag fabric = new CompoundTag();
        fabric.put("kegare:malice", malice(99));
        file.put("fabric:attachments", fabric);

        Map<String, String> rows = PlayerDataCodec.fromPlayerFile(file);
        assertEquals(java.util.Set.of("kegare:malice", "kegare:roll"), rows.keySet(), "only Kegare's");
        assertEquals("{\"capacity\":99}", rows.get("kegare:malice"));
        assertEquals("{\"capacity\":1}", rows.get("kegare:roll"));
    }

    @Test
    void kegaresKeysComeOutOfBothAndOthersStay() {
        CompoundTag file = new CompoundTag();
        CompoundTag fabric = new CompoundTag();
        fabric.put("kegare:malice", malice(1));
        fabric.put("trinkets:inventory", new CompoundTag());
        file.put("fabric:attachments", fabric);
        CompoundTag neo = new CompoundTag();
        neo.put("kegare:roll", malice(2));
        file.put("neoforge:attachments", neo);

        assertEquals(2, PlayerDataCodec.stripFromPlayerFile(file));
        assertFalse(file.getCompoundOrEmpty("fabric:attachments").contains("kegare:malice"));
        assertTrue(file.getCompoundOrEmpty("fabric:attachments").contains("trinkets:inventory"), "another mod's stays with the world");
        assertFalse(file.contains("neoforge:attachments"), "emptied, so gone");
    }

    @Test
    void rowsGoBackIntoTheFabricKey() {
        CompoundTag file = new CompoundTag();
        PlayerDataCodec.toPlayerFile(file, Map.of("kegare:malice", "{\"capacity\":5}"));
        assertEquals(5, file.getCompoundOrEmpty("fabric:attachments").getCompoundOrEmpty("kegare:malice").getIntOr("capacity", 0));
        PlayerDataCodec.toPlayerFile(file, Map.of());
        assertFalse(file.contains("fabric:attachments"), "no rows, nothing of Kegare's left");
    }
}
