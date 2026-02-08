# Global Mechanics Implementation Plan

This document details the implementation of core Monster Hunter mechanics: **Damage Types (Hit Types)**, **Guard Points**, and **Dodging**, within the Minecraft Forge environment using Better Combat.

## 1. Damage Types (Hit Types)

Monster Hunter weapons inflict specific types of damage. Unlike Minecraft's binary generic/magic damage, we need a system that tracks `Sever`, `Blunt`, and `Shot`.

### 1.1. The Types
*   **SEVER (Cut)**:
    *   **Sources**: Great Sword, Long Sword, Dual Blades, Switch Axe, CA (Sword/Axe), IG, Lance.
    *   **Effect**: Capable of cutting tails. High hitzone values on "soft" parts (tails, wings).
*   **BLUNT (Impact)**:
    *   **Sources**: Hammer, Hunting Horn, Charge Blade (Impact Phial), Shield Bashes (SnS/Lance).
    *   **Effect**: Cannot cut tails. Accumulates **Stun (KO)** when hitting the head. Accumulates **Exhaust** when hitting the body.
*   **SHOT (Ammo)**:
    *   **Sources**: Bow, Light/Heavy Bowgun, Gunlance (Shelling).
    *   **Effect**: Distance-dependent damage (Critical Distance). Lower hitzone values generally.

### 1.2. Implementation Architecture

We cannot easily modify vanilla entity models to have "parts". We will simulate hitzones based on hit context.

#### **Class Structure**
```java
public enum MHDamageType {
    SEVER,
    BLUNT,
    SHOT,
    FIXED // For Bombs, GL Shelling, CB Phials (ignores hitzones)
}

public interface IMHWeapon {
    MHDamageType getDamageType(ItemStack stack);
    float getStunValue(ItemStack stack);
}
```

#### **Hitzone Simulation (The `CombatReferee`)**
We will listen to `LivingHurtEvent`.
1.  **Head Detection**:
    *   If `damageSource.getDirectEntity()` is a player, calculate the vector of impact.
    *   If the hit height is within the **Top 20%** of the mob's bounding box, count as **HEAD**.
    *   **Effect**:
        *   `BLUNT`: 1.2x Damage, +Stun Value.
        *   `SEVER`: 1.0x Damage.
        *   `SHOT`: 1.1x Damage (if projectile).
2.  **Tail Detection**:
    *   If hit is in the **Bottom 30%** AND hit is from **Behind** (dot product of look vectors > 0).
    *   **Effect**:
        *   `SEVER`: 1.1x Damage, +TailCut Value.
        *   `BLUNT`: 0.9x Damage.
3.  **Legs/Body**:
    *   Default fallback.
    *   `BLUNT` on Body (not Head) adds **Exhaust**.

### 1.3. Status Accumulation (Capabilities)
We need a standard Capability `IMonsterStatus` attached to all `LivingEntity` (hostiles).
*   `float stunBuildup`: Decays over time. If >= Threshold, apply slowness/stun AI.
*   `float exhaustBuildup`: Decays over time. If >= Threshold, mob moves slower.
*   `float tailIntegrity`: Only decremented by `SEVER`. If <= 0, drop tail item + disable tail attacks (if possible).

---

## 2. Guard Points (GP) & Blocking

Guard Points are transient blocks that occur *during* an attack animation.

### 2.1. Mechanic Definition
*   **Standard Block**: Holding Right Click. Indefinite duration. Low Stamina cost.
*   **Guard Point**: Specific frames in an attack (e.g., *Switch Axe morph*, *Charge Blade roundslash*).
    *   **Bonus**: +1 Guard Level (Less Knockback/Chip Damage).
    *   **Reaction**: Triggers "Counter" frames (e.g., CB creates phial explosion).

### 2.2. Implementation
Integration with **Better Combat** and **GeckoLib/PlayerAnimator**.

1.  **Animation Tracking**:
    *   The `WeaponState` capability tracks the *current animation* and *current tick*.
    *   Each weapon defines a configured window for GPs:
        ```json
        "animations": {
            "morph_slash": { "gp_start": 5, "gp_end": 12 }
        }
        ```

2.  **Event Interception (`LivingAttackEvent`)**:
    *   Fires *before* armor/damage.
    *   **Check**: Is Player in a GP window? AND Is damage source blockable (Physical/Projectile)? AND Is Player facing source?
    *   **Result**:
        *   `event.setCanceled(true)`: Negate all damage.
        *   **Consume Stamina**: If 0, Guard Break (Take full damage).
        *   **Apply Knockback**: Based on `(Damage Value - Guard Value)`.
        *   **Trigger Callback**: `weapon.onGuardPointHit(player)`.

3.  **Guard Levels**:
    *   Small, Medium, Large Knockback thresholds.
    *   GPs usually upgrade the threshold (e.g., Medium Knockback becomes Small).

---

## 3. Dodge (Evasion)

Replacing the standard Minecraft "jump/sprint" evasion with strict i-frame dodging.

### 3.1. Logic
*   **Action**: Pressing Dodge Key (Default: Alt or Double Tap Direction).
*   **State**: Enters `DODGE` state in Weapon Capability.
*   **Animation**: Plays `roll_fwd`, `sidestep_left`, etc.

### 3.2. i-Frames (Invulnerability Frames)
*   **Duration**: Standard 6 ticks (0.3s).
*   **Integration**:
    *   During these ticks, the Player Capability sets `isInvulnerable = true`.
    *   `LivingAttackEvent`: If `isInvulnerable`, cancel event.

### 3.3. Perfect Dodge (Just Evade)
*   If an attack occurs during the *first 3 ticks* of a dodge:
    *   Trigger `onPerfectDodge()`.
    *   **Effect**:
        *   Refill Stamina.
        *   Buff next attack (e.g., Bow Charge Level +1).
        *   Play "Ding" sound.

---

## 4. Integration Summary

### Forge Events Required
| Event | Purpose |
| :--- | :--- |
| `LivingHurtEvent` | Calculating Hitzone modifiers (Head/Tail) and applying Stun/Exhaust. |
| `LivingAttackEvent` | Handling Guard Points and i-Frames (Canceling damage completely). |
| `TickEvent.Player` | Managing Dodge timers and Status decay. |

### Better Combat Interaction
Better Combat handles the *offense* ( swinging the weapon). We are handling the *defense* and *impact*.
*   We use Better Combat's `AttackHandSwingEvent` to sync our state, but we mostly run parallel to it.
