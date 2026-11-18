package com.cnethuka.kegare.impact.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cnethuka.kegare.Kegare;
import com.cnethuka.kegare.impact.ScreenEffects;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

/** The screen shader id comes from the server (DESIGN §3.23): only Kegare's own effects may be named. */
class ScreenFxTest {
    @Test
    void onlyKegaresOwnEffectsPlay() {
        for (Identifier effect : ScreenEffects.ALL) assertTrue(ScreenFx.allowed(effect), effect.toString());
        assertFalse(ScreenFx.allowed(Identifier.withDefaultNamespace("grayscale")));
        assertFalse(ScreenFx.allowed(Identifier.fromNamespaceAndPath("other_mod", "invert")));
        assertFalse(ScreenFx.allowed(Kegare.id("senses"))); // a shader of ours, but not a screen effect
    }
}
