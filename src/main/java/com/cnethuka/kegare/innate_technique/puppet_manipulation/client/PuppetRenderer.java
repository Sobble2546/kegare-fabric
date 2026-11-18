package com.cnethuka.kegare.innate_technique.puppet_manipulation.client;

import com.cnethuka.kegare.innate_technique.puppet_manipulation.PuppetEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.joml.Vector3f;

/**
 * Draws a puppet as its owner: their skin and arm model (wide or slim), held items and armor (Stand-In borrows them),
 * the Malice glow ({@link PuppetGlowLayer}) and, while it spins, the idle spin's body turn. The 1.21 scale attribute
 * makes it half size. No name tag (a Stand-In shows its owner's).
 * <p>
 * 1.21.11: it is drawn from the same render state as a player ({@link AvatarRenderState}); what a puppet has beyond that travels
 * with the state as {@link Look}.
 */
public class PuppetRenderer extends EntityRenderer<PuppetEntity, AvatarRenderState> {
    /** What a puppet looks like this frame beyond a player's render state. */
    public record Look(boolean possessed, boolean standIn, boolean invisible, float hop, boolean spinning, float spinSeconds, int maliceColor, int tier) {}

    public static final RenderStateDataKey<Look> LOOK = RenderStateDataKey.create(() -> "kegare puppet look");

    private final Body wide;
    private final Body slim;

    public PuppetRenderer(EntityRendererProvider.Context context) {
        super(context);
        wide = new Body(context, false);
        slim = new Body(context, true);
        shadowRadius = 0.5f;
    }

    private Body body(PlayerSkin skin) {
        return skin.model() == PlayerModelType.SLIM ? slim : wide;
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(PuppetEntity puppet, AvatarRenderState state, float partialTick) {
        PlayerSkin skin = PuppetSkins.skin(puppet);
        body(skin).extract(puppet, state, partialTick, skin);
        boolean possessed = puppet.isPossessed();
        state.setData(LOOK, new Look(possessed, puppet.isStandIn(), puppet.isInvisible(),
                possessed ? 0f : PuppetFx.hop(puppet, partialTick), puppet.isSpinning(), puppet.spinSeconds(partialTick),
                puppet.maliceColor(), puppet.tier().ordinal())); // a Directive's tug lifts it briefly on its strings
        if (possessed) state.nameTag = null; // its possessor is drawn as it instead: nothing of it shows, not even its shadow or name
    }

    /** The shadow is worked out after extraction (finalizeRenderState), so it is switched off here, from what extraction recorded. */
    @Override
    protected float getShadowRadius(AvatarRenderState state) {
        Look look = state.getData(LOOK);
        return look != null && look.possessed() ? 0f : super.getShadowRadius(state);
    }

    @Override
    public void submit(AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        Look look = state.getData(LOOK);
        if (look == null || look.possessed()) return;
        if (look.hop() != 0f) {
            pose.pushPose();
            pose.translate(0f, look.hop(), 0f);
        }
        body(state.skin).submit(state, pose, collector, camera);
        if (look.hop() != 0f) pose.popPose();
    }

    /** The renderer for one arm model; wide or slim is picked per puppet, as vanilla does per player. */
    static final class Body extends LivingEntityRenderer<PuppetEntity, AvatarRenderState, PlayerModel> {
        Body(EntityRendererProvider.Context context, boolean slim) {
            super(context, new PuppetModel(context.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim), 0.5f);
            addLayer(new HumanoidArmorLayer<>(this,
                    ArmorModelSet.bake(slim ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR, context.getModelSet(),
                            PuppetModel.Armor::new),
                    context.getEquipmentRenderer()));
            addLayer(new ItemInHandLayer<>(this));
            addLayer(new PuppetGlowLayer(this, context, slim));
        }

        @Override
        public AvatarRenderState createRenderState() {
            return new AvatarRenderState();
        }

        @Override
        public void extractRenderState(PuppetEntity puppet, AvatarRenderState state, float partialTick) {
            extract(puppet, state, partialTick, PuppetSkins.skin(puppet));
        }

        /** A player's render state, from a puppet: what a player's renderer reads that a puppet has too, and the whole skin showing. */
        void extract(PuppetEntity puppet, AvatarRenderState state, float partialTick, PlayerSkin skin) {
            super.extractRenderState(puppet, state, partialTick);
            HumanoidMobRenderer.extractHumanoidRenderState(puppet, state, partialTick, itemModelResolver);
            // Like a player: arms held out for whatever it holds (a Stand-In borrows its owner's items).
            state.rightArmPose = puppet.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
            state.leftArmPose = puppet.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
            state.skin = skin;
            state.id = puppet.getId();
            state.showHat = state.showJacket = state.showLeftPants = state.showRightPants = state.showLeftSleeve = state.showRightSleeve = true;
            state.showCape = false;
        }

        @Override
        public Identifier getTextureLocation(AvatarRenderState state) {
            return state.skin.body().texturePath();
        }

        @Override
        protected void setupRotations(AvatarRenderState state, PoseStack pose, float bodyRot, float entityScale) {
            super.setupRotations(state, pose, bodyRot, entityScale);
            Look look = state.getData(LOOK);
            if (look == null || !look.spinning()) return;
            PuppetPoses.idleSpin().ifPresent(clip -> {
                Vector3f body = clip.rotation("body", look.spinSeconds());
                if (body != null) pose.mulPose(Axis.YP.rotationDegrees(-body.y));
            });
        }

        @Override
        protected boolean shouldShowName(PuppetEntity puppet, double distanceSq) {
            return puppet.hasCustomName() && puppet.isCustomNameVisible() && super.shouldShowName(puppet, distanceSq);
        }
    }
}
