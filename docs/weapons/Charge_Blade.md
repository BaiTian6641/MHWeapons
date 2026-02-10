# Charge Blade (CB) — Technical Implementation Plan

**Overview:**
A complex, morphing weapon that oscillates between a mobile **Sword & Shield Mode** (building resources) and a heavy **Axe Mode** (expending resources). The loop involves attacking in Sword mode to build energy -> charging Phials -> using Phials in Axe mode for massive damage. 

Key Wilds features include **Power Axe Mode** (accessible via Perfect Guard/Wounds) and **Focus Strikes**.

Reference Manual: [Capcom MHWilds Guide](https://manual.capcom.com/mhwilds/en/steam/page/4/10)

## 1. Core Mechanics & Architecture

### 1.1. The Resource Loop (Phials)
*   **Sword Energy (Heat)**:
    *   Builds up with Sword attacks (Yellow -> Red -> Overheat).
    *   **Overheat**: If not loaded into phials, attacks deflect (bounce).
*   **Phials**:
    *   **Load**: **Guard + Weapon Action** (Block + X) converts Energy into filled Phials (3 for Yellow, 5 for Red, 6 for Overheat/Skill).
    *   **Capacity**: Standard 5 phials.
*   **Phial Types**:
    *   **Impact**: Deals stun/exhaust damage, ignores hitzones.
    *   **Element**: Massive elemental damage multiplier, hitzone dependent.

### 1.2. Shield Charge (Elemental Boost)
*   **Trigger**: Cancel a **Super Amped Element Discharge (SAED)** into a **Elemental Roundslash**.
*   **Effect** (Duration based on phials consumed):
    *   Increases Guard capacity (blocks heavier hits, +1 Guard Level).
    *   Unlocks **SAED** (Super Amped Element Discharge) instead of AED.
    *   Adds Phial damage explosions to Guard Points.
    *   Axe Mode attacks deal increased damage (`1.1x` to `1.2x`).

### 1.3. New Wilds Features
*   **Power Axe Mode** (Savage Axe):
    *   **Trigger**: Hitting a **Wound** with `Focus Strike` OR pressing **Attack** (Left Click) after a **Perfect Guard**.
    *   **Effect**: Phials slowly drain over time (or per hit). Axe attacks hit multiple times (sawblade ticks) on contact.
*   **Sword Boost Mode** (Condensed Element Slash):
    *   **Trigger**: Holding **Attack** (Left Click) during a Phial Load sequence (requires Shield Charge).
    *   **Effect**: Prevents deflection, adds Phial damage explosions (Mind's Eye effect) to sword attacks for ~45s.

---

## 2. Controls & Input Mapping Plan

We use `BetterCombat` + Custom Keys defined in `ClientKeybinds.java`.

| Function | Official Input | Mod Mapping | Notes |
| :--- | :--- | :--- | :--- |
| **Attack** | Triangle | **Left Click** | Primary combos. |
| **Weapon Action** | Circle | **X Key** | Charged Slash, Discharges. |
| **Weapon Action Alt** | Triangle + Circle | **C Key** | Shield Thrust, SAED, Special. |
| **Special Action** | R2 (Morph) | **F Key** | Morph Slash (Contextual). |
| **Guard** | R2 (Hold) | **L-Ctrl (Hold)** | Block (Sword Mode only). |
| **Focus Mode** | L2 | **L-Alt (Hold)** | Aiming / Focus Strikes. |

---

## 3. Moveset & State Machine

The weapon implements a strict State Machine: `SwordMode` vs `AxeMode`. 

### 3.1. Sword Mode State (High Mobility, Guarding)

#### Basic Attacks (Left Click Chain)
1.  **Sword: Weak Slash** (`Left Click`)
2.  **Sword: Return Stroke** (`Left Click` after Weak Slash)
3.  **Sword: Roundslash** (`Left Click` after Return Stroke)
    *   *End Lag GP*: Has a Guard Point at the end of animation.

#### Heavy / Gap Closer (X Key)
*   **Sword: Charged Double Slash** (`Hold X` -> Release)
    *   *Primary Gauge Builder*. Two hits. Release at correct flash for max energy.
    *   If released too early/late: **Sword: Rising Slash** (Weak energy gain).
*   **Sword: Forward Slash** (`W + Click` or `Spirit + Click`?) -> *Need to decide if mobility is context or key*
    *   In this plan: **Forward + Left Click**.

#### Special (C Key or Combo)
*   **Sword: Shield Thrust** (`C Key` or `Left Click + X Key`)
    *   Quick jab. Useful to chain into AED/SAED.
    *   Combo: *Any Attack* -> **Shield Thrust**.

#### Morphing (F Key)
*   **Sword: Morph Slash** (`F Key`)
    *   Transforms to Axe Mode with a heavy distinct overhead slam.
    *   *Start Up GP*: Has a Guard Point at the very start.

#### Guard Actions (Holding L-Ctrl)
*   **Guard**: Blocks attacks. Stamina drain.
*   **Charge**: (`Hold L-Ctrl + X Key`)
    *   Loads Phials from Energy.
    *   -> **Sword: Condensed Element Slash** (Hold `Left Click` during Charge).
*   **Axe: Morph Slash** (`F Key` while Guarding): Immediate morph to Axe.

---

### 3.2. Axe Mode State (High Damage, Committal)

#### Basic Attacks (Left Click Chain)
1.  **Axe: Rising Slash** (`Left Click`) -> Launches teammates (if enabled). Great vertical reach.
2.  **Axe: Overhead Slash** (`Left Click` again)
3.  **Axe: Lateral Fade Slash** (`S + Left Click`?) -> New Wilds move? Or standard Fade.

#### Discharge Chain (X Key)
1.  **Axe: Element Discharge I** (`X Key`) -> Forward dash side chop.
2.  **Axe: Element Discharge II** (`X Key` again) -> Double swing (2 hits).
3.  **Axe: Amped Element Discharge (AED)** (`X Key` finisher)
    *   Consumes 1 Phial. Big overhead slam.
    *   If Shield is Charged: Becomes **Super Amped Element Discharge (SAED)**.
        *   Consumes ALL Phials. Massive shockwave.

#### Cancel / Special Paths
*   **Axe: Morph Slash** (`F Key`): Morphs back to Sword (Roundslash GP).
*   **SAED Cancel -> Shield Boost**:
    *   During AED/SAED windup -> Press **F Key** (Special Action / Morph).
    *   Performs **Elemental Roundslash**. Charges Shield. Return to Sword Mode.
*   **Savage Axe Slash** (Power Axe):
    *   Trigger: **Attack** (Left Click) after a **Perfect Guard**.
    *   Enters **Power Axe State**: Ticks extra damage on hits.

---

### 3.3. Focus Mode & Wilds Specs
*   **Focus Slash: Double Rend** (`L-Alt + Click`)
    *   Gap closer. If it hits a **Wound** -> Triggers **Power Axe Mode**.

---

## 4. Technical Implementation

### 4.1. `ChargeBladeCapability`
Stores weapon state attached to the ItemStack or Player, synced to client.
```java
public class ChargeBladeData {
    // Resources
    int phials; // 0-5
    float swordEnergy; // 0.0 to 100.0 (Yellow=30, Red=60, Overheat=100)
    
    // States
    boolean isShieldCharged;
    int shieldChargeDuration;
    boolean isSwordBoosted;
    int swordBoostDuration;
    boolean isPowerAxeActive; // Savage Axe
    
    // Config values (synced or client known)
    ChargeBladeMode currentMode; // SWORD, AXE
}

enum ChargeBladeMode { SWORD, AXE }
```

### 4.2. File Structure
*   **`ChargeBladeItem.java`**: Main Item. Overrides `use`, `releaseUsing`, etc.
*   **`CBPhialManager.java`**: Energy math.
*   **`CBCombatEvents.java`**: Handles `AttackEvent`, `LivingHurtEvent` for Guard Points.
*   **`client/render/CBHud.java`**: Renders Phials/Bottles.

### 4.3. Data Config (`data/mhweapons/charge_blade/`)
```json
{
  "phial_damage_multiplier": 5.0,
  "shield_boost_duration_per_phial": 600,
  "guard_point_start_frames": {
    "morph_sword_to_axe": [0, 6],
    "roundslash": [18, 25]
  },
  "moves": {
    "saed_impact_radius": 4.0,
    "saed_element_cone_angle": 45.0
  }
}
```

---

## 5. Implementation Checklist

### Phase 1: Foundation
- [ ] **Item Registration**: `ChargeBladeItem`.
- [ ] **GeckoLib Model**: Setup `sword.geo.json` and `axe.geo.json` (or bone visibility toggling).
- [ ] **Input Handling**: Map `X`, `C`, `F` keys to packets.
- [ ] **Mode Switching**: Implement `F Key` swapping State + Animation.

### Phase 2: Combos & Energy
- [ ] **Sword Combos**: Define Better Combat `attack_chain` for Sword.
- [ ] **Energy Accumulation**: OnHit event -> Add Energy.
- [ ] **Phial Loading**: Implement `Guard + X` logic (Reset Energy -> Fill Phials).
- [ ] **HUD**: Draw the UI.

### Phase 3: Axe & Discharges
- [ ] **Axe Combos**: Define Axe chain.
- [ ] **AED/SAED**: Implement the Entity/AoE logic for the big finishers.
- [ ] **Shield Charge**: Logic to cancel SAED into Buff.

### Phase 4: Polish (Wilds)
- [ ] **Guard Points**: Implement frame-perfect block logic.
- [ ] **Power Axe**: Implement the multi-hit "saw" effect on Axe attacks.
- [ ] **Focus Strike**: Implement the Wound targeting logic.
