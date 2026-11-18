package com.cnethuka.kegare.innate_technique.rime.client;

import com.cnethuka.kegare.client.visual.ClientVisuals;
import com.cnethuka.kegare.innate_technique.rime.GlacialSpireEntity;
import com.cnethuka.kegare.innate_technique.rime.RimeIds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Glacial Spire's spike, drawn from the moment the server says it erupted (its spike loop) until the marker is gone, but never round
 * your own first-person camera ({@link #cameraInside}). A solid mesh of the same ice as the lance (LanceMesh.SPIRE), its pieces
 * shooting up out of the ground a beat apart, as tall and as heavy as the charge. Nothing during the warning (that's the crack on the
 * ground, SpireClient). The ground hides what is still under it. Seen from inside (someone launched off its point), its faces are
 * culled, so it never fills that player's screen.
 */
public class GlacialSpireRenderer extends EntityRenderer<GlacialSpireEntity, GlacialSpireRenderer.State> {
    /** What the spike looks like this frame, read off the entity when its state is extracted. */
    public static final class State extends EntityRenderState {
        /** Not drawn: no spike loop yet, or the first-person camera is inside it. */
        boolean hidden = true;
        /** Ticks into the spike loop. */
        float ticks;
        float charge;
        int id;
    }

    public GlacialSpireRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0f;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GlacialSpireEntity spire, State state, float partialTick) {
        super.extractRenderState(spire, state, partialTick);
        ClientVisuals.LoopTime time = ClientVisuals.loopTime(spire, RimeIds.SPIRE_SPIKE_KEY, partialTick);
        state.charge = spire.charge();
        state.id = spire.getId();
        state.ticks = time == null ? 0f : time.ticks();
        state.hidden = time == null || cameraInside(spire, SpireShape.width(state.charge), SpireShape.height(state.charge));
    }

    @Override
    public boolean shouldRender(GlacialSpireEntity spire, Frustum frustum, double camX, double camY, double camZ) {
        // The marker is a speck on the ground; the spike stands three blocks over it.
        AABB box = new AABB(spire.getX() - 1.6, spire.getY() - 0.2, spire.getZ() - 1.6, spire.getX() + 1.6, spire.getY() + 3.8, spire.getZ() + 1.6);
        return frustum.isVisible(box);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.hidden) return;
        float t = state.ticks;
        float charge = state.charge;
        float height = SpireShape.height(charge), width = SpireShape.width(charge);
        float turn = (state.id * 137.508f) % 360f; // each spike its own way round
        float quiver = SpireShape.quiver(t);
        List<PoseStack.Pose> poses = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < SpireShape.PIECES.size(); i++) {
            if (!SpireShape.shown(i, charge)) continue;
            float rise = SpireShape.rise(t, i);
            if (rise <= 0f) continue;
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(turn));
            pose.mulPose(Axis.XP.rotationDegrees(quiver));
            pose.mulPose(Axis.ZP.rotationDegrees(quiver * 0.6f));
            pose.translate(0f, -SpireShape.sunk(t, i) * height, 0f);
            pose.scale(width, height * Math.max(1f, rise), width);
            poses.add(pose.last().copy());
            names.add(SpireShape.PIECES.get(i));
            pose.popPose();
        }
        if (!names.isEmpty()) LanceMesh.SPIRE.draw(collector, poses, names, state.lightCoords);
    }

    /**
     * Your own first-person camera is inside the spike (it erupted under you, or you're standing in it): it isn't drawn until you're
     * out, or it would fill your view for the frames before the launch carries you clear. You see it below you once you're above it.
     */
    private static boolean cameraInside(GlacialSpireEntity spire, float width, float height) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera.isDetached()) return false;
        Vec3 c = camera.position();
        double dx = c.x - spire.getX(), dz = c.z - spire.getZ(), dy = c.y - spire.getY();
        double across = 1.4 * width;
        return dx * dx + dz * dz < across * across && dy > -0.5 && dy < 3.6 * height;
    }
}
