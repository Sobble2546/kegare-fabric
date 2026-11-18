package com.cnethuka.kegare.sixeyes.client;

import com.cnethuka.kegare.Kegare;
import com.cnethuka.kegare.innate_technique.rime.client.LanceMesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * Eyewear on the face, for everyone to see (DESIGN §3.32): a real 3D model ({@code models/fx/<name>.obj}), drawn under the head's
 * transform so it follows every pose, animations included. Worn by anyone, it means nothing on someone without the eyes.
 * <p>
 * It is drawn by the equipment slot's own renderer ({@code EyewearPlatform}, per Minecraft version, which knows the slot mod's API) and,
 * on a disguise, by {@link DisguisedEyewearLayer}; both call {@link #draw}.
 */
public final class EyewearRenderer {
    private final LanceMesh.Model model;
    private final String piece;
    private final Function<Identifier, RenderType> renderType;
    private final Identifier texture;

    EyewearRenderer(String name, Function<Identifier, RenderType> renderType) {
        this.texture = Kegare.id("textures/entity/" + name + ".png");
        this.model = new LanceMesh.Model(Kegare.id("models/fx/" + name + ".obj"), texture, texture);
        this.piece = name;
        this.renderType = renderType;
    }

    /** Resources reloaded: read the mesh again when it's next drawn. */
    void forget() {
        model.forget();
    }

    /** Whether this client is the wearer, who always sees their own. */
    static boolean ownView(LivingEntity wearer) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getUUID().equals(wearer.getUUID());
    }

    /** Draws this piece on a humanoid's head, following every pose. The model must be posed (it is, for layers and equipment renderers). */
    void draw(LivingEntityRenderState state, HumanoidModel<?> humanoid, PoseStack pose, SubmitNodeCollector collector, int light) {
        if (state.isInvisible || state instanceof AvatarRenderState avatar && avatar.isSpectator) return;
        LanceMesh.Piece mesh = model.piece(piece);
        if (mesh == null) return;
        int lit = LanceMesh.light(light);
        pose.pushPose();
        humanoid.head.translateAndRotate(pose);
        pose.mulPose(Axis.ZP.rotationDegrees(180f)); // into Blockbench's axes: up is up, the player's right +X
        collector.submitCustomGeometry(pose, renderType.apply(texture), (p, out) -> LanceMesh.emit(p, out, mesh, 1f, lit, false));
        pose.popPose();
    }
}
