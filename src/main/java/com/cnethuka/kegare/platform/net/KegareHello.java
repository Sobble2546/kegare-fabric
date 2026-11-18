package com.cnethuka.kegare.platform.net;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The configuration-phase handshake (DESIGN §3.5). The server sends {@link S2C}: its network version and, for every Kegare entry in
 * the entity type, item and sound event registries, the raw id it numbers it with. The client answers {@link C2S} with its own version.
 * Fabric does not check versions on play channels, so this is the whole version check, and it is what the presence gate rests on.
 */
public final class KegareHello {
    /** Most entries a hello may carry (Kegare registers far fewer; the limit keeps a hostile server from asking for a huge list). */
    static final int MAX_ENTRIES = 4096;

    private KegareHello() {}

    /** One Kegare entry of a synced registry and the server's raw id for it. */
    public record Entry(Identifier registry, Identifier name, int serverId) {
        static final StreamCodec<FriendlyByteBuf, Entry> CODEC = StreamCodec.of(
                (buf, e) -> {
                    buf.writeIdentifier(e.registry());
                    buf.writeIdentifier(e.name());
                    buf.writeVarInt(e.serverId());
                },
                buf -> new Entry(buf.readIdentifier(), buf.readIdentifier(), buf.readVarInt()));
    }

    public record S2C(String version, List<Entry> entries) implements CustomPacketPayload {
        public static final Type<S2C> TYPE = new Type<>(Identifier.fromNamespaceAndPath("kegare", "hello"));
        public static final StreamCodec<FriendlyByteBuf, S2C> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUtf(p.version(), 64);
                    buf.writeVarInt(p.entries().size());
                    for (Entry e : p.entries()) Entry.CODEC.encode(buf, e);
                },
                buf -> {
                    String version = buf.readUtf(64);
                    int n = buf.readVarInt();
                    if (n < 0 || n > MAX_ENTRIES) throw new IllegalArgumentException("Too many hello entries: " + n);
                    List<Entry> entries = new ArrayList<>(n);
                    for (int i = 0; i < n; i++) entries.add(Entry.CODEC.decode(buf));
                    return new S2C(version, List.copyOf(entries));
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record C2S(String version) implements CustomPacketPayload {
        public static final Type<C2S> TYPE = new Type<>(Identifier.fromNamespaceAndPath("kegare", "hello_ack"));
        public static final StreamCodec<FriendlyByteBuf, C2S> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> buf.writeUtf(p.version(), 64), buf -> new C2S(buf.readUtf(64)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
