package com.cnethuka.kegare.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

/** What NeoForge's {@code FMLEnvironment} answered: which side this is, and whether this is a development environment. */
public final class FMLEnvironment {
    private FMLEnvironment() {}

    /** True outside a development environment (a player's game). */
    public static final boolean production = !FabricLoader.getInstance().isDevelopmentEnvironment();

    /** The side the game code runs on. */
    public static final DistView dist = new DistView(FabricLoader.getInstance().getEnvironmentType());

    public static final class DistView {
        private final EnvType type;

        private DistView(EnvType type) {
            this.type = type;
        }

        public boolean isClient() {
            return type == EnvType.CLIENT;
        }

        public boolean isDedicatedServer() {
            return type == EnvType.SERVER;
        }

        public Dist get() {
            return isClient() ? Dist.CLIENT : Dist.DEDICATED_SERVER;
        }
    }
}
