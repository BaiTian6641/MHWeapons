package org.example.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import java.util.ArrayList;
import java.util.List;
import org.example.client.input.FocusModeClient;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.example.client.input.ClientKeybinds;
import org.example.common.combat.FocusModeHelper;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.combat.MHDamageTypeProvider;
import org.example.common.config.MHWeaponsConfig;
import org.example.common.data.DecorationDataManager;
import org.example.common.data.GearDecorationDataManager;
import org.example.common.data.WeaponData;
import org.example.common.data.WeaponDataResolver;
import org.example.common.util.CapabilityUtil;
import org.example.common.entity.KinsectEntity;
import org.example.item.MHTiers;
import org.example.item.WeaponIdProvider;

public final class WeaponHudOverlay {
    private static final int MAX_SHARPNESS = 100;

    public static final IGuiOverlay OVERLAY = (gui, guiGraphics, partialTick, width, height) -> {
        renderOverlay(guiGraphics, width, height);
    };

    @SuppressWarnings("null")
    private static void renderOverlay(GuiGraphics guiGraphics, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        renderFocusCrosshair(guiGraphics, width, height);
        Font font = minecraft.font;
        renderDecorationStatus(guiGraphics, font, width, height);
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof MHDamageTypeProvider)) {
            return;
        }

        int x = 10; // Top-left
        int y = 10;

        Component name = stack.getHoverName();
        guiGraphics.drawString(font, name, x, y, 0xFFFFFF, true);

        int barX = x;
        int barY = y + 12;
        PlayerWeaponState weaponState = CapabilityUtil.getPlayerWeaponState(player);
        PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(player);
        renderEffectIcons(guiGraphics, minecraft, player, barX, barY - 10);
        renderPlayerBars(guiGraphics, barX, barY, player, weaponState);

        int sharpness = resolveSharpness(stack);
        renderSharpnessBlade(guiGraphics, barX, barY + 18, sharpness, stack);

        if (stack.getItem() instanceof WeaponIdProvider weaponIdProvider) {
            if (weaponState != null) {
                renderWeaponGauge(guiGraphics, font, player, weaponIdProvider.getWeaponId(), weaponState, x, barY + 28);
            }
        }

        if (MHWeaponsConfig.SHOW_ATTACK_HUD.get()) {
            renderAttackHud(guiGraphics, font, width, player, weaponState, combatState);
        }

        if (stack.getItem() instanceof WeaponIdProvider weaponIdProvider && "insect_glaive".equals(weaponIdProvider.getWeaponId())) {
            if (weaponState != null) {
                int baseY = MHWeaponsConfig.SHOW_ATTACK_HUD.get() ? 120 : 70;
                renderKinsectHud(guiGraphics, font, width, weaponState, baseY);
            }
        }
    }

    private static void renderKinsectHud(GuiGraphics guiGraphics, Font font, int width, PlayerWeaponState state, int y) {
        // Render Extracts
        int xCenter = width / 2;
        int barWidth = 80;
        int x = xCenter - (barWidth / 2);
        
        int redColor = state.isInsectRed() ? 0xFFE53935 : 0xAA424242;
        int whiteColor = state.isInsectWhite() ? 0xFFEEEEEE : 0xAA424242;
        int orangeColor = state.isInsectOrange() ? 0xFFFFB74D : 0xAA424242;
        
        int slotW = 25;
        int gap = 2;
        
        guiGraphics.fill(x, y, x + slotW, y + 4, redColor);
        guiGraphics.fill(x + slotW + gap, y, x + slotW * 2 + gap, y + 4, whiteColor);
        guiGraphics.fill(x + (slotW + gap) * 2, y, x + slotW * 3 + gap * 2, y + 4, orangeColor);

        // Render Timer if active
        if (state.getInsectExtractTicks() > 0) {
            float ratio = Math.min(1.0f, state.getInsectExtractTicks() / 1200.0f);
            int timerW = Math.round((slotW * 3 + gap * 2) * ratio);
            int timerColor = 0xFFFFF176;
            if (state.isInsectRed() && state.isInsectWhite() && state.isInsectOrange()) {
                timerColor = 0xFF81C784; // Green for Triple Up
            }
            guiGraphics.fill(x, y + 6, x + timerW, y + 8, timerColor);
        }

        // Render Kinsect State Debug (Optional, smaller)
        // ... kept minimal
    }

    private static void renderFocusCrosshair(GuiGraphics guiGraphics, int width, int height) {
        FocusModeHelper.RangeTier tier = FocusModeClient.getRangeTier();
        boolean flash = FocusModeClient.shouldHighlightCrosshair();
        if (tier == FocusModeHelper.RangeTier.NONE && !flash) {
            return;
        }

        int cx = width / 2;
        int cy = height / 2;
        int len = 8;
        int gap = 3;

        int tierColor = switch (tier) {
            case SWEET_SPOT -> 0xFF26C6DA; // teal: ideal spacing
            case TOO_CLOSE -> 0xFFEFB549; // amber: back up a step
            case LONG -> 0xFF64B5F6; // blue: inch closer
            default -> 0xAA26C6DA;
        };
        int color = flash && tier == FocusModeHelper.RangeTier.NONE ? 0xCC26C6DA : tierColor;

        guiGraphics.fill(cx - gap - len, cy - 1, cx - gap, cy + 1, color);
        guiGraphics.fill(cx + gap, cy - 1, cx + gap + len, cy + 1, color);
        guiGraphics.fill(cx - 1, cy - gap - len, cx + 1, cy - gap, color);
        guiGraphics.fill(cx - 1, cy + gap, cx + 1, cy + gap + len, color);

        if (tier != FocusModeHelper.RangeTier.NONE) {
            int ring = 10;
            int stroke = 1;
            guiGraphics.fill(cx - ring, cy - ring, cx + ring, cy - ring + stroke, tierColor);
            guiGraphics.fill(cx - ring, cy + ring - stroke, cx + ring, cy + ring, tierColor);
            guiGraphics.fill(cx - ring, cy - ring, cx - ring + stroke, cy + ring, tierColor);
            guiGraphics.fill(cx + ring - stroke, cy - ring, cx + ring, cy + ring, tierColor);
            guiGraphics.fill(cx - 1, cy - 1, cx + 1, cy + 1, tierColor);
        }
    }

    @SuppressWarnings("null")
    private static void renderDecorationStatus(GuiGraphics guiGraphics, Font font, int width, int height) {
        int decoCount = DecorationDataManager.INSTANCE.getAll().size();
        int gearCount = GearDecorationDataManager.INSTANCE.getAll().size();
        int xRight = width - 10;
        int y = 10;
        int decoColor = decoCount > 0 ? 0xFF81C784 : 0xFFEF5350;
        int gearColor = gearCount > 0 ? 0xFF81C784 : 0xFFEF5350;
        drawRightAligned(guiGraphics, font, xRight, y, "Decorations: " + decoCount, decoColor);
        y += 10;
        drawRightAligned(guiGraphics, font, xRight, y, "Gear slots: " + gearCount, gearColor);
    }

    private static int resolveSharpness(ItemStack stack) {
        if (stack.hasTag()) {
            var tag = stack.getTag();
            if (tag != null && tag.contains("mh_sharpness")) {
                return Math.min(MAX_SHARPNESS, Math.max(0, tag.getInt("mh_sharpness")));
            }
        }
        if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
            float durability = 1.0f - (stack.getDamageValue() / (float) stack.getMaxDamage());
            return Math.min(MAX_SHARPNESS, Math.max(0, Math.round(durability * MAX_SHARPNESS)));
        }
        return MAX_SHARPNESS;
    }

    private static int sharpnessColor(int sharpness, ItemStack stack) {
        float ratio = sharpness / (float) MAX_SHARPNESS;
        if (stack.getItem() instanceof TieredItem tieredItem) {
             Tier tier = tieredItem.getTier();
             boolean isHighTier = tier == MHTiers.BLACK || tier == MHTiers.PURPLE;
             
             if (isHighTier) {
                 if (ratio >= 0.85f) return 0xFF9C27B0; // Purple
                 if (ratio >= 0.70f) return 0xFFFFFFFF; // White
                 if (ratio >= 0.55f) return 0xFF4FC3F7; // Blue
             }
        }
        if (ratio >= 0.8f) {
            return 0xFF4FC3F7; // blue
        }
        if (ratio >= 0.6f) {
            return 0xFF66BB6A; // green
        }
        if (ratio >= 0.4f) {
            return 0xFFFFEE58; // yellow
        }
        if (ratio >= 0.2f) {
            return 0xFFFFA726; // orange
        }
        return 0xFFEF5350; // red
    }

    @SuppressWarnings("null")
    private static void renderWeaponGauge(GuiGraphics guiGraphics, Font font, Player player, String weaponId, PlayerWeaponState state, int x, int y) {
        int barWidth = 90;
        int barHeight = 6;
        switch (weaponId) {
            case "longsword" -> {
                drawGauge(guiGraphics, x, y, barWidth, barHeight, state.getSpiritGauge() / 100.0f, 0xFF29B6F6);
                float spiritDecay = resolveSpiritDecayRatio(state.getSpiritLevel(), state.getSpiritLevelTicks());
                renderSpiritBlade(guiGraphics, x, y + 8, barWidth, 4, state.getSpiritLevel(), spiritDecay);
                guiGraphics.drawString(font, "Spirit Lv " + state.getSpiritLevel(), x + barWidth + 6, y - 1, 0xFFFFFF, true);
            }
            case "dual_blades" -> {
                int color = state.isDemonMode() ? 0xFFE53935 : 0xFFEC407A;
                drawGauge(guiGraphics, x, y, barWidth, barHeight, state.getDemonGauge() / 100.0f, color);
                String mode = state.isDemonMode() ? "Demon" : (state.isArchDemon() ? "Arch" : "Normal");
                guiGraphics.drawString(font, "Mode: " + mode, x + barWidth + 6, y - 1, 0xFFFFFF, true);
            }
            case "switch_axe" -> {
                drawGauge(guiGraphics, x, y, barWidth, barHeight, state.getSwitchAxeAmpGauge() / 100.0f, 0xFF66BB6A);
                String mode = state.isSwitchAxeSwordMode() ? "Sword" : "Axe";
                guiGraphics.drawString(font, "Mode: " + mode, x + barWidth + 6, y - 1, 0xFFFFFF, true);
            }
            case "charge_blade" -> {
                drawGauge(guiGraphics, x, y, barWidth, barHeight, state.getChargeBladeCharge() / 100.0f, 0xFFFFCA28);
                guiGraphics.drawString(font, "Phials: " + state.getChargeBladePhials(), x + barWidth + 6, y - 1, 0xFFFFFF, true);
            }
            case "hammer" -> {
                guiGraphics.drawString(font, "Charge Lv " + state.getHammerChargeLevel(), x, y - 1, 0xFFFFFF, true);
                if (state.isHammerPowerCharge()) {
                    guiGraphics.drawString(font, "Power Charge", x + 80, y - 1, 0xFFEF6C00, true);
                }
            }
            case "hunting_horn" -> {
                WeaponData data = WeaponDataResolver.resolve(player);
                renderHornScore(guiGraphics, font, x, y - 2, state.getHornNoteA(), state.getHornNoteB(), state.getHornNoteC(), state.getHornNoteD(), state.getHornNoteE());
                String melodies = state.getHornMelodyCount() <= 0
                        ? "Melodies: -"
                    : "Melodies: " + melodyName(data, state.getHornMelodyA()) + " | " + melodyName(data, state.getHornMelodyB()) + " | " + melodyName(data, state.getHornMelodyC());
                guiGraphics.drawString(font, melodies, x, y + 8, 0xFFFFFF, true);
                String nextLabel = state.getHornMelodyCount() <= 0
                    ? "-"
                    : melodyName(data, melodyIdAt(state, state.getHornMelodyIndex()));
                guiGraphics.drawString(font, "Next: " + nextLabel, x, y + 18, 0xBDBDBD, true);
            }
            case "lance" -> {
                String guard = state.isLanceGuardActive() ? "Guard Up" : "Guard";
                guiGraphics.drawString(font, guard, x, y - 1, 0xFFFFFF, true);
            }
            case "gunlance" -> {
                // Layout:
                // Row 1: Shell Icons + WSC Icon
                // Row 2: Wyvern's Fire Gauge
                int glOffsetY = 10;
                int baseY = y + glOffsetY;
                boolean chargingShell = state.isChargingAttack();

                int shells = state.getGunlanceShells();
                int maxShells = state.getGunlanceMaxShells();
                int shellIconSize = 8;
                int shellGap = 2;
                
                // Draw Shells
                for (int i = 0; i < maxShells; i++) {
                    int color = i < shells ? 0xFFFFD700 : 0xFF424242; // Gold / Dark Gray
                    if (chargingShell) {
                        color = i < shells ? 0xFFFFFF6B : 0xFF616161; // Brighter highlight when charging
                    }
                    int sx = x + (i * (shellIconSize + shellGap));
                    int sy = baseY - 10;
                    int border = chargingShell ? 0xFFFFC107 : 0xFF000000;
                    guiGraphics.fill(sx, sy, sx + shellIconSize, sy + shellIconSize, border); // Border
                    guiGraphics.fill(sx + 1, sy + 1, sx + shellIconSize - 1, sy + shellIconSize - 1, color);
                }

                // Draw Wyrmstake Cannon (WSC) Icon
                int wscX = x + (maxShells * (shellIconSize + shellGap)) + 4;
                int wscY = baseY - 10;
                int wscWidth = 10;
                int wscHeight = 10; // Slightly larger
                int wscColor = state.hasGunlanceStake() ? 0xFF00E5FF : 0xFF424242; // Cyan / Dark Gray
                
                guiGraphics.fill(wscX, wscY, wscX + wscWidth, wscY + wscHeight, 0xFF000000);
                guiGraphics.fill(wscX + 1, wscY + 1, wscX + wscWidth - 1, wscY + wscHeight - 1, wscColor);
                
                if (state.hasGunlanceStake()) {
                    // Draw a simple "spike" shape or letter inside
                    guiGraphics.fill(wscX + 4, wscY + 2, wscX + 6, wscY + 8, 0xFFFFFFFF);
                }

                // Draw Wyvern's Fire Gauge (Row 2)
                int gaugeY = baseY + 4;
                float wfGauge = state.getGunlanceWyvernFireGauge();
                int gaugeColor = (state.getGunlanceWyvernfireCooldown() > 0 && wfGauge < 1.0f) ? 0xFF808080 : 0xFFFF5722; // Orange-Red
                int segmentWidth = (barWidth / 2) - 2;
                
                // Segment 1
                drawGauge(guiGraphics, x, gaugeY, segmentWidth, barHeight, Math.min(1.0f, wfGauge), gaugeColor);
                // Segment 2
                drawGauge(guiGraphics, x + segmentWidth + 4, gaugeY, segmentWidth, barHeight, Math.max(0.0f, wfGauge - 1.0f), gaugeColor);

                // Text status
                if (state.getGunlanceWyvernfireCooldown() > 0 && wfGauge < 1.0f) {
                     int sec = state.getGunlanceWyvernfireCooldown() / 20;
                     guiGraphics.drawString(font, sec + "s", x + barWidth + 6, gaugeY - 1, 0xFFFFFF, true);
                } else if (wfGauge >= 1.0f) {
                     guiGraphics.drawString(font, "Ready", x + barWidth + 6, gaugeY - 1, 0xFFFFAB00, true);
                }
            }
            case "insect_glaive" -> {
                String extracts = (state.isInsectRed() ? "R" : "-") + (state.isInsectWhite() ? "W" : "-") + (state.isInsectOrange() ? "O" : "-");
                guiGraphics.drawString(font, "Extracts: " + extracts, x, y - 1, 0xFFFFFF, true);
            }
            case "tonfa" -> {
                drawGauge(guiGraphics, x, y, barWidth, barHeight, state.getTonfaComboGauge() / 100.0f, 0xFFAB47BC);
                String mode = state.isTonfaShortMode() ? "Short" : "Long";
                guiGraphics.drawString(font, "Mode: " + mode, x + barWidth + 6, y - 1, 0xFFFFFF, true);
            }
            case "magnet_spike" -> {
                int color = state.isMagnetSpikeImpactMode() ? 0xFFEF5350 : 0xFF26C6DA; // Red for Impact, Cyan for Cut
                drawGauge(guiGraphics, x, y, barWidth, barHeight, state.getMagnetGauge() / 100.0f, color);
                
                String mode = state.isMagnetSpikeImpactMode() ? "Impact Mode" : "Cutting Mode";
                guiGraphics.drawString(font, mode, x + barWidth + 6, y - 1, 0xFFFFFF, true);
                
                // Target Tracking UI
                if (state.getMagnetTargetTicks() > 0) {
                     int sec = state.getMagnetTargetTicks() / 20;
                     int tColor = sec < 10 ? 0xFFEF9A9A : 0xFF80CBC4;
                     guiGraphics.drawString(font, "Target Lock [" + sec + "s]", x, y + 10, tColor, true);
                     
                     // Pile Bunker Indicator
                     boolean gaugeReady = state.getMagnetGauge() >= 80;
                     boolean burstReady = state.getMagnetGauge() >= 50;
                     
                     if (gaugeReady) {
                         // Check distance if possible (client side entity)
                         boolean inRange = false;
                         if (net.minecraft.client.Minecraft.getInstance().player != null) {
                             net.minecraft.world.entity.Entity target = net.minecraft.client.Minecraft.getInstance().player.level().getEntity(state.getMagnetTargetId());
                             if (target != null && target.distanceToSqr(net.minecraft.client.Minecraft.getInstance().player) < 25.0) { // 5 blocks margin
                                 inRange = true;
                             }
                         }
                         
                         if (inRange) {
                             guiGraphics.drawString(font, "PILE BUNKER READY!", x + 80, y + 10, 0xFFFF5252, true);
                         } else {
                             guiGraphics.drawString(font, "Pile Bunker (Get Closer)", x + 80, y + 10, 0xFFFFA726, true);
                         }
                     } else if (burstReady) {
                         guiGraphics.drawString(font, "Burst Ready", x + 80, y + 10, 0xFFFFD54F, true);
                     }
                }
            }
            case "accel_axe" -> {
                drawGauge(guiGraphics, x, y, barWidth, barHeight, state.getAccelFuel() / 100.0f, 0xFFFF7043);
                guiGraphics.drawString(font, "Fuel", x + barWidth + 6, y - 1, 0xFFFFFF, true);
            }
            case "bow" -> {
                drawGauge(guiGraphics, x, y, barWidth, barHeight, state.getBowCharge(), 0xFF8BC34A);
                guiGraphics.drawString(font, "Coating: " + coatingLabel(state.getBowCoating()), x + barWidth + 6, y - 1, 0xFFFFFF, true);
            }
            default -> {
            }
        }
    }

    private static void drawGauge(GuiGraphics guiGraphics, int x, int y, int width, int height, float ratio, int color) {
        float clamped = Math.max(0.0f, Math.min(1.0f, ratio));
        int fillWidth = Math.round(clamped * width);
        guiGraphics.fill(x, y, x + width, y + height, 0xAA000000);
        guiGraphics.fill(x, y, x + fillWidth, y + height, color);
    }

    private static void renderSpiritBlade(GuiGraphics guiGraphics, int x, int y, int width, int height, int level, float decayRatio) {
        int segment = Math.max(1, width / 3);
        int filled = Math.max(0, Math.min(3, level));
        int white = 0xFFE0E0E0;
        int yellow = 0xFFFFF176;
        int red = 0xFFEF5350;
        int empty = 0xAA000000;
        for (int i = 0; i < 3; i++) {
            int color = empty;
            if (i < filled) {
                color = switch (i) {
                    case 0 -> white;
                    case 1 -> yellow;
                    default -> red;
                };
            }
            int sx = x + (i * segment);
            guiGraphics.fill(sx, y, sx + segment - 1, y + height, empty);
            if (i < filled) {
                int fillWidth = segment - 1;
                if (i == filled - 1 && decayRatio >= 0.0f && decayRatio < 1.0f) {
                    fillWidth = Math.max(1, Math.round((segment - 1) * decayRatio));
                }
                guiGraphics.fill(sx, y, sx + fillWidth, y + height, color);
            }
        }
    }

    private static float resolveSpiritDecayRatio(int level, int ticks) {
        if (level <= 0) {
            return 0.0f;
        }
        int max = switch (Math.max(1, Math.min(3, level))) {
            case 1 -> 900;
            case 2 -> 700;
            default -> 500;
        };
        return Math.max(0.0f, Math.min(1.0f, ticks / (float) max));
    }

    @SuppressWarnings("null")
    private static void renderHornScore(GuiGraphics guiGraphics, Font font, int x, int y, int a, int b, int c, int d, int e) {
        guiGraphics.drawString(font, "Score", x, y, 0xFFFFFF, true);
        int startX = x + 36;
        int size = 6;
        int gap = 3;
        renderNoteSquare(guiGraphics, startX, y, size, a);
        renderNoteSquare(guiGraphics, startX + size + gap, y, size, b);
        renderNoteSquare(guiGraphics, startX + (size + gap) * 2, y, size, c);
        renderNoteSquare(guiGraphics, startX + (size + gap) * 3, y, size, d);
        renderNoteSquare(guiGraphics, startX + (size + gap) * 4, y, size, e);
    }

    private static void renderNoteSquare(GuiGraphics guiGraphics, int x, int y, int size, int note) {
        int color = switch (note) {
            case 1 -> 0xFF42A5F5;
            case 2 -> 0xFFEF5350;
            case 3 -> 0xFF66BB6A;
            default -> 0xAA000000;
        };
        guiGraphics.fill(x, y, x + size, y + size, color);
    }

    private static String coatingLabel(int coating) {
        return switch (coating) {
            case 1 -> "Poison";
            case 2 -> "Para";
            case 3 -> "Sleep";
            default -> "None";
        };
    }

    private static int melodyIdAt(PlayerWeaponState state, int index) {
        if (state.getHornMelodyCount() <= 0) {
            return 0;
        }
        int clamped = Math.max(0, Math.min(index, state.getHornMelodyCount() - 1));
        return switch (clamped) {
            case 0 -> state.getHornMelodyA();
            case 1 -> state.getHornMelodyB();
            default -> state.getHornMelodyC();
        };
    }

    private static String melodyName(WeaponData data, int melodyId) {
        if (melodyId <= 0) {
            return "-";
        }
        if (data != null && data.getJson().has("songs") && data.getJson().get("songs").isJsonArray()) {
            var songs = data.getJson().getAsJsonArray("songs");
            int idx = melodyId - 1;
            if (idx >= 0 && idx < songs.size()) {
                var song = songs.get(idx).getAsJsonObject();
                if (song.has("id")) {
                    return prettyTitle(song.get("id").getAsString());
                }
            }
        }
        return "Melody " + melodyId;
    }

    private static String prettyTitle(String id) {
        if (id == null || id.isBlank()) {
            return "-";
        }
        String[] parts = id.replace('-', '_').split("_");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isBlank()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1));
            }
        }
        return out.length() == 0 ? "-" : out.toString();
    }

    private static void renderPlayerBars(GuiGraphics guiGraphics, int x, int y, Player player, PlayerWeaponState state) {
        int barWidth = 90;
        int hpHeight = 6;
        int staminaHeight = 4;

        float maxHp = Math.max(1.0f, player.getMaxHealth());
        float hp = Math.max(0.0f, Math.min(maxHp, player.getHealth()));
        float hpRatio = hp / maxHp;
        int hpFill = Math.round(barWidth * hpRatio);
        guiGraphics.fill(x, y, x + barWidth, y + hpHeight, 0xAA000000);
        guiGraphics.fill(x, y, x + hpFill, y + hpHeight, 0xFFE53935);

        float absorption = player.getAbsorptionAmount();
        if (absorption > 0.0f) {
            float absorptionRatio = Math.min(1.0f, absorption / maxHp);
            int absorbWidth = Math.round(barWidth * absorptionRatio);
            int start = Math.min(x + barWidth, x + hpFill);
            int end = Math.min(x + barWidth, start + absorbWidth);
            if (end > start) {
                guiGraphics.fill(start, y, end, y + hpHeight, 0xFFFFD54F);
            }
        }

        int staminaY = y + hpHeight + 2;
        if (state != null) {
            float maxStamina = Math.max(1.0f, state.getMaxStamina());
            float stamina = Math.max(0.0f, Math.min(maxStamina, state.getStamina()));
            int staminaFill = Math.round(barWidth * (stamina / maxStamina));
            guiGraphics.fill(x, staminaY, x + barWidth, staminaY + staminaHeight, 0xAA000000);
            guiGraphics.fill(x, staminaY, x + staminaFill, staminaY + staminaHeight, 0xFFEFEB3B);
        }
    }

    private static void renderSharpnessBlade(GuiGraphics guiGraphics, int x, int y, int sharpness, ItemStack stack) {
        int bladeWidth = 70;
        int bladeHeight = 4;
        int tipWidth = 6;
        float ratio = Math.max(0.0f, Math.min(1.0f, sharpness / (float) MAX_SHARPNESS));
        int fill = Math.round(bladeWidth * ratio);
        int color = sharpnessColor(sharpness, stack);

        guiGraphics.fill(x, y, x + bladeWidth + tipWidth, y + bladeHeight, 0xAA000000);
        guiGraphics.fill(x, y, x + fill, y + bladeHeight, color);
        if (fill < bladeWidth) {
            int tipStart = x + bladeWidth;
            int tipEnd = tipStart + tipWidth;
            guiGraphics.fill(tipStart, y, tipEnd, y + bladeHeight, 0x66000000);
        } else {
            guiGraphics.fill(x + bladeWidth, y, x + bladeWidth + tipWidth, y + bladeHeight, color);
        }
    }

    @SuppressWarnings("null")
    private static void renderEffectIcons(GuiGraphics guiGraphics, Minecraft minecraft, Player player, int x, int y) {
        if (player == null || minecraft == null || player.getActiveEffects().isEmpty()) {
            return;
        }
        int size = 9;
        int gap = 2;
        int max = 5;
        int index = 0;
        var textures = minecraft.getMobEffectTextures();
        for (var instance : player.getActiveEffects()) {
            if (index >= max) {
                break;
            }
            if (instance == null || !instance.isVisible() || instance.getEffect() == null) {
                continue;
            }
            TextureAtlasSprite sprite = textures.get(instance.getEffect());
            if (sprite == null) {
                continue;
            }
            int drawX = x + index * (size + gap);
            guiGraphics.blit(drawX, y, 0, size, size, sprite);
            index++;
        }
    }

    @SuppressWarnings("null")
    private static void renderAttackHud(GuiGraphics guiGraphics, Font font, int width, Player player, PlayerWeaponState state, PlayerCombatState combatState) {
        int xRight = width - 10;
        int yTop = 10;
        String weaponKey = ClientKeybinds.WEAPON_ACTION.getTranslatedKeyMessage().getString();
        String altKey = ClientKeybinds.WEAPON_ACTION_ALT.getTranslatedKeyMessage().getString();
        String specialKey = ClientKeybinds.SPECIAL_ACTION.getTranslatedKeyMessage().getString();
        List<String> hints = new ArrayList<>();

        String weaponLine = "Weapon: " + weaponKey;
        String altLine = "Alt: " + altKey;
        String specialLine = "Special: " + specialKey;
        String specialCombineLine = "Special Combine: Shift + " + specialKey;
        String altCombineLine = null;
        boolean followUpReady = false;

        if (player != null && state != null && player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider) {
            String weaponId = weaponIdProvider.getWeaponId();
            if ("longsword".equals(weaponId)) {
                boolean sheathe = state.isLongSwordSpecialSheathe();
                String weaponAction = "Thrust + Rising Slash";
                String altAction = state.getSpiritLevel() >= 3 ? "Spirit Thrust" : "Overhead Combo";
                String specialAction = "Spirit Thrust";
                String specialShift = sheathe ? "Cancel Sheathe" : "Special Sheathe";
                if (combatState != null && "spirit_helm_breaker".equals(combatState.getActionKey())) {
                    altAction = "Spirit Release Slash";
                }
                weaponLine = "Weapon: " + weaponKey + " (" + weaponAction + ")";
                altLine = "Alt: " + altKey + " (" + altAction + ")";
                specialLine = "Special: " + specialKey + " (" + specialAction + ")";
                specialCombineLine = "Special+Shift: " + specialKey + " (" + specialShift + ")";
                altCombineLine = "Alt+Shift: " + altKey + " (Fade Slash)";
                followUpReady = state.getLongSwordAltComboTicks() > 0;
            } else if ("magnet_spike".equals(weaponId)) {
                String mode = state.isMagnetSpikeImpactMode() ? "Impact" : "Cut";

                weaponLine = "Weapon: " + weaponKey + " (Magnet Tag)";
                altLine = "Alt: " + altKey + " (Magnetic Approach)";
                specialLine = "Special: " + specialKey + " (Toggle Mode: " + mode + ")";
                specialCombineLine = "Special+Shift: " + specialKey + " (Magnet Burst)";
                altCombineLine = "Alt+G: " + altKey + " (Magnetic Repel)";
            } else if ("hunting_horn".equals(weaponId)) {
                weaponLine = "Weapon: LMB (Note 1)";
                altLine = "Alt: RMB (Note 2)";
                specialLine = "Special: " + specialKey + " (Recital)";
                specialCombineLine = "Special+Shift: " + specialKey + " (Dance)";
                altCombineLine = "LMB+RMB (Note 3)";
            } else if ("insect_glaive".equals(weaponId)) {
                weaponLine = "Weapon: LMB (Rising Slash)  |  Combo: LMB > LMB > LMB > Alt";
                altLine = "Alt: " + altKey + " (Overhead Smash / Finisher)";
                specialLine = "Special: " + specialKey + " (Vault / Kinsect Aim)";
                specialCombineLine = "Special+Shift: " + specialKey + " (Kinsect Recall)";
                altCombineLine = "Focus Mode: LMB (Kinsect Combo)";

                // Kinsect key hints and status indicators
                String kinsectLaunch = ClientKeybinds.KINSECT_LAUNCH.getTranslatedKeyMessage().getString();
                String kinsectRecall = ClientKeybinds.KINSECT_RECALL.getTranslatedKeyMessage().getString();
                String kinsectLine = "Kinsect: Launch " + kinsectLaunch + " | Recall " + kinsectRecall;
                if (state != null) {
                    if (state.getKinsectEntityId() > 0) {
                        kinsectLine += "  (Active)";
                    } else if (state.getInsectExtractTicks() > 0) {
                        kinsectLine += "  (Extracts Ready)";
                    } else {
                        kinsectLine += "  (Ready)";
                    }
                }
                // append kinsect hint as a separate alt combine line to keep layout tidy when present
                altCombineLine = altCombineLine + "  |  " + kinsectLine;
            } else if ("gunlance".equals(weaponId)) {
                weaponLine = "Weapon: RMB (Shelling | Hold: Charged | Shift: Wyrmstake)";
                altLine = "Alt: " + altKey + " (Reload | Shift: Full Reload | Quick: after attack)";
                specialLine = "Special: " + specialKey + " (Wyvern's Fire)";
                specialCombineLine = "Special+LMB+RMB (Wyvern's Fire)";
                altCombineLine = null;
            }
        }

        hints.add(weaponLine);
        hints.add(altLine);
        hints.add(specialLine);
        hints.add(specialCombineLine);
        if (altCombineLine != null) {
            hints.add(altCombineLine);
        }
        if (followUpReady) {
            hints.add("Follow-up Ready: Spirit Blade / Overhead / Crescent");
        }
        hints.add("Dodge: " + ClientKeybinds.DODGE.getTranslatedKeyMessage().getString());
        hints.add("Guard: " + ClientKeybinds.GUARD.getTranslatedKeyMessage().getString());

        int maxHintWidth = 0;
        for (String hint : hints) {
            maxHintWidth = Math.max(maxHintWidth, font.width(hint));
        }
        String currentAction = resolveCurrentActionLabel(player, state, combatState);
        int leftWidth = Math.max(font.width("Current"), font.width(currentAction));
        int xLeft = Math.max(10, xRight - maxHintWidth - leftWidth - 24);
        guiGraphics.drawString(font, "Current", xLeft, yTop, 0xFFFFFF, true);
        int currentColor = "None".equals(currentAction) ? 0xBDBDBD : 0xE0E0E0;
        guiGraphics.drawString(font, currentAction, xLeft, yTop + 10, currentColor, true);

        int y = yTop;
        drawRightAligned(guiGraphics, font, xRight, y, "Actions", 0xFFFFFF);
        for (String hint : hints) {
            y += 10;
            drawRightAligned(guiGraphics, font, xRight, y, hint, 0xE0E0E0);
        }

        if (player != null && state != null && player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider
                && "hunting_horn".equals(weaponIdProvider.getWeaponId())) {
            String hint = buildHornMelodyHint(player, state);
            if (hint != null) {
                y += 12;
                drawRightAligned(guiGraphics, font, xRight, y, "Melody Hint:", 0xFFFFFF);
                y += 10;
                for (String line : hint.split("\\n")) {
                    drawRightAligned(guiGraphics, font, xRight, y, line, 0xBDBDBD);
                    y += 10;
                }
            }
        }
    }

    private static String resolveCurrentActionLabel(Player player, PlayerWeaponState state, PlayerCombatState combatState) {
        if (combatState == null) {
            return "None";
        }
        String key = combatState.getActionKey();
        if (key == null || key.isBlank() || combatState.getActionKeyTicks() <= 0) {
            return "None";
        }

        String generic = resolveGenericActionLabel(key);
        if (generic != null) {
            return generic;
        }

        String weaponId = null;
        if (player != null && state != null && player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider) {
            weaponId = weaponIdProvider.getWeaponId();
        }

        String label = resolveActionLabelForWeapon(weaponId, key, player, state, combatState);
        if (label != null && !label.isBlank()) {
            return label;
        }
        return prettyTitle(key);
    }

    public static String resolveActionLabelForWeapon(String weaponId, String key, Player player, PlayerWeaponState state, PlayerCombatState combatState) {
        if (weaponId == null || key == null) {
            return null;
        }
        switch (weaponId) {
            case "longsword": {
                switch (key) {
                    case "spirit_blade_1": return "Spirit Blade 1";
                    case "spirit_blade_2": return "Spirit Blade 2";
                    case "spirit_blade_3": return "Spirit Blade 3";
                    case "spirit_roundslash": return "Spirit Roundslash";
                    case "overhead_slash": return "Overhead Slash";
                    case "overhead_stab": return "Overhead Slash";
                    case "crescent_slash": return "Crescent Slash";
                    case "spirit_thrust": return "Spirit Thrust";
                    case "rising_slash": return "Rising Slash";
                    case "spirit_helm_breaker": return "Spirit Helm Breaker";
                    case "spirit_release_slash": return "Spirit Release Slash";
                    case "spirit_release_slash_left": return "Spirit Release Slash";
                    case "thrust_rising_slash": return "Thrust";
                    case "special_sheathe": return "Special Sheathe";
                    case "fade_slash": return "Fade Slash";
                    case "iai_slash": return "Foresight Slash";
                    default: {
                        // fallthrough to charge/combo stage logic below
                    }
                }
                if ("spirit_charge".equals(key)) {
                    int maxCharge = WeaponDataResolver.resolveInt(player, null, "chargeMaxTicks", 40);
                    int ticks = Math.max(0, state.getChargeAttackTicks());
                    int stage = 0;
                    if (maxCharge > 0) {
                        stage = ticks >= maxCharge ? 3 : (ticks >= (maxCharge * 2 / 3) ? 2 : (ticks >= (maxCharge / 3) ? 1 : 0));
                    }
                    return stage > 0 ? "Spirit Charge " + stage + "/3" : "Spirit Charge";
                }
                if (key.startsWith("spirit_blade")) {
                    int stage = Math.min(3, Math.max(1, state.getLongSwordSpiritComboIndex() + 1));
                    return "Spirit Combo " + stage + "/4";
                }
                if ("spirit_roundslash".equals(key)) {
                    return "Spirit Combo 4/4";
                }
                if (key.startsWith("overhead") || "crescent_slash".equals(key)) {
                    int stage = Math.min(3, Math.max(1, state.getLongSwordOverheadComboIndex() + 1));
                    return "Overhead Combo " + stage + "/3";
                }
                return null;
            }
            case "gunlance": {
                switch (key) {
                    case "shell": return "Shelling";
                    case "charge_shell": return "Charged Shelling";
                    case "reload": return "Reload";
                    case "wyrmstake": return "Wyrmstake Cannon";
                    case "wyvernfire": return "Wyvern's Fire";
                    case "quick_reload": return "Quick Reload";
                    case "burst_fire": return "Burst Fire";
                    case "gunlance_lateral_thrust_1": return "Lateral Thrust I";
                    case "gunlance_lateral_thrust_2": return "Lateral Thrust II";
                    case "gunlance_lateral_thrust_3": return "Lateral Thrust III";
                    case "gunlance_rising_slash": return "Rising Slash";
                    case "gunlance_wide_sweep": return "Wide Sweep";
                    case "gunlance_overhead_smash": return "Overhead Smash";
                    case "basic_attack": return "Lateral Thrust I"; // Fallback for BC
                    default: return null;
                }
            }
            case "magnet_spike": {
                if ("magnet_burst".equals(key) && player != null && !player.onGround()) {
                    return "Magnet Burst (Launch)";
                }
                return getMagnetSpikeMoveName(key);
            }
            case "insect_glaive": {
                switch (key) {
                    case "rising_slash": return "Rising Slash";
                    case "reaping_slash": return "Reaping Slash";
                    case "double_slash": return "Double Slash";
                    case "overhead_smash": return "Overhead Smash";
                    case "basic_attack": return "Rising Slash"; // Map generic BC basic attack to IG canonical starter
                    case "leaping_slash": return "Leaping Slash";
                    case "tornado_slash": return "Tornado Slash";
                    case "strong_descending_slash": return "Strong Descending Slash";
                    case "descending_slash": return "Descending Slash";
                    case "rising_spiral_slash": return "Rising Spiral Slash";
                    case "wide_sweep": return "Wide Sweep";
                    case "vault": return "Vault";
                    case "aerial_advancing_slash": return "Jumping Advancing Slash";
                    case "aerial_slash": return "Jumping Slash";
                    case "midair_evade": return "Midair Evade";
                    default: return null;
                }
            }
            case "tonfa": {
                switch (key) {
                    case "tonfa_long": return "Long Mode";
                    case "tonfa_short": return "Short Mode";
                    case "tonfa_long_1": return "Thrust (I)";
                    case "tonfa_long_2": return "Kick & Swing (II)";
                    case "tonfa_long_3": return "Uppercut (III)";
                    case "tonfa_short_rise": return "Rising Smash";
                    case "tonfa_short_flurry": return "Aerial Flurry";
                    case "tonfa_short_1": return "Short Combo 1";
                    case "tonfa_short_2": return "Short Combo 2";
                    case "tonfa_short_3": return "Short Combo 3";
                    case "tonfa_long_ex": return "EX Finisher";
                    case "tonfa_short_ex": return "EX Finisher";
                    case "tonfa_hover": return "Hover";
                    case "tonfa_dive": return "Dive Impact";
                    case "tonfa_charge": return "Concentrated Smash";
                    case "tonfa_dash": return "Jet Dash";
                    case "tonfa_air_slash": return "Aerial Slash";
                    case "tonfa_air_slam": return "Aerial Slam";
                    case "tonfa_drill": return "Pinpoint Drill";
                    case "basic_attack": return resolveTonfaBasicAttackLabel(player, state);
                    default: return null;
                }
            }
            default:
                return null;
        }
    }

    private static String resolveGenericActionLabel(String key) {
        return switch (key) {
            case "dodge" -> "Dodge";
            case "guard" -> "Guard";
            case "sheathe" -> "Sheathe";
            case "draw_slash" -> "Draw Slash";
            case "charge_start", "charge" -> "Charge";
            case "midair_evade" -> "Midair Evade";
            case "vault" -> "Vault";
            default -> null;
        };
    }

    private static String resolveTonfaBasicAttackLabel(Player player, PlayerWeaponState state) {
        if (player == null || state == null) {
            return "Attack";
        }
        int window = WeaponDataResolver.resolveInt(player, null, "comboWindowTicks", 40);
        int lastTick = state.getTonfaComboTick();
        boolean reset = (player.tickCount - lastTick) > window;

        if (state.isTonfaShortMode()) {
            if (!player.onGround()) {
                return "Aerial Slash";
            }
            return "Rising Smash";
        }

        if (reset) {
            return "Thrust (I)";
        }

        int idx = state.getTonfaComboIndex();
        return switch (idx) {
            case 1 -> "Kick & Swing (II)";
            case 2 -> "Uppercut (III)";
            default -> "Thrust (I)";
        };
    }

    private static String buildHornMelodyHint(Player player, PlayerWeaponState state) {
        if (state.getHornNoteCount() <= 0) {
            return null;
        }
        WeaponData data = WeaponDataResolver.resolve(player);
        if (data == null) {
            return null;
        }
        var json = data.getJson();
        if (!json.has("songs") || !json.get("songs").isJsonArray()) {
            return null;
        }
        int[] notes = lastHornNotes(state, resolveNoteQueueSize(json));
        if (notes.length == 0) {
            return null;
        }
        var songs = json.getAsJsonArray("songs");
        StringBuilder out = new StringBuilder();
        int matches = 0;
        for (int i = 0; i < songs.size(); i++) {
            var song = songs.get(i).getAsJsonObject();
            if (!song.has("pattern") || !song.get("pattern").isJsonArray()) {
                continue;
            }
            int[] pattern = readPattern(song.getAsJsonArray("pattern"));
            if (pattern.length == 0 || !matchesPattern(notes, pattern)) {
                continue;
            }
            String id = song.has("id") ? song.get("id").getAsString() : "melody_" + (i + 1);
            String label = prettyTitle(id);
            if (out.length() > 0) {
                out.append("\n");
            }
            out.append(patternLabel(pattern)).append(" ").append(label);
            matches++;
            if (matches >= 3) {
                break;
            }
        }
        return matches > 0 ? out.toString() : "-";
    }

    private static int resolveNoteQueueSize(com.google.gson.JsonObject json) {
        if (json.has("noteQueueSize") && json.get("noteQueueSize").isJsonPrimitive()) {
            return Math.max(1, json.get("noteQueueSize").getAsInt());
        }
        return 5;
    }

    private static int[] lastHornNotes(PlayerWeaponState state, int max) {
        int count = Math.min(state.getHornNoteCount(), max);
        if (count <= 0) {
            return new int[0];
        }
        int[] notes = new int[count];
        int total = state.getHornNoteCount();
        int start = Math.max(0, total - count);
        for (int i = 0; i < count; i++) {
            notes[i] = hornNoteAt(state, start + i);
        }
        return notes;
    }

    private static int hornNoteAt(PlayerWeaponState state, int index) {
        return switch (index) {
            case 0 -> state.getHornNoteA();
            case 1 -> state.getHornNoteB();
            case 2 -> state.getHornNoteC();
            case 3 -> state.getHornNoteD();
            case 4 -> state.getHornNoteE();
            default -> 0;
        };
    }

    private static int[] readPattern(com.google.gson.JsonArray array) {
        int[] out = new int[array.size()];
        for (int i = 0; i < array.size(); i++) {
            out[i] = array.get(i).getAsInt();
        }
        return out;
    }

    private static boolean matchesPattern(int[] last, int[] pattern) {
        if (pattern.length == 0 || last.length < pattern.length) {
            return false;
        }
        int offset = last.length - pattern.length;
        for (int i = 0; i < pattern.length; i++) {
            if (last[offset + i] != pattern[i]) {
                return false;
            }
        }
        return true;
    }

    private static String patternLabel(int[] pattern) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < pattern.length; i++) {
            if (i > 0) {
                out.append('-');
            }
            out.append(pattern[i]);
        }
        return out.toString();
    }

    @SuppressWarnings("null")
    private static void renderKinsectDebug(GuiGraphics guiGraphics, Font font, int width, PlayerWeaponState state, int startY) {
        int xRight = width - 10;
        int y = startY;
        drawRightAligned(guiGraphics, font, xRight, y, "Kinsect", 0xFFFFFF);
        y += 10;

        int id = state.getKinsectEntityId();
        if (Minecraft.getInstance().level != null && id > 0) {
            var entity = Minecraft.getInstance().level.getEntity(id);
            if (entity instanceof KinsectEntity kinsect) {
                drawRightAligned(guiGraphics, font, xRight, y, "State: " + kinsect.getStateName(), 0xE0E0E0);
                y += 10;
                String color = switch (kinsect.getColor()) {
                    case 1 -> "Red";
                    case 2 -> "White";
                    case 3 -> "Orange";
                    default -> "None";
                };
                drawRightAligned(guiGraphics, font, xRight, y, "Color: " + color, 0xE0E0E0);
                y += 10;
                int targetId = kinsect.getTargetEntityId();
                drawRightAligned(guiGraphics, font, xRight, y, "Target: " + (targetId > 0 ? targetId : "None"), 0xE0E0E0);
                y += 10;
                if (kinsect.getTargetPos() != null) {
                    double dist = kinsect.position().distanceTo(kinsect.getTargetPos());
                    drawRightAligned(guiGraphics, font, xRight, y, String.format("Dist: %.1f", dist), 0xBDBDBD);
                    y += 10;
                }
                drawRightAligned(guiGraphics, font, xRight, y, "Launch: " + ClientKeybinds.KINSECT_LAUNCH.getTranslatedKeyMessage().getString(), 0xBDBDBD);
                y += 10;
                drawRightAligned(guiGraphics, font, xRight, y, "Recall: " + ClientKeybinds.KINSECT_RECALL.getTranslatedKeyMessage().getString(), 0xBDBDBD);
                return;
            }
        }
        drawRightAligned(guiGraphics, font, xRight, y, "State: None", 0xE0E0E0);
        y += 10;
        drawRightAligned(guiGraphics, font, xRight, y, "Launch: " + ClientKeybinds.KINSECT_LAUNCH.getTranslatedKeyMessage().getString(), 0xBDBDBD);
        y += 10;
        drawRightAligned(guiGraphics, font, xRight, y, "Recall: " + ClientKeybinds.KINSECT_RECALL.getTranslatedKeyMessage().getString(), 0xBDBDBD);
    }

    @SuppressWarnings("null")
    private static void drawRightAligned(GuiGraphics guiGraphics, Font font, int xRight, int y, String text, int color) {
        int width = font.width(text);
        guiGraphics.drawString(font, text, xRight - width, y, color, true);
    }

    private static String getMagnetSpikeMoveName(String key) {
        if (key == null || key.isEmpty()) return "Neutral";
        switch (key) {
            case "magnet_slash_i": return "Slash I";
            case "magnet_slash_ii": return "Slash II";
            case "magnet_slash_iii": return "Slash III";
            case "magnet_cleave": return "Magnet Cleave";
            case "magnet_roundslash": return "Roundslash";
            case "magnet_fade_slash": return "Fade Slash";
            case "magnet_smash_i": return "Smash I";
            case "magnet_smash_ii": return "Smash II";
            case "magnet_crush": return "Crushing Blow";
            case "magnet_suplex": return "Suplex";
            case "magnet_counter_bash": return "Counter Bash";
            case "magnet_tag": return "Magnet Tag";
            case "magnet_burst": return "Magnet Burst";
            case "magnet_pile_bunker_drill": return "Pile Bunker";
            case "impact_charge_i": return "Impact Charge I";
            case "impact_charge_ii": return "Impact Charge II";
            case "impact_charge_iii": return "Impact Charge III";
            case "magnet_zip": return "Magnet Zip";
            case "magnet_approach": return "Magnetic Approach";
            case "magnet_repel": return "Magnetic Repel";
            case "magnet_impact": return "Mode Switch (Impact)";
            case "magnet_cut": return "Mode Switch (Cut)";
            case "impact_bash": return "Impact Bash";
            case "cut_heavy_slash": return "Heavy Slash";
            case "None": return "Neutral";
            default:
                // Fallback: capitalize
                String clean = key.replace("_", " ");
                char[] chars = clean.toCharArray();
                boolean upper = true;
                for (int i = 0; i < chars.length; i++) {
                    if (chars[i] == ' ') {
                        upper = true;
                    } else if (upper) {
                        chars[i] = Character.toUpperCase(chars[i]);
                        upper = false;
                    }
                }
                return new String(chars);
        }
    }

    private WeaponHudOverlay() {
    }
}