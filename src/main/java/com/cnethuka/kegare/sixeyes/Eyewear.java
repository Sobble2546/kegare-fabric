package com.cnethuka.kegare.sixeyes;

import com.cnethuka.kegare.registry.KegareItems;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * What covers the eyes (DESIGN §3.32): a blindfold or sunglasses in Trinkets' "eyes" slot (group {@code head}). Anyone can wear them and they
 * mean nothing on someone without the eyes, so wearing one proves nothing. On a bearer they change only how fast Strain builds and falls, and
 * whether the uncovered edge is there: never what the eyes see, nor how little their energy wastes.
 * <p>Trinkets is optional: without it nobody can wear eyewear (the items still exist and can be held), and {@link #of(LivingEntity)} is
 * {@link #NONE}. The Trinkets API is only ever touched by {@code EyewearSlots}, which this loads only when the mod is there; its spelling
 * differs per Minecraft target.
 */
public enum Eyewear {
    NONE, SUNGLASSES, BLINDFOLD;

    /** The slot's id in its group, and its group: Trinkets calls the slot {@code head/eyes}. */
    public static final String SLOT = "eyes";
    public static final String GROUP = "head";
    public static final String SLOT_ID = GROUP + "/" + SLOT;

    private static final boolean TRINKETS = FabricLoader.getInstance().isModLoaded("trinkets");

    public boolean covered() {
        return this != NONE;
    }

    /** Whether the eyewear slot exists at all in this game (Trinkets is installed). */
    public static boolean slotAvailable() {
        return TRINKETS;
    }

    public static Eyewear of(LivingEntity entity) {
        return TRINKETS ? EyewearSlots.worn(entity) : NONE;
    }

    public static Eyewear of(ItemStack stack) {
        if (stack.is(KegareItems.BLINDFOLD.get())) return BLINDFOLD;
        if (stack.is(KegareItems.SUNGLASSES.get())) return SUNGLASSES;
        return NONE;
    }

    /** The better of two (a blindfold covers more than sunglasses). */
    static Eyewear better(Eyewear a, Eyewear b) {
        return b.ordinal() > a.ordinal() ? b : a;
    }
}
