package com.cnethuka.kegare.test;

import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;

/**
 * Hands a test a running {@link MinecraftServer}: one throwaway server for the whole JVM, on a temp world, started on first use (what NeoForge's
 * testframework provided under the same name). The server class itself is per Minecraft target ({@code TestMinecraftServer}, in
 * targets/&lt;target&gt;/testJava).
 */
public class EphemeralTestServerProvider implements ParameterResolver {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Set by the server on its first tick: from then on it is up. */
    public static final AtomicReference<MinecraftServer> SERVER = new AtomicReference<>();
    public static final AtomicBoolean IN_CONSTRUCTION = new AtomicBoolean();
    /** Why the server never came up, kept so every later test fails at once instead of waiting (a hung suite holds the build lock). */
    private static volatile Throwable startFailure;

    private static final long START_TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(5);

    @Override
    public boolean supportsParameter(ParameterContext parameter, ExtensionContext extension) throws ParameterResolutionException {
        return parameter.getParameter().getType() == MinecraftServer.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameter, ExtensionContext extension) throws ParameterResolutionException {
        return grabServer();
    }

    public static MinecraftServer grabServer() {
        if (startFailure != null) throw new IllegalStateException("the test server failed to start earlier", startFailure);
        TestBootstrap.ensure();
        if (IN_CONSTRUCTION.compareAndSet(false, true)) {
            try {
                Path tempDir = Files.createTempDirectory("test-mc-server-");
                LevelStorageSource source = LevelStorageSource.createDefault(tempDir.resolve("world"));
                LevelStorageSource.LevelStorageAccess access = source.validateAndCreateAccess("main");
                PackRepository packs = ServerPacksSource.createPackRepository(access);
                MinecraftServer server = MinecraftServer.spin(thread -> TestMinecraftServer.create(thread, tempDir, access, packs));
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    server.stopServer();
                    org.apache.logging.log4j.LogManager.shutdown();
                }));
            } catch (Throwable e) {
                LOGGER.error(LogUtils.FATAL_MARKER, "Failed to start the test server", e);
                startFailure = e;
                throw new RuntimeException(e);
            }
        }
        // The server thread publishes itself after its first tick.
        long deadline = System.nanoTime() + START_TIMEOUT_NANOS;
        while (SERVER.get() == null) {
            if (startFailure != null) throw new IllegalStateException("the test server failed to start", startFailure);
            if (System.nanoTime() > deadline) {
                startFailure = new IllegalStateException("the test server didn't tick within 5 minutes");
                throw new IllegalStateException(startFailure);
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return SERVER.get();
    }
}
