# Plan

Convert the existing Gradle Java project into a Forge 1.20.1 mod skeleton. Replace `build.gradle`, add `MHWeaponsMod.java`, add `mods.toml`, `pack.mcmeta`, and basic docs.

Steps:
1. Replace `build.gradle` to use ForgeGradle and Forge 1.20.1.
2. Add `src/main/java/org/example/MHWeaponsMod.java` as mod entry point.
3. Add `src/main/resources/META-INF/mods.toml` and `src/main/resources/pack.mcmeta`.
4. Add documentation files in `docs/`.
5. Run `./gradlew --refresh-dependencies` and `./gradlew assemble` and `./gradlew runClient` for smoke testing.

6. Integrate Better Combat (required for this mod):
   - Add `maven { url 'https://api.modrinth.com/maven' }` to `repositories` and add `implementation fg.deobf('maven.modrinth:better-combat:<VERSION-forge>')` to `dependencies` to use Better Combat's Java API as documented. Note: this project sets Better Combat as a required dependency (mandatory=true in `mods.toml`).
   - Ensure companion dependencies are present and add them to `build.gradle` using their official repos:
     - Cloth Config (Forge): `implementation fg.deobf('me.shedaniel.cloth:cloth-config-forge:11.1.136')` (repo: `https://maven.shedaniel.me/`)
     - PlayerAnimator (Forge): `implementation fg.deobf('dev.kosmx.player-anim:player-animation-lib-forge:<VERSION>')` (repo: `https://maven.kosmx.dev/`)
     - MixinExtras (Forge): `implementation fg.deobf('io.github.llamalad7:mixinextras-forge:<VERSION>')` (Maven Central)
   - For PlayerAnimator development runs, ensure mixin properties are set in `runs.client` and `runs.server` per its docs. These properties are already added to `build.gradle`.
   - Verify no semantic conflicts (player animation, attack timing, dual wielding, custom item wielding) and update `docs/changes.md` with the exact versions used.

## Implementation Phases - Mechanics

### Phase 1: Core Systems (Global Mechanics)
- [ ] **Damage Types**:
  - Implementation of `MHDamageType` Enum (Sever, Blunt, Shot).
  - `CombatReferee` event handler for `LivingHurtEvent` to simulate hitzones.
- [ ] **Guard Points**:
  - `WeaponState` capability to track animation states.
  - `LivingAttackEvent` handler to intercept damage during GP windows.
- [ ] **Evasion**:
  - `Dodge` state handler with i-frame timer.
  - Event cancellation for invulnerability frames.

