# Session Summary: Long Sword Polish (Feb 7, 2026)

## 1. Overview
This session focused on refining the **Long Sword** mechanics, specifically the **Spirit Gauge** resource management, **Spirit Blade** costs/gains, **Charge Attack** logic, and **Spirit Helm Breaker** physics.

## 2. Changes Implemented

### 2.1 Spirit Gauge Mechanics
- **Decay**: Increased passive decay rate (`0.02f` -> `0.05f`) per tick to make resource maintenance more active.
- **Normal Attacks**: Added Spirit Gauge generation to standard attacks (Overhead, Thrust, Rising, Fade Slash).
  - **Logic**: Gauge is only added if the attack successfully hits a target (`applyLongSwordManualHit` returns true).
- **Consumption**: Ensured all **Spirit Blade** combos (Spirit Blade I, II, III, Roundslash) consistently consume gauge (`-5.0f` to `-25.0f`).
- **Level Up Reset**: Successfully landing a **Spirit Roundslash** (via combo or charge) and leveling up the sword (White/Yellow/Red) now **resets the Spirit Gauge to 0**.

### 2.2 Charge Attack Logic
- **Fixed Input Conflict**: Short clicks (< 5 ticks) are ignored by the charge logic to prevent accidental state entry.
- **Dynamic Build-up**: 
  - Holding the attack button (> 10 ticks) now dynamically builds Spirit Gauge over time.
  - **Rate**: `20.0f / maxCharge` per tick (approx. 0.33 gauge/tick for a 3s charge).
- **Tap vs. Charge**:
  - **Tap (< 10 ticks)**: Executes **Spirit Blade** combo (consumes gauge).
  - **Charge Release**: Executes a powerful slash based on charge level, but builds massive gauge if held to max (Stage 4).

### 2.3 Spirit Helm Breaker Polish
- **Jump Physics**: 
  - **Height**: Tuned initial vertical velocity to `4.0` (approx. 5.5 blocks peak).
  - **Duration**: Reduced total air time to **1.5 seconds** (30 ticks).
  - **Smoothing**: Implemented interpolated gravity during the ascent and "hang" phase for a floaty apex, followed by an accelerating dive curve (`-0.5` to `-6.0`) for impact weight.

## 3. Implementation Details

### Files Modified
1.  **`LongSwordHandler.java`**
    *   Checks for hit success before adding gauge on normal attacks.
    *   Handles `handleChargeRelease` branching (Tap = Spend, Charge = Build/Attack).
    *   Clears gauge on Level Up.

2.  **`WeaponStateEvents.java`**
    *   Manages passive gauge decay in `onPlayerTick`.
    *   Handles **Spirit Helm Breaker** movement physics (velocity updates per tick).

3.  **`ActionKeyTickEvents.java`**
    *   Manages the dynamic gauge gain while holding the charge button.
    *   Includes the 5-tick buffer to prevent "instant charge" on clicks.

4.  **`ActionKeyEvents.java`**
    *   Handles the server-side event when `spirit_roundslash` hits, triggering the level-up and gauge reset.

## 4. Technical Logic Summary

### Charge Handling
The charge logic is split between **ticking** (building gauge) and **release** (executing move).
- **Tick**: `ActionKeyTickEvents` checks `chargeAttackTicks`. If `> 10`, it adds `20.0f / max` to `spiritGauge`.
- **Release**: `LongSwordHandler` checks usage duration.
  - If `< 10` (Tap): Calls `setAction("spirit_blade_X")` and consumes gauge.
  - If `>= 10` (Charge): Calls `setAction("spirit_blade_X")` corresponding to charge stage, but does NOT consume gauge (often builds it if Max Charge).

### Combo Handling
- Normal attacks (Left Click) cycle through `Overhead -> Stab -> Rising`.
- This is tracked via `weaponState.setLongSwordOverheadComboIndex`.
- Spirit attacks (Right Click / Tap) cycle through `Spirit I -> Spirit II -> Spirit III -> Roundslash`.
- This is tracked via `weaponState.setLongSwordSpiritComboIndex`.
- **Timeout**: If input stops for `comboWindowTicks` (e.g. 24-100 ticks), the index resets to 0.

### HUD Integration (Existing)
- The HUD reads `weaponState.getSpiritGauge()` to fill the bar.
- It reads `weaponState.getSpiritLevel()` to change the bar color (None/White/Yellow/Red).
- It reads `combatState.getActionKey()` to display the current move name (e.g., "Spirit Helm Breaker").
