package com.cnethuka.kegare.api.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cnethuka.kegare.TestServer;
import com.cnethuka.kegare.api.KegareApi;
import com.cnethuka.kegare.api.ability.AbilityCategory;
import com.cnethuka.kegare.api.technique.InnateTechnique;
import com.cnethuka.kegare.api.trait.Trait;
import com.cnethuka.kegare.config.KegareServerConfig;
import com.cnethuka.kegare.innate_technique.swap.Swap;
import com.cnethuka.kegare.innatetechnique.CastIntent;
import com.cnethuka.kegare.innatetechnique.CastManager;
import com.cnethuka.kegare.innatetechnique.InnateTechniqueService;
import com.cnethuka.kegare.loadout.AbilityTabs;
import com.cnethuka.kegare.malice.MaliceService;
import com.cnethuka.kegare.player.BirthService;
import com.cnethuka.kegare.progression.RefinementService;
import com.cnethuka.kegare.roll.RollReason;
import com.cnethuka.kegare.roll.RollService;
import com.cnethuka.kegare.trait.TraitService;
import java.util.OptionalInt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The addon API (DESIGN §3.34) from an addon's side: {@link ExampleAddon}, written against {@code com.cnethuka.kegare.api} alone, registers
 * a technique, a shared ability and a trait; a player casts its abilities like Kegare's own; Refinement counts them; rolls land on it
 * unless the server turns addon techniques off.
 */
@ExtendWith(EphemeralTestServerProvider.class)
class ExampleAddonTest {
    private static final double EPS = 1.0e-6;
    private static final double Y = 120; // in the open air above the flat world

    private static ServerPlayer holder(MinecraftServer server, ServerLevel level, String name) {
        ServerPlayer p = TestServer.damageable(server, name);
        level.addNewPlayer(p);
        BirthService.rebirth(p, OptionalInt.of(2000));
        TraitService.removeAll(p, Trait.RemoveReason.ADMIN);
        InnateTechniqueService.setInnateTechnique(p, ExampleAddon.TECHNIQUE.get());
        MaliceService.refill(p);
        p.moveTo(0.5, Y, 0.5, 0f, 0f);
        return p;
    }

    private static double malice(ServerPlayer p) {
        return MaliceService.get(p).current();
    }

    @Test
    void itsTechniqueSharedAbilityAndTraitAreRegistered(MinecraftServer server) {
        InnateTechnique technique = ExampleAddon.TECHNIQUE.get();
        assertEquals(ResourceLocation.fromNamespaceAndPath(ExampleAddon.NAMESPACE, "example"), technique.id());
        assertSame(technique, KegareApi.innateTechniques().get(technique.id()), "in Kegare's registry");
        assertFalse(KegareApi.isKegares(technique), "an addon's");
        assertTrue(KegareApi.isKegares(Swap.SWAP.get()));
        assertEquals(java.util.List.of(ExampleAddon.JOLT, ExampleAddon.HUM), technique.abilities());
        assertTrue(technique.rollable(), "rolled by default");
        assertSame(ExampleAddon.KEEN.get(), KegareApi.traits().get(ExampleAddon.KEEN.getId()));
        assertTrue(AbilityTabs.abilities(AbilityCategory.MALICE, null).contains(ExampleAddon.STEADY), "its shared ability is under the Malice tab");
    }

    @Test
    void itsAbilitiesUnlockOnTheDefaultTable(MinecraftServer server) {
        InnateTechnique technique = ExampleAddon.TECHNIQUE.get();
        assertEquals(0, technique.unlockLevel(ExampleAddon.JOLT));
        assertEquals(0, technique.unlockLevel(ExampleAddon.HUM));
        assertEquals(3, technique.unlockLevel(ExampleAddon.HUM_BURST), "the second ability's special's level, as on Kegare's techniques");
    }

    @Test
    void aPlayerCastsItLikeKegaresOwnAndRefinementCountsIt(MinecraftServer server) throws Exception {
        TestServer.onServerThread(server, () -> {
            TestServer.ensureOverworld(server);
            ServerLevel level = server.overworld();
            Pig pig = EntityType.PIG.create(level);
            try {
                ServerPlayer p = holder(server, level, "addon_caster");
                pig.moveTo(2.5, Y, 0.5, 0f, 0f);
                pig.setNoGravity(true);
                pig.setNoAi(true);
                level.addFreshEntity(pig);
                float health = pig.getHealth();
                double xp = RefinementService.get(p).xp();
                int refinement = RefinementService.level(p);

                // The instant ability: paid, the cooldown started, the pig hurt, and the hit counted for Refinement with no code of its own.
                InnateTechniqueService.select(p, 0);
                double before = malice(p);
                CastManager.handleIntent(p, CastIntent.CAST_PRESS);
                assertEquals(before - ExampleAddon.JOLT_COST, malice(p), EPS, "paid");
                assertEquals(ExampleAddon.JOLT_COOLDOWN, InnateTechniqueService.cooldownRemaining(p, ExampleAddon.JOLT), "on cooldown");
                assertEquals(health - ExampleAddon.JOLT_DAMAGE, pig.getHealth(), EPS, "hurt");
                assertTrue(RefinementService.level(p) > refinement || RefinementService.get(p).xp() > xp, "Refinement counted the landing");

                // The special needs its toggle on: without it, it fizzles for nothing.
                InnateTechniqueService.select(p, 1);
                before = malice(p);
                CastManager.handleIntent(p, CastIntent.SPECIAL_PRESS);
                assertEquals(before, malice(p), EPS, "the special's requirement is unmet: nothing paid");
                CastManager.handleIntent(p, CastIntent.CAST_PRESS);
                assertTrue(CastManager.isToggleActive(p, ExampleAddon.HUM), "the toggle is on");
                assertEquals(before - ExampleAddon.HUM_COST, malice(p), EPS, "the toggle's cost");
                before = malice(p);
                CastManager.handleIntent(p, CastIntent.SPECIAL_PRESS);
                assertEquals(before - ExampleAddon.HUM_BURST_COST, malice(p), EPS, "now the special goes off");
                assertTrue(InnateTechniqueService.cooldownRemaining(p, ExampleAddon.HUM) > 0, "and starts the cooldown it shares");
            } finally {
                pig.discard();
                TestServer.removePlayers(server);
            }
        });
    }

    @Test
    void theSharedAbilityUnlocksThroughTheApi(MinecraftServer server) throws Exception {
        TestServer.onServerThread(server, () -> {
            TestServer.ensureOverworld(server);
            try {
                ServerPlayer p = holder(server, server.overworld(), "addon_shared");
                assertFalse(KegareApi.hasSharedAbility(p, ExampleAddon.STEADY));
                assertTrue(KegareApi.unlockSharedAbility(p, ExampleAddon.STEADY));
                assertTrue(KegareApi.hasSharedAbility(p, ExampleAddon.STEADY));
                assertFalse(KegareApi.unlockSharedAbility(p, ExampleAddon.STEADY), "only once");
            } finally {
                TestServer.removePlayers(server);
            }
        });
    }

    @Test
    void whoIsInTheRollPool() {
        ResourceLocation kegares = ResourceLocation.fromNamespaceAndPath("kegare", "rime");
        ResourceLocation addons = ResourceLocation.fromNamespaceAndPath("kegare_example", "example");
        assertTrue(RollService.inPool(kegares, true, false), "Kegare's own always roll");
        assertTrue(RollService.inPool(addons, true, true), "an addon's, by default");
        assertFalse(RollService.inPool(addons, true, false), "not when the server turns addon techniques off");
        assertFalse(RollService.inPool(addons, false, true), "never one that says it's never rolled");
        assertFalse(RollService.inPool(kegares, false, true));
    }

    @Test
    void rollsLandOnItUnlessTheServerTurnsAddonTechniquesOff(MinecraftServer server) throws Exception {
        TestServer.onServerThread(server, () -> {
            TestServer.ensureOverworld(server);
            boolean was = KegareServerConfig.ROLL_ADDON_TECHNIQUES.get();
            try {
                ServerPlayer p = TestServer.player(server, "addon_rolls");
                BirthService.rebirth(p, OptionalInt.of(1000));
                assertTrue(landsOnIt(p), "rolled like any technique by default");
                KegareServerConfig.ROLL_ADDON_TECHNIQUES.set(false);
                assertFalse(landsOnIt(p), "never with addon techniques off");
            } finally {
                KegareServerConfig.ROLL_ADDON_TECHNIQUES.set(was);
                TestServer.removePlayers(server);
            }
        });
    }

    /** Whether 400 rolls (each from Swap, with no pity) ever land on the example technique: with Swap held, about 30% a roll. */
    private static boolean landsOnIt(ServerPlayer p) {
        for (int i = 0; i < 400; i++) {
            InnateTechniqueService.setInnateTechnique(p, Swap.SWAP.get());
            RollService.setPity(p, 0);
            InnateTechnique rolled = RollService.rollInnateTechnique(p, RollReason.ADMIN).orElseThrow().innateTechnique();
            if (rolled == ExampleAddon.TECHNIQUE.get()) return true;
        }
        return false;
    }
}
