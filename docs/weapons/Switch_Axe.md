# Switch Axe (SA) — Technical Implementation Plan

**Overview:**
The Switch Axe (SA) is a morphing weapon that oscillates between mobility/reach (Axe Mode) and high DPS/explosions (Sword Mode). Implementation requires managing two resource gauges and complex state transitions. Modernized for *Monster Hunter Wilds* with counters, Power Axe mode, and seamless morphing.

## 1. Core Mechanics & Architecture

### 1.1. Dual Gauge System
*   **Switch Gauge (Axe Resource)**:
    *   **Function**: Required to perform Sword attacks. Regenerates in Axe Mode and while sheathed.
    *   **Threshold**: Requires **≥30%** to Morph to Sword from idle.
    *   **Fill Mechanics**: Passive regen (slow), Axe attacks (fast). *Spiral Burst Slash* fills roughly 30% instantly.
*   **Amp Gauge (Sword Resource)**:
    *   **Function**: Buildup towards **Amped State**.
    *   **Amped State**: When full, sword enters "Amped State" for `N` seconds (typically 45s).
    *   **Effect**: Adds **Phial Explosions** (secondary hits) to all Sword attacks. Unlocks *Zero Sum Discharge (ZSD)* and *Full Release Slash (FRS)*.

### 1.2. Key States
*   **Axe Mode**:
    *   **Power Axe Mode**: Triggered by *Heavy Slam* finisher (after Wild Swing).
    *   **Buff**: Increases Part Damage (Stagger) and Switch Gauge accumulation rate.
    *   **Visual**: Axe blade glows red/orange.
*   **Sword Mode**:
    *   **Phial Type**: Property of the weapon item (Power, Element, Exhaust, etc.).
    *   **Visual**: Sword emits energy; Amped State causes glowing electricity/elemental effects.

### 1.3. Key Moveset (Base)
*   **Axe Combo (Standard)**:
    *   `Overhead Slash` → `Side Slash` → `Spiral Burst Slash`.
    *   *Spiral Burst Slash* is key for maintaining gauge.
*   **Wild Swing Loop**:
    *   `Wild Swing` (infinite loop) → `Heavy Slam` (Power Axe Trigger).
*   **Sword Combo**:
    *   `Overhead Slash` → `Rising Slash` (Left/Right).
    *   `Double Slash` → `Heavenward Flurry` (Builds Amp Gauge rapidly).
*   **Morphing**:
    *   Can occur after almost any move.
    *   **Morph Slash**: Attacks while transforming (e.g., *Forward Morph*).

### 1.4. Wilds Update (New Mechanics)
*   **Unbridled Slash (Sword)**:
    *   High-commitment heavy hitter. Guaranteed Morph to Axe after use.
*   **Full Release Slash (FRS)**:
    *   Amped State exclusive. Massive AoE. Morphs to Axe.
*   **Counters**:
    *   **Offset Rising Slash (Axe)**: Vertically clears attacks. Success → `Follow-up Heavy Slam`.
    *   **Counter Rising Slash (Sword)**: Absorbs knockback. Success → `Heavenward Flurry`.
*   **Focus Strike (Morph Combination)**:
    *   Multi-hit chain targeting wounds using *Focus Mode*.

---

## 2. Integration Strategy

### 2.1. Controls & Input Mapping
Mapping complex inputs to Minecraft's limited bindings.
*   **Attack (Left Click)**:
    *   Axe: `Overhead Slash` combo sequence.
    *   Sword: `Overhead Slash` combo sequence.
*   **Use / Interact (Right Click / X)**:
    *   Axe: `Wild Swing` (Hold to loop). Release or press Attack to `Heavy Slam`.
    *   Sword: `Double Slash` → `Heavenward Flurry`.
*   **Special / Morph (F / Keybind)**:
    *   **Tap**: `Morph` (Stationary) or `Morph Slash` (if moving/attacking).
    *   **Hold (Sword)**: `Element Discharge` start.
*   **Alt Action (C / Alt)**:
    *   Axe: `Spiral Burst Slash` (if in combo) or `Fade Slash`.
    *   Sword: `Counter Rising Slash`.

### 2.2. State Machine (`SwitchAxeHandler`)
*   **Variables**:
    *   `mode`: `AXE` | `SWORD` | `TRANSITION`
    *   `switchGauge`: 0.0 - 100.0 (Float)
    *   `ampGauge`: 0.0 - 100.0 (Float)
    *   `isAmped`: boolean (Active State)
    *   `isPowerAxe`: boolean (Active State)
*   **Transition Logic**:
    *   Server-side validation for gauge requirements (prevent Sword morph if gauge < 30%).
    *   Forced Morph to Axe on empty Switch Gauge during Sword attacks.

---

## 3. Data Config (`data/mhweapons/switch_axe/`)

```json
// default.json
{
  "switch_gauge_max": 1000,
  "amp_gauge_max": 1000,
  "amped_duration_ticks": 900,
  "power_axe_duration_ticks": 1200,
  "morph_threshold": 300,
  "phial_explosion_mv": 0.1,
  "moves": {
    "heavy_slam_mv": 1.2,
    "frs_mv": 3.5,
    "unbridled_mv": 2.8
  }
}
```

## 4. Class Structure

1.  **`SwitchGaugeCapability`**:
    *   Manages both Switch and Amp gauges.
    *   Handles ticking (regen/decay).
    *   Syncs to client for HUD.
2.  **`SwitchAxeItem`**:
    *   Overrides `onLeftClickEntity`, `onItemUse`.
    *   Stores `PhialType` in NBT or Datapack properties.
3.  **`SwitchAxeHandler`**:
    *   Central logic for moveset execution.
    *   `tryMorph()`, `executeDischarge()`, `handleTick()`.
    *   Manages `counterWindow` for Offset/Counter moves.

## 5. Implementation Checklist

### Core Systems
- [ ] **Dual Gauge Capability**
    - [ ] Switch Gauge logic (Refill rule, Decay rule).
    - [ ] Amp Gauge logic (Gain on Sword hit).
    - [ ] Amped Timer & Power Axe Timer.
- [ ] **HUD**
    - [ ] Switch Gauge Bar (morph capability marker at 30%).
    - [ ] Amp Gauge (Glows when full).
    - [ ] Axe/Sword Mode Icon.
    - [ ] Phial Indicator.

### Basic Moveset
- [ ] **Axe Actions**
    - [ ] `Overhead` chain.
    - [ ] `Wild Swing` (Loop logic).
    - [ ] `Heavy Slam` (Power Axe trigger).
    - [ ] `Spiral Burst Slash` (Refill).
- [ ] **Sword Actions**
    - [ ] `Overhead` chain.
    - [ ] `Double Slash`.
    - [ ] `Heavenward Flurry` (High amp gain).

### Transformation
- [ ] **Morph Logic**
    - [ ] Idle Morph (Stand still).
    - [ ] Morph Slash (Mid-combo).
    - [ ] Forced Morph (Gauge Empty).

### Finishers (Wilds)
- [ ] **Counters**
    - [ ] `Offset Rising Slash` (Axe counter window).
    - [ ] `Counter Rising Slash` (Sword super-armor).
- [ ] **Discharge**
    - [ ] `Element Discharge` (Repeated inputs).
    - [ ] `Zero Sum Discharge` (Latch logic or sticky hit).
    - [ ] `Full Release Slash` (Explosion + Morph).
    - [ ] `Unbridled Slash` (Explosion + Morph).
