package com.cnethuka.kegare.mixin;

import com.cnethuka.kegare.database.KegareDatabase;
import com.cnethuka.kegare.platform.attachment.AttachmentHolder;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Two things at a player's login (Minecraft 1.21.2+ shape; checked against the 1.21.11 and 26.3 jars: the login task makes the player,
 * reads the file with {@code loadPlayerData}, loads it onto the player, then calls {@code placeNewPlayer}).
 * <ul>
 *   <li>{@code loadPlayerData}: a file the NeoForge build wrote keeps its attachments under {@code neoforge:attachments}, which Fabric
 *       doesn't read, so they are copied to {@code fabric:attachments} before the player loads from the tag.</li>
 *   <li>{@code placeNewPlayer}: the player has their world file on them and nothing else has looked at them yet. With Postgres storage on,
 *       what's in the database goes on them here, so nothing rolls for them first (DESIGN §3.33).</li>
 * </ul>
 */
@Mixin(PlayerList.class)
abstract class PlayerListLoadMixin {
    @Inject(method = "loadPlayerData", at = @At("RETURN"))
    private void kegare$readNeoForgeAttachments(NameAndId id, CallbackInfoReturnable<Optional<CompoundTag>> callback) {
        callback.getReturnValue().ifPresent(AttachmentHolder::migrateNeoForgeKey);
    }

    @Inject(method = "placeNewPlayer", at = @At("HEAD"))
    private void kegare$applyStoredData(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo callback) {
        KegareDatabase database = KegareDatabase.current();
        if (database != null) database.applyOnJoin(player);
    }
}
