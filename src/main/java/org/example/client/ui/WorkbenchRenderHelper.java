package org.example.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Shared rendering utilities for MHWeapons workbench screens.
 * Provides programmatic drawing of panels, slots, stat bars, dividers, and section headers
 * so that workbench GUIs look polished without requiring external texture PNGs.
 *
 * All coordinates are in absolute screen-space unless noted otherwise.
 *
 * Colour conventions (ARGB):
 * <ul>
 *   <li>Panel background: dark grey 0xFF1E1E2E</li>
 *   <li>Panel border:     brighter grey 0xFF4A4A5E</li>
 *   <li>Slot background:  very dark 0xFF0E0E1A</li>
 *   <li>Slot border:      medium grey 0xFF3A3A4E</li>
 *   <li>Highlight accent: MH gold 0xFFF0C040</li>
 * </ul>
 */
public final class WorkbenchRenderHelper {

    // ── Palette ──
    public static final int COL_PANEL_BG     = 0xFF1E1E2E;
    public static final int COL_PANEL_BORDER  = 0xFF4A4A5E;
    public static final int COL_SLOT_BG       = 0xFF0E0E1A;
    public static final int COL_SLOT_BORDER   = 0xFF3A3A4E;
    public static final int COL_ACCENT_GOLD   = 0xFFF0C040;
    public static final int COL_SECTION_BG    = 0xFF282840;
    public static final int COL_DIVIDER       = 0xFF3A3A4E;
    public static final int COL_LABEL         = 0xFFBBBBCC;
    public static final int COL_VALUE         = 0xFFFFFFFF;
    public static final int COL_BAR_BG        = 0xFF0A0A12;
    public static final int COL_POSITIVE      = 0xFF66DD66;
    public static final int COL_NEGATIVE      = 0xFFDD6666;
    public static final int COL_NEUTRAL       = 0xFFAAAAAA;

    private WorkbenchRenderHelper() {}

    // ────────────────────────────── Panels & Boxes ──────────────────────────────

    /** Draw a bordered rectangular panel. */
    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        drawPanel(g, x, y, w, h, COL_PANEL_BG, COL_PANEL_BORDER);
    }

    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h, int bg, int border) {
        // border
        g.fill(x, y, x + w, y + h, border);
        // inner
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
    }

    /** Draw a standard 18×18 slot (16 inner + 1px border). */
    public static void drawSlot(GuiGraphics g, int x, int y) {
        drawSlot(g, x, y, COL_SLOT_BORDER);
    }

    public static void drawSlot(GuiGraphics g, int x, int y, int borderCol) {
        g.fill(x, y, x + 18, y + 18, borderCol);
        g.fill(x + 1, y + 1, x + 17, y + 17, COL_SLOT_BG);
    }

    /** Draw a disabled / locked slot (visually darker, with X). */
    public static void drawDisabledSlot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF2A2A2A);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF0A0A0A);
        // draw small X
        g.fill(x + 4, y + 8, x + 14, y + 10, 0xFF333333);
        g.fill(x + 8, y + 4, x + 10, y + 14, 0xFF333333);
    }

    // ────────────────────────────── Dividers & Headers ──────────────────────────

    /** Horizontal divider line. */
    public static void drawHDivider(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, COL_DIVIDER);
    }

    /** Section header: coloured strip with centred text. */
    public static void drawSectionHeader(GuiGraphics g, net.minecraft.client.gui.Font font,
                                          String text, int x, int y, int w) {
        g.fill(x, y, x + w, y + 12, COL_SECTION_BG);
        g.fill(x, y + 12, x + w, y + 13, COL_ACCENT_GOLD);
        int textW = font.width(text);
        g.drawString(font, text, x + (w - textW) / 2, y + 2, COL_ACCENT_GOLD, false);
    }

    // ──────────────────────────────── Stat Bars ─────────────────────────────────

    /**
     * Render a horizontal stat bar with label.
     *
     * @param g        graphics context
     * @param font     font renderer
     * @param label    stat name (e.g. "Damage")
     * @param value    current value in [0..maxValue]
     * @param maxValue the maximum value for the bar
     * @param barCol   colour of the filled portion
     * @param x        left X
     * @param y        top Y
     * @param barW     total bar width
     */
    public static void drawStatBar(GuiGraphics g, net.minecraft.client.gui.Font font,
                                    String label, float value, float maxValue,
                                    int barCol, int x, int y, int barW) {
        // Label
        g.drawString(font, label, x, y, COL_LABEL, false);
        int barX = x;
        int barY = y + 10;
        int barH = 4;
        // Background
        g.fill(barX, barY, barX + barW, barY + barH, COL_BAR_BG);
        // Filled
        float frac = Math.max(0f, Math.min(value / maxValue, 1f));
        int fillW = (int)(barW * frac);
        if (fillW > 0) {
            g.fill(barX, barY, barX + fillW, barY + barH, barCol);
        }
    }

    /**
     * Draw a labelled multiplier stat (e.g. "DMG: ×1.25").
     */
    public static void drawMultiplierStat(GuiGraphics g, net.minecraft.client.gui.Font font,
                                           String label, float multiplier,
                                           int x, int y) {
        int col = multiplier > 1.0f ? COL_POSITIVE : (multiplier < 1.0f ? COL_NEGATIVE : COL_NEUTRAL);
        String text = label + ": ×" + String.format("%.2f", multiplier);
        g.drawString(font, text, x, y, col, true);
    }

    /**
     * Draw a labelled integer modifier stat (e.g. "Recoil: +3").
     */
    public static void drawModifierStat(GuiGraphics g, net.minecraft.client.gui.Font font,
                                         String label, int modifier,
                                         int x, int y) {
        int col = modifier < 0 ? COL_POSITIVE : (modifier > 0 ? COL_NEGATIVE : COL_NEUTRAL);
        String sign = modifier >= 0 ? "+" : "";
        String text = label + ": " + sign + modifier;
        g.drawString(font, text, x, y, col, true);
    }

    /**
     * Draw a boolean indicator (e.g. "Guard: Yes / No").
     */
    public static void drawBooleanStat(GuiGraphics g, net.minecraft.client.gui.Font font,
                                        String label, boolean value,
                                        int x, int y) {
        String text = label + ": " + (value ? "§aYes" : "§cNo");
        g.drawString(font, text, x, y, COL_VALUE, true);
    }

    // ────────────────────────────── Weight Indicator ────────────────────────────

    /**
     * Render a visual weight gauge: coloured bar + weight class label.
     * Weight 0-10 → Light (green), 11-20 → Medium (yellow), 21+ → Heavy (red).
     */
    public static void drawWeightGauge(GuiGraphics g, net.minecraft.client.gui.Font font,
                                        int weight, int weightClass,
                                        int x, int y, int barW) {
        String classLabel = switch (weightClass) {
            case 0 -> "Light";
            case 1 -> "Medium";
            default -> "Heavy";
        };
        int barCol = switch (weightClass) {
            case 0 -> 0xFF44BB44;
            case 1 -> 0xFFDDBB33;
            default -> 0xFFCC4444;
        };
        int classCol = switch (weightClass) {
            case 0 -> COL_POSITIVE;
            case 1 -> 0xFFDDDD44;
            default -> COL_NEGATIVE;
        };

        g.drawString(font, "Weight", x, y, COL_LABEL, false);
        // Bar
        int barY = y + 10;
        g.fill(x, barY, x + barW, barY + 6, COL_BAR_BG);
        float frac = Math.max(0f, Math.min(weight / 50f, 1f));
        int fillW = (int)(barW * frac);
        if (fillW > 0) {
            g.fill(x, barY, x + fillW, barY + 6, barCol);
        }
        // Class label to the right of bar
        g.drawString(font, classLabel + " (" + weight + ")", x + barW + 4, barY - 1, classCol, true);
    }

    // ──────────────────────────── Category Label Chip ───────────────────────────

    /**
     * Draw a small coloured "chip" label for a mod category (e.g. "Frame", "Barrel").
     */
    public static void drawCategoryChip(GuiGraphics g, net.minecraft.client.gui.Font font,
                                         String category, int x, int y) {
        int chipCol = getCategoryColor(category);
        int textW = font.width(category);
        int chipW = textW + 4;
        g.fill(x, y, x + chipW, y + 10, chipCol);
        g.drawString(font, category, x + 2, y + 1, 0xFFFFFFFF, false);
    }

    /** Get the accent colour for a bowgun mod category. */
    public static int getCategoryColor(String category) {
        return switch (category.toLowerCase()) {
            case "frame"    -> 0xFF5566AA;
            case "barrel"   -> 0xFF886633;
            case "stock"    -> 0xFF338855;
            case "magazine" -> 0xFF885533;
            case "shield"   -> 0xFF555588;
            case "special"  -> 0xFF885588;
            case "cosmetic" -> 0xFF888855;
            default         -> 0xFF555555;
        };
    }

    // ──────────────────────────── Decoration Tier ───────────────────────────────

    /** Tier border colour for decoration slots. */
    public static int getTierBorderColor(int tier) {
        return switch (tier) {
            case 1 -> 0xFF8899AA; // silver
            case 2 -> 0xFF55AA55; // green
            case 3 -> 0xFF5588DD; // blue
            case 4 -> 0xFFAA55BB; // purple
            default -> 0xFFDDBB44; // gold
        };
    }

    /** Rarity text colour. */
    public static int getRarityColor(String rarity) {
        if (rarity == null) return 0xFFB0BEC5;
        return switch (rarity.toLowerCase()) {
            case "common"    -> 0xFFB0BEC5;
            case "uncommon"  -> 0xFF81C784;
            case "rare"      -> 0xFF64B5F6;
            case "epic"      -> 0xFFBA68C8;
            case "legendary" -> 0xFFFFD54F;
            default          -> 0xFFFFFFFF;
        };
    }
}
