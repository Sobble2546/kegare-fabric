package com.cnethuka.kegare;

import com.mojang.authlib.GameProfile;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ServerLevelData;
import com.cnethuka.kegare.platform.FakePlayerFactory;
import com.cnethuka.kegare.platform.NeoForge;
import com.cnethuka.kegare.platform.Players;
import com.cnethuka.kegare.platform.event.entity.living.LivingDeathEvent;

/** Helpers for tests on the testframework's throwaway server. */
public final class TestServer {
    private TestServer() {}

    /** A fake player in the overworld with a stable UUID per name. */
    public static ServerPlayer player(MinecraftServer server, String name) {
        UUID uuid = UUID.nameUUIDFromBytes(("kegare-test:" + name).getBytes(StandardCharsets.UTF_8));
        return FakePlayerFactory.get(server.overworld(), new GameProfile(uuid, name));
    }

    /** A test player that keeps REAL advancements and counts as a real player to Kegare's code ({@link TestPlayer}, {@link Players#treatAsReal}). */
    public static ServerPlayer playerWithAdvancements(MinecraftServer server, String name) {
        UUID uuid = UUID.nameUUIDFromBytes(("kegare-test:" + name).getBytes(StandardCharsets.UTF_8));
        return Players.treatAsReal(new TestPlayer(server.overworld(), new GameProfile(uuid, name)));
    }

    /** A test player that can be hurt and hurt others (Fabric API's fake players can't), with real advancements ({@link TestPlayer}). */
    public static ServerPlayer damageable(MinecraftServer server, String name) {
        UUID uuid = UUID.nameUUIDFromBytes(("kegare-test:" + name).getBytes(StandardCharsets.UTF_8));
        ServerPlayer player = Players.treatAsReal(new TestPlayer(server.overworld(), new GameProfile(uuid, name)) {
            @Override
            public boolean isInvulnerableTo(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
                return false;
            }

            @Override
            public boolean canHarmPlayer(net.minecraft.world.entity.player.Player other) {
                return true;
            }

            /**
             * A test player dies with no connection to tell and not calling super skips the death mixin; a real player's first fires
             * LivingDeathEvent (cancellable).
             */
            @Override
            public void die(net.minecraft.world.damagesource.DamageSource source) {
                if (NeoForge.EVENT_BUS.post(new LivingDeathEvent(this, source)).isCanceled()) return;
                dead = true;
            }
        });
        try {
            java.lang.reflect.Field spawnProtection = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            spawnProtection.setAccessible(true);
            spawnProtection.setInt(player, 0);
        } catch (NoSuchFieldException e) {
            // 1.21.11 has no spawn protection field on ServerPlayer any more: nothing to clear.
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return player;
    }

    /** What an entity writes to its save (1.21.11 saves through a ValueOutput, with the registries and the attachments), as the tag. */
    public static net.minecraft.nbt.CompoundTag saveWithoutId(net.minecraft.world.entity.Entity entity) {
        net.minecraft.world.level.storage.TagValueOutput out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING, entity.level().registryAccess());
        entity.saveWithoutId(out);
        return out.buildResult();
    }

    /** The same with the id, as a chunk stores it ({@link net.minecraft.world.entity.Entity#save}). */
    public static net.minecraft.nbt.CompoundTag save(net.minecraft.world.entity.Entity entity) {
        net.minecraft.world.level.storage.TagValueOutput out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING, entity.level().registryAccess());
        entity.save(out);
        return out.buildResult();
    }

    /** Loads a tag onto an entity, as its own load does. */
    public static void load(net.minecraft.world.entity.Entity entity, net.minecraft.nbt.CompoundTag tag) {
        entity.load(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, entity.level().registryAccess(), tag));
    }

    /** An entity made from a saved tag (with its id), as a chunk loads it. */
    public static java.util.Optional<net.minecraft.world.entity.Entity> create(net.minecraft.nbt.CompoundTag tag, net.minecraft.world.level.Level level) {
        return net.minecraft.world.entity.EntityType.create(
                net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), tag),
                level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
    }

    /**
     * What NeoForge's {@code Entity.revive()} did: a killed (or removed) entity is alive to the game again. Health is the caller's to set. The death
     * guard ({@code dead}) and the removal reason are private to Minecraft, so the tests reach the fields.
     */
    public static void revive(net.minecraft.world.entity.Entity entity) {
        try {
            if (entity.isRemoved()) {
                java.lang.reflect.Field reason = net.minecraft.world.entity.Entity.class.getDeclaredField("removalReason");
                reason.setAccessible(true);
                reason.set(entity, null);
            }
            if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                java.lang.reflect.Field dead = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("dead");
                dead.setAccessible(true);
                dead.setBoolean(living, false);
                living.deathTime = 0;
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** FoodData has no getter or setter for the exhaustion level on 1.21.11 (only addExhaustion), so the tests reach its field. */
    public static float exhaustion(net.minecraft.world.food.FoodData food) {
        try {
            return exhaustionField().getFloat(food);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void setExhaustion(net.minecraft.world.food.FoodData food, float value) {
        try {
            exhaustionField().setFloat(food, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static java.lang.reflect.Field exhaustionField() throws NoSuchFieldException {
        java.lang.reflect.Field field = net.minecraft.world.food.FoodData.class.getDeclaredField("exhaustionLevel");
        field.setAccessible(true);
        return field;
    }

    /** Takes every test player out of the overworld again, so one test's players never get in the way of the next. */
    public static void removePlayers(MinecraftServer server) {
        if (server.overworld() == null) return; // nothing in this test touched the world
        for (ServerPlayer p : java.util.List.copyOf(server.overworld().players())) {
            server.overworld().removePlayerImmediately(p, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
    }

    public static boolean hasAdvancement(MinecraftServer server, ServerPlayer player, net.minecraft.resources.Identifier id) {
        var holder = server.getAdvancements().get(id);
        if (holder == null) throw new IllegalStateException("advancement " + id + " isn't loaded");
        return player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** The test server defines FLAT dimensions but never loads them; create them as startup does. */
    public static void ensureOverworld(MinecraftServer server) {
        // Tests of an ability's own behaviour start from a fresh player, who has every ability unlocked here. The tests
        // of the unlocks themselves turn this back on (see WithLocks).
        com.cnethuka.kegare.progression.ProgressionConfig.REQUIRE_REFINEMENT.set(false);
        // Nobody's born with the Six Eyes by chance (1% a rebirth): a stray bearer pays less for everything and sees what others don't. Tests
        // that want a bearer give the eyes themselves.
        com.cnethuka.kegare.config.KegareServerConfig.SIX_EYES_BIRTH_CHANCE.set(0.0);
        // Nor does the world break under them: a test's big hits would leave holes where the next test stands. BattleDamageTest sets
        // each mode itself.
        com.cnethuka.kegare.battledamage.BattleDamageConfig.MODE.set(com.cnethuka.kegare.battledamage.BattleDamageConfig.Mode.COSMETIC);
        if (server.overworld() == null) {
            try {
                Method createLevels = MinecraftServer.class.getDeclaredMethod("createLevels");
                createLevels.setAccessible(true);
                createLevels.invoke(server);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("couldn't create the test server's levels", e);
            }
        }
        testAreaReady(server.overworld());
    }

    /** The chunks round the origin the tests work in, held loaded for the whole run. */
    private static final int AREA = 2;

    /**
     * The tests' area, chunks -2 to 2 each way round the origin, held loaded (forced) with its entities ticking before a test runs.
     * A chunk loaded only on the spot (by a block lookup in the test) keeps its entities out of every area search
     * ({@code getEntitiesOfClass}) until the chunk system gets round to it, which inside one server-thread task depends on its worker
     * threads: a sweep then finds nobody beside its target, and sweep tests fail at random. Waits here, pumping the chunk system,
     * until every chunk's entities tick.
     */
    private static void testAreaReady(net.minecraft.server.level.ServerLevel level) {
        boolean ready = true;
        for (int x = -AREA; x <= AREA && ready; x++) {
            for (int z = -AREA; z <= AREA && ready; z++) ready = level.isPositionEntityTicking(new net.minecraft.core.BlockPos(x * 16, 0, z * 16));
        }
        if (ready) return;
        for (int x = -AREA; x <= AREA; x++) for (int z = -AREA; z <= AREA; z++) level.setChunkForced(x, z, true);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        for (int x = -AREA; x <= AREA; x++) {
            for (int z = -AREA; z <= AREA; z++) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x * 16, 0, z * 16);
                while (!level.isPositionEntityTicking(pos)) {
                    if (System.nanoTime() > deadline) throw new IllegalStateException("the test area's chunk at " + pos + " never got its entities ticking");
                    if (!level.getChunkSource().pollTask()) java.util.concurrent.locks.LockSupport.parkNanos(1_000_000);
                }
            }
        }
        // Clear what generation put there (a flat world's cows and sheep), which only show up once the entities tick: no test expects
        // them, and with no player between tests nothing spawns again.
        net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(-AREA * 16, level.getMinY(), -AREA * 16,
                (AREA + 1) * 16, level.getMaxY(), (AREA + 1) * 16);
        for (net.minecraft.world.entity.Entity e : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, area,
                e -> !(e instanceof net.minecraft.world.entity.player.Player))) {
            e.discard();
        }
    }

    /** Tests run on the server thread, so the world never ticks on its own: move game time forward by hand. */
    public static void advanceGameTime(MinecraftServer server, int ticks) {
        ServerLevelData data = (ServerLevelData) server.overworld().getLevelData();
        data.setGameTime(data.getGameTime() + ticks);
    }

    public static void onServerThread(MinecraftServer server, Runnable task) throws Exception {
        try {
            server.submit(task).get(5, TimeUnit.MINUTES);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Error error) throw error;
            if (e.getCause() instanceof Exception exception) throw exception;
            throw e;
        }
    }
}
