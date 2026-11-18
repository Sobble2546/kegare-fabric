package com.cnethuka.kegare.client;

import com.cnethuka.kegare.Kegare;
import com.cnethuka.kegare.client.gui.GuiDraw;
import com.cnethuka.kegare.client.visual.VisualClientEvents;
import com.cnethuka.kegare.client.gui.RevealQueue;
import com.cnethuka.kegare.platform.NeoForge;
import com.cnethuka.kegare.platform.bus.IEventBus;
import com.cnethuka.kegare.platform.client.ClientEventHooks;
import com.cnethuka.kegare.platform.client.ClientPlatform;
import com.cnethuka.kegare.platform.client.event.RegisterClientReloadListenersEvent;
import com.cnethuka.kegare.platform.client.event.RegisterGuiLayersEvent;
import com.cnethuka.kegare.platform.client.gui.VanillaGuiLayers;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Client-only entry point (fabric.mod.json "client" entrypoint). Never loaded on a dedicated server.
 * <p>
 * Runs very early in Minecraft's startup on Fabric, before options, window and resource manager exist: nothing below may read
 * {@code Minecraft.options}, the window or a level eagerly; everything that needs them is inside listeners.
 */
public final class KegareClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientEventHooks.init();
        init(ClientEventHooks.modBus());
        ClientEventHooks.fireRegistrationEvents();
    }

    static void init(IEventBus modBus) {
        // Mods list > Kegare > Config edits both client and (in singleplayer) server configs (needs Mod Menu to be seen).
        ClientPlatform.registerConfigScreen();
        // The @EventBusSubscriber classes of the client: Fabric has no annotation scanning. Each is registered exactly once, here;
        // their static @SubscribeEvent methods take game-bus events only (none takes a mod-bus event).
        NeoForge.EVENT_BUS.register(ClientInput.class);
        NeoForge.EVENT_BUS.register(HudClearance.class);
        NeoForge.EVENT_BUS.register(RevealQueue.class);
        NeoForge.EVENT_BUS.register(VisualClientEvents.class);
        modBus.addListener(KegareKeys::register);
        modBus.addListener(KegareClient::registerGuiLayers);
        modBus.addListener(KegareClient::registerReloadListeners);
        VisualClientEvents.init(modBus);
        com.cnethuka.kegare.client.visual.BodyCopies.init(); // afterimages and sensed outlines carry no name tag
        com.cnethuka.kegare.innate_technique.rime.client.RimeClient.init(modBus);
        com.cnethuka.kegare.melee.client.MeleeClient.init();
        com.cnethuka.kegare.melee.client.FistTell.init(modBus);
        com.cnethuka.kegare.reinforcement.client.AuraClient.init(modBus);
        com.cnethuka.kegare.backgroundtask.client.BackgroundClient.init();
        com.cnethuka.kegare.timing.client.ClientStamps.init(); // sub-tick timing (DESIGN §3.29)
        com.cnethuka.kegare.fists.client.BlackSparkClient.init(); // Black Spark's burst (DESIGN §3.28)
        com.cnethuka.kegare.fists.client.ZoneHud.init(); // the zone's "120%" under the crosshair
        com.cnethuka.kegare.melee.client.DashFeel.init(modBus);
        com.cnethuka.kegare.rmt.client.RmtClient.init();
        com.cnethuka.kegare.rmt.client.NosebleedClient.init(modBus); // a repair's tell (DESIGN §3.17)
        com.cnethuka.kegare.sixeyes.client.SightClient.init(); // Six Eyes (DESIGN §3.32)
        com.cnethuka.kegare.sixeyes.client.SightRender.init();
        com.cnethuka.kegare.sixeyes.client.SightHud.init(modBus);
        com.cnethuka.kegare.sixeyes.client.ReadsClient.init(modBus);
        com.cnethuka.kegare.sixeyes.client.StrainClient.init(modBus);
        com.cnethuka.kegare.sixeyes.client.EyewearClient.init(modBus);
        com.cnethuka.kegare.innate_technique.swap.client.SwapClient.init(modBus);
        com.cnethuka.kegare.innate_technique.puppet_manipulation.client.PuppetClient.init(modBus);
        com.cnethuka.kegare.innate_technique.time_manipulation.client.TimeClient.init(modBus);
        com.cnethuka.kegare.severed.client.SeveredClient.init(modBus);
        com.cnethuka.kegare.impact.client.ImpactClient.init(modBus);
        com.cnethuka.kegare.battledamage.client.BattleDamageClient.init();
        BurnoutHud.init();
    }

    /** Emblem/icon art is looked up once per texture; forget the answers when resource packs change. */
    private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> GuiDraw.clearArtCache());
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        // Technique Mode: the combat panel takes the hotbar's place (HudClearance hides the vanilla hotbar and XP);
        // it raises Gui.leftHeight/rightHeight so hearts and food sit on top of it.
        // Fabric draws a layer only when vanilla reaches the element it is attached to: the hotbar's layers are skipped in spectator mode.
        event.registerAbove(VanillaGuiLayers.HOTBAR, CombatPanelLayer.ID, new CombatPanelLayer());
        event.registerAbove(CombatPanelLayer.ID, CombatHotbarLayer.ID, new CombatHotbarLayer());
        // After hearts/armour/food/air, so it clears whatever they used. Anchored to the info bar, not the experience level: Fabric only
        // draws a layer when vanilla reaches the element it is attached to, and vanilla draws the level only with experience and level > 0
        // (never in creative), while the info bar is drawn every frame, after the health bars.
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_BAR, CombatNameLayer.ID, new CombatNameLayer());
        // Without the panel: freeze stacks and casting progress under the crosshair.
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, StatusLayer.ID, new StatusLayer());
    }
}
