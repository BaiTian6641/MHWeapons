# Tonfa (Frontier) — Wilds Modernization Plan

**Overview:**
A dual-wielded, variable-range impact weapon rooted in *Monster Hunter Frontier*. It features high mobility, aerial combat capabilities, and a unique mode-switching mechanic.

**Wilds Modernization Concept:**
In *Monster Hunter Wilds*, combat emphasizes "Focus Mode" (aiming/wounding) and "Offset Attacks" (counters). The Tonfa is modernized as a **close-quarters aerial skirmisher** that uses specific modes to either build Stun/KO (Long Mode) or exploit Wounds (Short Mode).

**Scope Note:**
Tonfa is a Frontier legacy weapon and not part of Wilds' base roster. This plan aligns it to Wilds-style systems already defined in this mod (`FocusMode`, `Wounds`, `Offset`) rather than attempting a 1:1 canon port.

---

## 1. Core Mechanics

### 1.1. Dual Modes (State Machine)
The weapon operates in two distinct modes, toggled by the `Special Action` key (Default: `R` / Mouse 4). The transformation is physical: the handle slides along the shaft.

#### **A. Long Mode (Normal / Ryuu)**
*   **Form**: Standard T-shape tonfa.
*   **Style**: Grounded, rhythmic, heavy swings.
*   **Damage Profile**: High Motion Values, Standard **BLUNT** damage.
*   **Status**: Poor status application due to slow hit rate.
*   **Role**: Primary tool for neutral game, spacing, and building **KO/Stun**.
*   **Offset Attack**: **Reversal Smash**. Timed attack that absorbs a hit and counters with a heavy slam.

#### **B. Short Mode (Cadence / Reverse)**
*   **Form**: Retracted I-shape (flush against forearm, like separate pile bunkers).
*   **Style**: Aerial, rapid-fire, linear thrusts.
*   **Damage Profile**: Low Motion Values per hit, high frequency.
*   **Status**: Excellent **Status Application** (Poison/Para/Sleep) due to high hit count. (Standard 1-in-3 application rule, but with 3x more hits).
*   **Mechanic: Impact Conversion (Reversed Impact Mapping)**.
    *   *Logic*: Always deals `BLUNT` damage (keeps KO identity), but remaps impact effectiveness in Short Mode so poor blunt zones become better and strong blunt zones are normalized.
    *   *Restriction*: No sever-only utility (no tail-cut behavior), and remap output should be capped (recommended final multiplier cap: `1.20x`).
    *   *Wilds Synergy*: On **Wounded** parts, remap still resolves through wound-adjusted hitzone data from the global wound system for burst windows.
*   **Offset Attack**: **Jet Counter**. An aerial evade that, if timed against a hitbox, triggers an explosive bounce-off.

### 1.2. The Rhythm Gauge (Senryu / Coming and Going)
*   **Mechanic**: A gauge that fills as you land hits.
*   **Thresholds**: No "levels" (White/Red). Just a fluid bar that glows red at max.
*   **Effect**:
    1.  **Damage Scaling**: Linear damage buff based on gauge fill.
    2.  **EX State**: When flashing red (Max), unlocks **EX Finishers** (e.g., *Concentrated Smash* gains extra shockwaves).
*   **Decay**: Highly volatile. Depletes rapidly if combat stops. Promotes aggression.

### 1.3. Jet Propulsion
Tonfas are equipped with piston-driven thrusters.
*   **Double Jump**: Standard mid-air jump.
*   **Air Dash (Evade)**: Directional dash in mid-air (costs Stamina).
*   **Hover**: Holding `Jump` in air acts as a slow-fall/hover.
*   **Drawn Guard/Step**: Short guard window and dash-step while weapon is drawn, preserving the Frontier identity in a Wilds-like defensive rhythm.

---

## 2. Moveset & Wilds Mechanics

### 2.1. Basic Structure
| Input | Long Mode (Ground) | Short Mode (Aerial Focus) |
| :--- | :--- | :--- |
| **Left Click** | **Vertical Smash**: Main combo starter. | **Rising Smash**: Launches player into air. |
| **Right Click** | **Horizontal Sweep**: Wide AOE, low commitment. | **Aerial Flurry**: Multi-hit rapid punches. |
| **Left + Right** | **Concentrated Smash**: Charging pile-bunker attack. | **Dive Impact**: Rocket-powered ground slam. |

### 2.2. Focus Mode (Wilds Exclusive)
*   **Activation**: Hold `Focus Button` (L2 / Left Alt).
*   **Aiming**: 360° movement while facing target.
*   **Focus Strike: "Pinpoint Drill"**
    *   *Input*: `Attack` while focusing on a **Wound**.
    *   *Animation*: Player locks both Tonfas together, revs the thrusters, and drives them into the wound.
    *   *Effect*: Deals rapid multi-hit burst, then **destroys the Wound** for a heavy stagger/part-break event.
    *   *Cost*: Consumes Rhythm Gauge (recommended: 35-50%) so it is a committed finisher, not a spam tool.

### 2.3. Combo Path & Inputs

#### **Ground Combo (Long Mode)**
*   **Basic Series**:
    1.  **Thrust (I)**: `Left Click`. Heavy forward piston thrust. Closes distance.
    2.  **High Kick & Swing (II)**: `Left Click` after I. A roundhouse kick followed by a horizontal swing.
    3.  **Uppercut Finisher (III)**: `Left Click` after II. A heavy double-handed swing finishing with an upward lift. Reaches high heads/tails.
    *   *Loop*: Can link back to (I) or into a Charge.
*   **Charge Attack**:
    *   **Concentrated Smash**: Hold `Left + Right`. Releases a massive singular blow (Pile Bunker).
    *   *Properties*: Huge KO value, Hyper Armor while charging. Best wake-up attack.

#### **Aerial Combo (Short Mode)**
The Tonfa can stay airborne for extended periods, but should be bounded by stamina and action budgets to fit Wilds combat pacing.
*   **Launcher**:
    *   **Rising Smash**: `Left Click` from ground. Launches player into air.
*   **Aerial Pressure Loop**:
    1.  **Aerial Flurry (I)**: `Right Click` in air. Hits 3 times rapidly.
    2.  **Dash Cancel**: `Air Dash` (Space/Dodge + Direction). cancel the end lag of (I).
    3.  **Aerial Flurry (II)**: `Right Click` after Dash.
    *   (Repeat while Stamina allows; recommended max `air_actions` budget before forced descent).
*   **Finisher**:
    *   **Pile Bunker Dive**: `Left + Right` in air. Corkscrews down into the ground.

### 2.4. Offset Attacks (Clash/Counter)
*   **Ground Offset (Reversal Smash)**:
    *   Performing a *Concentrated Smash* right as a monster attacks triggers an **Offset**: neutralize damage, then release a boosted counter slam.
*   **Aerial Offset (Jet Counter)**:
    *   Performing an *Air Dash* through an attack hitbox triggers an explosion, bouncing the player high up and refreshing air actions.

### 2.5. Wilds Alignment Rules (for this mod)
1.  **Focus first**: Pinpoint Drill should only reach full value against active wounds.
2.  **Offset, not Power Clash**: Tonfa uses timing-based offsets; no Lance-style cinematic struggle.
3.  **Burst windows**: Short Mode spikes during wounds; Long Mode remains main neutral/stun control.
4.  **Aerial limits**: Air pressure is strong, but bounded by stamina + per-air-sequence action limits.

---

## 3. Technical Implementation Strategy

### 3.1. Class Structure
The implementation will likely extend `GeoWeaponItem` for animations.

*   **`TonfaItem`**:
    *   Handles mode switching logic.
    *   Stores `currentMode` in NBT or Capability.
*   **`TonfaStateCapability`**:
    *   `enum Mode { NORMAL, SHORT }`
    *   `float comboGauge`
    *   `boolean isFlying`
*   **`TonfaCombatEvents`**:
    *   **Impulse Conversion**:
        *   Hook into shared combat resolution path (`CombatReferee`/damage resolver), not ad-hoc per-hit patches.
        *   If `mode == SHORT` or `target.hasWound()`:
            *   Get `Sever` Hitzone and `Blunt` Hitzone via `PartEntity`.
            *   Calculate capped multiplier based on `Sever > Blunt` logic.
*   **`TonfaPacketHandler`**:
    *   Syncs "Mode Switch" animations and "Jet Particle" events to clients.

### 3.2. Data & Config (`data/mhweapons/tonfa/`)
```json
{
  "modes": {
    "long": { "range": 3.0, "knockback": 1.5, "exhaust": 10 },
    "short": { "range": 2.0, "impact_conversion": true, "exhaust": 5 }
  },
  "gauge": {
    "max_value": 100,
    "decay_rate": 2, // per tick
        "damage_buff_max": 1.2,
        "focus_strike_cost": 40
  },
  "flight": {
    "air_dash_stamina": 20,
        "hover_gravity_mod": 0.1,
        "max_air_actions": 6
    },
    "conversion": {
        "enabled": true,
        "max_multiplier": 1.2,
        "wound_priority": true
  }
}
```

### 3.3. Better Combat Integration
*   **Dual Wielding Check**: Tonfas are always dual wielded.
*   **Attack Profiles**:
    *   Two separate JSON profiles: `tonfa_long.json` and `tonfa_short.json`.
    *   Switching modes dynamically updates the weapon's `WeaponAttribute` container or swaps the profile ID in the capability.

---

## 4. Development Tasks
- [ ] **Asset Creation**:
    - [ ] `tonfa_long.geo` & `tonfa_short.geo` (or one model with bone animations).
    - [ ] Animations: `switch_to_long`, `switch_to_short`, `pinpoint_drill` (loop).
- [ ] **Code**:
    - [ ] `TonfaItem` class registration.
    - [ ] `ImpactConversion` logic in event bus.
    - [ ] `FocusMode` overlay integration for the Drill attack.
- [ ] **VFX**:
    - [ ] Thruster particles (smoke/fire) for Air Dash.
    - [ ] "Red Glow" shader for Max Rhythm Gauge.

*   **EX evasion**:
    *   In extreme styles, the evade is replaced by a jet-step that has I-frames and maintains combo continuity.

*- Summary for Implementation -*
**Prioritize**:
1.  **Mode Switch + Core Combo**: Normal (KO neutral) vs Short (wound-burst).
2.  **Jet Movement with Limits**: Double jump/Air Dash + stamina/action budget guardrails.
3.  **Gauge + Focus Strike Economy**: Fill/decay, buff scaling, and Drill cost.
4.  **Offset Timing**: Reversal Smash + Jet Counter integrated into shared Offset framework.
