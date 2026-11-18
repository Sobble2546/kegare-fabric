package com.cnethuka.kegare.innate_technique.swap.client;

import com.cnethuka.kegare.Kegare;
import com.cnethuka.kegare.client.render.Avatars;
import com.cnethuka.kegare.client.visual.PlayerLayers;
import com.cnethuka.kegare.platform.client.event.EntityRenderersEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Mimic's flicker (DESIGN §3.19): the caster's body drawn again as a flat teal figure, its boxes slightly bigger all round
 * than the skin's outer layer so it shows over everything they wear. Being a layer of the player's renderer, it's posed
 * exactly as they're drawn, animation and all. Strength and timing come from {@link SwapFx#flickerAlpha}.
 */
final class MimicFlickerLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    static final ModelLayerLocation WIDE = new ModelLayerLocation(Kegare.id("mimic_flicker"), "main");
    static final ModelLayerLocation SLIM = new ModelLayerLocation(Kegare.id("mimic_flicker_slim"), "main");
    /** Out past the hat (0.5) and the jacket and sleeves (0.25): the teal sits clear of the skin rather than fighting it. */
    private static final float INFLATE = 0.85f;

    private final PlayerModel model;

    private MimicFlickerLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, PlayerModel model) {
        super(parent);
        this.model = model;
        // Parts sit at their own origin because they're drawn under the body parts' transforms.
        for (ModelPart part : new ModelPart[] {model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg}) {
            part.setPos(0f, 0f, 0f);
            part.setRotation(0f, 0f, 0f);
        }
    }

    static void registerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WIDE, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(INFLATE), false), 64, 64));
        event.registerLayerDefinition(SLIM, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(INFLATE), true), 64, 64));
    }

    static void addLayers(EntityRenderersEvent.AddLayers event) {
        PlayerLayers.addWithModels(event, (parent, models, slim) -> new MimicFlickerLayer(parent, new PlayerModel(models.bakeLayer(slim ? SLIM : WIDE), slim)));
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        AbstractClientPlayer player = Avatars.player(state);
        if (player == null) return;
        float alpha = SwapFx.flickerAlpha(player.getId());
        if (alpha <= 0f || state.isInvisible) return;
        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player && mc.options.getCameraType().isFirstPerson()) return;
        // Each teal part is drawn under the matching body part's transform as the animation left it (like the fist flames),
        // not posed again: the animation only poses the body's own model. The body model is posed now (it is, for its layers).
        PlayerModel body = getParentModel();
        ModelPart[] from = {body.head, body.body, body.rightArm, body.leftArm, body.rightLeg, body.leftLeg};
        ModelPart[] to = {model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg};
        RenderType type = RenderTypes.itemEntityTranslucentCull(SwapFx.WHITE);
        int color = Math.round(alpha * 255) << 24 | SwapFx.GHOST_RGB;
        for (int i = 0; i < from.length; i++) {
            if (!from[i].visible) continue;
            pose.pushPose();
            from[i].translateAndRotate(pose);
            collector.submitModelPart(to[i], pose, type, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null, color, null);
            pose.popPose();
        }
    }
}
