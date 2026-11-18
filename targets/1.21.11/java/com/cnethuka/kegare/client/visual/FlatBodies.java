package com.cnethuka.kegare.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
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

    /**
     * How a copy is coloured. {@code textured} is false for a position+colour target (the sensing mask), which gets no UV or light.
     * {@code forceVisible} draws a body that is invisible as if it weren't (sensed, not seen), by clearing the flag on the extracted state
     * for the draw: the entity itself is never touched.
     */
    public record Look(int rgb, int alpha, int light, boolean textured, boolean forceVisible) {
        public Look(int rgb, int alpha, int light, boolean textured) {
            this(rgb, alpha, light, textured, false);
        }

        public Look visible() {
            return new Look(rgb, alpha, light, textured, true);
        }
    }

    /**
     * Draws {@code entity} at {@code at} (its feet, from the camera) into {@code out}, which must take quads of the entity vertex format
     * when {@code look.textured()} and of position+colour otherwise. Nothing is drawn for a renderer that throws. The whole of it runs as
     * a copy ({@link BodyCopies}): the entity's render state is extracted once per entity per frame and shared by every copy of it (the
     * ten afterimages of one body cost one extraction, not ten).
     */
    public static void draw(Entity entity, Vec3 at, float partial, VertexConsumer out, Look look) {
        Minecraft mc = Minecraft.getInstance();
        try {
            BodyCopies.draw(() -> submit(mc, entity, at, partial, out, look));
        } catch (RuntimeException ignored) {
            // An odd renderer: no copy of it, rather than a crash.
        }
    }

    private static void submit(Minecraft mc, Entity entity, Vec3 at, float partial, VertexConsumer out, Look look) {
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        sync(mc, partial);
        EntityRenderState state = extracted(dispatcher, entity, partial);
        if (state == null) return;
        boolean invisible = state.isInvisible;
        boolean hidden = state instanceof LivingEntityRenderState living && living.isInvisibleToPlayer;
        if (look.forceVisible()) {
            state.isInvisible = false;
            if (state instanceof LivingEntityRenderState living) living.isInvisibleToPlayer = false;
        }
        try {
            dispatcher.submit(state, camera, at.x, at.y, at.z, new PoseStack(), new Collector(new Flat(out, look)));
        } finally {
            state.isInvisible = invisible; // the state is shared with the other copies of this body in the frame
            if (state instanceof LivingEntityRenderState living) living.isInvisibleToPlayer = hidden;
        }
    }

    // What one frame's copies share: the render state of each entity drawn, and what a renderer reads of the camera.
    private static final Map<Integer, EntityRenderState> STATES = new HashMap<>();
    private static CameraRenderState camera = new CameraRenderState();
    private static ClientLevel frameLevel;
    private static long frameTime = Long.MIN_VALUE;
    private static float framePartial = Float.NaN;
    private static Vec3 framePos;
    private static final Quaternionf FRAME_ROT = new Quaternionf();

    /** Starts a new frame's cache when the level, the tick, the partial tick or the camera is not what the last copy was drawn for. */
    private static void sync(Minecraft mc, float partial) {
        ClientLevel level = mc.level;
        Camera main = mc.gameRenderer.getMainCamera();
        long time = level == null ? Long.MIN_VALUE : level.getGameTime();
        if (level == frameLevel && time == frameTime && partial == framePartial && main.position().equals(framePos)
                && FRAME_ROT.equals(main.rotation())) return;
        frameLevel = level;
        frameTime = time;
        framePartial = partial;
        framePos = main.position();
        FRAME_ROT.set(main.rotation());
        STATES.clear();
        camera = cameraState(main);
    }

    /** Drops what is cached (a world is left): nothing in it may outlive the level. */
    public static void clear() {
        STATES.clear();
        frameLevel = null;
        frameTime = Long.MIN_VALUE;
        framePos = null;
    }

    @Nullable
    private static EntityRenderState extracted(EntityRenderDispatcher dispatcher, Entity entity, float partial) {
        if (STATES.containsKey(entity.getId())) return STATES.get(entity.getId());
        EntityRenderState state = null;
        try {
            state = dispatcher.extractEntity(entity, partial);
        } catch (RuntimeException odd) {
            // Remembered as no state: the rest of the frame's copies of it don't try again.
        }
        STATES.put(entity.getId(), state);
        return state;
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

    /** Pipelines of what an entity submits that is not its body (matched by pipeline, not by name). */
    private static final Set<RenderPipeline> NOT_THE_BODY = new HashSet<>(List.of(
            RenderPipelines.ENTITY_SHADOW, RenderPipelines.GLINT, RenderPipelines.LEASH, RenderPipelines.CRUMBLING,
            RenderPipelines.LINES, RenderPipelines.LINES_TRANSLUCENT,
            RenderPipelines.TEXT, RenderPipelines.GUI_TEXT, RenderPipelines.TEXT_BACKGROUND, RenderPipelines.TEXT_INTENSITY,
            RenderPipelines.GUI_TEXT_INTENSITY, RenderPipelines.TEXT_POLYGON_OFFSET, RenderPipelines.TEXT_SEE_THROUGH,
            RenderPipelines.TEXT_BACKGROUND_SEE_THROUGH, RenderPipelines.TEXT_INTENSITY_SEE_THROUGH,
            RenderPipelines.ENERGY_SWIRL, RenderPipelines.BREEZE_WIND)); // the swirl and the wind are bigger than the body

    /** Draws what's submitted straight through {@link Flat}; the body's quads only. */
    private static final class Collector implements SubmitNodeCollector {
        private final Flat flat;

        Collector(Flat flat) {
            this.flat = flat;
        }

        /** The body's own quads go in; its name, glint, lines, shadow, leash and outline don't, nor the armour layers that swell it. */
        private static boolean drawn(RenderType type) {
            return type.mode() == VertexFormat.Mode.QUADS && !type.isOutline() && !NOT_THE_BODY.contains(type.pipeline());
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
