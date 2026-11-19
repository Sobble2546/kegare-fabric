package com.cnethuka.kegare.client.visual;

/**
 * 26.x: there is no particle engine (Photon FX is built for 1.21.11 only and is not in these jars; DESIGN §3.13), so nothing
 * is installed and {@link PhotonEffects} keeps reporting "can't play": effects draw with vanilla particles. The 1.21.11
 * overlay holds the real bridge.
 */
final class PhotonBridge {
    private PhotonBridge() {}

    static void install() {}
}
