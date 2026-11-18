package com.cnethuka.kegare.innate_technique.puppet_manipulation.client;

import com.cnethuka.kegare.innate_technique.puppet_manipulation.PuppetEntity;
import com.cnethuka.kegare.innate_technique.puppet_manipulation.PuppetShellEntity;
import com.cnethuka.kegare.innate_technique.swap.client.DisguiseClient;
import com.mojang.authlib.GameProfile;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * A puppet's look: its owner's real skin (never a Swap disguise they happen to wear). From the tab list while the owner
 * is online; otherwise from the skin property the puppet carries (loaded once by the skin manager, which caches it),
 * falling back to the owner's default skin.
 */
public final class PuppetSkins {
    private PuppetSkins() {}

    /** A puppet's skin lookup, kept while the owner and the skin property it was made from stay the same (it was built every frame). */
    private record Looked(UUID owner, String value, Supplier<PlayerSkin> lookup) {}

    private static final Map<UUID, Looked> LOOKED = new HashMap<>();
    private static final int MAX_LOOKED = 256;

    public static PlayerSkin skin(PuppetEntity puppet) {
        return skin(puppet.getUUID(), puppet.ownerId(), puppet.skinValue(), puppet::ownerProfile);
    }

    /** A possessing player's body: their real skin too. */
    public static PlayerSkin skin(PuppetShellEntity shell) {
        return skin(shell.getUUID(), shell.ownerId(), shell.skinValue(), shell::ownerProfile);
    }

    private static PlayerSkin skin(UUID entity, UUID owner, String value, Supplier<GameProfile> profile) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (owner != null && connection != null) {
            PlayerInfo info = connection.getPlayerInfo(owner);
            if (info != null) return DisguiseClient.resolving(info::getSkin);
        }
        Looked looked = LOOKED.get(entity);
        if (looked == null || !Objects.equals(looked.owner(), owner) || !looked.value().equals(value)) {
            if (LOOKED.size() >= MAX_LOOKED) LOOKED.clear(); // puppets come and go; the skin manager keeps the skins themselves
            looked = new Looked(owner, value, mc.getSkinManager().createLookup(profile.get(), false));
            LOOKED.put(entity, looked);
        }
        return looked.lookup().get();
    }
}
