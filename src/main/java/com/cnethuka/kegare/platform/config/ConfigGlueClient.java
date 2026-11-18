package com.cnethuka.kegare.platform.config;

import com.cnethuka.kegare.Kegare;
import fuzs.forgeconfigapiport.fabric.api.v5.client.ConfigScreenFactoryRegistry;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/** Client only: the config screen Forge Config API Port builds from the specs, reachable from Mod Menu's config button. Needs Mod Menu to be seen. */
public final class ConfigGlueClient {
    private ConfigGlueClient() {}

    /** Call from the client entrypoint. NeoForge's {@code registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new)}. */
    public static void registerScreen() {
        ConfigScreenFactoryRegistry.INSTANCE.register(Kegare.MODID, (modId, parent) -> new ConfigurationScreen(modId, parent));
    }
}
