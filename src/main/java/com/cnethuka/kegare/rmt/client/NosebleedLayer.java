package com.cnethuka.kegare.rmt.client;

import com.cnethuka.kegare.client.visual.PlayerLayers;
import com.cnethuka.kegare.platform.client.event.EntityRenderersEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * The nosebleed's line (DESIGN §3.17), drawn on the player's own model with hard edges, so it turns with the head and follows any animation:
 * both nostrils, then one thin line past the mouth, ending at the chin. Half a skin pixel wide on purpose: a wide patch at the mouth reads as a
 * blob, and a bar carried on down the chest as a tongue. The drops ({@link NosebleedClient}) take it from the chin.
 */
final class NosebleedLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier WHITE = com.cnethuka.kegare.Kegare.id("textures/misc/white.png");
    /** On the head, in skin pixels from its pivot: out past the hat (4.5), so any skin shows it. */
    private static final float FACE_OUT = 4.55f;
    /** The nostrils' row on the head, counted down from its top (8 pixels tall, its pivot at the bottom). */
    private static final int NOSE_ROW = 5;
    /** Where the line runs across the face, in skin pixels from the middle: under one nostril; the other is its mirror. */
    private static final float LINE_LEFT = 0.25f, LINE_WIDTH = 0.5f;
    /** Nose, mouth, chin: the line's rows, in the order they appear. At the last it's reached the chin, and drops start falling. */
    static final int LENGTH = 3;
    private static final int BLOOD = 0xD01018, FRESH = 0xF02028;
    private static final int MIN_BLOCK_LIGHT = 7;

    private NosebleedLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    static void addLayers(EntityRenderersEvent.AddLayers event) {
        PlayerLayers.add(event, NosebleedLayer::new);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        int[] streak = NosebleedClient.streak(state.id, mc.level.getGameTime());
        if (streak == null || streak[0] >= streak[1]) return;
        int from = streak[0], to = streak[1];
        // A tell has to show: it never goes as dark as the night round it.
        int lit = LightTexture.pack(Math.max(LightTexture.block(light), MIN_BLOCK_LIGHT), LightTexture.sky(light));

        pose.pushPose();
        getParentModel().head.translateAndRotate(pose);
        collector.submitCustomGeometry(pose, RenderTypes.entityCutoutNoCull(WHITE), (face, out) -> {
            for (int i = from; i < to; i++) {
                float y = NOSE_ROW + i - 8; // the head's top is at -8, its pivot at 0
                int rgb = i == to - 1 ? FRESH : BLOOD;
                line(out, face, lit, LINE_LEFT, y, rgb);
                if (i == 0) line(out, face, lit, -LINE_LEFT - LINE_WIDTH, y, rgb); // the other nostril
            }
        });
        pose.popPose();
    }

    /** One row of the line, facing forward (the model faces -z): {@code left} and {@code y} in skin pixels from the head's pivot. */
    private static void line(VertexConsumer out, PoseStack.Pose p, int light, float left, float y, int rgb) {
        float x0 = left / 16f, x1 = (left + LINE_WIDTH) / 16f, y0 = y / 16f, y1 = (y + 1) / 16f, z = -FACE_OUT / 16f;
        vertex(out, p, light, x0, y1, z, rgb);
        vertex(out, p, light, x1, y1, z, rgb);
        vertex(out, p, light, x1, y0, z, rgb);
        vertex(out, p, light, x0, y0, z, rgb);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose p, int light, float x, float y, float z, int rgb) {
        out.addVertex(p, x, y, z).setColor(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, 255).setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0f, 0f, -1f);
    }
}
