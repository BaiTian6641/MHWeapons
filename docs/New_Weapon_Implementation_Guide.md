# New Weapon Implementation Guide

This guide outlines the steps to implement a new Monster Hunter weapon in the MHWeaponsMod.

## 1. Item Registration
*   **Utility:** `org.example.registry.MHWeaponsItems`
*   **Action:** Register a new `RegistryObject<Item>` using `ITEMS.register`.
*   **Class:** Create a new Item class extending `BlockableWeaponItem` or `GeoWeaponItem` (if using GeckoLib).
    *   Implement `WeaponIdProvider` to return a unique string ID (e.g., `"long_sword"`).

## 2. Handler Implementation
*   **Location:** `org/example/common/combat/weapon/`
*   **Action:** Create a handler class (e.g., `ChargeBladeHandler`).
*   **Structure:**
    *   This class should contain static methods to handle specific actions (`handleAction`, `handleAttack`, etc.).
    *   It should manipulate `PlayerCombatState` and `PlayerWeaponState`.

## 3. Action Routing
*   **Location:** `org/example/common/combat/weapon/WeaponActionHandler.java`
*   **Action:**
    *   In `handleAction`, add a check for your weapon's ID.
    *   Route the `WeaponActionType` (WEAPON, WEAPON_ALT, SPECIAL, etc.) to your handler's methods.

## 4. State Management
*   **Capabilities:** `PlayerWeaponState` holds weapon-specific data (gauges, charges, ammo).
*   **Action:**
    *   If the weapon generally uses a gauge (like Spirit Gauge), use existing fields or add new fields to `PlayerWeaponState`.
    *   If adding new fields, update `PlayerWeaponState`, `PlayerWeaponStateS2CPacket`, and synchronization logic.

## 5. Animation (Better Combat)
*   **Data:** `data/mhweapons/weapon_attributes` (or similar JSON config).
*   **Action:** Define attack animations and hitboxes using Better Combat's JSON format.
*   **Overrides:** Use `WeaponDataResolver` to dynamically change animations based on state (e.g., Spirit Level).

## 6. Client-Side Rendering
*   **Models:** Standard JSON models or GeckoLib models in `assets/mhweapons/geo`.
*   **Icons:** Add texture to `assets/mhweapons/textures/item`.

## 7. Configuration
*   **File:** `MHWeaponsConfig.java`
*   **Action:** Add any necessary tunable parameters (damage multipliers, gauge depletion rates).

## 8. Testing & Debugging
*   **Enable Debug:** Set `logWeaponActions` to `true` in `mhweapons-common.toml`.
*   **Verify:** Check logs for correct action dispatch and state transitions.
