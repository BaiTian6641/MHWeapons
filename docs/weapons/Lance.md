# Lance — Technical Implementation Plan

**Overview:**
The "Immovable Object". Precise poking, hopping, and the strongest shield in the game.

## 1. Core Mechanics & Architecture

### 1.1. Guard & Counters
*   **Guard**:
    *   **Input**: Right Click.
    *   **Effect**: Reduces damage/knockback based on "Guard" skill/level.
    *   **Stamina**: Drains on hit.
*   **Counter Thrust**:
    *   **Input**: `Special` + `High Thrust`.
    *   **Action**: Raises shield briefly. If hit -> Immediate powerful thrust.
*   **Power Guard**:
    *   **Input**: `Jump` during Counter Charge.
    *   **Effect**: 360° blocking, increased stamina drain, but zero knockback response.

### 1.2. Mobility (The Hop)
*   **Lance Hop**:
    *   Replaces standard roll.
    *   Short, quick backstep or sidestep.
    *   Can chain up to 3 hops.

### 1.3. Wilds Update (Focus & Clash)
*   **Perfect Guard**:
    *   **Mechanic**: Blocking at the exact frame of impact.
    *   **Effect**: 0 Stamina cost, 0 Chip Damage, enables "Cross Slash" follow-up.
*   **Power Clash**:
    *   **Event**: If a monster uses a specific "Heavy" attack against a Lance Block.
    *   **Cinematic**: A QTE (Quick Time Event) where the player mashes attack to push the monster back.
    *   *Mod Implementation*: Instead of QTE, just a "Clash Check" (Strength vs Strength).
*   **Focus Strike**:
    *   **Move**: **Corkscrew Thrust**.
    *   **Effect**: Multi-hit drill attack on Wounds.

---

## 2. Integration Strategy

### 2.1. Guard System (`GuardHandler`)
*   **Directionality**:
    *   Lance block angle is tight (120° front).
    *   Power Guard is 360°.
*   **Knockback**:
    *   We must override vanilla knockback. Lance should "slide" back, not fly back.

### 2.2. Better Combat
*   **Pokes**:
    *   Simple linear hitboxes.
    *   **Reach**: Long (3.5 - 4.0 blocks).

---

## 3. Data Config (`data/mhweapons/lance/`)

```json
// default.json
{
  "base_guard_level": 50,
  "perfect_guard_window": 5
}
```

## 4. Class Structure

1.  **`LanceGuardCapability`**:
    *   `boolean isPowerGuarding`
    *   `int perfectGuardTimer`
2.  **`LanceHopHandler`**:
    *   Overrides movement vector for hops.

## 5. Development Tasks
- [ ] **Guard Logic**: Directional damage mitigation.
- [ ] **Power Guard**: State that consumes stamina rapidly.
- [ ] **Dash Attack**: "Choo Choo Train" charge.
