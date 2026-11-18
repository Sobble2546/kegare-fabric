package com.cnethuka.kegare.platform.net;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.resources.Identifier;

/**
 * Raw registry ids across a version bridge (DESIGN §3.5, "Via servers").
 * <p>
 * Through ViaVersion a 1.21.11 client on a 26.x server gets every vanilla id translated, but Kegare's entries (entity types, items,
 * sounds) arrive with the id the server numbers them with, which is not the id the client numbers them with. The hello tells the client
 * what the server's ids are; this turns a server id into the local entry when the client decodes an id-based registry codec, and a local
 * id back into the server's when it encodes one. With the same version on both ends the ids match, the table is empty and every call
 * is the identity.
 * <p>
 * The snapshot is immutable and read on the network thread, so it lives in a volatile.
 */
public final class IdRemap {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    /** What the client has locally; a seam so the table can be built without a registry. */
    public interface Local {
        /** The local raw id of a registry entry, empty if the client has no such entry. */
        OptionalInt idOf(Identifier registry, Identifier name);

        /** How many entries the registry has that are not Kegare's. A server id below this could be a vanilla id Via already translated. */
        int foreignCount(Identifier registry);
    }

    /** Both directions for one registry. Only the ids that differ are in it. */
    record Table(Map<Integer, Integer> wireToLocal, Map<Integer, Integer> localToWire) {
        static final Table EMPTY = new Table(Map.of(), Map.of());
    }

    private static final Map<Identifier, Table> NONE = Map.of();
    private static volatile Map<Identifier, Table> tables = NONE;
    private static final AtomicInteger HOOKED = new AtomicInteger();
    private static final AtomicInteger HOLDER_HOOKED = new AtomicInteger();

    private IdRemap() {}

    /** The id-registry codec hook ran (it wraps one codec per registry it is applied to): proof that the mixin applied. */
    public static void noteHooked() {
        HOOKED.incrementAndGet();
    }

    public static boolean isHooked() {
        return HOOKED.get() > 0;
    }

    /** How often the id-registry hook has wrapped a codec (a probe compares before and after, see {@link IdHooks}). */
    static int registryHooks() {
        return HOOKED.get();
    }

    /** The sound-event holder codec hook ran: proof that the second injection applied. */
    public static void noteHolderHooked() {
        HOLDER_HOOKED.incrementAndGet();
    }

    static int holderHooks() {
        return HOLDER_HOOKED.get();
    }

    public static boolean isActive() {
        return !tables.isEmpty();
    }

    /** A server id in a codec of this registry, as the local id. */
    public static int toLocal(Identifier registry, int serverId) {
        Map<Identifier, Table> snapshot = tables;
        if (snapshot.isEmpty()) return serverId;
        Table table = snapshot.get(registry);
        if (table == null) return serverId;
        Integer local = table.wireToLocal().get(serverId);
        return local == null ? serverId : local;
    }

    /** A local id, as the id the server numbers it with. */
    public static int toWire(Identifier registry, int localId) {
        Map<Identifier, Table> snapshot = tables;
        if (snapshot.isEmpty()) return localId;
        Table table = snapshot.get(registry);
        if (table == null) return localId;
        Integer wire = table.localToWire().get(localId);
        return wire == null ? localId : wire;
    }

    public static void clear() {
        tables = NONE;
    }

    /** Builds and installs the table from a hello. Returns what was installed. */
    public static Map<Identifier, Table> install(List<KegareHello.Entry> serverEntries, Local local) {
        Map<Identifier, Table> built = build(serverEntries, local);
        tables = built;
        if (!built.isEmpty() && !isHooked()) {
            LOGGER.error("This server numbers Kegare's entries differently ({} registries) but the id-codec mixin did not apply: "
                    + "Kegare's entities and items will not decode. Join a server of the same Minecraft version, or report this.", built.size());
        }
        return built;
    }

    /**
     * Server id to local id, for the entries where they differ. A registry is skipped (identity, with a warning) if one of its Kegare
     * server ids is below the local count of other entries: it could be an id Via already translated, and rewriting it would corrupt a
     * vanilla one. An entry the client does not have is skipped with a warning too.
     */
    static Map<Identifier, Table> build(List<KegareHello.Entry> serverEntries, Local local) {
        Map<Identifier, Map<Integer, Integer>> toLocal = new HashMap<>();
        Map<Identifier, Map<Integer, Integer>> toWire = new HashMap<>();
        Map<Identifier, Boolean> unsafe = new HashMap<>();
        for (KegareHello.Entry entry : serverEntries) {
            OptionalInt id = local.idOf(entry.registry(), entry.name());
            if (id.isEmpty()) {
                LOGGER.warn("The server has {} in {} and this client does not", entry.name(), entry.registry());
                continue;
            }
            if (id.getAsInt() == entry.serverId()) continue;
            if (entry.serverId() < local.foreignCount(entry.registry())) unsafe.put(entry.registry(), true);
            toLocal.computeIfAbsent(entry.registry(), k -> new HashMap<>()).put(entry.serverId(), id.getAsInt());
            toWire.computeIfAbsent(entry.registry(), k -> new HashMap<>()).put(id.getAsInt(), entry.serverId());
        }
        Map<Identifier, Table> result = new HashMap<>();
        for (Map.Entry<Identifier, Map<Integer, Integer>> e : toLocal.entrySet()) {
            if (unsafe.containsKey(e.getKey())) {
                LOGGER.warn("Not remapping {}: a Kegare id the server uses is in the range of the ids this client shares with other entries", e.getKey());
                continue;
            }
            result.put(e.getKey(), new Table(Map.copyOf(e.getValue()), Map.copyOf(toWire.get(e.getKey()))));
        }
        return Map.copyOf(result);
    }
}
