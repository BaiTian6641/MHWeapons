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

import javax.annotation.Nullable;
import java.util.List;

/**
 * Switch Axe handler — MHWilds-accurate implementation.
 * <p>
 * Manages dual gauge system (Switch Gauge + Amp Gauge), two mode combo chains,
 * Power Axe Mode, Amped State, morphing, discharges, and counters.
 */
@SuppressWarnings("null")
public final class SwitchAxeHandler {
    private SwitchAxeHandler() {}

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

    private static void playAnimation(Player player, String animationKey) {
        if (player instanceof ServerPlayer sp) {
            ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> sp),
                    new PlayAttackAnimationS2CPacket(sp.getId(), animationKey, 0.5f, 0.3f, 1.0f, "", 0));
        }
    }

    @Nullable
    private static LivingEntity findTargetInFront(Player player, double range) {
        Vec3 look = player.getLookAngle();
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(look.scale(range));
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        return player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())
                .stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                .orElse(null);
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
        return WeaponDataResolver.resolveInt(player, null, "comboWindowTicks", 30);
    }

    private static boolean comboExpired(Player player, PlayerWeaponState state) {
        return (player.tickCount - state.getSwitchAxeComboTick()) > comboWindow(player);
    }

    private static void advanceCombo(Player player, PlayerWeaponState state, int maxIndex) {
        int next = comboExpired(player, state) ? 0 : state.getSwitchAxeComboIndex() + 1;
        if (next > maxIndex) next = 0;
        state.setSwitchAxeComboIndex(next);
        state.setSwitchAxeComboTick(player.tickCount);
    }

    // ── main dispatch ───────────────────────────────────────────────────

    public static void handleAction(WeaponActionType action, boolean pressed,
                                     Player player, PlayerCombatState combatState,
                                     PlayerWeaponState weaponState) {
        if (weaponState == null || combatState == null) return;

        boolean swordMode = weaponState.isSwitchAxeSwordMode();

        switch (action) {
            case WEAPON -> handleWeaponAction(pressed, player, combatState, weaponState, swordMode);
            case WEAPON_ALT -> handleAltAction(pressed, player, combatState, weaponState, swordMode);
            case SPECIAL -> handleSpecialAction(pressed, player, combatState, weaponState, swordMode);
            case CHARGE -> handleChargeAction(pressed, player, combatState, weaponState, swordMode);
            default -> {}
        }

        syncState(player, weaponState);
    }

    // ── WEAPON (primary attack / combo chain) ───────────────────────────

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
     * Axe combo: Overhead Slash → Side Slash → Rising Slash (loop).
     */
    private static void handleAxeCombo(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        advanceCombo(player, state, 2);
        int idx = state.getSwitchAxeComboIndex();

        float mv;
        String actionKey;
        String animKey;

        switch (idx) {
            case 0 -> {
                mv = WeaponDataResolver.resolveFloat(player, "motionValues", "sa_axe_overhead", 1.0f);
                actionKey = "sa_axe_overhead";
                animKey = "bettercombat:two_handed_slash_vertical_right";
            }
            case 1 -> {
                mv = WeaponDataResolver.resolveFloat(player, "motionValues", "sa_axe_side", 0.85f);
                actionKey = "sa_axe_side";
                animKey = "bettercombat:two_handed_slash_horizontal_left";
            }
            default -> {
                mv = WeaponDataResolver.resolveFloat(player, "motionValues", "sa_axe_rising", 0.95f);
                actionKey = "sa_axe_rising";
                animKey = "bettercombat:two_handed_slash_vertical_left";
            }
        }

        applyHit(player, 3.5, 1.0, mv);
        // Axe attacks regen switch gauge
        state.addSwitchAxeSwitchGauge(3.0f);
        if (state.isSwitchAxePowerAxe()) {
            state.addSwitchAxeSwitchGauge(2.0f); // bonus in Power Axe
        }
        setAction(combatState, actionKey, 10);
        state.setSwitchAxeComboTick(player.tickCount + 10);
        playAnimation(player, animKey);
    }

    /**
     * Sword combo: Overhead Slash → Double Slash → Rising Slash (loop).
     * Consumes Switch Gauge per swing.
     */
    private static void handleSwordCombo(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        float gaugeCost = WeaponDataResolver.resolveFloat(player, "switchGauge", "swordSwingCost", 3.0f);
        if (state.getSwitchAxeSwitchGauge() < gaugeCost) {
            // Forced morph back to axe when gauge is empty
            forceMorphToAxe(player, combatState, state);
            return;
        }

        advanceCombo(player, state, 2);
        int idx = state.getSwitchAxeComboIndex();

        float mv;
        String actionKey;
        String animKey;

        switch (idx) {
            case 0 -> {
                mv = WeaponDataResolver.resolveFloat(player, "motionValues", "sa_sword_overhead", 1.1f);
                actionKey = "sa_sword_overhead";
                animKey = "bettercombat:two_handed_slash_vertical_right";
            }
            case 1 -> {
                mv = WeaponDataResolver.resolveFloat(player, "motionValues", "sa_sword_double", 0.7f);
                actionKey = "sa_sword_double";
                animKey = "bettercombat:two_handed_slash_horizontal_right";
            }
            default -> {
                mv = WeaponDataResolver.resolveFloat(player, "motionValues", "sa_sword_rising", 1.0f);
                actionKey = "sa_sword_rising";
                animKey = "bettercombat:two_handed_slash_horizontal_left";
            }
        }

        state.addSwitchAxeSwitchGauge(-gaugeCost);
        boolean hit = applyHit(player, 3.0, 0.9, mv);

        // Sword attacks build Amp Gauge
        if (hit) {
            float ampGain = WeaponDataResolver.resolveFloat(player, "ampGauge", "gainPerHit", 8.0f);
            state.addSwitchAxeAmpGauge(ampGain);
            // Check for Amped State activation
            if (!state.isSwitchAxeAmped() && state.getSwitchAxeAmpGauge() >= 100.0f) {
                activateAmpedState(player, state);
            }
        }

        // Phial explosion on hit if amped
        if (hit && state.isSwitchAxeAmped()) {
            applyPhialExplosion(player, state);
        }

        setAction(combatState, actionKey, 10);
        state.setSwitchAxeComboTick(player.tickCount + 10);
        playAnimation(player, animKey);
    }

    // ── WEAPON_ALT (context-sensitive) ──────────────────────────────────

    private static void handleAltAction(boolean pressed, Player player,
                                         PlayerCombatState combatState,
                                         PlayerWeaponState state,
                                         boolean swordMode) {
        if (!pressed) return;

        if (swordMode) {
            // Sword Alt: Counter Rising Slash (enters counter window)
            handleCounterRisingSlash(player, combatState, state);
        } else {
            // Axe Alt: Spiral Burst Slash (gauge refill) or Wild Swing follow-up
            if (state.getSwitchAxeWildSwingCount() > 0) {
                // Heavy Slam finisher after Wild Swing
                handleHeavySlam(player, combatState, state);
            } else {
                handleSpiralBurstSlash(player, combatState, state);
            }
        }
    }

    /**
     * Spiral Burst Slash — Axe finisher that refills ~30% Switch Gauge.
     */
    private static void handleSpiralBurstSlash(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        float mv = WeaponDataResolver.resolveFloat(player, "motionValues", "sa_spiral_burst", 1.3f);
        boolean hit = applyHit(player, 3.5, 1.2, mv);
        float gaugeRefill = WeaponDataResolver.resolveFloat(player, "switchGauge", "spiralBurstRefill", 30.0f);
        state.addSwitchAxeSwitchGauge(gaugeRefill);
        setAction(combatState, "sa_spiral_burst", 14);
        playAnimation(player, "bettercombat:two_handed_slam_heavy");
        // Reset combo
        state.setSwitchAxeComboIndex(0);
        state.setSwitchAxeComboTick(player.tickCount + 14);
    }

    /**
     * Heavy Slam — Axe finisher from Wild Swing. Triggers Power Axe Mode.
     */
    private static void handleHeavySlam(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        float mv = WeaponDataResolver.resolveFloat(player, "motionValues", "sa_heavy_slam", 1.4f);
        applyHit(player, 3.0, 1.3, mv);

        // Activate Power Axe Mode
        int duration = WeaponDataResolver.resolveInt(player, null, "powerAxeDurationTicks", 1200);
        state.setSwitchAxePowerAxe(true);
        state.setSwitchAxePowerAxeTicks(duration);

        state.setSwitchAxeWildSwingCount(0);
        setAction(combatState, "sa_heavy_slam", 16);
        playAnimation(player, "bettercombat:two_handed_slam_heavy");

        // Spawn particles for Power Axe activation
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

    /**
     * Counter Rising Slash — Sword counter. Sets counter window.
     */
    private static void handleCounterRisingSlash(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        float cost = StaminaHelper.applyCost(player, 25.0f);
        if (state.getStamina() < cost) return;

        spendStamina(player, state, 25.0f, 20);
        int counterWindow = WeaponDataResolver.resolveInt(player, null, "counterWindowTicks", 15);
        state.setSwitchAxeCounterTicks(counterWindow);
        setAction(combatState, "sa_counter_rising", 15);
        playAnimation(player, "bettercombat:two_handed_slash_vertical_left");
    }

    // ── SPECIAL (morph / discharge) ─────────────────────────────────────

    private static void handleSpecialAction(boolean pressed, Player player,
                                             PlayerCombatState combatState,
                                             PlayerWeaponState state,
                                             boolean swordMode) {
        if (!pressed) return;

        if (swordMode) {
            // Sword Special: Element Discharge / Zero Sum Discharge / Full Release Slash
            handleSwordSpecial(player, combatState, state);
        } else {
            // Axe Special: Morph to Sword (with gauge check) or Offset Rising Slash
            handleAxeSpecial(player, combatState, state);
        }
    }

    private static void handleAxeSpecial(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        // If currently in combo, do a Morph Slash (attack while morphing)
        float morphThreshold = WeaponDataResolver.resolveFloat(player, "switchGauge", "morphThreshold", 30.0f);
        if (state.getSwitchAxeSwitchGauge() < morphThreshold) {
            // Can't morph — not enough gauge. Do Offset Rising Slash (axe counter) instead.
            handleOffsetRisingSlash(player, combatState, state);
            return;
        }

        // Morph to Sword
        state.setSwitchAxeSwordMode(true);
        state.setSwitchAxeComboIndex(0);
        state.setSwitchAxeWildSwingCount(0);

        float mv = WeaponDataResolver.resolveFloat(player, "motionValues", "sa_morph_slash", 1.1f);
        applyHit(player, 3.0, 0.9, mv);
        setAction(combatState, "sa_morph_to_sword", 12);
        playAnimation(player, "bettercombat:two_handed_slash_horizontal_right");
    }

    /**
     * Offset Rising Slash — Axe counter. Sets counter window.
     */
    private static void handleOffsetRisingSlash(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        float cost = StaminaHelper.applyCost(player, 20.0f);
        if (state.getStamina() < cost) return;

        spendStamina(player, state, 20.0f, 18);
        int counterWindow = WeaponDataResolver.resolveInt(player, null, "counterWindowTicks", 15);
        state.setSwitchAxeCounterTicks(counterWindow);
        setAction(combatState, "sa_offset_rising", 14);
        playAnimation(player, "bettercombat:two_handed_slash_vertical_right");
    }

    private static void handleSwordSpecial(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        // Amped: Full Release Slash (costs full amp gauge, massive AoE, morphs to axe)
        if (state.isSwitchAxeAmped()) {
            handleFullReleaseSlash(player, combatState, state);
            return;
        }

        // Not amped: Element Discharge (costs 50% amp gauge)
        if (state.getSwitchAxeFrcCooldown() > 0) return;

        if (state.getSwitchAxeAmpGauge() >= 50.0f) {
            handleElementDischarge(player, combatState, state);
        } else {
            // Fallback: Morph back to axe
            morphToAxe(player, combatState, state);
        }
    }

    /**
     * Element Discharge — Multi-hit discharge. Costs 50% amp gauge.
     */
    private static void handleElementDischarge(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        state.addSwitchAxeAmpGauge(-50.0f);
        state.setSwitchAxeFrcCooldown(60);

        float mv = WeaponDataResolver.resolveFloat(player, "motionValues", "sa_element_discharge", 1.6f);
        applyHit(player, 3.0, 1.0, mv);

        setAction(combatState, "sa_element_discharge", 16);
        playAnimation(player, "bettercombat:two_handed_slam_heavy");

        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 pos = player.position().add(player.getLookAngle().scale(1.5));
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    pos.x, pos.y + 1.0, pos.z, 1, 0.3, 0.3, 0.3, 0.0);
        }
    }

    /**
     * Full Release Slash — Amped State exclusive. Massive AoE, drains all amp,
     * ends Amped State, morphs to Axe.
     */
    private static void handleFullReleaseSlash(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        float mv = WeaponDataResolver.resolveFloat(player, "motionValues", "sa_full_release", 3.5f);
        applyHit(player, 4.0, 1.5, mv);

        // Drain amp and end amped state
        state.setSwitchAxeAmpGauge(0.0f);
        state.setSwitchAxeAmped(false);
        state.setSwitchAxeAmpedTicks(0);

        // Morph to axe
        state.setSwitchAxeSwordMode(false);
        state.setSwitchAxeComboIndex(0);

        setAction(combatState, "sa_full_release", 20);
        playAnimation(player, "bettercombat:two_handed_slam_heavy");

        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 pos = player.position().add(player.getLookAngle().scale(2.0));
            for (int i = 0; i < 12; i++) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        pos.x + (player.getRandom().nextFloat() - 0.5) * 2.0,
                        pos.y + 1.0,
                        pos.z + (player.getRandom().nextFloat() - 0.5) * 2.0,
                        1, 0.2, 0.2, 0.2, 0.0);
            }
            for (int i = 0; i < 6; i++) {
                serverLevel.sendParticles(ParticleTypes.FLASH,
                        pos.x, pos.y + 1.2, pos.z,
                        1, 0.5, 0.5, 0.5, 0.0);
            }
        }
    }

    // ── CHARGE (Wild Swing loop) ────────────────────────────────────────

    private static void handleChargeAction(boolean pressed, Player player,
                                            PlayerCombatState combatState,
                                            PlayerWeaponState state,
                                            boolean swordMode) {
        if (swordMode) {
            // Sword hold: Heavenward Flurry (rapid amp gauge build)
            if (pressed) {
                handleHeavenwardFlurry(player, combatState, state);
            }
        } else {
            // Axe hold: Wild Swing loop
            if (pressed) {
                handleWildSwing(player, combatState, state);
            }
        }
    }

    /**
     * Wild Swing — Infinite loop in Axe mode, increments count.
     * Follow up with Heavy Slam (via WEAPON_ALT) to activate Power Axe.
     */
    private static void handleWildSwing(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        float staminaCost = WeaponDataResolver.resolveFloat(player, "wildSwing", "staminaCost", 10.0f);
        if (state.getStamina() < StaminaHelper.applyCost(player, staminaCost)) return;

        spendStamina(player, state, staminaCost, 12);

        float mv = WeaponDataResolver.resolveFloat(player, "motionValues", "sa_wild_swing", 0.7f);
        applyHit(player, 3.5, 1.2, mv);

        int count = state.getSwitchAxeWildSwingCount() + 1;
        state.setSwitchAxeWildSwingCount(count);
        state.addSwitchAxeSwitchGauge(2.0f);

        setAction(combatState, "sa_wild_swing", 8);
        // Alternate animations for visual variety
        String anim = (count % 2 == 0)
                ? "bettercombat:two_handed_slash_horizontal_right"
                : "bettercombat:two_handed_slash_horizontal_left";
        playAnimation(player, anim);
    }

    /**
     * Heavenward Flurry — Sword rapid multi-hit. High amp gauge gain.
     */
    private static void handleHeavenwardFlurry(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        float gaugeCost = WeaponDataResolver.resolveFloat(player, "switchGauge", "flurryCost", 5.0f);
        if (state.getSwitchAxeSwitchGauge() < gaugeCost) {
            forceMorphToAxe(player, combatState, state);
            return;
        }

        float staminaCost = WeaponDataResolver.resolveFloat(player, "heavenwardFlurry", "staminaCost", 12.0f);
        if (state.getStamina() < StaminaHelper.applyCost(player, staminaCost)) return;

        spendStamina(player, state, staminaCost, 10);
        state.addSwitchAxeSwitchGauge(-gaugeCost);

        float mv = WeaponDataResolver.resolveFloat(player, "motionValues", "sa_heavenward_flurry", 0.5f);
        boolean hit = applyHit(player, 2.8, 0.8, mv);

        if (hit) {
            float ampGain = WeaponDataResolver.resolveFloat(player, "ampGauge", "flurryGainPerHit", 12.0f);
            state.addSwitchAxeAmpGauge(ampGain);
            if (!state.isSwitchAxeAmped() && state.getSwitchAxeAmpGauge() >= 100.0f) {
                activateAmpedState(player, state);
            }
            if (state.isSwitchAxeAmped()) {
                applyPhialExplosion(player, state);
            }
        }

        setAction(combatState, "sa_heavenward_flurry", 6);
        playAnimation(player, "bettercombat:two_handed_slash_vertical_right");
    }

    // ── Morph helpers ───────────────────────────────────────────────────

    private static void morphToAxe(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        state.setSwitchAxeSwordMode(false);
        state.setSwitchAxeComboIndex(0);
        setAction(combatState, "sa_morph_to_axe", 10);
        playAnimation(player, "bettercombat:two_handed_slash_horizontal_left");
    }

    private static void forceMorphToAxe(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        state.setSwitchAxeSwordMode(false);
        state.setSwitchAxeComboIndex(0);
        state.setSwitchAxeWildSwingCount(0);
        setAction(combatState, "sa_forced_morph", 14);
        playAnimation(player, "bettercombat:two_handed_slash_horizontal_left");
    }

    // ── Amped State / Phial ─────────────────────────────────────────────

    private static void activateAmpedState(Player player, PlayerWeaponState state) {
        state.setSwitchAxeAmped(true);
        int duration = WeaponDataResolver.resolveInt(player, null, "ampedDurationTicks", 900); // 45 seconds
        state.setSwitchAxeAmpedTicks(duration);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    15, 0.5, 0.8, 0.5, 0.1);
        }
    }

    /**
     * Apply phial explosion (secondary hit) during Amped State.
     */
    private static void applyPhialExplosion(Player player, PlayerWeaponState state) {
        float phialMv = WeaponDataResolver.resolveFloat(player, "phial", "explosionMv", 0.15f);
        applyHit(player, 2.5, 1.0, phialMv);

        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 pos = player.position().add(player.getLookAngle().scale(1.2));
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.x, pos.y + 1.0, pos.z,
                    3, 0.3, 0.3, 0.3, 0.05);
        }
    }

    // ── Counter success callback (called from WeaponStateEvents on hit during counter) ──

    /**
     * Called when the player is hit during a counter window.
     * Returns true if the counter was consumed (damage should be negated).
     */
    public static boolean tryConsumeCounter(Player player, PlayerCombatState combatState, PlayerWeaponState state) {
        if (state.getSwitchAxeCounterTicks() <= 0) return false;

        state.setSwitchAxeCounterTicks(0);

        if (state.isSwitchAxeSwordMode()) {
            // Counter Rising Slash success → Heavenward Flurry follow-up
            setAction(combatState, "sa_counter_success", 10);
            float mv = WeaponDataResolver.resolveFloat(player, "motionValues", "sa_counter_rising_hit", 1.3f);
            applyHit(player, 3.0, 1.0, mv);
            playAnimation(player, "bettercombat:two_handed_slash_vertical_left");
        } else {
            // Offset Rising Slash success → Heavy Slam follow-up
            setAction(combatState, "sa_offset_success", 12);
            float mv = WeaponDataResolver.resolveFloat(player, "motionValues", "sa_offset_rising_hit", 1.2f);
            applyHit(player, 3.5, 1.2, mv);
            playAnimation(player, "bettercombat:two_handed_slash_vertical_right");
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    3, 0.3, 0.3, 0.3, 0.0);
        }

        syncState(player, state);
        return true;
    }
}
