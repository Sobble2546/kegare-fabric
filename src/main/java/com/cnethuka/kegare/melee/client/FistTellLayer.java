package com.cnethuka.kegare.melee.client;

import com.cnethuka.kegare.client.render.Avatars;
import com.cnethuka.kegare.melee.MeleeTell;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Vector3f;

/**
 * The tell on a player's fists (a ready Strike on the attacking one, Reinforced Fists on both), drawn from the arm as the model has it,
 * so it follows every animation. It draws no items: it extends the item layer only because Player Animation Library's first-person pass
 * (your own arms during an animation) draws no layers on you but items and armor, and a ready fist has to show there too.
 */
final class FistTellLayer extends PlayerItemInHandLayer<AvatarRenderState, PlayerModel> {
    private final boolean slim;

    FistTellLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, boolean slim) {
        super(parent);
        this.slim = slim;
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        AbstractClientPlayer player = Avatars.player(state);
        if (player == null) return;
        float partialTick = Avatars.partialTick(state, player);
        boolean mainLeft = player.getMainArm() == HumanoidArm.LEFT;
        if (FistBurn.zoneOn(player) && !FistTell.firstPersonSelf(player)) { // the zone: a faint flicker of black and red pixels round both fists
            drawFlicker(pose, collector, player, getParentModel().leftArm, true, partialTick);
            drawFlicker(pose, collector, player, getParentModel().rightArm, false, partialTick);
        }
        // A ready Strike on the attacking hand; Reinforced Fists' fire, the very same, on both (§3.28).
        drawFist(pose, collector, light, player, false, mainLeft, partialTick);
        drawFist(pose, collector, light, player, true, !mainLeft, partialTick);
    }

    private void drawFist(PoseStack pose, SubmitNodeCollector collector, int light, AbstractClientPlayer player, boolean offHand, boolean left, float partialTick) {
        MeleeTell.Look look = FistTell.lookOn(player, offHand);
        if (look == null) return;
        ModelPart arm = left ? getParentModel().leftArm : getParentModel().rightArm;
        float time = player.tickCount + partialTick;
        pose.pushPose();
        arm.translateAndRotate(pose);
        Vector3f fist = pose.last().pose().transformPosition(FistShellMesh.armCentre(slim, left), FistShellMesh.fistY(), 0f, new Vector3f());
        if (FistTell.inWorld(player, fist, partialTick)) {
            boolean self = FistTell.firstPersonSelf(player);
            FistTell.drawArm(pose, collector, look, time, slim, left, light, fist.negate(new Vector3f()).normalize(), new Vector3f(0f, 1f, 0f), self);
            FistTell.reportFist(player, offHand, fist);
            if (!self) FistTell.drawWisps(player, offHand, collector, partialTick);
        } else {
            FistTell.drawArm(pose, collector, look, time, slim, left, light, new Vector3f(0f, 0f, 1f), new Vector3f(0f, -1f, 0f), false);
        }
        pose.popPose();
    }

    private void drawFlicker(PoseStack pose, SubmitNodeCollector collector, AbstractClientPlayer player, ModelPart arm, boolean leftArm, float partialTick) {
        pose.pushPose();
        arm.translateAndRotate(pose);
        Vector3f fist = pose.last().pose().transformPosition(FistShellMesh.armCentre(slim, leftArm), FistShellMesh.fistY(), 0f, new Vector3f());
        if (FistTell.inWorld(player, fist, partialTick)) {
            FistBurn.drawFlicker(pose, collector, player.tickCount, player.getId() * 2 + (leftArm ? 1 : 0), slim, leftArm, fist.negate(new Vector3f()).normalize());
        }
        pose.popPose();
    }
}
