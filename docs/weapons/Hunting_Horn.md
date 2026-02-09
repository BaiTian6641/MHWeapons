# Hunting Horn (HH) — Technical Implementation Plan

**Overview:**
A blunt weapon that excels at providing support via melodies and echo bubbles. The implementation emphasizes the **Rise/Wilds** "Performance Mode: Echo" style, prioritizing combat fluidity and instant buffs over the rigid recital mode of older titles. Incorporates the **Iceborne Echo Attack** (Echo Waves through active bubbles on Performance) alongside the full Wilds moveset.

## 1. Core Mechanics & Architecture

### 1.1. Note & Song System
*   **Notes**: Attacks generate notes on a musical staff (HUD).
    *   **Note 1 (Blue)**: Left Swing.
    *   **Note 2 (Red)**: Right Swing.
    *   **Note 3 (Green)**: Backwards Strike / Overhead Smash.
*   **Queue**: Stores the last `N` notes (Configurable via `noteQueueSize`, default 5).
*   **Resolution**:
    *   **Auto-Match**: Checked on every note input. If a pattern matches, the melody is auto-stocked (Wilds behavior).
    *   **Performance (Recital)**: On pressing `Special`, stocked melodies are played and buffs applied to nearby allies.

### 1.2. Key Moveset (Base)
*   **Left Swing (Note 1)**:
    *   **Input**: `Left Click` (WEAPON).
    *   **Action ID**: `note_one`.
    *   **Details**: Basic horizontal swing. Produces Note 1 on the staff.
*   **Right Swing (Note 2)**:
    *   **Input**: `Right Click` (WEAPON_ALT).
    *   **Action ID**: `note_two`.
    *   **Details**: Basic horizontal swing (opposite direction). Produces Note 2.
*   **Backwards Strike / Overhead Smash (Note 3)**:
    *   **Input**: `Shift` + `Left/Right Click` OR `Both Clicks` (HORN_NOTE_BOTH).
    *   **Action ID**: `note_three`.
    *   **Details**: High-damage swing. Produces Note 3. Can chain into follow-ups.
*   **Flourish**:
    *   **Input**: `Right Click` during an active combo.
    *   **Action ID**: `flourish`.
    *   **Details**: Produces Note 2. Can chain further note inputs during the attack.
*   **Hilt Stab (Counter)**:
    *   **Input**: `Shift` + any note input during an active combo.
    *   **Action ID**: `hilt_stab`.
    *   **Details**: Quick poke that produces a note. Grants a counter window (`hornSpecialGuardTicks`). If hit during the window, reduces damage by 70% and auto-stocks current notes as a melody.
*   **Perform / Recital**:
    *   **Input**: `F` (Special Action).
    *   **Action ID**: `recital`.
    *   **Details**: Plays stocked melodies, applying buffs to nearby allies. Also deals AoE damage. If used after a recent melody, acts as **Encore** (enhanced amplifier + extended duration).
*   **Performance Beat**:
    *   **Input**: `F` during an active Perform.
    *   **Action ID**: `performance_beat_1` / `_2` / `_3`.
    *   **Details**: Chain up to 3 beats, each playing the next stocked melody with escalating damage.
*   **Encore**:
    *   **Input**: After 3 Performance Beats, or `Special` after recent melody.
    *   **Action ID**: `encore`.
    *   **Details**: Boosts and extends the last melody effect (Amplifier + 1, Duration × 2). Highest damage in the performance chain.
*   **Echo Bubble**:
    *   **Input**: `Shift` + `F` (Special Action).
    *   **Action ID**: `echo_bubble`.
    *   **Details**: Spawns a stationary `EchoBubbleEntity`. Buff type determined by equipped horn's `fixedEchoBubble` config.

### 1.3. Iceborne Echo Attack (Echo Waves)
*   **Trigger**: Any Performance action (Recital, Performance Beat, or Encore) near active Echo Bubbles.
*   **Effect**: Each nearby bubble "resonates", dealing AoE damage to monsters within its radius.
*   **Implementation**: `HuntingHornHandler.triggerEchoResonance()` — searches for `EchoBubbleEntity` instances within 8 blocks, applies damage and spawns Sonic Boom particles.
*   **Damage**: `baseDamage × 3.0` motion value per resonating bubble.

### 1.4. Wilds Update (New Mechanics)
*   **Echo Bubble System**:
    *   **Entity**: `EchoBubbleEntity` — stationary AoE field.
    *   **Buff Tick**: Every 20 ticks, applies potion effect to players in radius.
    *   **Special Effects**: Speed bubbles grant stamina boost, Strength bubbles grant affinity, Absorption bubbles cleanse all debuffs.
*   **Offset Melody**:
    *   *Pending Implementation*.
    *   **Concept**: Super Armor performance that nullifies incoming attacks.
*   **Focus Strike (Reverb)**:
    *   *Pending Implementation*.
    *   **Concept**: Point-blank sound wave targeting wounds. Produces up to 6 notes with rhythm timing for bonus damage.

---

## 2. Integration Strategy

### 2.1. Handler Architecture
Logic is centralized in `HuntingHornHandler.java` (extracted from `WeaponActionHandler`), following the same pattern as `LongSwordHandler` and `SwitchAxeHandler`:
*   **Entry Point**: `handleAction(action, pressed, player, combatState, weaponState)`.
*   **Sub-handlers**:
    *   `handleNoteAttack` → Routes note generation (Left/Right/Both).
    *   `handleHiltStab` → Counter window + quick note.
    *   `handleFlourish` → Combo chain note producer.
    *   `handlePerform` → Recital: song matching, melody play, echo resonance.
    *   `handlePerformanceBeat` → Chained R2 hits during performance.
    *   `handleEncore` → Enhanced melody replay after beat chain.
    *   `handleEchoBubble` → Bubble entity spawning.
*   **Song Resolution**:
    *   `resolveSongMatch` → Full queue check against JSON patterns.
    *   `resolveAutoSongMatch` → Per-note instant check (Wilds auto-stock).
    *   `resolveMelodyPlay` / `resolveMelodyById` → Stocked melody consumption.
*   **Buff Application**:
    *   `applyMelodyEffect` → AoE `MobEffectInstance` to players in AABB.
    *   `applyHornSongBuff` → Internal stat tracking (`hornAttackSmallTicks`, etc.).
*   **Echo Resonance**: `triggerEchoResonance` → Iceborne Echo Attack.
*   **Counter Callback**: `onCounterHit` → Called from damage events during Hilt Stab window.

### 2.2. Combo Naming & Progression

| Input | Action ID | Note | Result |
| :--- | :--- | :--- | :--- |
| **LMB** | `note_one` | 1 | Left Swing → adds Note 1 |
| **RMB** | `note_two` | 2 | Right Swing → adds Note 2 |
| **LMB+RMB / Shift** | `note_three` | 3 | Backwards Strike → adds Note 3 |
| **Shift + Note (in combo)** | `hilt_stab` | any | Quick poke + counter window |
| **RMB (in combo)** | `flourish` | 2 | Chain attack, produces Note 2 |
| **Special (F)** | `recital` | — | Plays melody + AoE hit + Echo Resonance |
| **Special (during Perform)** | `performance_beat_N` | — | Chain beat (up to 3) + melody play |
| **After 3 beats** | `encore` | — | Boosted melody replay + max AoE |
| **Shift + Special** | `echo_bubble` | — | Spawns fixed Echo Bubble entity |

### 2.3. Audio
*   **Status**: Basic sound events. Distinct samples needed for Notes 1, 2, and 3.
*   **Planned**: Pitch-shifting or layered stems per note; full measure on Recital.

---

## 3. Data Config (`data/mhweaponsmod/weapons/hunting_horn.json`)

```json
{
  "id": "hunting_horn",
  "noteQueueSize": 5,
  "fixedEchoBubble": "attack_affinity",
  "motionValues": {
    "note_one": 1.0,
    "note_two": 1.0,
    "note_three": 1.2,
    "flourish": 0.8,
    "hilt_stab": 0.6,
    "recital": 1.4,
    "performance_beat": 1.2,
    "encore": 2.0,
    "echo_resonance": 3.0
  },
  "songs": [
    { "id": "self_improvement", "pattern": [1, 1], "bubble": "speed" },
    { "id": "attack_up_small", "pattern": [1, 3, 1], "bubble": "attack_small" },
    { "id": "healing_small", "pattern": [1, 2, 1], "bubble": "heal_small" },
    { "id": "attack_up_large", "pattern": [1, 3, 1, 2], "bubble": "attack_large" }
  ],
  "echoBubbles": [
    { "id": "speed", "effect": "minecraft:speed", "amplifier": 1, "duration": 400, "radius": 3.5 },
    { "id": "attack_small", "effect": "minecraft:strength", "amplifier": 0, "duration": 400, "radius": 3.5 },
    { "id": "attack_large", "effect": "minecraft:strength", "amplifier": 1, "duration": 400, "radius": 3.5 }
  ]
}
```

Additional rules config: `data/mhweapons/hunting_horn_rules/mhwilds.json`

## 4. Class Structure

1.  **`HuntingHornHandler`** (`common/combat/weapon/HuntingHornHandler.java`):
    *   Final utility class (private constructor). Entry point: `handleAction()`.
    *   Internal records: `BubbleDef`, `SongMatch`, `MelodyPlay`.
    *   Contains all note, song, melody, bubble, performance, and resonance logic.
2.  **`EchoBubbleEntity`** (`common/entity/EchoBubbleEntity.java`):
    *   Stationary entity with synched data (effect, amplifier, duration, radius).
    *   Handles collision logic: applies buff to players every 20 ticks.
    *   Particle rendering (splash + colored dust per effect type).
3.  **`PlayerWeaponState`** (`common/capability/player/PlayerWeaponState.java`):
    *   Note queue: `hornNoteA..E`, `hornNoteCount`.
    *   Melody stock: `hornMelodyA..C`, `hornMelodyCount`, `hornMelodyIndex`.
    *   Buff tracking: `hornAttackSmallTicks`, `hornAttackLargeTicks`, `hornDefenseLargeTicks`, etc.
    *   Counter: `hornSpecialGuardTicks` (Hilt Stab window).
    *   Encore: `hornLastMelodyId`, `hornLastMelodyEnhanceTicks`.
4.  **`WeaponActionHandler`** (`common/combat/weapon/WeaponActionHandler.java`):
    *   Delegates to `HuntingHornHandler.handleAction()` (same pattern as LS/SA).

## 5. Implementation Checklist

### Core Systems
- [x] **Separated Handler**: `HuntingHornHandler.java` extracted from `WeaponActionHandler`.
- [x] **Note Generation**: Left Swing (1) / Right Swing (2) / Backwards Strike (3).
- [x] **Song Pattern Matcher**: JSON-based matching via `resolveSongMatch`.
- [x] **Auto-Stock**: Wilds-style instant melody stocking on note input.
- [x] **Buff Application**: AoE `MobEffectInstance` to team within radius.
- [x] **Echo Bubbles**: Entity spawning (`EchoBubbleEntity`) with per-effect particles.

### Wilds Moveset
- [x] **Flourish**: RMB during combo → Note 2 + chain.
- [x] **Hilt Stab**: Back + note during combo → counter window (6 ticks, 70% DR).
- [x] **Perform / Recital**: Full melody play + AoE hit.
- [x] **Performance Beat**: Chain up to 3 beats with escalating damage.
- [x] **Encore**: Enhanced melody replay after beat chain.
- [x] **Echo Resonance (Iceborne)**: Nearby bubbles deal AoE damage on Performance.

### Pending
- [ ] **HUD**: Visual staff showing current notes and stocked melodies.
- [ ] **Offset Melody**: Super Armor performance counter mechanic.
- [ ] **Focus Strike (Reverb)**: Wound-targeting sound wave with rhythm input.
- [ ] **Infernal Melody Gauge**: Fills on hit, unlocks Magnificent Trio.
- [ ] **Models & Animation**: Dedicated backslam, recital dance, echo bubble placement animations.
