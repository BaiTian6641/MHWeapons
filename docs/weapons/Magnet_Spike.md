# Magnet Spike (Frontier / Wilds) — Technical Implementation Plan

**Overview:**
A transforming heavy weapon (Slash/Impact) that utilizes magnetism for extreme mobility and lockdown potential. Originating from *Monster Hunter Frontier*, this implementation modernizes the moveset to align with *Monster Hunter Wilds* design philosophies, integrating Focus Mode and seamless transformation combos.

## 1. Core Mechanics & Architecture

### 1.1. Dual Mode System (Transformation)
The weapon seamlessly transforms between two distinct forms, offering versatility for any monster part.
*   **Transformation Action**: Press `Special` (Mode Switch). Can be performed mid-combo to link moves (Transform Attack).
*   **HUD**: Displays current mode (Icon changes between Blade and Hammer).

#### A. Cutting Mode (Slash)
*   **Role**: Tail cutting, fast wide sweeps, mobility.
*   **Attribute**: **SEVER** damage.
*   **Base Combo (Left Click)**:
    1.  **Magnet Slash I**: Vertical opener.
    2.  **Magnet Slash II**: Rising slash.
    3.  **Magnet Slash III**: Wide horizontal sweep.
    4.  **Magnet Cleave**: Heavy finishing downward chop.
*   **Special Action**: `Magnet Roundslash` (Area denial).

#### B. Impact Mode (Blunt)
*   **Role**: Head snipping, stunning (KO), breaking hard parts.
*   **Attribute**: **BLUNT** damage (High Stun/Exhaust).
*   **Base Combo (Left Click)**:
    1.  **Magnet Smash I**: Diagonal slam.
    2.  **Magnet Smash II**: Returning uppercut.
    3.  **Magnet Crushing Blow**: Heavy ground pound.
    4.  **Magnet Suplex**: Massive damage throw/slam (Iconic Finisher).
*   **Hold Action (LMB)**: `Impact Charge` (3 Levels). **Charge is Impact-mode only.**

### 1.2. Magnetic Field System (The Gimmick)
The core loop involves managing the **Magnet Gauge** and establishing a **Magnetic Link**.

*   **Magnetic Shot (Focus Strike)**:
    *   **Input**: Press `R` (Weapon Action) while aiming.
    *   **Context**: Acts as the *Wilds* "Focus Strike".
    *   **Effect**: Fires a magnetic shell. If it hits, applies `MagnetTag` to the monster part.
    *   **Duration**: 45 seconds (Visual: Electric tether).
*   **Magnetic Approach / Repel (RMB)**:
    *   **Input**: `RMB` (Weapon Alt).
    *   **Approach**: Default RMB zips *to* target.
    *   **Repel**: Hold **Alt (G)** while RMB to zip *away*.
    *   **Wilds Integration (Just Evade)**: Using *Magnet Repel* on the frame of an attack triggers **Magnetic Reflex**, granting extended i-frames and instant Gauge refill.
*   **Magnet Burst (Finisher)**:
    *   **Input**: `Shift + Special` (Mode Switch key) when Gauge > 50%.
    *   **Effect**: Massive release of magnetic energy.

### 1.3. Ultimate: Magnetic Pile Bunker
To modernize the "Magnetic Bind" for the Minecraft sandbox:
*   **Condition**: Target Magnetized + Gauge MAX + Target Staggered/Downed.
*   **Execution**:
    1.  **Pin**: Player *Zips* to target and locks fast.
    2.  **Drill**: Weapon transforms into a pile bunker, dealing rapid tick damage.
    3.  **Bang**: Massive fixed damage explosion.

---

## 2. Integration Strategy

### 2.1. Handler Architecture
Logic is centralized in `MagnetSpikeHandler.java`:
*   `handleChargeRelease`: Logic for `Impact Charge` levels.
*   `handleMagnetZip`: Physics vector calculation for Approach/Repel.
*   `handleSpecialAction`: Mode switching and Finisher triggers.

### 2.2. Combo naming & Progression
We follow the *Rise/Wilds* naming convention for clarity in the code (`MagnetSpikeHandler` and data files).

| Input Steps | Cutting Mode Action | Impact Mode Action |
| :--- | :--- | :--- |
| **Step 1** | `magnet_slash_i` | `magnet_smash_i` |
| **Step 2** | `magnet_slash_ii` | `magnet_smash_ii` |
| **Step 3** | `magnet_slash_iii` | `magnet_crush` |
| **Finisher** | `magnet_cleave` | `magnet_suplex` |
| **Charge** | *N/A* | `impact_charge_i/ii/iii` |
| **Approach** | `magnet_approach` | `magnet_approach` |
| **Repel** | `magnet_repel` | `magnet_repel` |

---

## 3. Data Config (`data/mhweapons/magnet_spike/`)

```json
// default.json
{
  "magnet_duration_ticks": 900,
  "zip_cooldown": 25,
  "gauge_gain_per_hit": 8.0,
  "impact_stun_value": 45,
  "suplex_motion_value": 1.5
}
```

## 4. Development Tasks & Roadmap
- [x] **Core Transformation**: Cutting vs Impact modes.
- [x] **Magnet Zip**: Basic physics implementation.
- [x] **HUD**: Timer and Gauge visualization.
- [ ] **Combo Expansion**: Implement the full I -> II -> III -> Finisher chains in `MagnetSpikeHandler`.
- [ ] **Wilds Mechanics**: Add "Focus Strike" tagging logic.
- [ ] **Pile Bunker**: Pin animation and particle effects.
- [ ] **Sound Effects**: Magnetic hums, "clank" transformation, Zap sounds.
