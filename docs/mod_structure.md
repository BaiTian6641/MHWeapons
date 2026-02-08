# MHWeaponsMod — Detailed Mod Structure

This document defines the **complete mod architecture** for implementing Monster Hunter-style combat in Minecraft Forge (1.20.1), with Better Combat for hit logic and GeckoLib for animations.

---

## 1. High-Level Architecture

### 1.1. Core Goals
- **Faithful MH Feel**: hit-stop, motion-value attacks, counters, guard points, and wound systems.
- **Server Authority**: all damage, status, and state transitions occur server-side.
- **Data-Driven**: weapon logic, motion values, hitboxes, and rulesets are externalized in JSON.

### 1.2. Runtime Layers
1.  **Input Layer (Client)**
    - Handles keybinds (Focus Mode, Special Actions, Dodge).
    - Sends packets to server to request actions.
2.  **Action Layer (Server)**
    - Validates action requests.
    - Executes combat logic.
3.  **Combat Layer (Server)**
    - Applies damage formula + hit type.
    - Checks guard points, dodge i-frames, hitstop, wounds.
4.  **Feedback Layer (Client)**
    - Animations, VFX, SFX, HUD.

---

## 2. Package Layout (`src/main/java/org/example/mhweaponsmod/`)

```
org.example.mhweaponsmod
├── MHWeaponsMod.java
├── client/
│   ├── ClientSetup.java
│   ├── input/                 # Keybinds (Focus, Special, Dodge)
│   ├── render/                # GeckoLib item renderers
│   └── hud/                   # Gauges, buffs, extract icons
├── common/
│   ├── capability/
│   │   ├── player/             # Stamina, weapon state, dodge timers
│   │   └── mob/                # Wounds, stun/exhaust, part health
│   ├── combat/
│   │   ├── CombatReferee.java  # Central damage/hitzone logic
│   │   ├── GuardSystem.java    # Guard, Guard Points, Perfect Guard
│   │   ├── DodgeSystem.java    # i-frames, Perfect Dodge
│   │   └── WoundSystem.java    # Wound creation/consumption
│   ├── config/                 # Forge config + JSON schema loaders
│   ├── data/                   # JSON loaders (weapon configs)
│   ├── entity/                 # Kinsect, Wyrmstake, Magnet Marker
│   ├── item/                   # All weapon items
│   ├── network/                # Packets
│   └── util/                   # Math helpers, raycast utils
├── registry/
│   ├── ModItems.java
│   ├── ModEntities.java
│   └── ModSounds.java
└── weapons/
    ├── base/                   # Shared weapon logic (interfaces)
    ├── greatsword/
    ├── longsword/
    ├── chargeblade/
    ├── insectglaive/
    └── ...
```

---

## 3. Data-Driven Resources

### 3.1. JSON Directory Layout
```
src/main/resources/data/mhweapons/
├── rulesets/
│   ├── default.json
│   └── mhwilds.json
├── weapons/
│   ├── great_sword.json
│   ├── long_sword.json
│   └── ...
├── actions/                    # Motion values, frames, hitbox definitions
├── hitzones/                   # Per mob hitzone tables (optional)
└── effects/                    # Buff/debuff definitions
```

### 3.2. Assets
```
src/main/resources/assets/mhweaponsmod/
├── models/item/
├── textures/item/
├── animations/
├── sounds/
└── particles/
```

---

## 4. Combat Pipeline (Server)

1.  **Action Request**: Client sends packet (`C2S`) to request a weapon action.
2.  **Validation**: Server checks stamina, weapon state, cooldowns, gauge.
3.  **Attack Execution**:
    - Uses Better Combat hit detection.
    - Calculates motion value and hit type.
4.  **CombatReferee**:
    - Applies hitzone multipliers.
    - Resolves Guard Points, Perfect Guard, Dodge i-frames.
    - Applies status buildup (stun, exhaust, bleed).
5.  **Result Broadcast**:
    - Sends `S2C` packet for effects, animations, and HUD updates.

---

## 5. Core Systems for MH Feel

### 5.1. Hit Stop
*   Heavy hits temporarily freeze player and target to simulate impact.
*   Implemented in `HitStopManager` with a per-entity tick freeze.

### 5.2. Motion Values
*   Each action has a **Motion Value (MV)**.
*   Final damage = `Raw * MV * Hitzone`.

### 5.3. Guard Points / Perfect Guard
*   `GuardSystem` checks the animation frames for guard windows.
*   Perfect guard triggers clash animations and zero stamina cost.

### 5.4. Dodge i-Frames
*   `DodgeSystem` grants invulnerability for X ticks.
*   Perfect dodge triggers buffs or counter attacks.

---

## 6. Client Systems

### 6.1. GeckoLib
*   All weapon models use GeckoLib animations.
*   Player animation states updated through `PlayerAnimator`.

### 6.2. HUD
*   Weapon gauges (Spirit, Phials, Demon, etc.).
*   Extracts and buffs (IG, HH, etc.).

---

## 7. Networking

### 7.1. Packet Types
- `C2S_WeaponAction`: Request action execution.
- `S2C_ActionResult`: Confirm attack + apply animation.
- `S2C_GaugeSync`: Sync weapon gauges to client.
- `S2C_WoundUpdate`: Sync wound states for highlight.

---

## 8. Implementation Priority (Recommended)
1.  **Global Mechanics** (`CombatReferee`, `DodgeSystem`, `GuardSystem`).
2.  **Weapon Framework** (Interfaces, JSON loading, base item class).
3.  **One Weapon Prototype** (GS or LS) to validate pipeline.
4.  **Integrate Wilds Mechanics** (Focus Mode, Wounds, Clash).
