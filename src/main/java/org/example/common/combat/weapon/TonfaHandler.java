package org.example.common.combat.weapon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.capability.mob.MobWoundState;
import org.example.common.combat.StaminaHelper;
import org.example.common.data.WeaponDataResolver;
import org.example.common.util.CapabilityUtil;

public final class TonfaHandler {
    private static final Logger LOGGER = LogManager.getLogger("MHWeaponsMod/Tonfa");

    /** Maximum air actions (dashes/flurries) before forced descent. */
    private static final int MAX_AIR_ACTIONS = 6;
    /** Rhythm Gauge cost for Focus Strike (Pinpoint Drill), as a percentage (0-100). */
    private static final float FOCUS_STRIKE_GAUGE_COST = 40.0f;

    private TonfaHandler() {
    }

    public static void handle(Player player, WeaponActionType action, boolean pressed, PlayerCombatState combatState, PlayerWeaponState weaponState) {
        if (!pressed) {
            return;
        }

        boolean shortMode = weaponState.isTonfaShortMode();
        String currentAction = combatState.getActionKey();
        int actionTicks = combatState.getActionKeyTicks();
        int comboWindow = WeaponDataResolver.resolveInt(player, null, "comboWindowTicks", 40);
        int currentTick = (int) player.level().getGameTime();
        int comboDelta = currentTick - weaponState.getTonfaComboTick();
        boolean withinComboWindow = comboDelta >= 0 && comboDelta <= comboWindow;
        boolean isTonfaComboAction = currentAction != null && (currentAction.startsWith("tonfa_long_")
            || currentAction.startsWith("tonfa_short_") || "tonfa_short_rise".equals(currentAction));
        boolean focusEligible = false;
        if (action == WeaponActionType.WEAPON && combatState.isFocusMode()) {
            LivingEntity target = findTargetInFront(player, 4.0);
            focusEligible = isWoundedTarget(target);
        }

        LOGGER.info("Tonfa handle: action={}, pressed={}, shortMode={}, onGround={}, actionKey={}, actionTicks={}, comboIndex={}, comboTick={}, tickNow={}, withinWindow={}",
            action, pressed, shortMode, player.onGround(), currentAction, actionTicks, weaponState.getTonfaComboIndex(), weaponState.getTonfaComboTick(), currentTick, withinComboWindow);

        // Prevent input while an animation is active.
        if (actionTicks > 0) {
            boolean allowDrillOverride = action == WeaponActionType.WEAPON
                && ("tonfa_drill".equals(currentAction) || "focus_strike".equals(currentAction))
                && !focusEligible;
            if (!allowDrillOverride) {
                LOGGER.info("Tonfa input blocked: actionTicks={}, currentAction={}, shortMode={}, withinWindow={}",
                    actionTicks, currentAction, shortMode, withinComboWindow);
                return;
            }
        }

        // 1. Mode Switch (Special Action)
        if (action == WeaponActionType.SPECIAL) {
            weaponState.setTonfaShortMode(!shortMode);
            setAction(combatState, weaponState.isTonfaShortMode() ? "tonfa_short" : "tonfa_long", 10);
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.5, player.getZ(), 10, 0.2, 0.2, 0.2, 0.02);
                serverLevel.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.5, player.getZ(), 8, 0.1, 0.1, 0.1, 0.01);
            }
            return;
        }

        // 2. Primary Attack (Left Click / WEAPON)
        if (action == WeaponActionType.WEAPON) {
            int comboActionTicks = WeaponDataResolver.resolveInt(player, null, "comboActionTicks", 12);
            
            // Focus Strike: Pinpoint Drill (Wilds Mechanic)
            // Requires: Focus Mode + Wounded Target + Sufficient Gauge + Stamina
            if (combatState.isFocusMode() && focusEligible) {
                 float gaugeRequired = FOCUS_STRIKE_GAUGE_COST;
                 if (weaponState.getTonfaComboGauge() < gaugeRequired) {
                     LOGGER.info("Tonfa Focus Strike denied: gauge {}/{}", weaponState.getTonfaComboGauge(), gaugeRequired);
                     return;
                 }
                 float staminaCost = StaminaHelper.applyCost(player, 10.0f);
                 if (weaponState.getStamina() >= staminaCost) {
                     weaponState.addStamina(-staminaCost);
                     weaponState.setStaminaRecoveryDelay(20);
                     // Consume Rhythm Gauge
                     weaponState.addTonfaComboGauge(-gaugeRequired);
                     setAction(combatState, "tonfa_drill", comboActionTicks);
                     
                     // Small forward lunge
                     Vec3 forward = player.getLookAngle().normalize().scale(0.9);
                     player.setDeltaMovement(forward.x, Math.max(0.05, player.getDeltaMovement().y), forward.z);
                     player.hurtMarked = true;
                 }
                 return;
            }

            // Airborne Attacks
            if (!player.onGround() || weaponState.getTonfaFlyingTicks() > 0) {
                // Air action budget check
                if (weaponState.getTonfaAirActionCount() >= MAX_AIR_ACTIONS) {
                    LOGGER.info("Tonfa aerial attack denied: air action budget exhausted ({}/{})", weaponState.getTonfaAirActionCount(), MAX_AIR_ACTIONS);
                    return;
                }
                setAction(combatState, "tonfa_air_slash", comboActionTicks);
                weaponState.setTonfaAirActionCount(weaponState.getTonfaAirActionCount() + 1);
               
                // Propel forward slightly
                Vec3 forward = player.getLookAngle().normalize().scale(0.75);
                player.setDeltaMovement(forward.x, Math.max(0.18, player.getDeltaMovement().y), forward.z);
                player.hurtMarked = true;
                return;
            }

            // Ground Attacks
            if (shortMode) {
                // Short Mode Ground: Rising Smash (launcher) -> Short Combo chain
                // First hit always launches. If already in short combo window, continue chain.
                int window = comboWindow;
                int lastEndTick = weaponState.getTonfaComboTick();
                int current = weaponState.getTonfaComboIndex();

                int next;
                if (lastEndTick <= 0 || currentTick > (lastEndTick + window)) {
                    next = 0; // Rising Smash (launcher)
                } else if (currentTick < lastEndTick) {
                    LOGGER.info("Tonfa short combo input ignored: too early (now={}, endTick={})", currentTick, lastEndTick);
                    return;
                } else {
                    next = (current + 1) % 3;
                }

                weaponState.setTonfaComboIndex(next);

                // EX Finisher at step 2 if gauge >= 95%
                boolean triggerEx = next == 2 && weaponState.getTonfaComboGauge() >= 95.0f;

                String actionKey;
                if (next == 0) {
                    // Rising Smash — launches player
                    actionKey = "tonfa_short_rise";
                    player.setDeltaMovement(player.getDeltaMovement().add(0, 0.8, 0));
                    player.hurtMarked = true;
                } else if (triggerEx) {
                    actionKey = "tonfa_short_ex";
                    spawnExParticles(player);
                } else {
                    actionKey = switch (next) {
                        case 1 -> "tonfa_short_1";
                        default -> "tonfa_short_2";
                    };
                }

                setAction(combatState, actionKey, comboActionTicks);
                weaponState.setTonfaComboTick(currentTick + comboActionTicks);
                
            } else {
                // Long Mode Ground: Standard 3-Hit Combo
                // Thrust (I) -> Kick/Swing (II) -> Uppercut (III)
                
                int window = comboWindow;
                int lastEndTick = weaponState.getTonfaComboTick();
                int current = weaponState.getTonfaComboIndex();

                // Combo window starts after the previous attack finishes.
                // If the window is missed, reset to Combo I.
                int next;
                if (lastEndTick <= 0 || currentTick > (lastEndTick + window)) {
                    next = 0;
                } else if (currentTick < lastEndTick) {
                    // Too early (animation not finished yet)
                    LOGGER.info("Tonfa combo input ignored: too early (now={}, endTick={})", currentTick, lastEndTick);
                    return;
                } else {
                    next = (current + 1) % 3;
                }

                weaponState.setTonfaComboIndex(next);

                LOGGER.info("Tonfa combo: tick={}, lastTick={}, window={}, current={}, next={}, actionKey={}, actionTicks={}, shortMode={}, onGround={}",
                    currentTick, lastEndTick, window, current, next, combatState.getActionKey(), combatState.getActionKeyTicks(), shortMode, player.onGround());
                
                // EX Finisher Check (Gauge >= 95%)
                boolean triggerEx = next == 2 && weaponState.getTonfaComboGauge() >= 95.0f;
                
                String actionKey;
                if (triggerEx) {
                    actionKey = "tonfa_long_ex";
                    spawnExParticles(player);
                } else {
                    actionKey = switch (next) {
                        case 0 -> "tonfa_long_1";
                        case 1 -> "tonfa_long_2";
                        default -> "tonfa_long_3";
                    };
                }
                int actionDuration = 10;
                actionDuration = comboActionTicks;
                setAction(combatState, actionKey, actionDuration);
                weaponState.setTonfaComboTick(currentTick + actionDuration);
            }
            return;
        }

        // 4. Secondary Attack (Right Click / WEAPON_ALT)
        if (action == WeaponActionType.WEAPON_ALT) {
            if (shortMode) {
                if (!player.onGround()) {
                    // Aerial Flurry: Multi-hit rapid punches (air action budget)
                    if (weaponState.getTonfaAirActionCount() >= MAX_AIR_ACTIONS) {
                        LOGGER.info("Tonfa aerial flurry denied: air action budget exhausted");
                        return;
                    }
                    setAction(combatState, "tonfa_short_flurry", 10);
                    weaponState.setTonfaAirActionCount(weaponState.getTonfaAirActionCount() + 1);
                    // Suspend gravity briefly
                    player.setDeltaMovement(player.getDeltaMovement().multiply(0.8, 0.5, 0.8));
                    player.hurtMarked = true;
                } else {
                   // Ground Right Click?
                   // Doc says: "Aerial Flurry" is Right Click, but under "Basic Structure" -> "Short Mode (Aerial Focus)".
                   // Ground Right Click for Short Mode? 
                   // Table says "Rising Smash" is Left Click. "Aerial Flurry" is Right Click.
                   // If on Ground, maybe Right Click also launches or does a flurry?
                   // Docs are aerial focused. Let's assume Right Click on ground in Short Mode also does the Flurry (maybe low to ground)
                   // Or standard "Horizontal Sweep" if we follow Long Mode.
                   // Let's make it a quick launcher poke or the same flurry.
                   setAction(combatState, "tonfa_short_flurry", 10);
                }
            } else {
                // Long Mode: Horizontal Sweep (Wide AOE)
                setAction(combatState, "tonfa_long_sweep", 10);
            }
        }
    }

    public static boolean handleDodge(Player player, boolean pressed, PlayerCombatState combatState, PlayerWeaponState weaponState) {
        if (!pressed) return false;

        // Tonfa Mid-Air Air Dash (Jet Propulsion)
        if (!player.onGround()) {
             // Air action budget check
             if (weaponState.getTonfaAirActionCount() >= MAX_AIR_ACTIONS) {
                 LOGGER.info("Tonfa air dash denied: air action budget exhausted ({}/{})", weaponState.getTonfaAirActionCount(), MAX_AIR_ACTIONS);
                 return false;
             }

             float cost = StaminaHelper.applyCost(player, 20.0f);
             if (weaponState.getStamina() >= cost) {
                 if (combatState != null) {
                     combatState.setDodgeIFrameTicks(6);
                     combatState.setActionKey("midair_evade");
                     combatState.setActionKeyTicks(6);
                 }
                 weaponState.addStamina(-cost);
                 weaponState.setStaminaRecoveryDelay(16);
                 weaponState.setTonfaAirActionCount(weaponState.getTonfaAirActionCount() + 1);
                 
                 Vec3 dash = player.getLookAngle().normalize().scale(0.8);
                 // Cancel vertical momentum for a true dash feel
                 player.setDeltaMovement(dash.x, Math.max(0.15, dash.y), dash.z); 
                 player.hurtMarked = true;
                 
                 if (player.level() instanceof ServerLevel serverLevel) {
                     serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.5, player.getZ(), 5, 0.2, 0.2, 0.2, 0.02);
                 }
                 return true;
             }
        }
        return false; // let default dodge handle it
    }

    public static boolean handleDoubleJump(Player player, boolean pressed, PlayerCombatState combatState, PlayerWeaponState weaponState) {
        if (pressed && !player.onGround() && !weaponState.isTonfaDoubleJumped()) {
             float cost = StaminaHelper.applyCost(player, 15.0f);
             if (weaponState.getStamina() >= cost) {
                 weaponState.addStamina(-cost);
                 weaponState.setStaminaRecoveryDelay(16);
                 weaponState.setTonfaDoubleJumped(true);
                 
                 // Boost Y velocity
                 Vec3 current = player.getDeltaMovement();
                 player.setDeltaMovement(current.x, 0.6, current.z);
                 player.hurtMarked = true;
                 
                 if (player.level() instanceof ServerLevel serverLevel) {
                     serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 5, 0.1, 0.1, 0.1, 0.05);
                 }
                 return true;
             }
        }
        return false;
    }

    public static void handleChargeRelease(Player player, PlayerCombatState combatState) {
        if (player.onGround()) {
            // Ground: Concentrated Smash (Values huge KO, HA)
            setAction(combatState, "tonfa_charge", 10);
        } else {
            // Air: Pile Bunker Dive (Corkscrews down)
            setAction(combatState, "tonfa_dive", 10);
            // Gravity assist?
            player.setDeltaMovement(player.getDeltaMovement().add(0, -1.0, 0));
            player.hurtMarked = true;
        }
    }

    private static void setAction(PlayerCombatState combatState, String key, int ticks) {
        if (combatState != null) {
            combatState.setActionKey(key);
            combatState.setActionKeyTicks(ticks);
        }
    }

    /**
     * Called every tick for a player holding Tonfa. Handles:
     * - Air action counter reset on landing
     * - Double jump reset on landing
     * - Rhythm Gauge decay when not hitting
     */
    public static void tickTonfa(Player player, PlayerWeaponState weaponState) {
        // Reset air state when grounded
        if (player.onGround()) {
            if (weaponState.getTonfaAirActionCount() > 0) {
                weaponState.setTonfaAirActionCount(0);
            }
            if (weaponState.isTonfaDoubleJumped()) {
                weaponState.setTonfaDoubleJumped(false);
            }
        }

        // Rhythm Gauge decay: if no hit for 40+ ticks, decay 2/tick (configurable via data)
        int currentTick = (int) player.level().getGameTime();
        int lastHit = weaponState.getTonfaLastHitTick();
        float gauge = weaponState.getTonfaComboGauge();
        if (gauge > 0 && lastHit > 0) {
            int ticksSince = currentTick - lastHit;
            int decayDelay = WeaponDataResolver.resolveInt(player, null, "gaugeDecayDelay", 40);
            if (ticksSince > decayDelay) {
                float decayRate = 2.0f; // per tick
                weaponState.addTonfaComboGauge(-decayRate);
            }
        }
    }
    
    private static void spawnExParticles(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
             serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
        }
    }

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

    private static boolean isWoundedTarget(LivingEntity target) {
        if (target == null) {
            return false;
        }
        MobWoundState state = CapabilityUtil.getMobWoundState(target);
        return state != null && state.isWounded();
    }
}
