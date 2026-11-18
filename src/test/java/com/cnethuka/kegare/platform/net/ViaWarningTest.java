package com.cnethuka.kegare.platform.net;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ViaWarningTest {
    @Test
    void noWarningWithoutViaFabric() {
        assertFalse(ViaWarning.needsWarning(false, null));
        assertFalse(ViaWarning.needsWarning(false, "ignore-registry-sync-errors: false"));
    }

    @Test
    void warnsWhenViaFabricHasNoConfigYet() {
        assertTrue(ViaWarning.needsWarning(true, null), "the default is false");
    }

    @Test
    void warnsWhenTheFlagIsOffOrMissing() {
        assertTrue(ViaWarning.needsWarning(true, "ignore-registry-sync-errors: false\n"));
        assertTrue(ViaWarning.needsWarning(true, "something-else: true\n"));
        assertTrue(ViaWarning.needsWarning(true, "# ignore-registry-sync-errors: true\n"));
    }

    @Test
    void quietWhenTheFlagIsOn() {
        assertFalse(ViaWarning.needsWarning(true, "debug: false\nignore-registry-sync-errors: true\n"));
        assertFalse(ViaWarning.needsWarning(true, "  ignore-registry-sync-errors:   true   # keep\n"));
        assertFalse(ViaWarning.needsWarning(true, "ignore-registry-sync-errors: \"true\"\n"));
        assertFalse(ViaWarning.needsWarning(true, "ignore-registry-sync-errors: TRUE\n"));
    }
}
