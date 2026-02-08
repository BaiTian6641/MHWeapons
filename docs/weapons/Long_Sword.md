# Long Sword (LS) — Technical Implementation Plan

**Overview:**
The counter-king of Monster Hunter. Fluid combos, gauge management (Spirit Levels), and high-risk high-reward parries. Features the new Wilds mechanics like Spirit Charge and Focus Strike.

## 1. Core Mechanics & Architecture

### 1.1. Spirit Gauge System
*   **The Bar**: Fills on hit with normal attacks.
*   **Spirit Levels**: White -> Yellow -> Red.
    *   **Level Up**: Landing the final hit of the **Spirit Roundslash** or **Focus Strike** (Unbound Thrust).
    *   **Buffs**: Increases raw damage multiplier (1.05x -> 1.1x -> 1.2x).
    *   **Decay**: Levels decay over time.
    *   **Red Gauge**: Unlocks **Spinning Crimson Slash** and **Spirit Release Slash**.

### 1.2. Key Moveset (Base)
*   **Spirit Blade Combo**:
    *   **Input**: `Left Click` (Attack) - Cycles I, II, III, Roundslash.
    *   **Details**: Cycles through the combo steps with each click. Consumes Spirit Gauge. 
    *   **Spirit Roundslash**: Levels up gauge if it lands.
*   **Thrust & Rising Slash**:
    *   **Input**: `X` (Weapon Action). 
    *   **Details**: Quick poke / upsizing slash. Good for resets.
*   **Overhead Slash Combo**:
    *   **Input**: `C` (Weapon Action Alt).
    *   **Details**: Cycles Overhead Slash -> Overhead Stab -> Crescent Slash.
*   **Fade Slash**:
    *   **Input**: `Shift` + `C` (Weapon Action Alt).
    *   **Details**: Backwards slash to reposition.
*   **Foresight Slash**:
    *   **Input**: `Attack (Any)` + `Dodge` (Space).
    *   **Effect**: Backstep with i-frames ("foresight_slash" action). 
    *   **Counter**: If incoming damage matches i-frames, triggers Max Spirit Gauge (Check `LivingAttackEvent` logic).
    *   **Logic**: Implemented in `LongSwordHandler`. Checks `isAttacking` && `spiritGauge > 10`. Consumes gauge to start.
*   **Special Sheathe**:
    *   **Input**: `Shift` + `F` (Special Action).
    *   **Iai Slash**: `X` (Weapon Action) while sheathed. Auto-fills gauge on hit.
    *   **Iai Spirit Slash**: `C` (Weapon Action Alt) while sheathed. Counter move.
*   **Spirit Helm Breaker**:
    *   **Input**: `F` (Special Action).
    *   **Move**: **Spirit Thrust**.
    *   **Effect**: On hit, consumes 1 Level to deal massive damage.

### 1.3. Wilds Update (New Mechanics)
*   **Spirit Charge**:
    *   **Input**: Hold `Left Click` (Attack).
    *   **Effect**: Charges gauge manually. Unleashes a Spirit Blade attack based on charge time.
    *   **Red Gauge Bonus**: If used at Red Gauge, provides **Knockback Immunity** (Super Armor) during the charge/attack.
*   **Focus Strike (Unbound Thrust)**:
    *   *Implementation Pending*. (Planned: `Focus Mode` + Attack).
*   **Spinning Crimson Slash**:
    *   *Implementation Pending*.
*   **Spirit Release Slash**:
    *   *Implementation Pending*.

---

## 2. Integration Strategy

### 2.1. Controls & Input Mapping
Current implementation diverges from standard MH controls to fit Minecraft's limitations.
*   **Attack (Left Click)**: Defaulted to **Spirit Blade Combo** (usually R2) for easier access to the weapon's core mechanic.
*   **Weapon Action (X)**: Standard pokes.
*   **Weapon Action Alt (C)**: Standard heavy swings.
*   **Special (F)**: Spirit Thrust / Helm Breaker.
*   **Shift + F**: Special Sheathe.

### 2.2. Counter System (`CounterEvaluator`)
*   **Combo Graph**:
    *   Complex branching.
    *   Interruption points for `Foresight Slash` and `Special Sheathe` must be available after almost any attack.
*   **Gauge Sync**:
    *   Sword Glow: White/Yellow/Red emissive texture.
    *   HUD: Custom implementation required.

---

## 3. Data Config (`data/mhweapons/long_sword/`)

```json
// default.json
{
  "gauge_max": 100,
  "decay_seconds_red": 60,
  "foresight_iframe_ticks": 12,
  "helm_breaker_motion_value": 3.5,
  "iai_spirit_motion_value": 4.0
}
```

## 4. Class Structure

1.  **`SpiritGaugeCapability`**:
    *   `float value`, `int level` (0-3).
2.  **`LSCounterHandler`**:
    *   The "Referee" for Foresight and Iai slashes.
3.  **`LongSwordItem`**:
    *   Renders the sheath on the hip.
    *   Handles the `SpecialSheath` pose logic.

## 5. Implementation Checklist

### Core Systems
- [x] **Separated Logic**: `LongSwordHandler` created for robust handling.
- [x] **Spirit Gauge Capability**
    - [x] Value (0-100) logic.
    - [x] Level (0, 1, 2, 3) logic.
    - [x] Decay timers per level.
    - [x] Damage multiplier application.
- [x] **HUD & Visuals**
    - [x] Spirit Gauge Bar UI.
    - [x] Level Border UI (White/Yellow/Red).
    - [x] Weapon Emissive Glow sync.

### Basic Moveset
- [x] **Standard Attacks**
    - [x] Overhead Slash (Combo 1 & 2).
    - [x] Thrust.
    - [x] Rising Slash.
    - [x] Crescent Slash.
- [x] **Fade Slash**
    - [x] Backward movement logic.
    - [ ] Lateral movement logic.
- [x] **Reset of attack/combo**
    - [x] Passive reset after timeout (WeaponStateEvents).

### Spirit Attacks
- [x] **Spirit Blade Combo**
    - [x] Blade I, II, III.
    - [x] Spirit Roundslash.
    - [x] Level Up trigger (Event).
    - [x] Gauge consumption logic.
- [x] **Spirit Charge (Wilds)**
    - [x] Hold R2 (Attack) logic.
    - [ ] Super Armor effect (Red Gauge).

### Counters & Advanced
- [x] **Foresight Slash**
    - [x] Input Check (`Attack` + `Dodge`).
    - [x] Motion & Animation trigger.
    - [ ] Detection of enemy attack (Counter success).
    - [ ] Counter Follow-up (Fill Gauge).
    - [ ] Cancel into Roundslash.
- [x] **Special Sheathe**
    - [x] Pose/Stance logic.
    - [x] **Iai Slash**: Auto-regen buff on hit.
    - [x] **Iai Spirit Slash**: 
        - [x] Counter window logic (i-frames on trigger).
        - [x] Level Up on success (and clears gauge).
        - [x] Damage scaling via motion value.

### High Output
- [x] **Spirit Helm Breaker**
    - [x] Spirit Thrust (connector).
    - [x] Aerial leap logic.
    - [x] Level consumption (Red -> Yellow).
    - [x] Multi-hit damage application.
- [x] **Spirit Release Slash (Wilds)**
    - [x] Follow up logic after Helm Breaker (Red Gauge only).
- [x] **Spinning Crimson Slash (Wilds)**
    - [x] Chained from Roundslash (Red Gauge).
    - [x] Extension logic.
- [x] **Focus Strike (Wilds)**
    - [x] Unbound Thrust animation.
    - [x] Wound interaction/bonus damage.
    - [x] Level Up trigger.
