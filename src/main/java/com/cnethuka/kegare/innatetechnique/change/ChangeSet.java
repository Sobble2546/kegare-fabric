package com.cnethuka.kegare.innatetechnique.change;

import com.cnethuka.kegare.api.ability.Ability;
import com.cnethuka.kegare.api.change.AbilityTweak;
import com.cnethuka.kegare.api.technique.InnateTechnique;
import com.cnethuka.kegare.progression.UnlockTable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

/**
 * Addons' changes to innate techniques (DESIGN §3.34): additions, replacements and tweaks as they were registered, checked, and what
 * a technique looks like with them ({@link #view}). {@link #GLOBAL} is the game's; tests make their own.
 */
public final class ChangeSet {
    /** The changes the game applies (through {@link com.cnethuka.kegare.api.change.TechniqueChanges}). */
    public static final ChangeSet GLOBAL = new ChangeSet();

    record Addition(ResourceLocation technique, @Nullable ResourceLocation after, Ability ability, int level, @Nullable Integer specialLevel,
                    String by) {}

    record Replacement(Ability ability, String by) {}

    record Tweak(AbilityTweak tweak, String by) {}

    private final List<Addition> additions = new ArrayList<>();
    private final Map<ResourceLocation, Replacement> replacements = new LinkedHashMap<>();
    private final Map<ResourceLocation, List<Tweak>> tweaks = new LinkedHashMap<>();
    private boolean sealed;

    private void open() {
        if (sealed) {
            throw new IllegalStateException("Too late to change innate techniques: the registry is complete. Register changes from your mod's constructor.");
        }
    }

    public synchronized void add(ResourceLocation technique, @Nullable ResourceLocation after, Ability ability, int level,
                                 @Nullable Integer specialLevel, String by) {
        open();
        Objects.requireNonNull(technique, "technique");
        Objects.requireNonNull(ability, "ability");
        if (ability.isSpecial()) throw new IllegalArgumentException(by + " adds " + ability.id() + ", a special: add its base ability, which returns it from special()");
        if (level < 0 || specialLevel != null && specialLevel < 0) throw new IllegalArgumentException(by + " adds " + ability.id() + " at a negative unlock level");
        Optional<Ability> special = ability.special();
        if (special.isPresent() && specialLevel == null) {
            throw new IllegalArgumentException(by + " adds " + ability.id() + " without an unlock level for its special " + special.get().id());
        }
        if (special.isEmpty() && specialLevel != null) {
            throw new IllegalArgumentException(by + " adds " + ability.id() + " with a special unlock level, but it has no special");
        }
        if (special.isPresent() && (!special.get().isSpecial() || !special.get().base().map(Ability::id).equals(Optional.of(ability.id())))) {
            throw new IllegalArgumentException(by + " adds " + ability.id() + ", whose special " + special.get().id() + " doesn't say it's that ability's special");
        }
        for (Addition other : additions) {
            Set<ResourceLocation> theirs = idsOf(other.ability());
            for (ResourceLocation id : idsOf(ability)) {
                if (theirs.contains(id)) throw new IllegalStateException(by + " and " + other.by() + " both add an ability with the ID " + id);
            }
        }
        additions.add(new Addition(technique, after, ability, level, specialLevel, by));
    }

    public synchronized void replace(Ability replacement, String by) {
        open();
        Objects.requireNonNull(replacement, "replacement");
        Replacement before = replacements.get(replacement.id());
        if (before != null) throw new IllegalStateException(by + " and " + before.by() + " both replace " + replacement.id() + ": only one addon can");
        replacements.put(replacement.id(), new Replacement(replacement, by));
    }

    public synchronized void tweak(ResourceLocation ability, AbilityTweak tweak, String by) {
        open();
        Objects.requireNonNull(ability, "ability");
        Objects.requireNonNull(tweak, "tweak");
        tweaks.computeIfAbsent(ability, id -> new ArrayList<>()).add(new Tweak(tweak, by));
    }

    /** No more changes: the registry is complete. */
    public synchronized void seal() {
        sealed = true;
    }

    public synchronized boolean isChanged(ResourceLocation ability) {
        return replacements.containsKey(ability) || tweaks.containsKey(ability);
    }

    /** Every ability ID that was replaced or tweaked. */
    public synchronized Set<ResourceLocation> changed() {
        Set<ResourceLocation> out = new LinkedHashSet<>(replacements.keySet());
        out.addAll(tweaks.keySet());
        return Set.copyOf(out);
    }

    /** Who changed this ability and how, for the log and the join check: "replaced by x", "tweaked by y, z". */
    public synchronized List<String> describe(ResourceLocation ability) {
        List<String> out = new ArrayList<>();
        Replacement r = replacements.get(ability);
        if (r != null) out.add("replaced by " + r.by());
        List<Tweak> t = tweaks.get(ability);
        if (t != null) out.add("tweaked by " + String.join(", ", t.stream().map(Tweak::by).toList()));
        return out;
    }

    synchronized List<AbilityTweak> tweaksOf(ResourceLocation ability) {
        List<Tweak> list = tweaks.get(ability);
        return list == null ? List.of() : list.stream().map(Tweak::tweak).toList();
    }

    synchronized boolean anyTweaks() {
        return !tweaks.isEmpty();
    }

    /**
     * What a technique registered as {@code id} looks like with these changes. Throws when one can't apply: an addition's anchor
     * isn't there, or a replacement doesn't fit the slot it replaces.
     */
    public synchronized TechniqueViews.View view(InnateTechnique technique, ResourceLocation id) {
        List<Ability> list = new ArrayList<>(technique.definedAbilities());
        Map<ResourceLocation, Integer> addedLevels = new HashMap<>();
        Map<ResourceLocation, ResourceLocation> lastAfter = new HashMap<>();
        for (Addition a : additions) {
            if (!a.technique().equals(id)) continue;
            for (ResourceLocation added : idsOf(a.ability())) {
                if (technique.definedAbilities().stream().anyMatch(b -> idsOf(b).contains(added))
                        || technique.definedUltimate().map(u -> idsOf(u).contains(added)).orElse(false)) {
                    throw new IllegalStateException(a.by() + " adds " + added + " to " + id + ", which already has an ability with that ID");
                }
            }
            if (a.after() == null) {
                list.add(a.ability());
            } else {
                ResourceLocation anchor = lastAfter.getOrDefault(a.after(), a.after());
                int at = indexOf(list, anchor);
                if (at < 0) throw new IllegalStateException(a.by() + " adds " + a.ability().id() + " after " + a.after() + ", which isn't one of " + id + "'s abilities");
                list.add(at + 1, a.ability());
                lastAfter.put(a.after(), a.ability().id());
            }
            addedLevels.put(a.ability().id(), a.level());
            a.ability().special().ifPresent(s -> addedLevels.put(s.id(), a.specialLevel()));
        }

        Map<ResourceLocation, ResourceLocation> sameLevelAs = new HashMap<>();
        List<Ability> abilities = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Ability original = list.get(i);
            Ability changed = effective(original, id);
            abilities.add(changed);
            ResourceLocation oldSpecial = original.special().map(Ability::id).orElse(null);
            ResourceLocation newSpecial = changed.special().map(Ability::id).orElse(null);
            if (newSpecial != null && !newSpecial.equals(oldSpecial)) {
                if (oldSpecial != null) sameLevelAs.put(newSpecial, oldSpecial);
                else if (!addedLevels.containsKey(newSpecial)) addedLevels.put(newSpecial, defaultSpecialLevel(technique, original, i));
            }
        }
        Ability ultimate = technique.definedUltimate().map(u -> effective(u, id)).orElse(null);
        if (ultimate != null) {
            Ability defined = technique.definedUltimate().get();
            ResourceLocation oldSpecial = defined.special().map(Ability::id).orElse(null);
            ResourceLocation newSpecial = ultimate.special().map(Ability::id).orElse(null);
            if (newSpecial != null && !newSpecial.equals(oldSpecial)) {
                if (oldSpecial != null) sameLevelAs.put(newSpecial, oldSpecial);
                else addedLevels.put(newSpecial, UnlockTable.ULTIMATE);
            }
        }
        return new TechniqueViews.View(abilities, ultimate, addedLevels, sameLevelAs);
    }

    /** A special a replacement brings where there was none unlocks where the table puts that slot's special; an added ability's, after it. */
    private static int defaultSpecialLevel(InnateTechnique technique, Ability original, int index) {
        int defined = technique.definedAbilities().indexOf(original);
        return UnlockTable.special(defined >= 0 ? defined : index);
    }

    /** The ability (or its replacement) with its tweaks, and its special as players get it. Unchanged: the same object. */
    private Ability effective(Ability original, ResourceLocation technique) {
        Replacement r = replacements.get(original.id());
        Ability ability = original;
        if (r != null) {
            ability = r.ability();
            if (original.isSpecial()) {
                if (!ability.isSpecial() || !ability.base().map(Ability::id).equals(original.base().map(Ability::id))) {
                    throw new IllegalStateException(r.by() + " replaces " + original.id() + " (in " + technique + ") with something that isn't a special of "
                            + original.base().map(Ability::id).map(String::valueOf).orElse("its base"));
                }
            } else if (ability.isSpecial()) {
                throw new IllegalStateException(r.by() + " replaces " + original.id() + " (in " + technique + ") with a special: it has a number key of its own");
            }
        }
        List<AbilityTweak> own = tweaksOf(original.id());
        Optional<Ability> special = ability.special();
        Ability changedSpecial = special.map(s -> effective(s, technique)).orElse(null);
        boolean specialChanged = special.isPresent() && changedSpecial != special.get();
        if (own.isEmpty() && !specialChanged) return ability;
        return new TweakedAbility(ability, own, changedSpecial);
    }

    /**
     * Every change found what it changes: each addition's technique exists, and each replaced or tweaked ID is an ability of some
     * technique. Throws naming the first that didn't.
     */
    public synchronized void checkAllFound(Collection<ResourceLocation> techniques, Set<ResourceLocation> abilities) {
        for (Addition a : additions) {
            if (!techniques.contains(a.technique())) throw new IllegalStateException(a.by() + " adds " + a.ability().id() + " to " + a.technique() + ", which isn't an innate technique");
        }
        for (Map.Entry<ResourceLocation, Replacement> r : replacements.entrySet()) {
            if (!abilities.contains(r.getKey())) {
                throw new IllegalStateException(r.getValue().by() + " replaces " + r.getKey() + ", which isn't an ability of any innate technique (shared abilities can't be changed)");
            }
        }
        for (Map.Entry<ResourceLocation, List<Tweak>> t : tweaks.entrySet()) {
            if (!abilities.contains(t.getKey())) {
                throw new IllegalStateException(t.getValue().get(0).by() + " tweaks " + t.getKey() + ", which isn't an ability of any innate technique (shared abilities can't be changed)");
            }
        }
    }

    private static int indexOf(List<Ability> list, ResourceLocation id) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).id().equals(id)) return i;
        return -1;
    }

    private static Set<ResourceLocation> idsOf(Ability ability) {
        Set<ResourceLocation> out = new HashSet<>();
        out.add(ability.id());
        ability.special().ifPresent(s -> out.add(s.id()));
        return out;
    }
}
