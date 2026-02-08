# Wilds Decorations Effects Plan

## Phase 1 (Implemented)
**Goal:** Effects that can be expressed as attributes.

- Affinity (Critical Eye / Expert)
  - `mhweaponsmod:affinity`
- Critical damage bonus (Critical)
  - `mhweaponsmod:crit_damage_bonus`
- Element critical bonus (Crit Element)
  - `mhweaponsmod:element_crit_bonus`
- Status critical bonus (Crit Status)
  - `mhweaponsmod:status_crit_bonus`
- Element attack jewels (Blaze/Stream/Bolt/Frost/Dragon)
  - `mhweaponsmod:*_damage`
- Status attack jewels (Venom/Poison/Paralyzer/Sleep/Blast)
  - `mhweaponsmod:*_buildup`
- Attack/Defense/Vitality/Evasion/Armor/Focus
  - `minecraft:generic.attack_damage`
  - `minecraft:generic.armor`
  - `minecraft:generic.max_health`
  - `minecraft:generic.movement_speed`
  - `minecraft:generic.attack_speed`

## Phase 2 (Needs combat hooks)
**Goal:** Effects that require new combat systems.

- Guard/Guard Up (Ironwall/Shield)
- Critical Draw (Draw)
- Offensive Guard (Guardian)
- Bludgeoner/Handicraft/Razor Sharp/Sharp/Master's Touch (sharpness system)
- Normal/Pierce/Spread/Trueshot/Salvo (ranged ammo logic)
- Coating jewels (Blastcoat/Paracoat/Poisoncoat/Sleepcoat/Draincoat)
- Artillery/Charge/Charge Up/KO/Opener/Magazine (weapon-specific systems)
- Mind's Eye/Precise (hitzone/deflection logic)

## Phase 3 (Utility/Environment)
**Goal:** Non-combat or environment effects.

- Resistances (Fire/Water/Ice/Thunder/Dragon Res)
- Earplugs/Wind Resist/Flash/Perfume/Antidote/Antipara/Antiblast
- Stamina economy (Physique/Sprinter/Refresh)
- Misc utility (Botany/Geology/Ranger/Fungiform/Specimen/etc.)

## Notes
- Phase 1 effects are configured in [data/mhweaponsmod/decorations](data/mhweaponsmod/decorations) and [data/mhweaponsmod/decorations/wilds_bulk.json](data/mhweaponsmod/decorations/wilds_bulk.json).
- Phases 2–3 require new gameplay systems. Once you confirm priorities, I will implement them sequentially.
