# Insect Glaive (IG) 🐝 — Technical Implementation Plan

> **Source**: [MH Wilds Official Manual — Insect Glaive](https://manual.capcom.com/mhwilds/en/steam/page/4/11)

**Overview:**
A mobile, pole-based weapon that commands a symbiotic insect (Kinsect) to collect Extracts from monsters that power you up. Use the glaive to vault and perform midair attacks. The IG has no guard — its defense comes from mobility, aerial evasion, and buff uptime.

---

## 1. Current Implementation Status

### ✅ Implemented
| Component | File(s) | Notes |
|:---|:---|:---|
| KinsectEntity | `common/entity/KinsectEntity.java` | 3-state machine (FLYING, HOVERING, RETURNING). Homing flight, hover orbiting, extract collection via hit-height heuristic. Configures speed/range/damage from offhand `KinsectItem`. |
| KinsectItem | `item/KinsectItem.java` | Offhand item with `maxExtracts`, `speed`, `range`, `damage`, `damageType`, `element`. Falls back to config defaults. |
| Extract Buff System | `events/combat/InsectGlaiveEvents.java` | Per-tick attribute modifiers: White → +20% speed + jump boost. Red-only → +5% ATK. Red+White → +10% ATK. Triple → +15% ATK. Orange → +10 armor (+15 with W+O). Orange → knockback resistance (0.5, or 1.0 with triple). |
| White Jump Boost | `events/combat/InsectGlaiveEvents.java` | White extract boosts regular jump velocity by +0.1 Y. Also boosts vault height (0.8 → 1.0). |
| Knockback Resistance | `events/combat/InsectGlaiveEvents.java` | Orange extract → `KNOCKBACK_RESISTANCE` attribute modifier (0.5 normal, 1.0 triple up). |
| White+Orange Defense | `events/combat/InsectGlaiveEvents.java` | W+O or Triple → +15 armor (vs +10 for Orange alone). |
| PlayerWeaponState fields | `capability/player/PlayerWeaponState.java` | `insectRed/White/Orange`, `insectExtractTicks`, `insectAerialTicks`, `insectComboIndex/Tick`, `insectTripleFinisherStage/Ticks`, `insectAerialBounceLevel`, `insectCharging`, `insectChargeTicks`, `kinsectEntityId`. |
| Ground Combos (LMB) | `combat/weapon/WeaponActionHandler.java` | LMB chain: `rising_slash` → `reaping_slash` → `double_slash` (cycles 0→1→2→0 within combo window). |
| Wide Sweep / Overhead (RMB) | `combat/weapon/WeaponActionHandler.java` | RMB idle → `wide_sweep`. RMB after combo≥2 → `overhead_smash`. Both send S2C animation packet. |
| Dodge Slash | `combat/weapon/WeaponActionHandler.java` | Shift+RMB → `dodge_slash`. Backward movement (-0.8) + upward (0.3). Evasive repositioning. |
| Leaping Slash | `combat/weapon/WeaponActionHandler.java` | RMB during combo (index≥1, Red) → `leaping_slash`. Gap closer (forward 0.9, up 0.35). Auto-starts charge. |
| Charge System | `combat/weapon/WeaponActionHandler.java`, `ClientForgeEvents.java` | Hold RMB with Red → charge builds up (ticked in InsectGlaiveEvents, capped at 40). Release: 10-19t → Tornado Slash, ≥20t → Descending Slash/Thrust. |
| Tornado Slash | `combat/weapon/WeaponActionHandler.java` | Partial charge release (10-19 ticks) → `tornado_slash`. Spinning attack (MV 1.3). Can chain into another charge. |
| Descending Slash | `combat/weapon/WeaponActionHandler.java` | Full charge release (≥20 ticks, ground) → `descending_slash` (MV 1.4). Heavy overhead. With triple up → enables finisher chain. |
| Descending Thrust | `combat/weapon/WeaponActionHandler.java` | Full charge release (≥20 ticks, air) → `descending_thrust` (MV 1.5). Dive attack (Y=-1.2). |
| LMB+RMB Instant Slash | `ClientForgeEvents.java`, `WeaponActionHandler.java` | LMB+RMB simultaneous with Red → instant `descending_slash`/`descending_thrust` (slightly less damage). Uses CHARGE action type as signal. |
| RMB Hold/Release Tracking | `ClientForgeEvents.java` | `handleInsectGlaiveRmbInput()`: tracks `igRmbDown`, sends `WEAPON_ALT false` on release, suppresses BC auto-attacks via `keyUse.setDown(false)`. |
| Triple Finisher Chain | `combat/weapon/WeaponActionHandler.java` | Descending Slash (with Triple Up) → sets `finisherStage=1`. Next RMB → `rising_spiral_slash` (MV 2.0, 48 dmg blast). Launches player upward (Y=0.85). Consumes all extracts. 60-tick window. |
| Aerial System | `combat/weapon/WeaponActionHandler.java` | SPECIAL → Vault (12 stamina, Y=0.8/1.0 with White). LMB air → `aerial_advancing_slash`. RMB air → `aerial_slash` (with damage). Dodge air → `midair_evade` (20 stamina, 6 i-frames). |
| Backward Vault | `combat/weapon/WeaponActionHandler.java` | Shift+SPECIAL → backward launch + 10 i-frames. |
| Vaulting Dance (Bounce) | `combat/weapon/WeaponActionHandler.java` | LMB air hit → bounce (re-vault). Level 0→1→2. +0.15 speed, +20% damage, +0.1 Y lift per level. Refreshes aerial ticks. Resets on landing. |
| Kinsect Combos (Focus Mode) | `combat/weapon/WeaponActionHandler.java` | `triggerKinsectComboAttack()`: Focus Mode LMB auto-sends kinsect to target in front. |
| Kinsect Networking | `network/packet/KinsectLaunchC2SPacket.java` | Launch carries `targetEntityId` + `targetPos`. Recall via `WeaponActionC2SPacket(KINSECT_RECALL)`. |
| Kinsect Renderer | `client/render/KinsectRenderer.java` | Placeholder: colored wool block scaled 0.35×. |
| HUD — Extract/Charge/Finisher/Bounce | `client/ui/WeaponHudOverlay.java` | Center: R/W/O bars, timer, charge bar (orange→red), "FINISHER READY" flash, "Vault Lv.X". Upper-right: simplified attack hints with context-sensitive RMB info. |
| Weapon Data JSON | `data/mhweaponsmod/weapons/insect_glaive.json` | Full motion values for all attacks. Animation series for all combos. |
| BC Attributes JSON | `data/mhweaponsmod/weapon_attributes/insect_glaive.json` | Parent `bettercombat:claymore`, two-handed, FORWARD_BOX hitbox, range 1.1. |
| Kinsect: Mark Target | `entity/KinsectEntity.java`, `WeaponActionHandler.java` | Shift+Launch marks target. Kinsect auto-attacks marked enemy every 1.5s. Mark lasts 30s. HUD shows MARKED indicator. |
| Kinsect: Powder System | `entity/KinsectPowderCloudEntity.java`, `entity/KinsectEntity.java` | Kinsect leaves powder clouds along flight path and at hit location. Types: Blast (15 dmg explosion), Poison (200t poison), Paralysis (100t slow), Heal (4 HP + regen). Player attacks detonate nearby clouds. |
| Kinsect: Powder Trail Particles | `client/render/KinsectRenderer.java` | Colored DustParticle trail behind kinsect matching powder type. Mark mode shows enchant particles. |
| Kinsect Powder Cloud Renderer | `client/render/KinsectPowderCloudRenderer.java` | Particle-only renderer. Idle: colored dust + ambient particles. Detonation: burst + poof particles. |
| HUD — Powder & Mark | `client/ui/WeaponHudOverlay.java` | Powder type name+color below extract bars. MARKED flashing indicator when target marked. Attack hints show mark instructions. |

### ❌ Not Yet Implemented
| Feature | Priority | Notes |
|:---|:---|:---|
| Kinsect: Fire (long-range mark) | MEDIUM | Hold Aim + R1 fires long-range projectile mark. |
| Kinsect Charge / Boost | MEDIUM | Hold harvest → charged kinsect pierces through gathering multiple extracts. |
| Focus Thrust: Leaping Strike | HIGH | Focus + R1 → wound thrust → kinsect pierces → collects R+W+O. |
| Kinsect Clash | LOW | Kinsect intercepts incoming attack. Defensive mechanic. |
| Extract Clouds | LOW | Kinsect creates AoE extract zones. |
| Red-buff moveset changes | MEDIUM | Red extract alters attack animations/hit counts. |
| GeckoLib Kinsect Model | LOW | Replace wool block with proper insect model. |
| Separate Extract Timers | LOW | Independent timers per color. |

---

## 2. Core Mechanics Architecture

### 2.1. The Kinsect System

**States** (`KinsectEntity.KinsectState`):
| State | Description | Behavior |
|:---|:---|:---|
| `FLYING` | Moving to target | Homing at `flySpeed`. On arrival → collect extract → HOVERING. |
| `HOVERING` | Orbiting hit point | Circles (radius 0.8, 0.15 rad/tick). Stays until recalled. |
| `RETURNING` | Returning to owner | Flies at `returnSpeed`. On arrival → applies extracts → discards entity. |

**Extract Collection** (`resolveExtractColor()`):
| Hit Position | Color |
|:---|:---|
| ≥ 75% height | Red (Attack) |
| ≤ 40% height | White (Speed) |
| 40%–75% | Orange (Defense) |

### 2.2. The Charge System ✅

With **Red Extract** active:
1. **Hold RMB** → charge ticks up (InsectGlaiveEvents, capped at 40).
2. **Release RMB**: 10-19t → Tornado Slash → re-chain charge. ≥20t → Descending Slash/Thrust.
3. **LMB+RMB** → instant Descending Slash/Thrust (slightly reduced damage).
4. **Triple Up finisher**: Descending Slash → [60t window] → RMB → Rising Spiral Slash (consumes extracts).

### 2.3. Aerial Combat ✅

- **Vault**: SPECIAL (12 stamina). Y=0.8 (1.0 w/ White). aerialTicks=40.
- **Backward Vault**: Shift+SPECIAL. Backward + 10 i-frames.
- **Vaulting Dance**: LMB air hit → bounce off target → power level +1 (max 2). Refreshes air time.
- **Midair Evade**: Dodge air (20 stamina, 6 i-frames).

---

## 3. Attack Move List

### Ground
| Move | Input | Key | MV | Status |
|:---|:---|:---|:--|:---|
| Rising Slash | LMB (idle) | `rising_slash` | 1.0 | ✅ |
| Reaping Slash | LMB (combo 1) | `reaping_slash` | 1.05 | ✅ |
| Double Slash | LMB (combo 2) | `double_slash` | 1.1 | ✅ |
| Overhead Smash | RMB (combo≥2) | `overhead_smash` | 1.25 | ✅ |
| Wide Sweep | RMB (idle) | `wide_sweep` | 0.95 | ✅ |
| Leaping Slash | RMB (combo≥1, Red) | `leaping_slash` | 1.1 | ✅ |
| Dodge Slash | Shift+RMB | `dodge_slash` | 0.85 | ✅ |
| Tornado Slash | Release charge 10-19t | `tornado_slash` | 1.3 | ✅ |
| Descending Slash | Release charge ≥20t | `descending_slash` | 1.4 | ✅ |
| Rising Spiral Slash | RMB (finisher) | `rising_spiral_slash` | 2.0 | ✅ |
| Instant D. Slash | LMB+RMB (Red) | `descending_slash` | ~1.4 | ✅ |

### Aerial
| Move | Input | Key | MV | Status |
|:---|:---|:---|:--|:---|
| Vault | SPECIAL | `vault` | 0.8 | ✅ |
| Backward Vault | Shift+SPECIAL | `vault` | 0.8 | ✅ (+i-frames) |
| J. Advancing Slash | LMB (air) | `aerial_advancing_slash` | 1.15 | ✅ (+bounce) |
| Jumping Slash | RMB (air) | `aerial_slash` | 1.2 | ✅ |
| Descending Thrust | Charge release (air) | `descending_thrust` | 1.5 | ✅ |
| Midair Evade | Dodge (air) | `midair_evade` | — | ✅ |

### Kinsect
| Move | Input | Key | Status |
|:---|:---|:---|:---|
| Harvest Extract | Launch key | `kinsect_harvest` | ✅ |
| Recall | Recall key | `kinsect_recall` | ✅ |
| Focus Combo | LMB (Focus) | auto | ✅ Partial |
| Mark Target | Shift+Launch | `kinsect_mark` | ✅ |
| Powder Trail | Auto (flight) | auto | ✅ |
| Kinsect Fire | — | — | ❌ |
| Kinsect Boost | — | — | ❌ |

---

## 4. Remaining Roadmap

### Phase 1 — Kinsect Depth ✅ (Mostly Complete)
1. ~~Kinsect Mark Target + auto-attack~~ ✅
2. ~~Kinsect Powder System (entities, status effects)~~ ✅
3. Kinsect Charge/Boost (pierce through)
4. Kinsect Fire (long-range mark)

### Phase 2 — Focus & Advanced
5. Focus Thrust: Leaping Strike (wound mechanic)
6. Red Extract moveset changes

### Phase 3 — Polish
7. GeckoLib Kinsect Model
8. Kinsect Clash (defensive)
9. Extract Clouds (AoE)
10. Separate Extract Timers
11. Animation Polish

---

## 5. References
- [MH Wilds Official Manual — Insect Glaive](https://manual.capcom.com/mhwilds/en/steam/page/4/11)
- [MH Wilds Changes Summary](../weapons/mhwilds_changes_summary.md)
- [IG Implementation Session Log](../IG_Implementation_Summary.md)
