package com.cnethuka.kegare.innate_technique.puppet_manipulation.client;

import com.cnethuka.kegare.Kegare;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * The Malice a puppet was charged with: a slowly drifting shell of threads just outside its body (additive, like a
 * charged creeper's), in its owner's Malice color. Faint for a tap, normal for half, bright for full (a full puppet
 * also gets an outline, see {@code PuppetClient}).
 */
public class PuppetGlowLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static final ModelLayerLocation WIDE = new ModelLayerLocation(Kegare.id("puppet_glow"), "main");
    public static final ModelLayerLocation SLIM = new ModelLayerLocation(Kegare.id("puppet_glow_slim"), "main");
    static final Identifier TEXTURE = Kegare.id("textures/entity/puppet_glow.png");
    /** Glow strength per tier (tap, half, full). */
    private static final float[] INTENSITY = {0.22f, 0.45f, 0.85f};

    private final PuppetModel model;

    public PuppetGlowLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityRendererProvider.Context context, boolean slim) {
        super(parent);
        model = new PuppetModel(context.bakeLayer(slim ? SLIM : WIDE), slim);
    }

    public static LayerDefinition definition(boolean slim) {
        return LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(0.3f), slim), 64, 64);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        PuppetRenderer.Look look = state.getData(PuppetRenderer.LOOK);
        if (look == null || look.invisible() || look.standIn()) return; // a Stand-In looks exactly like its owner
        float t = state.ageInTicks;
        // The shell is posed when drawn, from the same state as the body, so it moves exactly with it. The 8-argument submitModel's int is the
        // OUTLINE colour (its tint stays white), so the tint goes through the full form: tint, no sprite, no outline, no crumbling.
        collector.submitModel(model, state, pose, RenderTypes.energySwirl(TEXTURE, (t * 0.004f) % 1f, (t * 0.007f) % 1f), light,
                OverlayTexture.NO_OVERLAY, color(look.maliceColor(), look.tier()), null, 0, null);
    }

    /** The glow's ARGB for a Malice color at a tier (additive, so brightness is strength). */
    static int color(int rgb, int tier) {
        float k = INTENSITY[Math.max(0, Math.min(INTENSITY.length - 1, tier))];
        int r = Math.round(((rgb >> 16) & 0xFF) * k), g = Math.round(((rgb >> 8) & 0xFF) * k), b = Math.round((rgb & 0xFF) * k);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
