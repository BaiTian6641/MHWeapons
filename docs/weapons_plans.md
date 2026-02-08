# MHWeaponsMod — Weapon Implementation Plans ✅

> Purpose: A concise, actionable plan for implementing each Monster Hunter weapon type into Minecraft.
> - Combat & movement behavior: handled by **Better Combat** (BC)
> - Weapon model & visual animations: handled by **GeckoLib** (GL)

---

## Table of Contents
- Melee
  - Great Sword
  - Long Sword
  - Sword and Shield
  - Dual Blades
  - Hammer
  - Hunting Horn
  - Lance
  - Gunlance
  - Switch Axe
  - Charge Blade
  - Insect Glaive
  - Tonfa
  - Magnet Spike
  - Accel Axe
- Ranged
  - Bow
  - Light Bowgun
  - Heavy Bowgun
  - Medium Bowgun

---

## Common sections in each weapon plan
Each weapon section contains:
- **Overview**: short description
- **Core mechanics**: gameplay features from Monster Hunter that must be preserved
- **Mapping to Better Combat**: how BC actions/states will be used or extended
- **GeckoLib model & animations**: models, animation states & transitions for the weapon item/entity
- **Data & config**: damage type(s), reach, attack timings, stamina/charge/gauges
- **Sounds / VFX**: particles, sounds, screenshake, hit flashes
- **Implementation tasks**: asset & code tasks, testing checklist
- **Priority & estimate**: S/M/L

---

### Great Sword (GS) 🔨
**Overview:** Large slow blade with charge attacks (3 charge levels), block ability, high single-hit damage.

Core mechanics:
- Charge attack with three charge levels
- Block (loses sharpness/condition)
- Hitzone positioning (center/hilt vs tip) for damage bonus

Mapping to Better Combat:
- Use BC charge-action mapping for the 3 charge levels (hold primary attack → charge states)
- Implement blocking as a BC block action with a sharpness/condition penalty resource
- Stagger/knockback resistance while charging (temporary knockback immunity flag)

GeckoLib:
- Animations: idle, draw, sheath, charge_lvl1, charge_lvl2, charge_lvl3, heavy_swing, block, stagger
- Blend trees for charge hold → release
- Model: large blade with readied pose and sheath states

Data & config:
- Damage: Cutting (high)
- Reach: long
- Charge times: lvl1, lvl2, lvl3 (configurable)
- Cooldown / recovery frames after release

SFX / VFX:
- Charge glow (particles & shader tint), heavy hit impact particle and camera shake

Tasks:
1. BC: Add charge-action states & configure input mapping (hold + release) (+ unit tests)
2. Item: Create `GreatswordItem` subclass hooking to BC actions
3. GL: Model + animations, implement charge blend and release
4. Balancing: tune damage and timings in config
5. Tests: charge correctness, blocking, damage multipliers

Estimate: M
Priority: High

---

### Long Sword (LS) ⚔️
**Overview:** Agile long blade with Spirit Gauge and Spirit Combos that power up attacks.

Core mechanics:
- Spirit Gauge filling from hits
- Spirit combos unlocked at thresholds
- Long reach and evasive combos

Mapping to Better Combat:
- Use BC's combo chains and add a SpiritGauge component on the player for stacking buffs on-hit
- Create combo transitions and unlock conditions based on gauge

GeckoLib:
- Animations: idle, draw, sheath, quick_slash, spirit_combo_1..3, unsheathe_slash
- Gauge UI overlay (simple bar/particle on weapon)

Data & config:
- Damage type: Cutting
- Gauge increments per hit and decay rate
- Spirit combo unlock thresholds and durations

Tasks:
1. BC: Implement combo chain nodes & gating by SpiritGauge value
2. Gameplay: `LongswordComponent` that tracks gauge and applies temporary buff
3. GL: Animations for Spirit Combos and visual gauge effects
4. Tests: gauge behavior, combo unlocking

Estimate: M
Priority: High

---

### Sword and Shield (SnS) 🛡️
**Overview:** Fast small sword with shield for blocking and item use while drawn.

Core mechanics:
- Fast combos, shield bashes (KO potential), use items while drawn

Mapping to Better Combat:
- Map quick attack combos to BC's fast chain system
- Implement shield bash as a BC special attack with brief stun/knockback
- Allow BC item-while-drawn hook to permit item usage

GeckoLib:
- Animations: idle, draw, sheath, fast_combo, shield_bash, item_use_animation

Data & config:
- Damage: Cutting (fast low-damage)
- Shield bash cooldown & KO threshold

Tasks:
1. BC: Allow item use while weapon drawn; add shield_bash action
2. GL: SnS model + shield bash animation & item-use animation
3. Tests: item use while drawn, shield bash effects

Estimate: S
Priority: High

---

### Dual Blades (DB) 🔥
**Overview:** Two blades with Demon Mode (infinite combos, stamina drain) and very fast attacks.

Core mechanics:
- Demon Mode toggle (enter / exit), constant stamina drain while active
- Very fast combo flow, high mobility

Mapping to Better Combat:
- Implement toggleable BC state `DemonMode` that disables standard recovery and allows infinite chain transitions while active
- Hook stamina drain and exit conditions

GeckoLib:
- Animations: idle, draw, rapid_slash_loop, demon_enter, demon_loop, demon_exit, dodge

Data & config:
- Stamina drain rate, damage multipliers in DemonMode

Tasks:
1. BC: Implement toggleable state, modify combo rules when active
2. Gameplay: Stamina drain system and UI indicator
3. GL: Demon visual effect & continuous attack loop animation
4. Tests: stamina drain, forced exit behavior

Estimate: M
Priority: High

---

### Hammer 🛠️
**Overview:** Heavy impact weapon; powerful KO potential with charged Superpound.

Core mechanics:
- Superpound (charge), heavy stun on head KOs
- Fast stagger build-up on head hits

Mapping to Better Combat:
- BC charge-action for Superpound with increased impact and stun meter increment
- Add head-targeting stun checks via hitzones

GeckoLib:
- Animations: idle, draw, swing_combo, superpound_charge, superpound_release

Data & config:
- Damage type: Impact, KO thresholds, charge durations

Tasks:
1. BC: Charge & superpound special attack
2. GL: Animations and camera shake
3. Tests: KO behavior and stun thresholds

Estimate: M
Priority: High

---

### Hunting Horn (HH) 🎶
**Overview:** Impact weapon that can play notes to give buffs to nearby players.

Core mechanics:
- Play note sequences (recital) to give buffs (attack, defense, regen, etc.)
- Melee impact attacks

Mapping to Better Combat:
- BC provides heavy attack chaining. Add a `HornRecital` component to detect specific note sequences
- Recital should be interruptible and have visual/audible feedback

GeckoLib:
- Animations: idle, play_note_X (1..3), recital_sequence, slam

Data & config:
- Note mapping, built-in buffs and durations, radius and stacking rules

Tasks:
1. Gameplay: Implement note recording and pattern recognition
2. BC: Integrate slam & recital trigger
3. GL: Animations and buff particle effects
4. Tests: buff application, radius & stacking

Estimate: L
Priority: Medium

---

### Lance 🪚
**Overview:** Long reach, heavy defense with shield, counter moves.

Core mechanics:
- Shield block, long stabs, counter move

Mapping to Better Combat:
- BC block state + counter window on charge or perfect block
- Implement long reach attacks via attack hitbox extension

GeckoLib:
- Animations: idle, stab, block, counter

Data & config:
- Impact/Cutting based on hitzone, counter timing window

Tasks:
1. BC: Perfect block detection and counter action
2. Gameplay: Implement extended reach for lance stabs
3. GL: animations & shield visuals

Estimate: M
Priority: Medium

---

### Gunlance 🔫+🪚
**Overview:** Lance-like weapon with shelling and explosive Wyvern's Fire.

Core mechanics:
- Shelling (close-range fire), reload, Wyvern's Fire extreme damage with heavy penalty

Mapping to Better Combat:
- Add shelling actions mapped to secondary input; reload action; Wyvern's Fire as a charged special with durability/sharpness penalty

GeckoLib:
- Animations: shell_fire, reload, wyvern_charge, wyvern_fire

Data & config:
- Shelling damage, reload time, wyvern cooldown and penalty

Tasks:
1. BC: Add shelling / reload mechanics
2. Gameplay: track shells, implement Wyvern's Fire special
3. GL: animations and explosive VFX

Estimate: L
Priority: Medium

---

### Switch Axe (SA) 🔁
**Overview:** Transforming weapon with Axe and Sword modes; Sword-mode consumes a gauge/time.

Core mechanics:
- Instant mode transforms; Sword-mode limited by gauge/time

Mapping to Better Combat:
- Use BC to register a transform toggle that switches attack sets; Sword-mode sets a timer/gauge to allow different combos

GeckoLib:
- Animations: transform (axe→sword), axe_swing, sword_combo, gauge_visual

Data & config:
- Gauge duration, transform cooldown

Tasks:
1. BC: Implement mode toggle with distinct combo sets
2. GL: transform animation and sword-mode effects
3. Tests: gauge depletion and mode switching reliability

Estimate: M
Priority: Medium

---

### Charge Blade (CB) ⚡
**Overview:** Complex transform weapon with phials that charge and can be consumed for powerful attacks.

Core mechanics:
- Phial charging via hits, store phials; ability to perform elemental discharge or powerful shield combos

Mapping to Better Combat:
- Create `PhialStorage` component; charge on specific successful hits; provide special discharge action consuming phials

GeckoLib:
- Animations: charge_phial, discharge, transform, axe/sword attacks

Data & config:
- Phial count, charge rates, discharge formulas

Tasks:
1. Gameplay: Phial charging/storage and discharge consumption
2. BC: Integrate phial-based special attack hooks
3. GL: animations & phial visuals
4. Tests: phial charge/discharge edge cases

Estimate: L
Priority: Medium

---

### Insect Glaive (IG) 🐝
**Overview:** Pole vaulting & aerial-focused weapon with a Kinsect pet that grants buffs.

Core mechanics:
- Vault/jump maneuver, Kinsect summons to attack and return buffs

Mapping to Better Combat:
- Add a vault-jump action to BC (mid-combat jump with follow-up), Kinsect as a lightweight entity with attach/damage/return and buff application

GeckoLib:
- Animations: vault_jump, aerial_combo, kinsect_launch, kinsect_return

Data & config:
- Kinsect types, buff types/durations, vault height & cooldown

Tasks:
1. BC: Vault action and aerial transitions
2. Gameplay: Kinsect entity AI + attack/return/buff logic
3. GL: animations for vault & kinsect
4. Tests: aerial control & kinsect buff correctness

Estimate: L
Priority: Medium

---

### Tonfa (Frontier) 🌀
**Overview:** Lightweight, agile weapon with normal and short modes and air attacks.

Core mechanics:
- Mode toggle, aerial spin/rolling attack

Mapping to Better Combat:
- Implement `TonfaMode` toggle and air-attack chaining available while airborne

GeckoLib:
- Animations: normal_mode, short_mode, air_spin, roll

Data & config:
- Mode-specific attack tables and mobility adjustments

Tasks:
1. BC: Mode toggle and air attack hooks
2. GL: animations and mode visuals

Estimate: M
Priority: Low

---

### Magnet Spike (Frontier) 🧲
**Overview:** Heavy weapon with slashing/impact modes and magnetic mechanics.

Core mechanics:
- Mode switching, magnetic acceleration, unusual mobility when magnetic assistance active

Mapping to Better Combat:
- Add mode toggle and a magnetic acceleration buff that modifies movement speed and attack cadence when active

GeckoLib:
- Animations: mode_swap, accel_slash, magnetic_glow

Tasks:
1. BC & gameplay: Mode-specific attacks and movement modifier
2. GL: animations & magnetic VFX

Estimate: L
Priority: Low

---

### Accel Axe (Explore) ⚡
**Overview:** Heavy, short-reach axe that converts built-up energy to speed bursts.

Core mechanics:
- Energy build & consume for burst movement and special slashes

Mapping to Better Combat:
- Add energy meter component and `BurstSlash` action that consumes energy and modifies movement temporarily

GeckoLib:
- Animations: build_energy, burst_slash, aerial_crash

Tasks:
1. Gameplay: energy accumulation and consumption logic
2. BC: special burst action implementation
3. GL: animations & VFX

Estimate: M
Priority: Low

---

## Ranged Weapons

### Bow 🏹
**Overview:** Charged shots with multiple shot types (Scatter, Pierce, Rapid), coatings for effects.

Core mechanics:
- Charge levels, shot modes, coatings

Mapping to Better Combat:
- Use BC charge to adjust shot power; add separate shot-type actions mapped to charge + modifier keys

GeckoLib:
- Animations: draw_charge_lvl1..3, shoot_scatter, shoot_pierce, apply_coating

Data & config:
- Arrow behavior, flights, pierce counts, coating effects

Tasks:
1. Implement charge-based projectiles with separate behaviors
2. GL: bow draw animation and shot-specific animations
3. Tests: shot trajectories, coating effects

Estimate: M
Priority: High

---

### Light Bowgun (LBG) & Heavy Bowgun (HBG) 🔫
**Overview:** Projectile weapons with ammo types and unique behaviors (rapid fire for LBG, siege mode for HBG).

Core mechanics:
- Ammo types and magazine mechanics, rapid-fire, siege/crouch mode (HBG), shield mounting

Mapping to Better Combat:
- Bowguns will use BC for reload/fire input flow but are heavily projectile-driven; add ammo arrays and per-weapon rapid-fire modes

GeckoLib:
- Animations: load, fire, rapid_fire, reload, siege_enter/exit

Data & config:
- Ammo types, reload times, siege/rapid rates, shield mechanics for HBG

Tasks:
1. Projectile system extension (ammo definitions, special bullet behaviors)
2. GL: animations for loading/firing and siege mode
3. Tests: ammo types, rapid-fire behavior, siege mode

Estimate: L
Priority: High

---

### Medium Bowgun (MBG)
**Overview:** Rare middle-ground bowgun; include as a variant or midweight weapon.

Tasks: Implement as HBG/LBG hybrid with adjusted mobility and damage.

Estimate: S
Priority: Low

---

## Implementation Roadmap & Priorities (Suggested) 🗺️
1. Core infra: BC hooks for charge/toggle/states, weapon item base class, and GL animation loader (Priority: High)
2. High-priority weapons: Great Sword, Long Sword, Sword & Shield, Dual Blades, Bow, Bowguns (High effort but high visibility)
3. Medium-priority: Hammer, Switch Axe, Charge Blade, Lance, Gunlance, Insect Glaive
4. Low-priority / extra: Hunting Horn (complex), Tonfa, Magnet Spike, Accel Axe

---

## Testing checklist (per weapon) ✅
- Attack timing & hitboxes match intended feel
- Special mechanics trigger and reset properly
- Visual feedback (GL animations + particles) are synchronized with actions
- No input conflicts with BC or other weapons
- Multiplayer sync & server authority tests for projectiles, kinsect, buffs

---

## Assets and files to produce 🔧
- GeckoLib models & animations: `models/item/<weapon>.geo.json`, `animations/<weapon>/*.json`
- Item classes: `src/main/java/org/example/item/<Weapon>Item.java`
- BC integration: `src/main/java/org/example/bettercombat/weaponstates/*`
- Config: `data/mhweapons/weapon_stats/<weapon>.json`

---

If you want, I can create individual markdown files in `docs/weapons/` (one per weapon) or add these per-weapon tasks into the project's `plan.md`. Which do you prefer? 💡
