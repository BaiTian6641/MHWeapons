# Dual Blades (DB) — Technical Implementation Plan

**Overview:**
The fastest weapon. Stamina management, high hit count (Elemental King), and mode switching (Demon / Archdemon).

## 1. Core Mechanics & Architecture

### 1.1. Demon Mode (State Machine)
*   **Toggle**: `Special Action` key (R2).
*   **Effect**:
    *   **Stamina Drain**: Continual consumption.
    *   **Move Set**: Changing from standard slashes to rapid "Demon Dance" moves.
    *   **Dodge**: Roll changes to **Demon Dash** (Faster, shorter recovery).
    *   **Speed**: Movement speed increased.

### 1.2. Archdemon Mode
*   **Demon Gauge**: Fills while dealing damage in Demon Mode.
*   **Activation**: When Gauge is full and player *exits* Demon Mode.
*   **Effect**:
    *   Retains the "Demon Dash".
    *   Unlocks "Demon Flurry" (Mini Demon Dance).
    *   Gauge depletes over time or on usage.

### 1.3. Wilds Update (Focus & Clash)
*   **Focus Strike**:
    *   **Move**: **Spinning Lunge**.
    *   **Effect**: Drills into the wound, hitting many times.
*   **Auto-Counter (Demon Mode)**:
    *   **Mechanic**: If a Perfect Dodge (i-frame dodge) occurs in Demon Mode, the player automatically deals damage to the attacker while evading.
    *   **Implementation**: `DodgeEvent` -> If `Perfect` -> Spawn `DamageSource` at player location.

---

## 2. Integration Strategy

### 2.1. Better Combat `DualBladesHandler`
*   **Attack Speed**:
    *   DB attacks are incredibly fast. We must ensure `invulnerabilityTicks` on mobs is set to 0 or minimized for DB damage sources so all hits register.
*   **Stamina Management**:
    *   Server-side ticker decrements Stamina/Hunger.
    *   If Stamina <= 0, force exit Demon Mode.

### 2.2. GeckoLib & Trails
*   **Blade Trails**:
    *   Critical visual feedback.
    *   **Normal**: White/None.
    *   **Demon Mode**: Red/Glow.
    *   **Archdemon**: Red Flashing.

---

## 3. Data Config (`data/mhweapons/dual_blades/`)

```json
// default.json
{
  "stamina_drain_per_tick": 0.5,
  "gauge_gain_per_hit": 3.0,
  "demon_mode_speed_mult": 1.2
}
```

## 4. Class Structure

1.  **`DemonGaugeCapability`**:
    *   `boolean inDemonMode`
    *   `float gauge`
2.  **`DBStaminaHandler`**:
    *   Handles the drain logic.
3.  **`DBHitHandler`**:
    *   Ensures multi-hit registration works (bypassing standard i-frames).

## 5. Development Tasks
- [ ] **Mode Switching**: Visual + Logic toggle.
- [ ] **Stamina Drain**: Robust system that works with sprint.
- [ ] **Demon Dash**: Replacing the vanilla jump/sprint with a quick vector impulse.
