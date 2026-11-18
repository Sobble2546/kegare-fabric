package com.cnethuka.kegare.innate_technique.puppet_manipulation.client;

import com.cnethuka.kegare.innate_technique.puppet_manipulation.PuppetShellEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * A possessing player's empty body: their skin, armor and name, standing slack (head bowed, arms hanging) where they
 * left it. Full size. Drawn from a player's render state ({@link AvatarRenderState}), as a puppet is.
 */
public class ShellRenderer extends EntityRenderer<PuppetShellEntity, AvatarRenderState> {
    private final Body wide;
    private final Body slim;

    public ShellRenderer(EntityRendererProvider.Context context) {
        super(context);
        wide = new Body(context, false);
        slim = new Body(context, true);
        shadowRadius = 0.5f;
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(PuppetShellEntity shell, AvatarRenderState state, float partialTick) {
        PlayerSkin skin = PuppetSkins.skin(shell);
        (skin.model() == PlayerModelType.SLIM ? slim : wide).extract(shell, state, partialTick, skin);
    }

    @Override
    public void submit(AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        (state.skin.model() == PlayerModelType.SLIM ? slim : wide).submit(state, pose, collector, camera);
    }

    /** The player model, slack: nobody is home. Posed from the render state, which {@link Body#extract} has stilled. */
    static final class SlackModel extends PlayerModel {
        SlackModel(ModelPart root, boolean slim) {
            super(root, slim);
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
            super.setupAnim(state);
            slack(this);
        }
    }

    /** The armor on a slack body hangs slack too (armor models are posed from the state alone, not from the body's model). */
    static final class SlackArmor extends HumanoidModel<AvatarRenderState> {
        SlackArmor(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
            super.setupAnim(state);
            slack(this);
        }
    }

    static void slack(HumanoidModel<?> model) {
        model.head.xRot = 0.55f; // bowed
        model.head.yRot = 0f; // the hat is a child of the head: it follows
        model.rightArm.xRot = 0.05f;
        model.rightArm.zRot = 0.04f;
        model.leftArm.xRot = 0.05f;
        model.leftArm.zRot = -0.04f; // the sleeves follow their arms
    }

    static final class Body extends LivingEntityRenderer<PuppetShellEntity, AvatarRenderState, PlayerModel> {
        Body(EntityRendererProvider.Context context, boolean slim) {
            super(context, new SlackModel(context.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim), 0.5f);
            addLayer(new HumanoidArmorLayer<>(this,
                    ArmorModelSet.bake(slim ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR, context.getModelSet(),
                            SlackArmor::new),
                    context.getEquipmentRenderer()));
        }

        @Override
        public AvatarRenderState createRenderState() {
            return new AvatarRenderState();
        }

        @Override
        public void extractRenderState(PuppetShellEntity shell, AvatarRenderState state, float partialTick) {
            extract(shell, state, partialTick, PuppetSkins.skin(shell));
        }

        void extract(PuppetShellEntity shell, AvatarRenderState state, float partialTick, PlayerSkin skin) {
            super.extractRenderState(shell, state, partialTick);
            HumanoidMobRenderer.extractHumanoidRenderState(shell, state, partialTick, itemModelResolver);
            // Nobody is home: no walking, no looking about.
            state.walkAnimationPos = 0f;
            state.walkAnimationSpeed = 0f;
            state.skin = skin;
            state.id = shell.getId();
            state.showHat = state.showJacket = state.showLeftPants = state.showRightPants = state.showLeftSleeve = state.showRightSleeve = true;
            state.showCape = false;
        }

        @Override
        public Identifier getTextureLocation(AvatarRenderState state) {
            return state.skin.body().texturePath();
        }
    }
}
