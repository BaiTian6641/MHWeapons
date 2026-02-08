# Decorations (Jewels) — Design Plan

## Goal
Add Monster Hunter–style **Decorations** that socket into **armor, weapons, and charms**. These give skill bonuses and can be swapped.

## Core Concepts
- **Decoration Item**: A jewel item that provides one or more **attribute modifiers** (e.g., Attack, Crit, Element, Status).
- **Slot Levels**: Slots have sizes (e.g., 1–4). A decoration has a **required level** and can only be inserted into a slot of **equal or higher** level.
   - We track this as **tier** (defaults to size). Tier is the required slot level.
- **Gear Slots**:
  - **Armor**: Each piece has 0–N deco slots.
  - **Weapon**: Optional slots for offense/element.
  - **Charm/Accessory**: Optional slots for utility/skills.

## Reference Summary (Monster Hunter Wilds)
Based on the Shacknews guide of all Decorations & Jewel Skills, decorations are split into **weapon** and **armor** jewel pools. Each jewel grants a skill that maps cleanly to attributes or gameplay modifiers. Common categories include:

**Weapon Jewel Skill Themes**
- **Element/Status Attack**: Fire/Water/Thunder/Ice/Dragon Attack, Poison/Paralysis/Sleep/Blast Attack.
- **Weapon Mechanics**: Guard, Guard Up, Handicraft, Razor Sharp, Protective Polish, Speed Sharpening.
- **Affinity/Crit**: Critical Eye, Critical Boost, Critical Element, Critical Status.
- **Weapon-Specific**: Artillery (shells/phials), Load Shells, Rapid Fire Up, Normal/Pierce/Spread Shots, Horn Maestro.

**Armor Jewel Skill Themes**
- **Defense/Resist**: Defense Boost, Fire/Water/Thunder/Ice/Dragon Resistance.
- **Survivability/Utility**: Divine Blessing, Recovery Speed/Up, Evade Window/Extender, Earplugs, Stun Resistance.
- **Hunt Utility**: Partbreaker, Flinch Free, Weakness Exploit, Agitator, Counter skills, and gathering/mobility skills.

This informs our **starting jewel set** and which attributes or custom hooks we need in code.

## Proposed Data Model
### Decoration JSON (data-driven)
Path: `data/mhweaponsmod/decorations/*.json`

```json
{
  "id": "attack_jewel",
  "size": 2,
   "tier": 2,
   "tier_multiplier": 1.0,
   "rarity": "uncommon",
   "category": "weapon",
  "attributes": [
   { "attribute": "minecraft:generic.attack_damage", "amount": 1.0, "operation": "add", "name": "mhweaponsmod.attack_jewel" }
  ],
  "tags": ["offense", "physical"]
}
```

### Slot Container (per gear)
Each gear item defines its decoration sockets via NBT (or JSON default):

```json
"deco_slots": [1, 2, 3] // sizes per slot
```

Defaults can be supplied via `data/mhweaponsmod/gear_decorations/<item>.json`:
```json
{ "slots": [2, 1] }
```

Use **weapon** or **armor** categories on decorations to restrict which gear they can be installed into.

Charms can define decoration slots from **1 to 3** via their gear defaults.

### Decoration Instance
Per item stack, store installed jewels:
```
"mh_decorations": [
  {"slot": 0, "id": "mhweaponsmod:attack_jewel"},
  {"slot": 1, "id": "mhweaponsmod:critical_jewel"}
]
```

## System Behavior
1. **Validation**
   - A decoration can only be inserted into a slot with size >= deco.tier.
   - Decorations cannot exceed available slots.
   - Tier defaults to size but can be set independently for balance.
2. **Attribute Aggregation**
   - Total modifiers = gear base modifiers + all installed decorations.
   - Decorations apply as attribute modifiers on equip.
3. **Swap/Remove**
   - Use a GUI or anvil-like interface to insert/remove jewels.
4. **Stacking Rules**
   - Some deco types can be limited with tags (e.g., only 1 "unique" or max per group).

## Implementation Plan (Forge + Curios)
1. **Data Loaders**
   - `DecorationDataManager` (like accessories) loads JSON.
2. **NBT Schema**
   - For armor/weapons/charms, store `mh_decorations` + `mh_deco_slots`.
3. **Attribute Application**
   - Hook into `ItemStack#getAttributeModifiers` for armor/weapons.
   - For Curios items, use `ICurioItem#getAttributeModifiers`.
   - Merge base modifiers with installed decoration modifiers.
4. **UI**
   - Add a simple socket UI (like a 1–4 slot grid).
   - Show slot sizes and inserted jewel icons.
   - Provide a **Decoration Workbench** for installation/removal (non-GUI fallback: hand + workbench).
   - Workbench GUI supports placing a gear item + jewel and installing/removing.
   - Slot grid renders jewel icons and tier color borders.
   - Tooltips show jewel tier/rarity and attribute lines.
   - Workbench supports a category filter (any/weapon/armor) for installs.
   - Installed jewel slots render rarity-colored borders.
5. **Balance Defaults**
   - Start with a small set of common jewels (Attack, Defense, Crit, Element, Status).
   - Later expand with complex multi-skill jewels.

## Suggested Phases
1. **Phase 1 — Data & NBT**
   - JSON loader, NBT schema, install/remove commands.
2. **Phase 2 — Attributes**
   - Apply modifiers for armor/weapon/charm.
3. **Phase 3 — UI**
   - Add in-game socket management screen + Decoration Workbench GUI.
4. **Phase 4 — Balance**
   - Add rarity tiers, unique jewels, and drop tables.

## Integration with Existing Systems
- **Accessory system**: Decorations apply to Curios-based charms and can add MH-specific attributes (element, status buildup, stamina, etc.).
- **Elemental system**: Decorations can boost elemental damage or status buildup attributes already defined.

## Starter Jewel Set (Suggested)
**Tier 1 (Basic, easy to balance):**
- Attack, Defense, Critical Eye, Critical Boost
- Fire/Water/Thunder/Ice/Dragon Attack
- Poison/Paralysis/Sleep/Blast Attack
- Speed Sharpening, Razor Sharp

**Tier 2 (Weapon-focused):**
- Artillery, Load Shells, Guard, Guard Up
- Normal Shots, Pierce, Spread, Rapid Fire Up
- Horn Maestro

**Tier 3 (Advanced/Conditional):**
- Weakness Exploit, Agitator, Protective Polish
- Critical Element, Critical Status

## Next Steps
If approved, I can implement Phase 1 (data + NBT + commands), then Phase 2 (attribute aggregation).