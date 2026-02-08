# Great Sword (GS) — Technical Implementation Plan

**Overview:**
The quintessential Monster Hunter weapon. High commitment, high reward. It relies on prediction, positioning, and "Big Number" damage.

## 1. Core Mechanics & Architecture

### 1.1. The Charge System
*   **Levels**: Charge 1, Charge 2, Charge 3.
*   **Timing**:
    *   **Overcharge**: If held too long, damage drops to Level 2.
    *   **Visuals**: Aura color changes (Red -> Orange -> White/Blue).
*   **Moveset**:
    *   **Draw Slash** (Chargeable).
    *   **Strong Charge Slash** (Chargeable, faster if chained).
    *   **True Charged Slash (TCS)**: The ultimate move. Two hits. First hit is weak; if it lands on a soft spot, the second hit gains a massive damage multiplier ("Power" TCS).

### 1.2. Defensive Actions
*   **Tackle**:
    *   **Input**: Press `Kick/Tackle Key` while Charging.
    *   **Effect**:
        *   Cancels Charge.
        *   **Hyper Armor**: Cannot be knocked back. Damage taken reduced by 50%.
        *   **Combo Skip**: Advances the combo stage (Draw -> Strong -> TCS).
*   **Guard**:
    *   Standard block. Consumes Sharpness.

### 1.3. Wilds Update (Focus & Clash)
*   **Focus Strike**:
    *   **Input**: Hold `Focus` + Attack.
    *   **Move**: Downward thrust that creates/exploits a "Wound".
    *   **Utility**: Can rotate 360° while charging in Focus Mode.
*   **Clash (Offset Attack)**:
    *   **Input**: `Upward Slash` timed against a monster attack.
    *   **Effect**: Blocks the attack and launches the player into a Strong Charge stance.

---

## 2. Integration Strategy

### 2.1. Hit Stop (The "Crunch")
GS requires "Hit Lag" to feel powerful.
*   **Implementation**:
    *   **Event**: `LivingDamageEvent`.
    *   **Logic**: If damage > Threshold (e.g., Charge 3), trigger `HitStopManager.freeze(entity, ticks)`.
    *   **Visuals**: Pause the animation of both Player and Mob. Shake screen.

### 2.2. Better Combat Configuration
*   **Charge Mechanics**:
    *   Better Combat doesn't natively support "Hold to Charge" changing the *damage* of the *release* dynamically in a simple way.
    *   **Solution**:
        *   Use `PlayerAnimator` to loop the "Charge" animation.
        *   On button release, send packet with "Charge Duration".
        *   Server calculates Level based on duration -> Applies damage multiplier.

### 2.3. GeckoLib
*   **Animations**:
    *   `charge_draw_loop`, `charge_strong_loop`, `charge_tcs_loop`.
    *   `tackle`.
    *   `clash_offset`.
*   **Models**:
    *   TCS requires the player to flip.

---

## 3. Data Config (`data/mhweapons/great_sword/`)

```json
// default.json
{
  "tcs_motion_value": 2.64,
  "tackle_damage_reduction": 0.5,
  "hit_stop_ticks_level3": 8
}
```

## 4. Class Structure

1.  **`GSChargeHandler`**:
    *   Client-side ticker to count frames held.
    *   Render overlay (Charge level).
2.  **`HitStopManager`**:
    *   Global system to pause entity ticking.
3.  **`GSEventHandler`**:
    *   Handles Tackle damage reduction (`LivingHurtEvent`).

## 5. Development Tasks
- [ ] **Hit Stop System**: The most critical "feel" mechanic.
- [ ] **Charge Logic**: syncing hold duration to server damage.
- [ ] **Tackle**: Implementing the hyper-armor window.
