package com.cnethuka.kegare.platform.net;

import com.cnethuka.kegare.Kegare;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/**
 * How a Kegare item stack travels (DESIGN §3.5): as paper carrying every component of the real stack (its name, model, stack size...)
 * plus a marker naming the real item, turned back by whoever reads it. A protocol translator between a newer server and an older
 * client (ViaBackwards) rewrites every item stack and knows no modded item id (seen: the client failed to decode the Frost Shard's
 * item and was dropped); paper with components is vanilla to it. Used by {@code mixin.ItemStackWireMixin} on both sides.
 */
public final class ItemWire {
    private ItemWire() {}

    static final String MARKER = "kegare:item";

    public static ItemStack toWire(ItemStack stack) {
        if (stack.isEmpty()) return stack;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.getNamespace().equals(Kegare.MODID)) return stack;
        ItemStack out = new ItemStack(Items.PAPER, stack.getCount());
        retype(stack, out, Items.PAPER);
        CustomData.update(DataComponents.CUSTOM_DATA, out, tag -> tag.putString(MARKER, id.toString()));
        return out;
    }

    public static ItemStack fromWire(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(Items.PAPER)) return stack;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return stack;
        Identifier id = Identifier.tryParse(data.copyTag().getStringOr(MARKER, ""));
        if (id == null || !id.getNamespace().equals(Kegare.MODID)) return stack;
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) return stack; // an item this side doesn't have: it stays the paper it arrived as
        ItemStack out = new ItemStack(item, stack.getCount());
        retype(stack, out, item);
        CustomData.update(DataComponents.CUSTOM_DATA, out, tag -> tag.remove(MARKER));
        CustomData left = out.get(DataComponents.CUSTOM_DATA);
        if (left != null && left.isEmpty()) out.remove(DataComponents.CUSTOM_DATA);
        return out;
    }

    /** Gives {@code to} (a fresh stack of {@code item}) exactly the components {@code from} has. */
    private static void retype(ItemStack from, ItemStack to, Item item) {
        DataComponentMap components = from.getComponents();
        to.applyComponents(components);
        for (DataComponentType<?> type : item.components().keySet()) {
            if (!components.has(type)) to.remove(type);
        }
    }
}
