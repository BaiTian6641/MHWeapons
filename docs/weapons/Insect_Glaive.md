# Insect Glaive (IG) 🐝 — Technical Implementation Plan

**Overview:**
A mobile, pole-based weapon that commands a symbiotic insect (Kinsect) to harvest buffs (Extracts) from monsters. The weapon defines "Aerial Combat" mechanics for Minecraft.

## 1. Core Mechanics & Architecture

### 1.1. The Kinsect System
The Kinsect is not just a projectile; it is a persistent entity state attached to the player or flying in the world.

*   **Logic (Server-Authoritative)**:
    *   **States**: `Docile` (On Arm), `Flying` (Moving to Target), `Hovering` (Stationary/Attacking), `Returning` (Moving to Player).
    *   **Entity**: `KinsectEntity extends Projectile` (or `TamableAnimal` if complex AI needed, but Projectile is cleaner for performance).
    *   **Stamina**: Kinsect has a stamina bar. Depletes while flying. Recall to recover.
*   **Interaction**:
    *   **Aim**: Hold specific key (default: `V` or `Mouse 4`) to enter "Aim Mode" (zooms camera, shows reticle).
    *   **Fire**: Left Click while Aiming -> Send Packet `KinsectLaunch`.
    *   **Recall**: Right Click while Aiming -> Send Packet `KinsectRecall`.

### 1.2. The Extract System (Buffs)
The IG power budget relies on "Triple Up" (Red + White + Orange).

*   **Capability**: `IGExtractCapability` attached to Player.
    *   **Slots**: Red, White, Orange, Green (Heal, instant).
    *   **Logic**:
        *   **Red (Attack)**: Changes Player Animator set to "Enhanced" (faster, multi-hit).
        *   **White (Speed)**: Movement Speed + Jump Height.
        *   **Orange (Defense)**: Defense + Flinch Free (Knockback Resist).
        *   **Triple Up**: When all 3 active, duration resets, and bonuses increase (~15%).
*   **Hitzone Mapping (CombatReferee)**:
    *   `HEAD` -> **Red**
    *   `LEGS/WINGS` -> **White**
    *   `BODY/TAIL` -> **Orange**
    *   `TAIL TIP` -> **Green**

### 1.3. Aerial Combat
Implementing true 3D movement.

*   **Vault Action**: Special Key (default: `Space` + `Block` or dedicated).
    *   Applies vertical velocity.
    *   Sets state: `AERIAL_MODE` (prevents fall damage until land).
    *   **Finalizer (Ground)**: The canonical ground finalizer is **Overhead Smash** — perform Alt after the 3rd LMB hit (Basic Combo: Rising Slash → Reaping Slash → Double Slash → Overhead Smash).
*   **3-Extract Finisher**: If you have **Red + White + Orange** extracts, hold Charge and release to start the 3-extract finisher sequence: **Tornado Slash** → **Strong Descending Slash** → **Rising Spiral Slash**. This consumes all extracts and deals heavy damage.
*   **Air Dash**:
    *   Mid-air Jump (consumes Stamina).
    *   Uses `Vec3` velocity overrides.
*   **Helicopter (Jumping Advancing Slash)**:
    *   If `Red` extract is active.
    *   On hit -> "Bounce" (Reset vertical velocity to stay airborne).
    *   Max bounces: 3 (Configurable).

---

## 2. Integration with Mods

### 2.1. Better Combat
*   **Ground Moves (Normal)**: Standard combo sequence.
*   **Ground Moves (Red Buff)**: We need to swap the attributes.
    *   **Method**: `IGWeaponHandler` listens to `TickEvent`. If Red Buff state changes, it modifies the `ItemAttributeModifier` or sends a packet to client to play different animation sets.
    *   *Alternative*: Use two different weapon data files `ig_normal.json` and `ig_red.json` and swap the underlying logic, but BC loads data on startup.
    *   *Selected Approach*: Use **Animation Branching**. The `attack` animation in GeckoLib will have a boolean parameter `is_empowered`.
        *   BC Weapon JSON defines *all* hitboxes (Normal and Red).
        *   Code logic validates: "If Normal, ignore hits 4-6 of the swing".
        *   *Simpler*: Just give Speed Effect (Red) and rely on motion values.

### 2.2. GeckoLib
*   **Kinsect Model**: Attached to the Glaive model during `Docile`. Hidden and spawned as Entity during `Flying`.
*   **VFX**: Glow path based on held extract (Red/White/Orange/Green particles).

---

## 3. Data-Driven Rulesets (`data/mhweapons/insect_glaive/rulesets/`)

We can toggle mechanics between generations (World vs Rise vs Wilds).

```json
// frontier.json
{
  "kinsect_stamina_cost": 2.0,
  "can_aerial_bounce": true,
  "bounce_cap": 3,
  "triple_duration": 90,
  "moveset": "magnet_spike_hybrid" // Example
}
```

```json
// mhwilds.json
{
  "can_aim_glide": true,
  "kinsect_auto_attack": true,
  "triple_duration": 60
}
```

## 4. Class Structure Plan

1.  **`KinsectEntity`**:
    *   Fields: `ownerUUID`, `currentExtract`, `stamina`.
    *   Methods: `launch(target)`, `recall()`, `onHitEntity(target)`.
2.  **`IGAerialController`**:
    *   Client-side event listener for Inputs.
    *   Handles Vault and Air Dash logic.
3.  **`ExtractCapability`**:
    *   Timers for R, W, O.
    *   Sync packets to client for HUD.
4.  **`ItemInsectGlaive`**:
    *   Custom renderer (GeckoLib).
    *   NBT data to store `kinsectType` (Speed / Power / Heavy).

## 5. Development Tasks
- [ ] **Kinsect Entity**: Create basic flying entity.
- [ ] **Extract Logic**: Implement Map<BodyPart, Color>.
- [ ] **Aerial State**: Implement `AirBorne` tag to disable fall damage.
- [ ] **Model**: Glaive model + Kinsect model.

---

## 10) Implementation Tasks (small and prioritized)
1. Data schema & sample rulesets (S)
   - Create `kinsect/` and `extracts/` sample JSONs with default and `mhwilds` variants.
2. BC: Implement `PoleVaultAction`, `AerialState`, `AerialAttack` nodes (M)
3. Entity: Implement server-side `KinsectEntity` with state machine & homing physics (M)
4. Gameplay: `ExtractManager` to track collected extracts per-player and to apply buff effects on return (M)
5. Item: `InsectGlaiveItem` hooking to BC actions and kinsect commands (M)
6. GL: Placeholder kinsect model + player aerial animations and buff VFX (M)
7. HUD: Basic on-screen kinsect state & buff indicators (S)
8. Tests & Playtests (M) — hostage to iteration

---

## 11) Risks & Mitigations
- Timing & Latency: aerial and kinsect operations are timing-sensitive and can be disrupted by lag. Mitigation: server-authoritative hit validation, client-side prediction for visuals only, and conservative reconciliation.
- Server Load: naive homing physics for many kinsects could be expensive. Mitigation: limit active kinsects per player, tunable homing frequency, and cheap homing approximations.
- Confusing UX: multiple extract types and stack rules can be confusing. Mitigation: clear HUD and particle feedback, and sensible defaults that mirror canonical behavior.

---

## 12) Acceptance Criteria
- Vault & Aerial states are responsive and allow intended aerial combos and landing follow-ups.
- Kinsect launches, hits, collects extracts, and returns reliably with server-validated extracts and buff application.
- Buffs are applied with correct duration, stacks, and visible HUD indicators; behavior is consistent across clients under latency.

---

## 13) Sources & Notes
- Community (Reddit) discussions indicate IG has strong momentum in recent generation(s) and that kinsect & aerial flow changes are a focus; official wiki pages were blocked by ad/redirect during automated fetches but archived snapshots exist for reference if you want verbatim source text.

---

Would you like sample `data/mhweapons/insect_glaive/default.json` and `mhwilds.json` rulesets created next, or should I scaffold `KinsectEntity` and `PoleVaultAction` classes for server-side testing first?