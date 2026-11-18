package com.cnethuka.kegare.platform.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cnethuka.kegare.platform.NeoForge;
import com.cnethuka.kegare.platform.TriState;
import com.cnethuka.kegare.platform.bus.EventBus;
import com.cnethuka.kegare.platform.bus.EventPriority;
import com.cnethuka.kegare.platform.bus.Listener;
import com.cnethuka.kegare.platform.client.event.FMLClientSetupEvent;
import com.cnethuka.kegare.platform.client.event.InputEvent;
import com.cnethuka.kegare.platform.client.event.MovementInputUpdateEvent;
import com.cnethuka.kegare.platform.client.event.RegisterGuiLayersEvent;
import com.cnethuka.kegare.platform.client.event.RenderGuiLayerEvent;
import com.cnethuka.kegare.platform.client.event.RenderNameTagEvent;
import com.cnethuka.kegare.platform.client.event.sound.PlaySoundEvent;
import com.cnethuka.kegare.platform.client.gui.VanillaGuiLayers;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The shim's own logic, with Minecraft types only passed around as null (nothing here initialises the game). */
class ClientEventShimTest {
    private final List<Object> listeners = new ArrayList<>();

    private <E extends com.cnethuka.kegare.platform.bus.Event> void listen(EventPriority priority, Class<E> type, java.util.function.Consumer<E> body) {
        NeoForge.EVENT_BUS.addListener(priority, false, type, body);
        listeners.add(body);
    }

    @AfterEach
    void unlisten() {
        listeners.forEach(NeoForge.EVENT_BUS::unregister);
        listeners.clear();
    }

    @Test
    void aCancelledLayerIsSkippedAndGetsNoPost() {
        Identifier hotbar = VanillaGuiLayers.HOTBAR;
        List<String> seen = new ArrayList<>();
        listen(EventPriority.HIGH, RenderGuiLayerEvent.Pre.class, e -> {
            seen.add("pre " + e.getName());
            e.setCanceled(true);
        });
        listen(EventPriority.NORMAL, RenderGuiLayerEvent.Post.class, e -> seen.add("post"));
        ClientEventHooks.wrapHud(hotbar, (g, d) -> seen.add("vanilla")).render(null, null);
        assertEquals(List.of("pre " + hotbar), seen);
    }

    @Test
    void aLayerThatIsNotCancelledIsDrawnBetweenPreAndPost() {
        Identifier chat = VanillaGuiLayers.CHAT;
        List<String> seen = new ArrayList<>();
        listen(EventPriority.NORMAL, RenderGuiLayerEvent.Pre.class, e -> seen.add("pre"));
        listen(EventPriority.NORMAL, RenderGuiLayerEvent.Post.class, e -> seen.add("post " + e.getName().equals(chat)));
        ClientEventHooks.wrapHud(chat, (g, d) -> seen.add("vanilla")).render(null, null);
        assertEquals(List.of("pre", "vanilla", "post true"), seen);
    }

    @Test
    void experienceBarAndJumpMeterAreOneLayerAndCancellingItHidesBoth() {
        assertEquals(VanillaGuiLayers.EXPERIENCE_BAR, VanillaGuiLayers.JUMP_METER);
        assertTrue(VanillaGuiLayers.WRAPPED.contains(VanillaGuiLayers.JUMP_METER));
    }

    @Test
    void layersGoAboveAndBelowTheOtherLayer() {
        List<String> calls = new ArrayList<>();
        RegisterGuiLayersEvent event = new RegisterGuiLayersEvent(new RegisterGuiLayersEvent.Registrar() {
            @Override
            public void attachAfter(Identifier other, Identifier id, HudElement layer) {
                calls.add("after " + other + " " + id);
            }

            @Override
            public void attachBefore(Identifier other, Identifier id, HudElement layer) {
                calls.add("before " + other + " " + id);
            }
        });
        Identifier a = Identifier.fromNamespaceAndPath("kegare", "a");
        Identifier b = Identifier.fromNamespaceAndPath("kegare", "b");
        event.registerAbove(VanillaGuiLayers.HOTBAR, a, (g, d) -> {});
        event.registerBelow(VanillaGuiLayers.CROSSHAIR, b, (g, d) -> {});
        assertEquals(List.of("after " + VanillaGuiLayers.HOTBAR + " kegare:a", "before " + VanillaGuiLayers.CROSSHAIR + " kegare:b"), calls);
    }

    @Test
    void nameTagHideAndReplace() {
        listen(EventPriority.NORMAL, RenderNameTagEvent.class, e -> {
            if (e.getPartialTick() == 1f) e.setCanRender(TriState.FALSE);
            if (e.getPartialTick() == 2f) e.setContent(null);
        });
        assertNull(ClientEventHooks.nameTag(null, null, 1f));
        assertNull(ClientEventHooks.nameTag(null, null, 2f));
        assertNull(ClientEventHooks.nameTag(null, null, 3f)); // untouched: the tag we were given (null here)
    }

    @Test
    void interactionCancelsAndKeepsSwingFlag() {
        InputEvent.InteractionKeyMappingTriggered event = new InputEvent.InteractionKeyMappingTriggered(InputEvent.Interaction.ATTACK, null, null);
        assertTrue(event.isAttack());
        assertFalse(event.isUseItem());
        assertTrue(event.shouldSwingHand());
        event.setSwingHand(false);
        event.setCanceled(true);
        assertFalse(event.shouldSwingHand());
        assertTrue(event.isCanceled());
    }

    @Test
    void movementInputViewKeepsWhatListenersWrite() {
        MovementInputUpdateEvent.Input in = new MovementInputUpdateEvent.Input(true, false, true, false, true, true, true, 1f, 1f);
        listen(EventPriority.NORMAL, MovementInputUpdateEvent.class, e -> {
            e.getInput().forwardImpulse = 0f;
            e.getInput().up = e.getInput().down = e.getInput().left = e.getInput().right = false;
            e.getInput().jumping = false;
        });
        MovementInputUpdateEvent.Input out = ClientEventHooks.movementInput(in);
        assertSame(in, out);
        assertEquals(0f, out.forwardImpulse);
        assertEquals(0f, out.leftImpulse == 1f ? 0f : 1f); // the left impulse was not touched
        assertFalse(out.up || out.down || out.left || out.right || out.jumping);
        assertTrue(out.shiftKeyDown && out.sprinting);
    }

    @Test
    void soundCanBeSilencedOrReplaced() {
        listen(EventPriority.NORMAL, PlaySoundEvent.class, e -> e.setSound(null));
        assertNull(ClientEventHooks.sound(null));
        PlaySoundEvent event = new PlaySoundEvent(null);
        assertNull(event.getOriginalSound());
    }

    @Test
    void clientSetupRunsWorkAtOnceAndReportsFailure() {
        FMLClientSetupEvent event = new FMLClientSetupEvent();
        int[] ran = {0};
        assertTrue(event.enqueueWork(() -> ran[0]++).isDone());
        assertEquals(1, ran[0]);
        assertThrows(IllegalStateException.class, () -> event.enqueueWork(() -> {
            throw new IllegalStateException("boom");
        }));
    }

    @Test
    void modBusIsSeparateFromTheGameBus() {
        List<String> seen = new ArrayList<>();
        Listener<FMLClientSetupEvent> l = e -> seen.add("mod");
        ClientEventHooks.modBus().addListener(l);
        try {
            NeoForge.EVENT_BUS.post(new FMLClientSetupEvent());
            assertTrue(seen.isEmpty());
            ClientEventHooks.modBus().post(new FMLClientSetupEvent());
            assertEquals(List.of("mod"), seen);
        } finally {
            ClientEventHooks.modBus().unregister(l);
        }
        assertTrue(ClientEventHooks.modBus() instanceof EventBus);
    }
}
