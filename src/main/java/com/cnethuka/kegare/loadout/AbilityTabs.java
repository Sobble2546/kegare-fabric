package com.cnethuka.kegare.loadout;

import com.cnethuka.kegare.api.ability.Ability;
import com.cnethuka.kegare.api.ability.AbilityCategory;
import com.cnethuka.kegare.api.technique.InnateTechnique;
import java.util.List;
import javax.annotation.Nullable;

/** The Sorcerer screen's tabs (DESIGN §3.16): one per {@link AbilityCategory}, and what each holds. */
public final class AbilityTabs {
    private AbilityTabs() {}

    /** The tabs a player sees: all of them, in order, or none for a Severed player (no tabs and no bar). */
    public static List<AbilityCategory> tabsFor(boolean severed) {
        return severed ? List.of() : List.of(AbilityCategory.values());
    }

    /** What's under this tab: the technique's own abilities for {@link AbilityCategory#INNATE}, else the shared abilities of this category. */
    public static List<Ability> abilities(AbilityCategory category, @Nullable InnateTechnique technique) {
        if (category == AbilityCategory.INNATE) return technique == null ? List.of() : technique.abilities();
        return SharedAbilities.all().stream().filter(a -> a.category() == category).toList();
    }
}
