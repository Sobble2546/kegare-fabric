package com.cnethuka.kegare.trait;

import com.cnethuka.kegare.Kegare;
import com.cnethuka.kegare.registry.KegareRegistries;
import com.cnethuka.kegare.platform.registry.DeferredHolder;
import com.cnethuka.kegare.platform.registry.DeferredRegister;
import com.cnethuka.kegare.api.trait.Trait;

public final class KegareTraits {
    public static final DeferredRegister<Trait> TRAITS = DeferredRegister.create(KegareRegistries.TRAIT_KEY, Kegare.MODID);

    /** Unique, needs Malice, passes on when its bearer is killed by a player. Its gameplay is the {@code sixeyes} package (DESIGN §3.32). */
    public static final DeferredHolder<Trait, Trait> SIX_EYES = TRAITS.register("six_eyes", SixEyesTrait::new);

    private KegareTraits() {}
}
