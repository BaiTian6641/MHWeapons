package org.example.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.example.common.combat.bowgun.BowgunModResolver;
import org.example.common.menu.BowgunWorkbenchMenu;
import org.example.item.BowgunItem;
import org.example.item.BowgunModItem;

import java.util.ArrayList;
import java.util.List;

import static org.example.client.ui.WorkbenchRenderHelper.*;

/**
 * Client-side screen for the Bowgun Modification Workbench.
 * <p>
 * Uses fully programmatic rendering instead of external texture files:
 * dark themed panel background, colour-coded mod-category slots,
 * stat bars, weight gauge, and rich tooltips on mod slots.
 */
@SuppressWarnings("null")
public class BowgunWorkbenchScreen extends AbstractContainerScreen<BowgunWorkbenchMenu> {

                private static final String[] CATEGORY_LABELS = {
                    "Frame", "Barrel", "Stock", "Magazine", "Shield", "Special", "Accessory", "Accessory", "Cosmetic"
                };

    /** Stat bar maximum references */
    private static final float MAX_DMG_MULT    = 2.0f;
    private static final float MAX_RELOAD_MULT = 2.0f;
    private static final int   MAX_RECOIL      = 10;

    public BowgunWorkbenchScreen(BowgunWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth  = 200;
        this.imageHeight = 240;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    // ───────────────────────────── Init ─────────────────────────────

    @Override
    protected void init() {
        super.init();
        // "Apply Mods" button – positioned below the stat preview area
        int btnX = this.leftPos + 8;
        int btnY = this.topPos + 120;
        this.addRenderableWidget(Button.builder(Component.literal("✔ Apply Mods"), btn -> sendButton(0))
                .bounds(btnX, btnY, 80, 18)
                .build());
    }

    private void sendButton(int id) {
        if (this.minecraft == null || this.minecraft.gameMode == null) return;
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    // ──────────────────────────── Background ────────────────────────

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;

        // ── Outer panel ──
        drawPanel(g, x, y, w, h);

        // ── Title bar ──
        g.fill(x + 1, y + 1, x + w - 1, y + 14, COL_SECTION_BG);
        g.fill(x + 1, y + 14, x + w - 1, y + 15, COL_ACCENT_GOLD);
        g.drawString(this.font, this.title, x + 6, y + 3, COL_ACCENT_GOLD, false);

        // ── Bowgun weapon section ──
        drawSectionHeader(g, this.font, "Weapon", x + 4, y + 17, 68);
        drawSlot(g, x + 25, y + 34, COL_ACCENT_GOLD); // bowgun slot at (26,35) → border at (25,34)

        // ── Mod slots section ──
        drawSectionHeader(g, this.font, "Modifications", x + 78, y + 17, 118);
        ItemStack bowgun = this.menu.getSlot(0).getItem();
        boolean showAccessory2 = bowgun.getItem() instanceof BowgunItem
                && BowgunItem.getWeightClass(bowgun) == 2;
        for (int i = 0; i < CATEGORY_LABELS.length; i++) {
            if (i == 7 && !showAccessory2) continue; // hide 2nd accessory slot unless Heavy
            // Row 1: y=28 (slot at 29), Row 2: y=54 (slot at 55)
            int sx = x + 79 + (i % 4) * 20;
            int sy = y + 28 + (i / 4) * 26;
            int catCol = getCategoryColor(CATEGORY_LABELS[i]);
            drawSlot(g, sx, sy, catCol);
        }

        // ── Category labels under each mod slot ──
        for (int i = 0; i < CATEGORY_LABELS.length; i++) {
            if (i == 7 && !showAccessory2) continue; // hide 2nd accessory slot label unless Heavy
            int sx = x + 79 + (i % 4) * 20;
            int sy = y + 28 + (i / 4) * 26;
            // 3-letter abbreviation below each slot
            String abbr = CATEGORY_LABELS[i].length() >= 3
                    ? CATEGORY_LABELS[i].substring(0, 3)
                    : CATEGORY_LABELS[i];
            int tw = this.font.width(abbr);
            g.drawString(this.font, abbr, sx + (18 - tw) / 2, sy + 19, getCategoryColor(CATEGORY_LABELS[i]), false);
        }

        // ── Stat preview panel ──
        drawPanel(g, x + 3, y + 82, w - 6, 34, 0xFF16162A, COL_PANEL_BORDER);

        // ── Player inventory separator ──
        drawHDivider(g, x + 4, y + 142, w - 8);

        // ── Player inventory background slots ──
        int invStartX = x + 8;
        int invStartY = y + 146;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(g, invStartX + col * 18 - 1, invStartY + row * 18 - 1);
            }
        }
        // Hotbar
        int hotbarY = invStartY + 58;
        for (int col = 0; col < 9; col++) {
            drawSlot(g, invStartX + col * 18 - 1, hotbarY - 1);
        }
    }

    // ──────────────────────────── Labels / Stats ────────────────────

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Inventory label
        g.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY + 12, COL_LABEL, false);

        // Stats preview when bowgun is present
        ItemStack bowgun = this.menu.getSlot(0).getItem();
        if (!bowgun.isEmpty() && bowgun.getItem() instanceof BowgunItem) {
            renderStatsPreview(g, bowgun);
        } else {
            g.drawString(this.font, "Place a bowgun to begin", 8, 92, 0xFF888899, false);
        }
    }

    private void renderStatsPreview(GuiGraphics g, ItemStack bowgun) {
        int x = 6;
        int y = 85;

        List<String> mods = BowgunItem.getInstalledMods(bowgun);
        float dmgMult    = BowgunModResolver.resolveDamageMultiplier(mods);
        float reloadMult = BowgunModResolver.resolveReloadMultiplier(mods);
        int recoilMod    = BowgunModResolver.resolveRecoilModifier(mods);
        boolean guard    = BowgunModResolver.resolveGuardEnabled(mods);
        int weight       = BowgunItem.getWeight(bowgun);
        int weightClass  = BowgunItem.getWeightClass(bowgun);

        // Line 1: Weight & Guard
        drawWeightGauge(g, this.font, weight, weightClass, x, y, 60);
        drawBooleanStat(g, this.font, "Guard", guard, x + 120, y);

        // Line 2: DMG & Reload
        drawMultiplierStat(g, this.font, "DMG", dmgMult, x, y + 12);
        drawMultiplierStat(g, this.font, "Reload", reloadMult, x + 80, y + 12);

        // Line 3: Recoil
        drawModifierStat(g, this.font, "Recoil", recoilMod, x, y + 24);
    }


    // ───────────────────────────── Tooltips ─────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);

        // Rich tooltips on mod category slots
        renderModSlotTooltips(g, mouseX, mouseY);

        this.renderTooltip(g, mouseX, mouseY);
    }

    /**
     * When hovering over a mod category slot, show a detailed tooltip with category
     * name and currently installed mod (if any).
     */
    private void renderModSlotTooltips(GuiGraphics g, int mouseX, int mouseY) {
        for (int i = 0; i < CATEGORY_LABELS.length; i++) {
            int sx = this.leftPos + 80 + (i % 4) * 20;
            int sy = this.topPos + 29 + (i / 4) * 26;
            if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.literal("§e" + CATEGORY_LABELS[i] + " Mod Slot"));
                ItemStack modStack = this.menu.getSlot(1 + i).getItem();
                if (modStack.isEmpty()) {
                    lines.add(Component.literal("§7Empty – place a " + CATEGORY_LABELS[i].toLowerCase() + " mod here"));
                } else {
                    lines.add(modStack.getHoverName().copy().withStyle(s -> s.withColor(0x55FF55)));
                    if (modStack.getItem() instanceof BowgunModItem modItem) {
                        lines.add(Component.literal("§7Category: " + modItem.getCategory()));
                    }
                }
                g.renderTooltip(this.font, lines, java.util.Optional.empty(), mouseX, mouseY);
                return;
            }
        }

        // Bowgun slot tooltip
        int bsx = this.leftPos + 26;
        int bsy = this.topPos + 35;
        if (mouseX >= bsx && mouseX < bsx + 16 && mouseY >= bsy && mouseY < bsy + 16) {
            ItemStack bowgun = this.menu.getSlot(0).getItem();
            if (!bowgun.isEmpty() && bowgun.getItem() instanceof BowgunItem) {
                List<Component> lines = new ArrayList<>();
                lines.add(bowgun.getHoverName().copy().withStyle(s -> s.withColor(COL_ACCENT_GOLD)));
                int weight = BowgunItem.getWeight(bowgun);
                int weightClass = BowgunItem.getWeightClass(bowgun);
                String className = switch (weightClass) {
                    case 0 -> "§aLight";
                    case 1 -> "§eMedium";
                    default -> "§cHeavy";
                };
                lines.add(Component.literal("§7Weight: " + weight + " (" + className + "§7)"));
                List<String> mods = BowgunItem.getInstalledMods(bowgun);
                lines.add(Component.literal("§7Installed mods: " + mods.size()));
                g.renderTooltip(this.font, lines, java.util.Optional.empty(), mouseX, mouseY);
            }
        }
    }
}
