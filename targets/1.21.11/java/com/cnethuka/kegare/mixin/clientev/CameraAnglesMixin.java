package com.cnethuka.kegare.mixin.clientev;

import com.cnethuka.kegare.platform.client.ClientEventHooks;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
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
    private static Vector3f FORWARDS;
    @Shadow
    @Final
    private static Vector3f UP;
    @Shadow
    @Final
    private static Vector3f LEFT;

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("RETURN"))
    private void kegare$angles(Level level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
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
}
