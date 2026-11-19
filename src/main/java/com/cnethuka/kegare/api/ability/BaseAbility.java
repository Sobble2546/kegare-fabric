package com.cnethuka.kegare.api.ability;

import net.minecraft.resources.Identifier;

/** Convenience base holding an ability's ID and cast type. */
public abstract class BaseAbility implements Ability {
    private final Identifier id;
    private final CastType castType;

    protected BaseAbility(Identifier id, CastType castType) {
        this.id = id;
        this.castType = castType;
    }

    @Override
    public final Identifier id() {
        return id;
    }

    @Override
    public final CastType castType() {
        return castType;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
