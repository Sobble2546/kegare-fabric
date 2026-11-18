package com.cnethuka.kegare.platform.config;

import com.cnethuka.kegare.Kegare;
import com.cnethuka.kegare.config.KegareClientConfig;
import com.cnethuka.kegare.config.KegareServerConfig;
import com.cnethuka.kegare.platform.bus.IEventBus;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents;
import net.neoforged.fml.config.ModConfig;

/**
 * NeoForge's {@code ModContainer.registerConfig} and {@code ModConfigEvent}, over Forge Config API Port. Common initialisation, on both sides.
 * <p>
 * The client config is open as soon as it is registered (earlier than NeoForge; nothing may read a client value before this runs). The
 * server config opens when a world starts, from {@code <world>/serverconfig/kegare-server.toml}, and Forge Config API Port syncs it to
 * clients, so it has to be installed on both sides (fabric.mod.json depends on it).
 */
public final class ConfigGlue {
    private ConfigGlue() {}

    /**
     * Registers both specs and forwards load, reload and unload to {@code modBus} as {@link ModConfigEvent}s. Listeners must already be on the
     * bus, because the client config loads inside this call.
     */
    public static void register(IEventBus modBus) {
        ModConfigEvents.loading(Kegare.MODID).register(config -> modBus.post(new ModConfigEvent.Loading(config)));
        ModConfigEvents.reloading(Kegare.MODID).register(config -> modBus.post(new ModConfigEvent.Reloading(config)));
        ModConfigEvents.unloading(Kegare.MODID).register(config -> modBus.post(new ModConfigEvent.Unloading(config)));
        ConfigRegistry.INSTANCE.register(Kegare.MODID, ModConfig.Type.SERVER, KegareServerConfig.SPEC);
        ConfigRegistry.INSTANCE.register(Kegare.MODID, ModConfig.Type.CLIENT, KegareClientConfig.SPEC);
    }
}
