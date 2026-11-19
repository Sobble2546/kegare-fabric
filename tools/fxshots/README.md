# Seeing Kegare's effects through Photon FX on the build box

The scripts used to look at real ability casts drawn by Photon FX through `PhotonBridge` (DESIGN §3.13) on the lab VPS: one
dedicated dev server and one dev client under Xvfb with software GL, driven by server console commands and `xdotool` keys,
captured with ImageMagick `import`. They are lab scripts with the lab's paths in them (`/opt/kegare-lab`, work dir
`work/bridge`, display `:96`, port 25611), kept here as they were run, not a general tool.

| Script | Where | What |
|---|---|---|
| `bridge-sync.sh` | laptop | rsync this tree to `work/bridge` |
| `bridge-build.sh <log> <gradle args>` | VPS | Gradle in `work/bridge` inside the shared lock |
| `bridge-ctl.sh prep\|xvfb\|server\|client\|cmd\|kill` | VPS | run directories (copied from the lab's `work/effects` runs), Xvfb, the two JVMs |
| `bridge-session.sh [seconds]` | VPS | takes the lock, starts server and client, ends when `work/bridge/stop` appears, kills what it started |
| `bridge-lib.sh` | VPS, sourced | `S` (console command), `K` (xdotool), `SH` (screen grabs) |
| `bridge-prep.sh [time] [innate]` | VPS | makes Tester a Rime user at Refinement 10 with a pig in front |
| `bridge-tp.sh [f5]` | VPS | dusk, aim at the pig, optionally third person |
| `bridge-rime.sh <prefix>` | VPS | Technique Mode, then Frost Shard, Glacial Lance (charge and release), Frost Field, Rime Shell, with grabs |

Order: build Photon FX's local maven (`photon-fabric`), `bridge-sync.sh`, `bridge-build.sh x -Ptarget=1.21.11 classes`, once
`runServer` and `runClient` through Gradle so Loom writes `build/loom-cache/argFiles/*` (the server stops at the EULA, the
client at the missing display), `bridge-ctl.sh prep`, set `vfxQuality` in `run-client/config/kegare-client.toml`,
`bridge-session.sh`, then the scenario scripts. The client log must say `Ability effects are played by Photon FX` and
`photonfx: preloaded 45 of 45 effects`.
