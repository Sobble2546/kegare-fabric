package com.cnethuka.kegare.platform;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.world.entity.player.Player;

/**
 * NeoForge's {@code Player.isFakePlayer()} (a NeoForge extension method, so not on Fabric's players). A Fabric API
 * {@link FakePlayer} is one, unless a test said it stands in for a real player ({@link #treatAsReal}).
 */
public final class Players {
    private Players() {}

    private static final Set<Player> REAL = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    public static boolean isFake(Player player) {
        return player instanceof FakePlayer && !REAL.contains(player);
    }

    /**
     * Whether this is a body nothing drives (no client, no tick): Fabric API's {@link FakePlayer}, and the test stand-ins registered with
     * {@link #treatAsReal}. NeoForge's tests overrode {@code isFakePlayer()} on a FakePlayer subclass, so {@code instanceof FakePlayer}
     * stayed true for them; this keeps that.
     */
    public static boolean isFakeBody(Object e) {
        return e instanceof FakePlayer || e instanceof Player p && REAL.contains(p);
    }

    /** Tests: a FakePlayer that must count as a real one for {@link #isFake} (what overriding {@code isFakePlayer()} did on NeoForge). */
    public static <P extends Player> P treatAsReal(P player) {
        REAL.add(player);
        return player;
    }
}
