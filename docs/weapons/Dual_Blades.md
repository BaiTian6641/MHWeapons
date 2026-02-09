# Dual Blades (DB) — Technical Implementation Plan

**Overview:**
The fastest weapon in the arsenal. Features high movement speed, rapid-fire hit counts, and a dedicated stance system (Demon Mode). Heavily relies on **Stamina Management** and **Demon Gauge** maintenance. Features new Wilds mechanics like Demon Boost Mode and Focus Strikes.

## 1. Core Mechanics & Architecture

### 1.1. The Demon Gauge
*   **The Bar**: Located below the stamina bar (or integrated into HUD).
*   **Mechanic**: Fills when landing hits while in **Demon Mode**.
*   **Archdemon Mode**:
    *   **Activation**: Automatically enters Archdemon Mode if the player exits Demon Mode while the gauge is fully filled.
    *   **Effect**: Strengthens normal attacks and unlocks "Demon Flurry" moves without draining stamina.
    *   **Decay**: Gauge depletes over time or when using specific Archdemon moves.

### 1.2. Demon Mode (Stance)
*   **Toggle**: `Special Action` (F).
*   **Effect**:
    *   Changes movement animation to a sprint (arms back).
    *   **Stamina Drain**: Constant consumption while active. Exits automatically when Stamina hits 0.
    *   **Knockback Immunity**: Grants super armor (minor knockback resistance).
    *   **Dodge**: Changes standard roll to **Demon Dodge** (Dash). Faster, shorter recovery.
*   **Demon Boost Mode** (Wilds New):
    *   **Trigger**: Successfully performing a Perfect Evade (Demon Dodge) against an attack.
    *   **Effect**: Increases attack power and elemental damage. allows attacking *during* the dodge.

### 1.3. Key Moveset (Base)
*   **Double Slash Combo**:
    *   **Input**: `Attack` (Left Click).
    *   **Moves**: Double Slash -> Double Slash Return Stroke -> Circle Slash.
    *   **Demon Mode**: Becomes **Demon Fangs** -> **Twofold Demon Slash** -> **Sixfold Demon Slash**.
*   **Lunging Strike**:
    *   **Input**: `Weapon Action` (X).
    *   **Details**: A gap-closer slash. Useful for repositioning. Can chain into Roundslash.
    *   **Demon Mode**: Becomes **Demon Flurry Rush** (Spinning gap closer).
*   **Blade Dance (Demon Mode)**:
    *   **Input**: `Weapon Action Alt` (C).
    *   **Requirement**: Must be in Demon Mode.
    *   **Details**: Stationary rapid combo. High commitment, high damage. Consumes Demon Gauge.
*   **Demon Flurry (Archdemon Mode)**:
    *   **Input**: `Weapon Action Alt` (C) (When in Archdemon Mode).
    *   **Details**: Stationary combo similar to Blade Dance but less committed. Consumes Gauge.

### 1.4. Wilds Update (New Mechanics)
*   **Focus Strike: Turning Tide**:
    *   **Input**: `Focus Key` + `Attack` (Left Click).
    *   **Move**: **Midair Spinning Blade Dance** / Drill Slash.
    *   **Effect**: If hitting a wound, drills into it for massive multi-hit damage. Can be triggered from a slide or leap.
*   **Demon Boost Mode**:
    *   Implemented via `DodgeEvent` checks. If `isPerfect` and `inDemonMode`, apply `MobEffect` "Demon Boost" for X seconds.

---

## 2. Integration Strategy

### 2.1. Controls & Input Mapping
*   **Special (F)**: Toggle Demon Mode (R2).
*   **Attack (Left Click)**: Basic Combo (Triangle).
    *   Normal: Double Slash.
    *   Demon: Demon Fangs.
*   **Weapon Action (X)**: Mobility / Lunging Strike (Circle).
    *   Normal: Lunging Strike.
    *   Demon: Demon Flurry Rush (The "Beyblades" spin).
*   **Weapon Action Alt (C)**: Big Finishers (Triangle + Circle).
    *   Demon Mode: Blade Dance.
    *   Archdemon: Demon Flurry.
    *   Normal: N/A or simple cross slash.

### 2.2. Stamina & Effect Handling
*   **Server-Side Logic**:
    *   `CommonWeaponTicker` must handle the stamina decrement for Demon Mode players.
    *   If `Stamina <= 0`, send packet to client to force "Exit Demon Mode" animation/state.
*   **Attack Speed Hack**:
    *   Dual Blade hits are very frequent (every 2-4 ticks).
    *   We must bypass standard `CheckSpawn` invulnerability or set target `hurtResistantTime` to 0 on every hit to ensure damage registration.

---

## 3. Data Config (`data/mhweapons/dual_blades/`)

```json
// default.json
{
  "stamina_drain_per_tick": 0.5,
  "gauge_gain_per_hit": 2.5,
  "demon_mode_speed_mult": 1.3,
  "blade_dance_dmg_mult": 0.8,
  "demon_dodge_iframe_ticks": 10
}
```

## 4. Class Structure

1.  **`DBStaminaHandler`**:
    *   Logic for draining stamina and force-cancelling mode.
2.  **`DemonGaugeCapability`**:
    *   `boolean inDemonMode`
    *   `boolean inArchdemonMode` (Calculated: !inDemonMode && gauge > 0).
    *   `float gaugeValue` (0.0 to 100.0).
3.  **`DualBladesItem`**:
    *   Dual wielding rendering logic.
4.  **`DBComboHandler`**:
    *   State machine for the complex branching of standard vs demon moves.

## 5. Combo Details (Technical Flow)

### 5.1. Normal Mode
*   **Basic Chain**: `Attack` -> `Attack` -> `Attack`
    *   *Double Slash* -> *Double Slash Return* -> *Circle Slash*
    *   Use: Building gauge slowly, safe poking.
*   **Gap Closer**: `Weapon Action (X)`
    *   *Lunging Strike*: Forward movement.
    *   Can chain into `Attack` (Double Slash Return).
*   **Archdemon Activator**: `Special (F)`
    *   Enters Demon Mode. Animation cancels most recovery frames.

### 5.2. Demon Mode (Stamina Draining)
*   **DPS Chain**: `Attack` -> `Attack` -> `Attack`
    *   *Demon Fangs* -> *Twofold Demon Slash* -> *Sixfold Demon Slash*
    *   High hit count, rapidly builds gauge.
*   **Mobility Chain**: `Weapon Action (X)` -> `Weapon Action (X)`
    *   *Demon Flurry Rush* (Spin forward) -> *Rising Slash* -> *Double Roundslash*.
    *   Best for repositioning while dealing damage.
*   **The Big One**: `Weapon Action Alt (C)`
    *   *Blade Dance*:
        *   Stage I: 4 hits.
        *   Stage II: 4 hits.
        *   Stage III: 6+ hits.
    *   **Lock**: Player cannot move or dodge during this animation. High risk.

### 5.3. Archdemon Mode (Gauge Active, Demon Mode Off)
*   **Modified Basic**: `Attack` chain mimics Demon Mode but slower/weaker.
*   **Demon Flurry**: `Weapon Action Alt (C)`
    *   *Demon Flurry I, II, III*.
    *   Consumes Gauge per stage.
    *   Less commitment than Blade Dance (can dodge out earlier).

### 5.4. Wilds Special Chains
*   **Evade Counter**:
    *   Condition: `Perfect Evade` in `Demon Mode`.
    *   Trigger: `Attack` immediately after dodge.
    *   Result: **Demon Boost Mode** Activated -> Counter Slash.
*   **Focus Strike**:
    *   Hold `Focus` + `Attack`.
    *   If `Target has Wound`: Trigger **Turning Tide** (Drill).
    *   If `No Wound`: Standard heavy slash.

---

## 6. Implementation Roadmap

### Phase 1: Core Items & State
- [ ] **DualBladesItem**: Create item class with two-handed rendering properties.
- [ ] **Capability**: Implement `DemonGauge` capability (stamina drain, gauge fill logic).
- [ ] **Packet**: `NetworkPackets` to sync gauge/mode status to client for HUD/Movement speed.
- [ ] **Mode Toggle**: Implement `Special (F)` key logic to switch `inDemonMode` boolean.

### Phase 2: Basic Animations & Hitboxes
- [ ] **GeckoLib Models**: Import `Idle`, `run`, `sprint` (Naruto run for Demon Mode).
- [ ] **Normal Combo**: Implement Double Slash chain (`Attack`).
- [ ] **Demon Combo**: Implement Demon Fangs chain (`Attack` + `inDemonMode`).
- [ ] **Hit Registration**: Ensure multi-hit attacks register correctly (adjust invulnerability frames).

### Phase 3: Advanced Mechanics
- [ ] **Stamina Drain**: Hook into tick event. Drain stamina if `inDemonMode`. Force exit if 0.
- [ ] **Demon Dash**: Override standard roll when in Demon Mode (short dash impulse).
- [ ] **Blade Dance**: Implement the specific `WeaponActionAlt` animation and lock-in logic.

### Phase 4: Archdemon & Wilds Features
- [ ] **Archdemon Logic**: Check `gauge == MAX` on exit to enable Archdemon flags.
- [ ] **Demon Flurry**: Implement Archdemon specific moves.
- [ ] **Wilds Focus Strike**: Implement wound detection and drill animation.
- [ ] **Demon Boost**: Add temporary potion effect/modifier on perfect dodge.
