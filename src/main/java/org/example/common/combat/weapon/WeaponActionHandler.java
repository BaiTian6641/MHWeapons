package org.example.common.combat.weapon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.PacketDistributor;
import org.example.common.data.WeaponData;
import org.example.common.data.WeaponDataResolver;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.compat.BetterCombatAnimationBridge;
import org.example.common.network.ModNetwork;
import org.example.common.network.packet.MagnetSpikeZipAnimationS2CPacket;
import org.example.common.network.packet.PlayAttackAnimationS2CPacket;
import org.example.common.network.packet.PlayerWeaponStateS2CPacket;
import org.example.common.util.CapabilityUtil;
import org.example.item.WeaponIdProvider;
import org.example.common.entity.KinsectEntity;
import org.example.registry.MHWeaponsItems;
import org.example.common.combat.StaminaHelper;
import org.example.common.config.MHWeaponsConfig;
import org.example.registry.MHAttributes;

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
            if (weaponState != null && "tonfa".equals(weaponId)) {
                if (TonfaHandler.handleDodge(player, true, combatState, weaponState)) {
                    return;
                }
            }
            float cost = StaminaHelper.applyCost(player, player.onGround() ? 20.0f : 30.0f);
            if (weaponState != null && weaponState.getStamina() >= cost) {
                if (combatState != null) {
                    combatState.setDodgeIFrameTicks(8);
                    combatState.setActionKey("dodge");
                    combatState.setActionKeyTicks(8);
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

        if (action == WeaponActionType.GUARD) {
            if (combatState != null) {
                combatState.setGuardPointActive(pressed);
            }
            if (weaponState != null && "lance".equals(weaponId)) {
                weaponState.setLanceGuardActive(pressed);
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
            case "dual_blades" -> handleDualBlades(action, pressed, player, combatState, weaponState);
            case "hammer" -> handleHammer(action, pressed, combatState, weaponState);
            case "hunting_horn" -> handleHuntingHorn(action, pressed, player, combatState, weaponState);
            case "lance" -> handleLance(action, pressed, player, combatState, weaponState);
            case "gunlance" -> handleGunlance(action, pressed, player, combatState, weaponState);
            case "switch_axe" -> {
                SwitchAxeHandler.handleAction(action, pressed, player, combatState, weaponState);
                syncWeaponState(player, weaponState);
            }
            case "charge_blade" -> handleChargeBlade(action, pressed, player, combatState, weaponState);
            case "insect_glaive" -> handleInsectGlaive(action, pressed, player, combatState, weaponState);
            case "tonfa" -> {
                TonfaHandler.handle(player, action, pressed, combatState, weaponState);
                syncWeaponState(player, weaponState);
            }
            case "magnet_spike" -> MagnetSpikeHandler.handleAction(action, pressed, player, combatState, weaponState);
            case "accel_axe" -> handleAccelAxe(action, pressed, player, combatState, weaponState);
            case "bow" -> handleBow(action, pressed, combatState, weaponState);
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

    private static void handleDualBlades(WeaponActionType action, boolean pressed, Player player, PlayerCombatState combatState, PlayerWeaponState weaponState) {
        if (!pressed) {
            return;
        }
        if (action == WeaponActionType.SPECIAL) {
            boolean next = !weaponState.isDemonMode();
            weaponState.setDemonMode(next);
            setAction(combatState, next ? "demon_mode" : "exit_demon", 10);
            return;
        }
        if (action == WeaponActionType.WEAPON) {
            if (weaponState.isDemonMode() || weaponState.isArchDemon()) {
                weaponState.addDemonGauge(-20.0f);
                spendStamina(player, weaponState, 12.0f, 20);
                setAction(combatState, "demon_dance", 12);
            }
            return;
        }
        if (action == WeaponActionType.WEAPON_ALT) {
            spendStamina(player, weaponState, 8.0f, 15);
            setAction(combatState, "blade_dance", 10);
        }
    }

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

    @SuppressWarnings("null")
    private static void handleHuntingHorn(WeaponActionType action, boolean pressed, Player player, PlayerCombatState combatState, PlayerWeaponState weaponState) {
        if (!pressed) {
            return;
        }
        
        // Note logic: Left=1, Right=2, Left+Right = 3 (Shift still allowed for legacy input)
        int note = 0;
        if (action == WeaponActionType.HORN_NOTE_BOTH) {
            note = 3;
        } else if (action == WeaponActionType.WEAPON) {
            note = 1;
            if (player.isShiftKeyDown()) note = 3; // Shift+Left = 3
        } else if (action == WeaponActionType.WEAPON_ALT) {
            note = 2;
            if (player.isShiftKeyDown()) note = 3; // Shift+Right = 3
        }
        boolean triggerAltAnimation = action == WeaponActionType.WEAPON_ALT;

        if (note > 0) {
            weaponState.addHornNote(note);
            String actionKey = note == 1 ? "note_one" : (note == 2 ? "note_two" : "note_three");
            setAction(combatState, actionKey, 10);

                if (triggerAltAnimation && player instanceof ServerPlayer serverPlayer) {
                String fallbackAnim = WeaponDataResolver.resolveString(player, "animationOverrides", "hunting_horn_note",
                    "bettercombat:two_handed_slash_horizontal_right");
                String animId = BetterCombatAnimationBridge.resolveComboAnimationServer(player, 0, fallbackAnim);
                float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 16.666666f);
                float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.55f);
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f, actionKey, 10));
            }
            WeaponData data = WeaponDataResolver.resolve(player);
            SongMatch autoMatch = resolveAutoSongMatch(data, weaponState);
            if (autoMatch != null) {
                weaponState.addHornMelody(autoMatch.melodyId);
            }
            return;
        }

        if (action == WeaponActionType.SPECIAL && !player.isShiftKeyDown()) {
            setAction(combatState, "recital", 12);
            WeaponData data = WeaponDataResolver.resolve(player);
            SongMatch match = resolveSongMatch(data, weaponState);
            if (match != null) {
                weaponState.addHornMelody(match.melodyId);
                weaponState.setHornBuffTicks(match.duration);
                weaponState.clearHornNotes();
                return;
            }
            if (weaponState.getHornLastMelodyEnhanceTicks() > 0 && weaponState.getHornLastMelodyId() > 0) {
                MelodyPlay last = resolveMelodyById(data, weaponState.getHornLastMelodyId());
                if (last != null) {
                    int amp = last.amplifier + 1;
                    int duration = last.duration + (last.duration / 2);
                    applyMelodyEffect(player, last.songId, last.effect, amp, duration, last.radius);
                    applyHornSongBuff(weaponState, last.songId, duration);
                    weaponState.setHornBuffTicks(duration);
                    weaponState.setHornMelodyPlayTicks(12);
                    weaponState.setHornLastMelodyEnhanceTicks(0);
                    weaponState.clearHornNotes();
                    return;
                }
            }
            MelodyPlay play = resolveMelodyPlay(data, weaponState);
            if (play != null) {
                applyMelodyEffect(player, play.songId, play.effect, play.amplifier, play.duration, play.radius);
                applyHornSongBuff(weaponState, play.songId, play.duration);
                weaponState.setHornBuffTicks(play.duration);
                weaponState.setHornMelodyPlayTicks(12);
                weaponState.setHornLastMelodyId(play.melodyId);
                weaponState.setHornLastMelodyEnhanceTicks(60);
                weaponState.clearHornNotes();
            }
        }

        if (action == WeaponActionType.SPECIAL && player.isShiftKeyDown()) {
            setAction(combatState, "dance", 12);
            WeaponData data = WeaponDataResolver.resolve(player);
            BubbleDef fixed = resolveFixedEchoBubble(data);
            if (fixed != null) {
                spawnEchoBubble(player, fixed.effect, fixed.amplifier, fixed.duration, fixed.radius);
                weaponState.setHornBuffTicks(fixed.duration);
            }
        }
    }

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
        }
    }

    private static void handleChargeBlade(WeaponActionType action, boolean pressed, Player player, PlayerCombatState combatState, PlayerWeaponState weaponState) {
        if (!pressed) {
            return;
        }
        if (action == WeaponActionType.WEAPON) {
            weaponState.setChargeBladeSwordMode(!weaponState.isChargeBladeSwordMode());
            setAction(combatState, "morph", 10);
            return;
        }
        if (action == WeaponActionType.WEAPON_ALT) {
            convertPhialCharge(weaponState);
            setAction(combatState, "charge_phials", 10);
            return;
        }

        if (action == WeaponActionType.SPECIAL) {
            spendStamina(player, weaponState, 12.0f, 20);
            if (weaponState.getChargeBladePhials() <= 0) {
                return;
            }
            weaponState.setChargeBladePhials(0);
            spendStamina(player, weaponState, 8.0f, 15);
        }
    }

    @SuppressWarnings("null")
    private static void handleInsectGlaive(WeaponActionType action, boolean pressed, Player player, PlayerCombatState combatState, PlayerWeaponState weaponState) {
        if (!pressed) {
            return;
        }
        if (action == WeaponActionType.KINSECT_LAUNCH) {
            return;
        }
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
        if (action == WeaponActionType.WEAPON) {
            if (weaponState.getInsectAerialTicks() > 0 && !player.onGround()) {
                setAction(combatState, "aerial_advancing_slash", 10);
                Vec3 forward = player.getLookAngle().normalize().scale(0.65);
                double lift = Math.max(0.25, player.getDeltaMovement().y);
                player.setDeltaMovement(forward.x, lift, forward.z);
                player.hurtMarked = true;
                return;
            }
            int window = WeaponDataResolver.resolveInt(player, null, "comboWindowTicks", 12);
            int lastTick = weaponState.getInsectComboTick();
            int current = weaponState.getInsectComboIndex();
            boolean withinWindow = (player.tickCount - lastTick) <= window;

            // Standard combo progression: cycle LMB -> LMB -> LMB (Rising, Reaping, Double)
            // Finalizer is performed via Alt (handled in WEAPON_ALT branch) as "Overhead Smash" per canonical manual.
            // Continue into next combo step when LMB pressed again within window.

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
        if (action == WeaponActionType.WEAPON_ALT) {
            // If we're in a triple-extract finisher sequence, progress it here
            if (weaponState.getInsectTripleFinisherStage() > 0) {
                int stage = weaponState.getInsectTripleFinisherStage();
                if (stage == 1) {
                    // Tornado Slash executed -> Strong Descending Slash
                    weaponState.setInsectTripleFinisherStage(2);
                    weaponState.setInsectTripleFinisherTicks(40);
                    setAction(combatState, "strong_descending_slash", 18);
                    // Small forward blast for stage feedback
                    if (player.level() instanceof ServerLevel serverLevel) {
                        blastInFront(player, 3.0, 12.0f);
                        Vec3 look = player.getLookAngle().normalize();
                        Vec3 center = player.position().add(look.scale(1.0)).add(0, 0.8, 0);
                        serverLevel.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 12, 0.2, 0.2, 0.2, 0.02);
                    }
                    syncWeaponState(player, weaponState);
                    return;
                } else if (stage == 2) {
                    // Final Rising Spiral Slash -> consume extracts and apply heavy damage
                    weaponState.setInsectTripleFinisherStage(0);
                    weaponState.setInsectTripleFinisherTicks(0);
                    setAction(combatState, "rising_spiral_slash", 24);
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
                    syncWeaponState(player, weaponState);
                    return;
                }
            }

            if (weaponState.getInsectAerialTicks() > 0 && !player.onGround()) {
                setAction(combatState, "aerial_slash", 10);
                Vec3 motion = player.getDeltaMovement();
                player.setDeltaMovement(motion.x, Math.min(-0.4, motion.y), motion.z);
                player.hurtMarked = true;
                return;
            }
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
        if (action == WeaponActionType.SPECIAL) {
            if (player.isShiftKeyDown()) {
                KinsectEntity kinsect = resolveKinsect(player, weaponState);
                if (kinsect != null) {
                    kinsect.recall();
                    setAction(combatState, "kinsect_recall", 10);
                }
            } else {
                if (weaponState.getStamina() < 12.0f) {
                    return;
                }
                spendStamina(player, weaponState, 12.0f, 20);
                setAction(combatState, "vault", 10);
                weaponState.setInsectAerialTicks(40);
                player.setDeltaMovement(player.getDeltaMovement().x, 0.8, player.getDeltaMovement().z);
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

    private static void convertPhialCharge(PlayerWeaponState weaponState) {
        int charge = weaponState.getChargeBladeCharge();
        if (charge < 100) {
            return;
        }
        int phials = weaponState.getChargeBladePhials();
        if (phials >= 5) {
            return;
        }
        weaponState.setChargeBladeCharge(charge - 100);
        weaponState.setChargeBladePhials(phials + 1);
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
        kinsect.launchTo(finalPos, target, targetPos != null ? targetPos : finalPos);
        setAction(combatState, "kinsect_harvest", 10);
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

    private static int[] lastNotes(PlayerWeaponState state, int max) {
        int[] notes = new int[] { state.getHornNoteA(), state.getHornNoteB(), state.getHornNoteC(), state.getHornNoteD(), state.getHornNoteE() };
        int count = Math.min(state.getHornNoteCount(), notes.length);
        int size = Math.min(max, count);
        int[] out = new int[size];
        int start = Math.max(0, count - size);
        for (int i = 0; i < size; i++) {
            out[i] = notes[start + i];
        }
        return out;
    }

    private static SongMatch resolveSongMatch(WeaponData data, PlayerWeaponState state) {
        if (data == null) {
            return null;
        }
        var json = data.getJson();
        if (!json.has("songs") || !json.get("songs").isJsonArray()) {
            return null;
        }
        int queueSize = json.has("noteQueueSize") ? json.get("noteQueueSize").getAsInt() : 5;
        int[] last = lastNotes(state, queueSize);
        var songs = json.getAsJsonArray("songs");
        int melodyId = 0;
        for (int i = 0; i < songs.size(); i++) {
            var song = songs.get(i).getAsJsonObject();
            if (!song.has("pattern") || !song.get("pattern").isJsonArray()) {
                continue;
            }
            int[] pattern = new int[song.getAsJsonArray("pattern").size()];
            for (int p = 0; p < pattern.length; p++) {
                pattern[p] = song.getAsJsonArray("pattern").get(p).getAsInt();
            }
            if (!matchesPattern(last, pattern)) {
                continue;
            }
            melodyId = i + 1;
            String bubbleId = song.has("bubble") ? song.get("bubble").getAsString() : null;
            BubbleDef bubble = resolveBubbleDef(json, bubbleId);
            if (bubble != null) {
                return new SongMatch(melodyId, bubble.effect, bubble.amplifier, bubble.duration, bubble.radius);
            }
        }
        return null;
    }

    private static SongMatch resolveAutoSongMatch(WeaponData data, PlayerWeaponState state) {
        if (data == null) {
            return null;
        }
        var json = data.getJson();
        if (!json.has("songs") || !json.get("songs").isJsonArray()) {
            return null;
        }
        int queueSize = json.has("noteQueueSize") ? Math.max(1, json.get("noteQueueSize").getAsInt()) : 5;
        int[] last = lastNotes(state, queueSize);
        if (last.length == 0) {
            return null;
        }
        var songs = json.getAsJsonArray("songs");
        for (int i = 0; i < songs.size(); i++) {
            var song = songs.get(i).getAsJsonObject();
            if (!song.has("pattern") || !song.get("pattern").isJsonArray()) {
                continue;
            }
            int[] pattern = readPattern(song.getAsJsonArray("pattern"));
            if (pattern.length != 2 && pattern.length != 3 && pattern.length != 4) {
                continue;
            }
            if (!matchesPattern(last, pattern)) {
                continue;
            }
            int melodyId = i + 1;
            String bubbleId = song.has("bubble") ? song.get("bubble").getAsString() : null;
            BubbleDef bubble = resolveBubbleDef(json, bubbleId);
            if (bubble != null) {
                return new SongMatch(melodyId, bubble.effect, bubble.amplifier, bubble.duration, bubble.radius);
            }
        }
        return null;
    }

    private static int[] readPattern(com.google.gson.JsonArray array) {
        int[] pattern = new int[array.size()];
        for (int i = 0; i < pattern.length; i++) {
            pattern[i] = array.get(i).getAsInt();
        }
        return pattern;
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

    @SuppressWarnings({"null", "deprecation"})
    private static BubbleDef resolveBubbleDef(com.google.gson.JsonObject json, String id) {
        if (!json.has("echoBubbles") || !json.get("echoBubbles").isJsonArray()) {
            return null;
        }
        var bubbles = json.getAsJsonArray("echoBubbles");
        for (int i = 0; i < bubbles.size(); i++) {
            var bubble = bubbles.get(i).getAsJsonObject();
            if (id != null && bubble.has("id") && !id.equals(bubble.get("id").getAsString())) {
                continue;
            }
            String effectId = bubble.has("effect") ? bubble.get("effect").getAsString() : "minecraft:speed";
            ResourceLocation effectKey = ResourceLocation.tryParse(effectId);
            if (effectKey == null) {
                continue;
            }
            MobEffect effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(effectKey);
            int amp = bubble.has("amplifier") ? bubble.get("amplifier").getAsInt() : 0;
            int duration = bubble.has("duration") ? bubble.get("duration").getAsInt() : 200;
            float radius = bubble.has("radius") ? bubble.get("radius").getAsFloat() : 3.5f;
            return new BubbleDef(effect, amp, duration, radius);
        }
        return null;
    }

    private static BubbleDef resolveFixedEchoBubble(WeaponData data) {
        if (data == null) {
            return null;
        }
        var json = data.getJson();
        if (json.has("fixedEchoBubble") && json.get("fixedEchoBubble").isJsonPrimitive()) {
            return resolveBubbleDef(json, json.get("fixedEchoBubble").getAsString());
        }
        return resolveBubbleDef(json, null);
    }

    private static MelodyPlay resolveMelodyPlay(WeaponData data, PlayerWeaponState state) {
        if (data == null || state.getHornMelodyCount() <= 0) {
            return null;
        }
        int index = Math.max(0, Math.min(state.getHornMelodyIndex(), state.getHornMelodyCount() - 1));
        int melodyId = state.consumeHornMelodyAt(index);
        if (melodyId <= 0) {
            return null;
        }
        return resolveMelodyById(data, melodyId);
    }

    private static MelodyPlay resolveMelodyById(WeaponData data, int melodyId) {
        if (data == null || melodyId <= 0) {
            return null;
        }
        var json = data.getJson();
        if (!json.has("songs") || !json.get("songs").isJsonArray()) {
            return null;
        }
        var songs = json.getAsJsonArray("songs");
        int idx = melodyId - 1;
        if (idx < 0 || idx >= songs.size()) {
            return null;
        }
        var song = songs.get(idx).getAsJsonObject();
        String songId = song.has("id") ? song.get("id").getAsString() : "melody_" + melodyId;
        String bubbleId = song.has("bubble") ? song.get("bubble").getAsString() : null;
        BubbleDef bubble = resolveBubbleDef(json, bubbleId);
        if (bubble == null) {
            return null;
        }
        return new MelodyPlay(melodyId, songId, bubble.effect, bubble.amplifier, bubble.duration, bubble.radius);
    }

    @SuppressWarnings("null")
    private static void applyMelodyEffect(Player player, String songId, MobEffect effect, int amplifier, int duration, float radius) {
        AABB box = player.getBoundingBox().inflate(radius, 1.0, radius);
        for (Player target : player.level().getEntitiesOfClass(Player.class, box)) {
            if ("healing_medium_depoison".equals(songId)) {
                target.removeEffect(net.minecraft.world.effect.MobEffects.POISON);
            }
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect, duration, amplifier, false, true));
        }
    }

    private static void applyHornSongBuff(PlayerWeaponState state, String songId, int duration) {
        if (songId == null) {
            return;
        }
        switch (songId) {
            case "attack_up_small" -> state.setHornAttackSmallTicks(duration);
            case "attack_up_large" -> state.setHornAttackLargeTicks(duration);
            case "defense_up_large" -> state.setHornDefenseLargeTicks(duration);
            case "melody_hit" -> state.setHornMelodyHitTicks(duration);
            default -> {
            }
        }
    }

    private record BubbleDef(MobEffect effect, int amplifier, int duration, float radius) {
    }

    private record SongMatch(int melodyId, MobEffect effect, int amplifier, int duration, float radius) {
    }

    private record MelodyPlay(int melodyId, String songId, MobEffect effect, int amplifier, int duration, float radius) {
    }


    @SuppressWarnings({"null", "deprecation"})
    private static void spawnEchoBubble(Player player, MobEffect effect, int amplifier, int duration, float radius) {
        var bubble = new org.example.common.entity.EchoBubbleEntity(
                MHWeaponsItems.ECHO_BUBBLE.get(),
                player.level(),
                net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getId(effect),
                amplifier,
                duration,
                radius
        );
        bubble.setPos(player.getX(), player.getY() + 0.1, player.getZ());
        player.level().addFreshEntity(bubble);
    }
}
