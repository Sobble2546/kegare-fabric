package com.cnethuka.kegare.platform.client.event;

import com.cnethuka.kegare.platform.bus.Event;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.joml.Matrix4f;

/**
 * A point in the frame's world drawing. Draw in camera-relative coordinates (subtract {@link #getCamera}'s position) with
 * {@link #getPoseStack()} (identity) or your own pose stack.
 * <ul>
 *   <li>AFTER_SKY: the frame's world drawing starts (a flag, not a place to draw).</li>
 *   <li>AFTER_ENTITIES: entities and block entities are submitted; Fabric's {@code WorldRenderEvents.AFTER_ENTITIES}.</li>
 *   <li>AFTER_PARTICLES, AFTER_WEATHER: a frame-graph pass right after the particles / weather pass.</li>
 * </ul>
 */
public final class RenderLevelStageEvent extends Event {
    public enum Stage {
        AFTER_SKY, AFTER_ENTITIES, AFTER_PARTICLES, AFTER_WEATHER
    }

    private final Stage stage;
    private final Camera camera;
    private final DeltaTracker partialTick;
    private final PoseStack poseStack;
    private final Matrix4f modelViewMatrix;
    private final Matrix4f projectionMatrix;

    public RenderLevelStageEvent(Stage stage, Camera camera, DeltaTracker partialTick, PoseStack poseStack, Matrix4f modelViewMatrix,
                                 Matrix4f projectionMatrix) {
        this.stage = stage;
        this.camera = camera;
        this.partialTick = partialTick;
        this.poseStack = poseStack;
        this.modelViewMatrix = modelViewMatrix;
        this.projectionMatrix = projectionMatrix;
    }

    public Stage getStage() {
        return stage;
    }

    public Camera getCamera() {
        return camera;
    }

    public DeltaTracker getPartialTick() {
        return partialTick;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    /** The world-to-camera rotation of this frame (no translation), a copy to read. */
    public Matrix4f getModelViewMatrix() {
        return modelViewMatrix;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
}
