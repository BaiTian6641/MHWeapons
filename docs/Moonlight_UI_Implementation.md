# Workbench UI Improvement – Moonlight Library Integration

> **Date:** 2025-02-07
> **Scope:** Bowgun Workbench & Decoration Workbench screens
> **Dependency added:** Moonlight Library `1.20-2.16.27-forge`

---

## 1. Overview

Both workbench UIs (`BowgunWorkbenchScreen` and `DecorationWorkbenchScreen`) have been
rewritten to use **fully programmatic rendering** instead of the generic Minecraft chest
texture (`generic_54.png`). The Moonlight Library is now declared as a dependency to
enable future dynamic-texture work (runtime texture generation for weapon variants,
palettes, etc.), even though the immediate UI improvements are achieved with vanilla
`GuiGraphics` draw calls routed through a new shared helper class.

### Goals
| # | Goal | Status |
|---|------|--------|
| 1 | Remove dependency on `generic_54.png` | ✅ Done |
| 2 | Dark themed panel backgrounds | ✅ Done |
| 3 | Colour-coded mod-category / tier-based slots | ✅ Done |
| 4 | Rich tooltips with attribute details | ✅ Done |
| 5 | Shared render helper for consistency | ✅ Done |
| 6 | Moonlight dep available for future texture work | ✅ Done |

---

## 2. Files Changed

### 2.1 New Files

| File | Purpose |
|------|---------|
| `src/main/java/org/example/client/ui/WorkbenchRenderHelper.java` | Shared static utility class with all programmatic drawing primitives: panels, slots, stat bars, weight gauges, category chips, tier/rarity colours |

### 2.2 Modified Files

| File | Summary of Changes |
|------|-------------------|
| `build.gradle` | Added `implementation fg.deobf('maven.modrinth:moonlight:1.20-2.16.27-forge')` |
| `META-INF/mods.toml` | Added `[[dependencies.mhweaponsmod]]` block for `moonlight` (mandatory, `[2.16,)`) |
| `BowgunWorkbenchScreen.java` | Complete rewrite – programmatic background, category-coloured mod slots with 3-letter abbreviations, stat preview with multiplier/modifier helpers, rich tooltips on every slot |
| `DecorationWorkbenchScreen.java` | Complete rewrite – programmatic background, tier-coloured decoration slots with hover highlight, rarity-bordered installed jewels, enhanced tooltips with attribute modifiers and separator, gear info panel |
| `BowgunWorkbenchMenu.java` | Player inventory `startY` moved from 100 → 110 to match new screen height |
| `DecorationWorkbenchMenu.java` | Player inventory `startY` moved from 128 → 138 to match new screen height |

---

## 3. Architecture

### 3.1 Rendering Pipeline

```
AbstractContainerScreen
  ├── renderBg()       ← programmatic panels, slots, dividers
  ├── renderLabels()   ← stat text, gear info (relative coords)
  └── render()         ← hover tooltips, filter button state
         └── WorkbenchRenderHelper.*   (static utility methods)
```

Both screens follow the same pattern:
1. **`renderBg`** draws the entire background using `WorkbenchRenderHelper` – outer panel,
   title bar with gold accent, section headers, slot outlines, player-inventory slot grid.
2. **`renderLabels`** draws text in container-relative coordinates (labels, stats, gear info).
3. **`render`** handles tooltips (hover detection) and updates filter button active states.

### 3.2 WorkbenchRenderHelper API

```java
// Panels & boxes
drawPanel(g, x, y, w, h)
drawPanel(g, x, y, w, h, bg, border)
drawSlot(g, x, y)
drawSlot(g, x, y, borderCol)
drawDisabledSlot(g, x, y)

// Dividers & headers
drawHDivider(g, x, y, w)
drawSectionHeader(g, font, text, x, y, w)

// Stats
drawStatBar(g, font, label, value, maxValue, barCol, x, y, barW)
drawMultiplierStat(g, font, label, multiplier, x, y)
drawModifierStat(g, font, label, modifier, x, y)
drawBooleanStat(g, font, label, value, x, y)
drawWeightGauge(g, font, weight, weightClass, x, y, barW)

// Category / Tier helpers
drawCategoryChip(g, font, category, x, y)
getCategoryColor(category)       → int ARGB
getTierBorderColor(tier)         → int ARGB
getRarityColor(rarity)           → int ARGB
```

### 3.3 Colour Palette

| Constant | Hex | Use |
|----------|-----|-----|
| `COL_PANEL_BG` | `#1E1E2E` | Main panel background |
| `COL_PANEL_BORDER` | `#4A4A5E` | Panel border lines |
| `COL_SLOT_BG` | `#0E0E1A` | Slot interior |
| `COL_SLOT_BORDER` | `#3A3A4E` | Default slot border |
| `COL_ACCENT_GOLD` | `#F0C040` | Title text, section underlines, highlights |
| `COL_SECTION_BG` | `#282840` | Section header strips |
| `COL_POSITIVE` | `#66DD66` | Good stat values |
| `COL_NEGATIVE` | `#DD6666` | Bad stat values |
| `COL_NEUTRAL` | `#AAAAAA` | Unchanged stats |

---

## 4. Bowgun Workbench Screen Details

### Layout (imageWidth=200, imageHeight=210)

```
┌──────────────────────────────────────────────┐
│ [Gold title bar]        "Bowgun Workbench"    │
├──────────┬───────────────────────────────────┤
│ Weapon   │  Modifications                    │
│ [Bowgun] │  [Frm] [Bar] [Sto] [Mag]         │
│  slot    │  [Shi] [Spe] [Cos]                │
├──────────┴───────────────────────────────────┤
│ DMG: ×1.25   Reload: ×0.80   Recoil: +2     │
│              Guard: Yes                      │
│ [✔ Apply Mods]                               │
├──────────────────────────────────────────────┤
│ Player Inventory (3×9 + hotbar)              │
└──────────────────────────────────────────────┘
```

**Key improvements:**
- Each mod slot border is colour-coded by category (Frame=blue, Barrel=brown, Stock=green, etc.)
- 3-letter abbreviation rendered below each slot
- Stats use colour-coded multiplier/modifier formatting (green = buff, red = nerf)
- Rich tooltip on each mod slot shows category name + installed mod details
- Rich tooltip on bowgun slot shows weight class and installed mod count

### Slot Positions (unchanged from menu)
- Bowgun: (26, 44)
- Mods: 80 + (i%4)×20, 24 + (i/4)×20
- Player inventory: (8, 110) — moved +10 from original

---

## 5. Decoration Workbench Screen Details

### Layout (imageWidth=200, imageHeight=232)

```
┌──────────────────────────────────────────────┐
│ [Gold title bar]     "Decoration Workbench"   │
├──────────┬───────────────────────────────────┤
│ Equipment│  Decorations                      │
│ [Gear]   │  [T2] [T3] [T1] [T2]             │
│  slot    │  [💎] [T1] [--] [--]              │
│          │  [--] [--] [--] [--]              │
├──────────┤                                   │
│ ⚔ Weapon │  Filter: ⚔ Weapon                 │
│ Slots: 4 │                                   │
│ Used: 1/4│                                   │
├──────────┴───────────────────────────────────┤
│ [✔ Install] [✖ Remove]                       │
│ [All] [Weapon] [Armor]                       │
├──────────────────────────────────────────────┤
│ Player Inventory (3×9 + hotbar)              │
└──────────────────────────────────────────────┘
```

**Key improvements:**
- Tier-coloured slot borders (Silver/Green/Blue/Purple/Gold for tiers 1-5)
- Disabled slots rendered with dark X pattern
- Rarity-coloured borders for installed decorations
- Hover highlight on decoration slots
- Enhanced tooltips with:
  - Rarity-coloured decoration name
  - Tier • Rarity • Category line
  - Separator line
  - Colour-coded attribute modifiers (green = positive, red = negative)
  - Slot position indicator ("Slot 2 of 4")
- Gear info panel showing category icon, slot count, and usage ratio
- Active filter buttons highlighted with `§e` (yellow) formatting

### Slot Positions (unchanged from menu)
- Gear: (26, 24)
- Decorations: 80 + (col)×18, 24 + (row)×18
- Player inventory: (8, 138) — moved +10 from original

---

## 6. Moonlight Library – Future Usage

The Moonlight Library (`net.mehvahdjukaar:moonlight`) is declared as a dependency but its
texture-generation APIs are **not yet actively used** in these screens. The dependency was
added now to establish the foundation for planned features:

### 6.1 Planned Dynamic Texture Features

| Feature | Moonlight API | Status |
|---------|---------------|--------|
| Weapon variant textures (recolours) | `TextureImage`, `Palette`, `Respriter` | Planned |
| Runtime GUI background textures | `DynamicClientResourceProvider`, `ResourceSink` | Planned |
| Composite weapon preview icons | `TextureCollager` | Planned |
| Bowgun mod part previews | `Respriter.of()` + palette swap | Planned |

### 6.2 How to Use Moonlight's Dynamic Resource System

```java
// 1. Create a provider class
public class MHWeaponsDynamicAssets extends DynamicClientResourceProvider {
    public MHWeaponsDynamicAssets() {
        super(new DynamicResourcesProvider.Info("mhweapons_dynamic"));
    }

    @Override
    public void regenerateDynamicAssets(ResourceSink sink) {
        // Generate textures at runtime
        try (TextureImage base = TextureImage.open(manager, baseTexturePath)) {
            Palette palette = Palette.fromImage(base);
            try (TextureImage recoloured = Respriter.of(base).recolor(newPalette)) {
                sink.addTexture(outputPath, recoloured);
            }
        }
    }
}

// 2. Register in mod init
RegHelper.registerDynamicResourceProvider(MHWeaponsDynamicAssets::new);
```

### 6.3 Moonlight Maven Coordinates

```groovy
// build.gradle
repositories {
    maven { url "https://api.modrinth.com/maven" }
}
dependencies {
    implementation fg.deobf('maven.modrinth:moonlight:1.20-2.16.27-forge')
}
```

```toml
# mods.toml
[[dependencies.mhweaponsmod]]
modId = "moonlight"
mandatory = true
versionRange = "[2.16,)"
ordering = "AFTER"
side = "BOTH"
```

---

## 7. Testing Checklist

- [ ] Open Bowgun Workbench → verify dark panel renders without texture errors
- [ ] Place a bowgun → verify stat preview shows coloured multipliers
- [ ] Hover mod slots → verify category tooltip appears
- [ ] Place mods → verify they render in colour-coded slots
- [ ] Open Decoration Workbench → verify dark panel renders
- [ ] Place gear → verify tier-coloured decoration slots appear
- [ ] Install decoration → verify rarity border and item icon
- [ ] Hover decoration → verify rich tooltip with attributes
- [ ] Test filter buttons → verify active state highlighting
- [ ] Hover disabled slot → verify no crash, X pattern visible
- [ ] Verify player inventory slots align with background
- [ ] Test shift-click transfer still works correctly

---

## 8. Known Limitations

1. **No external texture assets** – the UI is entirely code-drawn, which means no fancy
   gradients or noise textures. A future iteration could use Moonlight's
   `DynamicClientResourceProvider` to generate richer background textures at runtime.

2. **Button styling** – vanilla `Button` widgets are still used. These could be replaced
   with custom widgets for a more Monster Hunter-themed look in a future pass.

3. **Screen width increased** – `imageWidth` was changed from 176 to 200 to accommodate
   longer labels. This is wider than a standard chest GUI but still fits on all resolutions.

4. **Inventory slot Y offset** – both menus had their player-inventory `startY` adjusted
   (+10 pixels) to match the new screen heights. This is a breaking change if any
   external code relies on exact slot positions.
