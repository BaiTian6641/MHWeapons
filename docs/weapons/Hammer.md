# Hammer — Technical Implementation Plan (Refined)

**Overview:**
The King of KO. Blunt damage, mobile charging, and head-sniping. Adapted for MHWilds mechanics including Focus Mode and Power Charge.

**Reference:** [Official MH Wilds Manual — Hammer](https://manual.capcom.com/mhwilds/en/steam/page/4/5)

## 1. Controls & Keybindings (Aligned with Official Manual)

| User Input | MH Wilds Button | Action Name | Context | Description |
| :--- | :--- | :--- | :--- | :--- |
| **Left Click (LMB)** | Btn8 | `Standard Attack` | Idle / Combo | **Overhead Smash I** → **Overhead Smash II** → **Upswing** (High Stun). Driven by Better Combat → `ActionKeyEvents`. |
| **Right Click (RMB, Hold)** | BtnR2 (Hold) | `Charge` | Idle / Moving | Hold to accumulate Charge Levels (1, 2, 3). Drains stamina. Handled by `handleHammerRmbInput()`. |
| **Right Click (RMB, Release)** | BtnR2 (Release) | `Charged Attack` | Charging | Executes attack based on Charge Level (see §2.1). |
| **C Key (Special)** | Btn6 | `Charged Step` | Charging (Lvl 1-2) | **Evasive hop** that preserves charge level. 6 i-frames. |
| **C Key (Special)** | Btn6 | `Mighty Charge` | Charging (Lvl 3) | Activates **Mighty Charge** mode → continue charging → release for **Mighty Charge Slam**. |
| **C Key (Special)** | Btn6 | `Big Bang` | Idle | **Big Bang I** → **II** → **III** → **IV** → **Finisher**. Must hit to progress. |
| **C Key after attack** | Btn8+Btn6 | `Spinning Bludgeon` | After attack | Spinning attack → LMB: **Side Smash** → **Follow-up** → **Strong Upswing**. |
| **Dodge (Space)** | — | `Charged Step` | Charging | QoL alternative — same as C Key during charge. |
| **Focus Mode + LMB** | BtnL2+BtnR1 | `Focus Strike` | Focus Mode | **Focus Blow: Earthquake**. Sweeping smash effective against wounds. |

### Recommended Combos (from Official Manual)

| Combo Name | Input Sequence | Actions |
| :--- | :--- | :--- |
| **Basic Combo** | LMB → LMB → LMB | Overhead Smash I → Overhead Smash II → Upswing |
| **Charged Side Blow Combo** | RMB (Charge Lvl 1) → Release → LMB → LMB | Charged Side Blow → Charged Follow-up → Overhead Smash I |
| **Charged Upswing Combo** | RMB (Charge Lvl 2) → Release → LMB | Charged Upswing → Charged Follow-up |
| **Combo into Spinning Bludgeon** | LMB → C (after attack) → LMB → LMB → LMB | Overhead Smash I → Spinning Bludgeon → Side Smash → Follow-up → Strong Upswing |

## 2. Core Mechanics & Architecture

### 2.1. Charge Levels
*   **Input**: Hold **RMB** to charge (BtnR2 in official manual). Release to attack.
*   **Stamina Drain**: Charging drains 0.5 stamina/tick. If stamina depletes, charge auto-releases.
*   **Input Blocking**: During RMB charge, LMB is suppressed from Better Combat (`keyAttack.setDown(false)`) to prevent accidental attacks while charging.
*   **Level 1 (1/3 of max charge)**: **Charged Side Blow** → LMB: Follow-up → LMB: Overhead Smash I.
*   **Level 2 (2/3 of max charge)**: **Charged Upswing**. Fast gap-closer with High Stun.
    *   **Offset Feature**: If hit by a monster during Upswing, negates damage → **Offset Uppercut** → LMB → **Follow-up Spinslam**.
*   **Level 3 (max charge)**:
    *   **Normal**: **Charged Big Bang**.
    *   **Mighty Charge (C Key at Lvl 3)**: **Mighty Charge Slam** (superior finisher).
    *   **Aerial**: Check `!player.onGround()` → **Aerial Spinning Bludgeon**.

### 2.2. Mighty Charge (replaces old Power Charge toggle)
*   **Activation**: Press `C Key` (Special) at **Charge Level 3** while charging (RMB held).
*   **Effect**:
    *   Sets `hammerPowerCharge = true`.
    *   Visual: FLAME particles every 10 ticks.
    *   Stat: `attackMultiplier` (1.15×) applied in `CombatReferee`.
    *   Move Change: Charge Lvl 3 release → `hammer_mighty_charge_slam`.

### 2.3. Charged Step
*   **Input**: C Key (Special) or Dodge while Charging at Lvl 1-2.
*   **Logic**: Short dash (0.45 speed). Does *not* reset `chargeAttackTicks`. 6 i-frames.
*   **Stamina Cost**: 15.0.

### 2.4. Spinning Bludgeon
*   **Input**: C Key after an attack (LMB + C combo from manual).
*   **Chain**: Spinning Bludgeon → LMB: Spinning Side Smash → LMB: Spinning Follow-up → LMB: Spinning Strong Upswing.
*   **Note**: The longer you spin without attacking, the more powerful but also riskier it becomes.

### 2.4. Offset (Upswing Counter)
*   **Trigger**: Both standard `hammer_upswing` and `hammer_charged_upswing` can Offset.
*   **Logic**: Incoming `LivingHurtEvent` during active upswing → cancel damage → `hammer_offset_uppercut`.
*   **Follow-up**: LMB after Offset Uppercut → `hammer_follow_up_spinslam`.

### 2.5. Wilds Mechanics
*   **Focus Blow: Earthquake**:
    *   **Input**: Focus Mode + LMB.
    *   **Effect**: Sweeping smash effective against wounds. High motion value (1.8).

## 3. Data Structure (`data/mhweapons/weapons/hammer.json`)

```json
{
  "charge_levels": [ 20, 60 ], // Ticks for Lvl 2, Lvl 3
  "motion_values": {
    "overhead_smash_1": 42,
    "overhead_smash_2": 23,
    "upswing": 86,
    "charge_lvl1_side": 25,
    "charge_lvl2_uppercut": 55,
    "charge_lvl3_slam": 90,
    "charge_lvl3_power_slam": 110,
    "big_bang_finisher": 150,
    "spinning_bludgeon_tick": 20
  },
  "stun_values": {
    "upswing": 40,
    "charge_lvl2_uppercut": 50,
    "big_bang_finisher": 75
  },
  "power_charge_buff": {
    "attack_multiplier": 1.15,
    "stun_multiplier": 1.15
  }
}
```

## 4. Class Structure & Logic

### 4.1. `HammerHandler` (`org.example.common.combat.weapon`)
*   **Entry point**: `handleAction(WeaponActionType, pressed, player, combatState, weaponState)`
*   **WEAPON case**: Keyboard (X key) fallback for combo advance; primary LMB combo driven by BC → `ActionKeyEvents`.
*   **SPECIAL case**: Handles Big Bang, Charged Step, Mighty Charge, Spinning Bludgeon (context-dependent).
*   **Methods**:
    *   `handleLmbCombo(...)`: X-key fallback + charge follow-up chains.
    *   `handleSpecial(...)`: Context-sensitive C-key actions.
    *   `handleChargeRelease(...)`: Called from `WeaponActionHandler.handleCharge` on RMB release.
    *   `advanceBigBang(...)`: Big Bang combo stage advancement.
    *   `advanceSpinningCombo(...)`: Spinning Bludgeon sub-combo advancement.

### 4.2. Client-Side Input (`ClientForgeEvents`)
*   **LMB**: Normal `keyAttack.isDown()` — Better Combat handles attacks, fires `AttackEntityEvent`.
*   **RMB**: `handleHammerRmbInput()` — raw `mouseHandler.isRightPressed()`, charge hold/release tracking.
*   **C Key**: `WEAPON_ACTION_ALT` → `SPECIAL` packet to server → `HammerHandler.handleSpecial`.
*   **Suppression**: During RMB charge, both `keyAttack` and `keyUse` are suppressed.

### 4.2. `HammerChargeCapability` (PlayerWeaponState)
*   Need to store:
    *   `boolean isPowerCharged`
    *   `int chargeTicks`

### 4.3. Client-Side Animation
*   **GeckoLib**:
    *   `charge_idle`: Holding hammer ready.
    *   `charge_walk`: Moving with hammer.
    *   `power_charge_overlay`: Glow layer.

## 5. Implementation Checklist & Attack Logic

### Phase 1: Preparation & State
- [x] **Item Registration**:
    - [x] Create `HammerItem` class extending `GeoWeaponItem` (registered as `"hammer"`, BLUNT damage type in `MHWeaponsItems`).
    - [x] Register new item in `MHWeaponsItems`.
- [x] **Weapon State**:
    - [x] Add `hammerPowerCharge` (boolean) to `PlayerWeaponState`.
    - [x] Add `hammerChargeTicks` (int) to `PlayerWeaponState`.
    - [x] Add `hammerComboIndex` / `hammerComboTick` (int) for LMB combo tracking.
    - [x] Add `hammerBigBangStage` / `hammerBigBangTick` (int) for Big Bang combo.
    - [x] Full serialization (NBT save/load) and sync (copyFrom) for all fields.
- [x] **Handler Skeleton**:
    - [x] Create `HammerHandler.java` in `common/combat/weapon`.
    - [x] Hook `HammerHandler` into `WeaponActionHandler` (with `syncWeaponState`).

### Phase 2: Basic Combat (LMB)
- [x] **Standard Combo**:
    - [x] `hammer_overhead_smash_1` (Start) -> `hammer_overhead_smash_2` -> `hammer_upswing` (Finisher).
    - [x] **Follow-up**: `hammer_upswing` -> **LMB** -> `hammer_overhead_smash_1` (Loop via modulo).
    - [x] Combo timeout resets to stage 0 (uses `comboWindowTicks` from data JSON).
    - [x] `ActionKeyEvents` combo via `AttackEntityEvent` (like Magnet Spike / Dual Blades pattern).
    - [x] LMB flows through Better Combat naturally (no raw mouse interception for combo).
- [x] **Spinning Bludgeon (C after attack)**:
    - [x] C Key during active combo → `hammer_spinning_bludgeon` entry.
    - [x] LMB during spin → `hammer_spin_side_smash` → `hammer_spin_follow_up` → `hammer_spin_strong_upswing`.
    - [x] Handled in both `HammerHandler.handleSpecial` (C entry) and `ActionKeyEvents` (LMB advancement).
- [x] **Big Bang Combo (C Key, Idle)**:
    - [x] `hammer_big_bang_1` -> ... -> `hammer_big_bang_finisher`.
    - [x] Advanced by repeated C Key presses. LMB does NOT advance Big Bang.
    - [x] Countdown timeout (40-tick window, decayed in `WeaponStateEvents`).
    - [ ] **Constraint**: Must hit an entity to proceed to next stage (currently advances on input alone — hit-check not yet enforced).

### Phase 3: Charge System (RMB Hold)
- [x] **Charging Logic**:
    - [x] Route RMB Hold to `WeaponActionType.CHARGE` via `ClientForgeEvents.handleHammerRmbInput()`.
    - [x] **Input blocking**: Read raw right mouse (`mouseHandler.isRightPressed()`), suppress `keyUse.setDown(false)` while charging. Also suppress LMB during charge to prevent BC attacks.
    - [x] Tick counter via generic `chargeAttackTicks` in `ActionKeyTickEvents`.
    - [x] **Levels** derived from `chargeMaxTicks` (default 40) with 1/3 and 2/3 thresholds.
    - [x] Hammer removed from client `isChargeWeapon()` (charges on RMB, not LMB).
- [x] **Stamina Drain**:
    - [x] Drain 0.5 stamina/tick during charge (`ActionKeyTickEvents`).
    - [x] Auto-release charge when stamina depletes (calls `handleChargeRelease` + syncs state).
    - [x] Recovery delay set to 10 ticks during charge hold.
- [x] **Mobility**:
    - [x] Player moves at full speed while charging (no additional slowdown).
- [x] **Sound Cues**:
    - [x] Play `EXPERIENCE_ORB_PICKUP` with rising pitch at Lvl 1/2/3 transitions (`ActionKeyTickEvents`).
- [x] **Client-Side Charge HUD**:
    - [x] Real-time `hammer_charge_1/2/3` action keys set during hold (`ActionKeyTickEvents` + `ClientForgeEvents`).
    - [x] Client-side particles (CRIT) on level transitions (`ClientForgeEvents`).

### Phase 4: Charge Releases (RMB Release)
- [x] **Input Handling**:
    - [x] `HammerHandler.handleChargeRelease` resolves level from `chargeTicks` vs `chargeMaxTicks`.
    - [x] Sets `hammerChargeLevel` + 60-tick display timer.
- [x] **Level 1 Logic (Charged Side Blow)**:
    - [x] Action: `hammer_charged_side_blow`.
    - [x] **Follow-up (LMB)**: `hammer_charged_follow_up` -> `hammer_overhead_smash_1` (handled in `handleLmbCombo`).
- [x] **Level 2 Logic (Charged Upswing)**:
    - [x] Action: `hammer_charged_upswing`.
    - [x] **Follow-up (LMB)**: Same follow-up chain.
    - [x] **Offset**: Both `hammer_upswing` and `hammer_charged_upswing` can Offset (see Phase 6).
- [x] **Level 3 Logic (Charged Big Bang / Mighty Charge)**:
    - [x] **Stationary (Default)**: `hammer_charged_big_bang`.
    - [x] **Mighty Charged**: `hammer_mighty_charge_slam` (activated via Special at Lvl 3).
    - [x] **Aerial/Slope**: Check `!player.onGround()` -> `hammer_aerial_spin`.
    - [ ] **Moving ground**: `hammer_charged_spin_bludgeon` variant (currently only aerial check, no ground-moving spin).

### Phase 5: Mighty Charge & Charged Step (Special while Charging)
- [x] **Charged Step (Lvl 1-2)**:
    - [x] If `isChargingAttack()` && charge stage < 3 && `SPECIAL` pressed → evasive hop.
    - [x] Action: `hammer_charged_step`, 6 i-frames, 15 stamina cost, short dash.
    - [x] Charge state (`chargeAttackTicks`, `chargingAttack`) fully preserved.
    - [x] Also available via Dodge key (QoL alternative).
- [x] **Mighty Charge (Lvl 3)**:
    - [x] If charge stage == 3 && `SPECIAL` pressed → activate `hammerPowerCharge = true`, `hammer_mighty_charge` action.
    - [x] Continue charging → release for `hammer_mighty_charge_slam`.
- [x] **Buff Effects**:
    - [x] Visual: FLAME particles every 10 ticks while Mighty Charge is active (`WeaponStateEvents`).
    - [x] Stat: `attackMultiplier` (1.15×) from `powerChargeBuff` applied in `CombatReferee`.

### Phase 6: Wilds / Advanced Mechanics
- [x] **Charged Step / Keep Sway (Dodge while Charging)**:
    - [x] Intercept DODGE in `WeaponActionHandler` while `isChargingAttack` for hammer.
    - [x] Also handled via Special key in `HammerHandler.handleSpecial()`.
    - [x] Trigger `hammer_charged_step` action with 6 i-frames, short dash.
    - [x] `chargeAttackTicks` and `chargingAttack` preserved through the evade.
- [x] **Offset (Upswing Counter)**:
    - [x] In `CombatReferee`: Both `hammer_upswing` and `hammer_charged_upswing` can Offset.
    - [x] Cancel incoming damage, set action to `hammer_offset_uppercut`.
    - [x] **Follow-up Spinslam**: LMB after Offset → `hammer_follow_up_spinslam` (motion value 2.0).
- [x] **Focus Strike**:
    - [x] Focus Mode + LMB → `hammer_focus_blow_earthquake` (in `HammerHandler.handleLmbCombo`).
    - [x] HUD label: `"Focus Blow: Earthquake"`.

### HUD Status
- [x] **Current Attack labels**: All 20+ move names mapped in `WeaponHudOverlay.resolveActionLabelForWeapon`.
- [x] **Input hints**: Hammer-specific hints for combo, charge, special, Big Bang stage.
- [x] **Status display**: Charge Lv, Power Charge indicator, Big Bang stage (X/5).
- [x] **Data JSON**: Motion values, stun values, power charge buff config all present in `hammer.json`.
