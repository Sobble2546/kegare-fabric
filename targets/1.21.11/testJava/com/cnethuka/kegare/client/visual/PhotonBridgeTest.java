package com.cnethuka.kegare.client.visual;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cnethuka.kegare.innatetechnique.visual.Visual;
import com.cnethuka.photonfx.data.FxData;
import com.cnethuka.photonfx.data.FxReader;
import com.cnethuka.photonfx.data.FxResources;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/**
 * Kegare's side of the particle engine (DESIGN §3.13): the bridge to Photon FX, and that the engine this build nests can read
 * every effect Kegare ships. This test lives in the 1.21.11 overlay because only that target has the engine; the 26.x builds
 * have neither the engine nor this test.
 */
class PhotonBridgeTest {
    private static final Path PROJECT = Path.of(System.getProperty("kegare.projectDir", "."));
    private static final Path ASSETS = PROJECT.resolve("src/main/resources/assets/kegare");

    @Test
    void theEngineIsOnTheGameAsAMod() {
        assertTrue(FabricLoader.getInstance().isModLoaded("photonfx"), "photonfx is found as a mod (VisualClientEvents installs the bridge by this)");
        assertTrue(FabricLoader.getInstance().isModLoaded("ldlib2lite"), "ldlib2lite, which photonfx depends on, is found as a mod");
    }

    /**
     * A regenerated .fx that uses something the port does not read (a key outside its table, a new emitter type, another
     * version) comes back with problems, and fails here instead of silently looking wrong in the game.
     */
    @Test
    void everyEffectFileIsReadWithoutProblems() throws IOException {
        Path fxDir = ASSETS.resolve("fx");
        List<Path> files;
        try (Stream<Path> walk = Files.walk(fxDir)) {
            files = walk.filter(p -> p.toString().endsWith(".fx")).sorted().toList();
        }
        assertFalse(files.isEmpty(), "no .fx files below " + fxDir);
        List<String> wrong = new ArrayList<>();
        for (Path file : files) {
            String name = fxDir.relativize(file).toString().replace('\\', '/').replaceAll("\\.fx$", "");
            CompoundTag tag;
            try (InputStream in = Files.newInputStream(file)) {
                tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            }
            FxData fx = FxReader.load(Identifier.fromNamespaceAndPath("kegare", name), tag).fx();
            if (fx.emitters().isEmpty()) wrong.add(name + ": no emitters");
            for (String problem : fx.problems()) wrong.add(name + ": " + problem);
            // what it draws with exists: Photon FX's own textures in its jar, Kegare's in Kegare's assets
            for (Identifier authored : FxResources.referenced(fx)) {
                Identifier id = FxResources.resolve(authored);
                boolean found = switch (id.getNamespace()) {
                    case "kegare" -> Files.isRegularFile(ASSETS.resolve(id.getPath()));
                    case "photonfx" -> FxReader.class.getClassLoader().getResource("assets/photonfx/" + id.getPath()) != null;
                    default -> false;
                };
                if (!found) wrong.add(name + ": names " + authored + ", which neither Kegare nor Photon FX ships");
            }
        }
        assertEquals(List.of(), wrong, files.size() + " effect files were read");
    }

    @Test
    void anchorsKeepTheirMeaning() {
        for (Visual.Anchor anchor : Visual.Anchor.values()) {
            assertEquals(anchor.name(), PhotonBridge.anchor(anchor).name());
        }
    }

    /** Here there is no client, so the engine's first call fails: the bridge must swallow it, report "can't play" and stay off. */
    @Test
    void aFailingEngineSwitchesTheBridgeOffInsteadOfThrowing() {
        PhotonBridge bridge = new PhotonBridge();
        assertFalse(bridge.disabled());
        Identifier effect = Identifier.fromNamespaceAndPath("kegare", "rmt/rmt_orb");
        assertNull(assertDoesNotThrow(() -> bridge.at(effect, null, Vec3.ZERO, false)));
        assertTrue(bridge.disabled(), "the failed call switched the bridge off");
        assertNull(assertDoesNotThrow(() -> bridge.movableAt(effect, null, Vec3.ZERO, false)));
        assertNull(assertDoesNotThrow(() -> bridge.onEntity(effect, null, Visual.Anchor.BODY, false, true)));
        assertDoesNotThrow(() -> bridge.preload(List.of(effect)));
        PhotonEffects.Playing foreign = new PhotonEffects.Playing() {};
        assertFalse(bridge.isPlaying(foreign));
        assertDoesNotThrow(() -> {
            bridge.stop(foreign, true);
            bridge.scale(foreign, 2f);
            bridge.tint(foreign, 0xFFFF0000);
            bridge.persistLifetime(foreign, 20);
        });
    }

    /** A handle that is not the bridge's own is ignored, also while the bridge works. */
    @Test
    void aForeignHandleIsIgnored() {
        PhotonBridge bridge = new PhotonBridge();
        PhotonEffects.Playing foreign = new PhotonEffects.Playing() {};
        assertFalse(bridge.isPlaying(foreign));
        assertDoesNotThrow(() -> bridge.stop(foreign, false));
        assertFalse(bridge.disabled());
    }
}
