package com.cnethuka.kegare;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;

/**
 * A player for tests with no connection behind it. Fabric API's own {@code FakePlayer} is no use here: its mixin refuses every advancement
 * for any instance of that class ({@code PlayerAdvancementsMixin.preventGrantCriterion}), where on NeoForge a test player only had to say
 * {@code isFakePlayer() == false} to keep real ones. So this is what {@code FakePlayer} is (a {@link ServerPlayer} that doesn't tick, whose
 * packets go nowhere, which keeps no statistics), without being one.
 */
public class TestPlayer extends ServerPlayer {
    public TestPlayer(ServerLevel level, GameProfile profile) {
        super(level.getServer(), level, profile, ClientInformation.createDefault());
        // Fabric API's stand-in listener: accepts and drops every packet.
        this.connection = standIn(this);
        // The world's default is Creative here, and a Creative player is invulnerable; Fabric API's FakePlayer is forced to Survival by a mixin.
        this.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    }

    /** Fabric API's own {@code FakePlayer} listener, which has a different name in each Minecraft build it supports. */
    private static ServerGamePacketListenerImpl standIn(ServerPlayer player) {
        for (String name : new String[] {"FakePlayerNetworkHandler", "FakePlayerPacketListener"}) {
            try {
                Class<?> type = Class.forName("net.fabricmc.fabric.impl.event.interaction." + name);
                return (ServerGamePacketListenerImpl) type.getConstructor(ServerPlayer.class).newInstance(player);
            } catch (ClassNotFoundException e) {
                // the other spelling
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("Fabric API has no fake player listener to borrow");
    }

    @Override
    public void tick() {}

    @Override
    public void updateOptions(ClientInformation options) {}

    @Override
    public void awardStat(Stat<?> stat, int amount) {}

    @Override
    public void resetStat(Stat<?> stat) {}

    /** Like a fake player, unhurtable and harmless unless a test says otherwise ({@code TestServer.damageable}). */
    @Override
    public boolean isInvulnerableTo(ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
        return true;
    }

    @Override
    public boolean canHarmPlayer(net.minecraft.world.entity.player.Player other) {
        return false;
    }

    @Override
    public net.minecraft.world.scores.PlayerTeam getTeam() {
        return null;
    }
}
