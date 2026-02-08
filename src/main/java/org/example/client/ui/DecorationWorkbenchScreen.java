package org.example.client.ui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.example.common.menu.DecorationWorkbenchMenu;
import org.example.common.data.DecorationData;
import org.example.common.data.DecorationDataManager;
import org.example.common.util.DecorationUtil;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

@SuppressWarnings("null")
public class DecorationWorkbenchScreen extends AbstractContainerScreen<DecorationWorkbenchMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");
    private static final int SLOT_SIZE = 16;
    private static final int SLOT_SPACING = 18;
    private static final int DECORATION_COLS = 4;
    private static final int DECORATION_ROWS = 3;

    private Button filterAllButton;
    private Button filterWeaponButton;
    private Button filterArmorButton;

    public DecorationWorkbenchScreen(DecorationWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int btnY = this.topPos + 88;
        int btnX = this.leftPos + 8;
        this.addRenderableWidget(Button.builder(Component.literal("Install"), btn -> sendButton(0))
                .bounds(btnX, btnY, 60, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Remove"), btn -> sendButton(1))
                .bounds(btnX + 70, btnY, 60, 20)
                .build());

        int filterY = btnY + 26;
        this.filterAllButton = this.addRenderableWidget(Button.builder(Component.literal("All"), btn -> sendButton(2))
            .bounds(btnX, filterY, 40, 18)
            .build());
        this.filterWeaponButton = this.addRenderableWidget(Button.builder(Component.literal("Weapon"), btn -> sendButton(3))
            .bounds(btnX + 44, filterY, 60, 18)
            .build());
        this.filterArmorButton = this.addRenderableWidget(Button.builder(Component.literal("Armor"), btn -> sendButton(4))
            .bounds(btnX + 108, filterY, 56, 18)
            .build());
    }

    private void sendButton(int id) {
        if (this.minecraft == null || this.minecraft.gameMode == null) {
            return;
        }
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    @Override
    @SuppressWarnings("null")
    protected void renderBg(net.minecraft.client.gui.GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        renderDecorationSlots(guiGraphics, mouseX, mouseY);
    }

    @Override
    @SuppressWarnings("null")
    protected void renderLabels(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var gear = this.menu.getGearStack();
        if (gear.isEmpty()) {
            guiGraphics.drawString(this.font, "Gear: (none)", 8, 6, 0x404040, false);
            return;
        }
        int[] slots = DecorationUtil.getSlotsWithDefaults(gear);
        String slotStr = slots.length == 0 ? "(none)" : java.util.Arrays.toString(slots);
        guiGraphics.drawString(this.font, "Gear slots: " + slotStr, 8, 6, 0x404040, false);
        guiGraphics.drawString(this.font, "Gear type: " + gearCategoryLabel(DecorationUtil.getGearCategory(gear)), 8, 16, 0x404040, false);
        guiGraphics.drawString(this.font, "Filter: " + filterLabel(this.menu.getFilterMode()), 8, 26, 0x404040, false);
        var decos = DecorationUtil.getDecorations(gear);
        if (!decos.isEmpty()) {
            String decoStr = decos.stream().map(d -> "s" + d.slot()).collect(java.util.stream.Collectors.joining(","));
            guiGraphics.drawString(this.font, "Installed: " + decoStr, 8, 36, 0x404040, false);
        }
    }

    @Override
    @SuppressWarnings("null")
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateFilterButtonLabels();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderDecorationTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderDecorationSlots(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY) {
        var gear = this.menu.getGearStack();
        int[] slots = gear.isEmpty() ? new int[0] : DecorationUtil.getSlotsWithDefaults(gear);
        int startX = this.leftPos + 80;
        int startY = this.topPos + 24;
        for (int i = 0; i < DECORATION_COLS * DECORATION_ROWS; i++) {
            int col = i % DECORATION_COLS;
            int row = i / DECORATION_COLS;
            int x = startX + col * SLOT_SPACING;
            int y = startY + row * SLOT_SPACING;
            if (i >= slots.length) {
                guiGraphics.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, 0xFF3A3A3A);
                guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF151515);
                continue;
            }
            int tier = slots[i];
            int border = tierColor(tier);
            guiGraphics.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, border);
            guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF1A1A1A);
            var deco = DecorationUtil.findDecorationAt(gear, i);
            if (deco != null) {
                var item = ForgeRegistries.ITEMS.getValue(deco.id());
                if (item != null) {
                    DecorationData data = DecorationDataManager.INSTANCE.get(deco.id());
                    if (data != null) {
                        int rarityBorder = rarityColor(data.getRarity());
                        guiGraphics.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, rarityBorder);
                    }
                    guiGraphics.renderItem(new net.minecraft.world.item.ItemStack(item), x, y);
                }
            } else {
                guiGraphics.drawString(this.font, String.valueOf(tier), x + 5, y + 4, 0xFFFFFFFF, false);
            }
        }
    }

    private void renderDecorationTooltip(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY) {
        var gear = this.menu.getGearStack();
        if (gear.isEmpty()) {
            return;
        }
        int[] slots = DecorationUtil.getSlotsWithDefaults(gear);
        int startX = this.leftPos + 80;
        int startY = this.topPos + 24;
        for (int i = 0; i < DECORATION_COLS * DECORATION_ROWS && i < slots.length; i++) {
            int col = i % DECORATION_COLS;
            int row = i / DECORATION_COLS;
            int x = startX + col * SLOT_SPACING;
            int y = startY + row * SLOT_SPACING;
            if (mouseX >= x && mouseX <= x + SLOT_SIZE && mouseY >= y && mouseY <= y + SLOT_SIZE) {
                var deco = DecorationUtil.findDecorationAt(gear, i);
                if (deco == null) {
                    guiGraphics.renderTooltip(this.font, Component.literal("Empty slot (tier " + slots[i] + ")"), mouseX, mouseY);
                    return;
                }
                var item = ForgeRegistries.ITEMS.getValue(deco.id());
                var stack = item == null ? null : new net.minecraft.world.item.ItemStack(item);
                String name = stack == null ? deco.id().toString() : stack.getHoverName().getString();
                DecorationData data = DecorationDataManager.INSTANCE.get(deco.id());
                String rarity = data != null ? data.getRarity() : "common";
                int tier = data != null ? Math.max(1, data.getTier()) : slots[i];
                String category = data != null ? data.getCategory() : "armor";
                java.util.List<Component> lines = new java.util.ArrayList<>();
                lines.add(Component.literal(name));
                lines.add(Component.literal("Tier " + tier + " • " + rarity + " • " + category)
                    .withStyle(style -> style.withColor(rarityColor(rarity))));
                if (data != null) {
                    for (var mod : data.getModifiers()) {
                        String amountStr = formatAmount(mod.amount(), mod.operation());
                        Component attrName = Component.translatable(mod.attribute().getDescriptionId());
                        lines.add(Component.literal(amountStr + " ").append(attrName));
                    }
                }
                guiGraphics.renderTooltip(this.font, lines, java.util.Optional.empty(), mouseX, mouseY);
                return;
            }
        }
    }

    private String formatAmount(double amount, AttributeModifier.Operation operation) {
        double value = amount;
        switch (operation) {
            case ADDITION -> {
                return (value >= 0 ? "+" : "") + trimDouble(value);
            }
            case MULTIPLY_BASE, MULTIPLY_TOTAL -> {
                double pct = value * 100.0;
                String suffix = operation == AttributeModifier.Operation.MULTIPLY_BASE ? "% base" : "% total";
                return (pct >= 0 ? "+" : "") + trimDouble(pct) + suffix;
            }
        }
        return (value >= 0 ? "+" : "") + trimDouble(value);
    }

    private String trimDouble(double value) {
        String s = String.format(java.util.Locale.ROOT, "%.2f", value);
        if (s.endsWith(".00")) {
            return s.substring(0, s.length() - 3);
        }
        if (s.endsWith("0")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }

    private int tierColor(int tier) {
        return switch (tier) {
            case 1 -> 0xFFB0BEC5;
            case 2 -> 0xFF81C784;
            case 3 -> 0xFF64B5F6;
            case 4 -> 0xFFBA68C8;
            default -> 0xFFFFD54F;
        };
    }

    private int rarityColor(String rarity) {
        if (rarity == null) {
            return 0xFFB0BEC5;
        }
        return switch (rarity.toLowerCase()) {
            case "common" -> 0xFFB0BEC5;
            case "uncommon" -> 0xFF81C784;
            case "rare" -> 0xFF64B5F6;
            case "epic" -> 0xFFBA68C8;
            case "legendary" -> 0xFFFFD54F;
            default -> 0xFFFFFFFF;
        };
    }

    private String filterLabel(int mode) {
        return switch (mode) {
            case 1 -> "weapon \u2694";
            case 2 -> "armor \u26E8";
            default -> "any";
        };
    }

    private String gearCategoryLabel(String category) {
        if ("weapon".equalsIgnoreCase(category)) {
            return "weapon \u2694";
        }
        if ("armor".equalsIgnoreCase(category)) {
            return "armor \u26E8";
        }
        return category;
    }

    private void updateFilterButtonLabels() {
        if (filterAllButton == null || filterWeaponButton == null || filterArmorButton == null) {
            return;
        }
        int mode = this.menu.getFilterMode();
        filterAllButton.setMessage(Component.literal(mode == 0 ? "[All]" : "All"));
        filterWeaponButton.setMessage(Component.literal(mode == 1 ? "[Weapon]" : "Weapon"));
        filterArmorButton.setMessage(Component.literal(mode == 2 ? "[Armor]" : "Armor"));
    }
}