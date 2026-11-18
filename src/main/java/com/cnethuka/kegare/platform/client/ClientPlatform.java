package com.cnethuka.kegare.platform.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * What client code outside the event shim needs from the platform: keys, and the checks FMLEnvironment used to answer. Anything
 * that differs between Minecraft versions lives in the package-private {@code ClientCompat} (one per target).
 */
public final class ClientPlatform {
    private ClientPlatform() {}

    /** A key category (the heading in Controls). Its title is the lang key {@code key.category.<namespace>.<path>}. */
    public static KeyMapping.Category keyCategory(Identifier id) {
        return ClientCompat.keyCategory(id);
    }

    public static KeyMapping newKey(String name, InputConstants.Type type, int code, KeyMapping.Category category) {
        return ClientCompat.newKey(name, type, code, category);
    }

    /** Adds the key to Controls; call during client initialisation (the RegisterKeyMappingsEvent does). */
    public static void registerKey(KeyMapping mapping) {
        ClientCompat.registerKey(mapping);
    }

    /** The Mod Menu config button's screen (Forge Config API Port builds it from the specs). */
    public static void registerConfigScreen() {
        com.cnethuka.kegare.platform.config.ConfigGlueClient.registerScreen();
    }
}
