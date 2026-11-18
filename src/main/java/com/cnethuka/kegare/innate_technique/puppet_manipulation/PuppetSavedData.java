package com.cnethuka.kegare.innate_technique.puppet_manipulation;

import com.cnethuka.kegare.platform.saved.SavedDataCompat;
import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Every puppet in the world, loaded or not ({@code data/kegare_puppets.dat} in the overworld), by owner:
 * <ul>
 *   <li>the puppets' UUIDs, so the safety cap counts puppets in unloaded chunks too;</li>
 *   <li>a <b>generation</b> per owner. Each puppet remembers the generation it was made in. Rerolling away from
 *       Puppet Manipulation bumps it and forgets the list, so a puppet in an unloaded chunk finds out it's stale the
 *       moment its chunk loads, and dissolves.</li>
 * </ul>
 */
public final class PuppetSavedData extends SavedData {
    private static final String NAME = "kegare_puppets";
    /** The same compound the NeoForge build wrote ({@code Owners} list), so an existing {@code data/kegare_puppets.dat} reads back. */
    private static final Codec<PuppetSavedData> CODEC = CompoundTag.CODEC.xmap(PuppetSavedData::load, d -> d.save(new CompoundTag()));
    private static final SavedDataType<PuppetSavedData> TYPE = SavedDataCompat.type(NAME, PuppetSavedData::new, CODEC);

    private static final class Owner {
        int generation;
        final Set<UUID> puppets = new LinkedHashSet<>();
    }

    private final Map<UUID, Owner> owners = new HashMap<>();

    public static PuppetSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public int count(UUID owner) {
        Owner o = owners.get(owner);
        return o == null ? 0 : o.puppets.size();
    }

    public int generation(UUID owner) {
        Owner o = owners.get(owner);
        return o == null ? 0 : o.generation;
    }

    public boolean contains(UUID owner, UUID puppet) {
        Owner o = owners.get(owner);
        return o != null && o.puppets.contains(puppet);
    }

    public void add(UUID owner, UUID puppet) {
        if (owners.computeIfAbsent(owner, k -> new Owner()).puppets.add(puppet)) setDirty();
    }

    public void remove(UUID owner, UUID puppet) {
        Owner o = owners.get(owner);
        if (o != null && o.puppets.remove(puppet)) setDirty();
    }

    /** Every puppet of this owner is gone, including any not loaded right now (they dissolve when they load). */
    public void dissolveAll(UUID owner) {
        Owner o = owners.computeIfAbsent(owner, k -> new Owner());
        o.generation++;
        o.puppets.clear();
        setDirty();
    }

    private static PuppetSavedData load(CompoundTag tag) {
        PuppetSavedData data = new PuppetSavedData();
        for (Tag t : tag.getListOrEmpty("Owners")) {
            if (!(t instanceof CompoundTag entry)) continue;
            Owner o = new Owner();
            o.generation = entry.getIntOr("Generation", 0);
            for (Tag p : entry.getListOrEmpty("Puppets")) UUIDUtil.CODEC.parse(NbtOps.INSTANCE, p).result().ifPresent(o.puppets::add);
            entry.read("Owner", UUIDUtil.CODEC).ifPresent(id -> data.owners.put(id, o));
        }
        return data;
    }

    private CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        owners.forEach((id, o) -> {
            CompoundTag entry = new CompoundTag();
            entry.store("Owner", UUIDUtil.CODEC, id);
            entry.putInt("Generation", o.generation);
            ListTag puppets = new ListTag();
            for (UUID p : o.puppets) UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE, p).result().ifPresent(puppets::add);
            entry.put("Puppets", puppets);
            list.add(entry);
        });
        tag.put("Owners", list);
        return tag;
    }
}
