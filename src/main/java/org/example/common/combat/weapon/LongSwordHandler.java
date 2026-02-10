package org.example.common.combat.weapon;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.combat.StaminaHelper;
import org.example.common.data.WeaponDataResolver;
import org.example.common.network.ModNetwork;
import org.example.common.network.packet.PlayAttackAnimationS2CPacket;
import org.example.registry.MHAttributes;
import org.example.common.util.DebugLogger;

@SuppressWarnings("null")
public final class LongSwordHandler {
    private LongSwordHandler() {
    }

    private static int resolveSpiritLevelMaxTicks(int level) {
        return switch (Math.max(1, Math.min(3, level))) {
            case 1 -> 1200;
            case 2 -> 900;
            default -> 700;
        };
    }

    private static void setAction(PlayerCombatState combatState, String key, int ticks) {
        DebugLogger.logWeapon("LongSword Action: {}, Ticks: {}", key, ticks);
        combatState.setActionKey(key);
        combatState.setActionKeyTicks(ticks);
    }

    private static void spendStamina(Player player, PlayerWeaponState state, float baseCost, int recoveryDelayTicks) {
        if (state == null) {
            return;
        }
        float cost = StaminaHelper.applyCost(player, baseCost);
        state.addStamina(-cost);
        state.setStaminaRecoveryDelay(recoveryDelayTicks);
    }

    @SuppressWarnings("null")
    private static boolean applyLongSwordManualHit(Player player, double range, double radius, float damageMultiplier) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        Vec3 look = player.getLookAngle();
        Vec3 horiz = new Vec3(look.x, 0.0, look.z);
        if (horiz.lengthSqr() < 0.0001) {
            horiz = new Vec3(0.0, 0.0, 1.0);
        }
        horiz = horiz.normalize();
        
        Vec3 start = player.position().add(0.0, 0.9, 0.0);
        Vec3 end = start.add(horiz.scale(range));
        AABB box = new AABB(start, end).inflate(radius, 0.8, radius);
        
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float damage = (float) (Math.max(1.0, base) * damageMultiplier);
        
        LivingEntity target = serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
            .stream()
            .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
            .orElse(null);
            
        if (target != null) {
            target.hurt(player.damageSources().playerAttack(player), damage);
            return true;
        }
        return false;
    }

    @SuppressWarnings("null")
    private static boolean applyLongSwordRoundslashHit(Player player, float damageMultiplier) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float damage = (float) (Math.max(1.0, base) * damageMultiplier);
        double radius = 3.0;
        AABB box = player.getBoundingBox().inflate(radius);
        boolean anyHit = false;
        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)) {
            if (entity.hurt(player.damageSources().playerAttack(player), damage)) {
                anyHit = true;
            }
        }
        return anyHit;
    }

    private static int resolveSpiritChargeStage(int chargeTicks, int maxCharge) {
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

    private static String resolveSpiritBladeAnimation(int stage) {
        return switch (Math.max(1, Math.min(4, stage))) {
            case 1 -> "bettercombat:two_handed_slash_horizontal_left";
            case 2 -> "bettercombat:two_handed_slash_horizontal_right";
            case 3 -> "bettercombat:two_handed_slash_horizontal_left";
            default -> "bettercombat:two_handed_spin";
        };
    }

    /**
     * Handles the reward flow when Foresight Slash successfully counters an incoming hit.
     * Fills Spirit Gauge, performs an immediate Spirit Roundslash to level up on hit, and
     * shifts the action key to the roundslash for HUD/animation sync.
     */
    public static void handleForesightCounterSuccess(Player player, PlayerCombatState combatState, PlayerWeaponState weaponState) {
        if (player == null || combatState == null || weaponState == null) {
            return;
        }
        // Fill gauge to max as counter reward
        weaponState.setSpiritGauge(100.0f);

        // Trigger a free Spirit Roundslash follow-up
        setAction(combatState, "spirit_roundslash", 20);
        float mv = WeaponDataResolver.resolveMotionValue(player, "spirit_roundslash", 1.5f);
        boolean hit = applyLongSwordRoundslashHit(player, mv);
        if (hit) {
            int next = Math.min(3, weaponState.getSpiritLevel() + 1);
            weaponState.setSpiritLevel(next);
            weaponState.setSpiritLevelTicks(resolveSpiritLevelMaxTicks(next));
            weaponState.setSpiritGauge(0.0f); // Traditional LS drains gauge on level-up hit
        }

        // Sync animation for the follow-up swing if server-side
        if (player instanceof ServerPlayer serverPlayer) {
            String animId = resolveSpiritBladeAnimation(4); // roundslash spin
            float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 20.0f);
            float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.4f);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f,
                    "spirit_roundslash", 20));
        }
    }

    public static void handleIaiSpiritCounterSuccess(Player player, PlayerCombatState combatState, PlayerWeaponState weaponState) {
        if (player == null || combatState == null || weaponState == null) {
            return;
        }
        // Level Up on success
        int next = Math.min(3, weaponState.getSpiritLevel() + 1);
        weaponState.setSpiritLevel(next);
        weaponState.setSpiritLevelTicks(resolveSpiritLevelMaxTicks(next));
        weaponState.setSpiritGauge(0.0f);

        // High Damage Hit
        float mv = WeaponDataResolver.resolveMotionValue(player, "iai_spirit_slash", 4.0f);
        // Iai Spirit Slash is a wide cross slash
        applyLongSwordManualHit(player, 3.5, 1.5, mv);

        if (player.level() instanceof ServerLevel serverLevel) {
            // VFX
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT, 
                player.getX(), player.getY() + 1.0, player.getZ(), 
                10, 0.5, 0.5, 0.5, 0.1);
        }
    }

    static void triggerSpiritReleaseSlash(Player player, PlayerCombatState combatState, PlayerWeaponState weaponState) {
        triggerSpiritReleaseSlash(player, combatState, weaponState, "spirit_release_slash", "bettercombat:two_handed_spin");
    }

    static void triggerSpiritReleaseSlash(Player player, PlayerCombatState combatState, PlayerWeaponState weaponState,
            String animKey, String fallbackAnim) {
        if (player == null || combatState == null || weaponState == null) {
            return;
        }
        int current = weaponState.getSpiritLevel();
        if (current <= 0) {
            return;
        }
        int next = Math.max(0, current - 1);
        weaponState.setSpiritLevel(next);
        if (next > 0) {
            weaponState.setSpiritLevelTicks(resolveSpiritLevelMaxTicks(next));
        } else {
            weaponState.setSpiritLevelTicks(0);
        }

        setAction(combatState, "spirit_release_slash", 30);
        weaponState.setLongSwordChargeReady(true);
        // Multi-hit on the nearest target within range
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            Vec3 look = player.getLookAngle();
            Vec3 horiz = new Vec3(look.x, 0.0, look.z);
            if (horiz.lengthSqr() < 0.0001) {
                horiz = new Vec3(0.0, 0.0, 1.0);
            }
            horiz = horiz.normalize();
            Vec3 start = player.position().add(0.0, 0.9, 0.0);
            Vec3 end = start.add(horiz.scale(3.2));
            AABB box = new AABB(start, end).inflate(1.2, 0.8, 1.2);
            double base = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            float damage = (float) (Math.max(1.0, base) * 1.2f);
            LivingEntity target = serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                .stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                .orElse(null);
            if (target != null) {
                for (int i = 0; i < 3; i++) {
                    target.hurt(player.damageSources().playerAttack(player), damage);
                }
            }
        } else {
            applyLongSwordManualHit(player, 3.2, 2.2, 3.5f);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            String animId = WeaponDataResolver.resolveString(player, "animationOverrides", animKey, fallbackAnim);
            float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 30.0f);
            float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.5f);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f,
                    "spirit_release_slash", 30));
        }
    }

    static void handleChargeRelease(Player player, PlayerCombatState combatState, PlayerWeaponState weaponState,
            int chargeTicks, int maxCharge) {
        if (player == null || combatState == null || weaponState == null) {
            return;
        }

        // Prevent performing Spirit Combo if in Spirit Helm Breaker
        if ("spirit_helm_breaker".equals(combatState.getActionKey())) {
            return;
        }

        int stage = resolveSpiritChargeStage(chargeTicks, maxCharge);
        int tapThreshold = 10;
        if (chargeTicks < tapThreshold) {
            int window = 100; // Match extended combo window
            int lastTick = weaponState.getLongSwordSpiritComboTick();
            int current = weaponState.getLongSwordSpiritComboIndex();
            boolean timeout = (player.tickCount - lastTick) > window;

            int nextStep = timeout ? 0 : (current + 1);
            if (nextStep > 3) nextStep = 0;

            float cost = (nextStep == 3) ? 10.0f : 5.0f;

            if (weaponState.getSpiritGauge() >= cost) {
                weaponState.addSpiritGauge(-cost);
                weaponState.setLongSwordSpiritComboIndex(nextStep);
                weaponState.setLongSwordSpiritComboTick(player.tickCount);

                String actionKey = switch (nextStep) {
                    case 1 -> "spirit_blade_2";
                    case 2 -> "spirit_blade_3";
                    case 3 -> "spirit_roundslash";
                    default -> "spirit_blade_1";
                };
                int actionTicks = actionKey.equals("spirit_roundslash") ? 20 : 12;
                setAction(combatState, actionKey, actionTicks);

                float mv = WeaponDataResolver.resolveMotionValue(player, actionKey, 1.0f);
                if (nextStep == 3) {
                    if (applyLongSwordRoundslashHit(player, mv)) {
                        int level = weaponState.getSpiritLevel();
                        int nextLevel = Math.min(3, level + 1);
                        weaponState.setSpiritLevel(nextLevel);
                        weaponState.setSpiritLevelTicks(resolveSpiritLevelMaxTicks(nextLevel));
                        weaponState.setSpiritGauge(0.0f);
                    }
                } else {
                    applyLongSwordManualHit(player, 1.6, 1.4, mv);
                }

                if (player instanceof ServerPlayer serverPlayer) {
                    String animId = resolveSpiritBladeAnimation(nextStep + 1);
                    float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 16.666666f);
                    float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.25f);
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f, actionKey, actionTicks));
                }
            }
            weaponState.setLongSwordChargeReady(true);
            return;
        }
        // Charge Logic: Gauge is built dynamically in ActionKeyTickEvents
        // weaponState.addSpiritGauge(stage * 25.0f);
        
        // Reset combo index if a full charge attack is used, or treat it as opening?
        // Let's treat it as an opening if Charge is used, but if it ends in roundslash (stage 4) it levels up.
        // If stage < 4, it shouldn't break the combo if we want to seamlessly mix them, 
        // but traditionally charge is its own branch.
        // For now, setting index to match the "Spirit Blade I/II/III" equivalence so next tap continues combo.
        weaponState.setLongSwordSpiritComboIndex(stage >= 4 ? 3 : (stage - 1));
        weaponState.setLongSwordSpiritComboTick(player.tickCount);
        String actionKey = switch (stage) {
            case 1 -> "spirit_blade_1";
            case 2 -> "spirit_blade_2";
            case 3 -> "spirit_blade_3";
            default -> "spirit_roundslash";
        };
        int actionTicks = actionKey.equals("spirit_roundslash") ? 20 : 10;
        setAction(combatState, actionKey, actionTicks);

        float motionValue = WeaponDataResolver.resolveMotionValue(player, actionKey, 1.0f);
        if (stage >= 4) {
            if (applyLongSwordRoundslashHit(player, motionValue)) {
                int next = Math.min(3, weaponState.getSpiritLevel() + 1);
                weaponState.setSpiritLevel(next);
                weaponState.setSpiritLevelTicks(resolveSpiritLevelMaxTicks(next));
                weaponState.setSpiritGauge(0.0f);
            }
        } else {
            applyLongSwordManualHit(player, 1.6, 1.4, motionValue);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            String animId = resolveSpiritBladeAnimation(stage);
            float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 16.666666f);
            float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.25f);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f, actionKey, actionTicks));
        }

        // Spinning Crimson Slash (Red Gauge Follow-up)
        if (stage >= 4 && weaponState.getSpiritLevel() >= 3) {
             setAction(combatState, "spinning_crimson_ready", 15);
             // Logic handled in handleAction via WEAPON input during this window
        } else {
             weaponState.setLongSwordChargeReady(true);
        }
    }

    public static void handleAction(WeaponActionType action, boolean pressed, Player player, PlayerCombatState combatState, PlayerWeaponState weaponState) {
        handleAction(action, pressed, player, combatState, weaponState, 0.0f, 0.0f);
    }

    public static void handleAction(WeaponActionType action, boolean pressed, Player player, PlayerCombatState combatState, PlayerWeaponState weaponState, float inputX, float inputZ) {
        if (!pressed) {
            return;
        }
        if (weaponState == null) {
            return;
        }
        if (weaponState.isChargingAttack()) {
            return;
        }
        String currentAction = combatState.getActionKey();
        
        if ("spinning_crimson_ready".equals(currentAction)) {
            if (action == WeaponActionType.WEAPON) {
                 setAction(combatState, "spinning_crimson_slash", 25);
                 applyLongSwordRoundslashHit(player, 2.2f); // High damage
                 if (player instanceof ServerPlayer serverPlayer) {
                    String animId = WeaponDataResolver.resolveString(player, "animationOverrides", "spinning_crimson_slash",
                        "bettercombat:two_handed_draw");
                    float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 25.0f);
                    float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.4f);
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f,
                            "spinning_crimson_slash", 25));
                 }
            }
            return;
        }

        // Handle Foresight Slash (DODGE during combo)
        // Check if player is currently performing an action that allows a foresight cancel
        boolean isAttacking = currentAction != null && combatState.getActionKeyTicks() > 0;
        if (action == WeaponActionType.DODGE) {
             if (isAttacking && weaponState.getSpiritGauge() >= 10.0f) {
                 // Foresight Slash
                 // Requires simple check: Is attacking? Yes. 
                 // Cost: Consumes all spirit gauge (or significant chunk if not full logic yet)
                 // We will drain it in the event handler or here. For now mimicking consumption.
                 // Manual says consumes entire gauge.
                 
                 weaponState.setSpiritGauge(0.0f); 
                 spendStamina(player, weaponState, 20.0f, 20);
                 
                 // Backward evasion
                 Vec3 dash = player.getLookAngle().normalize().scale(-0.8);
                 player.setDeltaMovement(dash.x, player.getDeltaMovement().y + 0.1, dash.z);
                 player.hurtMarked = true;
                 
                 setAction(combatState, "foresight_slash", 20);
                 
                 // I-Frame setup
                 combatState.setDodgeIFrameTicks(12); // Generous i-frames
                 
                 if (player instanceof ServerPlayer serverPlayer) {
                    String animId = WeaponDataResolver.resolveString(player, "animationOverrides", "foresight_slash",
                        "bettercombat:two_handed_slash_horizontal_left"); // Placeholder animation until official assets
                     float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 20.0f);
                     float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.3f);
                     ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f,
                            "foresight_slash", 20));
                 }
                 weaponState.setLongSwordChargeReady(true);
                 return;
             }
             // Allow normal dodge if not valid context
             return;
        }

        if ("spirit_helm_breaker".equals(currentAction) || "spirit_thrust".equals(currentAction)) {
            return;
        }

        // Ignore new inputs while Spirit Thrust / Helm Breaker / follow-up swings are in progress
        if (("spirit_thrust".equals(currentAction)
            || "spirit_helm_breaker".equals(currentAction)
            || "spirit_helm_breaker_followup_left".equals(currentAction)
            || "spirit_helm_breaker_followup_right".equals(currentAction))
                && combatState.getActionKeyTicks() > 0) {
            return;
        }
        if (weaponState.getLongSwordThrustLockTicks() > 0) {
            return;
        }
        if ("helm_breaker_followup".equals(currentAction)) {
            // Only accept WEAPON_ALT inputs (Spirit Release Slash)
            if (action == WeaponActionType.WEAPON_ALT && weaponState.getSpiritLevel() > 0) {
                 // Spirit Release Slash (Wilds - Red Gauge Finisher)
                 weaponState.setLongSwordHelmBreakerFollowupTicks(0);
                 triggerSpiritReleaseSlash(player, combatState, weaponState);
                 return;
            }
            // Block other inputs
            return;
        }
        
        // LMB (Attack) -> Now mapped to Thrust / Rising Slash in docs, but Spirit Combo in current logical hints?
        // Wait, ClientForgeEvents hints maps Attack Key to SpiritCombo.
        // But here we receive "WEAPON" action for 'X' key, and "WEAPON_ALT" for 'C' key.
        // And we receive SpiritCombo via `handleCharge` logic or Spirit Hints?
        // The hints in ClientForgeEvents only set the ActionKey string, they don't trigger server logic directly?
        // No, ClientForgeEvents sends `WeaponActionC2SPacket(WeaponActionType.WEAPON, true)` etc.

        // WEAPON (X) -> Thrust / Rising Slash Combo
        // WEAPON_ALT (C) / RMB -> Overhead Slash Combo
        
        if (action == WeaponActionType.WEAPON) {
            weaponState.setLongSwordChargeReady(true);
            int window = WeaponDataResolver.resolveInt(player, null, "comboWindowTicks", 18);
            int lastTick = weaponState.getLongSwordThrustComboTick();
            int current = weaponState.getLongSwordThrustComboIndex();
            int next = (player.tickCount - lastTick) > window ? 0 : (current + 1) % 2;
            weaponState.setLongSwordThrustComboIndex(next);
            weaponState.setLongSwordThrustComboTick(player.tickCount);

            if (next == 0) {
                setAction(combatState, "thrust_rising_slash", 12);
                if (applyLongSwordManualHit(player, 1.75, 1.0, 0.9f)) {
                    weaponState.addSpiritGauge(3.0f); // Generate Gauge only on Hit
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    String animId = WeaponDataResolver.resolveString(player, "animationOverrides", "thrust_rising_slash",
                        "bettercombat:two_handed_stab_right");
                    float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 16.666666f);
                    float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.25f);
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f,
                            "thrust_rising_slash", 12));
                }
            } else {
                 setAction(combatState, "thrust_rising_slash", 12); // Using same key for HUD simplicity, or distinct if needed
                 if (applyLongSwordManualHit(player, 1.0, 1.5, 0.9f)) {
                     weaponState.addSpiritGauge(3.0f); // Generate Gauge only on Hit
                 }
                 if (player instanceof ServerPlayer serverPlayer) {
                     // Alternate animation for rising slash part
                     String animId = WeaponDataResolver.resolveString(player, "animationOverrides", "thrust_rising_slash_2",
                         "bettercombat:two_handed_slash_vertical_left");
                     float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 16.666666f);
                     float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.25f);
                     ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                         new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, -1.0f,
                             "thrust_rising_slash_2", 12));
                 }
            }
            return;
        }

        if (action == WeaponActionType.WEAPON_ALT && player.isShiftKeyDown()) {
            setAction(combatState, "fade_slash", 10);
            if (applyLongSwordManualHit(player, 1.6, 1.4, 1.0f)) { // Use manual hit instead of relying on default for gauge
                weaponState.addSpiritGauge(6.0f); // Generate Gauge only on Hit
            }
            if (player instanceof ServerPlayer serverPlayer) {
                String animId = WeaponDataResolver.resolveString(player, "animationOverrides", "fade_slash",
                    "bettercombat:two_handed_slash_horizontal_right");
                 float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 20.0f);
                 float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.3f);
                 ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f,
                        "fade_slash", 10));
            }
            // Direction-aware Fade Slash (backward default, lateral if strafing)
            Vec3 look = player.getLookAngle();
            Vec3 horiz = new Vec3(look.x, 0.0, look.z);
            if (horiz.lengthSqr() < 0.0001) {
                horiz = new Vec3(0.0, 0.0, 1.0);
            }
            horiz = horiz.normalize();
            Vec3 left = new Vec3(horiz.z, 0.0, -horiz.x);
            Vec3 right = new Vec3(-horiz.z, 0.0, horiz.x);
            // Check input from packet first, fall back to entity state
            float strafe = (Math.abs(inputX) > 0.01f) ? inputX : player.xxa;
            
            double dodgeDistanceMultiplier = 1.0 + player.getAttributeValue(MHAttributes.DODGE_DISTANCE_BONUS.get());
            double fadeDistance = 0.6 * Math.max(0.0, dodgeDistanceMultiplier);
            Vec3 dash;
            if (strafe > 0.15f) {
                 // Left input -> Move Left
                dash = left.scale(fadeDistance);
            } else if (strafe < -0.15f) {
                 // Right input -> Move Right
                dash = right.scale(fadeDistance);
            } else {
                dash = horiz.scale(-fadeDistance);
            }
            player.setDeltaMovement(dash.x, player.getDeltaMovement().y + 0.12, dash.z);
            player.hurtMarked = true;
            combatState.setDodgeIFrameTicks(Math.max(combatState.getDodgeIFrameTicks(), 6));
            weaponState.setLongSwordFadeSlashTicks(12);
            return;
        }

        if (action == WeaponActionType.WEAPON_ALT) {
            weaponState.setLongSwordChargeReady(true);
            int window = WeaponDataResolver.resolveInt(player, null, "comboWindowTicks", 12);
            int lastTick = weaponState.getLongSwordOverheadComboTick();
            int current = weaponState.getLongSwordOverheadComboIndex();
            int next = (player.tickCount - lastTick) > window ? 0 : (current + 1) % 3;
            weaponState.setLongSwordOverheadComboIndex(next);
            weaponState.setLongSwordOverheadComboTick(player.tickCount);
            String animName;
            if (next == 0) {
                animName = "overhead_slash";
                setAction(combatState, animName, 10);
                if (applyLongSwordManualHit(player, 1.4, 1.5, 1.2f)) {
                    weaponState.addSpiritGauge(5.0f); // Generate Gauge only on Hit
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    String animId = WeaponDataResolver.resolveString(player, "animationOverrides", "overhead_slash",
                        "bettercombat:two_handed_slash_vertical_right");
                    float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 16.66f);
                    float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.3f);
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f, animName, 10));
                }
            } else if (next == 1) {
                animName = "overhead_stab";
                setAction(combatState, animName, 10);
                if (applyLongSwordManualHit(player, 1.16, 0.8, 1.0f)) {
                    weaponState.addSpiritGauge(4.0f); // Generate Gauge only on hit
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    String animId = WeaponDataResolver.resolveString(player, "animationOverrides", "overhead_stab",
                        "bettercombat:two_handed_stab_right");
                    float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 16.66f);
                    float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.3f);
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f, animName, 10));
                }
            } else {
                animName = "rising_slash";
                setAction(combatState, animName, 12);
                if (applyLongSwordManualHit(player, 1.0, 2.5, 1.0f)) {
                    weaponState.addSpiritGauge(4.0f); // Generate Gauge only on Hit
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    String animId = WeaponDataResolver.resolveString(player, "animationOverrides", "rising_slash",
                        "bettercombat:two_handed_slash_vertical_left");
                    float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 20.0f);
                    float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.3f);
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, -1.0f, animName, 12));
                }
            }
            // Animation triggers for these could be added if missing from default BetterCombat mapping, 
            // but usually they map to Swing 1, 2, 3 automatically.
            return;
        }
        
        if (weaponState.isLongSwordSpecialSheathe()) {
            if (action == WeaponActionType.WEAPON) {
                // Iai Slash
                weaponState.setLongSwordSpecialSheathe(false);
                spendStamina(player, weaponState, 10.0f, 20);
                weaponState.addSpiritGauge(10.0f); // Auto regen buff usually, but here immediate gain
                setAction(combatState, "iai_slash", 12);
                weaponState.setLongSwordChargeReady(true);
                return;
            } else if (action == WeaponActionType.WEAPON_ALT) {
                // Iai Spirit Slash (Counter)
                weaponState.setLongSwordSpecialSheathe(false);
                spendStamina(player, weaponState, 12.0f, 20);
                if (weaponState.getSpiritLevel() > 0) {
                    // Consumes level if missed, but we'll implement counter logic in events
                    // For now just the move
                    setAction(combatState, "iai_spirit_slash", 16);
                    combatState.setDodgeIFrameTicks(8); // brief counter window
                } else {
                    setAction(combatState, "iai_slash", 12); // Fallback
                }
                weaponState.setLongSwordChargeReady(true);
                return;
            }
            // Cancel sheathe if moving or other action?
            weaponState.setLongSwordSpecialSheathe(false);
        }

        if (action == WeaponActionType.SPECIAL && player.isShiftKeyDown()) {
            // Special Sheathe Entry
            spendStamina(player, weaponState, 8.0f, 15);
            double sheatheSpeedBonus = player.getAttributeValue(MHAttributes.SHEATHE_SPEED_BONUS.get());
            double sheatheSpeedMultiplier = Math.max(0.1D, 1.0D + sheatheSpeedBonus);
            int sheatheActionTicks = (int) Math.max(6, Math.round(20.0D / sheatheSpeedMultiplier));
            int sheatheWindowTicks = (int) Math.max(20, Math.round(100.0D / sheatheSpeedMultiplier));
            weaponState.setLongSwordSpecialSheathe(true);
            weaponState.setLongSwordSheatheTicks(sheatheWindowTicks);
            setAction(combatState, "special_sheathe", sheatheActionTicks);
            return;
        }

        // Focus Strike (Shift + Attack)
        if (action == WeaponActionType.WEAPON && player.isShiftKeyDown()) {
             // Unbound Thrust
             if (weaponState.getSpiritGauge() >= 15.0f) {
                 weaponState.addSpiritGauge(-15.0f);
                 setAction(combatState, "focus_strike", 20);
                 
                 // Dash + Hit
                 Vec3 dash = player.getLookAngle().normalize().scale(1.2);
                 player.setDeltaMovement(dash.x, player.getDeltaMovement().y + 0.2, dash.z);
                 player.hurtMarked = true;
                 
                 applyLongSwordManualHit(player, 2.0, 1.0, 1.5f);
                 
                 // Level up on hit? (Simplified to always lvl up for now/testing)
                 int currentLv = weaponState.getSpiritLevel();
                 if (currentLv < 3) {
                     weaponState.setSpiritLevel(currentLv + 1);
                     weaponState.setSpiritLevelTicks(resolveSpiritLevelMaxTicks(currentLv + 1));
                 }
                 
                 if (player instanceof ServerPlayer serverPlayer) {
                    String animId = WeaponDataResolver.resolveString(player, "animationOverrides", "focus_strike",
                        "bettercombat:two_handed_stab_left");
                    float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 20.0f);
                    float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.3f);
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f,
                            "focus_strike", 20));
                 }
                 weaponState.setLongSwordChargeReady(true);
             }
             return;
        }

        if (action == WeaponActionType.SPECIAL) {
            if (weaponState.getSpiritLevel() <= 0) {
                return; // Spirit level required (white/yellow/red)
            }
            if (weaponState.getLongSwordHelmBreakerCooldown() > 0) {
                return; // Cooldown active
            }
            // Capture facing for the dash
            Vec3 look = player.getLookAngle();
            Vec3 horiz = new Vec3(look.x, 0.0, look.z);
            if (horiz.lengthSqr() > 0.0001) {
                horiz = horiz.normalize();
                weaponState.setLongSwordHelmBreakerDir(horiz);
            }
            setAction(combatState, "spirit_thrust", 12);
            weaponState.setLongSwordChargeReady(true);
            if (player instanceof ServerPlayer serverPlayer) {
                String animId = WeaponDataResolver.resolveString(player, "animationOverrides", "spirit_thrust",
                    "bettercombat:two_handed_stab_right");
                float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 16.666666f);
                float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.25f);
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f,
                        "spirit_thrust", 12));
            }
        }
    }
}
