package com.cnethuka.kegare.test;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import net.fabricmc.fabric.api.resource.v1.DataResourceStore;
import net.fabricmc.fabric.impl.resource.FabricDataResourceStoreHolder;
import net.minecraft.SystemReport;
import net.minecraft.commands.Commands;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.notifications.EmptyNotificationService;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.server.players.UserNameToIdResolver;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.debugchart.LocalSampleLogger;
import net.minecraft.util.debugchart.SampleLogger;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.slf4j.Logger;

/**
 * The throwaway server of the tests (Minecraft 1.21.11; the 26.x targets have their own copy with their constructor and abstract methods).
 * A flat world on a temp directory, no network, no ticking pace: tests run their code on its thread, and it ticks as fast as it can between
 * tasks. Vanilla's own {@code GameTestServer} is the model. Its levels are not loaded at start: {@code TestServer.ensureOverworld} does that.
 */
final class TestMinecraftServer extends MinecraftServer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Services NO_SERVICES = new Services(null, ServicesKeySet.EMPTY, null, new NoNames(), new NoProfiles());
    private static final WorldOptions WORLD_OPTIONS = new WorldOptions(0L, false, false);

    private final LocalSampleLogger sampleLogger = new LocalSampleLogger(1);
    private final Path tempDir;

    static MinecraftServer create(Thread thread, Path tempDir, LevelStorageSource.LevelStorageAccess access, PackRepository packs) {
        packs.reload();
        // Every pack there is: vanilla's, and the mods' (Kegare's advancements, Trinkets' slot data).
        List<String> enabled = new ArrayList<>(packs.getAvailableIds());
        enabled.remove("vanilla");
        enabled.addFirst("vanilla");
        WorldDataConfiguration dataConfig = new WorldDataConfiguration(new DataPackConfig(enabled, List.of()), FeatureFlags.REGISTRY.allFlags());
        // No mobs spawning and no weather turning over under a test, as before.
        GameRules rules = new GameRules(dataConfig.enabledFeatures());
        rules.set(GameRules.SPAWN_MOBS, false, null);
        rules.set(GameRules.ADVANCE_WEATHER, false, null);
        LevelSettings settings = new LevelSettings("Test Level", GameType.CREATIVE, false, Difficulty.NORMAL, true, rules, dataConfig);
        WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(packs, dataConfig, false, true);
        WorldLoader.InitConfig init = new WorldLoader.InitConfig(packConfig, Commands.CommandSelection.DEDICATED, LevelBasedPermissionSet.OWNER);
        try {
            LOGGER.debug("Starting resource loading");
            WorldStem stem = Util.blockUntilDone(executor -> load(init, settings, executor)).get();
            return new TestMinecraftServer(thread, access, packs, stem, tempDir);
        } catch (Exception e) {
            LOGGER.warn("Failed to load the datapacks for the test server", e);
            throw new IllegalStateException("couldn't build the test server", e);
        }
    }

    private static CompletableFuture<WorldStem> load(WorldLoader.InitConfig init, LevelSettings settings, Executor executor) {
        return WorldLoader.load(init, context -> {
            MappedRegistry<net.minecraft.world.level.dimension.LevelStem> stems = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable());
            stems.freeze();
            WorldPreset flat = context.datapackWorldgen().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value();
            var dimensions = flat.createWorldDimensions().bake(stems);
            return new WorldLoader.DataLoadOutput<>(
                    new PrimaryLevelData(settings, WORLD_OPTIONS, dimensions.specialWorldProperty(), dimensions.lifecycle()),
                    dimensions.dimensionsRegistryAccess());
        }, WorldStem::new, Util.backgroundExecutor(), executor);
    }

    private TestMinecraftServer(Thread thread, LevelStorageSource.LevelStorageAccess access, PackRepository packs, WorldStem stem, Path tempDir) {
        super(thread, access, packs, stem, Proxy.NO_PROXY, DataFixers.getDataFixer(), NO_SERVICES, new QuietLoadListener());
        this.tempDir = tempDir;
    }

    @Override
    public boolean initServer() {
        setPlayerList(new PlayerList(this, registries(), playerDataStorage, new EmptyNotificationService()) {});
        LOGGER.info("Started the ephemeral test server");
        return true;
    }

    @Override
    public void tickServer(BooleanSupplier haveTime) {
        super.tickServer(haveTime);
        EphemeralTestServerProvider.SERVER.set(this);
    }

    @Override
    public boolean saveEverything(boolean suppressLog, boolean flush, boolean force) {
        return false;
    }

    @Override
    public void stopServer() {
        LOGGER.info("Stopping the test server");
        try {
            getConnection().stop();
            getPlayerList().removeAll();
            storageSource.deleteLevel();
            storageSource.close();
            Files.delete(tempDir);
        } catch (Exception e) {
            LOGGER.error("Failed to clean up the test world {}", storageSource.getLevelId(), e);
        }
    }

    @Override
    public void waitUntilNextTick() {
        runAllTasks();
    }

    @Override
    public SystemReport fillServerSystemReport(SystemReport report) {
        report.setDetail("Type", "Test ephemeral server");
        return report;
    }

    /**
     * Fabric API's resource loader mixes {@code DataResourceStore} into {@code MinecraftServer} and implements it there, but javac only sees the
     * abstract interface method; this is the same one-line delegation to what the mixin gave the class.
     */
    @Override
    public <T> T getOrThrow(DataResourceStore.Key<T> key) {
        return ((FabricDataResourceStoreHolder) (Object) this).fabric$getDataResourceStore().getOrThrow(key);
    }

    @Override
    public boolean isHardcore() {
        return false;
    }

    @Override
    public int getMaxPlayers() {
        return 1;
    }

    @Override
    public LevelBasedPermissionSet operatorUserPermissions() {
        return LevelBasedPermissionSet.OWNER;
    }

    @Override
    public PermissionSet getFunctionCompilationPermissions() {
        return LevelBasedPermissionSet.OWNER;
    }

    @Override
    public boolean shouldRconBroadcast() {
        return false;
    }

    @Override
    public boolean isDedicatedServer() {
        return false;
    }

    @Override
    public int getRateLimitPacketsPerSecond() {
        return 0;
    }

    @Override
    public boolean useNativeTransport() {
        return false;
    }

    @Override
    public boolean isPublished() {
        return false;
    }

    @Override
    public boolean shouldInformAdmins() {
        return false;
    }

    @Override
    public boolean isSingleplayerOwner(NameAndId player) {
        return false;
    }

    @Override
    protected SampleLogger getTickTimeLogger() {
        return sampleLogger;
    }

    @Override
    public boolean isTickTimeLoggingEnabled() {
        return false;
    }

    /** Nobody to show progress to. */
    private static final class QuietLoadListener implements LevelLoadListener {
        @Override
        public void start(Stage stage, int total) {}

        @Override
        public void update(Stage stage, int done, int total) {}

        @Override
        public void finish(Stage stage) {}

        @Override
        public void updateFocus(ResourceKey<Level> dimension, ChunkPos pos) {}
    }

    private static final class NoNames implements UserNameToIdResolver {
        @Override
        public void add(NameAndId id) {}

        @Override
        public Optional<NameAndId> get(String name) {
            return Optional.empty();
        }

        @Override
        public Optional<NameAndId> get(UUID id) {
            return Optional.empty();
        }

        @Override
        public void resolveOfflineUsers(boolean offline) {}

        @Override
        public void save() {}
    }

    private static final class NoProfiles implements ProfileResolver {
        @Override
        public Optional<GameProfile> fetchByName(String name) {
            return Optional.empty();
        }

        @Override
        public Optional<GameProfile> fetchById(UUID id) {
            return Optional.empty();
        }
    }
}
