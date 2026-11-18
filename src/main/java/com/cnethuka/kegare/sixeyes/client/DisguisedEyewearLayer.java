package com.cnethuka.kegare.sixeyes.client;

import com.cnethuka.kegare.client.render.Avatars;
import com.cnethuka.kegare.innate_technique.swap.Disguises;
import com.cnethuka.kegare.sixeyes.Eyewear;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * The eyewear on a disguise (DESIGN §3.19, §3.32): to everyone but the wearer, a disguised player wears what the face they've borrowed is wearing
 * right now (the server sends it), or nothing if the face wears nothing. The wearer's own eyewear is hidden on it ({@link EyewearRenderer}), so
 * copying someone without a blindfold never shows a blindfold. It ends with the disguise. A Six Eyes bearer sees it too: they know who it is by
 * the energy (name, color, technique, the Read), and the face, its gear and its eyewear are what the face looks like.
 */
final class DisguisedEyewearLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    DisguisedEyewearLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        AbstractClientPlayer player = Avatars.player(state);
        if (player == null) return;
        Eyewear shown = Disguises.eyewearShown(Disguises.clientEntry(player.getUUID()), player.level().getGameTime(), EyewearRenderer.ownView(player));
        EyewearRenderer renderer = shown == null ? null : EyewearClient.renderer(shown);
        if (renderer != null) renderer.draw(state, getParentModel(), pose, collector, light);
    }
}
