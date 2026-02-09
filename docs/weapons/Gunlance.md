# Gunlance (GL) — Technical Implementation Plan

**Overview:**
The explosive cousin of the Lance. Combines long-reaching thrusts with shell fire. Great guarding capabilities. Features complex resource management (Shells, Wyrmstake, Wyvern's Fire Gauge) and explosive combos.

## 1. Core Mechanics & Architecture (MHWilds Specification)

### 1.1. Shelling System
*   **Ammo**: Requires reloading. Consumed by Shelling, Charged Shelling, Burst Fire.
*   **Shelling Types**:
    *   **Normal**: Highest capacity (6). Best **Burst Fire** (Full Burst).
    *   **Long**: Medium capacity (4). Stronger **Wyrmstake Cannon**. Increased shelling range.
    *   **Wide**: Low capacity (2). Strongest **Shelling** and **Wyvern's Fire**.
*   **Mechanics**:
    *   **Shelling**: Instant fixed fire damage. Ignores hitzones ("burns through hide").
    *   **Charged Shelling**: Hold fire button. Higher damage.
    *   **Burst Fire (Full Burst)**: Fires all remaining shells at once.

### 1.2. Wyrmstake Cannon (WSC)
*   **Ammo**: Requires specific "Full Reload" or specialized reloads. Single use capacity.
*   **Action**: Embeds a multi-hit ticking stake that explodes after a short delay.
*   **Wilds Update**:
    *   **Wyrmstake Full Blast**: Fires ALL shells + Wyrmstake simultaneously.
    *   **Multi Wyrmstake Full Blast**: Follow-up attack.

### 1.3. Wyvern's Fire (WF)
*   **Gauge**: New **2-segment Gauge**.
*   **Action**: Consumes 1 segment. Massive multi-hit explosion + Fire damage.
*   **Recharge**: Auto-recovers, or accelerates by landing hits / guarding hits.
*   **Implementation**: `GunlanceItem#useWyvernFire` handles the charge-up state and particle effects.

### 1.4. Focus Mode
*   **Focus Strike: Drake Auger**: A drilling attack that targets wounds. Transitions into Wyrmstake or Shelling.

---

## 2. Key Moveset & Combos

### 2.1. Basic Attacks
*   **Lateral Thrust I, II, III**: Standard poke combo (`Left Click` loop).
*   **Rising Slash**: Vertical upswing. Good for reaching high tails.
*   **Overhead Smash**: Heavy downward slam. Key transition to Burst Fire.
*   **Wide Sweep**: Horizontal heavy swing. High motion value.

### 2.2. Shelling Actions
*   **Shelling**: `Right Click`.
*   **Charged Shelling**: Hold `Right Click`.
*   **Reload**: `Guard` + `Shell`. Refills Shells + Wyrmstake.
*   **Quick Reload**: `Guard` + `Shell` during combo. Refills Shells only.

### 2.3. Combo Recommended Lists

#### **A. Basic Poke-Shell Loop** (Safe, Consistent)
1.  **Lateral Thrust I** (`Left Click`)
2.  **Lateral Thrust II** (`Left Click`)
3.  **Shelling** (`Right Click`)
4.  *Repeat or Quick Reload*

#### **B. Burst Fire (Full Burst) Loop** (High Damage, Normal/Long)
1.  **Rising Slash** (`Right Click` + `Left Click` or `Forward` + `Left Click`)
2.  **Overhead Smash** (`Left Click`)
3.  **Burst Fire** (`Right Click`) - *Fires all shells*
4.  **Wide Sweep** (`Left Click`)
5.  **Quick Reload** (`Right Click` + `Guard` Key)
6.  *Repeat from Overhead Smash*

#### **C. Wyrmstake Cannon Combo**
1.  **Lateral Thrust I** (`Left Click`)
2.  **Lateral Thrust II** (`Left Click`)
3.  **Wide Sweep** (`Left Click`x2 or after Overhead Smash)
4.  **Wyrmstake Cannon** (`Left Click` or `Right Click`) - *Embeds the stake*

#### **D. Wyrmstake Full Blast (Wilds Special)**
1.  **Shelling** (`Right Click`)
2.  **Moving Wide Sweep** (`Forward` + `Left Click` + `Right Click`?)
3.  **Wyrmstake Full Blast** (`Left Click` + `Right Click`)
4.  **Multi Wyrmstake Full Blast** (Follow up Input)

---

## 3. Integration Strategy (Minecraft)

### 3.1. Controls Mapping
*   **Attack (Left Click)**: Lateral Thrusts / Lunging Upthrust (Forward).
*   **Use (Right Click)**: Shelling.
    *   **Hold**: Charged Shelling.
*   **Reload**: `Sneak` + `Right Click`.
*   **Burst Fire**: Triggered contextually if `Right Click` is pressed after `Overhead Smash`.
*   **Wyvern's Fire**: Keybind `R` (Special Weapon Action) while Guarding? Or `Hold R`.

### 3.2. Codebase Status (`GunlanceItem.java` & `PlayerWeaponState`)
*   **Implemented**:
    *   `ShellingType` Enum (NORMAL, LONG, WIDE).
    *   `useShell`: Logic for removing ammo, charged calculations, and `WIDE` bonuses.
    *   `useWyvernFire`: Checks `GunlanceWyvernFireGauge`, sets charging state.
    *   `useWyvernFireBlast`: The actual damage event after charge.
*   **To Do**:
    *   **Wyrmstake Entity**: Need to implement the sticking projectile logic.
    *   **Reload Animations**: Connect `GeckoLib` animations to the reload action.
    *   **Combo Context**: `BetterCombat` configuration to detect "After Overhead Smash" for Burst Fire logic.

---

## 4. Data Config

### 4.1. Player State (`PlayerWeaponState`)
*   `int gunlanceShells`: Current count.
*   `int gunlanceMaxShells`: Based on gunlance tier.
*   `float gunlanceWyvernFireGauge`: 0.0 to 2.0.
*   `boolean gunlanceHasStake`: Loaded status.

### 4.2. JSON Config (`data/mhweapons/gunlance/`)
```json
{
  "shelling_damage_base": 15.0,
  "level_multiplier": 5.0,
  "charged_multiplier": 1.5,
  "wyvern_fire_cooldown": 200
}
```

---

## 5. Implementation Checklist

- [x] **Item Definition**: `GunlanceItem` class structure and properties.
- [x] **Shelling Logic (Base)**: `useShell` implementation for Normal/Long/Wide and Charged shots.
- [x] **Wyvern's Fire Logic**: Charge state and firing mechanism (`useWyvernFire`).
- [x] **State Management**:
    - [x] `gunlanceWyvernFireGauge` (float) in `PlayerWeaponState` — 2-segment gauge (0.0–2.0).
    - [x] `gunlanceHasStake` (boolean) in `PlayerWeaponState`.
    - [x] `gunlanceCharging` / `gunlanceChargeTicks` for WF charge sequence.
    - [x] `gunlanceCooldown` for shell fire cooldown.
    - [x] NBT serialization & sync packet logic for all fields.
- [x] **MaxShells Sync**: `syncMaxShells()` auto-sets capacity from `ShellingType` (Normal:6, Long:4, Wide:2).
- [x] **Reload Mechanics**:
    - [x] `reload()` — Full reload (Shells + Wyrmstake).
    - [x] `quickReload()` — Quick reload (+2 shells, no Wyrmstake).
    - [x] Bound to WEAPON_ALT key; context-sensitive (quick if mid-combo, full if shift).
- [x] **Burst Fire (Full Burst)**:
    - [x] `useBurstFire()` — Fires ALL remaining shells. Normal type bonus.
    - [x] Context trigger: Right Click after Overhead Smash (client + server).
    - [x] Animation override and action key wired.
- [x] **Combat Integration**:
    - [x] BetterCombat weapon attributes: 5-hit combo (Thrust I → Thrust II → Rising Slash → Overhead Smash → Wide Sweep).
    - [x] Weapon data config with motion values for all actions.
    - [x] Client input hint tracks 5-hit combo position.
- [x] **HUD**:
    - [x] Shell icons (gold/dark, count from maxShells).
    - [x] Wyrmstake icon (cyan/dark).
    - [x] Wyvern's Fire 2-segment gauge with cooldown timer.
    - [x] Move name display for all actions.
- [x] **VFX**:
    - [x] Shelling muzzle flash + impact particles.
    - [x] Charged Shelling enhanced particles + explosion.
    - [x] Burst Fire multi-shell blast particles.
    - [x] Wyvern's Fire charge + explosion emitter + recoil.
    - [x] Wyrmstake crit + smoke particles.
- [x] **Input Handling**:
    - [x] RMB: Shelling (tap) / Charged Shelling (hold).
    - [x] Shift+RMB: Wyrmstake Cannon.
    - [x] Special+LMB+RMB: Wyvern's Fire.
    - [x] Alt key: Reload / Quick Reload.
    - [x] Movement lock during WF charge (client + server).
- [ ] **Wyrmstake Entity** (Placeholder — currently uses instant raycast):
    - [ ] Create `WyrmstakeEntity` projectile that sticks to target.
    - [ ] Implement multi-hit ticking damage over time.
    - [ ] Implement delayed final explosion.
- [ ] **Wyrmstake Full Blast** (Wilds mechanic):
    - [ ] Fire all shells + Wyrmstake simultaneously.
    - [ ] Multi Wyrmstake Full Blast follow-up.
- [ ] **Focus Strike: Drake Auger** (Wilds mechanic):
    - [ ] Drill attack on wounds → Wyrmstake finisher.
- [ ] **Audio**:
    - [ ] SFX: Shell fire, Reload clank, Wyvern Fire roar, Burst Fire rumble.
- [ ] **Shelling Type Variants**:
    - [ ] Register separate Long and Wide gunlance items.