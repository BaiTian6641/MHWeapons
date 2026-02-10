# Charge Blade — Implementation Summary & Plan (2026-02-09)

## Summary of Changes

### Core Implementation
- Added a full Charge Blade combat handler with sword/axe combos, phial logic, shield charge, sword boost, power axe, guard points, and discharge chain.
- Extended player weapon state with all Charge Blade fields (timers, buffs, combo state, guard point window) and full serialization.
- Added tick logic for Charge Blade timers and combo timeout handling.
- Expanded HUD for Charge Blade to show energy gauge, phials, and buff indicators.

### Input & Combo Fixes
- Routed RMB to `WEAPON_ALT` for Charge Blade (Shield Thrust / Discharge chain).
- Routed LMB to `WEAPON` on press for Charge Blade and suppressed default attack key while pressed.
- Prevented keyboard weapon action (X) from triggering basic attacks for Charge Blade; it now triggers weapon skills.
- Disabled LMB release fallback for Charge Blade to avoid duplicate attack triggers.
- Adjusted Charge Blade to use raw mouse LMB state (like Long Sword) to prevent missed taps.

### Animation & HUD Sync
- Charge Blade forced animations now use configurable timing from weapon data.
- Charge Blade forced animations now include action keys/ticks in packets so the Current Attack HUD can display the active move correctly.
- Updated Current Attack HUD labels to use official Charge Blade names (incl. AED/SAED notation).

### Data Config Updates
- Rebuilt `weapon_attributes/charge_blade.json` with mode-gated attacks for sword and axe.
- Rebuilt `weapons/charge_blade.json` with motion values, phial config, and animation timing.

## Files Touched (Key)
- src/main/java/org/example/common/combat/weapon/ChargeBladeHandler.java
- src/main/java/org/example/common/combat/weapon/WeaponActionHandler.java
- src/main/java/org/example/common/combat/CombatReferee.java
- src/main/java/org/example/common/combat/weapon/WeaponStateEvents.java
- src/main/java/org/example/common/capability/player/PlayerWeaponState.java
- src/main/java/org/example/client/input/ClientForgeEvents.java
- src/main/java/org/example/client/ui/WeaponHudOverlay.java
- src/main/resources/data/mhweaponsmod/weapons/charge_blade.json
- src/main/resources/data/mhweaponsmod/weapon_attributes/charge_blade.json

## Current Behavior (Expected)
- LMB: Sword/Axe combo chain.
- RMB: Shield Thrust / Discharge chain.
- Keyboard: Weapon skills (X/C/F/R) without triggering basic attacks.
- Current Attack HUD shows Charge Blade move names in real-time.

## Plan / Next Steps
1. **Verify in-game combo flow**
   - Sword combo: Weak Slash → Return Stroke → Roundslash.
   - Axe combo: Rising Slash → Overhead Slash.
   - RMB discharge chain: ED I → ED II → AED/SAED.

2. **Tune animation timing per move (optional)**
   - If any move still feels too fast/slow, add per-action timing or refine `animationTiming` in data.

3. **Balance & polish**
   - Adjust motion values and phial damage based on playtesting.
   - Validate buff durations and guard point windows feel correct.

4. **Extended Wilds features (future)**
   - Focus Strike wound trigger for Power Axe Mode.
   - Additional Wilds moves (e.g., Fade Slash) if desired.