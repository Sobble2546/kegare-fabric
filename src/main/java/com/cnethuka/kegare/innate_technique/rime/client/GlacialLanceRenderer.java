package com.cnethuka.kegare.innate_technique.rime.client;

import com.cnethuka.kegare.innate_technique.rime.GlacialLanceEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A thrown Glacial Lance: the mesh, as many chunks as it was charged with, pointing where it flies and turning slowly about its own
 * length. Its tip leads the hitbox a little, so what touches first is what hits.
 */
public final class GlacialLanceRenderer extends EntityRenderer<GlacialLanceEntity, GlacialLanceRenderer.State> {
    /** The mesh is drawn at this size. */
    public static final float SCALE = 0.85f;
    /** How far the tip runs ahead of the middle of the hitbox. */
    private static final float TIP_LEAD = 0.3f;
    /** Degrees a tick about its own length: slow enough to read as a heavy thing turning, not a drill. */
    private static final float SPIN = 11f;
    /** Your own lance, in first person, isn't drawn until it's this far from the camera: it leaves from between your eyes. */
    private static final double OWN_HIDDEN_WITHIN = 3.5;

    /** What the lance looks like this frame, read off the entity when its state is extracted. */
    public static final class State extends EntityRenderState {
        float charge;
        float yRot, xRot;
        boolean hidden;
    }

    public GlacialLanceRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0f;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GlacialLanceEntity lance, State state, float partialTick) {
        super.extractRenderState(lance, state, partialTick);
        state.charge = lance.charge();
        state.yRot = Mth.lerp(partialTick, lance.yRotO, lance.getYRot());
        state.xRot = Mth.lerp(partialTick, lance.xRotO, lance.getXRot());
        state.hidden = hiddenFromOwnCamera(lance, partialTick);
    }

    @Override
    public boolean shouldRender(GlacialLanceEntity lance, Frustum frustum, double camX, double camY, double camZ) {
        if (!lance.shouldRender(camX, camY, camZ)) return false;
        return frustum.isVisible(lance.getBoundingBox().inflate(2.5)); // the lance is much longer than its hitbox
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.hidden) return;
        float minimum = LanceShape.minimum();
        float[] scales = new float[LanceShape.PIECES.size()];
        float tip = 0f;
        for (int i = 0; i < scales.length; i++) {
            if (!LanceShape.shown(i, state.charge, minimum)) continue;
            scales[i] = 1f;
            LanceMesh.Piece piece = LanceMesh.piece(LanceShape.PIECES.get(i));
            if (piece != null) tip = Math.max(tip, piece.maxZ());
        }
        pose.pushPose();
        pose.translate(0f, state.boundingBoxHeight * 0.5f, 0f);
        orient(pose, state.yRot, state.xRot);
        pose.translate(0f, 0f, TIP_LEAD - tip * SCALE);
        pose.mulPose(Axis.ZP.rotationDegrees(state.ageInTicks * SPIN));
        pose.scale(SCALE, SCALE, SCALE);
        LanceMesh.draw(collector, pose, LanceShape.PIECES, scales, state.lightCoords);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    /** Turns the mesh's +Z to where a projectile with these rotations is heading (a projectile's yaw is atan2(x, z), its pitch up). */
    static void orient(PoseStack pose, float yRot, float xRot) {
        pose.mulPose(Axis.YP.rotationDegrees(yRot - 90f));
        pose.mulPose(Axis.ZP.rotationDegrees(xRot));
        pose.mulPose(Axis.YP.rotationDegrees(90f));
    }

    private static boolean hiddenFromOwnCamera(GlacialLanceEntity lance, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || lance.getOwner() != mc.player || !mc.options.getCameraType().isFirstPerson()) return false;
        Vec3 camera = mc.gameRenderer.getMainCamera().position();
        return lance.getPosition(partialTick).distanceToSqr(camera) < OWN_HIDDEN_WITHIN * OWN_HIDDEN_WITHIN;
    }
}
