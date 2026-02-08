# Combat Codebase Summary (Attack HUD, Charge, Combo)

This document summarizes the main systems involved in **attack HUD**, **charge attacks**, and **combo attacks**.

## 1) Attack HUD

**Primary file**: `src/main/java/org/example/client/ui/WeaponHudOverlay.java`

### 1.1 Current Attack Label
- `renderAttackHud(...)` draws the “Current” label by calling `resolveCurrentActionLabel(...)`.
- `resolveCurrentActionLabel(...)`
  - Returns `None` if `actionKey` is empty or `actionKeyTicks <= 0`.
  - Resolves **generic actions** first (`dodge`, `guard`, `sheathe`, `charge`, `vault`, etc.).
  - Then resolves **weapon-specific labels** via `resolveActionLabelForWeapon(...)`.

### 1.2 Weapon-specific Labels
- `resolveActionLabelForWeapon(...)` maps `actionKey` → display label per weapon.
- Magnet Spike labels handled by `getMagnetSpikeMoveName(...)` and special cases (e.g. Magnet Burst launch).
- Gunlance and Tonfa include fallback for `basic_attack` to a meaningful label.

### 1.3 HUD Input Hints
- `renderAttackHud(...)` prints key hints per weapon (LMB/RMB/Special).
- For Magnet Spike:
  - `Weapon` → Magnet Tag
  - `Alt` → Magnetic Approach
  - `Alt+G` → Magnetic Repel
  - `Special` → Mode Switch
  - `Special+Shift` → Magnet Burst


## 2) Charge Attack Handling

### 2.1 Client-side Input (Hold vs Tap)
**Primary file**: `src/main/java/org/example/client/input/ClientForgeEvents.java`

- Detects LMB hold (`attackHoldTicks`) and sends `WeaponActionType.CHARGE`.
- For Long Sword, it continuously updates action key to `spirit_blade_*` and shows HUD stages.
- For Magnet Spike (Impact mode only):
  - Uses raw mouse press to avoid keyAttack suppression.
  - Sends charge after hold threshold.
  - Shows `impact_charge_i/ii/iii` while charging.
  - Emits stage-change particles.
  - Quick tap sends normal `WEAPON` action (combo).

### 2.2 Server-side Charge Tick / Release
**Primary files**:
- `src/main/java/org/example/common/events/combat/ActionKeyTickEvents.java`
- `src/main/java/org/example/common/combat/weapon/WeaponActionHandler.java`
- `src/main/java/org/example/common/combat/weapon/MagnetSpikeHandler.java`
- `src/main/java/org/example/common/combat/weapon/LongSwordHandler.java`

**Flow:**
1. Client sends `WeaponActionType.CHARGE` (pressed/released).
2. `WeaponActionHandler.handleCharge(...)` starts or ends charging.
3. During charge, `ActionKeyTickEvents` increments `chargeAttackTicks` and updates HUD action keys.
4. On release, specific weapon handlers execute:
   - Long Sword → `LongSwordHandler.handleChargeRelease(...)`
   - Magnet Spike → `MagnetSpikeHandler.handleChargeRelease(...)`

**Magnet Spike specifics:**
- Impact-only charge (blocked in cut mode).
- Charge release sets `impact_charge_i/ii/iii` or `impact_bash` (low charge).
- Damage and animation use `WeaponDataResolver` motion values and timing.


## 3) Combo Attack Handling

### 3.1 Server-side Combo Sequencing
**Primary file**: `src/main/java/org/example/common/events/combat/ActionKeyEvents.java`

- On `AttackEntityEvent`, server sets `actionKey` and `actionKeyTicks`.
- Magnet Spike combo:
  - Cut: `magnet_slash_i → ii → iii → cleave`
  - Impact: `magnet_smash_i → ii → crush → suplex`
- Timeouts reset to stage 1 when window exceeded or tick delta invalid.

### 3.2 Better Combat Integration (Client)
**Primary file**: `src/main/java/org/example/common/compat/BetterCombatAnimationBridge.java`

- Maps Better Combat animations to Magnet Spike combo actions.
- Ignores `bettercombat:attack_start` to prevent double‑advance.
- Skips BC attack updates during charging.

### 3.3 Combo Timing Data
**Primary file**: `src/main/resources/data/mhweaponsmod/weapons/magnet_spike.json`

- `comboWindowTicks` controls combo window (e.g., 36).
- `motionValues` define damage multipliers for all combo and charge actions.


## 4) Magnet Spike RMB Approach / Repel

**Primary files**:
- `src/main/java/org/example/client/input/ClientForgeEvents.java`
- `src/main/java/org/example/common/combat/weapon/MagnetSpikeHandler.java`

**Behavior:**
- RMB → Approach (zip to target).
- Alt(G) + RMB → Repel (zip away).
- Implemented via `WeaponActionType.WEAPON_ALT` and `WeaponActionType.WEAPON_ALT_REPEL`.


## 5) Key Data Sources

- `WeaponDataResolver` reads from `data/mhweaponsmod/weapons/<weapon>.json`.
- Motion values, combo windows, and charge timing come from weapon JSON.


## 6) Key Classes / Responsibilities

- **ClientForgeEvents**: input handling, charge thresholds, HUD hints.
- **ActionKeyEvents**: server combo sequencing and action key assignment.
- **ActionKeyTickEvents**: tick down action keys; drive charge stage updates.
- **WeaponActionHandler**: routes input actions by weapon.
- **MagnetSpikeHandler**: zip, burst, charge release, combat logic.
- **BetterCombatAnimationBridge**: Better Combat animation → action key mapping.
- **WeaponHudOverlay**: current attack HUD and labels.


## 7) Notes / Gotchas

- If HUD shows only `magnet_cut` / `magnet_impact`, the action key is likely being overwritten by Better Combat or not advancing.
- Ensure combo window is set in the **active** dataset (`mhweaponsmod`) to avoid constant resets.
- Charging should block attack spam; check that `isChargingAttack` is honored in both client and server paths.

## 8) Long Sword Specifics (Feb 2026)

- **HUD Labels:** Long Sword action keys map to readable names in [WeaponHudOverlay.java](src/main/java/org/example/client/ui/WeaponHudOverlay.java#L750-L770) (e.g., `spirit_blade_*`, `spirit_roundslash`, `spirit_helm_breaker`, `special_sheathe`).
- **Spirit Gauge Lifecycle:** Passive decay is handled in [WeaponStateEvents.java](src/main/java/org/example/common/combat/weapon/WeaponStateEvents.java#L310-L335). Normal attacks add gauge only when the manual hit connects; Spirit Blades consume gauge; landing `spirit_roundslash` to level up clears the gauge (see [LongSwordHandler.java](src/main/java/org/example/common/combat/weapon/LongSwordHandler.java#L200-L255) and [ActionKeyEvents.java](src/main/java/org/example/common/events/combat/ActionKeyEvents.java#L340-L355)).
- **Charge vs Tap:** Charge ticks ignore the first 5 ticks to avoid conflicts, build gauge dynamically when held (>10 ticks), and release uses `handleChargeRelease` to branch: tap consumes gauge for Spirit Blades; held charge executes staged Spirit Blades and stage 4 Roundslash (see [ActionKeyTickEvents.java](src/main/java/org/example/common/events/combat/ActionKeyTickEvents.java#L70-L105) and [LongSwordHandler.java](src/main/java/org/example/common/combat/weapon/LongSwordHandler.java#L175-L265)).
- **Fade Slash:** Shift + Alt attack triggers `fade_slash`, dashes backward, and grants gauge only on hit via manual hit detection (see [LongSwordHandler.java](src/main/java/org/example/common/combat/weapon/LongSwordHandler.java#L430-L455)).
- **Foresight Slash:** Dodge during an active attack if `spiritGauge >= 10` -> drains gauge to 0, spends stamina, applies a backward dash and 12 ticks of i-frames, sets action `foresight_slash`, and re-arms charge (see [LongSwordHandler.java](src/main/java/org/example/common/combat/weapon/LongSwordHandler.java#L305-L350)).
- **Special Sheathe:** Shift + Special enters sheathe for 60 ticks; `WEAPON` performs `iai_slash` (adds gauge), `WEAPON_ALT` performs `iai_spirit_slash` if a Spirit Level is available; state auto-cancels on other inputs (see [LongSwordHandler.java](src/main/java/org/example/common/combat/weapon/LongSwordHandler.java#L520-L575)).
- **Spirit Helm Breaker:** Triggered after `spirit_thrust` hit; jump velocity tuned to 4.0 with 1.5s air time and smoother rise/dive physics; direction stored for dive; cooldown 10s (see [WeaponStateEvents.java](src/main/java/org/example/common/combat/weapon/WeaponStateEvents.java#L90-L150) and per-tick motion at [WeaponStateEvents.java](src/main/java/org/example/common/combat/weapon/WeaponStateEvents.java#L140-L195)).
