# Description

MHWeaponsMod — minimal Forge mod skeleton for Forge 1.20.1. Contains mod main class and essential resources to build and run in the Forge dev environment.

Mod ID: `mhweaponsmod`
Group: `org.example`
Version: `1.0.0`

Integration note:
- This project includes integration with **Better Combat** (https://github.com/ZsoltMolnarrr/BetterCombat). See `build.gradle` for the `implementation fg.deobf('maven.modrinth:better-combat:1.8.4+1.20.1-forge')` example.
- **Better Combat is required (mandatory=true in `mods.toml`).**
- Companion libraries required or recommended:
  - Cloth Config (Forge): `implementation fg.deobf('me.shedaniel.cloth:cloth-config-forge:11.1.136')` (repo: `https://maven.shedaniel.me/`)
  - PlayerAnimator (Forge): `implementation fg.deobf('dev.kosmx.player-anim:player-animation-lib-forge:1.0.2-rc1+1.20')` (repo: `https://maven.kosmx.dev/`)
  - MixinExtras (Forge): `implementation fg.deobf('io.github.llamalad7:mixinextras-forge:0.5.3')` (Maven Central)
- For runtime testing, place matching mod jars (Better Combat, PlayerAnimator, Cloth Config) into `./run/mods`.
