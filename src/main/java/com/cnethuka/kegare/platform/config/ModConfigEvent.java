package com.cnethuka.kegare.platform.config;

import com.cnethuka.kegare.platform.bus.Event;
import net.neoforged.fml.config.ModConfig;

/** NeoForge's config lifecycle events, posted on the mod bus by {@link ConfigGlue} from Forge Config API Port's callbacks. */
public abstract class ModConfigEvent extends Event {
    private final ModConfig config;

    protected ModConfigEvent(ModConfig config) {
        this.config = config;
    }

    public ModConfig getConfig() {
        return config;
    }

    /** A config was read for the first time: the client one when it is registered, the server one when its world starts. */
    public static final class Loading extends ModConfigEvent {
        public Loading(ModConfig config) {
            super(config);
        }
    }

    /** The file changed on disk and was read again (on the file watcher's thread). */
    public static final class Reloading extends ModConfigEvent {
        public Reloading(ModConfig config) {
            super(config);
        }
    }

    public static final class Unloading extends ModConfigEvent {
        public Unloading(ModConfig config) {
            super(config);
        }
    }
}
