package org.example.common.combat.weapon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.network.PacketDistributor;
import org.example.common.data.WeaponDataResolver;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.compat.BetterCombatAnimationBridge;
import org.example.common.network.ModNetwork;
import org.example.common.network.packet.PlayAttackAnimationS2CPacket;
import org.example.common.network.packet.PlayerWeaponStateS2CPacket;
import org.example.common.util.CapabilityUtil;
import org.example.item.WeaponIdProvider;
import org.example.common.entity.KinsectEntity;
import org.example.registry.MHWeaponsItems;
import org.example.common.combat.StaminaHelper;
import org.example.common.config.MHWeaponsConfig;
import org.example.registry.MHAttributes;
import org.example.common.util.DebugLogger;
import org.example.common.combat.bowgun.BowgunHandler;

@SuppressWarnings("null")
public final class WeaponActionHandler {
    private WeaponActionHandler() {
    }

    @SuppressWarnings("unused")
    private static int resolveSpiritLevelMaxTicks(int level) {
        return switch (Math.max(1, Math.min(3, level))) {
            case 1 -> 1200;
            case 2 -> 900;
            default -> 700;
        };
    }

    public static void handleAction(Player player, WeaponActionType action, boolean pressed) {
        handleAction(player, action, pressed, 0.0f, 0.0f);
    }

    public static void handleAction(Player player, WeaponActionType action, boolean pressed, float inputX, float inputZ) {
        if (player == null) {
            return;
        }
        
        DebugLogger.logInput("Action: {}, Pressed: {}, Input: ({}, {})", action, pressed, inputX, inputZ);

        PlayerWeaponState weaponState = CapabilityUtil.getPlayerWeaponState(player);
        PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(player);
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof WeaponIdProvider weaponItem)) {
            return;
        }
        String weaponId = weaponItem.getWeaponId();

        if (action == WeaponActionType.DODGE && pressed) {
            if (weaponState != null && "insect_glaive".equals(weaponId)
                    && weaponState.getInsectAerialTicks() > 0 && !player.onGround()) {
                float cost = StaminaHelper.applyCost(player, 20.0f);
                if (weaponState.getStamina() >= cost) {
                    if (combatState != null) {
                        combatState.setDodgeIFrameTicks(6);
                        combatState.setActionKey("midair_evade");
                        combatState.setActionKeyTicks(6);
                    }
                    weaponState.addStamina(-cost);
                    weaponState.setStaminaRecoveryDelay(16);
                    Vec3 dash = player.getLookAngle().normalize().scale(0.7);
                    player.setDeltaMovement(dash.x, Math.max(0.15, player.getDeltaMovement().y), dash.z);
                    player.hurtMarked = true;
                }
                return;
            }
            if (weaponState != null && "dual_blades".equals(weaponId)) {
                if (DualBladesHandler.handleDemonDodge(player, combatState, weaponState)) {
                    return;
                }
            }
            if (weaponState != null && "tonfa".equals(weaponId)) {
                if (TonfaHandler.handleDodge(player, true, combatState, weaponState)) {
                    return;
                }
            }
            float baseCost = player.onGround() ? 20.0f : 30.0f;
            // Bowgun dodge tuning by weight class
            if ("bowgun".equals(weaponId)) {
                int weightClass = org.example.item.BowgunItem.getWeightClass(stack);
                if (weightClass == 2) {
                    return; // Heavy: no dodge/evade (uses shield)
                }
                baseCost = (weightClass == 0) ? (player.onGround() ? 14.0f : 22.0f) : baseCost;
            }
            float cost = StaminaHelper.applyCost(player, baseCost);
            if (weaponState != null && weaponState.getStamina() >= cost) {
                if (combatState != null) {
                    int iFrames = 8;
                    if ("bowgun".equals(weaponId)) {
                        int weightClass = org.example.item.BowgunItem.getWeightClass(stack);
                        iFrames = (weightClass == 0) ? 10 : 8;
                    }
                    combatState.setDodgeIFrameTicks(iFrames);
                    combatState.setActionKey("dodge");
                    combatState.setActionKeyTicks(iFrames);
                }
                weaponState.addStamina(-cost);
                weaponState.setStaminaRecoveryDelay(20);
                double dodgeBonus = player.getAttributeValue(MHAttributes.DODGE_DISTANCE_BONUS.get());
                double dist = 0.6 * Math.max(0.0, 1.0 + dodgeBonus);
                Vec3 dash = player.getLookAngle().normalize().scale(dist);
                player.setDeltaMovement(dash.x, player.getDeltaMovement().y + 0.1, dash.z);
                player.hurtMarked = true;
            }
            return;
        }

        if (action == WeaponActionType.BOWGUN_AIM || action == WeaponActionType.BOWGUN_RELOAD) {
            // Route bowgun-specific actions directly to BowgunHandler
            if ("bowgun".equals(weaponId) && weaponState != null && combatState != null) {
                BowgunHandler.handleAction(action, pressed, player, combatState, weaponState);
                syncWeaponState(player, weaponState);
            }
            return;
        }

        if (action == WeaponActionType.GUARD) {
            if (combatState != null) {
                combatState.setGuardPointActive(pressed);
            }
            if (weaponState != null && "lance".equals(weaponId)) {
                weaponState.setLanceGuardActive(pressed);
            }
            if (weaponState != null && "bowgun".equals(weaponId)) {
                BowgunHandler.handleAction(action, pressed, player, combatState, weaponState);
                syncWeaponState(player, weaponState);
            }
            return;
        }

        if (action == WeaponActionType.SHEATHE && pressed) {
            if (combatState != null) {
                double sheatheSpeedBonus = player.getAttributeValue(MHAttributes.SHEATHE_SPEED_BONUS.get());
                double sheatheSpeedMultiplier = Math.max(0.1D, 1.0D + sheatheSpeedBonus);
                int sheatheTicks = (int) Math.max(4, Math.round(10.0D / sheatheSpeedMultiplier));
                combatState.setActionKey("sheathe");
                combatState.setActionKeyTicks(sheatheTicks);
            }
            player.stopUsingItem();
            return;
        }

        if (weaponState == null || combatState == null) {
            return;
        }

        if (action == WeaponActionType.CHARGE) {
            // Bowgun uses CHARGE as fire button — bypass generic charge handling
            if ("bowgun".equals(weaponId)) {
                BowgunHandler.handleAction(action, pressed, player, combatState, weaponState);
                syncWeaponState(player, weaponState);
                return;
            }
            handleCharge(action, pressed, player, combatState, weaponState);
            return;
        }

        if (action == WeaponActionType.DOUBLE_JUMP && pressed) {
            if ("tonfa".equals(weaponId)) {
                TonfaHandler.handleDoubleJump(player, true, combatState, weaponState);
            }
            return;
        }

        if ((action == WeaponActionType.KINSECT_LAUNCH || action == WeaponActionType.KINSECT_RECALL)
                && !"insect_glaive".equals(weaponId)) {
            return;
        }

        switch (weaponId) {
            case "greatsword" -> handleGreatSword(action, pressed, combatState);
            case "longsword" -> LongSwordHandler.handleAction(action, pressed, player, combatState, weaponState, inputX, inputZ);
            case "sword_and_shield" -> handleSwordAndShield(action, pressed, combatState);
            case "dual_blades" -> {
                DualBladesHandler.handleAction(action, pressed, player, combatState, weaponState);
                syncWeaponState(player, weaponState);
            }
            case "hammer" -> handleHammer(action, pressed, combatState, weaponState);
            case "hunting_horn" -> HuntingHornHandler.handleAction(action, pressed, player, combatState, weaponState);
            case "lance" -> handleLance(action, pressed, player, combatState, weaponState);
            case "gunlance" -> handleGunlance(action, pressed, player, combatState, weaponState);
            case "switch_axe" -> {
                SwitchAxeHandler.handleAction(action, pressed, player, combatState, weaponState);
                syncWeaponState(player, weaponState);
            }
            case "charge_blade" -> {
                ChargeBladeHandler.handleAction(action, pressed, player, combatState, weaponState);
                syncWeaponState(player, weaponState);
            }
            case "insect_glaive" -> handleInsectGlaive(action, pressed, player, combatState, weaponState);
            case "tonfa" -> {
                TonfaHandler.handle(player, action, pressed, combatState, weaponState);
                syncWeaponState(player, weaponState);
            }
            case "magnet_spike" -> MagnetSpikeHandler.handleAction(action, pressed, player, combatState, weaponState);
            case "accel_axe" -> handleAccelAxe(action, pressed, player, combatState, weaponState);
            case "bow" -> handleBow(action, pressed, combatState, weaponState);
            case "bowgun" -> {
                BowgunHandler.handleAction(action, pressed, player, combatState, weaponState);
                syncWeaponState(player, weaponState);
            }
            default -> {
            }
        }
    }

    private static void handleCharge(WeaponActionType action, boolean pressed, Player player, PlayerCombatState combatState, PlayerWeaponState weaponState) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof WeaponIdProvider weaponItem)) {
            return;
        }
        String weaponId = weaponItem.getWeaponId();
        if (!isChargeWeapon(weaponId)) {
            return;
        }
        if ("magnet_spike".equals(weaponId)
                && weaponState != null
                && !weaponState.isMagnetSpikeImpactMode()) {
            if (pressed) {
                return;
            }
            if (weaponState.isChargingAttack()) {
                weaponState.setChargingAttack(false);
                weaponState.setChargeAttackTicks(0);
            }
            return;
        }
        if ("longsword".equals(weaponId) && combatState != null
                && "spirit_helm_breaker".equals(combatState.getActionKey())) {
            if (pressed) {
                weaponState.setChargingAttack(true);
                weaponState.setChargeAttackTicks(0);
                return;
            }
            if (weaponState.isChargingAttack()) {
                weaponState.setChargingAttack(false);
                weaponState.setChargeAttackTicks(0);
                LongSwordHandler.triggerSpiritReleaseSlash(player, combatState, weaponState);
            }
            return;
        }
        int maxCharge = org.example.common.data.WeaponDataResolver.resolveInt(player, null, "chargeMaxTicks", 40);
        if (pressed) {
            if ("longsword".equals(weaponId)) {
                weaponState.setLongSwordChargeReady(false);
            }
            weaponState.setChargingAttack(true);
            weaponState.setChargeAttackTicks(0);
            if ("longsword".equals(weaponId)) {
                setAction(combatState, "spirit_charge", Math.max(8, maxCharge));
            } else {
                setAction(combatState, "charge_start", 8);
            }
            return;
        }

        if (!weaponState.isChargingAttack()) {
            return;
        }
        int chargeTicks = weaponState.getChargeAttackTicks();
        weaponState.setChargingAttack(false);
        weaponState.setChargeAttackTicks(0);
        int chargeLevel = 0;
        if (maxCharge > 0) {
            if (chargeTicks >= maxCharge) {
                chargeLevel = 3;
            } else if (chargeTicks >= (maxCharge * 2 / 3)) {
                chargeLevel = 2;
            } else if (chargeTicks >= (maxCharge / 3)) {
                chargeLevel = 1;
            }
        }
        if (chargeLevel > 0) {
            spawnChargeParticles(player, chargeLevel);
        }

        if ("great_sword".equals(weaponId)) {
            boolean full = chargeTicks >= maxCharge;
            setAction(combatState, full ? "focus_strike" : "strong_charge", 12);
        } else if ("hammer".equals(weaponId)) {
            int level = chargeTicks >= (maxCharge * 2 / 3) ? 3 : (chargeTicks >= (maxCharge / 3) ? 2 : 1);
            weaponState.setHammerChargeLevel(level);
            weaponState.setHammerChargeTicks(60);
            setAction(combatState, "charge_lv" + level, 12);
        } else if ("longsword".equals(weaponId)) {
            LongSwordHandler.handleChargeRelease(player, combatState, weaponState, chargeTicks, maxCharge);
        } else if ("magnet_spike".equals(weaponId)) {
            MagnetSpikeHandler.handleChargeRelease(player, combatState, weaponState, chargeTicks, maxCharge);
        } else if ("lance".equals(weaponId)) {
            setAction(combatState, "charge_thrust", 12);
        } else if ("gunlance".equals(weaponId)) {
            if (weaponState.getGunlanceCooldown() <= 0
                    && player.getMainHandItem().getItem() instanceof org.example.item.GunlanceItem gl) {
                gl.useShell(player.level(), player, weaponState, true);
            }
            setAction(combatState, "charge_shell", 12);
        } else if ("insect_glaive".equals(weaponId)) {
            // If player has all three extracts, start the powerful finisher chain
            boolean hasAllExtracts = weaponState.isInsectRed() && weaponState.isInsectWhite() && weaponState.isInsectOrange();
            if (hasAllExtracts) {
                weaponState.setInsectTripleFinisherStage(1);
                weaponState.setInsectTripleFinisherTicks(60); // time window to continue the finisher
                setAction(combatState, "tornado_slash", 18);
            } else {
                // Basic descending/charge action when not fully powered
                setAction(combatState, "descending_slash", 12);
            }
            return;
        } else if ("sword_and_shield".equals(weaponId)) {
            setAction(combatState, "charged_slash", 10);
        } else if ("charge_blade".equals(weaponId)) {
            setAction(combatState, "charged_slash", 10);
        } else if ("tonfa".equals(weaponId)) {
            TonfaHandler.handleChargeRelease(player, combatState);
        } else if ("accel_axe".equals(weaponId)) {
            setAction(combatState, "accel_charge", 10);
        }
    }

    private static void spawnChargeParticles(Player player, int level) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double height = 0.9D;
        double spread = 0.2D + (level * 0.05D);
        int count = 10 + (level * 6);
        serverLevel.sendParticles(ParticleTypes.CRIT,
                player.getX(), player.getY() + height, player.getZ(),
                count, spread, 0.12D, spread, 0.02D);
    }

    @SuppressWarnings("unused")
    private static void spawnHelmBreakerParticles(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                12, 0.4D, 0.2D, 0.4D, 0.0D);
    }

    private static boolean isChargeWeapon(String weaponId) {
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

    private static void handleGreatSword(WeaponActionType action, boolean pressed, PlayerCombatState combatState) {
        if (!pressed) {
            return;
        }
        if (action == WeaponActionType.SPECIAL) {
            setAction(combatState, "tackle", 10);
        }
    }


    // handleLongSword removed - logic moved to LongSwordHandler


    private static void handleSwordAndShield(WeaponActionType action, boolean pressed, PlayerCombatState combatState) {
        if (!pressed) {
            return;
        }
        if (action == WeaponActionType.WEAPON) {
            setAction(combatState, "backstep", 10);
        } else if (action == WeaponActionType.WEAPON_ALT) {
            setAction(combatState, "shield_bash", 10);
        } else if (action == WeaponActionType.SPECIAL) {
            setAction(combatState, "perfect_rush", 12);
        }
    }

    // Dual Blades: Logic moved to DualBladesHandler.java

    private static void handleHammer(WeaponActionType action, boolean pressed, PlayerCombatState combatState, PlayerWeaponState weaponState) {
        if (!pressed) {
            return;
        }
        if (action == WeaponActionType.SPECIAL) {
            weaponState.setHammerPowerCharge(!weaponState.isHammerPowerCharge());
            setAction(combatState, "power_charge", 10);
            return;
        }
        if (action == WeaponActionType.WEAPON) {
            int next = weaponState.getHammerChargeLevel() + 1;
            if (next > 3) {
                next = 1;
            }
            weaponState.setHammerChargeLevel(next);
            weaponState.setHammerChargeTicks(80);
            setAction(combatState, "charge_lv" + next, 10);
            return;
        }
        if (action == WeaponActionType.WEAPON_ALT) {
            setAction(combatState, "golf_swing", 10);
        }
    }

    // Hunting Horn logic moved to HuntingHornHandler.java

    private static void handleLance(WeaponActionType action, boolean pressed, Player player, PlayerCombatState combatState, PlayerWeaponState weaponState) {
        if (!pressed) {
            return;
        }
        if (action == WeaponActionType.WEAPON) {
            setAction(combatState, "charge_thrust", 10);
        } else if (action == WeaponActionType.WEAPON_ALT) {
            if (player.isShiftKeyDown()) {
                weaponState.setLancePowerGuard(!weaponState.isLancePowerGuard());
                weaponState.setLanceGuardActive(weaponState.isLancePowerGuard());
                setAction(combatState, "power_guard", 10);
            } else {
                setAction(combatState, "guard_dash", 10);
            }
        } else if (action == WeaponActionType.SPECIAL) {
            weaponState.setLancePerfectGuardTicks(6);
            setAction(combatState, "counter_thrust", 10);
        }
    }

    private static void handleGunlance(WeaponActionType action, boolean pressed, Player player, PlayerCombatState combatState, PlayerWeaponState weaponState) {
        if (!pressed) {
            return;
        }
        if (!(player.getMainHandItem().getItem() instanceof org.example.item.GunlanceItem gl)) {
            return;
        }

        // Key Press: WEAPON_ALT -> Reload
        // Key Press: SPECIAL -> Wyvern's Fire
        // Mouse Right Click (Item Use) -> Shelling (handled in GunlanceItem.use)

        if (action == WeaponActionType.WEAPON_ALT) { // Reload
            boolean full = player.isShiftKeyDown();
                boolean shellsMissing = weaponState.getGunlanceShells() < weaponState.getGunlanceMaxShells();
                String actionKey = combatState.getActionKey();
                boolean recentGunlanceSwing = actionKey != null
                    && combatState.getActionKeyTicks() > 0
                    && (actionKey.startsWith("gunlance_") || "shell".equals(actionKey) || "charge_shell".equals(actionKey));

            if (!full && shellsMissing && recentGunlanceSwing) {
                gl.quickReload(player, weaponState);
                setAction(combatState, "quick_reload", 8);
                if (player instanceof ServerPlayer serverPlayer) {
                    String animId = WeaponDataResolver.resolveString(player, "animationOverrides", "quick_reload",
                            "bettercombat:two_handed_slash_horizontal_left");
                    float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 20.0f);
                    float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.3f);
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f,
                                    "quick_reload", 8));
                }
            } else {
                gl.reload(player, weaponState, full);
                setAction(combatState, "reload", 10);
                if (player instanceof ServerPlayer serverPlayer) {
                    String animId = WeaponDataResolver.resolveString(player, "animationOverrides", "reload",
                            "bettercombat:two_handed_slam");
                    float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 20.0f);
                    float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.3f);
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f,
                                    "reload", 10));
                }
            }
            syncWeaponState(player, weaponState);
            return;
        }
        if (action == WeaponActionType.SPECIAL) { // WyvernFire
            if (weaponState.getGunlanceWyvernFireGauge() >= 1.0f && !weaponState.isGunlanceCharging()) {
                gl.useWyvernFire(player.level(), player, weaponState);
                setAction(combatState, "wyvernfire", 60); // Longer animation state for charge
                if (player instanceof ServerPlayer serverPlayer) {
                    String animId = WeaponDataResolver.resolveString(player, "animationOverrides", "wyvernfire",
                            "bettercombat:two_handed_slam_heavy");
                    float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 20.0f);
                    float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.3f);
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f,
                                    "wyvernfire", 60));
                }
            }
            return;
        }
        
        // Manual Wyrmstake (Shift + Attack) or (Shift + Right Click via Packet)
        if (action == WeaponActionType.WEAPON) { 
             // Logic: If Shift is held, we assume the user wants Wyrmstake (if available)
             // This corresponds to the Shift+RightClick packet we send from ClientForgeEvents
             if (player.isShiftKeyDown() && weaponState.hasGunlanceStake()) {
                 gl.useWyrmstake(player.level(), player, weaponState);
                 setAction(combatState, "wyrmstake", 12);
                 if (player instanceof ServerPlayer serverPlayer) {
                     String animId = WeaponDataResolver.resolveString(player, "animationOverrides", "wyrmstake",
                             "bettercombat:two_handed_stab_right");
                     float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 20.0f);
                     float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.3f);
                     ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                             new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f,
                                     "wyrmstake", 12));
                 }
                 syncWeaponState(player, weaponState);
                 return;
             }

             // Burst Fire: After Overhead Smash (context-sensitive trigger)
             // Client sends WEAPON action without shift held when conditions are met
             String prevAction = combatState.getActionKey();
             boolean afterOverheadSmash = "gunlance_overhead_smash".equals(prevAction)
                     && combatState.getActionKeyTicks() > 0;
             if (!player.isShiftKeyDown() && afterOverheadSmash && weaponState.getGunlanceShells() > 0) {
                 gl.useBurstFire(player.level(), player, weaponState);
                 setAction(combatState, "burst_fire", 14);
                 if (player instanceof ServerPlayer serverPlayer) {
                     String animId = WeaponDataResolver.resolveString(player, "animationOverrides", "burst_fire",
                             "bettercombat:two_handed_slam_heavy");
                     float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 20.0f);
                     float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.3f);
                     ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                             new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f,
                                     "burst_fire", 14));
                 }
                 syncWeaponState(player, weaponState);
                 return;
             }
        }
    }

    @SuppressWarnings("null")
    private static void handleInsectGlaive(WeaponActionType action, boolean pressed, Player player, PlayerCombatState combatState, PlayerWeaponState weaponState) {
        // --- Handle charge release (RMB released while charging) ---
        if (action == WeaponActionType.WEAPON_ALT && !pressed) {
            if (weaponState.isInsectCharging()) {
                weaponState.setInsectCharging(false);
                int chargeTicks = weaponState.getInsectChargeTicks();
                weaponState.setInsectChargeTicks(0);

                if (chargeTicks >= 20) { // Full charge threshold → Descending Slash/Thrust
                    if (weaponState.getInsectAerialTicks() > 0 && !player.onGround()) {
                        // Aerial: Descending Thrust — dive attack
                        setAction(combatState, "descending_thrust", 16);
                        player.setDeltaMovement(player.getDeltaMovement().x * 0.3, -1.2, player.getDeltaMovement().z * 0.3);
                        player.hurtMarked = true;
                        if (player.level() instanceof ServerLevel serverLevel) {
                            blastInFront(player, 3.5, 18.0f);
                            Vec3 pos = player.position();
                            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y + 0.5, pos.z, 6, 0.5, 0.3, 0.5, 0.01);
                        }
                    } else {
                        // Ground: Descending Slash — heavy overhead
                        setAction(combatState, "descending_slash", 18);
                        if (player.level() instanceof ServerLevel serverLevel) {
                            blastInFront(player, 4.0, 22.0f);
                            Vec3 look = player.getLookAngle().normalize();
                            Vec3 center = player.position().add(look.scale(1.5)).add(0, 1.0, 0);
                            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 8, 0.3, 0.2, 0.3, 0.02);
                        }
                        // If triple up → enable finisher chain
                        if (weaponState.isInsectRed() && weaponState.isInsectWhite() && weaponState.isInsectOrange()) {
                            weaponState.setInsectTripleFinisherStage(1);
                            weaponState.setInsectTripleFinisherTicks(60);
                        }
                    }
                } else if (chargeTicks >= 10) { // Partial charge → Tornado Slash (spinning attack)
                    setAction(combatState, "tornado_slash", 14);
                    if (player.level() instanceof ServerLevel serverLevel) {
                        blastInFront(player, 3.5, 16.0f);
                        Vec3 pos = player.position();
                        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y + 1.0, pos.z, 5, 0.4, 0.2, 0.4, 0.02);
                    }
                    // Tornado Slash can chain into another charge for Descending Slash
                    weaponState.setInsectCharging(true);
                    weaponState.setInsectChargeTicks(0);
                }
                // else: too short, no attack produced

                weaponState.setInsectComboIndex(0);
                weaponState.setInsectComboTick(player.tickCount);
                syncWeaponState(player, weaponState);
                return;
            }
            return; // Normal RMB release, nothing to do
        }

        if (!pressed) {
            return;
        }

        // --- Kinsect Launch (handled via dedicated packet) ---
        if (action == WeaponActionType.KINSECT_LAUNCH) {
            return;
        }

        // --- LMB+RMB Instant Descending Slash (CHARGE action used as shortcut signal) ---
        if (action == WeaponActionType.CHARGE) {
            if (weaponState.isInsectRed()) {
                if (weaponState.getInsectAerialTicks() > 0 && !player.onGround()) {
                    // Aerial: instant Descending Thrust
                    setAction(combatState, "descending_thrust", 16);
                    player.setDeltaMovement(player.getDeltaMovement().x * 0.3, -1.2, player.getDeltaMovement().z * 0.3);
                    player.hurtMarked = true;
                    if (player.level() instanceof ServerLevel serverLevel) {
                        blastInFront(player, 3.5, 14.0f); // Slightly less than charged version
                        Vec3 pos = player.position();
                        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y + 0.5, pos.z, 4, 0.4, 0.3, 0.4, 0.01);
                    }
                } else {
                    // Ground: instant Descending Slash
                    setAction(combatState, "descending_slash", 16);
                    if (player.level() instanceof ServerLevel serverLevel) {
                        blastInFront(player, 3.5, 16.0f); // Slightly less than charged version
                        Vec3 look = player.getLookAngle().normalize();
                        Vec3 center = player.position().add(look.scale(1.5)).add(0, 1.0, 0);
                        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 6, 0.3, 0.2, 0.3, 0.02);
                    }
                    // Triple up → finisher chain (same as charged version)
                    if (weaponState.isInsectRed() && weaponState.isInsectWhite() && weaponState.isInsectOrange()) {
                        weaponState.setInsectTripleFinisherStage(1);
                        weaponState.setInsectTripleFinisherTicks(60);
                    }
                }
                weaponState.setInsectComboIndex(0);
                weaponState.setInsectComboTick(player.tickCount);
                weaponState.setInsectCharging(false);
                weaponState.setInsectChargeTicks(0);
                syncWeaponState(player, weaponState);
            }
            return;
        }

        // --- Kinsect Recall ---
        if (action == WeaponActionType.KINSECT_RECALL) {
            if (!hasKinsectOffhand(player)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Kinsect required in offhand"), true);
                return;
            }
            KinsectEntity kinsect = resolveKinsect(player, weaponState);
            if (kinsect != null) {
                kinsect.recall();
                setAction(combatState, "kinsect_recall", 10);
            }
            return;
        }

        // --- LMB (WEAPON) ---
        if (action == WeaponActionType.WEAPON) {
            // Aerial: Jumping Advancing Slash
            if (weaponState.getInsectAerialTicks() > 0 && !player.onGround()) {
                int bounceLevel = weaponState.getInsectAerialBounceLevel();
                float speedScale = 0.65f + (bounceLevel * 0.15f); // Power up per bounce level
                setAction(combatState, "aerial_advancing_slash", 10);
                Vec3 forward = player.getLookAngle().normalize().scale(speedScale);
                double lift = Math.max(0.25, player.getDeltaMovement().y);
                player.setDeltaMovement(forward.x, lift, forward.z);
                player.hurtMarked = true;
                // Check if there's a target in front for Vaulting Dance
                LivingEntity target = findTargetInFront(player, 3.0);
                if (target != null && bounceLevel < 2) {
                    // Vaulting Dance: bounce off the target, increase power level
                    weaponState.setInsectAerialBounceLevel(bounceLevel + 1);
                    weaponState.setInsectAerialTicks(Math.max(weaponState.getInsectAerialTicks(), 30)); // Refresh air time
                    float bounceLift = 0.6f + (bounceLevel * 0.1f);
                    player.setDeltaMovement(forward.x * 0.3, bounceLift, forward.z * 0.3);
                    player.hurtMarked = true;
                    float bounceDamage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * (1.0 + bounceLevel * 0.2));
                    target.hurt(player.damageSources().playerAttack(player), bounceDamage);
                    if (player.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                                4, 0.3, 0.2, 0.3, 0.01);
                    }
                }
                syncWeaponState(player, weaponState);
                return;
            }

            // Ground combo: Rising Slash → Reaping Slash → Double Slash (cycles 0→1→2→0)
            int window = WeaponDataResolver.resolveInt(player, null, "comboWindowTicks", 12);
            int lastTick = weaponState.getInsectComboTick();
            int current = weaponState.getInsectComboIndex();
            boolean withinWindow = (player.tickCount - lastTick) <= window;

            int next = withinWindow ? (current + 1) % 3 : 0;
            weaponState.setInsectComboIndex(next);
            weaponState.setInsectComboTick(player.tickCount);
            String actionKey = switch (next) {
                case 0 -> "rising_slash";
                case 1 -> "reaping_slash";
                default -> "double_slash";
            };
            setAction(combatState, actionKey, 10);
            if (combatState.isFocusMode()) {
                triggerKinsectComboAttack(player, weaponState);
            }
            return;
        }

        // --- RMB (WEAPON_ALT) ---
        if (action == WeaponActionType.WEAPON_ALT) {
            // Triple Extract Finisher chain progression
            if (weaponState.getInsectTripleFinisherStage() > 0) {
                int stage = weaponState.getInsectTripleFinisherStage();
                if (stage == 1) {
                    // After Descending Slash → Rising Spiral Slash (final, heavy damage, consume extracts)
                    weaponState.setInsectTripleFinisherStage(0);
                    weaponState.setInsectTripleFinisherTicks(0);
                    setAction(combatState, "rising_spiral_slash", 24);
                    // Launch player upward during Rising Spiral Slash
                    player.setDeltaMovement(player.getDeltaMovement().x * 0.3, 0.85, player.getDeltaMovement().z * 0.3);
                    player.hurtMarked = true;
                    if (player.level() instanceof ServerLevel serverLevel) {
                        blastInFront(player, 5.0, 48.0f);
                        Vec3 look = player.getLookAngle().normalize();
                        Vec3 center = player.position().add(look.scale(2.0)).add(0, 1.0, 0);
                        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
                        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z, 10, 0.4, 0.3, 0.4, 0.01);
                    }
                    // Consume extracts
                    weaponState.setInsectRed(false);
                    weaponState.setInsectWhite(false);
                    weaponState.setInsectOrange(false);
                    weaponState.setInsectExtractTicks(0);
                    weaponState.setInsectCharging(false);
                    weaponState.setInsectChargeTicks(0);
                    syncWeaponState(player, weaponState);
                    return;
                }
            }

            // Aerial: If charging in air, pressing RMB again should not override — charge release handles it
            // Jumping Slash (descend with attack)
            if (weaponState.getInsectAerialTicks() > 0 && !player.onGround()) {
                setAction(combatState, "aerial_slash", 10);
                Vec3 motion = player.getDeltaMovement();
                player.setDeltaMovement(motion.x, Math.min(-0.4, motion.y), motion.z);
                player.hurtMarked = true;
                if (player.level() instanceof ServerLevel) {
                    blastInFront(player, 3.0, 12.0f);
                }
                return;
            }

            // Ground: Check for directional input
            // Back + RMB → Dodge Slash (evasive backward slash)
            if (player.isShiftKeyDown()) {
                setAction(combatState, "dodge_slash", 12);
                Vec3 back = player.getLookAngle().normalize().scale(-0.8);
                player.setDeltaMovement(back.x, 0.3, back.z);
                player.hurtMarked = true;
                applyInsectGlaiveAltHit(player, false);
                weaponState.setInsectComboIndex(0);
                weaponState.setInsectComboTick(player.tickCount);
                return;
            }

            // Check if we should start charging (Red extract active, hold RMB)
            if (weaponState.isInsectRed()) {
                int window = WeaponDataResolver.resolveInt(player, null, "comboWindowTicks", 12);
                boolean inCombo = (player.tickCount - weaponState.getInsectComboTick()) <= window;

                // Forward + RMB → Leaping Slash (gap closer, leads into charge/Tornado Slash)
                // We detect "forward" as not-shifting and check combo state
                if (inCombo && weaponState.getInsectComboIndex() >= 1) {
                    // After at least one combo hit → Leaping Slash
                    setAction(combatState, "leaping_slash", 12);
                    Vec3 forward = player.getLookAngle().normalize().scale(0.9);
                    player.setDeltaMovement(forward.x, 0.35, forward.z);
                    player.hurtMarked = true;
                    applyInsectGlaiveAltHit(player, false);
                    weaponState.setInsectComboIndex(0);
                    weaponState.setInsectComboTick(player.tickCount);
                    // Start charging for Tornado Slash → Descending Slash chain
                    weaponState.setInsectCharging(true);
                    weaponState.setInsectChargeTicks(0);
                    syncWeaponState(player, weaponState);
                    return;
                }

                // Default with Red: start charging (will release into Descending Slash)
                // But first check overhead_smash eligibility
                boolean overheadReady = inCombo && weaponState.getInsectComboIndex() >= 2;
                if (overheadReady) {
                    // Overhead Smash takes priority when combo is ready
                    setAction(combatState, "overhead_smash", 10);
                    if (player instanceof ServerPlayer serverPlayer) {
                        String fallbackAnim = "bettercombat:two_handed_slam_heavy";
                        String animId = BetterCombatAnimationBridge.resolveComboAnimationServer(player, 0, fallbackAnim);
                        float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 16.666666f);
                        float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.25f);
                        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f, "overhead_smash", 10));
                    }
                    applyInsectGlaiveAltHit(player, true);
                    weaponState.setInsectComboIndex(0);
                    weaponState.setInsectComboTick(player.tickCount);
                    return;
                }

                // Wide Sweep (default RMB from idle with red) — also starts charge
                setAction(combatState, "wide_sweep", 10);
                if (player instanceof ServerPlayer serverPlayer) {
                    String fallbackAnim = "bettercombat:two_handed_slash_horizontal_left";
                    String animId = BetterCombatAnimationBridge.resolveComboAnimationServer(player, 0, fallbackAnim);
                    float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 16.666666f);
                    float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.25f);
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f, "wide_sweep", 10));
                }
                applyInsectGlaiveAltHit(player, false);
                if (combatState.isFocusMode()) {
                    KinsectEntity kinsect = resolveKinsect(player, weaponState);
                    if (kinsect != null) {
                        kinsect.recall();
                    }
                }
                // Begin charge after wide sweep (can hold RMB for Tornado Slash / Descending Slash)
                weaponState.setInsectCharging(true);
                weaponState.setInsectChargeTicks(0);
                syncWeaponState(player, weaponState);
                return;
            }

            // --- No Red extract: standard RMB behavior ---
            int window = WeaponDataResolver.resolveInt(player, null, "comboWindowTicks", 12);
            boolean overheadReady = (player.tickCount - weaponState.getInsectComboTick()) <= window
                    && weaponState.getInsectComboIndex() >= 2;
            String actionKey = overheadReady ? "overhead_smash" : "wide_sweep";
            setAction(combatState, actionKey, 10);
            if (player instanceof ServerPlayer serverPlayer) {
                String fallbackAnim = overheadReady
                    ? "bettercombat:two_handed_slam_heavy"
                    : "bettercombat:two_handed_slash_horizontal_left";
                String animId = BetterCombatAnimationBridge.resolveComboAnimationServer(player, 0, fallbackAnim);
                float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 16.666666f);
                float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.25f);
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f, actionKey, 10));
            }
            applyInsectGlaiveAltHit(player, overheadReady);
            if (overheadReady) {
                weaponState.setInsectComboIndex(0);
                weaponState.setInsectComboTick(player.tickCount);
            }
            if (!overheadReady && combatState.isFocusMode()) {
                KinsectEntity kinsect = resolveKinsect(player, weaponState);
                if (kinsect != null) {
                    kinsect.recall();
                }
            }
            return;
        }

        // --- SPECIAL (Vault / Kinsect Recall) ---
        if (action == WeaponActionType.SPECIAL) {
            if (player.isShiftKeyDown()) {
                // Shift+Special → Kinsect Recall
                KinsectEntity kinsect = resolveKinsect(player, weaponState);
                if (kinsect != null) {
                    kinsect.recall();
                    setAction(combatState, "kinsect_recall", 10);
                }
            } else {
                // Vault
                if (weaponState.getStamina() < 12.0f) {
                    return;
                }
                spendStamina(player, weaponState, 12.0f, 20);
                setAction(combatState, "vault", 10);
                weaponState.setInsectAerialTicks(40);
                weaponState.setInsectAerialBounceLevel(0); // Reset bounce on fresh vault
                // White extract gives extra jump height
                double vaultPower = 0.8;
                if (weaponState.isInsectWhite()) {
                    vaultPower = 1.0; // +25% vault height with White extract
                }
                // Backward vault: Shift held → backward direction + i-frames
                if (player.isShiftKeyDown()) {
                    Vec3 back = player.getLookAngle().normalize().scale(-0.6);
                    player.setDeltaMovement(back.x, vaultPower, back.z);
                    combatState.setDodgeIFrameTicks(10); // Backward vault grants 10 i-frames
                } else {
                    player.setDeltaMovement(player.getDeltaMovement().x, vaultPower, player.getDeltaMovement().z);
                }
                player.hurtMarked = true;
            }
        }
    }

    @SuppressWarnings("null")
    private static void applyInsectGlaiveAltHit(Player player, boolean overhead) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 horiz = new Vec3(look.x, 0.0, look.z);
        if (horiz.lengthSqr() < 0.0001) {
            horiz = new Vec3(0.0, 0.0, 1.0);
        }
        horiz = horiz.normalize();
        double range = overhead ? 3.6 : 3.0;
        double radius = overhead ? 1.1 : 1.3;
        Vec3 start = player.position().add(0.0, 0.9, 0.0);
        Vec3 end = start.add(horiz.scale(range));
        AABB box = new AABB(start, end).inflate(radius, 0.6, radius);
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float damage = (float) Math.max(1.0, base);
        LivingEntity target = serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
            .stream()
            .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
            .orElse(null);
        if (target != null) {
            target.hurt(player.damageSources().playerAttack(player), damage);
        }
    }



    private static void spendStamina(Player player, PlayerWeaponState state, float baseCost, int recoveryDelayTicks) {
        if (state == null) {
            return;
        }
        float cost = StaminaHelper.applyCost(player, baseCost);
        state.addStamina(-cost);
        state.setStaminaRecoveryDelay(recoveryDelayTicks);
    }

    private static void syncWeaponState(Player player, PlayerWeaponState state) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!state.isDirty()) {
            return;
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                new PlayerWeaponStateS2CPacket(player.getId(), state.serializeNBT()));
        state.clearDirty();
    }

    @SuppressWarnings("null")
    // Moved to MagnetSpikeHandler.java
    // private static void handleMagnetSpike(...)

    private static void handleAccelAxe(WeaponActionType action, boolean pressed, Player player, PlayerCombatState combatState, PlayerWeaponState weaponState) {
        if (!pressed) {
            return;
        }
        if (action == WeaponActionType.SPECIAL) {
            if (player.isShiftKeyDown()) {
                if (weaponState.getAccelFuel() < 10) {
                    return;
                }
                weaponState.addAccelFuel(-10);
                weaponState.setAccelParryTicks(8); // short blast-parry window
                setAction(combatState, "accel_parry", 8);
                return;
            }
            if (weaponState.getAccelFuel() < 30) {
                return;
            }
            weaponState.addAccelFuel(-30);
            setAction(combatState, "grand_slam", 12);
            player.setDeltaMovement(player.getDeltaMovement().x, 0.7, player.getDeltaMovement().z);
            player.hurtMarked = true;
            if (player.level() instanceof ServerLevel serverLevel) {
                dischargeAround(serverLevel, player, 3.0, 6.0f);
                double baseY = player.getY() + 0.4;
                // Dense cloud ring
                serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), baseY, player.getZ(), 40, 1.2, 0.2, 1.2, 0.1);
                // Flame bursts outward in a circle
                for (int i = 0; i < 24; i++) {
                    double angle = (Math.PI * 2 * i) / 24.0;
                    double dx = Math.cos(angle) * 0.8;
                    double dz = Math.sin(angle) * 0.8;
                    serverLevel.sendParticles(ParticleTypes.FLAME,
                            player.getX() + dx, baseY, player.getZ() + dz,
                            4, 0.15, 0.1, 0.15, 0.03);
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                            player.getX() + dx * 1.1, baseY, player.getZ() + dz * 1.1,
                            2, 0.15, 0.1, 0.15, 0.02);
                }
            }
            return;
        }
        if (action == WeaponActionType.WEAPON_ALT) {
            if (weaponState.getAccelFuel() <= 0) {
                return;
            }
            weaponState.setAccelDashTicks(20);
            setAction(combatState, "accel_dash", 10);
        }
    }

    private static void handleBow(WeaponActionType action, boolean pressed, PlayerCombatState combatState, PlayerWeaponState weaponState) {
        if (!pressed) {
            return;
        }
        if (action == WeaponActionType.SPECIAL) {
            int next = weaponState.getBowCoating() + 1;
            if (next > 3) {
                next = 0;
            }
            weaponState.setBowCoating(next);
            setAction(combatState, "bow_coating", 6);
        }
    }

    private static void setAction(PlayerCombatState combatState, String key, int ticks) {
        combatState.setActionKey(key);
        combatState.setActionKeyTicks(ticks);
    }

    @SuppressWarnings("null")
    private static void blastInFront(Player player, double range, float damage) {
        Vec3 start = player.getEyePosition();
        Vec3 dir = player.getLookAngle().normalize();
        Vec3 end = start.add(dir.scale(range));
        AABB box = player.getBoundingBox().expandTowards(dir.scale(range)).inflate(1.0);
        LivingEntity target = player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                .stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                .orElse(null);
        if (target == null) {
            return;
        }
        if (target.getBoundingBox().clip(start, end).isPresent()) {
            target.hurt(player.damageSources().playerAttack(player), damage);
        }
    }

    @SuppressWarnings("null")
    private static void dischargeAround(ServerLevel level, Player player, double radius, float damage) {
        if (player == null) {
            return;
        }
        AABB box = player.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)) {
            entity.hurt(player.damageSources().playerAttack(player), damage);
        }
    }

    @SuppressWarnings("null")
    private static LivingEntity findTargetInFront(Player player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 dir = player.getLookAngle().normalize();
        Vec3 end = start.add(dir.scale(range));
        AABB box = player.getBoundingBox().expandTowards(dir.scale(range)).inflate(1.0);
        return player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                .stream()
                .filter(e -> e.getBoundingBox().clip(start, end).isPresent())
                .findFirst()
                .orElse(null);
    }

    private static LivingEntity resolveEntityById(Player player, int id) {
        if (player.level() == null) {
            return null;
        }
        var entity = player.level().getEntity(id);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static KinsectEntity resolveKinsect(Player player, PlayerWeaponState weaponState) {
        int id = weaponState.getKinsectEntityId();
        if (id <= 0 || player.level() == null) {
            return null;
        }
        var entity = player.level().getEntity(id);
        return entity instanceof KinsectEntity kinsect ? kinsect : null;
    }

    private static KinsectEntity resolveOrCreateKinsect(Player player, PlayerWeaponState weaponState) {
        KinsectEntity existing = resolveKinsect(player, weaponState);
        if (existing != null) {
            return existing;
        }
        if (!hasKinsectOffhand(player)) {
            return null;
        }
        KinsectEntity kinsect = new KinsectEntity(MHWeaponsItems.KINSECT.get(), player.level(), player);
        player.level().addFreshEntity(kinsect);
        weaponState.setKinsectEntityId(kinsect.getId());
        return kinsect;
    }

    private static void triggerKinsectComboAttack(Player player, PlayerWeaponState weaponState) {
        if (player == null || weaponState == null) {
            return;
        }
        if (!hasKinsectOffhand(player)) {
            return;
        }
        KinsectEntity kinsect = resolveOrCreateKinsect(player, weaponState);
        if (kinsect == null) {
            return;
        }
        double range = MHWeaponsConfig.KINSECT_RANGE.get();
        LivingEntity target = findTargetInFront(player, range);
        Vec3 targetPos;
        if (target != null) {
            targetPos = target.position().add(0, target.getBbHeight() * 0.6, 0);
        } else {
            Vec3 start = player.getEyePosition();
            targetPos = start.add(player.getLookAngle().normalize().scale(range));
        }
        kinsect.launchTo(targetPos, target, targetPos);
    }

    @SuppressWarnings("null")
    public static void handleKinsectLaunch(Player player, int targetEntityId, Vec3 targetPos) {
        if (player == null) {
            return;
        }
        PlayerWeaponState weaponState = CapabilityUtil.getPlayerWeaponState(player);
        PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(player);
        if (weaponState == null || combatState == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof WeaponIdProvider weaponItem)) {
            return;
        }
        if (!"insect_glaive".equals(weaponItem.getWeaponId())) {
            return;
        }
        if (!hasKinsectOffhand(player)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Kinsect required in offhand"), true);
            return;
        }
        KinsectEntity kinsect = resolveOrCreateKinsect(player, weaponState);
        if (kinsect == null) {
            return;
        }
        LivingEntity target = resolveEntityById(player, targetEntityId);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 fallback = player.position().add(look.scale(12.0));
        Vec3 finalPos = targetPos != null ? targetPos : (target != null
                ? target.position().add(0, target.getBbHeight() * 0.6, 0)
                : fallback);

        // If target exists and player is shifting, use Mark mode
        if (target != null && player.isShiftKeyDown()) {
            kinsect.launchToMark(finalPos, target, targetPos != null ? targetPos : finalPos);
            setAction(combatState, "kinsect_mark", 12);
        } else {
            kinsect.launchTo(finalPos, target, targetPos != null ? targetPos : finalPos);
            setAction(combatState, "kinsect_harvest", 10);
        }
    }

    private static boolean hasKinsectOffhand(Player player) {
        if (player.getOffhandItem().getItem() instanceof org.example.item.KinsectItem) {
            return true;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof org.example.item.KinsectItem) {
                return true;
            }
        }
        return false;
    }

    // Hunting Horn song/note/melody/bubble helpers moved to HuntingHornHandler.java
}
