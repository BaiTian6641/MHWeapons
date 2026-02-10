# Bowgun (BG) 🔫 — Technical Implementation Plan

**Overview:**
A unified ranged weapon system that merges the Light Bowgun and Heavy Bowgun into a single highly customizable platform. Players craft a **Bowgun Base** and apply **Modifications** at a Weapon Workbench to slide between lightweight (mobility/evasion) and heavyweight (damage/guard/siege) configurations. This eliminates the balance gap between LBG and HBG in MHWilds while giving players unprecedented freedom in ranged playstyle.

> **Design Philosophy:** Instead of two separate weapons where one always feels inferior, the Bowgun system treats the frame as a spectrum. A fully lightened Bowgun behaves like a classic LBG (rapid-fire, burst step, dodge rolls). A fully heavied Bowgun behaves like a classic HBG (auto-guard, ignition special ammo, siege fire). A balanced build offers a hybrid playstyle with trade-offs in every axis.

---

## 1. Core Mechanics & Architecture

### 1.1. Bowgun Weight System

The central concept: every Bowgun has a **Weight Value** (0–100) determined by the sum of its installed modifications.

| Weight Range | Class Label | Movement Speed | Dodge Type | Guard | Special Mode |
|---|---|---|---|---|---|
| 0–30 | **Light** | 1.3× (sprint capable) | Burst Step / Sidestep Reload | ✗ | Rapid Fire Mode |
| 31–60 | **Medium** | 1.0× | Standard Roll | Partial (chip dmg) | Hybrid Mode |
| 61–100 | **Heavy** | 0.7× | Slow Roll / Backstep | Auto-Guard | Ignition Mode |

*   **Continuous, not discrete**: Weight affects movement speed, dodge distance, recoil recovery, reload speed, and ADS sway on a curve, not hard thresholds. The class labels are cosmetic indicators.
*   **Stored in NBT**: `mh_bowgun_weight` computed on workbench apply; cached until mods change.

### 1.2. Firing Modes

Every Bowgun has two modes toggled with `Switch Mode` (V):

#### Standard Mode (All weights)
- Basic fire, aim, reload, ammo cycling.
- Energy gauge passively regenerates.
- Standard recoil based on ammo + weight.

#### Special Mode (Weight-dependent)

| Weight Class | Special Mode Name | Gauge Name | Mechanic |
|---|---|---|---|
| Light (0–30) | **Rapid Fire Mode** | Rapid Fire Gauge | Fire multiple rounds per trigger pull. Gauge consumed per burst, regens in Standard Mode + on hit. |
| Medium (31–60) | **Versatile Mode** | Versatile Gauge | Choose between a 2-round rapid burst OR a single empowered shot per trigger. Gauge regens moderately. |
| Heavy (61–100) | **Ignition Mode** | Ignition Gauge | Fire special ammo (Wyvernheart, Wyvernpiercer, Wyverncounter, Wyvernblast). Gauge consumed, regens in Standard + on hit. |

### 1.3. Ammo System

Ammo is carried as inventory items (stackable). The Bowgun's modification loadout determines which ammo types it **can load** and the magazine capacity for each.

#### Ammo Families

| Ammo Type | Levels | Behavior | Primary Stat |
|---|---|---|---|
| **Normal** | 1, 2, 3 | Single impact projectile | Raw damage |
| **Pierce** | 1, 2, 3 | Passes through target, hits multiple times | Multi-hit raw |
| **Spread** | 1, 2, 3 | Fires pellet cone (3/5/7 pellets) | Close-range burst |
| **Sticky** | 1, 2, 3 | Adheres to target, explodes after delay | Fixed + stun KO |
| **Cluster** | 1, 2, 3 | Arc trajectory, explodes into bomblets on impact | AoE fixed |
| **Elemental** | Fire, Water, Thunder, Ice, Dragon | Single shot, element damage | Element damage |
| **Status** | Poison, Paralysis, Sleep, Blast, Exhaust | Single shot, applies status buildup | Status application |
| **Recovery** | 1, 2 | Heals target on hit (team support) | Heal value |
| **Armor** | — | Grants defense buff to target | Buff duration |
| **Demon** | — | Grants attack buff to target | Buff duration |
| **Tranq** | — | Applies tranq to target (for capture) | Capture threshold |
| **Wyvern** | — | High-damage single shot (long reload) | Massive raw |

#### Ammo Properties (per type, per level)

```
baseDamage        — Raw damage per hit
elementDamage     — Element damage per hit (elemental ammo)
statusValue       — Status buildup per hit (status ammo)
speed             — Projectile travel speed (blocks/tick)
gravity           — Projectile drop rate
pierceCount       — Number of hitbox passes (pierce ammo)
pelletCount       — Number of pellets (spread ammo)
spreadAngle       — Cone angle for pellet spread (degrees)
recoil            — Recoil severity (1–5, affects recovery frames)
reloadSpeed       — Reload time modifier (fast/normal/slow/very slow)
magazineCapacity  — Rounds per magazine (modified by bowgun mods)
rapidFireBurst    — Rounds fired per rapid-fire trigger (0 = not rapid-fire capable)
rapidFireDmgMult  — Damage multiplier per rapid-fire round (e.g., 0.6×)
```

### 1.4. Magazine & Reload

*   **Per-ammo magazines**: Each ammo type has its own magazine counter. Switching ammo does NOT discard loaded rounds.
*   **Reload (R)**: Reloads current ammo type. Hold R to reload all ammo types sequentially.
*   **Quick Reload**: Available mid-combo for Light builds (Burst Step Reload / Sidestep Reload).
*   **Reload Speed**: Base reload time × ammo reload modifier × weight modifier. Lighter = faster reloads.
*   **Reload Cancel**: Player can dodge-cancel a reload at the cost of not completing it.

### 1.5. Recoil System

Recoil determines recovery time after each shot:

| Recoil Level | Recovery Ticks | Can Walk? | Can Chain? |
|---|---|---|---|
| 1 (Very Low) | 2 | Yes | Immediate |
| 2 (Low) | 4 | Yes | Fast |
| 3 (Medium) | 8 | Slow | Moderate |
| 4 (High) | 12 | No | Slow |
| 5 (Very High) | 16 | No | Very Slow |

**Effective Recoil** = `ammo.recoil + bowgunWeight/25 - recoilMods`. Clamped 1–5.

### 1.6. Guard (Medium–Heavy Only)

*   **Auto-Guard** (Heavy, weight ≥ 70): Automatically blocks frontal attacks when idle or between shots. Cannot Perfect Guard.
*   **Manual Guard** (Medium, weight 40–69): Press Guard key. Can Perfect Guard for a counter-backstep.
*   **Shield Strength**: Determined by installed Shield Mod level. Higher = less chip damage, more knockback resist.
*   **No Guard** (Light, weight < 40): Cannot guard. Relies on mobility and Burst Step i-frames.

---

## 2. Key Moveset & Actions

### 2.1. Standard Mode Actions

#### Fire
*   **Input**: `Left Click` (Attack / R2)
*   **Details**: Fires loaded ammo straight ahead (hip-fire) or in aimed direction (if ADS active). Consumes 1 round from current ammo magazine. Applies recoil recovery.
*   **Motion Value**: Determined by ammo type (see Ammo Properties).
*   **Implementation**: `BowgunHandler.handleFire()` → validate magazine > 0 → spawn `AmmoProjectileEntity` server-side → decrement magazine → apply recoil state → send VFX packet (muzzle flash, tracer).

#### Aim / ADS
*   **Input**: `Right Click` (Use / L2) Hold
*   **Details**: Enters scoped/aimed view. Reduces spread, increases accuracy. Movement speed reduced by 40%. Enables precision targeting of hitzones.
*   **While Aiming**:
    *   `Left Click` → **Aimed Fire** (tighter spread, +10% damage)
    *   `R1 / Focus Key` → **Focus Blast** (see Special Actions)
*   **Implementation**: Sets `PlayerWeaponState.bowgunAiming = true`. Client renders crosshair overlay. Server applies movement debuff.

#### Chaser Shot (Light/Medium Only)
*   **Input**: `V` (Weapon Action) after firing
*   **Details**: Quick follow-up Power Shot that deals 1.3× ammo damage and restores more Special Gauge energy. Cannot chain after Focus Blast or Special Ammo.
*   **Motion Value**: `ammo.baseDamage × 1.3`
*   **Implementation**: Check `lastAction == FIRE && recoilTimer < 4` → spawn enhanced projectile → bonus gauge fill.

#### Reload
*   **Input**: `R` (Reload / Square)
*   **Details**: Reloads current ammo type from inventory. Hold to reload all types sequentially. Reload time = `baseReloadTicks × ammo.reloadSpeed × weightModifier`.
*   **Reload Cancel**: Dodge during reload → cancel (no ammo loaded). Light builds can Sidestep Reload instead.
*   **Implementation**: `BowgunHandler.handleReload()` → enter reload animation state → on complete, transfer ammo from inventory to magazine.

#### Select Ammo
*   **Input**: `TAB` (Hold) + `Scroll` or number keys
*   **Details**: Opens ammo radial selector. Shows all compatible ammo types, current magazine count, and inventory count. Switching ammo is instant (no animation).
*   **Implementation**: Client-side GUI overlay. Sends `AmmoSwitchPacket(ammoTypeId)` to server. Server validates ammo is compatible with current bowgun loadout.

#### Switch Mode
*   **Input**: `V` (Weapon Action) when not in post-fire window
*   **Details**: Toggle between Standard Mode and Special Mode. Visual/audio feedback on switch. Brief transition animation (3–5 ticks).
*   **Implementation**: `PlayerWeaponState.bowgunMode` toggles. Client plays switch animation. Server validates and syncs.

### 2.2. Rapid Fire Mode Actions (Light builds, weight 0–30)

#### Rapid Fire
*   **Input**: `Left Click` (Attack) while in Rapid Fire Mode
*   **Details**: Fires a burst of rounds (count determined by `ammo.rapidFireBurst`, typically 3–5). Each round deals `ammo.baseDamage × ammo.rapidFireDmgMult`. Consumes Rapid Fire Gauge per burst. Only certain ammo types support rapid fire.
*   **Rapid-capable ammo**: Normal 1/2, Pierce 1, Elemental (varies by bowgun), Spread 1.
*   **Implementation**: Server loops `rapidFireBurst` times with `rapidFireIntervalTicks` delay between each → spawn projectile per round → drain gauge.

#### Rapid Chaser Shot
*   **Input**: `V` (Weapon Action) after Rapid Fire
*   **Details**: Quick-fire burst ending with an empowered final shot. 4-round burst where last round deals 2× damage.
*   **Implementation**: Chain check → rapid burst with escalating damage on final round.

#### Burst Step
*   **Input**: `Direction` + `Space` (Dodge) after firing
*   **Details**: Quick directional dash with i-frames (8 ticks). Fires a shot mid-dash if ammo is loaded. If magazine empty, performs **Sidestep Reload** instead (reload during dodge).
*   **Motion Value**: `ammo.baseDamage × 0.7` (reduced for the dash-shot).
*   **Implementation**: Override dodge handler when `bowgunMode == RAPID_FIRE && lastAction == FIRE` → apply dash impulse + i-frames → if magazine > 0, spawn projectile; else reload 1 round.

### 2.3. Versatile Mode Actions (Medium builds, weight 31–60)

#### Versatile Burst
*   **Input**: `Left Click` (Attack) while in Versatile Mode
*   **Details**: Fires a controlled 2-round burst with tighter spread than rapid fire. Each round deals `ammo.baseDamage × 0.85`. Consumes moderate Versatile Gauge.
*   **Implementation**: 2-round burst with short interval. Middle-ground between rapid and single fire.

#### Empowered Shot
*   **Input**: Hold `Left Click` while in Versatile Mode
*   **Details**: Charges briefly (10 ticks), then fires a single empowered round dealing `ammo.baseDamage × 1.5`. Higher recoil. Consumes more Versatile Gauge than burst.
*   **Implementation**: Hold detection → charge timer → enhanced projectile spawn.

#### Pivot Guard
*   **Input**: `Guard Key` + `Direction` while in Versatile Mode
*   **Details**: Quick guard-step that blocks one hit and repositions. On Perfect Guard, gain +20% gauge and auto-counter with a quick shot.
*   **Implementation**: Guard frame check → on success, apply impulse + spawn counter projectile.

### 2.4. Ignition Mode Actions (Heavy builds, weight 61–100)

#### Wyvernheart Ignition
*   **Input**: `Left Click` (hold) while in Ignition Mode (Wyvernheart type equipped)
*   **Details**: Continuous rapid-fire stream. Power increases the longer you sustain hits on target. Consumes Ignition Gauge continuously. Movement locked to slow walk.
*   **Damage Scaling**: Starts at 0.5× motion value, ramps to 2.0× over 3 seconds of sustained hits.
*   **Implementation**: Tick-based fire loop while held → track `sustainedHitCounter` → scale damage → drain gauge per tick.

#### Wyvernpiercer Ignition
*   **Input**: `Left Click` while in Ignition Mode (Wyvernpiercer type equipped)
*   **Details**: Fires a slow, massive piercing round that passes through the entire target, hitting every hitzone it crosses. Damage increases per hitzone penetrated.
*   **Damage Scaling**: `baseDamage × (1.0 + 0.15 × hitzonesPenetrated)`
*   **Implementation**: Spawn special `WyvernpiercerEntity` with slow speed, multi-hitbox pierce logic, damage escalation.

#### Wyverncounter Ignition
*   **Input**: `Left Click` while in Ignition Mode (Wyverncounter type equipped)
*   **Details**: Close-range explosive counter. Press and hold Reload to charge, then fire for increased damage. Has an "offset" effect (partial knockback negation during charge).
*   **Damage Scaling**: `baseDamage × (1.0 + chargeLevel × 0.5)`, max 3 charge levels.
*   **Implementation**: Enter charge stance (super armor) → on release, AoE close-range blast → drain gauge.

#### Wyvernblast Ignition
*   **Input**: `Left Click` while in Ignition Mode (Wyvernblast type equipped)
*   **Details**: Mid-range explosive shot hitting a wide area. Good for grouped targets. Moderate gauge cost.
*   **Damage Scaling**: Fixed AoE damage, reduced per target beyond the first.
*   **Implementation**: Spawn `WyvernblastEntity` → on impact, AoE damage check in radius → diminishing returns per target.

#### Auto-Guard (Passive)
*   **Input**: Automatic (no input required)
*   **Details**: While in any idle/recovery state, automatically blocks frontal attacks. Cannot Perfect Guard. Chip damage applies. Strength based on Shield Mod level.
*   **Implementation**: `LivingAttackEvent` check → if `bowgunWeight >= 70 && !isAttacking && hasFrontalAngle` → reduce damage by shield multiplier.

### 2.5. Universal Special Actions

#### Focus Blast (All weights)
*   **Input**: `Right Click` (Hold ADS) + `R1` (Focus Key)
*   **Details**: Weapon-specific Focus Strike.
    *   **Light**: **Eagle Strike** — Precision shot effective on wounds. Hold R1 to increase potency and explosive range.
    *   **Medium**: **Hawk Barrage** — 3-round burst focused on wound, each hit increasing the next's damage.
    *   **Heavy**: **Wyvern Howl** — Piercing explosion on wound/weak point dealing massive damage. Auto-restores over time.
*   **Motion Values**: Light: 2.5×, Medium: 1.2× × 3 hits, Heavy: 4.0×.
*   **Implementation**: Focus mode check → wound detection on target → spawn specialized Focus projectile → apply bonus damage on wound hit.

#### Special Ammo (Wyvernblast / Adhesive — Light/Medium Only)
*   **Input**: `V` + `R` (Weapon Action + Reload)
*   **Details**:
    *   **Wyvernblast**: Places a proximity bomb that detonates when attacked. Gradually restores over time. (Light builds)
    *   **Adhesive Ammo**: Sticks to monster. Attacking it while stuck increases explosion damage. Restores over time. (Medium builds)
*   **Implementation**: Spawn `WyvernblastMineEntity` or `AdhesiveAmmoEntity` at crosshair location → register as attackable entity → on hit threshold, explode.

#### Melee Bash
*   **Input**: `C` (Weapon Action Alt) at close range
*   **Details**: Quick weapon bash. Low damage (MV 0.3×), but applies small stun KO value. Useful for emergency repositioning.
*   **Implementation**: Standard melee swing with short range, blunt damage type.

---

## 3. Combo Flows

### 3.1. Light Build — Rapid Fire DPS Loop
```
[Standard Mode]
Fire (L.Click) → Fire → Fire → Switch Mode (V)
[Rapid Fire Mode]
Rapid Fire (L.Click) → Rapid Chaser (V) → Burst Step (Dir+Space) → Rapid Fire → ...
↳ If magazine empty during Burst Step → Sidestep Reload (auto)
↳ Gauge depleted → auto-switch to Standard Mode
```

### 3.2. Light Build — Focus Strike Combo
```
[Standard Mode]
Aim (R.Click hold) → Fire → Fire → Chaser Shot (V) →
Aim (R.Click hold) + Focus (R1) → Eagle Strike → Reload (R)
```

### 3.3. Medium Build — Versatile Engage
```
[Standard Mode]
Fire (L.Click) → Chaser Shot (V) → Switch Mode (V)
[Versatile Mode]
Versatile Burst (L.Click) → Empowered Shot (Hold L.Click) →
Pivot Guard (Guard+Dir) → [Perfect Guard] → Counter Shot →
Switch back (V) → Reload (R)
```

### 3.4. Heavy Build — Ignition Siege
```
[Standard Mode]
Fire (L.Click) → Fire → Fire → Switch Mode (V)
[Ignition Mode — Wyvernheart]
Wyvernheart (Hold L.Click) → sustain until gauge depleted →
Auto-switch Standard → Reload (R) → repeat
[Ignition Mode — Wyvernpiercer]
Aim (R.Click) → Wyvernpiercer (L.Click) → high recoil recovery →
Switch Standard (V) → Reload (R)
```

### 3.5. Heavy Build — Defensive Play
```
[Standard Mode]
Fire (L.Click) → [Monster attacks] → Auto-Guard (passive) →
Guard (V+R) → [Perfect Guard] → Backstep → Aim → Fire →
Focus Blast: Wyvern Howl (ADS + R1)
```

### 3.6. Combo Summary Table

| Input Sequence | Weight Class | Resulting Action | Notes |
|---|---|---|---|
| `L.Click` | All | Fire | Standard shot |
| `L.Click` (hold, ADS) | All | Aimed Fire | +10% dmg, tight spread |
| `R.Click` (hold) | All | Aim / ADS | Scope view |
| `R.Click` + `R1` | All | Focus Blast | Weight-variant |
| `V` after fire | Light/Med | Chaser Shot | 1.3× follow-up |
| `V` idle | All | Switch Mode | Toggle Special |
| `R` | All | Reload | Hold = reload all |
| `Dir` + `Space` after fire | Light | Burst Step | i-frames + shoot |
| `L.Click` in Rapid Mode | Light | Rapid Fire | Multi-shot burst |
| `V` after Rapid Fire | Light | Rapid Chaser | Burst + empowered final |
| `L.Click` in Versatile | Medium | Versatile Burst | 2-round controlled |
| Hold `L.Click` in Versatile | Medium | Empowered Shot | 1.5× charged |
| `Guard` + `Dir` in Versatile | Medium | Pivot Guard | Guard-step |
| `L.Click` (hold) in Ignition | Heavy | Wyvernheart | Sustained DPS ramp |
| `L.Click` in Ignition | Heavy | Wyvernpiercer/Blast | Type-dependent |
| `V` + `R` | Light/Med | Special Ammo | Mine / Adhesive |
| `C` | All | Melee Bash | Emergency KO |
| `TAB` + Scroll | All | Select Ammo | Radial menu |

---

## 4. Modification System

### 4.1. Mod Slot Architecture

Every Bowgun Base has a fixed number of **Mod Slots** (3–5 depending on rarity). Mods are installed at a Weapon Workbench and determine the weapon's weight, capabilities, and ammo compatibility.

#### Mod Categories

| Category | Effect | Weight Impact | Examples |
|---|---|---|---|
| **Frame** | Core weight class shift | ±10–30 | Light Frame (-20 wt), Heavy Frame (+25 wt), Balanced Frame (+5 wt) |
| **Barrel** | Range, spread, recoil | ±2–8 | Long Barrel (+5 wt, -1 recoil, +range), Short Barrel (-3 wt, +spread) |
| **Stock** | Stability, ADS sway, reload | ±2–5 | Stabilizer Stock (+4 wt, -ADS sway), Quick Stock (-2 wt, +reload speed) |
| **Magazine** | Capacity, reload style | ±1–5 | Extended Mag (+3 wt, +capacity), Speed Loader (-1 wt, +reload speed, -capacity) |
| **Shield** | Guard capability | +8–20 | Shield Mod I/II/III (+8/14/20 wt, enables guard) |
| **Special** | Unique mechanics | ±3–10 | Rapid-Fire Enabler (-5 wt, enables rapid for more ammo types), Scope (+2 wt, +ADS zoom + damage at range), Wyvernheart Core (+10 wt, sets Ignition special to Wyvernheart) |
| **Ammo Expansion** | Unlocks ammo types | ±0–3 | Elemental Barrel (+2 wt, enables elemental ammo), Status Loader (+1 wt, enables status ammo) |

### 4.2. Mod Items

Mods are craftable items. Each Mod Item stores:

```json
{
  "modId": "heavy_frame",
  "category": "frame",
  "displayName": "Heavy Frame Mod",
  "weight": 25,
  "effects": {
    "recoilModifier": -1,
    "reloadModifier": 0.1,
    "damageMultiplier": 1.1,
    "ammoUnlocks": [],
    "capacityBonus": { "normal": 2, "pierce": 1 },
    "specialModeOverride": null,
    "guardEnabled": false,
    "rapidFireAmmoAdd": [],
    "adsSwayReduction": 0.0
  },
  "description": "Reinforced frame that increases firepower at the cost of mobility.",
  "rarity": 3,
  "craftMaterials": [
    { "item": "minecraft:iron_block", "count": 3 },
    { "item": "mhweaponsmod:monster_bone_l", "count": 2 }
  ]
}
```

### 4.3. Preset Configurations (Defaults)

To ease onboarding, the workbench offers preset loadouts:

| Preset | Mods | Weight | Play Style |
|---|---|---|---|
| **Scout (LBG-like)** | Light Frame, Short Barrel, Quick Stock, Speed Loader | 18 | High mobility, rapid fire, no guard |
| **Ranger (Balanced)** | Balanced Frame, Long Barrel, Stabilizer Stock, Extended Mag | 48 | Medium mobility, versatile mode, partial guard |
| **Juggernaut (HBG-like)** | Heavy Frame, Long Barrel, Shield Mod II, Wyvernheart Core, Extended Mag | 82 | Low mobility, ignition mode, auto-guard |
| **Sniper** | Balanced Frame, Long Barrel, Scope, Stabilizer Stock | 42 | Long range, empowered shots, precision |
| **Bomber** | Heavy Frame, Short Barrel, Extended Mag, Rapid-Fire Enabler | 55 | Sticky/Cluster spam, AoE focus |

---

## 5. GUI Design

### 5.1. Bowgun Workbench GUI

```
┌─────────────────────────────────────────────────────────────────┐
│  ⚙ BOWGUN WORKBENCH                                    [X]     │
│                                                                 │
│  ┌─────────────┐   ┌───────────────────────────────────────┐   │
│  │             │   │  MOD SLOTS                            │   │
│  │   [Bowgun   │   │  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐│   │
│  │    Model    │   │  │Frame │ │Barrel│ │Stock │ │Mag   ││   │
│  │   Preview]  │   │  │ [◆]  │ │ [◆]  │ │ [◆]  │ │ [◆]  ││   │
│  │             │   │  └──────┘ └──────┘ └──────┘ └──────┘│   │
│  │             │   │         ┌──────┐ ┌──────┐            │   │
│  │             │   │         │Shld  │ │Spcl  │            │   │
│  │             │   │         │ [  ]  │ │ [  ]  │            │   │
│  │             │   │         └──────┘ └──────┘            │   │
│  └─────────────┘   └───────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  STATS PREVIEW                                          │   │
│  │  Weight: ████████░░░░░░░░░░░░  38/100  [MEDIUM]        │   │
│  │  Recoil:     ★★☆☆☆  (Low)                              │   │
│  │  Reload:     ★★★☆☆  (Normal)                           │   │
│  │  Stability:  ★★★★☆  (High)                             │   │
│  │  Guard:      ★★☆☆☆  (Partial — chip dmg)               │   │
│  │  Mode:       Versatile Mode                             │   │
│  │  Ammo:       Normal 1-3 | Pierce 1-2 | Spread 1        │   │
│  │              Sticky 1 | Elemental (Fire/Water)          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌───────────────────────┐  ┌──────────────────────────────┐   │
│  │  PRESETS              │  │  MOD INVENTORY               │   │
│  │  ○ Scout (Light)      │  │  ┌────┐┌────┐┌────┐┌────┐  │   │
│  │  ○ Ranger (Balanced)  │  │  │Mod ││Mod ││Mod ││Mod │  │   │
│  │  ○ Juggernaut (Heavy) │  │  │ A  ││ B  ││ C  ││ D  │  │   │
│  │  ○ Sniper             │  │  └────┘└────┘└────┘└────┘  │   │
│  │  ○ Bomber             │  │  ┌────┐┌────┐┌────┐┌────┐  │   │
│  │  [APPLY PRESET]       │  │  │Mod ││Mod ││Mod ││    │  │   │
│  └───────────────────────┘  │  │ E  ││ F  ││ G  ││    │  │   │
│                              │  └────┘└────┘└────┘└────┘  │   │
│  [APPLY & SAVE]              └──────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

**Interactions:**
- Drag mods from inventory into mod slots (category-locked).
- Stats preview updates live as mods are placed/removed.
- Weight bar animates and class label changes dynamically.
- Ammo compatibility list updates in real-time.
- Preset buttons auto-fill slots (if player has the required mods).

### 5.2. In-Game HUD Overlay

```
┌─────────────────────────────────────┐
│  🔫 Bowgun [MEDIUM — Versatile]     │
│  ─────────────────────────────────  │
│  Ammo: Normal Lv2      ■■■■■□□  5/7│
│  Inv:  ×43                          │
│  ─────────────────────────────────  │
│  Versatile Gauge:                   │
│  ████████████░░░░░░  67%            │
│  ─────────────────────────────────  │
│  [V] Switch Mode  [R] Reload        │
│  [TAB] Ammo Select                  │
└─────────────────────────────────────┘
```

**HUD Elements:**
*   **Weapon Label**: Shows current weight class and active mode.
*   **Ammo Display**: Current ammo type + level, magazine bar (filled/empty segments), inventory count.
*   **Gauge Bar**: Color-coded by weight class:
    *   Light (Rapid Fire): Cyan/blue, fast fill animation.
    *   Medium (Versatile): Yellow/amber, moderate fill.
    *   Heavy (Ignition): Red/orange, slow fill with intensity glow.
*   **Keybind Hints**: Context-sensitive. Show available actions based on current state.
*   **Crosshair**: Dynamic spread indicator. Tightens during ADS. Shows effective range ring.

### 5.3. Ammo Selection Radial GUI

```
          ┌──────────┐
     ┌────│ Pierce 2 │────┐
     │    │  ■■■□ 3/4│    │
     │    └──────────┘    │
┌────┴────┐          ┌────┴────┐
│Normal 3 │          │Spread 1 │
│ ■■■■ 4/4│          │ ■■□□ 2/4│
└────┬────┘          └────┬────┘
     │    ┌──────────┐    │
     │    │ Sticky 1 │    │
     └────│  ■□□ 1/3 │────┘
          └──────────┘
        ┌──────────────┐
        │  Fire Ammo   │
        │  ■■■■■ 5/5   │
        └──────────────┘
```

**Interactions:**
- Hold TAB to open radial.
- Mouse direction or scroll to highlight ammo type.
- Release TAB to select.
- Greyed out = incompatible with current bowgun loadout.
- Red outline = out of ammo in inventory.
- Magazine bars show current loaded / max capacity.

---

## 6. Projectile System

### 6.1. `AmmoProjectileEntity`

Base entity for all bowgun projectiles. Extends forge `Projectile`.

**Properties:**
*   `ammoType` — enum identifier
*   `baseDamage`, `elementDamage`, `statusValue` — from ammo data
*   `pierceRemaining` — remaining hitbox passes for pierce ammo
*   `speed`, `gravity` — trajectory parameters
*   `owner` — shooting player reference
*   `ticksAlive` — lifetime counter (despawn after `maxLifetimeTicks`)

**Behavior:**
*   **Normal**: Single hit → apply damage → remove.
*   **Pierce**: Hit → apply damage → reduce `pierceRemaining` → continue if > 0.
*   **Spread**: Parent spawns N child projectiles in cone pattern, each is a Normal with reduced damage.
*   **Sticky**: Hit → attach to entity → start fuse timer → explode after delay.
*   **Cluster**: Arc trajectory → on hit ground/entity, spawn N bomblet sub-entities in radius.
*   **Elemental/Status**: Normal behavior + apply element/status through BC damage pipeline.
*   **Wyvern**: Very slow, very high damage, massive recoil, single-shot then long reload.

### 6.2. Special Projectile Entities

| Entity Class | Used By | Behavior |
|---|---|---|
| `WyvernheartProjectileEntity` | Ignition Wyvernheart | Rapid stream, damage ramps with sustained hits |
| `WyvernpiercerProjectileEntity` | Ignition Wyvernpiercer | Slow pierce-all, damage escalates per hitzone |
| `WyverncounterProjectileEntity` | Ignition Wyverncounter | Close-range AoE blast, chargeable |
| `WyvernblastProjectileEntity` | Ignition Wyvernblast | Mid-range wide AoE explosion |
| `WyvernblastMineEntity` | LBG Special Ammo | Proximity mine, detonates when attacked |
| `AdhesiveAmmoEntity` | Medium Special Ammo | Sticks to monster, grows damage when hit |
| `FocusBlastProjectileEntity` | Focus Blast variants | Enhanced projectile, bonus on wound hit |

### 6.3. Hit Detection & Damage

*   All projectiles use **server-authoritative** hit detection.
*   Client spawns local VFX tracer on fire input (prediction).
*   Server spawns authoritative `AmmoProjectileEntity`, computes trajectory, detects collision.
*   Damage routes through `CombatReferee` → hitzone lookup → damage type `MHDamageType.SHOT` → armor/resistance calculation → status application.
*   **Pellet spread**: Server generates pellet angles using seeded random (seed = `gameTime + playerUUID hash`) for consistency.

---

## 7. Data & Config

### 7.1. Weapon Data File

`data/mhweaponsmod/weapons/bowgun.json`:

```json
{
  "id": "bowgun",
  "category": "bowgun",
  "comboWindowTicks": 8,
  "comboWindowBufferTicks": 6,
  "chargeMaxTicks": 0,
  "baseDamage": 5.0,
  "damageType": "SHOT",
  "motionValues": {
    "fire_normal1": 1.0,
    "fire_normal2": 1.2,
    "fire_normal3": 1.5,
    "fire_pierce1": 0.5,
    "fire_pierce2": 0.6,
    "fire_pierce3": 0.7,
    "fire_spread1": 0.4,
    "fire_spread2": 0.35,
    "fire_spread3": 0.3,
    "fire_sticky1": 0.8,
    "fire_sticky2": 1.0,
    "fire_sticky3": 1.2,
    "fire_cluster1": 0.6,
    "fire_cluster2": 0.7,
    "fire_cluster3": 0.8,
    "fire_elemental": 0.8,
    "fire_status": 0.5,
    "fire_wyvern": 4.5,
    "chaser_shot": 1.3,
    "rapid_chaser_final": 2.0,
    "empowered_shot": 1.5,
    "melee_bash": 0.3,
    "focus_eagle_strike": 2.5,
    "focus_hawk_barrage_per_hit": 1.2,
    "focus_wyvern_howl": 4.0,
    "wyvernheart_min": 0.5,
    "wyvernheart_max": 2.0,
    "wyvernpiercer_base": 1.8,
    "wyvernpiercer_escalation": 0.15,
    "wyverncounter_base": 2.0,
    "wyverncounter_charge_bonus": 0.5,
    "wyvernblast_ignition": 2.5,
    "burst_step_shot": 0.7,
    "versatile_burst": 0.85,
    "adhesive_explosion": 1.8,
    "wyvernblast_mine": 2.0
  },
  "animationSeries": {
    "fire": ["fire_hip", "fire_ads"],
    "reload": ["reload_single", "reload_full"],
    "special": ["rapid_fire_loop", "ignition_fire", "versatile_burst"]
  },
  "animationTiming": {
    "fire_hip": { "hitFrame": 2, "totalFrames": 8 },
    "fire_ads": { "hitFrame": 2, "totalFrames": 6 },
    "reload_single": { "totalFrames": 20 },
    "reload_full": { "totalFrames": 40 },
    "mode_switch": { "totalFrames": 5 },
    "burst_step": { "iframeStart": 1, "iframeEnd": 8, "totalFrames": 12 },
    "melee_bash": { "hitFrame": 4, "totalFrames": 10 }
  }
}
```

### 7.2. Ammo Data File

`data/mhweaponsmod/bowgun/ammo_types.json`:

```json
{
  "ammoTypes": {
    "normal_1": {
      "displayName": "Normal Ammo Lv1",
      "baseDamage": 4.0, "speed": 3.0, "gravity": 0.01,
      "pierceCount": 0, "pelletCount": 1, "spreadAngle": 0.0,
      "recoil": 1, "reloadSpeed": "fast",
      "magazineCapacity": 8, "maxStack": 99,
      "rapidFireBurst": 3, "rapidFireDmgMult": 0.6,
      "craftRecipe": { "item": "minecraft:flint", "count": 1, "yields": 3 }
    },
    "normal_2": {
      "displayName": "Normal Ammo Lv2",
      "baseDamage": 7.0, "speed": 3.2, "gravity": 0.008,
      "pierceCount": 0, "pelletCount": 1, "spreadAngle": 0.0,
      "recoil": 2, "reloadSpeed": "normal",
      "magazineCapacity": 6, "maxStack": 99,
      "rapidFireBurst": 3, "rapidFireDmgMult": 0.55,
      "craftRecipe": { "item": "minecraft:iron_nugget", "count": 2, "yields": 2 }
    },
    "normal_3": {
      "displayName": "Normal Ammo Lv3",
      "baseDamage": 10.0, "speed": 3.5, "gravity": 0.005,
      "pierceCount": 0, "pelletCount": 1, "spreadAngle": 0.0,
      "recoil": 3, "reloadSpeed": "normal",
      "magazineCapacity": 4, "maxStack": 60,
      "rapidFireBurst": 0, "rapidFireDmgMult": 0.0
    },
    "pierce_1": {
      "displayName": "Pierce Ammo Lv1",
      "baseDamage": 3.0, "speed": 3.5, "gravity": 0.005,
      "pierceCount": 3, "pelletCount": 1, "spreadAngle": 0.0,
      "recoil": 2, "reloadSpeed": "normal",
      "magazineCapacity": 5, "maxStack": 60,
      "rapidFireBurst": 3, "rapidFireDmgMult": 0.5
    },
    "pierce_2": {
      "displayName": "Pierce Ammo Lv2",
      "baseDamage": 4.5, "speed": 3.8, "gravity": 0.003,
      "pierceCount": 4, "pelletCount": 1, "spreadAngle": 0.0,
      "recoil": 3, "reloadSpeed": "slow",
      "magazineCapacity": 4, "maxStack": 60,
      "rapidFireBurst": 0, "rapidFireDmgMult": 0.0
    },
    "pierce_3": {
      "displayName": "Pierce Ammo Lv3",
      "baseDamage": 6.0, "speed": 4.0, "gravity": 0.002,
      "pierceCount": 5, "pelletCount": 1, "spreadAngle": 0.0,
      "recoil": 4, "reloadSpeed": "slow",
      "magazineCapacity": 3, "maxStack": 40,
      "rapidFireBurst": 0, "rapidFireDmgMult": 0.0
    },
    "spread_1": {
      "displayName": "Spread Ammo Lv1",
      "baseDamage": 2.5, "speed": 2.5, "gravity": 0.02,
      "pierceCount": 0, "pelletCount": 3, "spreadAngle": 15.0,
      "recoil": 2, "reloadSpeed": "fast",
      "magazineCapacity": 6, "maxStack": 60,
      "rapidFireBurst": 2, "rapidFireDmgMult": 0.65
    },
    "spread_2": {
      "displayName": "Spread Ammo Lv2",
      "baseDamage": 3.0, "speed": 2.5, "gravity": 0.02,
      "pierceCount": 0, "pelletCount": 5, "spreadAngle": 18.0,
      "recoil": 3, "reloadSpeed": "normal",
      "magazineCapacity": 4, "maxStack": 60,
      "rapidFireBurst": 0, "rapidFireDmgMult": 0.0
    },
    "spread_3": {
      "displayName": "Spread Ammo Lv3",
      "baseDamage": 3.5, "speed": 2.5, "gravity": 0.02,
      "pierceCount": 0, "pelletCount": 7, "spreadAngle": 22.0,
      "recoil": 4, "reloadSpeed": "slow",
      "magazineCapacity": 3, "maxStack": 40,
      "rapidFireBurst": 0, "rapidFireDmgMult": 0.0
    },
    "sticky_1": {
      "displayName": "Sticky Ammo Lv1",
      "baseDamage": 6.0, "speed": 2.8, "gravity": 0.015,
      "pierceCount": 0, "pelletCount": 1, "spreadAngle": 0.0,
      "recoil": 3, "reloadSpeed": "slow",
      "magazineCapacity": 3, "maxStack": 30,
      "fuseTicks": 40, "stunValue": 15,
      "rapidFireBurst": 0, "rapidFireDmgMult": 0.0
    },
    "sticky_2": {
      "displayName": "Sticky Ammo Lv2",
      "baseDamage": 9.0, "speed": 2.8, "gravity": 0.015,
      "pierceCount": 0, "pelletCount": 1, "spreadAngle": 0.0,
      "recoil": 4, "reloadSpeed": "slow",
      "magazineCapacity": 2, "maxStack": 20,
      "fuseTicks": 40, "stunValue": 25,
      "rapidFireBurst": 0, "rapidFireDmgMult": 0.0
    },
    "cluster_1": {
      "displayName": "Cluster Ammo Lv1",
      "baseDamage": 4.0, "speed": 2.0, "gravity": 0.04,
      "pierceCount": 0, "pelletCount": 1, "spreadAngle": 0.0,
      "recoil": 4, "reloadSpeed": "very_slow",
      "magazineCapacity": 2, "maxStack": 10,
      "bombletCount": 3, "bombletRadius": 2.0,
      "rapidFireBurst": 0, "rapidFireDmgMult": 0.0
    },
    "fire_ammo": {
      "displayName": "Flaming Ammo",
      "baseDamage": 2.0, "elementDamage": 8.0, "element": "fire",
      "speed": 3.0, "gravity": 0.01,
      "pierceCount": 0, "pelletCount": 1, "spreadAngle": 0.0,
      "recoil": 2, "reloadSpeed": "normal",
      "magazineCapacity": 5, "maxStack": 60,
      "rapidFireBurst": 3, "rapidFireDmgMult": 0.6
    },
    "water_ammo": {
      "displayName": "Water Ammo",
      "baseDamage": 2.0, "elementDamage": 8.0, "element": "water",
      "speed": 3.0, "gravity": 0.01,
      "pierceCount": 0, "pelletCount": 1, "spreadAngle": 0.0,
      "recoil": 2, "reloadSpeed": "normal",
      "magazineCapacity": 5, "maxStack": 60,
      "rapidFireBurst": 3, "rapidFireDmgMult": 0.6
    },
    "thunder_ammo": {
      "displayName": "Thunder Ammo",
      "baseDamage": 2.0, "elementDamage": 8.0, "element": "thunder",
      "speed": 3.5, "gravity": 0.005,
      "pierceCount": 0, "pelletCount": 1, "spreadAngle": 0.0,
      "recoil": 2, "reloadSpeed": "normal",
      "magazineCapacity": 5, "maxStack": 60,
      "rapidFireBurst": 0, "rapidFireDmgMult": 0.0
    },
    "ice_ammo": {
      "displayName": "Freeze Ammo",
      "baseDamage": 2.0, "elementDamage": 8.0, "element": "ice",
      "speed": 3.0, "gravity": 0.01,
      "pierceCount": 0, "pelletCount": 1, "spreadAngle": 0.0,
      "recoil": 2, "reloadSpeed": "normal",
      "magazineCapacity": 5, "maxStack": 60,
      "rapidFireBurst": 0, "rapidFireDmgMult": 0.0
    },
    "dragon_ammo": {
      "displayName": "Dragon Ammo",
      "baseDamage": 3.0, "elementDamage": 12.0, "element": "dragon",
      "speed": 3.5, "gravity": 0.005,
      "pierceCount": 2, "pelletCount": 1, "spreadAngle": 0.0,
      "recoil": 4, "reloadSpeed": "very_slow",
      "magazineCapacity": 3, "maxStack": 20,
      "rapidFireBurst": 0, "rapidFireDmgMult": 0.0
    },
    "poison_ammo_1": {
      "displayName": "Poison Ammo Lv1",
      "baseDamage": 2.0, "statusValue": 15, "status": "poison",
      "speed": 3.0, "gravity": 0.01,
      "recoil": 2, "reloadSpeed": "normal",
      "magazineCapacity": 4, "maxStack": 30
    },
    "paralysis_ammo_1": {
      "displayName": "Paralysis Ammo Lv1",
      "baseDamage": 2.0, "statusValue": 12, "status": "paralysis",
      "speed": 3.0, "gravity": 0.01,
      "recoil": 2, "reloadSpeed": "normal",
      "magazineCapacity": 3, "maxStack": 20
    },
    "sleep_ammo_1": {
      "displayName": "Sleep Ammo Lv1",
      "baseDamage": 2.0, "statusValue": 15, "status": "sleep",
      "speed": 3.0, "gravity": 0.01,
      "recoil": 2, "reloadSpeed": "normal",
      "magazineCapacity": 3, "maxStack": 20
    },
    "exhaust_ammo_1": {
      "displayName": "Exhaust Ammo Lv1",
      "baseDamage": 3.0, "statusValue": 10, "status": "exhaust",
      "speed": 2.8, "gravity": 0.015,
      "recoil": 2, "reloadSpeed": "normal",
      "magazineCapacity": 4, "maxStack": 30
    },
    "recovery_ammo_1": {
      "displayName": "Recovery Ammo Lv1",
      "healValue": 4.0,
      "speed": 3.0, "gravity": 0.01,
      "recoil": 1, "reloadSpeed": "fast",
      "magazineCapacity": 5, "maxStack": 30
    },
    "recovery_ammo_2": {
      "displayName": "Recovery Ammo Lv2",
      "healValue": 8.0,
      "speed": 3.0, "gravity": 0.01,
      "recoil": 2, "reloadSpeed": "normal",
      "magazineCapacity": 3, "maxStack": 20
    },
    "demon_ammo": {
      "displayName": "Demon Ammo",
      "buffEffect": "attack_up", "buffDuration": 600,
      "speed": 3.0, "gravity": 0.01,
      "recoil": 1, "reloadSpeed": "fast",
      "magazineCapacity": 3, "maxStack": 10
    },
    "armor_ammo": {
      "displayName": "Armor Ammo",
      "buffEffect": "defense_up", "buffDuration": 600,
      "speed": 3.0, "gravity": 0.01,
      "recoil": 1, "reloadSpeed": "fast",
      "magazineCapacity": 3, "maxStack": 10
    },
    "tranq_ammo": {
      "displayName": "Tranq Ammo",
      "tranqValue": 50,
      "speed": 3.0, "gravity": 0.01,
      "recoil": 1, "reloadSpeed": "fast",
      "magazineCapacity": 4, "maxStack": 10
    },
    "wyvern_ammo": {
      "displayName": "Wyvern Ammo",
      "baseDamage": 25.0, "speed": 1.5, "gravity": 0.0,
      "pierceCount": 0, "pelletCount": 1, "spreadAngle": 0.0,
      "recoil": 5, "reloadSpeed": "very_slow",
      "magazineCapacity": 1, "maxStack": 5,
      "rapidFireBurst": 0, "rapidFireDmgMult": 0.0
    }
  }
}
```

### 7.3. Bowgun Modification Data

`data/mhweaponsmod/bowgun/modifications.json`:

```json
{
  "modifications": {
    "light_frame": {
      "displayName": "Light Frame",
      "category": "frame",
      "weight": -20,
      "effects": { "recoilModifier": 0, "reloadMultiplier": 0.85, "damageMultiplier": 0.95 },
      "description": "Stripped-down frame for maximum mobility."
    },
    "balanced_frame": {
      "displayName": "Balanced Frame",
      "category": "frame",
      "weight": 5,
      "effects": { "recoilModifier": 0, "reloadMultiplier": 1.0, "damageMultiplier": 1.0 },
      "description": "Standard frame with no extremes."
    },
    "heavy_frame": {
      "displayName": "Heavy Frame",
      "category": "frame",
      "weight": 25,
      "effects": { "recoilModifier": -1, "reloadMultiplier": 1.15, "damageMultiplier": 1.1 },
      "description": "Reinforced frame for maximum firepower."
    },
    "long_barrel": {
      "displayName": "Long Barrel",
      "category": "barrel",
      "weight": 5,
      "effects": { "recoilModifier": -1, "rangeMultiplier": 1.3, "spreadReduction": 0.3 },
      "description": "Extended barrel for range and accuracy."
    },
    "short_barrel": {
      "displayName": "Short Barrel",
      "category": "barrel",
      "weight": -3,
      "effects": { "recoilModifier": 0, "rangeMultiplier": 0.8, "spreadReduction": -0.15 },
      "description": "Compact barrel for quick handling."
    },
    "stabilizer_stock": {
      "displayName": "Stabilizer Stock",
      "category": "stock",
      "weight": 4,
      "effects": { "adsSwayReduction": 0.5, "recoilModifier": -1 },
      "description": "Precision stock that reduces aim sway."
    },
    "quick_stock": {
      "displayName": "Quick Stock",
      "category": "stock",
      "weight": -2,
      "effects": { "reloadMultiplier": 0.8, "adsSwayReduction": 0.1 },
      "description": "Lightweight stock for fast reloads."
    },
    "extended_magazine": {
      "displayName": "Extended Magazine",
      "category": "magazine",
      "weight": 3,
      "effects": { "capacityBonusAll": 2 },
      "description": "Larger magazine for all ammo types."
    },
    "speed_loader": {
      "displayName": "Speed Loader",
      "category": "magazine",
      "weight": -1,
      "effects": { "reloadMultiplier": 0.7, "capacityBonusAll": -1 },
      "description": "Faster reloads at the cost of magazine size."
    },
    "shield_mod_1": {
      "displayName": "Shield Mod I",
      "category": "shield",
      "weight": 8,
      "effects": { "guardEnabled": true, "guardStrength": 1, "chipDamageMultiplier": 0.5 },
      "description": "Basic shield attachment. Enables manual guard."
    },
    "shield_mod_2": {
      "displayName": "Shield Mod II",
      "category": "shield",
      "weight": 14,
      "effects": { "guardEnabled": true, "guardStrength": 2, "chipDamageMultiplier": 0.25, "autoGuardWeight": 70 },
      "description": "Reinforced shield. Enables auto-guard at high weight."
    },
    "shield_mod_3": {
      "displayName": "Shield Mod III",
      "category": "shield",
      "weight": 20,
      "effects": { "guardEnabled": true, "guardStrength": 3, "chipDamageMultiplier": 0.1, "autoGuardWeight": 60 },
      "description": "Maximum guard. Near-complete chip damage negation."
    },
    "rapid_fire_enabler": {
      "displayName": "Rapid-Fire Enabler",
      "category": "special",
      "weight": -5,
      "effects": { "rapidFireAmmoAdd": ["normal_3", "pierce_2", "spread_2"] },
      "description": "Enables rapid fire for additional ammo types."
    },
    "scope": {
      "displayName": "Scope",
      "category": "special",
      "weight": 2,
      "effects": { "adsZoomLevel": 2.0, "adsDamageBonus": 0.1, "adsSwayReduction": 0.3 },
      "description": "Optical scope for precision targeting at range."
    },
    "wyvernheart_core": {
      "displayName": "Wyvernheart Core",
      "category": "special",
      "weight": 10,
      "effects": { "ignitionType": "wyvernheart" },
      "description": "Sets Ignition Mode to Wyvernheart sustained fire."
    },
    "wyvernpiercer_core": {
      "displayName": "Wyvernpiercer Core",
      "category": "special",
      "weight": 10,
      "effects": { "ignitionType": "wyvernpiercer" },
      "description": "Sets Ignition Mode to Wyvernpiercer drilling shot."
    },
    "wyverncounter_core": {
      "displayName": "Wyverncounter Core",
      "category": "special",
      "weight": 8,
      "effects": { "ignitionType": "wyverncounter" },
      "description": "Sets Ignition Mode to Wyverncounter close-range blast."
    },
    "wyvernblast_core": {
      "displayName": "Wyvernblast Core",
      "category": "special",
      "weight": 8,
      "effects": { "ignitionType": "wyvernblast" },
      "description": "Sets Ignition Mode to Wyvernblast wide AoE."
    },
    "elemental_barrel": {
      "displayName": "Elemental Barrel",
      "category": "special",
      "weight": 2,
      "effects": { "ammoUnlocks": ["fire_ammo", "water_ammo", "thunder_ammo", "ice_ammo", "dragon_ammo"] },
      "description": "Enables loading of elemental ammo types."
    },
    "status_loader": {
      "displayName": "Status Loader",
      "category": "special",
      "weight": 1,
      "effects": { "ammoUnlocks": ["poison_ammo_1", "paralysis_ammo_1", "sleep_ammo_1", "exhaust_ammo_1"] },
      "description": "Enables loading of status ammo types."
    },
    "support_kit": {
      "displayName": "Support Kit",
      "category": "special",
      "weight": 0,
      "effects": { "ammoUnlocks": ["recovery_ammo_1", "recovery_ammo_2", "demon_ammo", "armor_ammo"] },
      "description": "Enables team support ammo types."
    },
    "silencer": {
      "displayName": "Silencer",
      "category": "barrel",
      "weight": -1,
      "effects": { "recoilModifier": -1, "aggroReduction": 0.5, "damageMultiplier": 0.95 },
      "description": "Reduces recoil and monster aggro, slight damage penalty."
    }
  }
}
```

---

## 8. GeckoLib — Models & Animations

### 8.1. Model Architecture

*   **Base Model**: `bowgun_base.geo.json` — Core bowgun shape with attachment points.
*   **Mod Overlays**: Each mod category has optional visual overlays:
    *   `barrel_long.geo.json`, `barrel_short.geo.json`
    *   `stock_stabilizer.geo.json`, `stock_quick.geo.json`
    *   `shield_mod_1/2/3.geo.json`
    *   `scope.geo.json`
*   **Dynamic Assembly**: Renderer reads NBT mod list and composites visible parts.

### 8.2. Animation List

| Animation ID | Trigger | Duration | Notes |
|---|---|---|---|
| `idle` | Default | Loop | Weapon held at hip |
| `idle_ads` | ADS active | Loop | Weapon shouldered |
| `draw` | Weapon switch in | 10f | Pull from back |
| `fire_hip` | Fire (no ADS) | 8f | Quick hip-fire jolt |
| `fire_ads` | Fire (ADS) | 6f | Tight shoulder fire |
| `reload_single` | Reload (tap) | 20f | Single ammo type reload |
| `reload_full` | Reload (hold) | 40f | All ammo sequential |
| `rapid_fire_loop` | Rapid Fire Mode fire | Loop | Fast cycling bolt/mechanism |
| `rapid_chaser` | Rapid Chaser Shot | 12f | Quick burst ending heavy |
| `burst_step` | Burst Step dodge | 12f | Dash with mid-shot |
| `sidestep_reload` | Empty Burst Step | 14f | Dash + magazine swap |
| `mode_switch` | Switch Mode | 5f | Mechanism toggle VFX |
| `ignition_enter` | Enter Ignition Mode | 8f | Power-up transformation |
| `ignition_fire` | Ignition fire | Varies | Per special ammo type |
| `wyvernheart_loop` | Wyvernheart sustained | Loop | Continuous fire vibration |
| `wyvernpiercer_charge` | Wyvernpiercer | 15f | Charge + release |
| `wyverncounter_charge` | Wyverncounter | Hold | Charge stance with offset |
| `wyvernblast_fire` | Wyvernblast | 10f | Wide AoE launch |
| `guard_block` | Guard / Auto-Guard | Hold | Shield raised |
| `guard_perfect` | Perfect Guard | 8f | Flash + backstep |
| `melee_bash` | Melee bash | 10f | Weapon butt strike |
| `focus_blast` | Focus Blast variants | 15f | Charged precision shot |
| `ammo_select` | Ammo radial open | 3f | Hand adjusts mechanism |
| `versatile_burst` | Versatile Mode burst | 10f | Controlled double-tap |
| `empowered_charge` | Empowered Shot charge | 10f | Glow buildup |
| `empowered_fire` | Empowered Shot release | 8f | Enhanced recoil |
| `pivot_guard` | Pivot Guard step | 10f | Guard + reposition |

### 8.3. VFX

*   **Muzzle Flash**: Per-ammo variants (normal = small spark, elemental = colored flash, wyvern = massive blast).
*   **Tracers**: Projectile-attached trail particles. Normal = white streak, Pierce = blue line, Spread = scattered sparks, Elemental = colored trail, Sticky = yellow blink, Cluster = arc trail.
*   **Impact VFX**: Hit sparks, elemental explosions (fire burst, water splash, thunder crackle), status clouds (poison purple, sleep blue, paralysis yellow).
*   **Gauge VFX**: Weapon glows when Special Mode gauge is full. Cyan (Light), Amber (Medium), Red (Heavy).

---

## 9. Class Structure

### 9.1. Items & Registry

| Class | Extends | Purpose |
|---|---|---|
| `BowgunItem` | `GeoWeaponItem` | Main weapon item. Stores mods in NBT. Computes weight. Implements `WeaponIdProvider` with `"bowgun"`. |
| `AmmoItem` | `Item` | Stackable ammo item. Each ammo type is a separate registered item. Stores ammo type ID. |
| `BowgunModItem` | `Item` | Mod item. Stores mod ID, category, weight, effects. Used in workbench GUI. |

### 9.2. Capabilities & State

| Class | Purpose |
|---|---|
| `BowgunState` (fields in `PlayerWeaponState`) | `bowgunMode` (STANDARD/RAPID/VERSATILE/IGNITION), `bowgunWeight`, `bowgunAiming`, `bowgunGauge`, `bowgunRecoilTimer`, `bowgunReloadTimer`, `bowgunMagazines` (Map<AmmoType, int>), `bowgunCurrentAmmo`, `bowgunInstalledMods`, `bowgunIgnitionType`, `bowgunSustainedHitCounter`, `bowgunAutoGuard` |
| `BowgunMagazineManager` | Helper class managing per-ammo magazine counts, reload logic, ammo switching, compatibility checks |
| `BowgunModResolver` | Reads installed mods from NBT, computes aggregate weight, effective recoil/reload/spread/guard stats |

### 9.3. Handler & Logic

| Class | Purpose |
|---|---|
| `BowgunHandler` | Main handler (static methods). Dispatches `ATTACK`, `USE`, `WEAPON_ACTION`, `WEAPON_ACTION_ALT`, `SPECIAL`, `RELOAD` actions. Manages mode switching, fire logic, gauge management. |
| `BowgunProjectileHandler` | Server-side projectile spawn, trajectory, hit detection, damage routing through `CombatReferee`. |
| `BowgunGuardHandler` | Guard/Auto-Guard/Perfect Guard logic for medium/heavy builds. |

### 9.4. GUI & Networking

| Class | Purpose |
|---|---|
| `BowgunWorkbenchBlock` | Block that opens workbench GUI. |
| `BowgunWorkbenchMenu` | Server-side menu (extends `AbstractContainerMenu`). Validates mod placement, computes stats. |
| `BowgunWorkbenchScreen` | Client-side GUI renderer. Mod slots, stat preview, presets, drag-and-drop. |
| `AmmoSelectOverlay` | Client-side radial ammo selector. |
| `BowgunHudRenderer` | Integrated into `WeaponHudOverlay` — gauge, magazine, mode display. |
| `BowgunModSyncPacket` | C2S: Send mod loadout changes. S2C: Sync computed stats. |
| `AmmoSwitchPacket` | C2S: Player switches ammo type. |
| `BowgunFirePacket` | C2S: Fire request with aim direction. Server validates and spawns projectile. |

### 9.5. Entities

| Class | Extends | Purpose |
|---|---|---|
| `AmmoProjectileEntity` | `Projectile` | Base ammo projectile with type-specific behavior (normal/pierce/spread/sticky/cluster). |
| `WyvernheartStreamEntity` | `AmmoProjectileEntity` | Rapid-stream projectile for Wyvernheart Ignition. |
| `WyvernpiercerEntity` | `AmmoProjectileEntity` | Slow-moving full-pierce projectile. |
| `WyverncounterBlastEntity` | `Entity` | Close-range AoE explosion entity. |
| `WyvernblastExplosionEntity` | `Entity` | Mid-range wide AoE entity. |
| `WyvernblastMineEntity` | `Entity` | Placeable mine, detonates when attacked. |
| `AdhesiveAmmoEntity` | `Entity` | Sticks to mob, accumulates damage, explodes. |

---

## 10. Implementation Roadmap

### Phase 1: Core Item & State Foundation
- [ ] **`BowgunItem`**: Create item class extending `GeoWeaponItem`. NBT storage for installed mods. Weight computation. Register with `"bowgun"` weapon ID.
- [ ] **`BowgunModItem`**: Create mod item class. Category, weight, effects stored per-item.
- [ ] **`AmmoItem`**: Create ammo item class with type ID. Register all ammo types in `MHWeaponsItems`.
- [ ] **State Fields**: Add bowgun fields to `PlayerWeaponState` (mode, weight, gauge, magazines, currentAmmo, mods, recoil/reload timers, etc.). Serialize/deserialize.
- [ ] **Data Loading**: Add `bowgun.json` weapon data. Create `BowgunDataResolver` for ammo and mod data loading.
- [ ] **`BowgunHandler`**: Skeleton handler with action dispatch. Register `case "bowgun"` in `WeaponActionHandler`.

### Phase 2: Basic Firing & Ammo
- [ ] **`AmmoProjectileEntity`**: Base projectile entity. Registration. Trajectory (speed, gravity). Single-hit damage. Despawn logic.
- [ ] **Fire Action**: `BowgunHandler.handleFire()` — magazine check → spawn projectile → decrement → recoil.
- [ ] **Aim/ADS**: Toggle `bowgunAiming`. Client crosshair. Server movement debuff. Spread reduction.
- [ ] **Reload**: Single and full reload logic. Ammo transfer from inventory to magazine. Reload timer with cancel.
- [ ] **Ammo Switch**: `AmmoSwitchPacket`. Radial GUI stub (simple list first, radial later).
- [ ] **HUD**: Basic bowgun HUD in `WeaponHudOverlay` — ammo display, magazine bar, weight class label.

### Phase 3: Projectile Variants
- [ ] **Pierce**: Multi-hitbox pass logic in `AmmoProjectileEntity`.
- [ ] **Spread**: Pellet cone spawning (N child projectiles).
- [ ] **Sticky**: Attach-to-entity + fuse timer + explode.
- [ ] **Cluster**: Arc trajectory + bomblet sub-entities on impact.
- [ ] **Elemental/Status**: Route element damage and status buildup through `CombatReferee` + `MobStatusState`.
- [ ] **Support Ammo**: Recovery (heal on hit), Demon/Armor (buff on hit), Tranq (capture check).

### Phase 4: Mode System
- [ ] **Mode Switch**: Toggle logic, animation, gauge init.
- [ ] **Rapid Fire Mode** (Light): Burst fire, gauge consumption, Rapid Chaser Shot, Burst Step with i-frames.
- [ ] **Versatile Mode** (Medium): 2-round burst, Empowered Shot (hold charge), Pivot Guard.
- [ ] **Ignition Mode** (Heavy): Wyvernheart (sustained), Wyvernpiercer (slow pierce), Wyverncounter (charged AoE), Wyvernblast (wide AoE). Gauge drain. Special entity classes.
- [ ] **Gauge Regeneration**: Standard Mode passive regen + on-hit regen.

### Phase 5: Modification Workbench
- [ ] **`BowgunWorkbenchBlock`**: Block registration, right-click opens menu.
- [ ] **`BowgunWorkbenchMenu`**: Server-side container. Mod slot validation (category lock). Stat computation. Mod read/write to `BowgunItem` NBT.
- [ ] **`BowgunWorkbenchScreen`**: Full GUI with mod slots, stat preview bars, preset buttons, drag-and-drop.
- [ ] **`BowgunModResolver`**: Compute aggregate stats from installed mods. Weight, recoil, reload, guard, ammo compatibility, special mode type.
- [ ] **Presets**: Scout, Ranger, Juggernaut, Sniper, Bomber auto-fill logic.

### Phase 6: Guard & Defense
- [ ] **Manual Guard** (Medium): Guard key → block → chip damage based on shield mod level. Perfect Guard → backstep + counter.
- [ ] **Auto-Guard** (Heavy): `LivingAttackEvent` passive check. Frontal angle detection. Chip damage application.
- [ ] **Guard VFX/SFX**: Shield raise animation, impact particles, perfect guard flash.

### Phase 7: Focus Blast & Special Ammo
- [ ] **Eagle Strike** (Light Focus): Precision wound shot.
- [ ] **Hawk Barrage** (Medium Focus): 3-round escalating burst.
- [ ] **Wyvern Howl** (Heavy Focus): Piercing explosion on wound.
- [ ] **Wyvernblast Mine** (Light Special): Placeable proximity bomb entity.
- [ ] **Adhesive Ammo** (Medium Special): Sticking accumulator entity.

### Phase 8: Animations, VFX & Polish
- [ ] **GeckoLib Models**: Base bowgun + mod overlays.
- [ ] **All Animations**: Fire, reload, mode switch, burst step, ignition, guard, focus, melee bash.
- [ ] **VFX**: Muzzle flash variants, tracers, impact effects, elemental effects, gauge glow.
- [ ] **SFX**: Fire sounds per ammo weight, reload clicks, mode switch, guard clang, gauge full chime.
- [ ] **Ammo Radial GUI**: Full radial selector with magazine bars and inventory counts.
- [ ] **HUD Polish**: Animated gauge, dynamic crosshair spread, context-sensitive hints.

### Phase 9: Networking & Security
- [ ] **Server-Authoritative Fire**: Validate ammo availability, rate limit projectile spawns, enforce cooldowns.
- [ ] **Hit Validation**: Server-side trajectory + collision check. Reject suspicious hit reports.
- [ ] **Gauge Anti-Cheat**: Server tracks gauge state; reject client gauge modifications.
- [ ] **AoE Server Resolution**: All AoE damage computed server-side (Cluster, Wyvernblast, Ignition specials).

### Phase 10: Testing & Balance
- [ ] **Unit Tests**: Magazine logic (reload/empty/switch), mod stat computation, weight thresholds, gauge drain rates.
- [ ] **Integration Tests**: Projectile hit accuracy at various ranges, pierce pass-through counts, spread distributions, sticky timing.
- [ ] **Playtest**: Weight class feel (Light vs Heavy mobility), DPS curves per build, ammo economy balance, Focus Blast reward on wound.
- [ ] **Balance Tuning**: Motion values, gauge rates, recoil recovery, magazine sizes, mod weights.

---

## 11. Networking & Security Summary

| Concern | Solution |
|---|---|
| Client-spoofed fire | Server validates magazine > 0 and recoil timer expired before spawning projectile |
| Gauge manipulation | Gauge is server-authoritative; client display is synced read-only |
| AoE damage spoofing | All AoE (Cluster, Wyvernblast, Ignition) resolved server-side |
| Rate limiting | Max projectile spawns capped per tick (e.g., 5/tick for Wyvernheart) |
| Ammo duplication | Magazine decrement and inventory removal are atomic server operations |
| Mod validation | Workbench menu validates mod category, slot count, and material costs server-side |
| Auto-guard bypass | Guard state checked server-side on `LivingAttackEvent`; cannot be force-toggled by client |

---

## 12. Cross-Mod & Integration Notes

*   **Damage Pipeline**: All projectile damage routes through `CombatReferee` → `MHDamageType.SHOT` → hitzone → armor. Compatible with existing wound, status, and decoration systems.
*   **Data-Driven Extensibility**: Other mods can add new ammo types by adding entries to `ammo_types.json` and new mods by adding to `modifications.json`. No code changes needed.
*   **Better Combat**: Bowgun does NOT use BC's melee combo system. Fire/aim/reload are custom-handled. Only melee bash uses a BC attack definition.
*   **Decoration Slots**: `BowgunItem` supports decoration jewel slots via existing `DecorationDataManager`. Decorations affect shot damage, element, affinity as usual.

---

## 13. Notes & Design Rationale

### Why merge LBG and HBG?
1. **MHWilds HBG complaints**: The community widely criticizes HBG's sluggishness and limited identity compared to LBG in Wilds. By unifying, we avoid picking a "worse" weapon.
2. **Customization depth**: The mod system gives players agency. Want a light HBG? Install lighter mods. Want a guarding LBG? Add a shield. The spectrum is continuous.
3. **Balance**: One weapon with one balance curve is easier to tune than two separate weapons competing for the same niche.
4. **Content density**: Instead of two thin weapon implementations, one rich system with deep customization is more engaging.

### Weight System Philosophy
*   Weight is NOT just a number — it affects the entire feel of the weapon through movement speed curves, dodge types, recoil recovery, reload speed, and available mechanics.
*   The three class labels (Light/Medium/Heavy) are convenience markers. The actual stats interpolate smoothly, so a "32 weight" Medium plays very differently from a "58 weight" Medium.
*   Players are encouraged to experiment. A dedicated workbench means loadout changes are deliberate (not mid-hunt), promoting build identity while allowing flexibility between hunts.
