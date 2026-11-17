package com.cnethuka.kegare.innate_technique.time_manipulation;

import com.cnethuka.kegare.Kegare;
import com.cnethuka.kegare.api.ability.Ability;
import com.cnethuka.kegare.api.ability.BaseAbility;
import com.cnethuka.kegare.api.ability.CastType;
import com.cnethuka.kegare.api.ability.SpecialAbility;
import java.util.function.Supplier;

/** Shared bits of Time Manipulation's abilities. */
abstract class TimeAbility extends BaseAbility {
    TimeAbility(String name, CastType castType) {
        super(Kegare.id(name), castType);
    }

    /** A Time Manipulation special (R): shares its base ability's cooldown. */
    abstract static class Special extends SpecialAbility {
        Special(String name, CastType castType, Supplier<? extends Ability> base) {
            super(Kegare.id(name), castType, base);
        }
    }
}
