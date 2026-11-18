package com.cnethuka.kegare.client.visual;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.GpuFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
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
 * This is the 26.x file (compiled, UNVERIFIED in game).
 */
public final class RenderCompat {
    private RenderCompat() {}

    public static List<BakedQuad> quads(@Nullable BlockStateModel model) {
        List<BakedQuad> out = new ArrayList<>();
        if (model == null) return out;
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(42L), parts);
        for (BlockStateModelPart part : parts) {
            out.addAll(part.getQuads(null));
            for (Direction side : Direction.values()) out.addAll(part.getQuads(side));
        }
        return out;
    }

    public static void put(VertexConsumer out, PoseStack.Pose pose, BakedQuad quad, int light) {
        QuadInstance instance = new QuadInstance();
        instance.setColor(-1);
        instance.setLightCoords(light);
        instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);
        out.putBakedQuad(pose, quad, instance);
    }

    public static Identifier atlas(BakedQuad quad) {
        return quad.materialInfo().sprite().atlasLocation();
    }

    public static TextureAtlasSprite particleIcon(BlockState state) {
        return Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state).particleMaterial().sprite();
    }

    public static int blockColor(BlockState state, ClientLevel level, BlockPos pos) {
        BlockTintSource source = Minecraft.getInstance().getBlockColors().getTintSource(state, 0);
        return source == null ? -1 : source.colorInWorld(state, level, pos);
    }

    public static float shade(ClientLevel level, Direction face) {
        return level.cardinalLighting().byFace(face);
    }

    public static TextureTarget target(String name, int width, int height) {
        return new TextureTarget(name, width, height, GpuFormat.RGBA8_UNORM, null);
    }
}
