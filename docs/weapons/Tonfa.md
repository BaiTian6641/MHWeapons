# Tonfa (Frontier) — Wilds Modernization Plan

**Overview:**
A dual-wielded, variable-range impact weapon rooted in *Monster Hunter Frontier*. It features high mobility, aerial combat capabilities, and a unique mode-switching mechanic.

**Wilds Modernization Concept:**
In *Monster Hunter Wilds*, combat emphasizes "Focus Mode" (aiming/wounding) and "Offset Attacks" (counters). The Tonfa is modernized as a **close-quarters aerial skirmisher** that uses specific modes to either build Stun/KO (Long Mode) or exploit Wounds (Short Mode).

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
*   **Mechanic: Impact Conversion (Hitzone Reversal)**.
    *   *Logic*: Deals `BLUNT` damage (KO), but calculates damage using the target's `SEVER` (cutting) hitzone if it is more vulnerable to cutting.
    *   *Wilds Synergy*: If a part is **Wounded**, Short Mode's conversion logic automatically targets the wounded soft hitzone value, creating massive burst potential.
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
    *   *Effect*: Deals massive tick damage (destroying sharpness). **Destroys the Wound** for a huge stagger/part break.

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
The Tonfa is unique in its ability to stay airborne indefinitely by consuming Stamina.
*   **Launcher**:
    *   **Rising Smash**: `Left Click` from ground. Launches player into air.
*   **Aerial Infinite Loop**:
    1.  **Aerial Flurry (I)**: `Right Click` in air. Hits 3 times rapidly.
    2.  **Dash Cancel**: `Air Dash` (Space/Dodge + Direction). cancel the end lag of (I).
    3.  **Aerial Flurry (II)**: `Right Click` after Dash.
    *   (Repeat I -> Dash -> II -> Dash until Stamina drains).
*   **Finisher**:
    *   **Pile Bunker Dive**: `Left + Right` in air. Corkscrews down into the ground.

### 2.4. Offset Attacks (Clash/Counter)
*   **Ground Offset (Reversal Smash)**:
    *   Performing a *Concentrated Smash* right as a monster attacks will "clash", neutralizing damage and automatically performing a max-charge release.
*   **Aerial Offset (Jet Counter)**:
    *   Performing an *Air Dash* through an attack hitbox triggers an explosion, bouncing the player high up and refreshing air actions.

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
        *   Hook into `LivingHurtEvent`.
        *   If `mode == SHORT` or `target.hasWound()`:
            *   Get `Sever` Hitzone and `Blunt` Hitzone via `PartEntity`.
            *   Calculate multiplier based on `Sever > Blunt` logic.
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
    "damage_buff_max": 1.2
  },
  "flight": {
    "air_dash_stamina": 20,
    "hover_gravity_mod": 0.1
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
1.  **Mode Switch**: Normal (Impact) vs Short (Hitzone optimization).
2.  **Jet Movement**: Double jump and Air Dash.
3.  **Gauge**: Simple fill-on-hit bar for damage multiplier.
