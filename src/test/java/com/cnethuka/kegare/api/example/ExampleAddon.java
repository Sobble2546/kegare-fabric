package com.cnethuka.kegare.api.example;

import com.cnethuka.kegare.api.KegareApi;
import com.cnethuka.kegare.api.ability.Ability;
import com.cnethuka.kegare.api.ability.AbilityCategory;
import com.cnethuka.kegare.api.ability.AbilityContext;
import com.cnethuka.kegare.api.ability.BaseAbility;
import com.cnethuka.kegare.api.ability.CastType;
import com.cnethuka.kegare.api.ability.Requirement;
import com.cnethuka.kegare.api.ability.SpecialAbility;
import com.cnethuka.kegare.api.technique.InnateTechnique;
import com.cnethuka.kegare.api.technique.InnateTechniqueRarity;
import com.cnethuka.kegare.api.trait.Trait;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * A small addon, written only against {@code com.cnethuka.kegare.api}, that the tests load beside Kegare: an innate technique with an
 * instant ability, a toggle and its special, a shared ability and a trait. It never ships: it's in the test sources only.
 * <p>
 * A real addon does the same from its own mod constructor, on its own mod bus. Here there's no second mod, so it hooks Kegare's
 * construction instead and registers under its own namespace, as an addon's would be.
 */
@EventBusSubscriber(modid = KegareApi.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ExampleAddon {
    public static final String NAMESPACE = "kegare_example";

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
    }

    public static final double JOLT_COST = 30;
    public static final int JOLT_COOLDOWN = 40;
    public static final float JOLT_DAMAGE = 3f;
    public static final double HUM_COST = 20;
    public static final double HUM_UPKEEP = 4;
    public static final double HUM_BURST_COST = 15;

    /** Hurts the nearest living thing within 5 blocks. It runs inside the cast, so the hit counts for Refinement by itself. */
    public static final Ability JOLT = new BaseAbility(id("jolt"), CastType.INSTANT) {
        @Override public double cost(ServerPlayer player) { return JOLT_COST; }
        @Override public int cooldownTicks(ServerPlayer player) { return JOLT_COOLDOWN; }

        @Override
        public void execute(AbilityContext ctx) {
            ServerPlayer player = ctx.player();
            ctx.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5), e -> e != player && e.isAlive()).stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                    .ifPresent(target -> target.hurt(player.damageSources().indirectMagic(player, player), JOLT_DAMAGE));
        }
    };

    /** A toggle with upkeep; its special only works while it's on. */
    public static final Ability HUM = new BaseAbility(id("hum"), CastType.TOGGLE) {
        @Override public double cost(ServerPlayer player) { return HUM_COST; }
        @Override public int cooldownTicks(ServerPlayer player) { return 20; }
        @Override public double upkeepPerSecond(ServerPlayer player) { return HUM_UPKEEP; }
        @Override public void execute(AbilityContext ctx) {}
        @Override public Optional<Ability> special() { return Optional.of(HUM_BURST); }
    };

    public static final SpecialAbility HUM_BURST = new SpecialAbility(id("hum_burst"), CastType.INSTANT, () -> HUM, Requirement.toggleActive(HUM)) {
        @Override public double cost(ServerPlayer player) { return HUM_BURST_COST; }
        @Override public void execute(AbilityContext ctx) {}
    };

    /** A shared ability under the Malice tab: unlocked once, kept through rerolls. */
    public static final Ability STEADY = new BaseAbility(id("steady"), CastType.INSTANT) {
        @Override public double cost(ServerPlayer player) { return 10; }
        @Override public int cooldownTicks(ServerPlayer player) { return 20; }
        @Override public AbilityCategory category() { return AbilityCategory.MALICE; }
        @Override public void execute(AbilityContext ctx) {}
    };

    private static final DeferredRegister<InnateTechnique> TECHNIQUES = DeferredRegister.create(KegareApi.INNATE_TECHNIQUES, NAMESPACE);
    private static final DeferredRegister<Trait> TRAITS = DeferredRegister.create(KegareApi.TRAITS, NAMESPACE);

    /** Rolled like any other (the default), sharing the Rare weight with Rime. */
    public static final DeferredHolder<InnateTechnique, InnateTechnique> TECHNIQUE = TECHNIQUES.register("example",
            () -> InnateTechnique.builder(InnateTechniqueRarity.RARE)
                    .ability(JOLT)
                    .ability(HUM)
                    .defaultUnlockLevels()
                    .lockedUltimate()
                    .maliceColor(0x55CC88)
                    .tabIcon(Items.LIGHTNING_ROD)
                    .build());

    /** Never rolled at birth (no chance set), so it changes nothing for the rest of the tests. */
    public static final DeferredHolder<Trait, Trait> KEEN = TRAITS.register("keen", () -> new Trait(new Trait.Properties()));

    private ExampleAddon() {}

    @SubscribeEvent
    static void construct(FMLConstructModEvent event) {
        // A real addon: its constructor's own IEventBus.
        IEventBus modBus = ModList.get().getModContainerById(KegareApi.MOD_ID).orElseThrow().getEventBus();
        TECHNIQUES.register(modBus);
        TRAITS.register(modBus);
        KegareApi.registerSharedAbility(STEADY);
    }
}
