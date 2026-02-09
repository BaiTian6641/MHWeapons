# Insect Glaive — Implementation Session Summary

> **Date**: February 7–9, 2026
> **Files Modified**: 10 source files, 1 data file, 1 plan document

---

## Session Overview

This document chronicles the implementation of the Insect Glaive weapon system across multiple sessions. The work progressed in phases: document planning → core implementation → bugfixes/polish → feature completion.

---

## Phase 1 — Document & Planning (Feb 7)

### Work Done
- Reviewed the existing Insect Glaive codebase: `KinsectEntity`, `InsectGlaiveEvents`, `KinsectItem`, `WeaponActionHandler`, `ClientForgeEvents`, `WeaponHudOverlay`
- Fetched the [MH Wilds Official Manual](https://manual.capcom.com/mhwilds/en/steam/page/4/11) for reference
- Produced comprehensive `Insect_Glaive.md` plan with:
  - Full implementation status audit (✅/❌ for every component)
  - Complete MH Wilds faithful move list with combo trees
  - Extract system gap analysis
  - Prioritized 3-phase development roadmap (17 tasks)
  - Acceptance criteria

### Key Discoveries
- Extract buff system was incomplete (Red-only gave no benefit, Orange lacked knockback resist, W+O defense missing)
- No charge mechanic existed at all
- No Leaping/Dodge/Tornado Slash moves
- No Vaulting Dance bounce system
- Client never sent RMB release events for IG

---

## Phase 2 — Core Implementation (Feb 7)

### PlayerWeaponState.java
**3 new fields added** with full getter/setter/serialization/copyFrom support:
- `insectAerialBounceLevel` (int, 0-2) — tracks Vaulting Dance power level
- `insectCharging` (boolean) — true while holding RMB with Red extract
- `insectChargeTicks` (int) — charge buildup counter, capped at 40

### InsectGlaiveEvents.java — Complete Rewrite
**Before**: Simple speed/attack modifiers, basic aerial tick-down.

**After**: Full per-tick state machine:
- **Extract Buffs**: Red-only → +5% ATK. Red+White → +10%. Triple → +15%. Orange → +10 armor (+15 with W+O). Orange → knockback resistance (0.5, 1.0 for triple).
- **Charge Tick-up**: While `insectCharging && isInsectRed()`, increment `insectChargeTicks` (cap 40). Cancel if Red expires or weapon swapped.
- **Aerial/Bounce State**: Aerial ticks decrement. Bounce level resets on landing. Fall damage zeroed during aerial.
- **Finisher Timeout**: Triple finisher ticks decrement, stage resets at 0.
- **Extract Expiry**: Charge canceled when extracts expire.

### WeaponActionHandler.java — handleInsectGlaive() Rewrite
**Before**: Basic LMB combo (3 attacks), RMB wide sweep/overhead, vault.

**After**: Full MH Wilds combo tree:

| Feature | Implementation |
|:---|:---|
| **Charge Release** (WEAPON_ALT !pressed) | If `insectCharging`: 10-19t → Tornado Slash (re-chainable). ≥20t → Descending Slash (ground, 22 dmg) or Descending Thrust (air, Y=-1.2, 18 dmg). Triple Up → finisher stage 1. |
| **LMB Ground** | Rising Slash → Reaping → Double Slash (cycles 0→1→2). Focus Mode triggers kinsect combo. |
| **LMB Aerial** | Jumping Advancing Slash with Vaulting Dance: `findTargetInFront(3.0)`, bounce level 0→1→2, +0.15 speed, +20% damage, refreshes aerialTicks. |
| **RMB Finisher** | If `finisherStage==1` → Rising Spiral Slash (48 dmg, Y=+0.85, consumes extracts). |
| **RMB Aerial** | Jumping Slash (fall attack with damage via blastInFront). |
| **Shift+RMB** | Dodge Slash (backward -0.8, up +0.3). |
| **RMB + Red (combo≥1)** | Leaping Slash (gap closer, forward 0.9, up 0.35, auto-starts charge). |
| **RMB + Red (overhead ready)** | Overhead Smash takes priority. S2C animation. |
| **RMB + Red (default)** | Wide Sweep + starts charge. Focus Mode recalls kinsect. |
| **Vault** | 12 stamina. Y=0.8 (1.0 with White). aerialTicks=40. Bounce reset. |

### insect_glaive.json — Updated
Added motion values: `leaping_slash: 1.1`, `dodge_slash: 0.85`, `descending_slash: 1.4`, `descending_thrust: 1.5`, `strong_descending_slash: 1.6`, `rising_spiral_slash: 2.0`. Added `chargeMaxTicks: 40`, `chargeThresholdTicks: 20`. Added animation series for new attacks.

---

## Phase 3 — Bugfixes & Polish (Feb 9, Session 1)

### Problem 1: RMB Charge Not Working
**Root Cause**: `ClientForgeEvents.java` sent `WEAPON_ALT true` on RMB press but NEVER sent `WEAPON_ALT false` on release. The server-side charge release handler (`!pressed`) was unreachable.

**Fix** — `ClientForgeEvents.java`:
- Added `igRmbDown` and `igChargeSent` state variables
- Added `handleInsectGlaiveRmbInput()` method (called every client tick):
  - Tracks RMB hold state
  - **Suppresses BC auto-spam**: `keyUse.setDown(false)` while held (follows LongSword pattern)
  - **Sends release**: `WEAPON_ALT false` packet when RMB goes from down→up
- Updated `onUseKey` IG handler to set `igRmbDown = true` on initial press

### Problem 2: Rising Spiral Slash No Upward Movement
**Fix**: Added `player.setDeltaMovement(x * 0.3, 0.85, z * 0.3)` + `hurtMarked = true` before the blast, so the player launches upward during the finisher.

### Problem 3: HUD Too Verbose
**Fix**: Simplified `renderAttackHud()` IG section from 6+ verbose lines to 3-4 clean lines:
- `LMB: Combo (Rising > Reaping > Double)`
- `RMB: Hold=Charge | Shift+RMB=Dodge` (context-sensitive with Red)
- Shows `Charging... (Xs) Release RMB!` or `Finisher Ready! Press RMB` status

---

## Phase 4 — Feature Completion (Feb 9, Session 2)

### New Feature: Tornado Slash (intermediate charge)
- Partial charge release (10-19 ticks) → `tornado_slash` action
- MV 1.3, 14-tick action window
- blastInFront(3.5, 16 dmg)
- **Re-chainable**: sets `insectCharging = true` again so player can hold for Descending Slash
- Added motion value and animation series to `insect_glaive.json`

### New Feature: LMB+RMB Instant Descending Slash
- **Client**: `handleInsectGlaiveRmbInput()` detects both `lmbDown && rmbDown` simultaneously
- Sends `CHARGE true` as signal (reuses existing action type)
- Suppresses both `keyAttack` and `keyUse` to prevent BC interference
- **Server**: New `CHARGE` handler in `handleInsectGlaive()`:
  - Ground → instant `descending_slash` (16 dmg, slightly less than charged 22)
  - Air → instant `descending_thrust` (14 dmg, slightly less than charged 18)
  - Still triggers finisher chain with Triple Up

### New Feature: Backward Vault i-frames
- Shift+SPECIAL → backward direction vault
- `Vec3 back = lookAngle.scale(-0.6)` for backward movement
- `combatState.setDodgeIFrameTicks(10)` for 10 i-frames
- Same vault height (0.8 or 1.0 with White)

### New Feature: White Extract Jump Boost
- In `InsectGlaiveEvents.java`, detects regular jump (positive Y velocity 0.38-0.50, not aerial)
- Adds +0.1 Y velocity for ~25% higher regular jumps
- Only triggers once per jump (velocity range check)

### New Feature: Charge Bar HUD (Center)
- Orange → Red fill bar during charge (below extract bars)
- "Charging" label at <20 ticks, "MAX" at ≥20 ticks
- Color transitions from orange (0xFFFF9800) to red (0xFFFF5722) when ready

### New Feature: Finisher Ready Indicator
- Flashing "FINISHER READY" text (alternates gold/orange every 5 ticks)
- Shown below charge bar when `finisherStage > 0`

### New Feature: Bounce Level Indicator
- "Vault Lv.X" text above extract bars during aerial with bounce > 0
- Cyan color (0xFF80DEEA) for visibility

### New Feature: Aerial Slash Damage
- `aerial_slash` (RMB air) now calls `blastInFront(3.0, 12 dmg)` — previously had no damage hit

### HUD Action Label Updates
- Added `tornado_slash → "Tornado Slash"`, `descending_thrust → "Descending Thrust"`, `charging → "Charging..."` to action label resolver

---

## Files Changed — Complete List

| File | Changes |
|:---|:---|
| `PlayerWeaponState.java` | +3 fields (bounceLevel, charging, chargeTicks) with getters/setters/serialization/copyFrom |
| `InsectGlaiveEvents.java` | Complete rewrite: buff system, charge ticking, jump boost, knockback resist, aerial/bounce/finisher state |
| `WeaponActionHandler.java` | `handleInsectGlaive()` rewrite: charge release (tornado/descending), LMB+RMB instant, aerial bounce, leaping/dodge slash, finisher chain, backward vault i-frames |
| `ClientForgeEvents.java` | +`igRmbDown`/`igChargeSent` tracking, +`handleInsectGlaiveRmbInput()` (hold/release, BC suppression, LMB+RMB detection), updated `onUseKey` IG handler |
| `WeaponHudOverlay.java` | Simplified attack hints, +charge bar, +finisher indicator, +bounce level, +action labels, removed verbose kinsect key hints |
| `insect_glaive.json` | +motion values (leaping, tornado, dodge, descending, spiral), +animation series, +charge config |
| `Insect_Glaive.md` | Complete rewrite reflecting current implementation status |

---

## Architecture Decisions

### Why reuse CHARGE action for LMB+RMB?
Adding a new `WeaponActionType` requires changes to the enum, serialization, and packet handling. Since IG doesn't use `CHARGE` for anything else (it uses `WEAPON_ALT` with hold/release tracking instead), reusing `CHARGE` as the "instant descending slash" signal is clean and avoids touching shared infrastructure.

### Why track RMB in tick handler instead of onUseKey?
`onUseKey` fires once on press. The charge mechanic needs continuous hold detection and precise release timing. The tick handler (`handleInsectGlaiveRmbInput`) runs every frame, allowing:
- Suppression of BC auto-attacks every tick (`keyUse.setDown(false)`)
- Precise release detection (was down, now up → send false)
- LMB+RMB simultaneous detection on the same tick

### Why Tornado Slash as intermediate charge?
In MH Wilds, releasing charge early produces Tornado Slash, which can chain into another charge for Descending Slash. This creates a "partial charge → full charge" decision tree that rewards skill. The 10-19 tick window gives players a meaningful choice between quick Tornado and committed Descending.

### Why backward vault uses dodgeIFrameTicks?
The codebase already has `PlayerCombatState.dodgeIFrameTicks` with tick-down logic in `WeaponStateEvents` and damage cancellation in `LivingAttackEvent`. Reusing this system is consistent with how LongSword foresight slash, Dual Blades demon dodge, and Magnet Spike magnetic repel grant i-frames.

---

## Remaining Work

See [Insect_Glaive.md §7 Remaining Roadmap](weapons/Insect_Glaive.md) for the prioritized list. Key items:
1. ~~**Kinsect Mark Target + Powder System**~~ ✅ Implemented (Phase 5)
2. **Focus Thrust: Leaping Strike** (wound mechanic integration)
3. **Red Extract moveset changes** (animation branching)
4. **GeckoLib Kinsect Model** (art asset)
5. **Kinsect Charge/Boost** (pierce-through multi-extract)
6. **Kinsect Fire** (long-range projectile mark)

---

## Phase 5 — Kinsect Depth: Mark Target & Powder System (Feb 9)

### Work Done

#### New Files Created
- **`KinsectPowderCloudEntity.java`** — Stationary powder cloud entity with 4 types:
  - **Blast** (type 1): 15 damage explosion on detonation, orange-red particles
  - **Poison** (type 2): 200-tick poison effect, purple particles
  - **Paralysis** (type 3): 100-tick slowness + mining fatigue, yellow particles
  - **Heal** (type 4): 4 HP heal + regeneration to nearby players, green particles
  - Each cloud lives 300 ticks (15 seconds), uses `DustParticleOptions` for colored idle particles
  - Detonated by player attacks within 2.5 block radius, applies effects in 3.5 block radius
  - Detonation spawns burst particles + poof effects

- **`KinsectPowderCloudRenderer.java`** — Minimal renderer (powder clouds are entirely particle-based)

#### Modified Files

- **`KinsectEntity.java`** — Major additions:
  - `powderType` field configured from KinsectItem, synced via `POWDER_TYPE_DATA` EntityDataAccessor
  - `markMode` flag for mark-target launches
  - `launchToMark()` method: launches kinsect + sets mark on owner's PlayerWeaponState (600 ticks / 30 seconds)
  - `trySpawnPowderTrail()`: spawns powder clouds every 3 blocks along flight path
  - `spawnPowderCloud()`: creates KinsectPowderCloudEntity at position
  - Auto-attack during hover: attacks marked target every 30 ticks (1.5s) for 60% damage, spawns powder at target every 60 ticks
  - Hover position follows marked target (dynamic tracking)

- **`KinsectItem.java`** — Added `powderType` field (int, 0-4) with getter and backwards-compatible constructor (defaults to Blast=1)

- **`PlayerWeaponState.java`** — Added 3 new fields:
  - `kinsectPowderType` (int): powder type synced from KinsectItem
  - `kinsectMarkedTargetId` (int): entity ID of marked target (-1 if none)
  - `kinsectMarkedTicks` (int): remaining ticks on mark timer
  - All fields: getters/setters, copyFrom, serializeNBT, deserializeNBT

- **`InsectGlaiveEvents.java`** — Added:
  - Mark timer tick-down (clears mark when timer reaches 0)
  - Powder cloud detonation on player attacks: detects nearby KinsectPowderCloudEntity within range and calls `detonate()` when player performs an IG attack (actionKeyTicks >= 8, excludes utility actions)
  - `detonatePowderCloudsNearby()` helper method

- **`WeaponActionHandler.java`** — Updated `handleKinsectLaunch()`:
  - If player holds Shift while launching kinsect at a target → calls `launchToMark()` instead of `launchTo()`
  - Sets `kinsect_mark` action key (12 ticks) for mark launches

- **`KinsectRenderer.java`** — Added:
  - Powder-colored dust particle trail behind kinsect during flight (every 2 ticks)
  - Enchant particles for mark mode glow indicator (every 4 ticks)

- **`MHWeaponsItems.java`** — Registered `KINSECT_POWDER_CLOUD` entity type (0.5×0.5, fire immune)

- **`ClientModEvents.java`** — Registered `KinsectPowderCloudRenderer` for the powder cloud entity

- **`WeaponHudOverlay.java`** — Added:
  - Powder type indicator (● symbol + name, colored by type) below extract bars
  - MARKED flashing indicator when target is marked
  - Attack hint line showing mark instructions or mark status
  - Action labels for `kinsect_harvest`, `kinsect_recall`, `kinsect_mark`, `dodge_slash`

### Architecture Decisions

#### Why particle-based powder clouds?
Powder clouds are meant to be visually distinctive but lightweight. Using `DustParticleOptions` with per-type RGB colors gives each powder a unique visual identity without requiring custom textures or models. The entity's `tick()` handles particle spawning client-side, while server-side handles lifetime and detonation logic cleanly.

#### Why detonation via attack proximity?
In Monster Hunter, Kinsect Powders explode when hit by the player's attacks. We detect this by checking for active IG attack actions (actionKeyTicks >= 8) and scanning for nearby powder clouds. This is cheaper than per-entity collision checks and aligns with the existing action system.

#### Why mark lasts 30 seconds?
MH Wilds marks are long-lasting to allow sustained kinsect auto-attack damage. 600 ticks (30s) gives enough time for meaningful DPS contribution without being permanent. The kinsect attacks every 1.5s for 60% of its base damage — weaker than direct hits but passive income.
