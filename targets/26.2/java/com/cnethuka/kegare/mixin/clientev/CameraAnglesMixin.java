package com.cnethuka.kegare.mixin.clientev;

import com.cnethuka.kegare.platform.client.ClientEventHooks;
import net.minecraft.client.Camera;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ViewportEvent.ComputeCameraAngles: after the camera was set up for the frame, the listeners may turn it (yaw, pitch) and roll it.
 * Vanilla's own {@code setRotation} builds the camera quaternion with {@code rotationYXZ(PI - yaw, -pitch, 0)}; the third angle is the
 * roll about the view axis, so a roll is the same call with that angle set (and the three direction vectors rebuilt as vanilla does).
 * NeoForge rotated the view matrix by +roll, which is -roll on the camera's own rotation. The sign has not been looked at on screen.
 */
@Mixin(Camera.class)
public abstract class CameraAnglesMixin {
    @Shadow
    private float xRot;
    @Shadow
    private float yRot;
    @Shadow
    @Final
    private Quaternionf rotation;
    @Shadow
    @Final
    private Vector3f forwards;
    @Shadow
    @Final
    private Vector3f up;
    @Shadow
    @Final
    private Vector3f left;
    @Shadow
    @Final
    private static org.joml.Vector3fc FORWARDS;
    @Shadow
    @Final
    private static org.joml.Vector3fc UP;
    @Shadow
    @Final
    private static org.joml.Vector3fc LEFT;

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    /** 26.x: the camera lines itself up with its entity in alignWithEntity (1.21.11's setup). UNVERIFIED in game. */
    @Inject(method = "alignWithEntity", at = @At("RETURN"))
    private void kegare$angles(float partialTick, CallbackInfo ci) {
        float[] angles = ClientEventHooks.computeCameraAngles((Camera) (Object) this, partialTick, yRot, xRot);
        if (angles[0] != yRot || angles[1] != xRot) setRotation(angles[0], angles[1]);
        if (angles[2] != 0f) {
            float toRadians = (float) Math.PI / 180f;
            rotation.rotationYXZ((float) Math.PI - yRot * toRadians, -xRot * toRadians, -angles[2] * toRadians);
            FORWARDS.rotate(rotation, forwards);
            UP.rotate(rotation, up);
            LEFT.rotate(rotation, left);
        }
    }

    /** ViewportEvent.ComputeFov: 26.x works the field of view out on the camera (the world's here, the hand's below). UNVERIFIED in game. */
    @com.llamalad7.mixinextras.injector.ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float kegare$fov(float fov, float partialTick) {
        return ClientEventHooks.computeFov((Camera) (Object) this, partialTick, fov, true);
    }

    @com.llamalad7.mixinextras.injector.ModifyReturnValue(method = "calculateHudFov", at = @At("RETURN"))
    private float kegare$hudFov(float fov, float partialTick) {
        return ClientEventHooks.computeFov((Camera) (Object) this, partialTick, fov, false);
    }
}
