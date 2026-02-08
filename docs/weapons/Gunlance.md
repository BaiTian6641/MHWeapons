# Gunlance (GL) — Technical Implementation Plan

**Overview:**
The explosive cousin of the Lance. Combines long-reaching thrusts with shell fire. Great guarding capabilities.

## 1. Core Mechanics & Architecture (MHWilds Specification)

### 1.0. Official Move List & Terminology
Based on MHWilds research:
*   **Basic Attacks**:
    *   **Lateral Thrust** (I, II, III): Standard poke combo.
    *   **Rising Slash**: Vertical upswing (can chain into Overhead Smash).
    *   **Overhead Smash**: Heavy downward slam (leads to Burst Fire).
    *   **Wide Sweep**: Horizontal heavy swing (high damage).
    *   **Lunging Upthrust**: Gap closer.
*   **Shelling Attacks**:
    *   **Shelling**: Standard fire.
    *   **Charged Shelling**: Held fire (Hold O/Right Click).
    *   **Burst Fire**: Fires all remaining shells (often called Full Burst).
*   **Reload Actions**:
    *   **Reload**: Standard reload (fills Shells + Wyrmstake).
    *   **Quick Reload**: Fast reload during combos (fills Shells only).
    *   **Guard Reload**: (If applicable to type/switch skill, fills fewer shells + Wyrmstake).
*   **Special Moves**:
    *   **Wyvern's Fire**: Multi-hit explosion. Now uses a **2-segment Gauge**. Consumes 1 segment.
    *   **Wyrmstake Cannon**: Embeds the explosive stake.
    *   **Wyrmstake Full Blast**: *New in Wilds*. Fires all Shells AND the Wyrmstake simultaneously.
*   **Focus Mode**:
    *   **Focus Strike: Drake Auger**: *New in Wilds*. A drilling attack that targets Wounds. Transitions into Wyrmstake or Shelling.

### 1.0.1. HUD & UI Elements
*   **Shell Icons**: Displayed as bullet-shaped icons. Count depends on Shell Type (Normal: 6, Long: 4, Wide: 2).
*   **Wyrmstake Icon**: A single stake icon next to the shell bar. Indicates if Wyrmstake is loaded.
*   **Wyvern's Fire Gauge**: A new bar/gauge with 2 segments. Indicates charges available for Wyvern's Fire.
*   **Reload State**: Visual feedback (color change or animation) on icons when empty.

### 1.1. Shelling System
*   **Ammo**: Requires reloading. Consumed by Shelling, Charged Shelling, Burst Fire.
*   **Types**:
    *   **Normal**: Highest capacity (6). Best **Full Burst**.
    *   **Long**: Medium capacity (4). Stronger **Wyrmstake Cannon**. Increased range.
    *   **Wide**: Low capacity (2). Strongest **Shelling** and **Wyvern's Fire**.
*   **Mechanics**:
    *   **Shelling**: Instant fixed fire damage.
    *   **Charged Shelling**: Hold fire button. Higher damage.
    *   **Rapid Fire**: (From MHWilds manual, implies holding button fires continuously?). *Note: Manual mentions "Holding the Shell button now fires continuously" in my previous search, but the specific manual page just says "Charged Shelling... Hold... firing them all at once" for Charged. Need to clarify Rapid Fire vs Charged.*
    *   **Moving Wide Sweep**: Shelling during movement/combo.

### 1.2. Wyrmstake Cannon (WSC)
*   **Ammo**: Requires specific reload (Full Reload or specialized).
*   **Action**: Embeds a staking projectile that ticks damage before exploding.
*   **Wilds Update**:
    *   **Wyrmstake Full Blast**: Fires ALL shells + Wyrmstake.
    *   **Multi Wyrmstake Full Blast**: Follow up to the above?

### 1.3. Wyvern's Fire (WF)
*   **Gauge**: New 2-segment gauge.
*   **Action**: Consumes 1 segment. Massive multi-hit explosion + Fire damage.
*   **Recharge**: Auto-recovers over time, or by landing hits.

### 1.4. Focus Mode
*   **Focus Strike: Drake Auger**: Drill attack on wounds -> Wyrmstake finisher.

---

## 2. Integration Strategy (Minecraft)

### 2.1. Controls Mapping
*   **Left Click (Attack)**: Thrusts (Lateral/Lunging).
*   **Right Click (Use)**: Shelling.
    *   **Hold**: Charged Shelling.
*   **Sneak + Right Click**: Reload (Full).
*   **Sneak + Right Click (during combo)**: Quick Reload.
*   **Right Click + Left Click**: Rising Slash / Burst Fire (Context dependent).
*   **Keybind (Default R)**: Guard? Or use Shield mechanics.

### 2.2. Better Combat Integration
*   Weapons defined as "Polearm" or "Spear" for reach.
*   Custom combo sequence.
*   **Shelling**: Implemented as a "cast" or instant "shoot" via Right Click.

### 2.3. Projectiles
*   **Shells**: Raycast (Hitscan) for instant feedback. `MHDamageType.FIXED`.
*   **Wyrmstake**: Projectile Entity (`WyrmstakeEntity`) that sticks to target.

---

## 3. Data Config & State

### 3.1. Player State (`PlayerWeaponState`)
Usage of existing `PlayerWeaponState` fields:
*   `gunlanceShells`: Current shell count.
*   `gunlanceMaxShells`: Max capacity (based on weapon tier/type).
*   `gunlanceHasStake`: Boolean for Wyrmstake ammo.
*   `gunlanceWyvernfireCooldown`: Replaced/Augmented by Gauge logic.
    *   *New Field needed*: `float gunlanceWyvernFireGauge` (0.0 to 2.0).

### 3.2. Weapon Item (`GunlanceItem`)
*   Extends `GeoWeaponItem`.
*   Properties: `ShellType` (Normal, Long, Wide), `ShellLevel`.

---

## 4. Development Tasks
- [ ] **Item Class**: Create `GunlanceItem` extending `GeoWeaponItem`.
- [ ] **State Management**: Update `PlayerWeaponState` to support WF Gauge.
- [ ] **Shelling Logic**: Implement `use` method for Shelling and Charged Shelling.
- [ ] **Reload Logic**: Implement Reload mechanism.
- [ ] **Wyrmstake**: Create `WyrmstakeEntity` and firing logic.
- [ ] **Wyvern's Fire**: Implement the 2-segment gauge and firing logic.
- [ ] **Animations**: Hook up GeckoLib animations for Shelling/Reloading.
