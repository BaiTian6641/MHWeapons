# Monster Hunter Wilds — Mechanics Summary & Integration Guide

This document summarizes the new mechanics introduced in *Monster Hunter Wilds* and how they map to our Minecraft Mod implementation.

## 1. Global Mechanics

### 1.1. Focus Mode
*   **Concept**: A toggleable aiming mode (L2/Left Trigger) that highlights "Wounds" on the monster.
*   **Mod Implementation**:
    *   **Input**: Keybind (Default `Left Alt`).
    *   **Visuals**: Highlights specific hitboxes (Wounds) on the target mob using a shader or glowing outline.
    *   **Effect**: Allows 360° aiming for attacks that are normally direction-locked.
    *   **Focus Strike**: A special attack input available only in this mode.

### 1.2. Wounds
*   **Concept**: Damaging the same part repeatedly opens a "Wound".
*   **Mod Implementation**:
    *   **Logic**: `MonsterStatusCapability` tracks damage per part.
    *   **Threshold**: When `Damage > WoundThreshold`, set `isWounded = true`.
    *   **Benefit**: Hitzone Value increases (Mob takes more damage).
    *   **Pop**: Focus Strikes destroy the wound for massive damage, resetting the threshold.

### 1.3. Clash / Offset Attacks
*   **Concept**: Hitting the monster exactly as it hits you.
*   **Mod Implementation**:
    *   **Event**: `LivingAttackEvent`.
    *   **Logic**: If `PlayerState` is `ATTACKING` AND `AttackType` is `OFFSET_CAPABLE` AND `Timing` is `Perfect`:
        *   Cancel Player Damage.
        *   Trigger "Clash Win" animation.
        *   Apply Stagger/Knockback to Monster.

---

## 2. Weapon-Specific Updates

| Weapon | Focus Strike | Clash / Offset Mechanic | Notes |
| :--- | :--- | :--- | :--- |
| **Great Sword** | **Offset Uppercut**: Blocks attack and chains into Strong Charge. | **Guard Tackle**: Hyper-armor tackle. | Focus Mode allows aiming TCS. |
| **Long Sword** | **Spirit Counter**: Parries and chains to Spirit III. | **Spirit Release**: Dumps gauge for damage. | Sacred Sheath logic refined. |
| **Sword & Shield** | **Guard Slash Offset**: Repels monster, chains to Perfect Rush. | **Lunging Stab**: Pinpoint wound breaker. | Slide Slash added for mobility. |
| **Dual Blades** | **Offset Dodge**: Perfect dodge triggers auto-counter. | **Spinning Lunge**: Multi-hit wound breaker. | Demon Mode auto-counter. |
| **Hammer** | **Offset Uppercut**: Stops monster charge. | **Stationary Slam**: Heavy wound breaker. | Keep Sway (Dodge while charging). |
| **Hunting Horn** | **Offset Recital**: Hyper-armor performance. | **Sonic Blast**: Point-blank wound breaker. | **Echo Bubbles**: Songs create AoE zones. |
| **Lance** | **Power Clash**: Button mash struggle. | **Corkscrew Thrust**: Drilling wound breaker. | Perfect Guard returns. |
| **Gunlance** | **Offset Guard Shot**: Shelling deflects attack. | **Super Wyrmstake**: Drills wound. | Rapid Fire Shelling added. |
| **Switch Axe** | **Compressed Finisher**: Element discharge. | **Offset Morph**: Morphing deflects attack. | Focus Mode allows aiming discharge. |
| **Charge Blade** | **Savage Axe Rip**: Saw blade wound opener. | **Guard Point**: Standard GP (Buffed). | Overlimit Mechanic (Power Phials). |
| **Insect Glaive** | **Descending Thrust**: Aerial wound breaker. | **Kinsect Clash**: Kinsect intercepts attack. | Kinsect creates extract clouds. |
| **Bow** | **Tracer Shot**: Homing arrow. | **Offset Shot**: Arrow deflects attack. | Homing arrows in Focus Mode. |
| **LBG / HBG** | **Focus Scope**: Snipe wound. | **Wyvern Counter**: Blast deflects attack. | Rapid Fire/Siege Mode refined. |
| **Tonfa** | **Drill Mode**: Multi-hit focus. | **Jet Counter**: Air dash deflects attack. | Impact Conversion logic. |
| **Magnet Spike** | **Magnet Gun**: Tags wound. | **Magnetic Parry**: Repel field deflects. | Pin Finisher logic. |
| **Accel Axe** | **Rocket Thrust**: Linear drill. | **Blast Parry**: Ignition deflects attack. | Momentum physics. |

## 3. Implementation Priority
1.  **Global Mechanics**: `CombatReferee` (Wounds/Hitzones) and `FocusHandler` (Camera/Input).
2.  **Weapon Core**: Basic combos + Better Combat integration.
3.  **Wilds Features**: Adding the specific Clash/Focus moves on top of the core.
