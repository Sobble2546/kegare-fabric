package com.cnethuka.kegare.innate_technique.puppet_manipulation.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.joml.Vector3f;

/**
 * The owner's player model, posed like a player (crouching, arms held out for what it holds: both are in the render state, see
 * {@link PuppetRenderer}), plus the idle spin's arm and head keyframes while spinning ({@link PuppetPoses}; the whole-body turn is
 * applied by the renderer). It is posed when it is drawn, from the render state alone.
 */
public class PuppetModel extends PlayerModel {
    public PuppetModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
        super.setupAnim(state);
        spin(this, state);
    }

    /** The idle spin's arm and head keyframes, if the state is a spinning puppet's. */
    static void spin(HumanoidModel<?> model, AvatarRenderState state) {
        PuppetRenderer.Look look = state.getData(PuppetRenderer.LOOK);
        if (look == null || !look.spinning()) return;
        PuppetPoses.idleSpin().ifPresent(clip -> {
            float t = look.spinSeconds();
            apply(clip.rotation("right_arm", t), model.rightArm);
            apply(clip.rotation("left_arm", t), model.leftArm);
            apply(clip.rotation("head", t), model.head); // the hat and the sleeves are children of these parts: they follow
        });
    }

    /** The armor a puppet wears: posed from the state alone like any armor model, so it takes the spin's keyframes itself. */
    static final class Armor extends HumanoidModel<AvatarRenderState> {
        Armor(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
            super.setupAnim(state);
            spin(this, state);
        }
    }

    /** Degrees on the model's own axes (player_animations/puppet_manipulation.json): x negative = arm forward, z positive = right arm out. */
    private static void apply(Vector3f degrees, ModelPart part) {
        if (degrees == null) return;
        part.xRot = (float) Math.toRadians(degrees.x);
        part.yRot = (float) Math.toRadians(degrees.y);
        part.zRot = (float) Math.toRadians(degrees.z);
    }
}
