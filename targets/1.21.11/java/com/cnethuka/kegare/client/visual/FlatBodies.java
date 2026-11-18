package com.cnethuka.kegare.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * An entity drawn again where it isn't, every part of it in one flat colour (Time Manipulation's afterimages, Blur's smear, a Severed
 * player's sensed silhouette). 1.21.11 has no "render this entity into that buffer source": an entity is extracted to a render state and
 * submitted to a {@link SubmitNodeCollector}. This is a collector that draws what is submitted to it at once, through one
 * {@link Flat} consumer, and leaves out everything that isn't the body: its shadow, name tag, fire, leash, glint, outline and lines.
 * Because the name tag is never submitted, a copy has none (the old {@code BodyCopies} job).
 */
public final class FlatBodies {
    private FlatBodies() {}

    /** How a copy is coloured. {@code textured} is false for a position+colour target (the sensing mask), which gets no UV or light. */
    public record Look(int rgb, int alpha, int light, boolean textured) {}

    /**
     * Draws {@code entity} at {@code at} (its feet, from the camera) into {@code out}, which must take quads of the entity vertex format
     * when {@code look.textured()} and of position+colour otherwise. Nothing is drawn for a renderer that throws.
     */
    public static void draw(Entity entity, Vec3 at, float partial, VertexConsumer out, Look look) {
        Minecraft mc = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        try {
            Holder<EntityRenderState> state = new Holder<>();
            BodyCopies.draw(() -> state.value = dispatcher.extractEntity(entity, partial));
            if (state.value == null) return;
            Collector collector = new Collector(new Flat(out, look));
            dispatcher.submit(state.value, cameraState(mc.gameRenderer.getMainCamera()), at.x, at.y, at.z, new PoseStack(), collector);
        } catch (RuntimeException ignored) {
            // An odd renderer: no copy of it, rather than a crash.
        }
    }

    private static final class Holder<T> {
        T value;
    }

    /** What a renderer reads of the camera while it submits. */
    private static CameraRenderState cameraState(Camera camera) {
        CameraRenderState s = new CameraRenderState();
        s.pos = camera.position();
        s.blockPos = camera.blockPosition();
        s.entityPos = camera.entity() != null ? camera.entity().position() : camera.position();
        s.orientation = new Quaternionf(camera.rotation());
        s.initialized = true;
        return s;
    }

    /** Writes each vertex whole, in one colour, the moment it's started; whatever the renderer sets after is ignored. */
    private static final class Flat implements VertexConsumer {
        private final VertexConsumer out;
        private final int r, g, b, a, light;
        private final boolean textured;

        Flat(VertexConsumer out, Look look) {
            this.out = out;
            this.r = look.rgb() >> 16 & 0xFF;
            this.g = look.rgb() >> 8 & 0xFF;
            this.b = look.rgb() & 0xFF;
            this.a = Math.max(0, Math.min(255, look.alpha()));
            this.light = look.light();
            this.textured = look.textured();
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            VertexConsumer v = out.addVertex(x, y, z).setColor(r, g, b, a);
            if (textured) v.setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0f, 1f, 0f);
            return this;
        }

        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setColor(int argb) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
        @Override public VertexConsumer setLineWidth(float width) { return this; }
    }

    /** Draws what's submitted straight through {@link Flat}; the body's quads only. */
    private static final class Collector implements SubmitNodeCollector {
        private final Flat flat;

        Collector(Flat flat) {
            this.flat = flat;
        }

        /** The body's own quads go in; its name, glint, lines, shadow and outline don't. */
        private static boolean drawn(RenderType type) {
            if (type.mode() != VertexFormat.Mode.QUADS || type.isOutline()) return false;
            String name = type.pipeline().getLocation().getPath();
            return !name.contains("text") && !name.contains("glint") && !name.contains("shadow") && !name.contains("outline")
                    && !name.contains("leash") && !name.contains("lines");
        }

        @Override
        public net.minecraft.client.renderer.OrderedSubmitNodeCollector order(int order) {
            return this;
        }

        @Override
        public <S> void submitModel(Model<? super S> model, S state, PoseStack pose, RenderType type, int light, int overlay, int tint,
                                    TextureAtlasSprite sprite, int outline, ModelFeatureRenderer.CrumblingOverlay crumbling) {
            if (!drawn(type)) return;
            model.setupAnim(state);
            model.renderToBuffer(pose, flat, light, OverlayTexture.NO_OVERLAY, -1);
        }

        @Override
        public void submitModelPart(ModelPart part, PoseStack pose, RenderType type, int light, int overlay, TextureAtlasSprite sprite,
                                    boolean sheeted, boolean foil, int tint, ModelFeatureRenderer.CrumblingOverlay crumbling, int outline) {
            if (!drawn(type)) return;
            part.render(pose, flat, light, OverlayTexture.NO_OVERLAY, -1);
        }

        @Override
        public void submitItem(PoseStack pose, ItemDisplayContext context, int light, int overlay, int outline, int[] tints,
                               List<BakedQuad> quads, RenderType type, ItemStackRenderState.FoilType foil) {
            if (!drawn(type)) return;
            for (BakedQuad quad : quads) flat.putBulkData(pose.last(), quad, 1f, 1f, 1f, 1f, light, OverlayTexture.NO_OVERLAY);
        }

        @Override
        public void submitCustomGeometry(PoseStack pose, RenderType type, CustomGeometryRenderer renderer) {
            if (!drawn(type)) return;
            renderer.render(pose.last(), flat);
        }

        @Override public void submitShadow(PoseStack pose, float radius, List<EntityRenderState.ShadowPiece> pieces) {}
        @Override public void submitNameTag(PoseStack pose, Vec3 offset, int y, Component text, boolean seeThrough, int light, double distanceSq,
                                            CameraRenderState camera) {}
        @Override public void submitText(PoseStack pose, float x, float y, FormattedCharSequence text, boolean dropShadow, Font.DisplayMode mode,
                                         int light, int color, int background, int outline) {}
        @Override public void submitFlame(PoseStack pose, EntityRenderState state, Quaternionf rotation) {}
        @Override public void submitLeash(PoseStack pose, EntityRenderState.LeashState leash) {}
        @Override public void submitBlock(PoseStack pose, BlockState block, int light, int overlay, int outline) {}
        @Override public void submitMovingBlock(PoseStack pose, MovingBlockRenderState block) {}
        @Override public void submitBlockModel(PoseStack pose, RenderType type, BlockStateModel model, float r, float g, float b, int light,
                                               int overlay, int outline) {}
        @Override public void submitParticleGroup(ParticleGroupRenderer renderer) {}
    }
}
