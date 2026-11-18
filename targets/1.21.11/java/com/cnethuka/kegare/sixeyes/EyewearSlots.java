package com.cnethuka.kegare.sixeyes;

import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;

/**
 * Trinkets 3.x (Minecraft 1.21.11): the only class in the shared package that names the Trinkets API. 26.x has Trinkets 4 with another API
 * ({@code eu.pb4.trinkets}, attachments), so that target ships its own copy of this class with the same members. Only loaded when the
 * mod is installed ({@link Eyewear#slotAvailable()}).
 * <p>{@code TrinketComponent} extends a Cardinal Components interface that this build doesn't have on its compile classpath (Trinkets nests
 * that jar), so the one call on the component, {@code getInventory()}, goes through a cached {@link Method}. Adding Cardinal Components as a
 * compile-only dependency would turn it into a plain call.
 */
final class EyewearSlots {
    private EyewearSlots() {}

    /** Whether this target has the eyewear slot wired up at all (26.x does not yet). */
    static final boolean SUPPORTED = true;

    private static Method getInventory;

    /** The best eyewear in the wearer's eyes slot (it holds one item; a datapack could give it more). */
    static Eyewear worn(LivingEntity entity) {
        TrinketInventory inventory = inventory(entity);
        if (inventory == null) return Eyewear.NONE;
        Eyewear best = Eyewear.NONE;
        for (int i = 0; i < inventory.getContainerSize(); i++) best = Eyewear.better(best, Eyewear.of(inventory.getItem(i)));
        return best;
    }

    /** The eyes slot's inventory, or null when the entity has none (not a player, or the slot data isn't loaded). */
    static TrinketInventory inventory(LivingEntity entity) {
        Optional<?> component = TrinketsApi.getTrinketComponent(entity);
        if (component.isEmpty()) return null;
        try {
            if (getInventory == null) getInventory = Class.forName("dev.emi.trinkets.api.TrinketComponent").getMethod("getInventory");
            Object groups = getInventory.invoke(component.get());
            Object group = ((Map<?, ?>) groups).get(Eyewear.GROUP);
            if (group == null) return null;
            return (TrinketInventory) ((Map<?, ?>) group).get(Eyewear.SLOT);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Trinkets' TrinketComponent.getInventory() isn't there", e);
        }
    }
}
