package org.example.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.MHWeaponsMod;
import org.example.client.compat.MagnetSpikeZipClientAnimationTracker;
import org.example.client.fx.DamageNumberClientTracker;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.combat.weapon.WeaponActionType;
import org.example.common.network.ModNetwork;
import org.example.common.network.packet.FocusModeC2SPacket;
import org.example.common.network.packet.KinsectLaunchC2SPacket;
import org.example.common.network.packet.WeaponActionC2SPacket;
import org.example.common.data.WeaponDataResolver;
import org.example.common.util.CapabilityUtil;
import org.example.item.WeaponIdProvider;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = MHWeaponsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private static final Logger LOGGER = LogManager.getLogger("MHWeaponsMod/ClientInput");
    private static boolean guardDown;
    private static boolean attackDown;
    private static int attackHoldTicks;
    private static boolean chargeSent;
    private static final int CHARGE_HOLD_TICKS = 12;
    private static final int GUNLANCE_CHARGE_HOLD_TICKS = 18;
    private static int longSwordSpiritClickIndex;
    private static int longSwordSpiritClickTick;
    private static int longSwordAltActionTick;
    private static int longSwordAltActionIndex;
    private static int longSwordThrustComboIndex;
    private static int longSwordThrustComboTick;
    private static boolean longSwordHelmBreakerAltSent;
    private static int magnetChargeStage;

    private static boolean hornLeftDown;
    private static boolean hornRightDown;
    private static boolean glLeftDown;
    private static boolean glRightDown;

    @SubscribeEvent
    public static void onComputeFov(net.minecraftforge.client.event.ComputeFovModifierEvent event) {
        if (event.getPlayer() == null) return;
        PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(event.getPlayer());
        if (combatState != null && "wyvernfire".equals(combatState.getActionKey())) {
             event.setNewFovModifier(event.getNewFovModifier() * 0.85f); // Zoom in
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        String currentWeaponId = resolveWeaponId(Minecraft.getInstance());
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        net.minecraft.world.entity.player.Player localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) {
            return;
        }

        PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(localPlayer);
        PlayerWeaponState weaponState = CapabilityUtil.getPlayerWeaponState(localPlayer);

        boolean isAnimating = org.example.common.compat.BetterCombatAnimationBridge.isAttackAnimationActive(localPlayer);

        if (ClientKeybinds.FOCUS_MODE.consumeClick()) {
            FocusModeClient.toggle();
            ModNetwork.CHANNEL.sendToServer(new FocusModeC2SPacket(FocusModeClient.isEnabled()));
        }

        if (ClientKeybinds.DODGE.consumeClick() && !isAnimating) {
            ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.DODGE, true));
        }

        if (ClientKeybinds.SHEATHE.consumeClick() && !isAnimating) {
            ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.SHEATHE, true));
        }

        if (ClientKeybinds.SPECIAL_ACTION.consumeClick()) {
            if (!"gunlance".equals(currentWeaponId)) {
                if (!isAnimating) {
                    ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.SPECIAL, true));
                }
            }
        }

        if (Minecraft.getInstance().options.keyJump.consumeClick()) {
            if (!localPlayer.onGround()
                    && !localPlayer.getAbilities().flying
                    && !isAnimating) {
                 ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.DOUBLE_JUMP, true));
            }
        }
        
        boolean skipKeyboardWeaponActions = "tonfa".equals(currentWeaponId)
                || "magnet_spike".equals(currentWeaponId)
                || "insect_glaive".equals(currentWeaponId)
                || "gunlance".equals(currentWeaponId); // Skip default WEAPON handler for GL to avoid conflicts

        boolean weaponClick = ClientKeybinds.WEAPON_ACTION.consumeClick();
        if (weaponClick && !skipKeyboardWeaponActions && !isAnimating) {
            ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.WEAPON, true));
            applyLongSwordThrustHint(Minecraft.getInstance());
        }

        if (weaponClick && "gunlance".equals(currentWeaponId) && !isAnimating) {
             // Manual handler for GL if we want specific key logic separate from default
             // But if mouse right click is also triggering this, we might be double-firing or misinterpreting
             // Currently WEAPON_ACTION defaults to 'X'. 
             // If the user presses 'X', it sends WEAPON packet.
             ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.WEAPON, true));
        }

        boolean altClick = ClientKeybinds.WEAPON_ACTION_ALT.consumeClick();
        if (altClick && !skipKeyboardWeaponActions && !isAnimating) {
            ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.WEAPON_ALT, true));
            applyLongSwordActionHint(Minecraft.getInstance(), true);
        }

        if ("longsword".equals(currentWeaponId)
                && combatState != null
                && ("spirit_helm_breaker".equals(combatState.getActionKey())
                    || "helm_breaker_followup".equals(combatState.getActionKey()))) {
            boolean altDown = ClientKeybinds.WEAPON_ACTION_ALT.isDown();
            if (altDown && !longSwordHelmBreakerAltSent) {
                ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.WEAPON_ALT, true));
                longSwordHelmBreakerAltSent = true;
            } else if (!altDown) {
                longSwordHelmBreakerAltSent = false;
            }
        } else {
            longSwordHelmBreakerAltSent = false;
        }

        // Allow WEAPON_ALT for Gunlance even though other keyboard weapon actions are skipped
        if (altClick && "gunlance".equals(currentWeaponId) && !isAnimating) {
            ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.WEAPON_ALT, true));
        }

        // Allow WEAPON_ALT for Insect Glaive even though other keyboard weapon actions are skipped
        if (altClick && "insect_glaive".equals(currentWeaponId) && !isAnimating) {
            ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.WEAPON_ALT, true));
        }

        if (ClientKeybinds.KINSECT_LAUNCH.consumeClick() && !isAnimating) {
            var hit = Minecraft.getInstance().hitResult;
            int targetId = -1;
            net.minecraft.world.phys.Vec3 targetPos = null;
            if (hit != null) {
                targetPos = hit.getLocation();
                if (hit instanceof net.minecraft.world.phys.EntityHitResult entityHit) {
                    targetId = entityHit.getEntity().getId();
                }
            }
            ModNetwork.CHANNEL.sendToServer(new KinsectLaunchC2SPacket(targetId, targetPos));
        }

        if (ClientKeybinds.KINSECT_RECALL.consumeClick() && !isAnimating) {
            ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.KINSECT_RECALL, true));
        }

        boolean magnetChargeHold = "magnet_spike".equals(currentWeaponId)
                && weaponState != null
                && weaponState.isMagnetSpikeImpactMode();
        boolean nowAttackDown = ("longsword".equals(currentWeaponId) || magnetChargeHold)
            ? Minecraft.getInstance().mouseHandler.isLeftPressed()
            : Minecraft.getInstance().options.keyAttack.isDown();
        if (nowAttackDown && !attackDown) {
            if (!isAnimating) {
                if (!"longsword".equals(currentWeaponId)) {
                    applyLongSwordSpiritClickHint(Minecraft.getInstance());
                }
                applyGunlanceAttackHint(Minecraft.getInstance());
                if ("insect_glaive".equals(currentWeaponId)) {
                    ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.WEAPON, true));
                }
            }
        }
        boolean releasedAttack = attackDown && !nowAttackDown;
        attackDown = nowAttackDown;

        if (!isAnimating) {
            handleHuntingHornMouseInput(Minecraft.getInstance());
            handleGunlanceMouseInput(Minecraft.getInstance());
        }

        if ("longsword".equals(currentWeaponId) && nowAttackDown) {
            Minecraft.getInstance().options.keyAttack.setDown(false);
        }
        if (magnetChargeHold && nowAttackDown && (chargeSent || (weaponState != null && weaponState.isChargingAttack()))) {
            Minecraft.getInstance().options.keyAttack.setDown(false);
        }

        if (nowAttackDown && isChargeWeapon(Minecraft.getInstance().player)) {
            if ("longsword".equals(currentWeaponId)) {
                attackHoldTicks++;
                if (!chargeSent) {
                    chargeSent = true;
                    ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.CHARGE, true));
                }
                if (combatState != null) {
                    int maxCharge = WeaponDataResolver.resolveInt(localPlayer, null, "chargeMaxTicks", 40);
                    int stage = resolveLongSwordChargeStage(attackHoldTicks, maxCharge);
                    String key = switch (stage) {
                        case 1 -> "spirit_blade_1";
                        case 2 -> "spirit_blade_2";
                        case 3 -> "spirit_blade_3";
                        case 4 -> "spirit_roundslash";
                        default -> "spirit_charge";
                    };
                    combatState.setActionKey(key);
                    combatState.setActionKeyTicks(2);
                }
            } else if ("magnet_spike".equals(currentWeaponId)
                    && weaponState != null
                    && weaponState.isMagnetSpikeImpactMode()) {
                attackHoldTicks++;
                int holdTicks = CHARGE_HOLD_TICKS;
                if (!chargeSent && attackHoldTicks >= holdTicks) {
                    chargeSent = true;
                    ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.CHARGE, true));
                }
                if (combatState != null && chargeSent) {
                    int maxCharge = WeaponDataResolver.resolveInt(localPlayer, null, "chargeMaxTicks", 40);
                    int stage = resolveMagnetChargeStage(attackHoldTicks, maxCharge);
                    String key = switch (stage) {
                        case 1 -> "impact_charge_i";
                        case 2 -> "impact_charge_ii";
                        case 3 -> "impact_charge_iii";
                        default -> "charge_start";
                    };
                    combatState.setActionKey(key);
                    combatState.setActionKeyTicks(6);
                    if (stage != magnetChargeStage && localPlayer.level() != null) {
                        for (int i = 0; i < 6 + (stage * 2); i++) {
                            double ox = (localPlayer.getRandom().nextDouble() - 0.5) * 0.6;
                            double oy = localPlayer.getRandom().nextDouble() * 0.6 + 0.8;
                            double oz = (localPlayer.getRandom().nextDouble() - 0.5) * 0.6;
                            localPlayer.level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                                    localPlayer.getX() + ox, localPlayer.getY() + oy, localPlayer.getZ() + oz,
                                    0.0, 0.01, 0.0);
                        }
                        magnetChargeStage = stage;
                    }
                }
            } else if (chargeSent || !isAnimating) {
                attackHoldTicks++;
                int holdTicks = "gunlance".equals(currentWeaponId) ? GUNLANCE_CHARGE_HOLD_TICKS : CHARGE_HOLD_TICKS;
                if (!chargeSent && attackHoldTicks >= holdTicks) {
                    chargeSent = true;
                    ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.CHARGE, true));
                }
            }
        } else {
            if (magnetChargeHold && releasedAttack && !chargeSent && attackHoldTicks > 0 && !isAnimating) {
                ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.WEAPON, true));
            }
            if ("longsword".equals(currentWeaponId) && releasedAttack) {
                ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.CHARGE, false));
                chargeSent = false;
                attackHoldTicks = 0;
            }
            if (chargeSent) {
                ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.CHARGE, false));
                chargeSent = false;
            } else if (attackHoldTicks > 0) {
                 if (!"insect_glaive".equals(currentWeaponId)
                         && !"longsword".equals(currentWeaponId)
                         && !isAnimating) {
                      if ("tonfa".equals(currentWeaponId)) {
                         LOGGER.info("Tonfa LMB release -> send WEAPON (isAnimating={}, holdTicks={})", isAnimating, attackHoldTicks);
                      }
                      ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.WEAPON, true));
                  } else if ("tonfa".equals(currentWeaponId)) {
                      LOGGER.info("Tonfa LMB release blocked (isAnimating={}, holdTicks={})", isAnimating, attackHoldTicks);
                 }
            }
            attackHoldTicks = 0;
            magnetChargeStage = 0;
        }

        boolean guardPressed = ClientKeybinds.GUARD.isDown();
        if (guardPressed != guardDown && !isAnimating) {
            guardDown = guardPressed;
            ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.GUARD, guardPressed));
        }

        FocusModeClient.updateHighlights();
        MagnetSpikeZipClientAnimationTracker.tick();
        DamageNumberClientTracker.tick();
    }

    @SuppressWarnings("null")
    private static void handleHuntingHornMouseInput(Minecraft mc) {
        if (mc == null || mc.player == null) {
            hornLeftDown = false;
            hornRightDown = false;
            return;
        }
        if (!(mc.player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider)
                || !"hunting_horn".equals(weaponIdProvider.getWeaponId())) {
            hornLeftDown = false;
            hornRightDown = false;
            return;
        }
        boolean leftDown = mc.options.keyAttack.isDown();
        boolean rightDown = mc.options.keyUse.isDown();

        if (leftDown && rightDown && !(hornLeftDown && hornRightDown)) {
            ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.HORN_NOTE_BOTH, true));
        } else if (leftDown && !rightDown && !hornLeftDown) {
            ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.WEAPON, true));
        } else if (rightDown && !leftDown && !hornRightDown) {
            ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.WEAPON_ALT, true));
            InputConstants.Key attackKey = mc.options.keyAttack.getKey();
            if (attackKey != null) {
                KeyMapping.click(attackKey);
            }
        }

        hornLeftDown = leftDown;
        hornRightDown = rightDown;
    }

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onUseKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        if (org.example.common.compat.BetterCombatAnimationBridge.isAttackAnimationActive(mc.player)) {
            event.setCanceled(true);
            return;
        }

        if (event.isAttack()) {
            if (isChargeWeapon(mc.player)) {
                event.setCanceled(true);
                return;
            }
        }

        if (!event.isUseItem()) {
            return;
        }
        
        if (mc.player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider) {
            String weaponId = weaponIdProvider.getWeaponId();
            if ("hunting_horn".equals(weaponId)) {
                event.setCanceled(true);
                return;
            }
            if ("gunlance".equals(weaponId)) {
                return; // Allow default item use for shelling/WSC without forcing attacks
            }
            if ("insect_glaive".equals(weaponId)) {
                // RMB should be WEAPON_ALT (Wide Sweep / Overhead Smash), without triggering LMB
                
                // Update local state for Better Combat prediction
                PlayerWeaponState state = CapabilityUtil.getPlayerWeaponState(mc.player);
                PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(mc.player);
                if (state != null && combatState != null) {
                    int window = 28; // Matching the JSON comboWindowTicks
                    boolean overheadReady = (mc.player.tickCount - state.getInsectComboTick()) <= window
                            && state.getInsectComboIndex() >= 2;
                    String action = overheadReady ? "overhead_smash" : "wide_sweep";
                    combatState.setActionKey(action);
                    combatState.setActionKeyTicks(8);
                }

                ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.WEAPON_ALT, true));
                event.setCanceled(true);
                return;
            }
            if ("magnet_spike".equals(weaponId)) {
                boolean repel = ClientKeybinds.WEAPON_ACTION_ALT.isDown();
                ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(
                        repel ? WeaponActionType.WEAPON_ALT_REPEL : WeaponActionType.WEAPON_ALT, true));
                event.setCanceled(true);
                return;
            }
            if ("longsword".equals(weaponId)) {
                ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.WEAPON_ALT, true));
                applyLongSwordActionHint(mc, true);
                event.setCanceled(true);
                return;
            }
        }
        if (!(mc.player.getMainHandItem().getItem() instanceof org.example.item.GeoWeaponItem)) {
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.WEAPON_ALT, true));
        applyLongSwordActionHint(mc, true);
        InputConstants.Key attackKey = mc.options.keyAttack.getKey();
        if (attackKey != null) {
            KeyMapping.click(attackKey);
        }
        event.setCanceled(true);
    }


    @SuppressWarnings("null")
    private static void applyLongSwordActionHint(Minecraft mc, boolean altAction) {
        if (mc == null || mc.player == null || !altAction) {
            return;
        }
        if (!(mc.player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider)) {
            return;
        }
        if (!"longsword".equals(weaponIdProvider.getWeaponId())) {
            return;
        }
        PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(mc.player);
        PlayerWeaponState weaponState = CapabilityUtil.getPlayerWeaponState(mc.player);
        if (combatState == null || weaponState == null) {
            return;
        }
        int now = mc.player.tickCount;
        int window = 60; // Slightly lenient window for valid combo hint
        int nextIndex = (now - longSwordAltActionTick) > window ? 0 : (longSwordAltActionIndex + 1) % 3;
        longSwordAltActionIndex = nextIndex;
        longSwordAltActionTick = now;
        String key = switch (nextIndex) {
            case 0 -> "overhead_slash";
            case 1 -> "overhead_stab";
            case 2 -> "rising_slash";
            default -> "overhead_slash";
        };
        combatState.setActionKey(key);
        combatState.setActionKeyTicks(4);
    }

    @SuppressWarnings("null")
    private static void applyLongSwordThrustHint(Minecraft mc) {
        if (mc == null || mc.player == null) {
            return;
        }
        if (!(mc.player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider)) {
            return;
        }
        if (!"longsword".equals(weaponIdProvider.getWeaponId())) {
            return;
        }
        PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(mc.player);
        if (combatState == null) {
            return;
        }
        int now = mc.player.tickCount;
        int window = 20; 
        int nextIndex = (now - longSwordThrustComboTick) > window ? 0 : (longSwordThrustComboIndex + 1) % 2;
        longSwordThrustComboIndex = nextIndex;
        longSwordThrustComboTick = now;
        
        // Update hint to match the combo
        combatState.setActionKey("thrust_rising_slash");
        combatState.setActionKeyTicks(4);
    }

    @SuppressWarnings("null")
    private static void applyLongSwordSpiritClickHint(Minecraft mc) {
        if (mc == null || mc.player == null) {
            return;
        }
        if (!(mc.player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider)) {
            return;
        }
        if (!"longsword".equals(weaponIdProvider.getWeaponId())) {
            return;
        }
        PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(mc.player);
        if (combatState == null) {
            return;
        }
        int now = mc.player.tickCount;
        int window = 60;
        int nextIndex = (now - longSwordSpiritClickTick) > window ? 0 : (longSwordSpiritClickIndex + 1) % 4;
        longSwordSpiritClickIndex = nextIndex;
        longSwordSpiritClickTick = now;
        String actionKey = switch (nextIndex) {
            case 0 -> "spirit_blade_1";
            case 1 -> "spirit_blade_2";
            case 2 -> "spirit_blade_3";
            default -> "spirit_roundslash";
        };
        combatState.setActionKey(actionKey);
        combatState.setActionKeyTicks(6);
    }

    private static boolean isChargeWeapon(net.minecraft.world.entity.player.Player player) {
        if (player == null) {
            return false;
        }
        var item = player.getMainHandItem().getItem();
        if (!(item instanceof org.example.item.WeaponIdProvider weaponIdProvider)) {
            return false;
        }
        String weaponId = weaponIdProvider.getWeaponId();
        return "great_sword".equals(weaponId)
                || "hammer".equals(weaponId)
                || "longsword".equals(weaponId)
                || "lance".equals(weaponId)
                || "gunlance".equals(weaponId)
                || "sword_and_shield".equals(weaponId)
                || "charge_blade".equals(weaponId)
            || "insect_glaive".equals(weaponId)
                || "tonfa".equals(weaponId)
                || "magnet_spike".equals(weaponId)
                || "accel_axe".equals(weaponId)
                || "switch_axe".equals(weaponId);
    }

    private static int resolveLongSwordChargeStage(int chargeTicks, int maxCharge) {
        if (maxCharge <= 0) {
            return 0;
        }
        if (chargeTicks >= maxCharge) {
            return 4;
        }
        if (chargeTicks >= (maxCharge * 2 / 3)) {
            return 3;
        }
        if (chargeTicks >= (maxCharge / 3)) {
            return 2;
        }
        return 1;
    }

    private static int resolveMagnetChargeStage(int chargeTicks, int maxCharge) {
        if (maxCharge <= 0) {
            return 0;
        }
        if (chargeTicks >= maxCharge) {
            return 3;
        }
        if (chargeTicks >= (maxCharge * 2 / 3)) {
            return 2;
        }
        if (chargeTicks >= (maxCharge / 3)) {
            return 1;
        }
        return 0;
    }

    @SuppressWarnings("null")
    private static String resolveWeaponId(Minecraft mc) {
        if (mc == null || mc.player == null) {
            return null;
        }
        if (mc.player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider) {
            return weaponIdProvider.getWeaponId();
        }
        return null;
    }

    @SuppressWarnings("null")
    private static void applyGunlanceAttackHint(Minecraft mc) {
        if (mc == null || mc.player == null) {
            return;
        }
        if (!(mc.player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider)) {
            return;
        }
        if (!"gunlance".equals(weaponIdProvider.getWeaponId())) {
            return;
        }
        PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(mc.player);
        if (combatState == null) {
            return;
        }
        
        // Use BetterCombat state for accurate combo index
        int comboCount = org.example.common.compat.BetterCombatAnimationBridge.resolveCurrentComboIndex(mc.player, true);
        int next = comboCount % 3; // 3-hit combo

        String actionKey = switch (next) {
            case 0 -> "gunlance_lateral_thrust_1";
            case 1 -> "gunlance_lateral_thrust_2";
            case 2 -> "gunlance_wide_sweep";
            default -> "gunlance_lateral_thrust_1";
        };
        combatState.setActionKey(actionKey);
        combatState.setActionKeyTicks(60); // Persist longer for HUD visibility during animation
    }

    @SuppressWarnings("null")
    private static void handleGunlanceMouseInput(Minecraft mc) {
        if (mc == null || mc.player == null) {
            glLeftDown = false;
            glRightDown = false;
            return;
        }
        if (!(mc.player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider)
                || !"gunlance".equals(weaponIdProvider.getWeaponId())) {
            glLeftDown = false;
            glRightDown = false;
            return;
        }
        
        boolean leftDown = mc.options.keyAttack.isDown();
        boolean rightDown = mc.options.keyUse.isDown();
        boolean specialDown = ClientKeybinds.SPECIAL_ACTION.isDown();

        // Wyvern's Fire: Special (Hold) + Left + Right — only send when gauge exists and not already charging
        if (specialDown && leftDown && rightDown && !(glLeftDown && glRightDown)) {
            PlayerWeaponState state = CapabilityUtil.getPlayerWeaponState(mc.player);
            if (state != null && state.getGunlanceWyvernFireGauge() >= 1.0f && !state.isGunlanceCharging()) {
                ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.SPECIAL, true));
            } else {
                // Provide brief feedback when insufficient gauge
                if (state != null && state.getGunlanceWyvernFireGauge() < 1.0f) {
                    mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Not enough Wyvern's Fire"), true);
                }
            }
        }

        // Manual Wyrmstake: Shift + Right Click
        // Only trigger if Special is NOT held, to avoid conflict with WF
        if (!specialDown && mc.player.isShiftKeyDown() && rightDown && !glRightDown) {
             ModNetwork.CHANNEL.sendToServer(new WeaponActionC2SPacket(WeaponActionType.WEAPON, true));
        }
        
        glLeftDown = leftDown;
        glRightDown = rightDown;
    }

    @SubscribeEvent
    public static void onMovementInputUpdate(net.minecraftforge.client.event.MovementInputUpdateEvent event) {
        if (event == null || event.getEntity() == null) return;
        Player player = (Player) event.getEntity();
        if (!(player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider)
                || !"gunlance".equals(weaponIdProvider.getWeaponId())) {
            return;
        }
        PlayerWeaponState state = CapabilityUtil.getPlayerWeaponState(player);
        if (state == null) return;
        if (state.isGunlanceCharging()) {
            // Block horizontal movement while charging Wyvern's Fire, but allow turning
            var input = event.getInput();
            input.leftImpulse = 0.0f;
            input.forwardImpulse = 0.0f;
            input.left = false;
            input.right = false;
        }
    }
}
