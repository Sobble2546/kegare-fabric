package com.cnethuka.kegare.platform.client.event;

import com.cnethuka.kegare.platform.bus.Event;
import net.minecraft.client.Camera;

/** Camera settings of this frame, adjustable by listeners. */
public abstract class ViewportEvent extends Event {
    private final Camera camera;
    private final double partialTick;

    private ViewportEvent(Camera camera, double partialTick) {
        this.camera = camera;
        this.partialTick = partialTick;
    }

    public Camera getCamera() {
        return camera;
    }

    public double getPartialTick() {
        return partialTick;
    }

    /** The field of view, in degrees. {@link #usedConfiguredFov()} is false for the hand's own view. */
    public static final class ComputeFov extends ViewportEvent {
        private final boolean usedConfiguredFov;
        private double fov;

        public ComputeFov(Camera camera, double partialTick, double fov, boolean usedConfiguredFov) {
            super(camera, partialTick);
            this.fov = fov;
            this.usedConfiguredFov = usedConfiguredFov;
        }

        public double getFOV() {
            return fov;
        }

        public void setFOV(double fov) {
            this.fov = fov;
        }

        public boolean usedConfiguredFov() {
            return usedConfiguredFov;
        }
    }

    /** The camera's angles in degrees: yaw and pitch as the game set them, roll 0. */
    public static final class ComputeCameraAngles extends ViewportEvent {
        private float yaw;
        private float pitch;
        private float roll;

        public ComputeCameraAngles(Camera camera, double partialTick, float yaw, float pitch, float roll) {
            super(camera, partialTick);
            this.yaw = yaw;
            this.pitch = pitch;
            this.roll = roll;
        }

        public float getYaw() {
            return yaw;
        }

        public void setYaw(float yaw) {
            this.yaw = yaw;
        }

        public float getPitch() {
            return pitch;
        }

        public void setPitch(float pitch) {
            this.pitch = pitch;
        }

        public float getRoll() {
            return roll;
        }

        public void setRoll(float roll) {
            this.roll = roll;
        }
    }
}
