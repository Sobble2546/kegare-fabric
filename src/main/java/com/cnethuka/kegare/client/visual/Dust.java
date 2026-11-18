package com.cnethuka.kegare.client.visual;

import net.minecraft.core.particles.DustParticleOptions;

/** Vanilla's coloured dust: 1.21.11 takes the colour as one 0xRRGGBB int, where it took three floats. */
public final class Dust {
    private Dust() {}

    public static DustParticleOptions of(float r, float g, float b, float scale) {
        return new DustParticleOptions(channel(r) << 16 | channel(g) << 8 | channel(b), scale);
    }

    public static DustParticleOptions ofRgb(int rgb, float scale) {
        return new DustParticleOptions(rgb & 0xFFFFFF, scale);
    }

    private static int channel(float c) {
        return Math.max(0, Math.min(255, Math.round(c * 255f)));
    }
}
