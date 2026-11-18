package com.cnethuka.kegare.test;

import com.cnethuka.kegare.Kegare;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
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
        loadModTranslations();
        done = true;
    }

    /**
     * Vanilla's {@code Language} on a server has the game's own en_us only, and a client takes the mods' from its resource packs. NeoForge also
     * put the mods' en_us.json into the server-side one, which is what tests that read a command's reply ({@code Component.getString()}) count
     * on: "Set shorty's Innate Technique to Rime", not {@code commands.kegare.innate.set}. Kegare's file (generated, on the classpath) goes on top.
     */
    private static void loadModTranslations() {
        Language base = Language.getInstance();
        Map<String, String> mine = new HashMap<>();
        try (InputStream in = TestBootstrap.class.getResourceAsStream("/assets/" + Kegare.MODID + "/lang/en_us.json")) {
            if (in == null) throw new IllegalStateException("no en_us.json for Kegare on the classpath");
            Language.loadFromJson(in, mine::put);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Language.inject(new Language() {
            @Override
            public String getOrDefault(String key, String fallback) {
                String own = mine.get(key);
                return own != null ? own : base.getOrDefault(key, fallback);
            }

            @Override
            public boolean has(String key) {
                return mine.containsKey(key) || base.has(key);
            }

            @Override
            public boolean isDefaultRightToLeft() {
                return base.isDefaultRightToLeft();
            }

            @Override
            public FormattedCharSequence getVisualOrder(FormattedText text) {
                return base.getVisualOrder(text);
            }
        });
    }
}
