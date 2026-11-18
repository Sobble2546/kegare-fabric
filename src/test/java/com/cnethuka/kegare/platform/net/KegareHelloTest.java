package com.cnethuka.kegare.platform.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

/** The hello's wire format is part of the network contract between 1.21.11 and 26.x builds: it must not move. */
class KegareHelloTest {
    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }

    private static byte[] bytes(FriendlyByteBuf buf) {
        byte[] out = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), out);
        return out;
    }

    @Test
    void serverHelloRoundTrips() {
        var hello = new KegareHello.S2C("28", List.of(new KegareHello.Entry(id("item"), Identifier.fromNamespaceAndPath("kegare", "talisman"), 1658),
                new KegareHello.Entry(id("entity_type"), Identifier.fromNamespaceAndPath("kegare", "orb"), 161)));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        KegareHello.S2C.STREAM_CODEC.encode(buf, hello);
        assertEquals(hello, KegareHello.S2C.STREAM_CODEC.decode(buf));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void serverHelloBytesAreFixed() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        KegareHello.S2C.STREAM_CODEC.encode(buf, new KegareHello.S2C("28", List.of(new KegareHello.Entry(id("item"), Identifier.fromNamespaceAndPath("kegare", "x"), 300))));
        FriendlyByteBuf expected = new FriendlyByteBuf(Unpooled.buffer());
        expected.writeUtf("28");
        expected.writeVarInt(1);
        expected.writeUtf("minecraft:item");
        expected.writeUtf("kegare:x");
        expected.writeVarInt(300);
        assertArrayEquals(bytes(expected), bytes(buf), "version string, entry count, then registry name, entry name and varint id per entry");
    }

    @Test
    void clientAnswerBytesAreFixed() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        KegareHello.C2S.STREAM_CODEC.encode(buf, new KegareHello.C2S("28"));
        assertArrayEquals(new byte[] {2, '2', '8'}, bytes(buf));
        assertEquals(new KegareHello.C2S("28"), KegareHello.C2S.STREAM_CODEC.decode(buf));
    }

    @Test
    void hugeEntryCountsAreRefused() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf("28");
        buf.writeVarInt(KegareHello.MAX_ENTRIES + 1);
        assertThrows(IllegalArgumentException.class, () -> KegareHello.S2C.STREAM_CODEC.decode(buf));
    }
}
