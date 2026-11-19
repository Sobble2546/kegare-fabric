package com.cnethuka.kegare.platform;

import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

/**
 * NeoForge's ModLoadingContext, the one member Kegare calls. Fabric has no "mod being loaded now", so the active namespace is the mod
 * whose code called into Kegare: the first class on the call stack outside the addon API's registration entry points, and the mod
 * whose files hold that class. Only used to name a mod in messages; when no mod owns the class, its name stands in.
 */
public final class ModLoadingContext {
    private static final ModLoadingContext INSTANCE = new ModLoadingContext();
    /** Packages whose frames are Kegare taking the call, not the caller. */
    private static final Set<String> SKIPPED = Set.of("com.cnethuka.kegare.platform", "com.cnethuka.kegare.api.change");

    private ModLoadingContext() {}

    public static ModLoadingContext get() {
        return INSTANCE;
    }

    public String getActiveNamespace() {
        Class<?> caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(frames -> frames
                .map(StackWalker.StackFrame::getDeclaringClass)
                .filter(c -> !SKIPPED.contains(c.getPackageName()))
                .findFirst()).orElse(null);
        if (caller == null) return "an unknown mod";
        String file = caller.getName().replace('.', '/') + ".class";
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            if (mod.findPath(file).isPresent()) return mod.getMetadata().getId();
        }
        return caller.getName();
    }
}
