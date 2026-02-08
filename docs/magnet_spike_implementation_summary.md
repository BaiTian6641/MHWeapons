# Magnet Spike Work Summary (2026‑02‑07)

## 1) Chat History Summary

- **Goal**: Implement full Magnet Spike combos, charge behavior, and accurate HUD current‑attack labels. Align input handling with Long Sword patterns and ensure Better Combat integration works.
- **Problems observed**:
  - HUD showed only mode switch labels (`magnet_cut` / `magnet_impact`).
  - Combos skipped the first hit or reset too quickly.
  - Charge HUD flashed, and LMB spam happened while charging.
  - RMB behavior needed to be Approach (default) and Repel (Alt/G).
- **Approach**:
  - Added combo state tracking, server/client sequencing, and Better Combat bridging.
  - Added charge release damage logic and motion values.
  - Added debug logs to analyze `latest.log` and tune combo window and input flow.
  - Updated HUD labels to include special/movement actions and Magnet Burst launch state.

## 2) Key Functional Changes Implemented

### 2.1 Combo Sequencing (Cut/Impact)
- **Server LMB combo**: `magnet_slash_i → ii → iii → cleave` or `magnet_smash_i → ii → crush → suplex` with timing window.
- **Client/BC combo**: Better Combat events now map to Magnet Spike combo actions, avoiding generic `basic_attack` or mode switch labels.
- **Stale tick protection**: Negative tick deltas or stale ticks now reset combos to stage 1.

### 2.2 Charge Behavior (Impact‑only)
- **Impact‑only charge**: Charge only applies when in Impact mode.
- **Hold‑to‑charge**: Holding LMB enters charge state; release triggers `impact_charge_i/ii/iii` or `impact_bash` on low charge.
- **Blocking spam**: While charging, LMB attack events are blocked to stop spam attacks.
- **HUD stages**: Charge stages are displayed while charging, similar to Long Sword.
- **Particles**: Client stage changes emit electric spark particles.

### 2.3 RMB Behavior (Approach / Repel)
- **Default RMB** → **Magnetic Approach** (zip to target).
- **Alt (G) + RMB** → **Magnetic Repel** (zip away).
- HUD hints updated to reflect new RMB behavior.

### 2.4 HUD Logic Updates
- Current attack label now includes:
  - Magnet Burst **Launch** state (airborne).
  - Generic movement/special actions (dodge, guard, sheathe, charge, vault).
  - Magnet approach/repel labels.

### 2.5 Data Updates
- Updated Magnet Spike weapon data in `mhweaponsmod` to ensure the active dataset has combo timing and full motion values for all magnet actions.

## 3) Implementation Details by Area

### 3.1 Input Handling (Client)
- **File**: `src/main/java/org/example/client/input/ClientForgeEvents.java`
- **Key logic**:
  - Magnet Spike in Impact mode uses raw mouse left press for charge hold detection.
  - Charge HUD and particles begin only after charge is actually sent (prevents quick tap showing charge).
  - Quick LMB taps trigger normal combo attacks.
  - RMB Approach/Repel routed by Alt(G) state.

### 3.2 Combat / Action Handlers (Server)
- **File**: `src/main/java/org/example/common/events/combat/ActionKeyEvents.java`
  - Blocks magnet attacks while charging.
  - Handles magnet LMB combo sequencing and timeout logic.
- **File**: `src/main/java/org/example/common/combat/weapon/MagnetSpikeHandler.java`
  - Charge release uses motion values, applies damage & animations.
  - Magnet Burst + Pile Bunker logic maintained.
  - RMB route for Approach/Repel zip behavior.
- **File**: `src/main/java/org/example/common/compat/BetterCombatAnimationBridge.java`
  - Maps BC animations to Magnet Spike combos.
  - Ignores `bettercombat:attack_start` to prevent double‑advance.
  - Skips BC attack updates while charging.

### 3.3 HUD
- **File**: `src/main/java/org/example/client/ui/WeaponHudOverlay.java`
  - Added generic action labels for movement/special actions.
  - Magnet Burst “Launch” label when airborne.
  - Added `magnet_approach` and `magnet_repel` labels.
  - Updated RMB hint to Approach/Repel.

### 3.4 Data
- **File**: `src/main/resources/data/mhweaponsmod/weapons/magnet_spike.json`
  - Updated `comboWindowTicks` to 36.
  - Added full motion values for slash/smash combos + charge actions.

## 4) Debugging & Verification

- Added debug logs for:
  - Magnet LMB combo sequencing (server).
  - Better Combat combo sequencing (client).
  - Magnet action inputs + charge release.
- Verified issues in `latest.log`:
  - `bettercombat:attack_start` was advancing combos twice.
  - Short `comboWindowTicks` caused frequent resets.

## 5) Current Behavior Summary

- **Quick LMB tap** → normal combo step (Smash/Slash depending on mode).
- **Hold LMB** in Impact → charge stages I/II/III, release for charge hit.
- **RMB** → Magnetic Approach.
- **Alt(G)+RMB** → Magnetic Repel.
- **HUD** shows current combo step, charge stage, and movement/special actions.

## 6) Files Touched (High‑level)

- `src/main/java/org/example/common/events/combat/ActionKeyEvents.java`
- `src/main/java/org/example/common/compat/BetterCombatAnimationBridge.java`
- `src/main/java/org/example/common/combat/weapon/MagnetSpikeHandler.java`
- `src/main/java/org/example/common/events/combat/ActionKeyTickEvents.java`
- `src/main/java/org/example/common/combat/weapon/WeaponActionHandler.java`
- `src/main/java/org/example/client/input/ClientForgeEvents.java`
- `src/main/java/org/example/client/ui/WeaponHudOverlay.java`
- `src/main/resources/data/mhweaponsmod/weapons/magnet_spike.json`
- `docs/weapons/Magnet_Spike.md` (doc updated)

## 7) Remaining Item (Optional)

- Add **Magnet Spike‑specific charge state** to `PlayerWeaponState` if you want separate persistence beyond the shared `chargeAttackTicks`.
