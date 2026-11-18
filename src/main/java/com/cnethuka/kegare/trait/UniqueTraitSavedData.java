package com.cnethuka.kegare.trait;

import com.cnethuka.kegare.platform.saved.SavedDataCompat;
import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import com.cnethuka.kegare.database.KegareDatabase;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * World-level record for unique traits ({@code data/kegare_unique_traits.dat} in the overworld). It is the source
 * of truth: player copies are fixed on login ({@link TraitService#reconcile}).
 * <ul>
 *   <li>{@code bearers}: unique trait → current bearer.</li>
 *   <li>{@code pending}: inheritable traits between bearers (succession in progress or waiting for an heir).
 *       A pending trait can't be claimed by a birth roll.</li>
 *   <li>{@code revoked}: traits taken from an offline player; stripped from them on their next login.</li>
 *   <li>{@code lastSeen}: last login/logout time (epoch millis) per player, for the inactivity rule.</li>
 * </ul>
 */
public final class UniqueTraitSavedData extends SavedData {
    /**
     * With Postgres storage (DESIGN §3.33) this record is network-wide: it answers from memory without waiting and reports
     * every change to the backend, which is the real authority. Nothing is saved in the world then.
     */
    public interface Backend {
        void claimed(Identifier trait, UUID player, boolean force);

        void released(Identifier trait, UUID player);

        void succession(Identifier trait, Pending pending);

        void revoked(UUID player, Identifier trait);

        void revocationsTaken(UUID player);

        void seen(UUID player, long epochMillis);
    }

    /**
     * A trait between bearers.
     *
     * @param killer  the player who caused the loss, if any (KILLER transfer mode)
     * @param dueTick when it may pass on: overworld game time, or with Postgres storage wall-clock time in ticks (game time differs per server)
     */
    public record Pending(UUID previousBearer, Optional<UUID> killer, long dueTick) {}

    private static final String NAME = "kegare_unique_traits";
    /** The same compound the NeoForge build wrote, so an existing {@code data/kegare_unique_traits.dat} reads back. */
    private static final Codec<UniqueTraitSavedData> CODEC = CompoundTag.CODEC.xmap(UniqueTraitSavedData::load, d -> d.save(new CompoundTag()));
    private static final SavedDataType<UniqueTraitSavedData> TYPE = SavedDataCompat.type(NAME, UniqueTraitSavedData::new, CODEC);

    private final Map<Identifier, UUID> bearers = new HashMap<>();
    private final Map<Identifier, Pending> pending = new LinkedHashMap<>();
    private final Map<UUID, Set<Identifier>> revoked = new HashMap<>();
    private final Map<UUID, Long> lastSeen = new HashMap<>();
    @Nullable
    private final Backend backend;
    /** Everyone online across the network (Postgres storage only). */
    private final Set<UUID> networkOnline = new HashSet<>();

    public UniqueTraitSavedData() {
        this(null);
    }

    public UniqueTraitSavedData(@Nullable Backend backend) {
        this.backend = backend;
    }

    public static UniqueTraitSavedData get(MinecraftServer server) {
        KegareDatabase database = KegareDatabase.current();
        return database != null ? database.traitRecord() : inWorld(server);
    }

    /** The world's own copy regardless of storage mode: what a migration reads and an export writes. */
    public static UniqueTraitSavedData inWorld(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Whether this is the network's record rather than one world's. */
    public boolean isNetworked() {
        return backend != null;
    }

    /** Online anywhere on the network, this server or another. */
    public boolean onlineOnNetwork(UUID player) {
        return networkOnline.contains(player);
    }

    /** Replaces the contents with the network record's current state (lastSeen is merged, not cleared). */
    public void replaceAll(Map<Identifier, UUID> bearers, Map<Identifier, Pending> pending, Map<UUID, Set<Identifier>> revoked, Map<UUID, Long> lastSeen, Set<UUID> online) {
        this.bearers.clear();
        this.bearers.putAll(bearers);
        this.pending.clear();
        this.pending.putAll(pending);
        this.revoked.clear();
        revoked.forEach((player, traits) -> this.revoked.put(player, new HashSet<>(traits)));
        this.lastSeen.putAll(lastSeen);
        this.networkOnline.clear();
        this.networkOnline.addAll(online);
    }

    /** Players with a revocation waiting. */
    public Set<UUID> revokedPlayers() {
        return Set.copyOf(revoked.keySet());
    }

    /** Read views for import and export. */
    public Map<UUID, Set<Identifier>> revokedView() {
        return Collections.unmodifiableMap(revoked);
    }

    public Map<UUID, Long> lastSeenView() {
        return Collections.unmodifiableMap(lastSeen);
    }

    // ------------------------------------------------------------------ bearers

    public Optional<UUID> bearer(Identifier trait) {
        return Optional.ofNullable(bearers.get(trait));
    }

    public Map<Identifier, UUID> bearers() {
        return Collections.unmodifiableMap(bearers);
    }

    /** Normal claim (birth roll, login restore). Fails if someone else holds it or it is pending succession. */
    public boolean tryClaim(Identifier trait, UUID player) {
        UUID current = bearers.get(trait);
        if (current != null) return current.equals(player);
        if (pending.containsKey(trait)) return false;
        bearers.put(trait, player);
        if (backend != null) backend.claimed(trait, player, false);
        else setDirty();
        return true;
    }

    /** Admin grant or succession: overrides a pending succession, but never steals from a current bearer. */
    public boolean forceClaim(Identifier trait, UUID player) {
        UUID current = bearers.get(trait);
        if (current != null && !current.equals(player)) return false;
        pending.remove(trait);
        bearers.put(trait, player);
        if (backend != null) backend.claimed(trait, player, true);
        else setDirty();
        return true;
    }

    /** Releases the trait if {@code player} holds it. */
    public void release(Identifier trait, UUID player) {
        if (player.equals(bearers.get(trait))) {
            bearers.remove(trait);
            if (backend != null) backend.released(trait, player);
            else setDirty();
        }
    }

    public Set<Identifier> heldBy(UUID player) {
        Set<Identifier> out = new HashSet<>();
        bearers.forEach((trait, uuid) -> {
            if (uuid.equals(player)) out.add(trait);
        });
        return out;
    }

    // ------------------------------------------------------------------ succession

    public Optional<Pending> pending(Identifier trait) {
        return Optional.ofNullable(pending.get(trait));
    }

    public Map<Identifier, Pending> pendingView() {
        return Collections.unmodifiableMap(pending);
    }

    public void startSuccession(Identifier trait, Pending entry) {
        pending.put(trait, entry);
        if (backend != null) backend.succession(trait, entry);
        else setDirty();
    }

    // ------------------------------------------------------------------ offline revocation

    public void revoke(UUID player, Identifier trait) {
        revoked.computeIfAbsent(player, u -> new HashSet<>()).add(trait);
        if (backend != null) backend.revoked(player, trait);
        else setDirty();
    }

    /** Returns and clears the traits revoked from {@code player} while they were offline. */
    public Set<Identifier> takeRevocations(UUID player) {
        Set<Identifier> out = revoked.remove(player);
        if (out == null) return Set.of();
        if (backend != null) backend.revocationsTaken(player);
        else setDirty();
        return out;
    }

    // ------------------------------------------------------------------ activity

    public Optional<Long> lastSeen(UUID player) {
        return Optional.ofNullable(lastSeen.get(player));
    }

    public void markSeen(UUID player, long epochMillis) {
        lastSeen.put(player, epochMillis);
        if (backend != null) backend.seen(player, epochMillis);
        else setDirty();
    }

    // ------------------------------------------------------------------ persistence

    private CompoundTag save(CompoundTag tag) {
        ListTag bearerList = new ListTag();
        bearers.forEach((trait, uuid) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("trait", trait.toString());
            entry.store("bearer", UUIDUtil.CODEC, uuid);
            bearerList.add(entry);
        });
        tag.put("bearers", bearerList);

        ListTag pendingList = new ListTag();
        pending.forEach((trait, p) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("trait", trait.toString());
            entry.store("previous", UUIDUtil.CODEC, p.previousBearer());
            p.killer().ifPresent(k -> entry.store("killer", UUIDUtil.CODEC, k));
            entry.putLong("due", p.dueTick());
            pendingList.add(entry);
        });
        tag.put("pending", pendingList);

        ListTag revokedList = new ListTag();
        revoked.forEach((uuid, traits) -> traits.forEach(trait -> {
            CompoundTag entry = new CompoundTag();
            entry.store("player", UUIDUtil.CODEC, uuid);
            entry.putString("trait", trait.toString());
            revokedList.add(entry);
        }));
        tag.put("revoked", revokedList);

        ListTag seenList = new ListTag();
        lastSeen.forEach((uuid, millis) -> {
            CompoundTag entry = new CompoundTag();
            entry.store("player", UUIDUtil.CODEC, uuid);
            entry.putLong("millis", millis);
            seenList.add(entry);
        });
        tag.put("last_seen", seenList);
        return tag;
    }

    private static UniqueTraitSavedData load(CompoundTag tag) {
        UniqueTraitSavedData data = new UniqueTraitSavedData();
        for (CompoundTag e : compounds(tag, "bearers")) {
            Identifier trait = trait(e);
            Optional<UUID> bearer = e.read("bearer", UUIDUtil.CODEC);
            if (trait != null && bearer.isPresent()) data.bearers.put(trait, bearer.get());
        }
        for (CompoundTag e : compounds(tag, "pending")) {
            Identifier trait = trait(e);
            Optional<UUID> previous = e.read("previous", UUIDUtil.CODEC);
            if (trait == null || previous.isEmpty()) continue;
            data.pending.put(trait, new Pending(previous.get(), e.read("killer", UUIDUtil.CODEC), e.getLongOr("due", 0L)));
        }
        for (CompoundTag e : compounds(tag, "revoked")) {
            Identifier trait = trait(e);
            Optional<UUID> player = e.read("player", UUIDUtil.CODEC);
            if (trait != null && player.isPresent()) data.revoked.computeIfAbsent(player.get(), u -> new HashSet<>()).add(trait);
        }
        for (CompoundTag e : compounds(tag, "last_seen")) {
            e.read("player", UUIDUtil.CODEC).ifPresent(player -> data.lastSeen.put(player, e.getLongOr("millis", 0L)));
        }
        return data;
    }

    @Nullable
    private static Identifier trait(CompoundTag entry) {
        return Identifier.tryParse(entry.getStringOr("trait", ""));
    }

    private static Iterable<CompoundTag> compounds(CompoundTag tag, String key) {
        java.util.List<CompoundTag> out = new java.util.ArrayList<>();
        for (Tag entry : tag.getListOrEmpty(key)) if (entry instanceof CompoundTag compound) out.add(compound);
        return out;
    }
}
