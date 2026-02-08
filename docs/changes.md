# Changes

- Replaced top-level `build.gradle` to use ForgeGradle and Forge 1.20.1 (recommend using 1.20.1-47.4.10+).
- Added `src/main/java/org/example/MHWeaponsMod.java` (entry point).
- Added `src/main/resources/META-INF/mods.toml` and `src/main/resources/pack.mcmeta`.
- Added docs: `plan.md`, `changes.md`, `description.md`.

## Build notes
- Initial `./gradlew assemble` ran Forge setup, mappings and asset download steps and completed successfully on this machine.
- If `build/libs` lacks the final jar, run `./gradlew reobfJar assemble` or `./gradlew build` to ensure the mod jar is produced.

## Integration updates
- Added Better Combat integration (Modrinth): `implementation fg.deobf('maven.modrinth:better-combat:1.8.4+1.20.1-forge')` and added `https://api.modrinth.com/maven` to `repositories`. **Better Combat is now a required dependency (mandatory=true in `mods.toml`).** Follow Better Combat documentation for the correct version to use for your target Minecraft version.
- Added companion dependencies (required by Better Combat or recommended for full functionality):
  - Cloth Config (Forge): `implementation fg.deobf('me.shedaniel.cloth:cloth-config-forge:11.1.136')` (repo: `https://maven.shedaniel.me/`)
  - PlayerAnimator (Forge): `implementation fg.deobf('dev.kosmx.player-anim:player-animation-lib-forge:1.0.2-rc1+1.20')` (repo: `https://maven.kosmx.dev/`)
  - MixinExtras (Forge): `implementation fg.deobf('io.github.llamalad7:mixinextras-forge:0.5.3')` (Maven Central)
- Note: PlayerAnimator requires mixin remap properties to be set in the Gradle run configurations for development (added to `build.gradle`).
- Remember to include the matching runtime mod jars (Better Combat, PlayerAnimator, Cloth Config) in `./run/mods` when testing runtime behavior.

