package com.cnethuka.kegare.platform.client;

import com.cnethuka.kegare.platform.NeoForge;
import com.cnethuka.kegare.platform.TriState;
import com.cnethuka.kegare.platform.bus.EventBus;
import com.cnethuka.kegare.platform.client.event.ClientPlayerNetworkEvent;
import com.cnethuka.kegare.platform.client.event.ClientTickEvent;
import com.cnethuka.kegare.platform.client.event.EntityRenderersEvent;
import com.cnethuka.kegare.platform.client.event.FMLClientSetupEvent;
import com.cnethuka.kegare.platform.client.event.InputEvent;
import com.cnethuka.kegare.platform.client.event.ModelEvent;
import com.cnethuka.kegare.platform.client.event.MovementInputUpdateEvent;
import com.cnethuka.kegare.platform.client.event.RegisterClientReloadListenersEvent;
import com.cnethuka.kegare.platform.client.event.RegisterGuiLayersEvent;
import com.cnethuka.kegare.platform.client.event.RegisterKeyMappingsEvent;
import com.cnethuka.kegare.platform.client.event.RenderFrameEvent;
import com.cnethuka.kegare.platform.client.event.RenderGuiLayerEvent;
import com.cnethuka.kegare.platform.client.event.RenderHandEvent;
import com.cnethuka.kegare.platform.client.event.RenderLevelStageEvent;
import com.cnethuka.kegare.platform.client.event.RenderNameTagEvent;
import com.cnethuka.kegare.platform.client.event.RenderPlayerEvent;
import com.cnethuka.kegare.platform.client.event.ToastAddEvent;
import com.cnethuka.kegare.platform.client.event.ViewportEvent;
import com.cnethuka.kegare.platform.client.event.sound.PlaySoundEvent;
import com.cnethuka.kegare.platform.client.gui.VanillaGuiLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import javax.annotation.Nullable;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * Fires NeoForge-shaped client events from Fabric callbacks and from the mixins in {@code kegare-client-events.mixins.json}
 * (each of those is a one-line call into this class). Two buses, as in NeoForge: {@link NeoForge#EVENT_BUS} for everything
 * that happens while playing, and {@link #modBus()} for the registration events that fire once during client initialisation.
 * <p>
 * Order, from {@code KegareClient.onInitializeClient}: {@link #init()} (Fabric callbacks), the modules' init (they add their
 * listeners), then {@link #fireRegistrationEvents()}. Nothing here looks at whether the server runs Kegare: logout cleanup and
 * registration must always happen; the gate is {@code ClientNet.isActive()} where something is sent or drawn.
 */
public final class ClientEventHooks {
    private ClientEventHooks() {}

    private static final EventBus MOD_BUS = new EventBus();
    private static final PlayerReplacement<LocalPlayer> PLAYER = new PlayerReplacement<>();
    /** A disconnect, waiting for the client thread to announce it. */
    private record Leave(@Nullable LocalPlayer player, net.minecraft.network.Connection connection) {}

    private static final HandOff<Leave> LEAVES = new HandOff<>();
    private static int clientTicks;
    private static boolean initialised;
    private static boolean registered;
    private static int reloaders;

    /** The bus the registration events are posted on (NeoForge's mod event bus, client side). */
    public static EventBus modBus() {
        return MOD_BUS;
    }

    // ------------------------------------------------------------------------------------------------ startup

    /** Registers the Fabric callbacks. Idempotent. Call before the modules add their listeners (order does not matter to the bus, this is for clarity). */
    public static synchronized void init() {
        if (initialised) return;
        initialised = true;
        ClientCompat.registerTicks(ClientEventHooks::tickPre, ClientEventHooks::tickPost);
        ClientCompat.registerConnection(
                (mc, handler) -> {
                    postPendingLeaves(); // a leave from the last connection that never got its turn comes before this join
                    PLAYER.joined(mc.player);
                    NeoForge.EVENT_BUS.post(new ClientPlayerNetworkEvent.LoggingIn(mc.player, handler.getConnection()));
                },
                (mc, handler) -> {
                    // Fabric's DISCONNECT runs on the Netty thread when the channel drops (Connection.channelInactive), and the listeners
                    // clear collections and GPU targets the client thread owns: they run there, with the player captured before the hop.
                    // The leave waits in a queue the client posts from as well as the hop: Minecraft.disconnect drops the client's task
                    // queue (and the hop with it), so the next client tick, or the next join, posts what is still waiting.
                    LocalPlayer player = mc.player != null ? mc.player : PLAYER.last();
                    LEAVES.add(new Leave(player, handler.getConnection()));
                    if (mc.isSameThread()) postPendingLeaves();
                    else mc.execute(ClientEventHooks::postPendingLeaves);
                });
        LevelStages.init();
    }

    /**
     * Posts the registration events on the mod bus and hands what listeners registered to Fabric: key bindings, HUD layers,
     * reload listeners, entity renderers and model layers, extra models. Also schedules {@link FMLClientSetupEvent} for when the
     * client has started. Call once, after the modules added their listeners.
     */
    public static synchronized void fireRegistrationEvents() {
        if (registered) return;
        registered = true;
        MOD_BUS.post(new RegisterKeyMappingsEvent(ClientPlatform::registerKey));
        for (Identifier layer : VanillaGuiLayers.WRAPPED.stream().distinct().toList()) {
            HudElementRegistry.replaceElement(layer, previous -> wrapHud(layer, previous));
        }
        MOD_BUS.post(new RegisterGuiLayersEvent(new RegisterGuiLayersEvent.Registrar() {
            @Override
            public void attachAfter(Identifier other, Identifier id, HudElement layer) {
                HudElementRegistry.attachElementAfter(other, id, layer);
            }

            @Override
            public void attachBefore(Identifier other, Identifier id, HudElement layer) {
                HudElementRegistry.attachElementBefore(other, id, layer);
            }
        }));
        MOD_BUS.post(new RegisterClientReloadListenersEvent(listener -> ClientCompat.registerReloader(nextReloaderId(), listener)));
        ClientCompat.registerReloaderAfterModels(nextReloaderId(), (net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager ->
                MOD_BUS.post(new ModelEvent.BakingCompleted()));
        ClientCompat.registerExtraModels(MOD_BUS::post);
        EntityRenderersEvent.Registrar renderers = ClientCompat.rendererRegistrar();
        MOD_BUS.post(new EntityRenderersEvent.RegisterRenderers(renderers));
        MOD_BUS.post(new EntityRenderersEvent.RegisterLayerDefinitions(renderers));
        ClientCompat.onClientStarted(() -> MOD_BUS.post(new FMLClientSetupEvent()));
    }

    private static Identifier nextReloaderId() {
        return Identifier.fromNamespaceAndPath("kegare", "client_reload_" + reloaders++);
    }

    // ------------------------------------------------------------------------------------------------ ticks and players

    /** Client thread: posts every disconnect that is waiting (each once, whoever gets here first). */
    private static void postPendingLeaves() {
        LEAVES.drain(leave -> {
            com.cnethuka.kegare.Kegare.LOGGER.debug("Posting LoggingOut on thread {}", Thread.currentThread().getName());
            PLAYER.left();
            NeoForge.EVENT_BUS.post(new ClientPlayerNetworkEvent.LoggingOut(leave.player(), leave.connection()));
        });
    }

    /** Client ticks since the game started: one per tick whatever the player or the level are doing. */
    public static int clientTicks() {
        return clientTicks;
    }

    private static void tickPre() {
        clientTicks++;
        postPendingLeaves();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer old = PLAYER.poll(mc.player);
        if (old != null) {
            NeoForge.EVENT_BUS.post(new ClientPlayerNetworkEvent.Clone(old, mc.player, mc.getConnection() == null ? null : mc.getConnection().getConnection()));
        }
        NeoForge.EVENT_BUS.post(new ClientTickEvent.Pre());
    }

    private static void tickPost() {
        NeoForge.EVENT_BUS.post(new ClientTickEvent.Post());
    }

    // ------------------------------------------------------------------------------------------------ HUD

    /** A vanilla HUD layer with Pre (cancel = skip it) and Post around it. */
    static HudElement wrapHud(Identifier layer, HudElement previous) {
        return (graphics, delta) -> {
            if (NeoForge.EVENT_BUS.post(new RenderGuiLayerEvent.Pre(graphics, delta, layer)).isCanceled()) return;
            previous.render(graphics, delta);
            NeoForge.EVENT_BUS.post(new RenderGuiLayerEvent.Post(graphics, delta, layer));
        };
    }

    // ------------------------------------------------------------------------------------------------ level

    /** A point in the world drawing was reached (called by LevelStages). */
    public static void levelStage(RenderLevelStageEvent.Stage stage, Camera camera, DeltaTracker partialTick, PoseStack pose, Matrix4f modelView, Matrix4f projection) {
        NeoForge.EVENT_BUS.post(new RenderLevelStageEvent(stage, camera, partialTick, pose, modelView, projection));
    }

    public static void frameStart(DeltaTracker partialTick) {
        NeoForge.EVENT_BUS.post(new RenderFrameEvent.Pre(partialTick));
    }

    public static void frameEnd(DeltaTracker partialTick) {
        NeoForge.EVENT_BUS.post(new RenderFrameEvent.Post(partialTick));
    }

    /** @return true if a listener cancelled this hand */
    public static boolean renderHand(InteractionHand hand, float partialTick, float pitch, float swing, ItemStack stack, float equip, PoseStack pose,
                                     SubmitNodeCollector collector, int light) {
        return NeoForge.EVENT_BUS.post(new RenderHandEvent(hand, pose, collector, light, partialTick, pitch, swing, equip, stack)).isCanceled();
    }

    // ------------------------------------------------------------------------------------------------ viewport

    public static float computeFov(Camera camera, float partialTick, float fov, boolean usedConfiguredFov) {
        ViewportEvent.ComputeFov event = NeoForge.EVENT_BUS.post(new ViewportEvent.ComputeFov(camera, partialTick, fov, usedConfiguredFov));
        return (float) event.getFOV();
    }

    /** @return the camera angles after the listeners had their say: {yaw, pitch, roll} in degrees */
    public static float[] computeCameraAngles(Camera camera, float partialTick, float yaw, float pitch) {
        ViewportEvent.ComputeCameraAngles event = NeoForge.EVENT_BUS.post(new ViewportEvent.ComputeCameraAngles(camera, partialTick, yaw, pitch, 0f));
        return new float[] {event.getYaw(), event.getPitch(), event.getRoll()};
    }

    // ------------------------------------------------------------------------------------------------ entities

    /** Fires AddLayers for the freshly built player renderers (from EntityRenderDispatcher's reload). */
    public static void addPlayerLayers(Map<PlayerModelType, ? extends EntityRenderer<?, ?>> renderers, EntityModelSet models) {
        MOD_BUS.post(new EntityRenderersEvent.AddLayers(renderers, models));
    }

    public static void playerExtractStart(Player player, AvatarRenderer<?> renderer, AvatarRenderState state, float partialTick) {
        NeoForge.EVENT_BUS.post(new RenderPlayerEvent.Pre(player, renderer, state, partialTick));
    }

    public static void playerExtractEnd(Player player, AvatarRenderer<?> renderer, AvatarRenderState state, float partialTick) {
        NeoForge.EVENT_BUS.post(new RenderPlayerEvent.Post(player, renderer, state, partialTick));
    }

    /**
     * Lets listeners rewrite or hide an entity's name tag.
     *
     * @return the tag to show: the same one, a replacement, or null for none
     */
    @Nullable
    public static Component nameTag(Entity entity, Component tag, float partialTick) {
        RenderNameTagEvent event = NeoForge.EVENT_BUS.post(new RenderNameTagEvent(entity, tag, partialTick));
        return event.canRender() == TriState.FALSE ? null : event.getContent();
    }

    // ------------------------------------------------------------------------------------------------ input

    public static void key(int key, int scanCode, int action, int modifiers) {
        NeoForge.EVENT_BUS.post(new InputEvent.Key(key, scanCode, action, modifiers));
    }

    public static void mouseButton(boolean before, int button, int action, int modifiers) {
        NeoForge.EVENT_BUS.post(before ? new InputEvent.MouseButton.Pre(button, action, modifiers) : new InputEvent.MouseButton.Post(button, action, modifiers));
    }

    /** Attack, use or pick-block is about to run. @return true if it was cancelled (the hand has been swung if the listener left that on) */
    public static boolean interaction(InputEvent.Interaction kind, InteractionHand hand, KeyMapping key) {
        InputEvent.InteractionKeyMappingTriggered event = NeoForge.EVENT_BUS.post(new InputEvent.InteractionKeyMappingTriggered(kind, hand, key));
        if (!event.isCanceled()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (event.shouldSwingHand() && mc.player != null) com.cnethuka.kegare.platform.EntityCompat.swing(mc.player, hand);
        return true;
    }

    /** Posts the movement input event for the view and returns the (possibly changed) view. */
    public static MovementInputUpdateEvent.Input movementInput(MovementInputUpdateEvent.Input input) {
        NeoForge.EVENT_BUS.post(new MovementInputUpdateEvent(input));
        return input;
    }

    // ------------------------------------------------------------------------------------------------ toasts and sounds

    /** @return true if a listener cancelled the toast */
    public static boolean toast(Toast toast) {
        return NeoForge.EVENT_BUS.post(new ToastAddEvent(toast)).isCanceled();
    }

    /** @return the sound that should play (the original, a replacement), or null for none */
    @Nullable
    public static SoundInstance sound(SoundInstance sound) {
        return NeoForge.EVENT_BUS.post(new PlaySoundEvent(sound)).getSound();
    }

}
