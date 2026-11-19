package com.cnethuka.kegare.loadout;

import com.cnethuka.kegare.api.ability.Ability;
import com.cnethuka.kegare.api.technique.InnateTechnique;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.resources.Identifier;
import com.cnethuka.kegare.api.ability.AbilityCategory;

/** Finds any ability by ID, wherever it comes from: the innate technique (with specials) or the shared ones. */
public final class AbilityLookup {
    private AbilityLookup() {}

    public static Optional<Ability> find(@Nullable InnateTechnique technique, Identifier id) {
        if (technique != null) {
            Optional<Ability> own = technique.findAbility(id);
            if (own.isPresent()) return own;
        }
        return SharedAbilities.find(id);
    }

    public static AbilityCategory category(Identifier id) {
        return SharedAbilities.find(id).map(com.cnethuka.kegare.api.ability.Ability::category).orElse(AbilityCategory.INNATE);
    }
}
