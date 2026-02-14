package org.example.common.combat.weapon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.combat.StaminaHelper;
import org.example.common.data.WeaponDataResolver;
import org.example.common.network.ModNetwork;
import org.example.common.network.packet.PlayAttackAnimationS2CPacket;
import org.example.common.network.packet.PlayerWeaponStateS2CPacket;
import org.example.registry.MHAttributes;

/**
 * Dual Blades weapon handler — implements all MH Wilds DB mechanics:
 * <ul>
 *   <li>Normal Mode: Double Slash combo, Lunging Strike</li>
 *   <li>Demon Mode: Demon Fangs combo, Demon Flurry Rush, Blade Dance</li>
 *   <li>Archdemon Mode: Enhanced normals, Demon Flurry</li>
 *   <li>Demon Dodge (faster dash in Demon/Archdemon Mode)</li>
 *   <li>Demon Boost Mode (Perfect Evade reward)</li>
 *   <li>Focus Strike: Turning Tide</li>
 * </ul>
 */
@SuppressWarnings("null")
public final class DualBladesHandler {
    private static final Logger LOGGER = LogManager.getLogger("MHWeaponsMod/DualBlades");

    // Stamina costs
    private static final float DEMON_MODE_DRAIN_PER_TICK = 0.35f;
    private static final float BLADE_DANCE_STAMINA_COST = 15.0f;
    private static final float DEMON_FLURRY_STAMINA_COST = 10.0f;
    private static final float DEMON_DODGE_STAMINA_COST = 18.0f;
    private static final float LUNGING_STRIKE_STAMINA_COST = 8.0f;
    private static final float FOCUS_STRIKE_STAMINA_COST = 20.0f;

    // Gauge values
    private static final float GAUGE_GAIN_PER_HIT = 5.0f;
    private static final float GAUGE_GAIN_PER_HIT_DEMON = 8.0f;
    private static final float GAUGE_ARCHDEMON_THRESHOLD = 100.0f;
    private static final float GAUGE_DECAY_PER_TICK = 0.15f;
    private static final float BLADE_DANCE_GAUGE_COST = 12.0f;
    private static final float DEMON_FLURRY_GAUGE_COST = 8.0f;

    // Demon Boost
    private static final int DEMON_BOOST_DURATION_TICKS = 200; // 10 seconds

    // Blade Dance lock durations (ticks per stage)
    private static final int BLADE_DANCE_I_TICKS = 16;
    private static final int BLADE_DANCE_II_TICKS = 16;
    private static final int BLADE_DANCE_III_TICKS = 22;

    private DualBladesHandler() {
    }

    // ───────────────────────────────────────────────────────────
    // Action Handler (called from WeaponActionHandler)
    // ───────────────────────────────────────────────────────────

    public static void handleAction(WeaponActionType action, boolean pressed, Player player,
                                    PlayerCombatState combatState, PlayerWeaponState weaponState) {
        if (!pressed) {
            return;
        }

        // Block all input while Blade Dance is locked
        if (weaponState.getDbBladeDanceLockTicks() > 0) {
            LOGGER.debug("DB input blocked: Blade Dance lock active ({}t remaining)",
                    weaponState.getDbBladeDanceLockTicks());
            return;
        }

        boolean inDemon = weaponState.isDemonMode();
        boolean inArch = weaponState.isArchDemon();
        int comboWindow = WeaponDataResolver.resolveInt(player, null, "comboWindowTicks", 12);
        int currentTick = player.tickCount;

        switch (action) {
            case SPECIAL -> handleSpecial(player, combatState, weaponState, inDemon, currentTick);
            case WEAPON -> handlePrimaryAttack(player, combatState, weaponState, inDemon, inArch,
                    comboWindow, currentTick);
            case WEAPON_ALT -> handleSecondaryAttack(player, combatState, weaponState, inDemon, inArch,
                    comboWindow, currentTick);
            default -> { }
        }
    }

    // ──── SPECIAL: Toggle Demon Mode ────

    private static void handleSpecial(Player player, PlayerCombatState combatState,
                                      PlayerWeaponState weaponState, boolean inDemon, int currentTick) {
        if (inDemon) {
            // Exit Demon Mode
            weaponState.setDemonMode(false);
            // If gauge is full, enter Archdemon
            if (weaponState.getDemonGauge() >= GAUGE_ARCHDEMON_THRESHOLD) {
                weaponState.setArchDemon(true);
            }
            setAction(combatState, "exit_demon", 8);
            LOGGER.debug("DB: Exiting Demon Mode (gauge={})", weaponState.getDemonGauge());
        } else {
            // Enter Demon Mode - requires stamina
            if (weaponState.getStamina() < 10.0f) {
                return;
            }
            weaponState.setDemonMode(true);
            weaponState.setArchDemon(false); // Archdemon is overridden by full Demon
            resetCombos(weaponState);
            setAction(combatState, "demon_mode", 10);

            // VFX: Demon mode activation burst
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        20, 0.4, 0.3, 0.4, 0.05);
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        player.getX(), player.getY() + 0.8, player.getZ(),
                        10, 0.3, 0.3, 0.3, 0.02);
            }
            LOGGER.debug("DB: Entering Demon Mode");
        }
    }

    // ──── WEAPON (Left Click): Primary Attack Combos ────

    private static void handlePrimaryAttack(Player player, PlayerCombatState combatState,
                                            PlayerWeaponState weaponState, boolean inDemon,
                                            boolean inArch, int comboWindow, int currentTick) {
        // Focus Strike check
        if (combatState.isFocusMode()) {
            handleFocusStrike(player, combatState, weaponState);
            return;
        }

        // Block combo input during active animation
        if (combatState.getActionKeyTicks() > 0) {
            return;
        }

        if (inDemon) {
            // Demon Mode: Demon Fangs -> Twofold Demon Slash -> Sixfold Demon Slash
            int actionTicks = WeaponDataResolver.resolveInt(player, null, "comboActionTicks", 10);
            int lastTick = weaponState.getDbDemonComboTick();
            int current = weaponState.getDbDemonComboIndex();
            int comboIdx = (currentTick - lastTick) > comboWindow ? 0 : (current + 1) % 3;
            weaponState.setDbDemonComboIndex(comboIdx);
            weaponState.setDbDemonComboTick(currentTick + actionTicks);

            String actionKey = switch (comboIdx) {
                case 0 -> "db_demon_fangs";
                case 1 -> "db_twofold_slash";
                default -> "db_sixfold_slash";
            };
            setAction(combatState, actionKey, actionTicks);

            // Small forward lunge per hit
            Vec3 forward = player.getLookAngle().normalize().scale(0.25);
            player.setDeltaMovement(player.getDeltaMovement().add(forward.x, 0, forward.z));
            player.hurtMarked = true;

            LOGGER.debug("DB Demon combo: step={}, action={}", comboIdx, actionKey);
        } else if (inArch) {
            // Archdemon Mode: Enhanced normal attacks (faster, slightly stronger)
            int actionTicks = WeaponDataResolver.resolveInt(player, null, "comboActionTicks", 10);
            int lastTick = weaponState.getDbComboTick();
            int current = weaponState.getDbComboIndex();
            int comboIdx = (currentTick - lastTick) > comboWindow ? 0 : (current + 1) % 3;
            weaponState.setDbComboIndex(comboIdx);
            weaponState.setDbComboTick(currentTick + actionTicks);

            String actionKey = switch (comboIdx) {
                case 0 -> "db_arch_slash_1";
                case 1 -> "db_arch_slash_2";
                default -> "db_arch_slash_3";
            };
            setAction(combatState, actionKey, actionTicks);
        } else {
            // Normal Mode: Double Slash -> Return Stroke -> Circle Slash
            int actionTicks = WeaponDataResolver.resolveInt(player, null, "comboActionTicks", 12);
            int lastTick = weaponState.getDbComboTick();
            int current = weaponState.getDbComboIndex();
            int comboIdx = (currentTick - lastTick) > comboWindow ? 0 : (current + 1) % 3;
            weaponState.setDbComboIndex(comboIdx);
            weaponState.setDbComboTick(currentTick + actionTicks);

            String actionKey = switch (comboIdx) {
                case 0 -> "db_double_slash";
                case 1 -> "db_return_stroke";
                default -> "db_circle_slash";
            };
            setAction(combatState, actionKey, actionTicks);
        }
    }

    // ──── WEAPON_ALT (Right Click): Secondary Attacks ────

    private static void handleSecondaryAttack(Player player, PlayerCombatState combatState,
                                              PlayerWeaponState weaponState, boolean inDemon,
                                              boolean inArch, int comboWindow, int currentTick) {
        // Block combo input during active animation
        if (combatState.getActionKeyTicks() > 0) {
            return;
        }

        if (inDemon) {
            // Demon Mode: Blade Dance (3-stage lock-in combo)
            float staminaCost = StaminaHelper.applyCost(player, BLADE_DANCE_STAMINA_COST);
            if (weaponState.getStamina() < staminaCost) {
                return;
            }

            // Determine Blade Dance stage from demon combo state
            int lastTick = weaponState.getDbDemonComboTick();
            int current = weaponState.getDbDemonComboIndex();
            int stage = (currentTick - lastTick) > comboWindow ? 0 : (current + 1) % 3;
            weaponState.setDbDemonComboIndex(stage);

            int lockTicks;
            String actionKey;
            switch (stage) {
                case 0 -> {
                    actionKey = "db_blade_dance_1";
                    lockTicks = BLADE_DANCE_I_TICKS;
                }
                case 1 -> {
                    actionKey = "db_blade_dance_2";
                    lockTicks = BLADE_DANCE_II_TICKS;
                }
                default -> {
                    actionKey = "db_blade_dance_3";
                    lockTicks = BLADE_DANCE_III_TICKS;
                }
            }

            weaponState.addStamina(-staminaCost);
            weaponState.setStaminaRecoveryDelay(20);
            weaponState.addDemonGauge(-BLADE_DANCE_GAUGE_COST);
            weaponState.setDbBladeDanceLockTicks(lockTicks);
            weaponState.setDbDemonComboTick(currentTick + lockTicks);
            setAction(combatState, actionKey, lockTicks);

            LOGGER.debug("DB Blade Dance stage={}, lockTicks={}", stage, lockTicks);

            // VFX for blade dance
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        player.getX(), player.getY() + 0.8, player.getZ(),
                        5, 0.5, 0.3, 0.5, 0.02);
            }
        } else if (inArch) {
            // Archdemon Mode: Demon Flurry (gauge-consuming multi-hit)
            if (weaponState.getDemonGauge() < DEMON_FLURRY_GAUGE_COST) {
                return;
            }

            int lastTick = weaponState.getDbDemonComboTick();
            int current = weaponState.getDbDemonComboIndex();
            int stage = (currentTick - lastTick) > comboWindow ? 0 : (current + 1) % 2;
            weaponState.setDbDemonComboIndex(stage);

            int actionTicks = 14;
            String actionKey = stage == 0 ? "db_demon_flurry_1" : "db_demon_flurry_2";

            weaponState.addDemonGauge(-DEMON_FLURRY_GAUGE_COST);
            float staminaCost = StaminaHelper.applyCost(player, DEMON_FLURRY_STAMINA_COST);
            weaponState.addStamina(-staminaCost);
            weaponState.setStaminaRecoveryDelay(16);
            weaponState.setDbDemonComboTick(currentTick + actionTicks);
            setAction(combatState, actionKey, actionTicks);

            LOGGER.debug("DB Demon Flurry stage={}", stage);
        } else {
            // Normal Mode: Lunging Strike -> Roundslash
            float staminaCost = StaminaHelper.applyCost(player, LUNGING_STRIKE_STAMINA_COST);
            if (weaponState.getStamina() < staminaCost) {
                return;
            }

            int lastTick = weaponState.getDbComboTick();
            int current = weaponState.getDbComboIndex();
            int stage = (currentTick - lastTick) > comboWindow ? 0 : (current + 1) % 2;
            weaponState.setDbComboIndex(stage);

            int actionTicks = 12;
            String actionKey = stage == 0 ? "db_lunging_strike" : "db_roundslash";

            weaponState.addStamina(-staminaCost);
            weaponState.setStaminaRecoveryDelay(16);
            weaponState.setDbComboTick(currentTick + actionTicks);
            setAction(combatState, actionKey, actionTicks);

            // Forward lunge
            Vec3 forward = player.getLookAngle().normalize().scale(0.6);
            player.setDeltaMovement(forward.x, player.getDeltaMovement().y, forward.z);
            player.hurtMarked = true;
        }
    }

    // ──── Focus Strike: Turning Tide ────

    private static void handleFocusStrike(Player player, PlayerCombatState combatState,
                                          PlayerWeaponState weaponState) {
        float staminaCost = StaminaHelper.applyCost(player, FOCUS_STRIKE_STAMINA_COST);
        if (weaponState.getStamina() < staminaCost) {
            return;
        }

        weaponState.addStamina(-staminaCost);
        weaponState.setStaminaRecoveryDelay(25);
        setAction(combatState, "db_turning_tide", 20);

        // Leap up and forward
        Vec3 forward = player.getLookAngle().normalize().scale(0.8);
        player.setDeltaMovement(forward.x, 0.6, forward.z);
        player.hurtMarked = true;

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    15, 0.3, 0.4, 0.3, 0.1);
        }

        LOGGER.debug("DB Focus Strike: Turning Tide");
    }

    // ───────────────────────────────────────────────────────────
    // Demon Dodge (called from WeaponActionHandler DODGE)
    // ───────────────────────────────────────────────────────────

    /**
     * Handles the Demon Dodge — a faster dash with shorter recovery used in
     * Demon Mode and Archdemon Mode. Returns true if the dodge was handled.
     */
    public static boolean handleDemonDodge(Player player, PlayerCombatState combatState,
                                           PlayerWeaponState weaponState) {
        if (weaponState == null || combatState == null) {
            return false;
        }
        boolean inDemon = weaponState.isDemonMode();
        boolean inArch = weaponState.isArchDemon();
        if (!inDemon && !inArch) {
            return false;
        }

        float cost = StaminaHelper.applyCost(player, DEMON_DODGE_STAMINA_COST);
        if (weaponState.getStamina() < cost) {
            return false;
        }

        weaponState.addStamina(-cost);
        weaponState.setStaminaRecoveryDelay(14);

        // Demon Dodge: faster, with i-frames, shorter recovery
        combatState.setDodgeIFrameTicks(10); // Slightly more i-frames than normal (8)
        combatState.setActionKey("demon_dodge");
        combatState.setActionKeyTicks(6); // Shorter recovery than normal dodge

        double dodgeBonus = player.getAttributeValue(MHAttributes.DODGE_DISTANCE_BONUS.get());
        double dist = 0.8 * Math.max(0.0, 1.0 + dodgeBonus); // Faster dash distance
        Vec3 dash = player.getLookAngle().normalize().scale(dist);
        player.setDeltaMovement(dash.x, player.getDeltaMovement().y + 0.05, dash.z);
        player.hurtMarked = true;

        LOGGER.debug("DB Demon Dodge (inDemon={}, inArch={})", inDemon, inArch);
        return true;
    }

    // ───────────────────────────────────────────────────────────
    // Perfect Evade → Demon Boost Mode (called from DodgeSystem)
    // ───────────────────────────────────────────────────────────

    /**
     * Activates Demon Boost Mode after a successful Perfect Evade in Demon Mode.
     * Grants increased damage and allows attacking during dodges for a limited time.
     */
    public static void activateDemonBoost(Player player, PlayerCombatState combatState,
                                          PlayerWeaponState weaponState) {
        weaponState.setDbDemonBoostTicks(DEMON_BOOST_DURATION_TICKS);
        setAction(combatState, "demon_boost_activate", 5);

        if (player.level() instanceof ServerLevel serverLevel) {
            // Purple/red burst for Demon Boost activation
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    player.getX(), player.getY() + 0.8, player.getZ(),
                    20, 0.4, 0.4, 0.4, 0.15);
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    10, 0.3, 0.3, 0.3, 0.05);
        }

        LOGGER.debug("DB: Demon Boost Mode activated!");
    }

    // ───────────────────────────────────────────────────────────
    // On-Hit Callback (called from WeaponStateEvents.onLivingHurt)
    // ───────────────────────────────────────────────────────────

    /**
     * Called when a Dual Blades player deals damage. Builds Demon Gauge and
     * applies Demon Boost damage multiplier.
     */
    public static void onHit(Player player, PlayerWeaponState weaponState, LivingEntity target, float damage) {
        if (weaponState.isDemonMode()) {
            weaponState.addDemonGauge(GAUGE_GAIN_PER_HIT_DEMON);
        } else {
            weaponState.addDemonGauge(GAUGE_GAIN_PER_HIT);
        }

        // Demon Boost Mode: bonus damage
        if (weaponState.getDbDemonBoostTicks() > 0) {
            float bonus = damage * 0.15f; // 15% bonus
            target.hurt(player.damageSources().playerAttack(player), bonus);
        }
    }

    // ───────────────────────────────────────────────────────────
    // Tick Handler (called from WeaponStateEvents every tick)
    // ───────────────────────────────────────────────────────────

    /**
     * Per-tick update for Dual Blades state — handles stamina drain, gauge decay,
     * mode transitions, blade dance lock, and demon boost timer.
     */
    public static void tick(Player player, PlayerWeaponState state) {
        // Demon Mode stamina drain
        if (state.isDemonMode()) {
            float drain = StaminaHelper.applyCost(player, DEMON_MODE_DRAIN_PER_TICK);
            state.addStamina(-drain);
            state.setStaminaRecoveryDelay(10);

            if (state.getStamina() <= 0.0f) {
                state.setDemonMode(false);
                if (state.getDemonGauge() >= GAUGE_ARCHDEMON_THRESHOLD) {
                    state.setArchDemon(true);
                }
                LOGGER.debug("DB: Demon Mode ended (stamina depleted)");
            }

            // Speed boost + knockback resistance while in Demon Mode
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 40, 1, false, false));

            // Food exhaustion for Demon Mode
            player.causeFoodExhaustion(0.012f);
        }

        // Archdemon Mode: gauge depletes over time
        if (state.isArchDemon() && !state.isDemonMode()) {
            state.addDemonGauge(-GAUGE_DECAY_PER_TICK);
            if (state.getDemonGauge() <= 0.0f) {
                state.setArchDemon(false);
                LOGGER.debug("DB: Archdemon Mode ended (gauge depleted)");
            }
        }

        // Recalculate Archdemon based on gauge
        if (!state.isDemonMode()) {
            boolean shouldArch = state.getDemonGauge() > 0.0f
                    && state.getDemonGauge() >= GAUGE_ARCHDEMON_THRESHOLD * 0.5f;
            // Only set arch if gauge was filled to max at some point (tracked by archDemon flag)
            // Keep arch going as long as gauge > 0
            if (state.isArchDemon() && state.getDemonGauge() <= 0.0f) {
                state.setArchDemon(false);
            }
        }

        // Blade Dance lock tick-down
        if (state.getDbBladeDanceLockTicks() > 0) {
            state.setDbBladeDanceLockTicks(state.getDbBladeDanceLockTicks() - 1);
        }

        // Demon Boost Mode timer
        if (state.getDbDemonBoostTicks() > 0) {
            state.setDbDemonBoostTicks(state.getDbDemonBoostTicks() - 1);

            // Damage boost while demon boost is active
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 40, 0, false, false));

            // Periodic VFX
            if (state.getDbDemonBoostTicks() % 20 == 0 && player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        3, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }

    // ───────────────────────────────────────────────────────────
    // Helpers
    // ───────────────────────────────────────────────────────────

    private static void resetCombos(PlayerWeaponState weaponState) {
        weaponState.setDbComboIndex(0);
        weaponState.setDbComboTick(0);
        weaponState.setDbDemonComboIndex(0);
        weaponState.setDbDemonComboTick(0);
    }

    private static void setAction(PlayerCombatState combatState, String key, int ticks) {
        combatState.setActionKey(key);
        combatState.setActionKeyTicks(ticks);
    }
}
