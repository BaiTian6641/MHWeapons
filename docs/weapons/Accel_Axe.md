# Accel Axe (Explore) — Technical Implementation Plan

**Overview:**
A rocket-powered Greataxe from *Monster Hunter Explore*. It uses an engine mechanism to propel attacks and dash across the field. It is the "Great Sword for Speedsters."

## 1. Core Mechanics & Architecture

### 1.1. Ignition System (Heat/Fuel)
Rather than a "sharpness" gauge or "spirit" gauge, the AA uses an **Ignition Gauge**.
*   **Fuel Buildup**: Standard attacks (without rocket assist) and Reload actions build Fuel.
*   **Fuel Consumption**: Holding the *Trigger* button during an attack consumes Fuel to execute **Accel Moves**.
*   **Accel Moves**:
    *   Faster startup animation.
    *   Forward momentum (Player slides forward during the swing).
    *   Additional fire/blast damage tick.

### 1.2. Rocket Moveset
*   **Accel Dash**:
    *   Sprint + Attack = Rocket Skates.
    *   The player surfs on the ground at high speed (`MovementSpeed * 1.5`).
    *   Can turn gently. Use `Jump` to perform a **Rocket Jump**.
*   **Grand Slam**:
    *   High-commitment finisher. Rocket straight up -> Gravity Slam down.
    *   **AOE**: Creates a shockwave on impact (Explosion damage).

### 1.3. Wilds Update (Focus & Clash)
*   **Focus Mode (Rocket Thrust)**:
    *   **Move**: **Linear Bore**.
    *   Aim with camera. The player launches forward in a straight line, weapon spinning like a drill.
    *   **Targeting**: Specifically destroys "Wounds" (Wilds mechanic).
*   **Blast Parry (Clash)**:
    *   **Trigger**: Execute an Ignition Attack exactly as getting hit.
    *   **Effect**: The explosive force of the axe ignition neutralizes the incoming attack.
    *   **Result**: Zero damage taken, Monster takes blast damage + recoil.

---

## 2. Integration Strategy

### 2.1. Better Combat Configuration
*   **Attribute Overrides**:
    *   Normal attacks: Standard Axe profile.
    *   Accel attacks: High `attack_range` (due to sliding forward).
*   **Dash Mechanic**:
    *   We can't just use BC "Dash" attribute because it's cooldown-based.
    *   **Solution**: Custom `PlayerTick` handler. If `State == ACCEL_DASH`, apply velocity vector in look direction.

### 2.2. GeckoLib & Particles
*   **The Engine**:
    *   The model needs a visible "Engine/Muffler" bone.
    *   **Particle Emitter**:
        *   Idle: Light smoke.
        *   Accel Move: Burst of flame + dark smoke.
        *   Overheat: Constant steam venting.
*   **Sound Design**: Heavy emphasis on mechanical "Clunk-Hiss-Boom" sounds.

---

## 3. Data Config (`data/mhweapons/accel_axe/`)

```json
// default.json
{
  "max_fuel": 100,
  "accel_dash_consumption_per_tick": 1,
  "grand_slam_explosion_radius": 4.0
}
```

## 4. Class Structure

1.  **`AccelAxeItem`**:
2.  **`AxeEngineCapability`**:
    *   `int fuelLevel`
    *   `boolean isOverheated` (If we implement the Heat mechanic).
3.  **`RocketPhysicsHandler`**:
    *   Handles the "drift" feeling of the Accel Dash. Friction reduction on blocks while dashing.
4.  **`BlastParryEvaluator`**:
    *   Listens to `LivingAttackEvent`. Checks if `player.getUseItemRemainingTicks()` is within the "Ignition Window".

## 5. Development Tasks
- [ ] **Momentum Physics**: Implementing the slide/drift correctly so it doesn't feel like standard creative flight.
- [ ] **Fuel Management**: GUI overlay for the fuel gauge.
- [ ] **Particle System**: Connecting GeckoLib bone positions to vanilla particle spawning.
