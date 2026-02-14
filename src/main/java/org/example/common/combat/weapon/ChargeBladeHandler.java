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
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.combat.StaminaHelper;
import org.example.common.data.WeaponDataResolver;
import org.example.common.network.ModNetwork;
import org.example.common.network.packet.PlayAttackAnimationS2CPacket;
import org.example.common.network.packet.PlayerWeaponStateS2CPacket;

import java.util.List;

/**
 * Charge Blade handler — MHWilds-accurate implementation.
 * <p>
 * Manages Sword Energy / Phial system, two mode combo chains (Sword & Shield / Axe),
 * Shield Charge (Element Boost), Sword Boost, Power Axe Mode, Guard Points,
 * Element Discharge chain (ED I → ED II → AED/SAED), and Elemental Roundslash cancel.
 */
@SuppressWarnings("null")
public final class ChargeBladeHandler {
    private ChargeBladeHandler() {}

    // ── helpers ─────────────────────────────────────────────────────────

    private static void setAction(PlayerCombatState combatState, String key, int ticks) {
        combatState.setActionKey(key);
        combatState.setActionKeyTicks(ticks);
    }

    private static void spendStamina(Player player, PlayerWeaponState state, float baseCost, int recoveryDelay) {
        if (state == null) return;
        float cost = StaminaHelper.applyCost(player, baseCost);
        state.addStamina(-cost);
        state.setStaminaRecoveryDelay(recoveryDelay);
    }

    private static void syncState(Player player, PlayerWeaponState state) {
        if (player instanceof ServerPlayer sp) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new PlayerWeaponStateS2CPacket(sp.getId(), state.serializeNBT()));
        }
    }

    private static void playAnimation(Player player, String animationKey, String actionKey, int actionKeyTicks) {
        if (player instanceof ServerPlayer sp) {
            float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 14.0f);
            float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.3f);
            ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> sp),
                    new PlayAttackAnimationS2CPacket(sp.getId(), animationKey, length, upswing, 1.0f, actionKey, actionKeyTicks));
        }
    }

    private static boolean applyHit(Player player, double range, double radius, float damageMultiplier) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return false;
        Vec3 look = player.getLookAngle();
        Vec3 horiz = new Vec3(look.x, 0.0, look.z);
        if (horiz.lengthSqr() < 0.0001) horiz = new Vec3(0.0, 0.0, 1.0);
        horiz = horiz.normalize();
        Vec3 start = player.position().add(0.0, 0.9, 0.0);
        Vec3 end = start.add(horiz.scale(range));
        AABB box = new AABB(start, end).inflate(radius, 0.8, radius);
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float dmg = (float) Math.max(1.0, base * damageMultiplier);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive());
        for (LivingEntity entity : targets) {
            entity.hurt(player.damageSources().playerAttack(player), dmg);
        }
        return !targets.isEmpty();
    }

    // ── combo window ────────────────────────────────────────────────────

    private static int comboWindow(Player player) {
        return WeaponDataResolver.resolveInt(player, null, "comboWindowTicks", 20);
    }

    private static boolean comboExpired(Player player, PlayerWeaponState state) {
        return (player.tickCount - state.getCbComboTick()) > comboWindow(player);
    }

    private static void advanceCombo(Player player, PlayerWeaponState state, int maxIndex) {
        int next = comboExpired(player, state) ? 0 : state.getCbComboIndex() + 1;
        if (next > maxIndex) next = 0;
        state.setCbComboIndex(next);
        state.setCbComboTick(player.tickCount);
    }

    // ── main dispatch ───────────────────────────────────────────────────

    public static void handleAction(WeaponActionType action, boolean pressed,
                                     Player player, PlayerCombatState combatState,
                                     PlayerWeaponState weaponState) {
        if (weaponState == null || combatState == null) return;

        boolean swordMode = weaponState.isChargeBladeSwordMode();

        switch (action) {
            case WEAPON -> handleWeaponAction(pressed, player, combatState, weaponState, swordMode);
            case WEAPON_ALT -> handleAltAction(pressed, player, combatState, weaponState, swordMode);
            case SPECIAL -> handleSpecialAction(pressed, player, combatState, weaponState, swordMode);
            case CHARGE -> handleChargeAction(pressed, player, combatState, weaponState, swordMode);
            default -> {}
        }

        syncState(player, weaponState);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  WEAPON (Left Click — primary combo chain)
    // ═══════════════════════════════════════════════════════════════════

    private static void handleWeaponAction(boolean pressed, Player player,
                                            PlayerCombatState combatState,
                                            PlayerWeaponState state,
                                            boolean swordMode) {
        if (!pressed) return;

        // Block combo input during active animation
        if (combatState.getActionKeyTicks() > 0) {
            return;
        }

        if (swordMode) {
            handleSwordCombo(player, combatState, state);
        } else {
            handleAxeCombo(player, combatState, state);
        }
    }

    /**
     * Sword combo: Sword: Weak Slash → Sword: Return Stroke → Sword: Roundslash (loop).
     * Roundslash end has a Guard Point window.
     * Builds Sword Energy on hit.
     */
    private static void handleSwordCombo(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        advanceCombo(player, state, 2);
        int idx = state.getCbComboIndex();

        float mv;
        String actionKey;
        String animKey;

        switch (idx) {
            case 0 -> {
                mv = WeaponDataResolver.resolveFloat(player, "motionValues", "cb_sword_weak_slash", 0.8f);
                actionKey = "cb_sword_weak_slash";
                animKey = "bettercombat:one_handed_slash_horizontal_right";
            }
            case 1 -> {
                mv = WeaponDataResolver.resolveFloat(player, "motionValues", "cb_sword_return_stroke", 0.85f);
                actionKey = "cb_sword_return_stroke";
                animKey = "bettercombat:one_handed_slash_horizontal_left";
            }
            default -> {
                mv = WeaponDataResolver.resolveFloat(player, "motionValues", "cb_sword_roundslash", 1.0f);
                actionKey = "cb_sword_roundslash";
                animKey = "bettercombat:one_handed_slash_vertical_right";
                // Roundslash end → Guard Point (6 ticks)
                state.setCbGuardPointTicks(6);
            }
        }

        boolean hit = applyHit(player, 2.5, 0.8, mv);
        if (hit) {
            addSwordEnergy(player, state, 15);
            // Sword Boost: add phial explosion on hit
            if (state.isCbSwordBoosted()) {
                applyPhialExplosion(player, state);
            }
        }

        // Reset discharge stage since we're doing basic sword combo
        state.setCbDischargeStage(0);

        setAction(combatState, actionKey, 10);
        state.setCbComboTick(player.tickCount + 10);
        playAnimation(player, animKey, actionKey, 10);
    }

    /**
     * Axe combo: Axe: Rising Slash → Axe: Overhead Slash (loop).
     * Power Axe Mode: multi-tick hits.
     */
    private static void handleAxeCombo(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        advanceCombo(player, state, 1);
        int idx = state.getCbComboIndex();

        float mv;
        String actionKey;
        String animKey;

        switch (idx) {
            case 0 -> {
                mv = WeaponDataResolver.resolveFloat(player, "motionValues", "cb_axe_rising_slash", 1.0f);
                actionKey = "cb_axe_rising_slash";
                animKey = "bettercombat:two_handed_slash_vertical_right";
            }
            default -> {
                mv = WeaponDataResolver.resolveFloat(player, "motionValues", "cb_axe_overhead_slash", 1.1f);
                actionKey = "cb_axe_overhead_slash";
                animKey = "bettercombat:two_handed_slash_vertical_left";
            }
        }

        boolean hit = applyHit(player, 3.5, 1.0, mv);

        // Power Axe: extra tick damage on hit
        if (hit && state.isCbPowerAxe()) {
            applyPowerAxeTick(player, state);
        }

        // Reset discharge stage
        state.setCbDischargeStage(0);

        setAction(combatState, actionKey, 12);
        state.setCbComboTick(player.tickCount + 12);
        playAnimation(player, animKey, actionKey, 12);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  WEAPON_ALT (X Key — context-sensitive: Shield Thrust / Discharge)
    // ═══════════════════════════════════════════════════════════════════

    private static void handleAltAction(boolean pressed, Player player,
                                         PlayerCombatState combatState,
                                         PlayerWeaponState state,
                                         boolean swordMode) {
        if (!pressed) return;

        if (swordMode) {
            // Sword: Shield Thrust → can chain into AED/SAED via SPECIAL
            handleShieldThrust(player, combatState, state);
        } else {
            // Axe: Element Discharge chain (ED I → ED II → AED/SAED)
            handleDischargeChain(player, combatState, state);
        }
    }

    /**
     * Sword: Shield Thrust — Quick bash. Sets up for AED/SAED follow-up.
     */
    private static void handleShieldThrust(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        float mv = WeaponDataResolver.resolveFloat(player, "motionValues", "cb_shield_thrust", 0.6f);
        boolean hit = applyHit(player, 2.0, 0.7, mv);
        if (hit) {
            addSwordEnergy(player, state, 10);
        }

        // Prep for discharge follow-up: set discharge stage to 1 so SPECIAL triggers AED
        state.setCbDischargeStage(1);

        setAction(combatState, "cb_shield_thrust", 8);
        playAnimation(player, "bettercombat:one_handed_slam_light", "cb_shield_thrust", 8);
    }

    /**
     * Axe: Element Discharge chain.
     * Stage 0/reset → ED I (1 phial)
     * Stage 1 → ED II (1 phial)
     * Stage 2 → AED or SAED (if shield charged, spends all phials)
     */
    private static void handleDischargeChain(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        // Block discharge chain input during active animation
        if (combatState.getActionKeyTicks() > 0) {
            return;
        }

        int stage = state.getCbDischargeStage();

        if (comboExpired(player, state)) {
            stage = 0;
        }

        if (stage <= 0) {
            // Axe: Element Discharge I
            handleElementDischarge1(player, combatState, state);
        } else if (stage == 1) {
            // Axe: Element Discharge II
            handleElementDischarge2(player, combatState, state);
        } else {
            // Axe: Amped Element Discharge (AED) or Super (SAED)
            handleAmpedElementDischarge(player, combatState, state);
        }
    }

    /**
     * Axe: Element Discharge I — Forward dash side chop, consumes 1 phial.
     */
    private static void handleElementDischarge1(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        if (state.getChargeBladePhials() <= 0) {
            // No phials: do a basic axe slash instead
            handleAxeCombo(player, combatState, state);
            return;
        }

        state.setChargeBladePhials(state.getChargeBladePhials() - 1);
        float mv = WeaponDataResolver.resolveFloat(player, "motionValues", "cb_element_discharge_1", 1.2f);
        boolean hit = applyHit(player, 3.0, 1.0, mv);

        if (hit) {
            applyPhialExplosion(player, state);
        }
        if (state.isCbPowerAxe() && hit) {
            applyPowerAxeTick(player, state);
        }

        // Forward lunge
        Vec3 look = player.getLookAngle();
        player.push(look.x * 0.3, 0.0, look.z * 0.3);

        state.setCbDischargeStage(1);
        state.setCbComboTick(player.tickCount + 12);
        setAction(combatState, "cb_element_discharge_1", 12);
        playAnimation(player, "bettercombat:two_handed_slash_horizontal_right", "cb_element_discharge_1", 12);
    }

    /**
     * Axe: Element Discharge II — Double swing (2 hits), consumes 1 phial.
     */
    private static void handleElementDischarge2(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        if (state.getChargeBladePhials() <= 0) {
            handleAxeCombo(player, combatState, state);
            return;
        }

        state.setChargeBladePhials(state.getChargeBladePhials() - 1);
        float mv = WeaponDataResolver.resolveFloat(player, "motionValues", "cb_element_discharge_2", 1.3f);
        boolean hit = applyHit(player, 3.0, 1.0, mv);

        if (hit) {
            applyPhialExplosion(player, state);
        }
        if (state.isCbPowerAxe() && hit) {
            applyPowerAxeTick(player, state);
        }

        state.setCbDischargeStage(2);
        state.setCbComboTick(player.tickCount + 14);
        setAction(combatState, "cb_element_discharge_2", 14);
        playAnimation(player, "bettercombat:two_handed_slash_horizontal_left", "cb_element_discharge_2", 14);
    }

    /**
     * Axe: Amped Element Discharge (AED) — Big overhead slam, consumes 1 phial.
     * If Shield is Charged → Super Amped Element Discharge (SAED) — consumes ALL phials.
     */
    private static void handleAmpedElementDischarge(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        if (state.getChargeBladePhials() <= 0) {
            handleAxeCombo(player, combatState, state);
            return;
        }

        if (state.isCbShieldCharged()) {
            // ─── SAED: Super Amped Element Discharge ───
            int phialsSpent = state.getChargeBladePhials();
            state.setChargeBladePhials(0);

            float baseMv = WeaponDataResolver.resolveFloat(player, "motionValues", "cb_saed", 2.5f);
            float phialBonus = phialsSpent * 0.3f;
            applyHit(player, 4.0, 1.5, baseMv + phialBonus);

            // Spawn multiple phial explosions
            spawnDischargeExplosions(player, phialsSpent);

            state.setCbDischargeStage(0);
            setAction(combatState, "cb_saed", 24);
            playAnimation(player, "bettercombat:two_handed_slam_heavy", "cb_saed", 24);
        } else {
            // ─── AED: Amped Element Discharge ───
            state.setChargeBladePhials(state.getChargeBladePhials() - 1);

            float mv = WeaponDataResolver.resolveFloat(player, "motionValues", "cb_aed", 1.8f);
            boolean hit = applyHit(player, 3.5, 1.2, mv);

            if (hit) {
                applyPhialExplosion(player, state);
            }

            state.setCbDischargeStage(0);
            setAction(combatState, "cb_aed", 18);
            playAnimation(player, "bettercombat:two_handed_slam_heavy", "cb_aed", 18);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SPECIAL (F Key — Morph / Shield Charge / SAED shortcut)
    // ═══════════════════════════════════════════════════════════════════

    private static void handleSpecialAction(boolean pressed, Player player,
                                             PlayerCombatState combatState,
                                             PlayerWeaponState state,
                                             boolean swordMode) {
        if (!pressed) return;

        // If in middle of AED/SAED windup (discharge stage >= 2), cancel into Elemental Roundslash
        if (state.getCbDischargeStage() >= 2 || "cb_aed".equals(combatState.getActionKey()) || "cb_saed".equals(combatState.getActionKey())) {
            handleElementalRoundslash(player, combatState, state);
            return;
        }

        // If Shield Thrust was just performed (discharge stage == 1), go to AED/SAED
        if (state.getCbDischargeStage() == 1 && !comboExpired(player, state)) {
            // Morph to axe and perform AED/SAED
            state.setChargeBladeSwordMode(false);
            handleAmpedElementDischarge(player, combatState, state);
            return;
        }

        // Default: Morph Slash (with Guard Point at start)
        if (swordMode) {
            handleSwordMorphSlash(player, combatState, state);
        } else {
            handleAxeMorphSlash(player, combatState, state);
        }
    }

    /**
     * Sword: Morph Slash — Transforms to Axe with overhead slam.
     * Has Guard Point at start of animation (6 ticks).
     */
    private static void handleSwordMorphSlash(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        state.setChargeBladeSwordMode(false);
        state.setCbComboIndex(0);
        state.setCbDischargeStage(0);

        // Guard Point at start
        state.setCbGuardPointTicks(6);

        float mv = WeaponDataResolver.resolveFloat(player, "motionValues", "cb_morph_to_axe", 1.1f);
        boolean hit = applyHit(player, 3.0, 1.0, mv);

        if (hit && state.isCbPowerAxe()) {
            applyPowerAxeTick(player, state);
        }

        setAction(combatState, "cb_morph_to_axe", 14);
        playAnimation(player, "bettercombat:two_handed_slash_vertical_right", "cb_morph_to_axe", 14);
    }

    /**
     * Axe: Morph Slash — Transforms back to Sword.
     * Includes a Guard Point (roundslash transition).
     */
    private static void handleAxeMorphSlash(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        state.setChargeBladeSwordMode(true);
        state.setCbComboIndex(0);
        state.setCbDischargeStage(0);

        // Guard Point at end
        state.setCbGuardPointTicks(6);

        float mv = WeaponDataResolver.resolveFloat(player, "motionValues", "cb_morph_to_sword", 0.9f);
        applyHit(player, 2.5, 0.8, mv);

        setAction(combatState, "cb_morph_to_sword", 12);
        playAnimation(player, "bettercombat:one_handed_slash_horizontal_left", "cb_morph_to_sword", 12);
    }

    /**
     * Elemental Roundslash — Cancel AED/SAED windup to charge the shield.
     * Consumes phials to grant Shield Charge (Element Boost).
     * Returns to Sword Mode.
     */
    private static void handleElementalRoundslash(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        int phials = state.getChargeBladePhials();
        if (phials <= 0) {
            // No phials to charge shield with; just morph back
            handleAxeMorphSlash(player, combatState, state);
            return;
        }

        // Consume phials to charge shield
        int ticksPerPhial = WeaponDataResolver.resolveInt(player, "shieldCharge", "ticksPerPhial", 600);
        int duration = phials * ticksPerPhial;
        state.setChargeBladePhials(0);
        state.setCbShieldCharged(true);
        state.setCbShieldChargeTicks(duration);

        // Return to Sword Mode
        state.setChargeBladeSwordMode(true);
        state.setCbComboIndex(0);
        state.setCbDischargeStage(0);

        float mv = WeaponDataResolver.resolveFloat(player, "motionValues", "cb_elemental_roundslash", 1.0f);
        applyHit(player, 2.5, 1.0, mv);

        setAction(combatState, "cb_elemental_roundslash", 14);
        playAnimation(player, "bettercombat:one_handed_slash_horizontal_right", "cb_elemental_roundslash", 14);

        // VFX: shield charge activation
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    15, 0.5, 0.8, 0.5, 0.1);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CHARGE (Hold action — Phial Loading / Shield Boost / Sword Boost)
    // ═══════════════════════════════════════════════════════════════════

    private static void handleChargeAction(boolean pressed, Player player,
                                            PlayerCombatState combatState,
                                            PlayerWeaponState state,
                                            boolean swordMode) {
        if (!pressed) return;

        if (swordMode) {
            // Sword Mode: Charge Phials (convert Sword Energy to Phials)
            handleChargePhials(player, combatState, state);
        } else {
            // Axe Mode: Forward Slash → Shield Thrust shortcut
            handleShieldThrust(player, combatState, state);
        }
    }

    /**
     * Charge Phials — Converts stored Sword Energy into Phials.
     * Yellow (≥30) = 1-3 phials, Red (≥60) = 3-5 phials.
     * If Shield is already charged + hold continues → Sword Boost (Condensed Element Slash).
     */
    private static void handleChargePhials(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        int energy = state.getChargeBladeCharge();
        int currentPhials = state.getChargeBladePhials();

        if (energy < 30 && currentPhials < 5) {
            // Not enough energy to charge
            setAction(combatState, "cb_charge_fail", 6);
            return;
        }

        if (energy >= 30) {
            int phialsToAdd;
            if (energy >= 80) {
                phialsToAdd = 5;
            } else if (energy >= 60) {
                phialsToAdd = 3;
            } else {
                phialsToAdd = 1;
            }

            int newPhials = Math.min(5, currentPhials + phialsToAdd);
            int overflow = (currentPhials + phialsToAdd) - 5;

            state.setChargeBladePhials(newPhials);
            state.setChargeBladeCharge(0);

            // If phials were already full (overflow) and shield is charged → Sword Boost
            if (overflow > 0 && state.isCbShieldCharged()) {
                activateSwordBoost(player, state);
            }
        }

        setAction(combatState, "cb_charge_phials", 12);
        playAnimation(player, "bettercombat:one_handed_slash_vertical_right", "cb_charge_phials", 12);

        // VFX
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    8, 0.3, 0.5, 0.3, 0.02);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Buff activations
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Activate Sword Boost (Condensed Element Slash).
     * Adds phial effect to all sword attacks, prevents deflection.
     * Duration: ~45 seconds.
     */
    private static void activateSwordBoost(Player player, PlayerWeaponState state) {
        int duration = WeaponDataResolver.resolveInt(player, "swordBoost", "durationTicks", 900);
        state.setCbSwordBoosted(true);
        state.setCbSwordBoostTicks(duration);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    12, 0.3, 0.6, 0.3, 0.05);
        }
    }

    /**
     * Activate Power Axe Mode (Savage Axe).
     * Axe attacks deal multi-tick damage.
     */
    public static void activatePowerAxe(Player player, PlayerWeaponState state) {
        int duration = WeaponDataResolver.resolveInt(player, "powerAxe", "durationTicks", 900);
        state.setCbPowerAxe(true);
        state.setCbPowerAxeTicks(duration);

        if (player.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 8; i++) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        player.getX() + (player.getRandom().nextFloat() - 0.5) * 1.5,
                        player.getY() + 1.0,
                        player.getZ() + (player.getRandom().nextFloat() - 0.5) * 1.5,
                        1, 0.05, 0.1, 0.05, 0.02);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Phial & Power Axe damage helpers
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Apply a phial explosion (secondary AoE hit) on contact.
     */
    private static void applyPhialExplosion(Player player, PlayerWeaponState state) {
        float phialMv = WeaponDataResolver.resolveFloat(player, "phial", "explosionMv", 0.2f);
        applyHit(player, 2.5, 1.0, phialMv);

        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 pos = player.position().add(player.getLookAngle().scale(1.2));
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.x, pos.y + 1.0, pos.z,
                    3, 0.3, 0.3, 0.3, 0.05);
        }
    }

    /**
     * Power Axe multi-tick (sawblade effect).
     */
    private static void applyPowerAxeTick(Player player, PlayerWeaponState state) {
        float tickMv = WeaponDataResolver.resolveFloat(player, "powerAxe", "tickMv", 0.15f);
        applyHit(player, 3.0, 0.8, tickMv);

        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 pos = player.position().add(player.getLookAngle().scale(1.0));
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    pos.x, pos.y + 1.0, pos.z,
                    1, 0.2, 0.2, 0.2, 0.0);
        }
    }

    /**
     * Spawn multiple phial explosion particles for SAED.
     */
    private static void spawnDischargeExplosions(Player player, int phialCount) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        Vec3 look = player.getLookAngle();
        Vec3 base = player.position().add(look.scale(1.5));

        for (int i = 0; i < phialCount; i++) {
            double offset = (i + 1) * 1.2;
            Vec3 pos = base.add(look.scale(offset));
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    pos.x + (player.getRandom().nextFloat() - 0.5) * 0.8,
                    pos.y + 0.5,
                    pos.z + (player.getRandom().nextFloat() - 0.5) * 0.8,
                    1, 0.3, 0.3, 0.3, 0.0);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Energy Management
    // ═══════════════════════════════════════════════════════════════════

    private static void addSwordEnergy(Player player, PlayerWeaponState state, int amount) {
        int current = state.getChargeBladeCharge();
        state.setChargeBladeCharge(Math.min(100, current + amount));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Guard Point callback (called from WeaponStateEvents when hit during GP)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Called when the player is hit during a Guard Point window.
     * Returns true if the GP was consumed (damage should be negated/reduced).
     * On successful GP: can follow up with SAED or activate Power Axe (Savage Axe Slash).
     */
    public static boolean tryConsumeGuardPoint(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        if (state.getCbGuardPointTicks() <= 0) return false;

        state.setCbGuardPointTicks(0);

        // Shield charged GPs deal phial explosion
        if (state.isCbShieldCharged() && state.getChargeBladePhials() > 0) {
            applyPhialExplosion(player, state);
        }

        // Set up for SAED follow-up or Savage Axe Slash follow-up
        state.setCbDischargeStage(2); // Ready for AED/SAED

        setAction(combatState, "cb_guard_point_success", 10);
        playAnimation(player, "bettercombat:one_handed_slam_light", "cb_guard_point_success", 10);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    3, 0.3, 0.3, 0.3, 0.0);
        }

        syncState(player, state);
        return true;
    }
}
