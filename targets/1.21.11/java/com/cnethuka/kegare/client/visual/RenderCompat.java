package com.cnethuka.kegare.client.visual;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Baked-model quads, block tints, face shading and off-screen targets: small things whose spelling differs per Minecraft version.
 * This is the 1.21.11 file; targets/26.3/java has the twin.
 */
public final class RenderCompat {
    private RenderCompat() {}

    /** Every quad of a baked model, unculled and on every side (it is drawn as a mesh, whatever faces it hides). Empty while not baked. */
    public static List<BakedQuad> quads(@Nullable BlockStateModel model) {
        List<BakedQuad> out = new ArrayList<>();
        if (model == null) return out;
        for (BlockModelPart part : model.collectParts(RandomSource.create(42L))) {
            out.addAll(part.getQuads(null));
            for (Direction side : Direction.values()) out.addAll(part.getQuads(side));
        }
        return out;
    }

    /** One quad, white, no overlay, at the given light. */
    public static void put(VertexConsumer out, PoseStack.Pose pose, BakedQuad quad, int light) {
        out.putBulkData(pose, quad, 1f, 1f, 1f, 1f, light, OverlayTexture.NO_OVERLAY);
    }

    /** The atlas the quad's texture was stitched into. */
    public static Identifier atlas(BakedQuad quad) {
        return quad.sprite().atlasLocation();
    }

    public static TextureAtlasSprite particleIcon(BlockState state) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(state);
    }

    /** The block's tint at a place (grass, leaves, water), or -1 when it has none. */
    public static int blockColor(BlockState state, ClientLevel level, BlockPos pos) {
        return Minecraft.getInstance().getBlockColors().getColor(state, level, pos, 0);
    }

    /** Vanilla's fixed shade of a block face (top brightest). */
    public static float shade(ClientLevel level, Direction face) {
        return level.getShade(face, true);
    }

    /** A colour-only off-screen target. */
    public static TextureTarget target(String name, int width, int height) {
        return new TextureTarget(name, width, height, false);
    }
}
