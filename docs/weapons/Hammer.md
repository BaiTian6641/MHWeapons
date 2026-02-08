# Hammer — Technical Implementation Plan

**Overview:**
The King of KO. Blunt damage, mobile charging, and head-sniping. Simple but effective.

## 1. Core Mechanics & Architecture

### 1.1. Charge Mechanics
*   **Levels**: Charge 1 (Side Smash), Charge 2 (Uppercut), Charge 3 (Big Bang/Superpound).
*   **Mobility**: Can move at full speed while charging.
*   **Power Charge (State)**:
    *   **Input**: Press `Special` while Charging.
    *   **Effect**: "Stores" the charge into the hammer.
    *   **Result**: Hammer glows. Attack + Stun values increased. Superpound animation changes.
    *   **Lost**: On flinch/damage.

### 1.2. Slope Mechanics (The Spin)
*   **Aerial Spin**:
    *   If sliding down a slope (or falling) while Charging.
    *   **Move**: Mid-air spinning bludgeon (Sonic Spin).
    *   **Implementation**: Check `player.onGround` and velocity. If conditions met, release triggers `AerialSpinAction`.

### 1.3. Wilds Update (Focus & Clash)
*   **Keep Sway**:
    *   **Mechanic**: An evade *while* charging that maintains the charge level.
    *   **Input**: Dodge while Holding Charge.
    *   **Invincibility**: Short i-frames.
*   **Offset Uppercut (Clash)**:
    *   **Input**: Release Level 2 Charge (Uppercut) exactly as monster attacks.
    *   **Effect**: Negates damage, stops monster charge (KO).
*   **Focus Strike**:
    *   **Move**: **Stationary Overhead Slam**.
    *   **Effect**: Massive single-hit damage to Wounds.

---

## 2. Integration Strategy

### 2.1. Better Combat `HammerHandler`
*   **Hit Stop**: Crucial for Hammer. Heavy pauses on impact.
*   **Head Targeting**:
    *   Hammer *needs* to hit the head.
    *   **CombatReferee**: If target is a large mob, check hit height. If Top 20% -> Apply Stun Bonus.

### 2.2. GeckoLib
*   **Power Charge Visuals**:
    *   Hammer head texture changes or emits particles when Power Charged.

---

## 3. Data Config (`data/mhweapons/hammer/`)

```json
// default.json
{
  "stun_value_charge3": 50,
  "exhaust_value_charge3": 20,
  "power_charge_buff_pct": 0.15
}
```

## 4. Class Structure

1.  **`HammerChargeCapability`**:
    *   `int chargeTicks`
    *   `boolean isPowerCharged`
2.  **`HammerSwayHandler`**:
    *   Handles the dodge-while-charging logic.

## 5. Development Tasks
- [ ] **Power Charge**: State retention logic.
- [ ] **Aerial Spin**: Detecting "air" state to swap the attack action.
- [ ] **Keep Sway**: Implementing the evade without cancelling the item use.
