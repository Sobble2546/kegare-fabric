package com.cnethuka.kegare.platform.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class IdRemapTest {
    private static final Identifier ITEM = Identifier.fromNamespaceAndPath("minecraft", "item");
    private static final Identifier ENTITY = Identifier.fromNamespaceAndPath("minecraft", "entity_type");

    /** A client whose registries have the given Kegare ids and this many other entries each. */
    private static IdRemap.Local client(Map<String, Integer> ids, int foreign) {
        return new IdRemap.Local() {
            @Override
            public OptionalInt idOf(Identifier registry, Identifier name) {
                Integer id = ids.get(registry + "/" + name);
                return id == null ? OptionalInt.empty() : OptionalInt.of(id);
            }

            @Override
            public int foreignCount(Identifier registry) {
                return foreign;
            }
        };
    }

    private static KegareHello.Entry entry(Identifier registry, String name, int serverId) {
        return new KegareHello.Entry(registry, Identifier.fromNamespaceAndPath("kegare", name), serverId);
    }

    @AfterEach
    void reset() {
        IdRemap.clear();
    }

    @Test
    void sameVersionMeansNoTableAndEveryIdPassesThrough() {
        Map<String, Integer> ids = new HashMap<>();
        ids.put("minecraft:item/kegare:talisman", 1505);
        IdRemap.install(List.of(entry(ITEM, "talisman", 1505)), client(ids, 1500));
        assertFalse(IdRemap.isActive());
        assertEquals(1505, IdRemap.toLocal(ITEM, 1505));
        assertEquals(42, IdRemap.toWire(ITEM, 42));
    }

    @Test
    void offsetServerIdsMapToLocalIdsAndBack() {
        // the lab's numbers: items +153, entity types +4
        Map<String, Integer> ids = new HashMap<>();
        ids.put("minecraft:item/kegare:token", 1505);
        ids.put("minecraft:entity_type/kegare:orb", 157);
        IdRemap.install(List.of(entry(ITEM, "token", 1658), entry(ENTITY, "orb", 161)), client(ids, 150));
        assertTrue(IdRemap.isActive());
        assertEquals(1505, IdRemap.toLocal(ITEM, 1658));
        assertEquals(157, IdRemap.toLocal(ENTITY, 161));
        assertEquals(1658, IdRemap.toWire(ITEM, 1505));
        assertEquals(161, IdRemap.toWire(ENTITY, 157));
    }

    @Test
    void anIdOutsideTheTableIsLeftAlone() {
        Map<String, Integer> ids = new HashMap<>();
        ids.put("minecraft:item/kegare:token", 1505);
        IdRemap.install(List.of(entry(ITEM, "token", 1658)), client(ids, 150));
        assertEquals(77, IdRemap.toLocal(ITEM, 77), "a vanilla id Via already translated");
        assertEquals(77, IdRemap.toWire(ITEM, 77));
        assertEquals(1658, IdRemap.toLocal(ENTITY, 1658), "another registry has no table");
    }

    @Test
    void aServerIdInTheRangeOfSharedIdsIsNotRemapped() {
        // an older server: its Kegare id 100 could be a vanilla id Via translated, so rewriting it could corrupt a vanilla entry
        Map<String, Integer> ids = new HashMap<>();
        ids.put("minecraft:item/kegare:token", 1505);
        IdRemap.install(List.of(entry(ITEM, "token", 100)), client(ids, 1500));
        assertFalse(IdRemap.isActive());
        assertEquals(100, IdRemap.toLocal(ITEM, 100));
    }

    @Test
    void anEntryTheClientLacksIsSkipped() {
        IdRemap.install(List.of(entry(ITEM, "ghost", 1658)), client(Map.of(), 150));
        assertFalse(IdRemap.isActive());
    }

    @Test
    void clearForgetsTheTable() {
        Map<String, Integer> ids = new HashMap<>();
        ids.put("minecraft:item/kegare:token", 1505);
        IdRemap.install(List.of(entry(ITEM, "token", 1658)), client(ids, 150));
        IdRemap.clear();
        assertFalse(IdRemap.isActive());
        assertEquals(1658, IdRemap.toLocal(ITEM, 1658));
    }
}
