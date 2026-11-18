package com.cnethuka.kegare.test;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;

/**
 * Brings Minecraft and the mods up once per test JVM: what a game launch does before anything else. fabric-loader-junit only starts Knot (class
 * loading and mixins); it neither bootstraps the game nor runs any mod's entrypoints.
 * <p>Order, as in a launch (checked in the bytecode): vanilla's {@code Bootstrap} first (registry creation throws before it), which under
 * Fabric API leaves the registries open (its BootstrapMixin turns the freeze in it into {@code createContents}); then the mods' {@code main}
 * entrypoints (Kegare registers its registries, items and sounds right away); then {@code BuiltInRegistries.bootStrap()}, which only freezes
 * and validates the second time round, as fabric-registry-sync's MainMixin does at the end of a launch. Every test class reaches this through
 * {@link KegareTestSetup} (a JUnit extension that runs for all of them) or through {@link EphemeralTestServerProvider}.
 */
public final class TestBootstrap {
    private TestBootstrap() {}

    private static boolean done;

    public static synchronized void ensure() {
        if (done) return;
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        FabricLoader.getInstance().invokeEntrypoints("main", ModInitializer.class, ModInitializer::onInitialize);
        BuiltInRegistries.bootStrap();
        done = true;
    }
}
