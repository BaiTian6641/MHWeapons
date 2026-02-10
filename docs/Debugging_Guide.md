# Debugging Guide

This guide explains how to use the debugging system to trace issues in weapon mechanics, combat, and input.

## Configuration
The debugging system is controlled via the mod's common configuration file (`config/mhweapons-common.toml` or `MHWeaponsConfig.java`).

### Available Flags
*   `debug.logCombatEvents`: Logs damage calculations, hit registration, and guard events.
    *   *Examples:* Damage dealt, i-frames active, guard points triggered.
*   `debug.logInputEvents`: Logs raw input actions processed by the server.
    *   *Examples:* Key presses (Attack, Special, Dodge), input vectors.
*   `debug.logStateChanges`: Logs changes to player capability data.
    *   *Examples:* Stamina usage, Demon Mode toggle, Gauge updates.
*   `debug.logWeaponActions`: Logs specific weapon logic decisions.
    *   *Examples:* Combo transitions, Spirit Level changes, Phial loading.

## How to use

1.  **Enable Logging:**
    *   Edit the config file in your `run/config` folder.
    *   Set the desired flags to `true`.
    *   Restart the game or reload config if supported.

2.  **View Logs:**
    *   Logs are output to the standard log file (`run/logs/latest.log`) and the game console.
    *   Filter for tags: `[COMBAT]`, `[INPUT]`, `[STATE]`, `[WEAPON]`.

3.  **Code Instrumentation:**
    *   Use `org.example.common.util.DebugLogger` to add new debug points.
    *   `DebugLogger.logWeapon("Transitioning to: {}", newState);`

## Common Scenarios

### Attack Not Triggering
1.  Enable `logInputEvents`.
2.  Press the key.
3.  If no log appears, the input isn't reaching the server (ClientForgeEvents issue?).
4.  If log appears ("Action: WEAPON"), enable `logWeaponActions`.
5.  Check if the request is rejected by a condition (e.g., "Locked in animation").

### Wrong Animation
1.  Enable `logWeaponActions`.
2.  Perform the combo.
3.  Check the "Action Key" set in `PlayerCombatState`.
4.  Verify `WeaponDataResolver` is picking up the correct animation override.

### Stamina Not Draining
1.  Enable `logStateChanges`.
2.  Perform action.
3.  Check `PlayerWeaponState` updates.
