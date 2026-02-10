# Documentation Plan

This document outlines the structure for the project's documentation to ensure maintainability and ease of onboarding.

## 1. Core Documentation (Root / docs)
*   `README.md`: General overview, installation, and dependencies.
*   `plan.md`: High-level roadmap and TODO list.
*   `mod_structure.md`: Explanation of the package structure (Common, Client, Network, Capabilities).

## 2. Mechanic Guides (docs/mechanics)
*   `Global_Mechanics.md`: I-frames, stamina, healing, sharpness.
*   `Combat_System.md`: How the Better Combat integration works, hitboxes, and animation system.
*   `Decorations.md`: Implementation of jewels and slots.

## 3. Weapon Implementation (docs/weapons)
*   **Summary Files:** `[WeaponName]_Implementation_Summary.md` (e.g., `LS_Session_Summary_20260207.md`).
*   **Design Docs:** `[WeaponName].md` describing movesets, motion values, and special mechanics.
*   **New Weapon Guide:** `New_Weapon_Implementation_Guide.md` (Created).

## 4. Developer Guides (docs/dev)
*   `Debugging_Guide.md`: How to use the logging system (Created).
*   `Network_Protocol.md`: Explanation of C2S and S2C packets.
*   `Capability_System.md`: Breakdown of `PlayerCombatState`, `PlayerWeaponState`.

## 5. Maintenance
*   Updates should be made to `plan.md` after every major feature.
*   Weapon summaries should be updated when mechanics change.
