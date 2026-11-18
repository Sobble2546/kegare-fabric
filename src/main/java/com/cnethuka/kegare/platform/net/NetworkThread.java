package com.cnethuka.kegare.platform.net;

import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The payloads handled on the connection's own thread (DESIGN §3.29). Fabric only runs handlers on the main thread, so two mixins
 * ({@code ServerboundCustomPayloadPacketMixin}, {@code ClientboundCustomPayloadPacketMixin}) call {@link #handleOnServer} /
 * {@link #handleOnClient} from the packet's own {@code handle}, which runs on the netty thread before any listener sees the packet.
 */
public final class NetworkThread {
    private record Entry(Object type, IPayloadHandler<?> handler) {}

    private static final Map<String, Entry> SERVER = new ConcurrentHashMap<>();
    private static final Map<String, Entry> CLIENT = new ConcurrentHashMap<>();
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean FELL_BACK = new AtomicBoolean();

    private NetworkThread() {}

    static <T extends CustomPacketPayload> void registerServer(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler) {
        SERVER.put(type.id().toString(), new Entry(type, handler));
    }

    static <T extends CustomPacketPayload> void registerClient(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler) {
        CLIENT.put(type.id().toString(), new Entry(type, handler));
    }

    /** True if this C2S payload is handled on the network thread. */
    public static boolean isServerPayload(CustomPacketPayload payload) {
        return payload != null && SERVER.containsKey(payload.type().id().toString());
    }

    public static boolean isClientPayload(CustomPacketPayload payload) {
        return payload != null && CLIENT.containsKey(payload.type().id().toString());
    }

    /** Runs the handler of a network-thread C2S payload for a player. Returns false if the payload is not one. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean handleOnServer(CustomPacketPayload payload, IPayloadContext context) {
        Entry entry = payload == null ? null : SERVER.get(payload.type().id().toString());
        if (entry == null) return false;
        ((IPayloadHandler) entry.handler()).handle(payload, context);
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean handleOnClient(CustomPacketPayload payload, IPayloadContext context) {
        Entry entry = payload == null ? null : CLIENT.get(payload.type().id().toString());
        if (entry == null) return false;
        ((IPayloadHandler) entry.handler()).handle(payload, context);
        return true;
    }

    /** The ordinary receiver of a network-thread payload: it runs only if the hook did not (the main thread, so late), and says so once. */
    static <T extends CustomPacketPayload> IPayloadHandler<T> fallback(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler) {
        return (payload, context) -> {
            if (FELL_BACK.compareAndSet(false, true)) {
                LOGGER.warn("{} was handled on the main thread: the network-thread hook did not run, so sub-tick timing is off (DESIGN §3.29)", type.id());
            }
            handler.handle(payload, context);
        };
    }

    /** For tests only: forgets this payload type's hook handlers (and only this one's). */
    static void forgetForTests(CustomPacketPayload.Type<?> type) {
        SERVER.remove(type.id().toString());
        CLIENT.remove(type.id().toString());
    }
}
