package com.cnethuka.kegare.platform.client.event;

import com.cnethuka.kegare.platform.bus.Event;
import com.cnethuka.kegare.platform.bus.ICancellableEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * One first-person hand (with its item) is about to be drawn. Cancel to draw nothing for it. Drawing is submit-based on
 * 1.21.11: what NeoForge called the multi-buffer source is a {@link SubmitNodeCollector} here, so emit geometry with
 * {@code submitCustomGeometry} instead of {@code getBuffer}.
 */
public final class RenderHandEvent extends Event implements ICancellableEvent {
    private final InteractionHand hand;
    private final PoseStack poseStack;
    private final SubmitNodeCollector collector;
    private final int packedLight;
    private final float partialTick;
    private final float interpolatedPitch;
    private final float swingProgress;
    private final float equipProgress;
    private final ItemStack itemStack;

    public RenderHandEvent(InteractionHand hand, PoseStack poseStack, SubmitNodeCollector collector, int packedLight, float partialTick,
                           float interpolatedPitch, float swingProgress, float equipProgress, ItemStack itemStack) {
        this.hand = hand;
        this.poseStack = poseStack;
        this.collector = collector;
        this.packedLight = packedLight;
        this.partialTick = partialTick;
        this.interpolatedPitch = interpolatedPitch;
        this.swingProgress = swingProgress;
        this.equipProgress = equipProgress;
        this.itemStack = itemStack;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    /** Where the hand's geometry goes (NeoForge's accessor name, 1.21.11's type). */
    public SubmitNodeCollector getMultiBufferSource() {
        return collector;
    }

    public SubmitNodeCollector getSubmitNodeCollector() {
        return collector;
    }

    public int getPackedLight() {
        return packedLight;
    }

    public float getPartialTick() {
        return partialTick;
    }

    public float getInterpolatedPitch() {
        return interpolatedPitch;
    }

    public float getSwingProgress() {
        return swingProgress;
    }

    public float getEquipProgress() {
        return equipProgress;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
