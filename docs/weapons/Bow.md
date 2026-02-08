# Bow 🏹 — Detailed Implementation Plan

**Overview:**
The Bow is a versatile ranged weapon with charge levels, multiple shot types (Normal/Pierce/Spread/Dragonpiercer/Long/Scatter depending on generation), and coatings that add status or elemental modifiers. It relies on precise aiming, charge timing, and shot-type selection.

---

## 1) Core mechanics
- Charge levels: typically 3 charge levels that increase base damage & motion value and modify shot behavior (e.g., Pierce penetration at higher charges, Dragonpiercer special).
- Shot types: Normal (single projectile), Pierce (penetrates multiple targets/hitzones), Spread (pellet-like), Dragon Piercer (special piercing beam at full charge in some gens), Rapid/Full Burst modes in some generations
- Coatings: apply status or elemental effects to arrows (e.g., Poison, Paralysis, Sleep, Blast) and can be toggled or consumed per shot
- Mobility & Aiming: hip-fire vs aimed shots (ADS) trade accuracy for mobility; certain moves like Slinger integrations affect options
- Special attacks: sometimes include charged charge-skill attacks, sticky explosive arrows, or power coatings that change behavior

---

## 2) Mapping to Better Combat (BC)
- Projectile System
  - Use BC `AmmoProjectile` with charge-scaling (damage & speed), shot-type behaviour (pierce count, pellet spread), and optional beam-like Dragonpiercer handling (special ray trace attack computed server-side).
- Charge mechanic
  - Implement `BowChargeState` to track hold time and compute charge level on release; server authoritative for damage and special behavior (client-side prediction for visuals allowed).
- Coatings & Ammo Modifiers
  - `CoatingComponent` to apply consumable coatings that alter projectile `AmmoProjectile` properties (status chance, elemental damage, area effect on hit)
- Aiming/ADS state
  - `AimState` reduces spread & modifies projectile origin/trajectory; allow movement penalty config and stamina cost if desired.
- Hit Validation & Hitzones
  - Projectile hits are computed server-side using BC hitzone mechanics; ensure pierce/pellet multiple-hit logic uses hitzone-aware calculation.

---

## 3) GeckoLib — Models & Animations
- Animations: `idle`, `draw_charge_lvl1..3`, `aim_enter`, `aim_loop`, `release`, `charge_special`, `apply_coating`.
- VFX: arrow trails, impact particles, beam effect for dragonpiercer, coating visual on weapon strings and arrow tips.
- UI: charge meter overlay and current coating icon; aim reticle for ADS mode.

---

## 4) Data & Config (tunable)
- `data/mhweapons/bow/*.json` fields:
  - `chargeLevels`: timings and damage multipliers per level
  - `shotTypes`: definitions for `normal`, `pierce`, `spread`, `dragonpiercer`, each with `baseDamage`, `motionValues`, `pelletCount` or `pierceCount`, `spreadAngle`, `specialBehavior`
  - `coatings`: mapping names to effects ({status, elementalAdd, perShotConsumption})
  - `aim`: spread multipliers, movement speed modifiers, stamina cost
  - `rulesets`: per-generation flags for special shots (Dragonpiercer availability, rapid-fire behaviors, etc.)

---

## 5) Implementation Tasks (small & testable)
1. Data & Schema (S): create `data/mhweapons/bow/default.json` and generation variants (e.g., `mhwilds.json`) listing charge timings, shot types, and coatings.
2. BC: `BowChargeState` and `AmmoProjectile` support for shot types (M): implement dragonpiercer as server-side ray-trace special and pellet spread support.
3. Gameplay: `CoatingComponent` and HUD indicator (S)
4. Item: `BowItem` (M): bind draw/aim/release and coat application.
5. GL: models and charged draw animations (M)
6. Tests: verify charge levels, pellet hit distributions, pierce counts, dragonpiercer ray damage, coating status application, and aim-state spread reduction (M)

---

## 6) Network & Security
- Server-authoritative projectile spawning & damage application is mandatory to prevent client spoofing, especially for piercing and beam effects.
- Clients may show predictive VFX on draw/hold but server must confirm final projectile behavior.

---

## 7) Cross-Mod Notes
- Expose `shotTypes` and `coatings` as JSON-extensible so other mods can add custom arrows or coatings.
- Use BC hitzone pipeline for damage/status so armor, resistances, and other mods interact normally.

---

## 8) Acceptance Criteria
- Charge levels apply correct damage/motion values; Dragonpiercer & special shots behave per ruleset and are reliable.
- Coatings correctly modify projectile behavior and apply status effects with correct chances.
- Aiming reduces spread and changes projectile flight to be more accurate; HUD provides clear feedback.

---

## 9) Sources & Notes
- Fandom blocked automated fetch; core Bow mechanics are canonical and validated by community/guides. I can add sample `data/mhweapons/bow/default.json` and `mhwilds.json` rulesets next if you want.

---

Would you like sample bow rulesets created now or should I scaffold `BowChargeState` and `dragonpiercer` ray-trace handling first?