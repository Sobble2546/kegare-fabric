package com.cnethuka.kegare.test;

import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.junit.platform.launcher.LauncherInterceptor;

/**
 * Makes JUnit load the test classes through Knot, the way the game's own classes are loaded.
 * <p>
 * fabric-loader-junit starts Knot (class loading, mixins, mod discovery) and sets it as the context class loader, but Gradle's test worker
 * hands JUnit test classes it has already loaded itself, through the application class loader. Those then see their own un-mixed copy of
 * Minecraft, next to the mixed one every mod entrypoint sees, and nothing works ("Not bootstrapped", two copies of every registry). JUnit
 * does not take the context class loader of the test thread but of the call that discovers and runs them, so this puts Knot's there for
 * each of them (registered in META-INF/services, switched on by {@code junit.platform.launcher.interceptors.enabled} in
 * junit-platform.properties).
 * <p>
 * Nothing of Fabric's is touched before the loader's own session listener has started Knot (it sets {@code fabric.unitTest} when its class
 * loads): asking for the launcher earlier initialises the loader in the wrong state and it then finds none of the mods.
 */
public final class KnotLauncherInterceptor implements LauncherInterceptor {
    @Override
    public <T> T intercept(Invocation<T> invocation) {
        ClassLoader before = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader knot = knotLoader();
            if (knot != null) Thread.currentThread().setContextClassLoader(knot);
            return invocation.proceed();
        } finally {
            Thread.currentThread().setContextClassLoader(before);
        }
    }

    private static ClassLoader knotLoader() {
        if (System.getProperty("fabric.unitTest") == null) return null;
        FabricLauncher launcher = FabricLauncherBase.getLauncher();
        return launcher == null ? null : launcher.getTargetClassLoader();
    }

    @Override
    public void close() {}
}
