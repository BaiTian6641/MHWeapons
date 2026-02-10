package org.example.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.combat.bowgun.BowgunMagazineManager;
import org.example.common.util.CapabilityUtil;
import org.example.item.BowgunItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Client-side overlay for ammo selection.
 * When visible, shows a scrollable ammo list above the compact ammo HUD.
 * The compact HUD always shows current ammo, magazine bar, and mode.
 */
public class AmmoSelectOverlay {
    private static boolean visible = false;
    private static int scrollIndex = 0;
    private static final int MAX_VISIBLE_ROWS = 8;

    private AmmoSelectOverlay() {}

    public static void setVisible(boolean v) {
        visible = v;
        if (v) {
            // Reset scroll to current ammo position
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                ItemStack stack = mc.player.getMainHandItem();
                if (stack.getItem() instanceof BowgunItem) {
                    PlayerWeaponState state = CapabilityUtil.getPlayerWeaponState(mc.player);
                    if (state != null) {
                        Set<String> compatible = BowgunMagazineManager.getCompatibleAmmo(stack);
                        List<String> list = new ArrayList<>(compatible);
                        int idx = list.indexOf(state.getBowgunCurrentAmmo());
                        scrollIndex = Math.max(0, idx - MAX_VISIBLE_ROWS / 2);
                    }
                }
            }
        }
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void scroll(int direction) {
        scrollIndex = Math.max(0, scrollIndex + direction);
    }

    /**
     * Render the ammo selection overlay. Called from a RenderGuiOverlayEvent handler.
     */
    public static void render(GuiGraphics guiGraphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof BowgunItem)) return;

        PlayerWeaponState state = CapabilityUtil.getPlayerWeaponState(player);
        if (state == null) return;

        String currentAmmo = state.getBowgunCurrentAmmo();
        Set<String> compatible = BowgunMagazineManager.getCompatibleAmmo(stack);
        if (compatible.isEmpty()) return;

        List<String> ammoList = new ArrayList<>(compatible);

        int screenW = guiGraphics.guiWidth();
        int screenH = guiGraphics.guiHeight();

        if (visible) {
            // Full radial/list mode
            renderFullList(guiGraphics, mc, player, stack, ammoList, currentAmmo, screenW, screenH);
        } else {
            // Compact HUD — just show current ammo & count
            renderCompact(guiGraphics, mc, player, stack, currentAmmo, screenW, screenH, state);
        }
    }

    private static void renderFullList(GuiGraphics gui, Minecraft mc, Player player,
                                        ItemStack stack, List<String> ammoList,
                                        String currentAmmo, int screenW, int screenH) {
        // Position: just above the compact HUD area (compact HUD is at screenH - 40)
        int panelW = 160;
        int rowH = 14;
        int visibleRows = Math.min(MAX_VISIBLE_ROWS, ammoList.size());
        int panelH = visibleRows * rowH + 16;
        int x = screenW / 2 + 100;
        int y = screenH - 50 - panelH; // above the compact HUD

        // Background
        gui.fill(x - 2, y, x + panelW, y + panelH, 0xCC000000);

        // Header
        gui.drawString(mc.font, "§e[ Ammo Select - Scroll to switch ]", x + 2, y + 3, 0xFFFFFF, true);

        int startRow = Math.max(0, Math.min(scrollIndex, ammoList.size() - MAX_VISIBLE_ROWS));
        int endRow = Math.min(ammoList.size(), startRow + visibleRows);

        for (int i = startRow; i < endRow; i++) {
            String ammoId = ammoList.get(i);
            int rowY = y + 14 + (i - startRow) * rowH;

            boolean selected = ammoId.equals(currentAmmo);
            int bgColor = selected ? 0x44FFFF00 : 0x00000000;
            gui.fill(x, rowY, x + panelW - 4, rowY + rowH - 1, bgColor);

            // Ammo name
            String displayName = formatAmmoName(ammoId);
            int nameColor = selected ? 0xFFFFD54F : 0xFFCCCCCC;
            gui.drawString(mc.font, (selected ? "▶ " : "  ") + displayName, x + 2, rowY + 2, nameColor, true);

            // Magazine count
            int magCount = BowgunItem.getMagazineCount(stack, ammoId);
            int invCount = BowgunMagazineManager.countAmmoInInventory(player, ammoId);
            String countStr = magCount + "/" + invCount;
            int countColor = magCount > 0 ? 0xFF8BC34A : (invCount > 0 ? 0xFFFF9800 : 0xFFFF5252);
            gui.drawString(mc.font, countStr, x + panelW - mc.font.width(countStr) - 8, rowY + 2, countColor, true);
        }

        // Scroll indicators
        if (startRow > 0) {
            gui.drawString(mc.font, "▲ more", x + panelW / 2 - 15, y + 14, 0x88FFFFFF, true);
        }
        if (endRow < ammoList.size()) {
            gui.drawString(mc.font, "▼ more", x + panelW / 2 - 15, y + panelH - 8, 0x88FFFFFF, true);
        }
    }

    private static void renderCompact(GuiGraphics gui, Minecraft mc, Player player,
                                       ItemStack stack, String currentAmmo,
                                       int screenW, int screenH, PlayerWeaponState state) {
        if (currentAmmo.isEmpty()) return;

        int x = screenW / 2 + 100;
        int y = screenH - 40;

        String displayName = formatAmmoName(currentAmmo);
        int magCount = BowgunItem.getMagazineCount(stack, currentAmmo);
        int capacity = BowgunMagazineManager.getEffectiveMagCapacity(stack, currentAmmo);
        int invCount = BowgunMagazineManager.countAmmoInInventory(player, currentAmmo);

        // Ammo name
        gui.drawString(mc.font, displayName, x, y, 0xFFFFD54F, true);

        // Magazine bar
        int barX = x;
        int barY = y + 10;
        int barW = 60;
        int barH = 4;
        float ratio = capacity > 0 ? (float) magCount / capacity : 0;
        gui.fill(barX, barY, barX + barW, barY + barH, 0xAA000000);
        int fillW = Math.round(ratio * barW);
        int barColor = magCount > 0 ? 0xFF8BC34A : 0xFFFF5252;
        gui.fill(barX, barY, barX + fillW, barY + barH, barColor);

        // Count text
        gui.drawString(mc.font, magCount + "/" + capacity + " [" + invCount + "]",
                barX + barW + 4, barY - 2, 0xFFCCCCCC, true);

        // Mode indicator
        int mode = state.getBowgunMode();
        String modeStr = switch (mode) {
            case 1 -> "§a[RAPID]";
            case 2 -> "§e[VERSATILE]";
            case 3 -> "§c[IGNITION]";
            default -> "[STANDARD]";
        };
        gui.drawString(mc.font, modeStr, x, y - 10, 0xFFFFFF, true);
    }

    private static String formatAmmoName(String ammoId) {
        if (ammoId == null || ammoId.isEmpty()) return "None";
        // Convert snake_case to Title Case
        String[] parts = ammoId.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1));
                sb.append(' ');
            }
        }
        return sb.toString().trim();
    }
}
