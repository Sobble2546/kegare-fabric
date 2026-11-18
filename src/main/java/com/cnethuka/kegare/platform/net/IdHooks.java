package com.cnethuka.kegare.platform.net;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.sounds.SoundEvent;
import org.slf4j.Logger;

/**
 * Did {@code ByteBufCodecsMixin} apply? Its two injections are optional ({@code require = 0}), so a miss (a refmap that was not written, a
 * vanilla change) would otherwise only show as Kegare's sounds silently not playing through ViaVersion, or Kegare's entities and items not
 * decoding. This asks vanilla for one codec of each kind and sees whether the hook wrapped it, once at startup, and says so in the log.
 * Where a miss would break a Via setup it stops the game instead of letting that happen quietly.
 */
final class IdHooks {
    private static final Logger LOGGER = LogUtils.getLogger();

    record Status(boolean registry, boolean holder) {
        @Override
        public String toString() {
            return "registry=" + (registry ? "applied" : "MISSING") + " holder=" + (holder ? "applied" : "MISSING");
        }
    }

    private IdHooks() {}

    /** Builds the two vanilla codecs a hook would wrap (harmless: no state, only codec objects) and reports which were wrapped. */
    static Status probe() {
        int registryBefore = IdRemap.registryHooks();
        int holderBefore = IdRemap.holderHooks();
        ByteBufCodecs.registry(Registries.ENTITY_TYPE);
        ByteBufCodecs.holder(Registries.SOUND_EVENT, SoundEvent.DIRECT_STREAM_CODEC);
        return new Status(IdRemap.registryHooks() > registryBefore, IdRemap.holderHooks() > holderBefore);
    }

    /** Logs the status. With ViaFabric loaded a missing hook is fatal: Kegare's sounds would silently never play for Via clients. */
    static void check(FabricLoader loader) {
        Status status = probe();
        LOGGER.info("Kegare id-codec hooks: {}", status);
        if (status.registry() && status.holder()) return;
        String message = "Kegare's network id-codec mixin did not fully apply (" + status + "): Via clients on another Minecraft version will not "
                + "hear Kegare's sounds or decode its entities and items. The built jar's mixin refmap is probably missing or stale; report this.";
        if (loader.isModLoaded("viafabric")) throw new IllegalStateException(message);
        LOGGER.error(message + " Only the Via bridge is affected, so the game carries on.");
    }
}
