package com.cnethuka.kegare.platform.attachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

/** The platform AttachmentType's own logic, which never touches Fabric until it is registered. */
class AttachmentTypeTest {
    static final class Box {
        int count;
        List<String> names = new ArrayList<>();

        static final Codec<Box> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("count", 0).forGetter(b -> b.count),
                Codec.STRING.listOf().optionalFieldOf("names", List.of()).forGetter(b -> b.names)
        ).apply(i, (count, names) -> {
            Box b = new Box();
            b.count = count;
            b.names = new ArrayList<>(names);
            return b;
        }));
    }

    @Test
    void aCodecTypeRoundTripsAndCopiesIndependently() {
        AttachmentType<Box> type = AttachmentType.builder(Box::new).serialize(Box.CODEC).copyOnDeath().build();
        assertTrue(type.isSaved());
        assertTrue(type.copyOnDeath());
        Box box = type.create();
        box.count = 7;
        box.names.add("a");
        Tag tag = type.encode(box, null);
        assertNotNull(tag);
        Box back = type.decode(tag, null);
        assertEquals(7, back.count);
        assertEquals(List.of("a"), back.names);
        Box copy = type.copy(box, null);
        assertNotNull(copy);
        assertNotSame(box, copy);
        copy.names.add("b");
        assertEquals(List.of("a"), box.names, "a copy shares nothing with the original");
    }

    @Test
    void aRuntimeOnlyTypeIsNotSavedAndHasNothingToCopy() {
        AttachmentType<Box> type = AttachmentType.builder(Box::new).build();
        assertFalse(type.isSaved());
        assertFalse(type.copyOnDeath());
        assertNull(type.codec());
        assertNull(type.encode(new Box(), null));
        assertNull(type.copy(new Box(), null));
        assertThrows(IllegalStateException.class, () -> type.decode(new CompoundTag(), null));
    }

    @Test
    void aTypeHasNoIdOrFabricTypeBeforeItIsRegistered() {
        AttachmentType<Box> type = AttachmentType.builder(Box::new).build();
        assertFalse(type.isRegistered());
        assertThrows(IllegalStateException.class, type::id);
        assertThrows(IllegalStateException.class, type::fabric);
    }

    /** NeoForge style: tag in, tag out, and null for "nothing to store". */
    static final class Toggle {
        String who;

        boolean active() {
            return who != null;
        }
    }

    static final IAttachmentSerializer<CompoundTag, Toggle> TOGGLE = new IAttachmentSerializer<>() {
        @Override
        public Toggle read(IAttachmentHolder holder, CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
            Toggle t = new Toggle();
            if (tag.contains("Who")) t.who = tag.getStringOr("Who", "");
            return t;
        }

        @Override
        public CompoundTag write(Toggle t, net.minecraft.core.HolderLookup.Provider provider) {
            if (!t.active()) return null;
            CompoundTag tag = new CompoundTag();
            tag.putString("Who", t.who);
            return tag;
        }
    };

    @Test
    void aSerializerTypeWritesNothingWhenItSaysSoAndItsCodecStoresAnEmptyCompound() {
        AttachmentType<Toggle> type = AttachmentType.builder(Toggle::new).serialize(TOGGLE).build();
        assertTrue(type.isSaved());
        assertNull(type.encode(new Toggle(), null), "NeoForge stored nothing for it; the row/key is skipped");
        // Fabric stores through the codec, which can't say "nothing": an empty compound that reads back as the empty value.
        Tag viaCodec = type.codec().encodeStart(NbtOps.INSTANCE, new Toggle()).getOrThrow();
        assertEquals(new CompoundTag(), viaCodec);
        assertFalse(type.codec().parse(NbtOps.INSTANCE, viaCodec).getOrThrow().active());
    }

    @Test
    void aSerializerTypeRoundTripsThroughItsCodec() {
        AttachmentType<Toggle> type = AttachmentType.builder(Toggle::new).serialize(TOGGLE).build();
        Toggle on = new Toggle();
        on.who = "puppet";
        Tag tag = type.codec().encodeStart(NbtOps.INSTANCE, on).getOrThrow();
        assertEquals("puppet", type.codec().parse(NbtOps.INSTANCE, tag).getOrThrow().who);
        Toggle copy = type.copy(on, null);
        assertNotNull(copy);
        assertEquals("puppet", copy.who);
        assertNull(type.copy(new Toggle(), null), "nothing stored, nothing to copy");
    }

    @Test
    void aSerializerFailureIsAnErrorResultNotAnException() {
        IAttachmentSerializer<CompoundTag, Toggle> broken = new IAttachmentSerializer<>() {
            @Override
            public Toggle read(IAttachmentHolder holder, CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
                throw new IllegalArgumentException("bad tag");
            }

            @Override
            public CompoundTag write(Toggle t, net.minecraft.core.HolderLookup.Provider provider) {
                throw new IllegalStateException("cannot write");
            }
        };
        AttachmentType<Toggle> type = AttachmentType.builder(Toggle::new).serialize(broken).build();
        assertTrue(type.codec().parse(NbtOps.INSTANCE, new CompoundTag()).error().isPresent());
        assertTrue(type.codec().encodeStart(NbtOps.INSTANCE, new Toggle()).error().isPresent());
    }
}
