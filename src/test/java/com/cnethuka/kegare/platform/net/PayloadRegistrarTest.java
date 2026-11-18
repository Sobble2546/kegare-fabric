package com.cnethuka.kegare.platform.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PayloadRegistrarTest {
    record Ping(int n) implements CustomPacketPayload {
        static final Type<Ping> TYPE = new Type<>(Identifier.fromNamespaceAndPath("kegare", "unit_test_ping"));
        static final StreamCodec<FriendlyByteBuf, Ping> CODEC = StreamCodec.of((b, p) -> b.writeVarInt(p.n()), b -> new Ping(b.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Ping p, IPayloadContext c) {}
    }

    record Other() implements CustomPacketPayload {
        // same channel id as Ping, but a different Type object
        static final Type<Other> TYPE = new Type<>(Identifier.fromNamespaceAndPath("kegare", "unit_test_ping"));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Records what the registrar asked the platform to do. */
    static final class Recorder implements PayloadRegistrar.Backend {
        final List<String> calls = new ArrayList<>();

        @Override
        public <T extends CustomPacketPayload> void registerPlayS2C(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
            calls.add("s2c " + type.id());
        }

        @Override
        public <T extends CustomPacketPayload> void registerPlayC2S(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
            calls.add("c2s " + type.id());
        }

        @Override
        public <T extends CustomPacketPayload> void serverReceiver(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler) {
            calls.add("server receiver " + type.id());
        }

        @Override
        public <T extends CustomPacketPayload> void clientReceiver(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler) {
            calls.add("client receiver " + type.id());
        }
    }

    /** Only what these tests registered is forgotten: the suite may share a JVM with the real registrations. */
    @BeforeEach
    @AfterEach
    void forget() {
        PayloadRegistrar.forgetForTests(Ping.TYPE);
        PayloadRegistrar.forgetForTests(Other.TYPE);
    }

    @Test
    void aPayloadIsRegisteredOncePerDirectionWhoeverAsks() {
        Recorder backend = new Recorder();
        PayloadRegistrar a = new RegisterPayloadHandlersEvent(backend).registrar("1");
        PayloadRegistrar b = new RegisterPayloadHandlersEvent(backend).registrar("1");
        a.playToClient(Ping.TYPE, Ping.CODEC, Ping::handle);
        b.playToClient(Ping.TYPE, Ping.CODEC, Ping::handle);
        assertEquals(List.of("s2c kegare:unit_test_ping", "client receiver kegare:unit_test_ping"), backend.calls);
        a.playToServer(Ping.TYPE, Ping.CODEC, Ping::handle);
        assertEquals(4, backend.calls.size(), "the other direction is its own registration");
    }

    @Test
    void twoTypesOnOneChannelIsABug() {
        Recorder backend = new Recorder();
        PayloadRegistrar registrar = new RegisterPayloadHandlersEvent(backend).registrar("1");
        registrar.playToClient(Ping.TYPE, Ping.CODEC, Ping::handle);
        assertThrows(IllegalStateException.class, () -> registrar.playToClient(Other.TYPE, StreamCodec.unit(new Other()), (p, c) -> {}));
    }

    @Test
    void networkThreadPayloadsGoToTheHookAndKeepAnOrdinaryReceiverToo() {
        Recorder backend = new Recorder();
        PayloadRegistrar network = new RegisterPayloadHandlersEvent(backend).registrar("1").executesOn(HandlerThread.NETWORK);
        network.playToServer(Ping.TYPE, Ping.CODEC, Ping::handle);
        assertTrue(NetworkThread.isServerPayload(new Ping(1)));
        assertFalse(NetworkThread.isClientPayload(new Ping(1)));
        assertEquals(List.of("c2s kegare:unit_test_ping", "server receiver kegare:unit_test_ping"), backend.calls);
    }

    @Test
    void anOrdinaryPayloadIsNotAHookPayload() {
        Recorder backend = new Recorder();
        new RegisterPayloadHandlersEvent(backend).registrar("1").playToServer(Ping.TYPE, Ping.CODEC, Ping::handle);
        assertFalse(NetworkThread.isServerPayload(new Ping(1)));
    }

    @Test
    void theHookRunsTheHandlerItWasGiven() {
        Recorder backend = new Recorder();
        List<Integer> seen = new ArrayList<>();
        new RegisterPayloadHandlersEvent(backend).registrar("1").executesOn(HandlerThread.NETWORK)
                .playToServer(Ping.TYPE, Ping.CODEC, (p, c) -> seen.add(p.n()));
        assertTrue(NetworkThread.handleOnServer(new Ping(7), null));
        assertEquals(List.of(7), seen);
        assertFalse(NetworkThread.handleOnClient(new Ping(7), null), "registered for the server only");
    }
}
