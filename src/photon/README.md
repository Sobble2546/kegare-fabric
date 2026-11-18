Photon-backed VFX, kept for the Photon-on-Fabric phase. These sources are NOT compiled by the Fabric build: Photon and
LDLib2 have no official Fabric build yet (see DESIGN §3.13). `PhotonEffects.java` here is the full NeoForge-era
implementation; the compiled `client/visual/PhotonEffects` is the engine-neutral facade that falls back to vanilla
particles. The `*FxAuthoring`, `FxKit`, `FxCatalog` and `FxDevCommand` classes regenerate `assets/kegare/fx/*.fx`.
