# Tonfa (MH Wilds Style) — Detailed Implementation Checklist (UPDATED)

This checklist tracks implementation status against the current codebase. ✅ = implemented in code; ⚙️ = partially implemented / needs assets; ⏳ = planned.

## Phase 1: Core Foundation & Assets

- [⚙️] **Item Registration**
    - [⏳] Create `TonfaItem` extending `GeoWeaponItem` (or mod base).
    - [⏳] Register item in `WeaponRegistry`.
    - [⏳] Set base stats (Damage, Attack Speed) — dynamic stats pending.

- [✅] **Capability System (State Machine)**
    - [✅] `Tonfa` capability fields added to `PlayerWeaponState` (`tonfaShortMode`, `tonfaComboGauge`, `tonfaAirActionCount`, `tonfaLastHitTick`, etc.) — implemented in `PlayerWeaponState.java`.
    - [✅] Capability attached & serialized (NBT) — serialization updated.
    - [⏳] `TonfaPacketHandler` for gauge/mode visual sync — not yet added (client uses capability read on tick).

- [⚙️] **Basic Assets**
    - [⏳] `tonfa.geo.json` / model(s) — not in repo.
    - [⏳] `tonfa_texture.png` — not in repo.
    - [⏳] GeckoLib animations (`transform`, `pinpoint_drill`) — not in repo.

## Phase 2: Combat System Integration (Better Combat)

- [⏳] **Weapon Attributes Data**
    - [⏳] `tonfa_long.json` / `tonfa_short.json` (Better Combat attack profiles) — missing; `weapon_attributes/tonfa.json` currently basic and should be split.

- [⏳] **Attack Animation Files**
    - [⏳] `tonfa_attack_long.json` / `tonfa_attack_short.json` — animation data not present.

- [⏳] **Dynamic Profile Switching**
    - [⏳] Hook in `TonfaItem` or attribute resolver to swap profiles by mode — TODO.

## Phase 3: Unique Mechanics Implementation

- [✅] **Mode Switching Logic**
    - [✅] Mode toggle implemented (`Special` action) — `TonfaHandler.setAction` + `PlayerWeaponState.setTonfaShortMode()`.
    - [🔧] Transform animation & sound not yet added (VFX/SFX TODO).

- [✅] **Jet Propulsion & Aerial Movement**
    - [✅] Double jump implemented (`handleDoubleJump`).
    - [✅] Mid-air dash with stamina cost implemented (`handleDodge`) and limited by `tonfaAirActionCount` (MAX_AIR_ACTIONS = 6).
    - [⚙️] Jet VFX/SFX not implemented yet.

- [✅] **Rhythm Gauge**
    - [✅] Accumulation implemented on hit (`TonfaCombatEvents.java` — per-action gains).
    - [✅] Decay implemented via `TonfaHandler.tickTonfa()` (uses `tonfaLastHitTick`).
    - [✅] Damage buff scaling implemented on hit (linear up to +20%).
    - [✅] HUD rendering added (`WeaponHudOverlay.java`): mode-colored gauge, EX flash, Air counter, contextual labels.

- [✅] **Impact Conversion (Short Mode)**
    - [✅] Reversed impact mapping implemented in `CombatReferee.resolveHitzoneMultiplier(...)` (capped at 1.2x and wound-aware).

## Phase 4: Wilds Alignment (Focus & Offset)

- [✅] **Focus Strike: "Pinpoint Drill"**
    - [✅] Input handling and execution implemented (`TonfaHandler` & `TonfaCombatEvents`).
    - [✅] **Gauge cost enforced**: Drill consumes 40% Rhythm Gauge.
    - [✅] Wound destruction handled in `TonfaCombatEvents`.
    - [⚙️] Pinpoint Drill animation + multi-hit timing refinement (tuning / visual polish pending).

- [✅] **Offset Attacks (Counters)**
    - [✅] Aerial Jet Counter (midair evade) implemented in `CombatReferee.performTonfaJetCounter()`.
    - [⚙️] Ground Reversal Smash registration (offset-window) — partial (charge action exists; explicit offset window hook TODO).

## Phase 5: Polish & Tuning

- [⚙️] **Visual Effects**
    - [⏳] Thruster particles for dash/jump: TODO
    - [⏳] Mode-specific impact particles: TODO
    - [⏳] EX gauge shader/glow: HUD shows pulse; in-world shader TODO

- [⚙️] **Sound Design**
    - [⏳] Transform, jet, and impact sounds: TODO

- [⚙️] **Config & Data Exposure**
    - [✅] `tonfa.json` (weapon data) updated with `gauge`, `flight`, and `conversion` blocks.
    - [⏳] Expose runtime tuning `tonfa_config.json` (external config) — TODO
    - [⏳] BetterCombat attribute split (`tonfa_long` / `tonfa_short`) — TODO

## Tests, QA & Next Steps
- [✅] Unit/compile: Code compiles after changes.
- [⏳] Playtesting checklist:
    - [ ] Verify EX finisher timings and damage scaling.
    - [ ] Test air-action budget edge cases (exhaustion + reset on land/jet counter).
    - [ ] Confirm Focus Strike consumes gauge & destroys wounds reliably.
    - [ ] Balance motion values / gauge gain per hit.

## Summary & Priority
1. ✅ Core code mechanics (gauge, mode, air dash, focus drill, impact conversion) — DONE.
2. ⚙️ Medium priority: Add BetterCombat attribute profiles + GeckoLib animations + VFX/SFX.
3. ⏳ Low priority: Full asset set, tuning, and config exposure for public mod settings.