package org.example.common.combat.weapon;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.network.ModNetwork;
import org.example.common.network.packet.PlayAttackAnimationS2CPacket;
import org.example.common.network.packet.PlayerWeaponStateS2CPacket;
import org.example.common.util.CapabilityUtil;
import org.example.common.data.WeaponDataResolver;
import org.example.common.combat.StaminaHelper;
import org.example.item.WeaponIdProvider;
import org.example.common.capability.player.PlayerCombatState;
import org.example.MHWeaponsMod;
import org.joml.Vector3f;

public final class WeaponStateEvents {
    private static int resolveSpiritLevelMaxTicks(int level) {
        return switch (Math.max(1, Math.min(3, level))) {
            case 1 -> 1200;
            case 2 -> 900;
            default -> 700;
        };
    }

    @SubscribeEvent
    @SuppressWarnings("null")
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide) {
            return;
        }
        PlayerWeaponState state = CapabilityUtil.getPlayerWeaponState(player);
        if (state == null) {
            return;
        }
        PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(player);
        if (combatState != null && "spirit_thrust".equals(combatState.getActionKey())
                && combatState.getActionKeyTicks() > 0) {
            if (state.getSpiritLevel() <= 0) {
                combatState.setActionKey(null);
                combatState.setActionKeyTicks(0);
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;
                return;
            }
            Vec3 dir = state.getLongSwordHelmBreakerDir();
            if (dir.lengthSqr() < 0.0001) {
                Vec3 look = player.getLookAngle();
                dir = new Vec3(look.x, 0.0, look.z);
                if (dir.lengthSqr() > 0.0001) {
                    dir = dir.normalize();
                    state.setLongSwordHelmBreakerDir(dir);
                }
            }
            if (dir.lengthSqr() > 0.0001) {
                // Dash forward up to ~5 blocks over the thrust window, locked to stored facing
                float thrustDistance = org.example.common.data.WeaponDataResolver.resolveFloat(player, "spiritThrust", "distance", 0.95f);
                player.setDeltaMovement(dir.scale(thrustDistance));
                player.hurtMarked = true;
            }
            player.fallDistance = 0.0f;

            // Lightweight hit detection during the thrust dash
            Vec3 start = player.position();
            float thrustDistance = org.example.common.data.WeaponDataResolver.resolveFloat(player, "spiritThrust", "distance", 0.75f);
            Vec3 end = start.add(dir.scale(thrustDistance));
            AABB box = new AABB(start, end).inflate(0.8);
            var hits = player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != player);
            if (!hits.isEmpty()) {
                LivingEntity target = hits.get(0);
                double base = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                float thrustDamage = (float) Math.max(1.0, base * 0.45);
                target.hurt(player.damageSources().playerAttack(player), thrustDamage);
                // Reuse the existing thrust->helm breaker promotion
                int currentLevel = state.getSpiritLevel();
                state.setLongSwordHelmBreakerSpiritLevel(currentLevel);
                int next = Math.max(0, currentLevel - 1);
                state.setSpiritLevel(next);
                if (next > 0) {
                    state.setSpiritLevelTicks(resolveSpiritLevelMaxTicks(next));
                } else {
                    state.setSpiritLevelTicks(0);
                }
                combatState.setActionKey("spirit_helm_breaker");
                int hangTime = 30; // 1.5s total (1s hang + 0.5s dive)
                combatState.setActionKeyTicks(hangTime);
                if (player instanceof ServerPlayer serverPlayer) {
                    String animId = org.example.common.data.WeaponDataResolver.resolveString(player, "animationOverrides", "spirit_helm_breaker",
                        "bettercombat:two_handed_slam");
                    float length = org.example.common.data.WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 16.666666f);
                    float upswing = org.example.common.data.WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.25f);
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f,
                            "spirit_helm_breaker", hangTime));
                }
                Vec3 targetDir = target.position().subtract(player.position());
                Vec3 horiz = new Vec3(targetDir.x, 0.0, targetDir.z);
                if (horiz.lengthSqr() <= 0.0001) {
                    horiz = dir;
                }
                if (horiz.lengthSqr() > 0.0001) {
                    horiz = horiz.normalize();
                }
                state.setLongSwordHelmBreakerDir(horiz);
                // initial minimal jump, handled mostly by hang logic
                // Raise to ~5.5 blocks peak height
                player.setDeltaMovement(horiz.scale(0.2).add(0.0, 4.0, 0.0));
                // Set cooldown to prevent spamming immediately after
                state.setLongSwordHelmBreakerCooldown(200); // 10 seconds cooldown
                player.hurtMarked = true;
                if (player.level() instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 8; i++) {
                        double scale = 0.6 + (i * 0.4);
                        serverLevel.sendParticles(ParticleTypes.CRIT,
                                player.getX() + horiz.x * scale,
                                player.getY() + 1.1 + (i * 0.02),
                                player.getZ() + horiz.z * scale,
                                2, 0.05, 0.05, 0.05, 0.01);
                    }
                }
            }
            if (combatState.getActionKeyTicks() <= 1 && state.getLongSwordThrustLockTicks() <= 0) {
                state.setLongSwordThrustLockTicks(5);
            }
        }
        if (combatState != null && "spirit_helm_breaker".equals(combatState.getActionKey())
            && combatState.getActionKeyTicks() > 0) {
            // Debug: Spirit Helm Breaker movement tick
            MHWeaponsMod.LOGGER.info("LS debug: Helm Breaker tick player={} ticksLeft={} pos=({}, {}, {}) vel=({}, {}, {})",
                player.getId(), combatState.getActionKeyTicks(),
                String.format("%.2f", player.getX()),
                String.format("%.2f", player.getY()),
                String.format("%.2f", player.getZ()),
                String.format("%.2f", player.getDeltaMovement().x),
                String.format("%.2f", player.getDeltaMovement().y),
                String.format("%.2f", player.getDeltaMovement().z));
            boolean descending = combatState.getActionKeyTicks() <= 10;

                // Allow steering while rising; lock direction once the dive begins.
                Vec3 lookDir = player.getLookAngle();
                Vec3 lookHorizontal = new Vec3(lookDir.x, 0.0, lookDir.z);
                Vec3 storedDir = state.getLongSwordHelmBreakerDir();
                if (!descending) {
                    // During rising/hanging phase, allow significant air control with smooth physics
                    Vec3 currentVel = player.getDeltaMovement();
                    
                    // Smooth Y velocity handling
                    double newY = currentVel.y;
                    if (newY > 0) {
                         newY *= 0.85; // Smooth deceleration on ascent
                    } else {
                         newY = Math.max(newY - 0.05, -0.2); // Gentle drift on hang
                    }

                    // Horizontal damping
                    player.setDeltaMovement(currentVel.x * 0.9, newY, currentVel.z * 0.9);
                    
                    if (lookHorizontal.lengthSqr() > 0.0001) {
                        state.setLongSwordHelmBreakerDir(lookHorizontal.normalize());
                    }
                } else {
                    if (storedDir.lengthSqr() > 0.0001) {
                        storedDir = storedDir.normalize();
                    } else if (lookHorizontal.lengthSqr() > 0.0001) {
                        storedDir = lookHorizontal.normalize();
                        state.setLongSwordHelmBreakerDir(storedDir);
                    }
                    
                    Vec3 horizontal = storedDir.lengthSqr() > 0.0001 ? storedDir : Vec3.ZERO;
                    // Accelerating fall for "curvy" feel with smoother start
                    int diveTicks = 10 - combatState.getActionKeyTicks();
                    double vertical = -0.5 - (0.55 * diveTicks); // Starts at -0.5, ramps up to ~-6.0
                    player.setDeltaMovement(horizontal.scale(0.0).add(0.0, vertical, 0.0));
                }
                player.hurtMarked = true;
                player.fallDistance = 0.0f;
                if (descending) {
                    player.invulnerableTime = Math.max(player.invulnerableTime, 14);
                }
                if (combatState.getActionKeyTicks() <= 2) {
                    player.setDeltaMovement(Vec3.ZERO);
                    player.hurtMarked = true;
                    player.invulnerableTime = Math.max(player.invulnerableTime, 8);
                }


                if (descending && player.level() instanceof ServerLevel serverLevel) {
                    int spiritLevel = state.getLongSwordHelmBreakerSpiritLevel();
                    if (spiritLevel <= 0) {
                        spiritLevel = state.getSpiritLevel();
                    }
                    float r = spiritLevel >= 3 ? 1.0f : (spiritLevel == 2 ? 1.0f : 1.0f);
                    float g = spiritLevel >= 3 ? 0.2f : (spiritLevel == 2 ? 0.85f : 1.0f);
                    float b = spiritLevel >= 3 ? 0.2f : (spiritLevel == 2 ? 0.1f : 1.0f);

                    double cx = player.getX();
                    double cz = player.getZ();
                    double top = player.getY() + 1.8;
                    double bottom = player.getY() - 1.2;
                    int steps = 8;
                    for (int i = 0; i <= steps; i++) {
                        double y = top - ((top - bottom) * (i / (double) steps));
                        serverLevel.sendParticles(ParticleTypes.GLOW, cx, y, cz, 1, 0.02, 0.02, 0.02, 0.0);
                    }
                    // Blade trail on the fast dive for clearer feedback.
                    Vec3 base = player.position().add(0.0, 0.6, 0.0);
                    // Re-calculate diveDir as horizontal might not be visible here if it was defined in else/if block
                    Vec3 stored = state.getLongSwordHelmBreakerDir();
                    Vec3 h = stored.lengthSqr() > 0.0001 ? stored : Vec3.ZERO;
                    Vec3 diveDir = h.lengthSqr() > 0.0001 ? h : lookDir.normalize();

                    for (int i = 0; i < 4; i++) {
                        double scale = 0.4 + (i * 0.3);
                        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                                base.x + diveDir.x * scale,
                                base.y - (i * 0.2),
                                base.z + diveDir.z * scale,
                                1, 0.02, 0.02, 0.02, 0.0);
                        serverLevel.sendParticles(new DustParticleOptions(new Vector3f(r, g, b), 1.0f),
                            base.x + diveDir.x * scale,
                            base.y - (i * 0.2),
                            base.z + diveDir.z * scale,
                            4, 0.02, 0.02, 0.02, 0.0);
                    }
                }
                if (descending && (player.onGround() || combatState.getActionKeyTicks() <= 1) && player.level() instanceof ServerLevel serverLevel) {
                    int spiritLevel = state.getLongSwordHelmBreakerSpiritLevel();
                    if (spiritLevel <= 0) {
                        spiritLevel = state.getSpiritLevel();
                    }
                    spiritLevel = Math.max(1, spiritLevel);
                Vec3 impactLookDir = player.getLookAngle();
                Vec3 impactStoredDir = state.getLongSwordHelmBreakerDir();
                Vec3 impactDir = impactStoredDir.lengthSqr() > 0.0001
                    ? impactStoredDir.normalize()
                    : new Vec3(impactLookDir.x, 0.0, impactLookDir.z).normalize();
                double reach = 0.75;
                Vec3 start = player.position().add(0.0, 0.8, 0.0);
                Vec3 end = start.add(impactDir.scale(reach));
                AABB box = new AABB(start, end).inflate(0.6);
                double base = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                double scale = switch (Math.min(3, spiritLevel + 1)) {
                    case 1 -> 0.65;
                    case 2 -> 0.85;
                    default -> 1.15;
                };
                float hitDamage = (float) Math.max(1.0, base * scale);
                for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)) {
                    entity.hurt(player.damageSources().playerAttack(player), hitDamage);
                }
                // Open follow-up window on landing impact
                int followupWindow = 25; // Window for Spirit Release Slash trigger (slightly extended for network latency)
                state.setLongSwordHelmBreakerFollowupTicks(followupWindow);
                state.setLongSwordHelmBreakerFollowupStage(0);
                combatState.setActionKey("helm_breaker_followup");
                combatState.setActionKeyTicks(followupWindow);
            }
        }

        if (state.getLongSwordAltComboTicks() > 0) {
            state.setLongSwordAltComboTicks(state.getLongSwordAltComboTicks() - 1);
        }

        if (state.getLongSwordHelmBreakerFollowupTicks() > 0) {
            state.setLongSwordHelmBreakerFollowupTicks(state.getLongSwordHelmBreakerFollowupTicks() - 1);
        }

        if (state.getLongSwordThrustLockTicks() > 0) {
            state.setLongSwordThrustLockTicks(state.getLongSwordThrustLockTicks() - 1);
        }

        if (state.getLongSwordHelmBreakerCooldown() > 0) {
            state.setLongSwordHelmBreakerCooldown(state.getLongSwordHelmBreakerCooldown() - 1);
        }

        if (state.getSpiritLevel() > 0) {
            int maxTicks = resolveSpiritLevelMaxTicks(state.getSpiritLevel());
            if (state.getSpiritLevelTicks() <= 0) {
                state.setSpiritLevelTicks(maxTicks);
            }
            int remaining = state.getSpiritLevelTicks() - 1;
            if (remaining <= 0) {
                int next = Math.max(0, state.getSpiritLevel() - 1);
                state.setSpiritLevel(next);
                if (next > 0) {
                    state.setSpiritLevelTicks(resolveSpiritLevelMaxTicks(next));
                } else {
                    state.setSpiritLevelTicks(0);
                }
            } else {
                state.setSpiritLevelTicks(remaining);
            }
        }

        ItemStack mainHand = player.getMainHandItem();
        String weaponId = null;
        if (mainHand.getItem() instanceof WeaponIdProvider weaponItem) {
            weaponId = weaponItem.getWeaponId();
        }

        if ("longsword".equals(weaponId)) {
            // Combo Resets
            int comboTimeout = 100; // Increased window (5s) for more relaxed inputs
            if (player.tickCount - state.getLongSwordSpiritComboTick() > comboTimeout) {
                state.setLongSwordSpiritComboIndex(0);
            }
            if (player.tickCount - state.getLongSwordOverheadComboTick() > comboTimeout) {
                state.setLongSwordOverheadComboIndex(0);
            }
            if (player.tickCount - state.getLongSwordThrustComboTick() > comboTimeout) {
                state.setLongSwordThrustComboIndex(0);
            }


            // Fade Slash delayed forward motion
            int fadeTicks = state.getLongSwordFadeSlashTicks();
            if (fadeTicks > 0) {
                state.setLongSwordFadeSlashTicks(fadeTicks - 1);
                if (fadeTicks == 5) {
                    Vec3 look = player.getLookAngle();
                    Vec3 forward = new Vec3(look.x, 0.0, look.z);
                    if (forward.lengthSqr() > 0.0001) {
                         forward = forward.normalize().scale(0.8);
                         player.setDeltaMovement(forward.x, player.getDeltaMovement().y + 0.1, forward.z);
                         player.hurtMarked = true;
                    }
                }
            }

            // Special Sheathe upkeep and auto-cancel on movement or timeout
            if (state.isLongSwordSpecialSheathe()) {
                int remainingSheathe = state.getLongSwordSheatheTicks();
                double horizSpeed = player.getDeltaMovement().horizontalDistance();
                boolean movingTooFast = horizSpeed > 0.12 || player.isSprinting() || player.isSwimming();
                boolean timeExpired = remainingSheathe <= 0;
                if (timeExpired || movingTooFast) {
                    state.setLongSwordSpecialSheathe(false);
                    state.setLongSwordSheatheTicks(0);
                } else {
                    state.setLongSwordSheatheTicks(remainingSheathe - 1);
                }
            }

            float decay = WeaponDataResolver.resolveFloat(player, "spiritGauge", "decayPerTick", 0.05f);
            if (decay > 0.0f && state.getSpiritGauge() > 0.0f) {
                state.addSpiritGauge(-decay);
            }
            if (state.getSpiritLevel() > 0) {
                int amp = Math.max(0, state.getSpiritLevel() - 1);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, amp, false, false));
            }
        }

        // Dual Blades: delegate all tick logic to DualBladesHandler
        if ("dual_blades".equals(weaponId)) {
            DualBladesHandler.tick(player, state);
        } else {
            // Non-DB demon mode (legacy support)
            if (state.isDemonMode()) {
                state.addDemonGauge(-0.4f);
                if (state.getDemonGauge() <= 0.0f) {
                    state.setDemonMode(false);
                }
            }
            boolean arch = !state.isDemonMode() && state.getDemonGauge() >= 50.0f;
            state.setArchDemon(arch);
        }

        if (state.getHammerChargeTicks() > 0) {
            state.setHammerChargeTicks(state.getHammerChargeTicks() - 1);
        } else if (state.getHammerChargeLevel() > 0) {
            state.setHammerChargeLevel(0);
        }

        if (state.getGunlanceCooldown() > 0) {
            state.setGunlanceCooldown(state.getGunlanceCooldown() - 1);
        }

        // ── Bowgun tick ──
        if ("bowgun".equals(weaponId)) {
            org.example.common.combat.bowgun.BowgunHandler.tick(player, state);
            org.example.common.combat.bowgun.BowgunGuardHandler.tickGuard(player, state);
        }

        // Sync max shells based on held weapon's shelling type
        if ("gunlance".equals(weaponId) && player.getMainHandItem().getItem() instanceof org.example.item.GunlanceItem gl) {
            gl.syncMaxShells(state);
        }

        if (state.getGunlanceWyvernfireCooldown() > 0) {
            state.setGunlanceWyvernfireCooldown(state.getGunlanceWyvernfireCooldown() - 1);
        } else if (state.getGunlanceWyvernFireGauge() < 2.0f) {
             // Recharge gauge
             state.addGunlanceWyvernFireGauge(0.00042f);
        }

        // Wyvern's Fire Charge Logic
        if (state.isGunlanceCharging()) {
            // Block horizontal movement while charging on the server as well
            Vec3 vel = player.getDeltaMovement();
            player.setDeltaMovement(0.0, vel.y, 0.0);
            player.hurtMarked = true;

            int ticks = state.getGunlanceChargeTicks();
            if (ticks > 0) {
                state.setGunlanceChargeTicks(ticks - 1);
                // Trigger fire at end of charge
                if (ticks == 1) {
                    state.setGunlanceCharging(false);
                    state.setGunlanceChargeTicks(0);
                    if (player instanceof ServerPlayer) {
                        org.example.item.GunlanceItem gl = (org.example.item.GunlanceItem) player.getMainHandItem().getItem();
                        gl.useWyvernFireBlast(player.level(), player, state); // Separated blast method
                    }
                }
            } else {
                 state.setGunlanceCharging(false);
            }
        }

        if (state.getSwitchAxeFrcCooldown() > 0) {
            state.setSwitchAxeFrcCooldown(state.getSwitchAxeFrcCooldown() - 1);
        }

        if (state.getSwitchAxeAmpGauge() > 0.0f && !state.isSwitchAxeSwordMode()) {
            state.addSwitchAxeAmpGauge(-0.1f);
        }

        // Switch Gauge passive regen in Axe Mode or when sheathed
        if (!"switch_axe".equals(weaponId) || !state.isSwitchAxeSwordMode()) {
            if (state.getSwitchAxeSwitchGauge() < 100.0f) {
                float regenRate = state.isSwitchAxePowerAxe() ? 0.15f : 0.08f;
                state.addSwitchAxeSwitchGauge(regenRate);
            }
        }

        // Switch Gauge decay in Sword Mode (slow passive drain)
        if ("switch_axe".equals(weaponId) && state.isSwitchAxeSwordMode()) {
            state.addSwitchAxeSwitchGauge(-0.05f);
            if (state.getSwitchAxeSwitchGauge() <= 0.0f) {
                // Force morph to axe
                state.setSwitchAxeSwordMode(false);
                state.setSwitchAxeComboIndex(0);
            }
        }

        // Power Axe timer countdown
        if (state.isSwitchAxePowerAxe()) {
            int remaining = state.getSwitchAxePowerAxeTicks() - 1;
            if (remaining <= 0) {
                state.setSwitchAxePowerAxe(false);
                state.setSwitchAxePowerAxeTicks(0);
            } else {
                state.setSwitchAxePowerAxeTicks(remaining);
            }
        }

        // Amped State timer countdown
        if (state.isSwitchAxeAmped()) {
            int remaining = state.getSwitchAxeAmpedTicks() - 1;
            if (remaining <= 0) {
                state.setSwitchAxeAmped(false);
                state.setSwitchAxeAmpedTicks(0);
                state.setSwitchAxeAmpGauge(0.0f);
            } else {
                state.setSwitchAxeAmpedTicks(remaining);
            }
        }

        // Counter window countdown
        if (state.getSwitchAxeCounterTicks() > 0) {
            state.setSwitchAxeCounterTicks(state.getSwitchAxeCounterTicks() - 1);
        }

        // Combo timeout reset
        if ("switch_axe".equals(weaponId)) {
            int comboTimeout = 30;
            if ((player.tickCount - state.getSwitchAxeComboTick()) > comboTimeout) {
                state.setSwitchAxeComboIndex(0);
                state.setSwitchAxeWildSwingCount(0);
            }
        }

        // ── Charge Blade timers ──

        // CB: Shield Charge timer countdown
        if (state.isCbShieldCharged()) {
            int remaining = state.getCbShieldChargeTicks() - 1;
            if (remaining <= 0) {
                state.setCbShieldCharged(false);
                state.setCbShieldChargeTicks(0);
            } else {
                state.setCbShieldChargeTicks(remaining);
            }
        }

        // CB: Sword Boost timer countdown
        if (state.isCbSwordBoosted()) {
            int remaining = state.getCbSwordBoostTicks() - 1;
            if (remaining <= 0) {
                state.setCbSwordBoosted(false);
                state.setCbSwordBoostTicks(0);
            } else {
                state.setCbSwordBoostTicks(remaining);
            }
        }

        // CB: Power Axe timer countdown
        if (state.isCbPowerAxe()) {
            int remaining = state.getCbPowerAxeTicks() - 1;
            if (remaining <= 0) {
                state.setCbPowerAxe(false);
                state.setCbPowerAxeTicks(0);
            } else {
                state.setCbPowerAxeTicks(remaining);
            }
        }

        // CB: Guard Point window countdown
        if (state.getCbGuardPointTicks() > 0) {
            state.setCbGuardPointTicks(state.getCbGuardPointTicks() - 1);
        }

        // CB: Combo timeout reset
        if ("charge_blade".equals(weaponId)) {
            int comboTimeout = 20;
            if ((player.tickCount - state.getCbComboTick()) > comboTimeout) {
                state.setCbComboIndex(0);
                state.setCbDischargeStage(0);
            }
        }

        if (state.getInsectExtractTicks() > 0) {
            state.setInsectExtractTicks(state.getInsectExtractTicks() - 1);
            if (state.getInsectExtractTicks() <= 0) {
                state.setInsectRed(false);
                state.setInsectWhite(false);
                state.setInsectOrange(false);
            }
        }

        if (state.getInsectTripleFinisherTicks() > 0) {
            state.setInsectTripleFinisherTicks(state.getInsectTripleFinisherTicks() - 1);
            if (state.getInsectTripleFinisherTicks() <= 0) {
                state.setInsectTripleFinisherStage(0);
            }
        }

        if (state.getInsectAerialTicks() > 0) {
            state.setInsectAerialTicks(state.getInsectAerialTicks() - 1);
            player.fallDistance = 0.0f;
        }

        if (state.getInsectExtractTicks() > 0) {
            if (state.isInsectRed()) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false));
            }
            if (state.isInsectWhite()) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 0, false, false));
            }
            if (state.isInsectOrange()) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false));
            }
        }

        if (state.getHornBuffTicks() > 0) {
            state.setHornBuffTicks(state.getHornBuffTicks() - 1);
        }

        if (state.getHornSpecialGuardTicks() > 0) {
            state.setHornSpecialGuardTicks(state.getHornSpecialGuardTicks() - 1);
            if (state.getHornSpecialGuardTicks() <= 0 && combatState != null) {
                combatState.setGuardPointActive(false);
            }
        }

        if (state.getHornMelodyPlayTicks() > 0) {
            state.setHornMelodyPlayTicks(state.getHornMelodyPlayTicks() - 1);
        }

        if (state.getHornLastMelodyEnhanceTicks() > 0) {
            state.setHornLastMelodyEnhanceTicks(state.getHornLastMelodyEnhanceTicks() - 1);
        }

        if (state.getHornAttackSmallTicks() > 0) {
            state.setHornAttackSmallTicks(state.getHornAttackSmallTicks() - 1);
        }

        if (state.getHornAttackLargeTicks() > 0) {
            state.setHornAttackLargeTicks(state.getHornAttackLargeTicks() - 1);
        }

        if (state.getHornDefenseLargeTicks() > 0) {
            state.setHornDefenseLargeTicks(state.getHornDefenseLargeTicks() - 1);
        }

        if (state.getHornMelodyHitTicks() > 0) {
            state.setHornMelodyHitTicks(state.getHornMelodyHitTicks() - 1);
        }

        if (state.getHornStaminaBoostTicks() > 0) {
            state.setHornStaminaBoostTicks(state.getHornStaminaBoostTicks() - 1);
        }

        if (state.getHornAffinityTicks() > 0) {
            state.setHornAffinityTicks(state.getHornAffinityTicks() - 1);
        }

        if (state.getLancePerfectGuardTicks() > 0) {
            state.setLancePerfectGuardTicks(state.getLancePerfectGuardTicks() - 1);
        }

        if (state.isLancePowerGuard()) {
            player.causeFoodExhaustion(0.03f);
        }

        if (state.getTonfaFlyingTicks() > 0) {
            state.setTonfaFlyingTicks(state.getTonfaFlyingTicks() - 1);
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x * 0.95, Math.max(motion.y, 0.02), motion.z * 0.95);
            player.hurtMarked = true;
        }

        if (state.getTonfaComboGauge() > 0.0f && player.tickCount % 4 == 0) {
            state.addTonfaComboGauge(-0.2f);
        }

        if (player.onGround() && state.isTonfaDoubleJumped()) {
            state.setTonfaDoubleJumped(false);
        }

        if (state.getMagnetTargetTicks() > 0) {
            state.setMagnetTargetTicks(state.getMagnetTargetTicks() - 1);
            if (state.getMagnetTargetTicks() <= 0) {
                state.setMagnetTargetId(-1);
            }
        }

        boolean magnetZipActive = combatState != null
                && "magnet_zip".equals(combatState.getActionKey())
                && "magnet_spike".equals(weaponId);
        if ("magnet_spike".equals(weaponId) && state.getMagnetZipAnimTicks() > 0) {
            LivingEntity target = player.level().getEntity(state.getMagnetTargetId()) instanceof LivingEntity living
                    ? living
                    : null;
            if (target != null) {
                Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
                Vec3 toTarget = targetPos.subtract(player.position().add(0, player.getBbHeight() * 0.5, 0));
                double distance = toTarget.length();
                if (distance > 0.001) {
                    Vec3 dir = toTarget.normalize();
                    Vec3 motion = player.getDeltaMovement();
                    double towards = motion.dot(dir);
                    if (towards > 0) {
                        float slowdownDistance = WeaponDataResolver.resolveFloat(player, "magnetZip", "slowdownDistance", 1.2f);
                        float slowdownFactor = WeaponDataResolver.resolveFloat(player, "magnetZip", "slowdownFactor", 0.25f);
                        if (distance <= slowdownDistance) {
                            double currentSpeed = motion.length();
                            double targetSpeed = Math.min(currentSpeed * slowdownFactor, Math.max(0.05, distance * 0.6));
                            player.setDeltaMovement(dir.scale(targetSpeed));
                            player.hurtMarked = true;
                        }
                    }
                }
            }
        }
        if (magnetZipActive && (player.swinging || player.swingTime > 0)) {
            Vec3 motion = player.getDeltaMovement();
            AABB sweepBox = player.getBoundingBox().expandTowards(motion).inflate(0.4);
            if (player.level() instanceof ServerLevel serverLevel) {
                for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, sweepBox, e -> e != player && e.isAlive())) {
                    if (state.registerMagnetZipHit(entity.getId())) {
                        float base = state.isMagnetSpikeImpactMode() ? 6.5f : 5.0f;
                        float gaugeBonus = 1.0f + 0.4f * (state.getMagnetGauge() / 100.0f); // up to +40% at full gauge
                        float damage = base * gaugeBonus;
                        entity.hurt(player.damageSources().playerAttack(player), damage);
                        entity.push(motion.x * 0.35, 0.15, motion.z * 0.35);
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                                6, 0.15, 0.2, 0.15, 0.02);
                    }
                }
            }
        }

        if (state.getAccelDashTicks() > 0) {
            state.setAccelDashTicks(state.getAccelDashTicks() - 1);
            if (state.getAccelFuel() > 0) {
                Vec3 forward = player.getLookAngle().normalize().scale(0.6);
                player.setDeltaMovement(forward.x, player.getDeltaMovement().y, forward.z);
                player.hurtMarked = true;
                state.addAccelFuel(-1);
            }
        }

        if (state.getAccelParryTicks() > 0) {
            state.setAccelParryTicks(state.getAccelParryTicks() - 1);
        }

        if (state.getAccelFuel() < 100 && player.tickCount % 20 == 0) {
            state.addAccelFuel(1);
        }

        if (!player.isUsingItem() && state.getBowCharge() > 0.0f) {
            state.setBowCharge(0.0f);
        }

        float maxStamina = StaminaHelper.resolveMax(player, 100.0f);
        state.setMaxStamina(maxStamina);
        if (state.getStamina() > maxStamina) {
            state.setStamina(maxStamina);
        }

        if (state.getStaminaRecoveryDelay() > 0) {
            state.setStaminaRecoveryDelay(state.getStaminaRecoveryDelay() - 1);
        } else if (state.getStamina() < state.getMaxStamina() && !player.isSprinting() && !state.isDemonMode()) {
            float regen = StaminaHelper.resolveRegenPerTick(player, state, 1.5f);
            state.addStamina(regen);
        }

        if (player.isSprinting() && player.getDeltaMovement().lengthSqr() > 0.01) {
            float drain = StaminaHelper.applyCost(player, 0.3f);
            state.addStamina(-drain);
            state.setStaminaRecoveryDelay(20);
        }

        if (state.getStamina() <= 0) {
            player.setSprinting(false);
            if (state.isDemonMode()) {
                state.setDemonMode(false);
            }
            if (state.getInsectAerialTicks() > 0) {
                state.setInsectAerialTicks(0);
            }
        }

        if (state.isDirty()) {
            syncState(player, state);
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        LivingEntity attacker = null;
        if (event.getSource().getEntity() instanceof LivingEntity livingEntity) {
            attacker = livingEntity;
        } else if (event.getSource().getDirectEntity() instanceof LivingEntity directEntity) {
            attacker = directEntity;
        }
        if (!(attacker instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide) {
            return;
        }
        PlayerWeaponState state = CapabilityUtil.getPlayerWeaponState(player);
        if (state == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof WeaponIdProvider weaponItem)) {
            return;
        }
        String weaponId = weaponItem.getWeaponId();

        // Cancel Spirit Helm Breaker if the player takes damage mid-move
        PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(player);
        if (combatState != null && "spirit_helm_breaker".equals(combatState.getActionKey()) && combatState.getActionKeyTicks() > 0) {
            combatState.setActionKey(null);
            combatState.setActionKeyTicks(0);
        }

        switch (weaponId) {
            case "longsword" -> state.addSpiritGauge(8.0f);
            case "dual_blades" -> DualBladesHandler.onHit(player, state, event.getEntity(), event.getAmount());
            case "switch_axe" -> {
                // Amp gauge gain on hit
                float ampGain = state.isSwitchAxeSwordMode() ? 8.0f : 5.0f;
                state.addSwitchAxeAmpGauge(ampGain);
                // Switch gauge gain from axe hits
                if (!state.isSwitchAxeSwordMode()) {
                    float sgGain = state.isSwitchAxePowerAxe() ? 5.0f : 3.0f;
                    state.addSwitchAxeSwitchGauge(sgGain);
                }
                // Check for amped state activation
                if (!state.isSwitchAxeAmped() && state.getSwitchAxeAmpGauge() >= 100.0f) {
                    state.setSwitchAxeAmped(true);
                    state.setSwitchAxeAmpedTicks(900); // 45 seconds
                }
            }
            case "charge_blade" -> {
                if (state.isChargeBladeSwordMode()) {
                    int charge = state.getChargeBladeCharge() + 10;
                    if (charge >= 100 && state.getChargeBladePhials() < 5) {
                        state.setChargeBladePhials(state.getChargeBladePhials() + 1);
                        charge -= 100;
                    }
                    state.setChargeBladeCharge(charge);
                }
            }
            case "tonfa" -> state.addTonfaComboGauge(6.0f);
            case "magnet_spike" -> state.addMagnetGauge(6.0f);
            case "accel_axe" -> state.addAccelFuel(2);
            case "bowgun" -> org.example.common.combat.bowgun.BowgunHandler.onHit(player, state);
            default -> {
            }
        }

        if ("accel_axe".equals(weaponId) && state.getAccelParryTicks() > 0 && state.getAccelFuel() > 0) {
            // Blast parry: negate damage and retaliate
            float cost = StaminaHelper.applyCost(player, 8.0f);
            if (state.getStamina() >= cost) {
                state.addStamina(-cost);
                state.addAccelFuel(-5);
                event.setCanceled(true);
                if (player.level() instanceof ServerLevel serverLevel) {
                    AABB box = player.getBoundingBox().inflate(3.0);
                    for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)) {
                        entity.hurt(player.damageSources().playerAttack(player), 5.0f);
                    }
                    serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.5, player.getZ(), 16, 0.6, 0.3, 0.6, 0.05);
                    serverLevel.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.5, player.getZ(), 16, 0.5, 0.3, 0.5, 0.05);
                }
            }
            state.setAccelParryTicks(0);
        }

        if (state.isDirty()) {
            syncState(player, state);
        }
    }

    private void syncState(Player player, PlayerWeaponState state) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
            new PlayerWeaponStateS2CPacket(player.getId(), state.serializeNBT()));
        state.clearDirty();
    }
}
