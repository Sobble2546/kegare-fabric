package com.cnethuka.kegare.platform;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.level.ServerLevel;

/** NeoForge's {@code FakePlayerFactory}, over Fabric API's {@link FakePlayer} (the class every {@code instanceof FakePlayer} in Kegare names). */
public final class FakePlayerFactory {
    private FakePlayerFactory() {}

    /** The shared fake player for a profile in a level. */
    public static FakePlayer get(ServerLevel level, GameProfile profile) {
        return FakePlayer.get(level, profile);
    }

    /** The shared "Minecraft" fake player, who stands in for the game itself. */
    public static FakePlayer getMinecraft(ServerLevel level) {
        return FakePlayer.get(level);
    }
}
