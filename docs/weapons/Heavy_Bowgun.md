# Heavy Bowgun (HBG) 🔫 — Detailed Implementation Plan

**Overview:**
The Heavy Bowgun is the heavy-hitting, high-recoil counterpart to the Light Bowgun: high raw damage projectiles, siege (mounted) modes for sustained fire, heavier attachments, and often the ability to deploy a shield or mount for reduced recoil and increased stability. It trades mobility for firepower and versatility in ammo effects.

---

## 1) Core mechanics
- Ammo families: strong emphasis on high-damage shell/pierce/elemental/status ammo, clustering/pellet variants, and some generations include Wyvernheart-like hard-hitting special rounds.
- Siege / Mount mode: HBG can enter a static or braced firing stance (siege) that increases accuracy, reduces recoil, and enables sustained rapid-fire for certain ammo types at the cost of mobility.
- Reload & Magazine: HBG magazines and reload patterns vary by generation—some support rapid-fire belts or magazine-based consumption.
- Attachments: Mounting platforms, recoil reducers, stability mods, shield mounts, or specialized ammo modules change playstyle.
- Support roles: HBG can provide team utility via status/element ammo and suppression-like sustained fire.

---

## 2) Mapping to Better Combat (BC)
- Projectile System
  - Reuse `AmmoProjectile` entity (from LBG) but add heavier recoil and multi-hit/impact-phase descriptors; ensure high-damage projectiles are server-spawned and validated.
- Siege State
  - Implement `SiegeAction`/`BracedFireState` that reduces spread, recoil and enables `RapidSustainFire` patterns for specific ammo types.
  - Siege entry/exit must be server-validated and provide clear VFX/SFX to clients; optionally allow partial movement (pivoting) depending on ruleset.
- Shield Mount / Stabilizer
  - Provide optional `ShieldMountAction` that temporarily grants huge recoil/knockback reduction and sight-stabilization; treat as a toggle or dodge-to-enter depending on desired UX.
- Projectile Validation & Hitzones
  - Heavier projectiles may have travel-time, knockback, and multi-phase AoE (e.g., burst shells) — reuse BC hitzone calculations and ensure AoE damage is computed server-side.
- Magazine / Ammo Management
  - `MagazineComponent` tracks ammo types, counts, and supports magazine-specific reloads (full vs single-shell reload variations) and rapid-fire belt behavior.

---

## 3) GeckoLib — Models & Animations
- Animations: `idle`, `draw`, `aim_enter`, `aim_loop`, `reload_full`, `reload_shell`, `siege_enter`, `siege_fire_loop`, `shield_mount`, `jam_recover`.
- VFX: heavy muzzle flashes, tracer/projectile visualization for heavy rounds, charged shell explode VFX, siege vignette & screen shake on sustained fire.
- HUD: large magazine counter, ammo type indicator, siege status overlay, and turret-like reticle for braced/siege fire.

---

## 4) Data & Config (tunable)
- `data/mhweapons/heavy_bowgun/*.json` fields:
  - `ammoTypes`: { `shell_normal`: { `baseDamage`, `AoERadius`, `pierce`, `recoil` }, `pierce`, `cluster`, `sticky`, `wyvern` }
  - `magazine`: { `capacityPerAmmoType`, `reloadStyle`: "single"|"full", `reloadTimeMs` }
  - `siege`: { `accuracyMultiplier`, `recoilMultiplier`, `allowedAmmoRapidFire`: [], `movementPenalty` }
  - `attachments`: mods that alter `recoil`, `stability`, `magazineCapacity`, `siegeEnabled` flags
  - `safety`: `serverDamageCapPerShot`, `aoeServerValidation` toggles
- Rulesets: generation variants for siege availability, magazine behavior, and special high-damage rounds

---

## 5) Implementation Tasks (small & testable)
1. Data & Schema (S)
   - Create `data/mhweapons/heavy_bowgun/default.json` and a `mhwilds.json` variant if needed to capture generation differences.
2. BC: `SiegeAction` & `ShieldMountAction` (M)
   - Implement braced fire state and ensure rapid-fire loops are safe and server-authoritative.
3. Projectile: `HeavyAmmoProjectile` (M)
   - Add heavier physics, AoE handling, and multi-phase impacts; write server tests for AoE correctness across latencies.
4. Gameplay: `MagazineComponent` & reload styles (M)
   - Support single-shell reload and full-mag reloads; implement partial interrupts (reload cancel) and animations sync.
5. Item: `HeavyBowgunItem` (M)
   - Input bindings: fire, aim, reload, cycle ammo, enter siege, mount shield.
6. GL: Animations & HUD (M)
   - Placeholder animations for siege entry/exit and rapid fire loops; HUD elements for magazine and siege status.
7. Tests (M)
   - Projectile impact accuracy, AoE server validation, magazine/reload edge cases, and siege state transitions under simulated latency.

---

## 6) Networking & Security Considerations
- Server-authoritative projectile spawning & hit computation to avoid client-side spoofing.
- Enforce ammo consumption & rate limits server-side; reject or correct client-side prediction when mismatches occur.
- Avoid client-initiated AoE damage; all AoE resolution executed on the server and reconciled with clients.

---

## 7) Cross-Mod & Integration Notes
- Make attachments and ammo types data-driven so other mods can add new ammo or attachments via JSON files.
- Ensure damage/status application routes through BC's hitzone system for consistent interaction with armor and other mods.

---

## 8) Tests & Acceptance Criteria
- Siege mode provides correct accuracy/recoil mitigation and enables configured rapid-fire patterns only for allowed ammo types.
- Magazines and reload mechanics enforce ammo availability and prevent firing when empty; rapid-fire respects server-side cooldowns.
- Heavy projectile AoE and multi-phase hits are computed server-side and apply correct damage/hitzone logic.

---

## 9) Notes & Sources
- Fandom page blocked automated fetches; LBG/HBG canonical behaviors validated from community guides and domain knowledge. If you want, I can generate sample `data/mhweapons/heavy_bowgun/default.json` ruleset next.

---

Would you like me to generate sample `data/mhweapons/heavy_bowgun/default.json` (and a `mhwilds` variant) now, or scaffold `SiegeAction` and `HeavyAmmoProjectile` server-side stubs with unit tests?