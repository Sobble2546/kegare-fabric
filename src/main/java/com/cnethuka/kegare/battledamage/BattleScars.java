package com.cnethuka.kegare.battledamage;

import com.cnethuka.kegare.platform.saved.SavedDataCompat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.cnethuka.kegare.platform.NeoForge;
import com.cnethuka.kegare.platform.event.tick.LevelTickEvent;

/**
 * RESTORING mode (DESIGN §3.24): every block battle damage broke comes back, piece by piece, once the fight has moved on. Saved with
 * the dimension ({@code data/kegare_battle_scars.dat}), so a restart loses nothing. A scar is one hit's blocks; once no battle damage
 * has happened within {@code quietRadius} of it for {@code delay}, it rebuilds one block every {@code interval} ticks: solid blocks
 * first, lowest first, so what hangs on them has something to hang on. A block comes back, with its block entity's contents, only
 * where its space is empty again: never over something placed since (a container that can't come back drops what it held, so
 * nothing is lost). A block with someone in it or right beside it waits for them to move while the rest of the scar comes back round
 * them. Scars made in RESTORING mode still rebuild if the mode changes later: they were promised.
 */
public final class BattleScars extends SavedData {
    private static final String NAME = "kegare_battle_scars";

    /** One block to bring back: what it was, and its block entity's data (a container's contents) if it had one. */
    public record Piece(BlockPos pos, BlockState state, @Nullable CompoundTag blockEntity) {
        static final Codec<Piece> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Piece::pos),
                BlockState.CODEC.fieldOf("state").forGetter(Piece::state),
                CompoundTag.CODEC.optionalFieldOf("blockEntity").forGetter(p -> Optional.ofNullable(p.blockEntity())))
                .apply(i, (pos, state, entity) -> new Piece(pos, state, entity.orElse(null))));
    }

    static final class Scar {
        final Vec3 centre;
        final List<Piece> pieces;
        long quietSince;
        long nextAt;

        Scar(Vec3 centre, List<Piece> pieces, long quietSince) {
            this.centre = centre;
            this.pieces = pieces;
            this.quietSince = quietSince;
        }

        /** The file layout is the one this saved before 1.21.11: x, y, z, quietSince and the pieces (pos as an int array, state as Name/Properties). */
        static final Codec<Scar> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("x").forGetter(s -> s.centre.x),
                Codec.DOUBLE.fieldOf("y").forGetter(s -> s.centre.y),
                Codec.DOUBLE.fieldOf("z").forGetter(s -> s.centre.z),
                Codec.LONG.fieldOf("quietSince").forGetter(s -> s.quietSince),
                Piece.CODEC.listOf().fieldOf("pieces").forGetter(s -> s.pieces))
                .apply(i, (x, y, z, quietSince, pieces) -> new Scar(new Vec3(x, y, z), new ArrayList<>(pieces), quietSince)));
    }

    private static final Codec<BattleScars> CODEC = RecordCodecBuilder.create(i -> i.group(
            Scar.CODEC.listOf().optionalFieldOf("scars", List.of()).forGetter(d -> d.scars))
            .apply(i, scars -> {
                BattleScars data = new BattleScars();
                for (Scar scar : scars) if (!scar.pieces.isEmpty()) data.scars.add(scar);
                return data;
            }));

    private static final SavedDataType<BattleScars> TYPE = SavedDataCompat.type(NAME, BattleScars::new, CODEC);

    private final List<Scar> scars = new ArrayList<>();

    static void init() {
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post e) -> {
            if (e.getLevel() instanceof ServerLevel level) get(level).tick(level);
        });
    }

    public static BattleScars get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** How many blocks are still waiting to come back. */
    public int pending() {
        int n = 0;
        for (Scar s : scars) n += s.pieces.size();
        return n;
    }

    void add(ServerLevel level, Vec3 centre, List<Piece> pieces) {
        List<Piece> sorted = new ArrayList<>(pieces);
        sorted.sort(Comparator.comparing((Piece p) -> p.state.isCollisionShapeFullBlock(level, p.pos) ? 0 : 1)
                .thenComparingInt(p -> p.pos.getY())
                .thenComparingDouble(p -> Vec3.atCenterOf(p.pos).distanceToSqr(centre)));
        scars.add(new Scar(centre, sorted, level.getGameTime()));
        setDirty();
    }

    /** Battle damage at {@code at}: the fight is still going there, so scars near it wait again. */
    static void fightNear(ServerLevel level, Vec3 at) {
        BattleScars data = get(level);
        double r = BattleDamageConfig.RESTORE_QUIET_RADIUS.get();
        for (Scar s : data.scars) {
            if (s.centre.distanceToSqr(at) <= r * r) {
                s.quietSince = level.getGameTime();
                data.setDirty();
            }
        }
    }

    /** Drops every pending rebuild (tests). */
    void forget() {
        scars.clear();
        setDirty();
    }

    /** Every scar starts rebuilding now (the debug command, and filming). */
    public void restoreNow(ServerLevel level) {
        long delay = Math.round(BattleDamageConfig.RESTORE_DELAY.get() * 20);
        for (Scar s : scars) s.quietSince = level.getGameTime() - delay;
        setDirty();
    }

    void tick(ServerLevel level) {
        if (scars.isEmpty()) return;
        long now = level.getGameTime();
        long delay = Math.round(BattleDamageConfig.RESTORE_DELAY.get() * 20);
        int interval = BattleDamageConfig.RESTORE_INTERVAL.get();
        for (Iterator<Scar> it = scars.iterator(); it.hasNext(); ) {
            Scar s = it.next();
            if (now - s.quietSince < delay || now < s.nextAt) continue;
            if (s.pieces.isEmpty()) {
                it.remove();
                setDirty();
                continue;
            }
            // The first in order that can come back now: its chunk is there and nobody is standing in it or right beside it. One person
            // in the crater must not hold the whole scar up, and the blocks round them wait so they're never walled into a pit.
            int next = -1;
            for (int k = 0; k < s.pieces.size() && next < 0; k++) {
                Piece p = s.pieces.get(k);
                if (level.isLoaded(p.pos) && !occupied(level, p)) next = k;
            }
            if (next < 0) { // all waiting: try again a little later
                s.nextAt = now + 20;
                continue;
            }
            Piece piece = s.pieces.remove(next);
            restore(level, piece);
            s.nextAt = now + interval;
            setDirty();
            if (s.pieces.isEmpty()) it.remove();
        }
    }

    /** Something alive stands in its space or right beside it (within a block across): it waits for them to move. */
    static boolean occupied(ServerLevel level, Piece piece) {
        if (piece.state.getCollisionShape(level, piece.pos).isEmpty()) return false;
        return !level.getEntitiesOfClass(LivingEntity.class, new AABB(piece.pos).inflate(1.0, 0.0, 1.0), LivingEntity::isAlive).isEmpty();
    }

    static void restore(ServerLevel level, Piece piece) {
        BlockState now = level.getBlockState(piece.pos);
        if (!now.isAir() && !now.canBeReplaced()) { // something else is there now: never over it
            dropContents(level, piece);
            return;
        }
        // No shape updates while it's put back: what hangs on a block that isn't back yet must not pop off and drop.
        level.setBlock(piece.pos, piece.state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        if (piece.blockEntity != null) {
            BlockEntity entity = level.getBlockEntity(piece.pos);
            if (entity != null) {
                entity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), piece.blockEntity));
                entity.setChanged();
                level.sendBlockUpdated(piece.pos, piece.state, piece.state, Block.UPDATE_CLIENTS);
            }
        }
        level.updateNeighborsAt(piece.pos, piece.state.getBlock());
        level.playSound(null, piece.pos, piece.state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.5f, 1.2f);
    }

    /** A container that can't come back drops what it held where it stood, so nothing is ever lost. */
    private static void dropContents(ServerLevel level, Piece piece) {
        if (piece.blockEntity == null || !piece.blockEntity.contains("Items")) return;
        NonNullList<ItemStack> items = NonNullList.withSize(256, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), piece.blockEntity), items);
        Containers.dropContents(level, piece.pos, items);
    }

    // ---------------------------------------------------------------- the file's tag (the codec's view, for a restart round trip)

    /** What the world file holds, as a tag: {@code scars} is a list of {x, y, z, quietSince, pieces}. */
    CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        Tag written = CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), this).getOrThrow();
        tag.merge((CompoundTag) written);
        return tag;
    }

    static BattleScars load(CompoundTag tag, HolderLookup.Provider registries) {
        return CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow();
    }
}
