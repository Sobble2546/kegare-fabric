package com.cnethuka.kegare.innatetechnique.change;

import com.cnethuka.kegare.Kegare;
import com.cnethuka.kegare.advancement.AdvancementAbilities;
import com.cnethuka.kegare.api.ability.Ability;
import com.cnethuka.kegare.api.change.AbilityTweak;
import com.cnethuka.kegare.api.technique.InnateTechnique;
import com.cnethuka.kegare.progression.Attribution;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Every innate technique as players get it, addons' changes applied (DESIGN §3.34). They're applied in this one place, once the
 * technique registry is complete ({@link #bake}, on both sides, again after a client syncs with a server); before that a technique
 * is as its definition says. {@link InnateTechnique#abilities()} and the rest read from here.
 */
public final class TechniqueViews {
    /**
     * A technique with the changes applied: its abilities and ultimate as players get them, the unlock levels the additions brought,
     * and new specials that unlock where the specials they took over did.
     */
    public record View(List<Ability> abilities, @Nullable Ability ultimate, Map<ResourceLocation, Integer> addedLevels,
                       Map<ResourceLocation, ResourceLocation> sameLevelAs, List<Ability> every) {
        public View(List<Ability> abilities, @Nullable Ability ultimate, Map<ResourceLocation, Integer> addedLevels,
                    Map<ResourceLocation, ResourceLocation> sameLevelAs) {
            this(List.copyOf(abilities), ultimate, Map.copyOf(addedLevels), Map.copyOf(sameLevelAs), every(abilities, ultimate));
        }

        /**
         * The Refinement level that unlocks {@code id}: where an addition said, or for a new special a replacement brought, where the
         * special it took over unlocked; otherwise what the definition gives ({@code defined}).
         */
        public int unlockLevel(ResourceLocation id, java.util.function.ToIntFunction<ResourceLocation> defined) {
            Integer added = addedLevels.get(id);
            if (added != null) return added;
            ResourceLocation same = sameLevelAs.getOrDefault(id, id);
            added = addedLevels.get(same);
            return added != null ? added : defined.applyAsInt(same);
        }

        /** As the definition says, with no changes. */
        static View of(InnateTechnique technique) {
            return new View(technique.definedAbilities(), technique.definedUltimate().orElse(null), Map.of(), Map.of());
        }

        private static List<Ability> every(List<Ability> abilities, @Nullable Ability ultimate) {
            List<Ability> out = new ArrayList<>();
            for (Ability a : abilities) {
                out.add(a);
                a.special().ifPresent(out::add);
            }
            if (ultimate != null) {
                out.add(ultimate);
                ultimate.special().ifPresent(out::add);
            }
            return List.copyOf(out);
        }
    }

    private static volatile Map<InnateTechnique, View> views = Map.of();

    private TechniqueViews() {}

    public static View view(InnateTechnique technique) {
        View view = views.get(technique);
        return view != null ? view : View.of(technique);
    }

    /** The registry is complete: seal the changes and apply them to every technique. Throws (stopping the game) if one can't apply. */
    public static void bake(Registry<InnateTechnique> registry) {
        ChangeSet changes = ChangeSet.GLOBAL;
        changes.seal();
        Map<InnateTechnique, View> out = new IdentityHashMap<>();
        Set<ResourceLocation> abilities = new HashSet<>();
        for (Map.Entry<net.minecraft.resources.ResourceKey<InnateTechnique>, InnateTechnique> entry : registry.entrySet()) {
            InnateTechnique technique = entry.getValue();
            View view = changes.view(technique, entry.getKey().location());
            out.put(technique, view);
            for (Ability a : View.of(technique).every()) abilities.add(a.id());
            for (Ability a : view.every()) abilities.add(a.id());
        }
        changes.checkAllFound(registry.keySet(), abilities);
        views = out;
        for (ResourceLocation id : changes.changed()) {
            Kegare.LOGGER.info("Innate technique ability {} was {}", id, String.join(" and ", changes.describe(id)));
        }
        for (ResourceLocation advancement : AdvancementAbilities.turnedOff(changes.changed())) {
            Kegare.LOGGER.info("Advancement {} is turned off: an ability it depends on was changed by an addon", advancement);
        }
    }

    /** A tweak's {@code damage}: on any hit Kegare traces back to that ability (DESIGN §3.21's attribution), before defences. */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        ChangeSet changes = ChangeSet.GLOBAL;
        if (!changes.anyTweaks()) return;
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level)) return;
        Attribution.Hit hit = Attribution.of(event.getSource(), level.getServer());
        if (hit == null || hit.ability() == null) return;
        List<AbilityTweak> tweaks = changes.tweaksOf(hit.ability().id());
        if (tweaks.isEmpty()) return;
        float amount = event.getAmount();
        for (AbilityTweak tweak : tweaks) amount = tweak.damage(hit.attacker(), target, amount);
        event.setAmount(Math.max(0f, amount));
    }
}
