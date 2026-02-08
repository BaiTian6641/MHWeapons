# Light Bowgun (LBG) 🔫 — Detailed Implementation Plan

**Overview:**
The Light Bowgun is a fast, mobile ranged weapon that fires specialized ammo types with varying behavior (normal/pierce/shot/pellet/elemental/status) and supports rapid-fire modes and attachments (mods) that change magazine size, recoil, and fire patterns.

---

## 1) Core mechanics
- Ammo types: Normal (1/2/3), Pierce, Spread/Pellet, Elemental, Status (Poison/Paralysis/Sleep), Sticky, Sticky-explosive (varies by generation)
- Magazine / Reload mechanics: LBGs have magazine capacities and reloading; rapid-fire is implemented by firing multiple shots per input at reduced damage per shot depending on ammo type
- Mods / Attachments: Mods alter recoil, stability, reload speed, rapid-fire capability, and add specialized ammo (e.g., Wyvernheart in HBG/HBG exclusive)
- Mobility & Aiming: Light Bowgun emphasizes movement—hip-fire and aimed fire (ADS) with modified accuracy/spread and uptime
- Special tools: Some LBG play styles use clustering or support ammo for team utility (traps, coatings – bows only) and turret-like guns for suppression in some generations

---

## 2) Mapping to Better Combat (BC)
- Projectile System
  - Extend BC to support projectile definitions: `AmmoType` with attributes (speed, gravity, spread, pierceCount, areaOfEffect, travelLifetime, motionValues, statusChance).
  - Spawn projectiles on server authoritative side with client-requested intent; validate ammo availability and server spawns projectiles to compute hits and apply damage.
- Magazine & Reload
  - Implement `MagazineComponent` that tracks ammo pools per weapon instance, supports partial/full reloads, and handles rapid-fire consumption rates.
- Rapid-Fire & Fire Modes
  - `FireMode` component to define per-weapon/attachment rapid-fire patterns (burst counts, fire interval, recoil per-shot) and optionally overheat rules/cooldowns.
- Aiming & Spread
  - Add `AimState` that reduces spread when aiming (ADS) and increases stability; account for movement penalties and in-air penalties.
- Attachments & Mods
  - Implement `Attachment` modifier system to change ammo lists, magazine sizes, recoil multipliers, and projectile properties; data-driven for easy mod packs.
- Hit Validation & Hitzones
  - Projectiles should use BC's hitzone damage computation for consistent interaction with monster hitboxes, armor, and status mechanics.

---

## 3) GeckoLib — Models & Animations
- Animations: `idle`, `draw`, `reload`, `fire_hip`, `fire_ads`, `rapid_fire_loop`, `switch_ammo_animation`, `jam_recover` (if jam mechanics are used)
- VFX: muzzle flash variants, tracer/projectile visualization, impact sparks and elemental VFX per ammo
- UI: small HUD overlay for magazine count, current ammo type, remaining rapid-fire bursts/overheat indicator

---

## 4) Data & Config (tunable)
- `data/mhweapons/light_bowgun/base.json` sample fields:
  - `ammoTypes`: {
      `normal1`: { `baseDamage`, `speed`, `gravity`, `pierce`:0, `spread`:0.02, `motionValue` },
      `pierce1`: { `pierce`: 3, `baseDamage`, `speed`, `motionValue` },
      `pellet`: { `pelletCount`, `pelletSpread`, `perPelletDamage` },
      `sticky`: { `status`: "sticky", `explosionOnHit` }
    }
  - `magazine`: { `capacityPerAmmoType`, `reloadTimeMs` }
  - `fireModes`: { `single`, `rapid`, `charged` } with intervals & consumption rules
  - `attachments`: list of modifiers to change values above
- Rulesets: allow generation variants (`mhw`, `mhrise`, `mhwilds`) to toggle magazine/reload behavior, rapid-fire availability, and special ammo inclusion

---

## 5) Implementation Tasks (small & testable)
1. Data & Schema (S)
   - Create `data/mhweapons/light_bowgun/default.json` and `mhwilds.json` example rulesets describing ammo properties and magazine sizes.
2. BC: Projectile system (M)
   - Implement `AmmoProjectile` server-side entity with computed trajectory, hit detection, damage application, and status application. Include spread and randomization rules for pellet ammo.
3. Gameplay: `MagazineComponent` & `FireMode` (M)
   - Track ammo counts, implement reload action and rapid-fire logic, handle mod attachment application.
4. Item: `LightBowgunItem` (M)
   - Bind inputs: fire, alt-fire/aim, reload, cycle ammo; read data for magazine and ammo types.
5. GL: Animations & VFX (M)
   - Create base firing/reload animations and per-ammo VFX for fast testing.
6. Tests: Unit & integration (M)
   - Projectile hit accuracy tests, pellet spread consistency checks, magazine/reload edge cases (reload cancel, mid-air reload), rapid-fire timing stability under server validation.
7. Balance: tuning & playtest (S)
   - Tune base damages, projectile travel-time adjustments, and magazine values per generation ruleset.

---

## 6) Networking & Security Considerations
- Server-authoritative projectile spawning & hit validation to prevent client spoofing and cheating.
- Limit projectile creation rate and enforce server-side cooldowns/consumption checks.
- Use prediction for client VFX (spawn local tracer on fire input) but reconcile with server authoritative projectile IDs and hit messages.

---

## 7) Cross-Mod & Integration Notes
- Provide API to add new ammo types and attachments via JSON so other mods can introduce custom ammo with status effects or unique behavior.
- Damage & status application goes through BC's hitzone/damage pipeline so armor and other mods interact naturally.

---

## 8) Tests & Acceptance Criteria
- Projectiles travel and hit correctly at various ranges and hitzones; pellet spreads match configured distributions.
- Magazine & reload mechanics prevent firing when empty, support mid-reload cancellation, and handle rapid-fire consumption correctly.
- Attachments dynamically change weapon behavior on server and sync to clients.

---

## 9) Notes & Sources
- Fandom page redirected during automated fetch; canonical LBG behavior (ammo types, rapid-fire, magazines) is validated from community guides and the Monster Hunter series' known mechanics. If you want, I can generate example `data/mhweapons/light_bowgun/default.json` rulesets next.

---

Would you like me to generate the sample `data/mhweapons/light_bowgun/default.json` and a `mhwilds.json` variant, or scaffold the `AmmoProjectile` and `MagazineComponent` server stubs next?