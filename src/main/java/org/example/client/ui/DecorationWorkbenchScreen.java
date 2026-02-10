package org.example.client.ui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.example.common.menu.DecorationWorkbenchMenu;
import org.example.common.data.DecorationData;
import org.example.common.data.DecorationDataManager;
import org.example.common.util.DecorationUtil;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.example.client.ui.WorkbenchRenderHelper.*;

/**
 * Client-side screen for the Decoration (Jewel) Workbench.
 * <p>
 * Fully programmatic rendering – no external texture file. Features:
 * <ul>
 *   <li>Dark panel background with gold accent title bar</li>
 *   <li>Colour-coded decoration slots based on tier</li>
 *   <li>Rarity-bordered installed decorations</li>
 *   <li>Rich tooltips with attribute modifier details</li>
 *   <li>Active-state filter buttons with visual feedback</li>
 * </ul>
 */
@SuppressWarnings("null")
public class DecorationWorkbenchScreen extends AbstractContainerScreen<DecorationWorkbenchMenu> {

    private static final int SLOT_SIZE = 16;
    private static final int SLOT_SPACING = 18;
    private static final int DECORATION_COLS = 4;
    private static final int DECORATION_ROWS = 3;

    private Button filterAllButton;
    private Button filterWeaponButton;
    private Button filterArmorButton;

    public DecorationWorkbenchScreen(DecorationWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 200;
        this.imageHeight = 240;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    // ───────────────────────────── Init ─────────────────────────────

    @Override
    protected void init() {
        super.init();
        int btnX = this.leftPos + 8;
        int btnY = this.topPos + 90;

        this.addRenderableWidget(Button.builder(Component.literal("✔ Install"), btn -> sendButton(0))
                .bounds(btnX, btnY, 60, 18)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("✖ Remove"), btn -> sendButton(1))
                .bounds(btnX + 64, btnY, 60, 18)
                .build());

        int filterY = btnY + 26;
        this.filterAllButton = this.addRenderableWidget(Button.builder(Component.literal("All"), btn -> sendButton(2))
                .bounds(btnX, filterY, 40, 16)
                .build());
        this.filterWeaponButton = this.addRenderableWidget(Button.builder(Component.literal("Weapon"), btn -> sendButton(3))
                .bounds(btnX + 44, filterY, 55, 16)
                .build());
        this.filterArmorButton = this.addRenderableWidget(Button.builder(Component.literal("Armor"), btn -> sendButton(4))
                .bounds(btnX + 103, filterY, 50, 16)
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

        // ── Gear slot section ──
        drawSectionHeader(g, this.font, "Equipment", x + 4, y + 17, 68);
        drawSlot(g, x + 25, y + 23, COL_ACCENT_GOLD); // gear slot at (26,24)

        // ── Decoration slots section ──
        drawSectionHeader(g, this.font, "Decorations", x + 78, y + 17, 118);
        renderDecorationSlots(g, mouseX, mouseY);

        // ── Gear info panel ──
        drawPanel(g, x + 3, y + 46, 70, 38, 0xFF16162A, COL_PANEL_BORDER);

        // ── Player inventory separator ──
        drawHDivider(g, x + 4, y + 140, w - 8);

        // ── Player inventory background slots ──
        int invStartX = x + 8;
        int invStartY = y + 148;
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

    /**
     * Draw decoration slot grid with tier-coloured borders and installed jewel icons.
     */
    private void renderDecorationSlots(GuiGraphics g, int mouseX, int mouseY) {
        var gear = this.menu.getGearStack();
        int[] slots = gear.isEmpty() ? new int[0] : DecorationUtil.getSlotsWithDefaults(gear);
        int startX = this.leftPos + 80;
        int startY = this.topPos + 24;

        for (int i = 0; i < DECORATION_COLS * DECORATION_ROWS; i++) {
            int col = i % DECORATION_COLS;
            int row = i / DECORATION_COLS;
            int sx = startX + col * SLOT_SPACING;
            int sy = startY + row * SLOT_SPACING;

            if (i >= slots.length) {
                // Disabled / unavailable slot
                drawDisabledSlot(g, sx - 1, sy - 1);
                continue;
            }

            int tier = slots[i];
            int borderCol = getTierBorderColor(tier);

            var deco = DecorationUtil.findDecorationAt(gear, i);
            if (deco != null) {
                // Installed decoration – show rarity border
                DecorationData data = DecorationDataManager.INSTANCE.get(deco.id());
                if (data != null) {
                    borderCol = getRarityColor(data.getRarity());
                }
                drawSlot(g, sx - 1, sy - 1, borderCol);
                var item = ForgeRegistries.ITEMS.getValue(deco.id());
                if (item != null) {
                    g.renderItem(new ItemStack(item), sx, sy);
                }
            } else {
                // Empty slot – show tier number
                drawSlot(g, sx - 1, sy - 1, borderCol);
                String tierStr = String.valueOf(tier);
                int tw = this.font.width(tierStr);
                g.drawString(this.font, tierStr, sx + (16 - tw) / 2, sy + 4, borderCol, false);
            }

            // Hover highlight
            if (mouseX >= sx - 1 && mouseX < sx + 17 && mouseY >= sy - 1 && mouseY < sy + 17) {
                g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0x40FFFFFF);
            }
        }
    }

    // ──────────────────────────── Labels ────────────────────────────

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Inventory label
        g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, COL_LABEL, false);

        // Gear info panel content
        var gear = this.menu.getGearStack();
        int ix = 6;
        int iy = 48;
        if (gear.isEmpty()) {
            g.drawString(this.font, "No gear", ix, iy, 0xFF888899, false);
            g.drawString(this.font, "placed", ix, iy + 10, 0xFF888899, false);
        } else {
            int[] slots = DecorationUtil.getSlotsWithDefaults(gear);
            String cat = DecorationUtil.getGearCategory(gear);
            String catIcon = "weapon".equalsIgnoreCase(cat) ? "⚔" : "armor".equalsIgnoreCase(cat) ? "⛨" : "?";

            g.drawString(this.font, catIcon + " " + capitalize(cat), ix, iy, COL_ACCENT_GOLD, false);
            g.drawString(this.font, "Slots: " + slots.length, ix, iy + 10, COL_LABEL, false);

            var decos = DecorationUtil.getDecorations(gear);
            int filled = decos.size();
            int decoCol = filled == slots.length ? COL_POSITIVE : COL_NEUTRAL;
            g.drawString(this.font, "Used: " + filled + "/" + slots.length, ix, iy + 20, decoCol, false);
        }

        // Filter indicator
        int mode = this.menu.getFilterMode();
        String filterStr = switch (mode) {
            case 1 -> "⚔ Weapon";
            case 2 -> "⛨ Armor";
            default -> "All";
        };
        g.drawString(this.font, "Filter: " + filterStr, 6, 38, COL_LABEL, false);
    }

    // ───────────────────────────── Render ───────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        updateFilterButtonLabels();
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderDecorationTooltip(g, mouseX, mouseY);
        this.renderTooltip(g, mouseX, mouseY);
    }

    // ──────────────────────────── Tooltips ──────────────────────────

    private void renderDecorationTooltip(GuiGraphics g, int mouseX, int mouseY) {
        var gear = this.menu.getGearStack();
        if (gear.isEmpty()) return;

        int[] slots = DecorationUtil.getSlotsWithDefaults(gear);
        int startX = this.leftPos + 80;
        int startY = this.topPos + 24;

        for (int i = 0; i < DECORATION_COLS * DECORATION_ROWS && i < slots.length; i++) {
            int col = i % DECORATION_COLS;
            int row = i / DECORATION_COLS;
            int sx = startX + col * SLOT_SPACING;
            int sy = startY + row * SLOT_SPACING;

            if (mouseX >= sx - 1 && mouseX <= sx + 17 && mouseY >= sy - 1 && mouseY <= sy + 17) {
                var deco = DecorationUtil.findDecorationAt(gear, i);
                if (deco == null) {
                    List<Component> lines = new ArrayList<>();
                    lines.add(Component.literal("§7Empty Slot §f(Tier " + slots[i] + ")"));
                    lines.add(Component.literal("§8Place a tier " + slots[i] + " or lower decoration"));
                    g.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
                    return;
                }

                var item = ForgeRegistries.ITEMS.getValue(deco.id());
                var stack = item == null ? null : new ItemStack(item);
                String name = stack == null ? deco.id().toString() : stack.getHoverName().getString();
                DecorationData data = DecorationDataManager.INSTANCE.get(deco.id());
                String rarity = data != null ? data.getRarity() : "common";
                int tier = data != null ? Math.max(1, data.getTier()) : slots[i];
                String category = data != null ? data.getCategory() : "armor";

                List<Component> lines = new ArrayList<>();
                lines.add(Component.literal(name).withStyle(s -> s.withColor(getRarityColor(rarity))));
                lines.add(Component.literal("§8Tier " + tier + " • " + capitalize(rarity) + " • " + capitalize(category)));

                // Separator
                lines.add(Component.literal("§8──────────────"));

                if (data != null) {
                    for (var mod : data.getModifiers()) {
                        String amountStr = formatAmount(mod.amount(), mod.operation());
                        Component attrName = Component.translatable(mod.attribute().getDescriptionId());
                        int modCol = mod.amount() >= 0 ? 0x55FF55 : 0xFF5555;
                        lines.add(Component.literal(amountStr + " ")
                                .withStyle(s -> s.withColor(modCol))
                                .append(attrName));
                    }
                }

                lines.add(Component.literal(""));
                lines.add(Component.literal("§7Slot " + (i + 1) + " of " + slots.length));

                g.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
                return;
            }
        }
    }

    // ──────────────────────────── Helpers ───────────────────────────

    private String formatAmount(double amount, AttributeModifier.Operation operation) {
        switch (operation) {
            case ADDITION -> {
                return (amount >= 0 ? "+" : "") + trimDouble(amount);
            }
            case MULTIPLY_BASE, MULTIPLY_TOTAL -> {
                double pct = amount * 100.0;
                String suffix = operation == AttributeModifier.Operation.MULTIPLY_BASE ? "% base" : "% total";
                return (pct >= 0 ? "+" : "") + trimDouble(pct) + suffix;
            }
        }
        return (amount >= 0 ? "+" : "") + trimDouble(amount);
    }

    private String trimDouble(double value) {
        String s = String.format(Locale.ROOT, "%.2f", value);
        if (s.endsWith(".00")) return s.substring(0, s.length() - 3);
        if (s.endsWith("0"))  return s.substring(0, s.length() - 1);
        return s;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1).toLowerCase(Locale.ROOT);
    }

    private void updateFilterButtonLabels() {
        if (filterAllButton == null || filterWeaponButton == null || filterArmorButton == null) return;
        int mode = this.menu.getFilterMode();
        filterAllButton.setMessage(Component.literal(mode == 0 ? "§e[All]" : "All"));
        filterWeaponButton.setMessage(Component.literal(mode == 1 ? "§e[Weapon]" : "Weapon"));
        filterArmorButton.setMessage(Component.literal(mode == 2 ? "§e[Armor]" : "Armor"));
    }
}