# Third-party notices

Everything in this repository is GPL-3.0 (see LICENSE) except the items below.
Each entry lists what we ship, where it comes from, and under what terms it
reaches you. There are no other bundled third-party assets: the Minecraft
client itself and its assets are downloaded at runtime from Mojang's official
piston-meta/CDN endpoints and are never redistributed by us — the launcher
verifies every download against Mojang's published SHA1 before use.

## Runtime dependencies (fetched at install time, not redistributed)

| Component | Source | Terms |
|---|---|---|
| Minecraft: Java Edition client & assets | Mojang piston-meta / piston-data CDN | Mojang EULA; downloaded, hash-verified, never bundled |
| Fabric Loader / Fabric API | fabricmc.net maven | Apache-2.0 |
| Fabric Loader / Fabric API | fabricmc.net maven (`net.fabricmc.fabric-api:fabric-api`), fetched at install time | Apache-2.0 |
| Sodium | Modrinth (`project: sodium`), fetched at install time | LGPL-3.0; source at github.com/CaffeineMC/sodium-fabric |
| Lithium | Modrinth (`project: lithium`), fetched at install time | LGPL-3.0; source at github.com/CaffeineMC/lithium-fabric |

## Fonts

| Component | Source | Terms |
|---|---|---|
| Pretendard (Regular/SemiBold/ExtraBold, bundled in launcher `public/fonts/`) | github.com/orioncactus/pretendard | SIL Open Font License 1.1 |

## Build-time only (never shipped)

Rust crates, npm packages and Gradle plugins resolve from their registries at
build time under their own licenses; none are modified by us. See
`launcher/src-tauri/Cargo.lock`, `launcher/package-lock.json` and
`pinion-mod/gradle.lockfile` for exact pinned versions.

## Upstream lineage

This project began as a fork of Pinion (github.com/RedHatOnTop/pinion), which
is itself the work of the same author. The full history is preserved in this
repository's git log.
