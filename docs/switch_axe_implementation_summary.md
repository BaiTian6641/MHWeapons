# Switch Axe Implementation Summary

## Overview
The Switch Axe (SA) has been fully implemented into the *MHWeaponsMod*, featuring its signature morphing mechanics, dual gauge system (Switch Gauge & Amp Gauge), and high-commitment transformation combos. The implementation integrates with `Better Combat` for animations and hit detection, while using custom Forge logic for state management.

## Core Mechanics Implemented

### 1. Dual Mode System
*   **Axe Mode**: (Default) Offers mobility and reach. Attacks regenerate Switch Gauge.
*   **Sword Mode**: High DPS mode that consumes Switch Gauge.
    *   **Requirement**: Requires >30% Switch Gauge to enter.
    *   **Forced Morph**: If Switch Gauge drops to 0 during Sword mode, the weapon forcibly morphs back to Axe.

### 2. Gauge Management
*   **Switch Gauge**:
    *   **Regeneration**: Passive regeneration while in Axe Mode.
    *   **Consumption**: Sword attacks consume specific amounts (defined in config).
    *   **Reload**: If attempting to morph to Sword with <30% gauge, a "Reload" action is triggered (simulated), restoring gauge.
*   **Amp Gauge**:
    *   **Build-up**: Dealing damage in Sword Mode builds the Amp Gauge.
    *   **Amped State**: When full, the weapon enters "Amped State" for 120 seconds.
    *   **Effect**: Amped state adds Phial Burst damage (bonus ticks) to attacks and grants visual particle effects.

### 3. Power Axe Mode
*   **Activation**: Triggered by performing the `heavy_slam` combo finisher in Axe mode.
*   **Effect**: Grants a buffer (default 45s) where the weapon has increased part-break/trip efficiency (simulated via logic).

### 4. Counters
*   **Offset Rising Slash / Counter Rising**:
    *   Specific attack windows allow the player to negate incoming damage.
    *   Handled via `CombatReferee` checking strict timing windows (`counterTicks`).
    *   Successful counters can trigger immediate follow-ups or gauge restoration.

## Technical Implementation

### Class Architecture

#### 1. Logic Handler (`SwitchAxeHandler.java`)
*   **Role**: Central controller for SA logic.
*   **Key Functions**:
    *   `handleWeaponAction`: Routes attacks based on current mode (Axe/Sword).
    *   `performMorph`: Handles logic for switching states and swapping item display (if applicable in future).
    *   `canMorphToSword`: Checks the 30% threshold.
    *   `manageGauges/States`: Updates ticks, decays gauges, and handles "Amped" timers.

#### 2. Data Persistence (`PlayerWeaponState.java`)
Added capability fields to track persistent player state:
*   `switchAxeSwitchGauge` (0-100)
*   `switchAxeAmpGauge` (0-100)
*   `switchAxePowerAxe` (Tick timer)
*   `switchAxeAmped` (Boolean state)
*   `switchAxeCounterTicks` (Int, for active counter frames)

#### 3. Event Bus Integration
*   **`WeaponStateEvents.java`**:
    *   **Tick Event**: Handles passive Switch Gauge regen (Axe mode) and Power Axe/Amped timer decay.
    *   **Attack Event**: Calculates gauge consumption (Sword) or gain (Axe). Applies Phial Bursts if Amped.
*   **`CombatReferee.java`**:
    *   Intercepts `LivingAttackEvent` to check for active counter frames. If `tryConsumeCounter()` returns true, damage is negated.
*   **`WeaponActionHandler.java`**:
    *   Routes generic `SWORD` or right-click inputs to `SwitchAxeHandler`.

#### 4. User Interface (`WeaponHudOverlay.java`)
*   **Switch Gauge Bar**: Blue bar. Shows a marker at 30% to indicate the Morph threshold.
*   **Amp Gauge Bar**: Inner bar. Pulses cyan when Amped State is active.
*   **Power Axe Icon**: Indicated by a specific texture or highlight when the timer is active.
*   **Keybind Hints**: Context-sensitive labels (e.g., changing "Light Attack" to "Morph Slash" when appropriate).

### Configuration (`switch_axe.json`)
Data/Config file controlling balancing values:
```json
{
  "mode": "axe", // Default start
  "switch_gauge": { "max": 100, "regen_rate": 1, "sword_threshold": 30 },
  "amp_gauge": { "max": 100, "decay_rate": 0.5 },
  "motion_values": {
    "axe_overhead": 0.8,
    "sword_double_slash": 1.2
    // ...
  }
}
```

## Networking
*   **`PlayerWeaponStateS2CPacket`**: Updated to sync all new Switch Axe NBT fields to the client for HUD rendering.
*   **`PlayAttackAnimationS2CPacket`**: Used by `SwitchAxeHandler` to force specific animations (e.g., recoil, morphs) regardless of input.

## Known Limitations / Future Work
*   **Visual Model Morphing**: Currently relies on a single item model. True model morphing (mesh swapping) requires complex Geckolib animations setup.
*   **Zero Sum Discharge (ZSD)**: The grappling mechanic is simplified purely to a high-commitment animation chain rather than physically attaching the player entity to the mob.
