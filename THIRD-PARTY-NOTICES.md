# Third-party work inside the Kegare jar

Kegare's own licence (`LICENSE`, All Rights Reserved) covers only Kegare's own work. The Kegare jar for **Minecraft 1.21.11**
also carries the works below, each as a separate jar nested under `META-INF/jars/`, each under its own licence. They are not
Kegare's, Kegare's licence does not apply to them, and their licences do not apply to Kegare. The jars for 26.2 and 26.3 do not
contain them.

## Photon FX (mod id `photonfx`)
- A runtime-only port of **Photon 2.2.10** by KilaBash / Low-Drag-MC (https://github.com/Low-Drag-MC/Photon) to Fabric: it plays
  Photon `.fx` effects and has no editor. It is a modified derivative, not made, reviewed or endorsed by Photon's author.
- Licence: Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International
  (https://creativecommons.org/licenses/by-nc-sa/4.0/) plus the additional terms of Photon's own licence file, its
  "Restrictions on Photon 2 Ports" among them. **Non-commercial:** no fees, paid downloads, donations, sponsorships or
  crowdfunding may be tied to it.
- In this jar: `META-INF/jars/photonfx-<minecraft>-<version>.jar`. Its licence text, what was changed and the list of every file
  derived from Photon are in that jar's root and, copied from it, in `META-INF/licenses/photonfx/` (`LICENSE`, `NOTICE.md`,
  `DERIVED.md`).

## ldlib2-lite (mod id `ldlib2lite`)
- A small maths subset (curves, gradients, colours, noise, transforms) of **LDLib2 2.2.45** by the LDLib2 contributors /
  Low-Drag-MC (https://github.com/Low-Drag-MC/LDLib2), modified. Not made, reviewed or endorsed by its authors.
- Licence: GNU Lesser General Public License, version 3 only.
- In this jar: nested inside the Photon FX jar (`META-INF/jars/ldlib2lite-<minecraft>-<version>.jar` there), as a mod of its
  own, so it can be replaced by a modified build: put a jar with the mod id `ldlib2lite` and a higher version in the `mods`
  folder and Fabric Loader uses that one. Its licence texts and file list are in its root and, copied from it, in
  `META-INF/licenses/ldlib2lite/` (`LICENSE-LGPL-3.0`, `COPYING.LESSER`, `COPYING`, `NOTICE.md`, `DERIVED.md`).

## Source
Both are built from the `photon-fabric` source tree (`ldlib2-lite/` in it for the LGPL part), which is kept next to Kegare's
and is not part of this repository. Ask the Kegare author for it.

Also nested, unchanged: the PostgreSQL JDBC driver (`org.postgresql:postgresql`, BSD-2-Clause, https://jdbc.postgresql.org/).
