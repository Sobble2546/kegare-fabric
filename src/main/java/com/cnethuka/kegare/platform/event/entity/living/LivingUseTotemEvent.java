package com.cnethuka.kegare.platform.event.entity.living;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** A totem of undying has saved someone. (Not cancellable here: by the time it is seen the death has been undone.) */
public final class LivingUseTotemEvent extends LivingEvent {
    private final DamageSource source;
    private final ItemStack totem;
    private final InteractionHand hand;

    public LivingUseTotemEvent(LivingEntity entity, DamageSource source, ItemStack totem, InteractionHand hand) {
        super(entity);
        this.source = source;
        this.totem = totem;
        this.hand = hand;
    }

    public DamageSource getSource() {
        return source;
    }

    public ItemStack getTotem() {
        return totem;
    }

    public InteractionHand getHandHoldingTotem() {
        return hand;
    }
}
