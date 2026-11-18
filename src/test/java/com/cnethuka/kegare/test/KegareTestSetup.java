package com.cnethuka.kegare.test;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Registered for every test class (META-INF/services plus {@code junit.jupiter.extensions.autodetection.enabled} in junit-platform.properties):
 * boots the game once, so the tests that only touch an {@code ItemStack} or a registry need no annotation. Kept out of a
 * {@code LauncherSessionListener} on purpose: those run in service-loader order, and this has to run after fabric-loader-junit's, which starts Knot.
 */
public final class KegareTestSetup implements BeforeAllCallback {
    @Override
    public void beforeAll(ExtensionContext context) {
        TestBootstrap.ensure();
    }
}
