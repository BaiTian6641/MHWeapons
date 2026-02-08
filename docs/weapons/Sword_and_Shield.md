# Sword and Shield (SnS) — Technical Implementation Plan

**Overview:**
The "Jack of All Trades". High mobility, item usage while drawn, and a mix of Cutting (Sword) and Blunt (Shield) damage.

## 1. Core Mechanics & Architecture

### 1.1. Item Usage While Drawn
*   **Unique Feature**: Can use items (Potions, Blocks) without sheathing.
*   **Implementation**:
    *   Normally, `Right Click` blocks.
    *   **Input**: Holding `Guard` + `Use Item Key` (or just `Use Item Key` if configured) triggers item use.
    *   **Code**: Override `use` logic in `SwordShieldItem`.

### 1.2. The Moveset (Hybrid Damage)
*   **Sword Attacks**: High attack speed, low commitment. `SEVER` damage.
*   **Shield Bash**:
    *   Harder hitting. `BLUNT` damage (KO).
    *   Does *not* consume Sharpness.
*   **Perfect Rush**:
    *   Timed combo. requires rhythmic inputs ("Just Timing").
    *   Massive damage output if timed correctly.

### 1.3. Wilds Update (Focus & Clash)
*   **Focus Strike**:
    *   **Move**: **Lunging Precision Stab**.
    *   **Effect**: Pinpoint strike to break Wounds.
*   **Slide Slash**:
    *   New mobility tool. Attack while sliding left/right/back.
    *   **Tech**: Uses `PlayerAnimator` root motion or velocity overrides.
*   **Guard Slash Offset (Clash)**:
    *   **Input**: Attack while Guarding exactly when hit.
    *   **Effect**: Repels the monster (Knockback to mob) and allows immediate follow-up.

---

## 2. Integration Strategy

### 2.1. Better Combat `SnSHandler`
*   **Dual Wield Logic**:
    *   Although it's one item, it renders as two (Sword + Shield).
    *   Shield actions (Block) need to visually raise the offhand model.
*   **Backstep**:
    *   The core of SnS mobility.
    *   **Input**: `Back` + `Dodge` during a combo.
    *   **Action**: i-frame backhop -> Charged Slash (Launch into air) OR Perfect Rush.

### 2.2. GeckoLib
*   **Perfect Rush Visuals**:
    *   The character glows red briefly at the "Just Timing" window.
*   **Oils (Optional)**:
    *   If implementing MHGen Oils, apply visual shaders to the blade.

---

## 3. Data Config (`data/mhweapons/sword_and_shield/`)

```json
// default.json
{
  "shield_bash_stun_value": 15.0,
  "perfect_rush_multiplier": 1.5,
  "guard_point_start": 0,
  "guard_point_end": 5
}
```

## 4. Class Structure

1.  **`SnSItem`**:
    *   Overrides `onItemRightClick` for guarding.
    *   Custom interaction for using other items in inventory.
2.  **`PerfectRushHandler`**:
    *   Tracks the timer of the current combo node.
    *   If input received in `[SuccessWindow]`, apply damage bonus.

## 5. Development Tasks
- [ ] **Item Use**: The logic to consume inventory items without sheathing.
- [ ] **Perfect Rush**: Rhythm game logic.
- [ ] **Backstep**: Backward velocity jump with i-frames.
