package com.cnethuka.kegare.sixeyes.client;

import com.cnethuka.kegare.innate_technique.swap.Disguises;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.client.TrinketRenderer;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Where eyewear meets the equipment slot mod: Trinkets 3.x on 1.21.11 (package {@code dev.emi.trinkets}). This is the 1.21.11 file
 * (targets/1.21.11/java); Trinkets 4.x on 26.x lives in {@code eu.pb4.trinkets} with a different renderer interface, so 26.x needs a twin of
 * this class with the same {@link #register} member.
 */
final class EyewearPlatform {
    private EyewearPlatform() {}

    static void register(Item item, EyewearRenderer renderer) {
        TrinketRendererRegistry.registerRenderer(item, new Worn(renderer));
    }

    /** Eyewear in its slot, on whoever wears it. */
    private record Worn(EyewearRenderer renderer) implements TrinketRenderer {
        @Override
        public void render(ItemStack stack, SlotReference slot, EntityModel<? extends LivingEntityRenderState> model, PoseStack pose,
                           SubmitNodeCollector collector, int light, LivingEntityRenderState state, float limbAngle, float limbDistance) {
            // The slot's owner is a player (the eyes slot is theirs alone); the state names them (Trinkets' component, which would say, needs
            // Cardinal Components at compile time).
            Minecraft mc = Minecraft.getInstance();
            LivingEntity wearer = state instanceof AvatarRenderState avatar && mc.level != null && mc.level.getEntity(avatar.id) instanceof Player p ? p : null;
            // On a disguise everyone else sees the face's eyewear, never the wearer's own: DisguisedEyewearLayer draws that instead.
            if (wearer != null && Disguises.eyewearShown(Disguises.clientEntry(wearer.getUUID()), wearer.level().getGameTime(),
                    EyewearRenderer.ownView(wearer)) != null) return;
            if (model instanceof HumanoidModel<?> humanoid) renderer.draw(state, humanoid, pose, collector, light);
        }
    }
}
