# Hunting Horn (HH) — Technical Implementation Plan

**Overview:**
Support weapon that deals Blunt damage. Attacks generate "Notes". Recitals play songs to buff the team.
*Design Choice*: We will lean towards the **Rise/Wilds** style (Faster, more combat-fluid) rather than the slow World style.

## 1. Core Mechanics & Architecture

### 1.1. Song System
*   **Notes**:
    *   Left Click: Note 1 (Blue)
    *   Right Click: Note 2 (Red)
    *   Both: Note 3 (Green)
*   **Queue**: Stores last 4 notes.
*   **Recital**:
    *   **Input**: `Special Key` (R2).
    *   **Effect**: Checks Note Queue. If it matches a Song Pattern -> Triggers Buff.
    *   **Wilds Update (Echo Bubbles)**:
        *   Songs now place a **Bubble Entity** at the player's location.
        *   Players stepping into the bubble get the buff.
        *   *Advantage*: Visual clarity + encourages positioning.

### 1.2. Infernal Melody (Gauge)
*   **Gauge**: Fills on hit.
*   **Activation**: Special combo after a Recital (Magnificent Trio).
*   **Effect**: Massive Attack Buff for short duration.

### 1.3. Wilds Update (Focus & Clash)
*   **Echo Bubbles**: As described above.
*   **Offset Recital (Clash)**:
    *   **Input**: Recital timed against attack.
    *   **Effect**: "Super Armor" performance. You take reduced damage and play the song instantly without interruption.
*   **Focus Strike**:
    *   **Move**: **Sonic Blast**.
    *   **Effect**: Point-blank sound wave into a Wound.

---

## 2. Integration Strategy

### 2.1. Song Engine (`SongEvaluator`)
*   **Data Driven**: Songs are defined in JSON.
    ```json
    "attack_up": { "notes": ["red", "red"], "effect": "strength", "radius": 10 }
    ```
*   **Buffer**: Needs a HUD element showing the staff and notes.

### 2.2. Better Combat
*   **Swing Angles**: HH has weird swing angles (backslam).
    *   We must ensure the hitbox covers the player's back for the famous "Backslam" move.

### 2.3. Audio
*   **Dynamic Music**:
    *   Each attack plays a stem.
    *   Recital plays a full measure.
    *   *Tech*: Use standard SoundEvents but pitch-shifted or varying samples.

---

## 3. Data Config (`data/mhweapons/hunting_horn/`)

```json
// songs.json
{
  "songs": [
    { "id": "self_improvement", "pattern": ["1", "1"], "buff": "speed_ii" },
    { "id": "attack_up_l", "pattern": ["1", "2", "3", "1"], "buff": "strength_ii" }
  ]
}
```

## 4. Class Structure

1.  **`NoteQueueCapability`**:
    *   `List<Note> queue`
2.  **`SongManager`**:
    *   Checks queue against registry.
    *   Spawns `EchoBubbleEntity`.
3.  **`EchoBubbleEntity`**:
    *   Stationary, intangible.
    *   `onCollide`: Apply potion effect.

## 5. Development Tasks
- [ ] **Note HUD**: Drawing the staff on screen.
- [ ] **Song Pattern Matcher**: Logic to find valid songs.
- [ ] **Echo Bubbles**: Entity implementation.
