package com.cnethuka.kegare.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

/**
 * What NeoForge's {@code FMLEnvironment} answered: which side this is, and whether this is a development environment. Read lazily, on
 * first use, and with a default when there is no Fabric loader at all (a plain JVM): production, on the server.
 */
public final class FMLEnvironment {
    private FMLEnvironment() {}

    /** True outside a development environment (a player's game). */
    public static boolean production() {
        return Facts.PRODUCTION;
    }

    /** The side the game code runs on. */
    public static DistView dist() {
        return Facts.DIST;
    }

    /** Looked up once, when first asked. */
    private static final class Facts {
        static final boolean PRODUCTION;
        static final DistView DIST;

        static {
            boolean production = true;
            EnvType type = EnvType.SERVER;
            try {
                FabricLoader loader = FabricLoader.getInstance();
                production = !loader.isDevelopmentEnvironment();
                type = loader.getEnvironmentType();
            } catch (Throwable noLoader) {
                // not running under Fabric's loader: the defaults stand
            }
            PRODUCTION = production;
            DIST = new DistView(type);
        }
    }

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
