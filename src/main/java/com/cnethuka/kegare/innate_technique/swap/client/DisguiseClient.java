package com.cnethuka.kegare.innate_technique.swap.client;

import com.cnethuka.kegare.innate_technique.swap.Disguises;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import com.cnethuka.kegare.sixeyes.client.SightClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractSkullBlock;
import com.cnethuka.kegare.platform.bus.EventPriority;
import com.cnethuka.kegare.platform.client.event.ClientPlayerNetworkEvent;
import com.cnethuka.kegare.platform.client.event.RenderNameTagEvent;
import com.cnethuka.kegare.platform.client.event.RenderPlayerEvent;
import com.cnethuka.kegare.platform.NeoForge;

/**
 * Draws Swap's disguises ({@link Disguises}, synced). A disguised player is drawn as who they look like: skin, arm
 * model and cape ({@code PlayerInfoMixin}), tab-list name ({@code PlayerTabOverlayMixin}), name tag, visible armor and
 * held items (render events below). Never for the local player: everyone sees their own body, HUD and inventory
 * normally. Nothing real moves: equipment is only borrowed for the length of one render.
 */
public final class DisguiseClient {
    /** Set while resolving the face behind a disguise, so a two-way swap doesn't chase itself. Client thread only. */
    private static boolean resolving;
    private DisguiseClient() {}

    static void init() {
        NeoForge.EVENT_BUS.addListener(DisguiseClient::onNameTag);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, DisguiseClient::onRenderPost);
        NeoForge.EVENT_BUS.addListener(DisguiseClient::onLogout);
    }

    /** Who this player should be drawn as on this client, or null (not disguised, expired, or it's us). */
    @Nullable
    public static UUID shownAs(UUID player) {
        if (resolving) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || player.equals(mc.player.getUUID())) return null;
        Disguises.Entry entry = Disguises.clientEntry(player);
        if (entry == null || entry.endTick() <= mc.level.getGameTime()) return null;
        return entry.as();
    }

    /** The tab-list entry of who this player looks like, or null. A Six Eyes bearer's list keeps the real name: they know people by their energy. */
    @Nullable
    public static PlayerInfo shownInfo(UUID player) {
        UUID as = SightClient.isBearer() ? null : shownAs(player);
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return as == null || connection == null ? null : connection.getPlayerInfo(as);
    }

    /** The skin to draw for this player, or null for their own. */
    @Nullable
    public static PlayerSkin skinFor(UUID player) {
        UUID as = shownAs(player);
        if (as == null) return null;
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        PlayerInfo info = connection == null ? null : connection.getPlayerInfo(as);
        if (info == null) return DefaultPlayerSkin.get(as);
        return resolving(info::getSkin);
    }

    /** Runs {@code task} with disguises switched off (to read the real look of the face being borrowed). */
    public static <T> T resolving(Supplier<T> task) {
        boolean was = resolving;
        resolving = true;
        try {
            return task.get();
        } finally {
            resolving = was;
        }
    }

    // ---------------------------------------------------------------- name tag

    private static void onNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        // The face stays, but a Six Eyes bearer knows people by their energy and reads the real name over it (DESIGN §3.32).
        UUID as = SightClient.isBearer() ? null : shownAs(player.getUUID());
        if (as != null) event.setContent(nameOf(as));
    }

    private static Component nameOf(UUID as) {
        Minecraft mc = Minecraft.getInstance();
        Player face = mc.level == null ? null : mc.level.getPlayerByUUID(as);
        if (face != null) return face.getDisplayName();
        PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(as);
        return Component.literal(info != null ? info.getProfile().name() : "?");
    }

    // ---------------------------------------------------------------- armor and held items

    /**
     * The disguised player's render state has just been read off them: swap what it holds for the face's visible gear, or for nothing if
     * this client can't see the face (better bare than the wearer's own). On 1.21.11 armor, held items and the head item are read into the
     * state when it's extracted, so writing the face's into the state is all it takes: nothing of the wearer's own is touched.
     */
    private static void onRenderPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        UUID as = shownAs(player.getUUID());
        if (as == null) return;
        Minecraft mc = Minecraft.getInstance();
        Player face = mc.level == null ? null : mc.level.getPlayerByUUID(as);
        AvatarRenderState state = event.getRenderState();
        state.headEquipment = worn(face, EquipmentSlot.HEAD);
        state.chestEquipment = worn(face, EquipmentSlot.CHEST);
        state.legsEquipment = worn(face, EquipmentSlot.LEGS);
        state.feetEquipment = worn(face, EquipmentSlot.FEET);
        // A head item that isn't armor (a pumpkin, a skull) goes through the head item layer.
        state.headItem.clear();
        state.wornHeadType = null;
        state.wornHeadProfile = null;
        if (!HumanoidArmorLayer.shouldRender(state.headEquipment, EquipmentSlot.HEAD)) {
            mc.getItemModelResolver().updateForLiving(state.headItem, state.headEquipment, ItemDisplayContext.HEAD, player);
            if (state.headEquipment.getItem() instanceof BlockItem block && block.getBlock() instanceof AbstractSkullBlock skull) {
                state.wornHeadType = skull.getType();
                state.wornHeadProfile = state.headEquipment.get(DataComponents.PROFILE);
            }
        }
        // Hands: the face's main hand and off hand, on the wearer's arms as they hold theirs.
        ItemStack main = face != null ? face.getMainHandItem() : ItemStack.EMPTY, off = face != null ? face.getOffhandItem() : ItemStack.EMPTY;
        boolean rightMain = state.mainArm == HumanoidArm.RIGHT;
        state.rightHandItemStack = rightMain ? main : off;
        state.leftHandItemStack = rightMain ? off : main;
        mc.getItemModelResolver().updateForLiving(state.rightHandItemState, state.rightHandItemStack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, player);
        mc.getItemModelResolver().updateForLiving(state.leftHandItemState, state.leftHandItemStack, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, player);
        // The arms follow what's held: bare if the face holds nothing there, held out if it holds something (a bow or a shield the wearer was
        // using keeps its pose: that is the wearer's own action).
        state.rightArmPose = armPose(state.rightArmPose, state.rightHandItemStack);
        state.leftArmPose = armPose(state.leftArmPose, state.leftHandItemStack);
    }

    private static ItemStack worn(Player face, EquipmentSlot slot) {
        return face != null ? face.getItemBySlot(slot) : ItemStack.EMPTY;
    }

    private static HumanoidModel.ArmPose armPose(HumanoidModel.ArmPose own, ItemStack held) {
        if (held.isEmpty()) return HumanoidModel.ArmPose.EMPTY;
        return own == HumanoidModel.ArmPose.EMPTY ? HumanoidModel.ArmPose.ITEM : own;
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        Disguises.clearClient();
    }
}
