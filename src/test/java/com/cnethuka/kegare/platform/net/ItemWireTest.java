package com.cnethuka.kegare.platform.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cnethuka.kegare.registry.KegareItems;
import com.cnethuka.kegare.test.EphemeralTestServerProvider;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Kegare item stacks cross the network as marked paper and come back as themselves (DESIGN §3.5). */
@ExtendWith(EphemeralTestServerProvider.class)
class ItemWireTest {
    @Test
    void aKegareItemIsPaperOnTheWireAndItselfAgain() {
        ItemStack blindfold = new ItemStack(KegareItems.BLINDFOLD.get());
        blindfold.set(DataComponents.CUSTOM_NAME, Component.literal("Mine"));
        ItemStack wire = ItemWire.toWire(blindfold);
        assertTrue(wire.is(Items.PAPER), "paper on the wire");
        assertEquals(blindfold.get(DataComponents.ITEM_MODEL), wire.get(DataComponents.ITEM_MODEL), "looks like the real item");
        assertEquals(blindfold.getMaxStackSize(), wire.getMaxStackSize());
        ItemStack back = ItemWire.fromWire(wire);
        assertTrue(ItemStack.matches(blindfold, back), "the same stack again: " + back.getComponentsPatch());
        assertTrue(ItemStack.matches(new ItemStack(KegareItems.BLINDFOLD.get()), ItemWire.fromWire(ItemWire.toWire(new ItemStack(KegareItems.BLINDFOLD.get())))),
                "a plain one comes back with no extra components");
    }

    @Test
    void vanillaStacksAreLeftAlone() {
        ItemStack paper = new ItemStack(Items.PAPER, 3);
        assertSame(paper, ItemWire.toWire(paper));
        assertSame(paper, ItemWire.fromWire(paper));
    }

    @Test
    void theNetworkCodecDoesIt(MinecraftServer server) {
        ItemStack talisman = new ItemStack(KegareItems.PURIFICATION_TALISMAN.get());
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, talisman);
        int paperId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(Items.PAPER);
        RegistryFriendlyByteBuf peek = new RegistryFriendlyByteBuf(buf.copy(), server.registryAccess());
        assertEquals(1, peek.readVarInt(), "count");
        assertEquals(paperId, peek.readVarInt(), "the item id written is paper's");
        assertTrue(ItemStack.matches(talisman, ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)), "and it reads back as the talisman");
        buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
        ItemStack.STREAM_CODEC.encode(buf, talisman);
        assertTrue(ItemStack.matches(talisman, ItemStack.STREAM_CODEC.decode(buf)), "the non-optional codec too");
    }
}
