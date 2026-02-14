package org.example.common.events.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.example.MHWeaponsMod;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.network.ModNetwork;
import org.example.common.network.packet.PlayAttackAnimationS2CPacket;
import org.example.common.data.WeaponDataResolver;
import org.example.common.util.CapabilityUtil;
import org.example.item.GeoWeaponItem;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

public final class ActionKeyEvents {
    private static int resolveSpiritLevelMaxTicks(int level) {
        return switch (Math.max(1, Math.min(3, level))) {
            case 1 -> 1200;
            case 2 -> 900;
            default -> 700;
        };
    }

    @SubscribeEvent
    @SuppressWarnings("null")
    public void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        PlayerCombatState state = CapabilityUtil.getPlayerCombatState(player);
        if (state == null) {
            return;
        }
        PlayerWeaponState weaponState = CapabilityUtil.getPlayerWeaponState(player);
        String currentAction = state.getActionKey();
        if (weaponState != null && player.getMainHandItem().getItem() instanceof GeoWeaponItem weaponItem
            && "longsword".equals(weaponItem.getWeaponId())) {
            if (weaponState.isChargingAttack()) {
                event.setCanceled(true);
                return;
            }
            if ("helm_breaker_followup".equals(currentAction) && weaponState.getLongSwordHelmBreakerFollowupTicks() <= 0) {
                state.setActionKey(null);
                state.setActionKeyTicks(0);
                weaponState.setLongSwordHelmBreakerFollowupStage(0);
                currentAction = null;
            }

            if ("spirit_helm_breaker_followup_left".equals(currentAction) && event.getTarget() instanceof LivingEntity target) {
                double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                int spiritLevel = weaponState.getLongSwordHelmBreakerSpiritLevel();
                if (spiritLevel <= 0) {
                    spiritLevel = weaponState.getSpiritLevel();
                }
                spiritLevel = Math.max(1, spiritLevel);
                double spiritScale = 0.9 + (0.18 * (spiritLevel - 1));
                float damage = (float) Math.max(1.0, base * spiritScale);
                target.hurt(player.damageSources().playerAttack(player), damage);
                Vec3 dir = player.getLookAngle().normalize();
                target.push(dir.x * 0.3, 0.2, dir.z * 0.3);
                if (player.level() instanceof ServerLevel serverLevel) {
                    float r = spiritLevel >= 3 ? 1.0f : (spiritLevel == 2 ? 1.0f : 1.0f);
                    float g = spiritLevel >= 3 ? 0.2f : (spiritLevel == 2 ? 0.85f : 1.0f);
                    float b = spiritLevel >= 3 ? 0.2f : (spiritLevel == 2 ? 0.1f : 1.0f);
                    DustParticleOptions starColor = new DustParticleOptions(new Vector3f(r, g, b), 1.0f);
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                            target.getX() + dir.x * 0.6,
                            target.getY() + 1.0,
                            target.getZ() + dir.z * 0.6,
                            6, 0.06, 0.06, 0.06, 0.0);
                    serverLevel.sendParticles(starColor,
                            target.getX(), target.getY() + 0.9, target.getZ(),
                            12, 0.2, 0.2, 0.2, 0.02);
                }
                state.setActionKey(null);
                state.setActionKeyTicks(0);
                weaponState.setLongSwordHelmBreakerFollowupStage(1);
                return;
            }

            if ("spirit_helm_breaker_followup_right".equals(currentAction) && event.getTarget() instanceof LivingEntity target) {
                double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                int spiritLevel = Math.max(1, weaponState.getSpiritLevel());
                double spiritScale = 1.0 + (0.2 * (spiritLevel - 1));
                float damage = (float) Math.max(1.0, base * spiritScale);
                Vec3 dir = player.getLookAngle().normalize();
                target.hurt(player.damageSources().playerAttack(player), damage);
                target.push(dir.x * 0.35, 0.18, dir.z * 0.35);
                if (player.level() instanceof ServerLevel serverLevel) {
                    float r = spiritLevel >= 3 ? 1.0f : (spiritLevel == 2 ? 1.0f : 1.0f);
                    float g = spiritLevel >= 3 ? 0.2f : (spiritLevel == 2 ? 0.85f : 1.0f);
                    float b = spiritLevel >= 3 ? 0.2f : (spiritLevel == 2 ? 0.1f : 1.0f);
                    DustParticleOptions starColor = new DustParticleOptions(new Vector3f(r, g, b), 1.0f);
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                            target.getX() + dir.x * 0.6,
                            target.getY() + 1.0,
                            target.getZ() + dir.z * 0.6,
                            8, 0.08, 0.08, 0.08, 0.0);
                    serverLevel.sendParticles(starColor,
                            target.getX(), target.getY() + 0.9, target.getZ(),
                            16, 0.24, 0.24, 0.24, 0.02);
                }
                state.setActionKey(null);
                state.setActionKeyTicks(0);
                weaponState.setLongSwordHelmBreakerFollowupTicks(0);
                weaponState.setLongSwordHelmBreakerFollowupStage(0);
                return;
            }

            // Iai Slash (from Special Sheathe)
            if ("iai_slash".equals(currentAction) && event.getTarget() instanceof LivingEntity target) {
                double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                float mv = WeaponDataResolver.resolveMotionValue(player, "iai_slash", 1.1f);
                float damage = (float) Math.max(1.0, base * mv);
                target.hurt(player.damageSources().playerAttack(player), damage);

                // Minor sustain buff on successful hit
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, true));

                state.setActionKey(null);
                state.setActionKeyTicks(0);
                return;
            }

            // Iai Spirit Slash (counter strike from Special Sheathe)
            if ("iai_spirit_slash".equals(currentAction) && event.getTarget() instanceof LivingEntity target) {
                double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                float mv = WeaponDataResolver.resolveMotionValue(player, "iai_spirit_slash", 1.4f);
                float damage = (float) Math.max(1.0, base * mv);
                target.hurt(player.damageSources().playerAttack(player), damage);

                // Level up on success and clear gauge
                int nextLevel = Math.min(3, weaponState.getSpiritLevel() + 1);
                weaponState.setSpiritLevel(nextLevel);
                weaponState.setSpiritLevelTicks(resolveSpiritLevelMaxTicks(nextLevel));
                weaponState.setSpiritGauge(0.0f);

                // Brief offensive reward (knockback / particles)
                Vec3 dir = player.getLookAngle().normalize();
                target.push(dir.x * 0.3, 0.2, dir.z * 0.3);
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                            target.getX(), target.getY() + 1.0, target.getZ(),
                            10, 0.1, 0.1, 0.1, 0.02);
                }

                state.setActionKey(null);
                state.setActionKeyTicks(0);
                return;
            }

            // Spirit Helm Breaker dive impact
            if ("spirit_helm_breaker".equals(currentAction) && event.getTarget() instanceof LivingEntity target) {
                if (weaponState.getLongSwordHelmBreakerFollowupTicks() > 0) {
                    return;
                }
                MHWeaponsMod.LOGGER.info("LS debug: Spirit Helm Breaker impact player={} target={} pos=({}, {}, {})",
                        player.getId(), target.getId(),
                        String.format("%.2f", player.getX()),
                        String.format("%.2f", player.getY()),
                        String.format("%.2f", player.getZ()));
                int spiritLevel = Math.max(1, weaponState.getSpiritLevel());
                if (player.level() instanceof ServerLevel serverLevel) {
                    double cx = target.getX();
                    double cz = target.getZ();
                    double top = target.getY() + 2.0;
                    double bottom = target.getY() - 1.2;
                    int steps = 10;
                    float r = spiritLevel >= 3 ? 1.0f : (spiritLevel == 2 ? 1.0f : 1.0f);
                    float g = spiritLevel >= 3 ? 0.2f : (spiritLevel == 2 ? 0.85f : 1.0f);
                    float b = spiritLevel >= 3 ? 0.2f : (spiritLevel == 2 ? 0.1f : 1.0f);
                    DustParticleOptions starColor = new DustParticleOptions(new Vector3f(r, g, b), 1.2f);

                    for (int i = 0; i <= steps; i++) {
                        double y = top - ((top - bottom) * (i / (double) steps));
                        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, cx, y, cz, 1, 0.02, 0.02, 0.02, 0.0);
                    }
                    Vec3 dir = target.position().subtract(player.position()).normalize();
                    for (int i = 0; i < 10; i++) {
                        double scale = 0.7 + (i * 0.35);
                        serverLevel.sendParticles(ParticleTypes.FIREWORK,
                                cx + dir.x * scale,
                                target.getY() + 0.6,
                                cz + dir.z * scale,
                                1, 0.08, 0.08, 0.08, 0.02);
                        serverLevel.sendParticles(starColor,
                                cx + dir.x * scale,
                                target.getY() + 0.6,
                                cz + dir.z * scale,
                                3, 0.05, 0.05, 0.05, 0.0);
                    }
                    serverLevel.sendParticles(starColor,
                            cx, target.getY() + 0.9, cz, 18, 0.35, 0.45, 0.35, 0.02);
                }
                double reach = 7.5;
                Vec3 dir = target.position().subtract(player.position()).normalize();
                Vec3 start = player.position().add(0.0, 0.8, 0.0);
                Vec3 end = start.add(dir.scale(reach));
                AABB box = new AABB(start, end).inflate(1.6);
                double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                int powerLevel = Math.min(3, weaponState.getSpiritLevel() + 1); // approximate level before the thrust decrement
                double scale = switch (powerLevel) {
                    case 1 -> 0.65;
                    case 2 -> 0.85;
                    default -> 1.15;
                };
                float hitDamage = (float) Math.max(1.0, base * scale);
                Set<LivingEntity> hitEntities = new HashSet<>();
                for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != player)) {
                    if (hitEntities.add(entity)) {
                        entity.hurt(player.damageSources().playerAttack(player), hitDamage);
                    }
                }
                target.hurt(player.damageSources().playerAttack(player), hitDamage);

                // Prepare a short follow-up window for the post-Helm spin finisher
                weaponState.setLongSwordHelmBreakerFollowupTicks(18);
                weaponState.setLongSwordHelmBreakerFollowupStage(0);
                state.setActionKey("helm_breaker_followup");
                state.setActionKeyTicks(18);
                return;
            }

            // Spirit Thrust -> Helm Breaker transition (requires Spirit level)
            if ("spirit_thrust".equals(currentAction)) {
                if (weaponState.getSpiritLevel() <= 0) {
                    state.setActionKey(null);
                    state.setActionKeyTicks(0);
                    return;
                }
                if (event.getTarget() instanceof LivingEntity target && weaponState.getSpiritLevel() > 0) {
                    int currentLevel = weaponState.getSpiritLevel();
                    weaponState.setLongSwordHelmBreakerSpiritLevel(currentLevel);
                    int next = Math.max(0, currentLevel - 1);
                    weaponState.setSpiritLevel(next);
                    if (next > 0) {
                        weaponState.setSpiritLevelTicks(resolveSpiritLevelMaxTicks(next));
                    } else {
                        weaponState.setSpiritLevelTicks(0);
                    }
                    state.setActionKey("spirit_helm_breaker");
                    state.setActionKeyTicks(14);
                        if (player instanceof ServerPlayer serverPlayer) {
                        String animId = org.example.common.data.WeaponDataResolver.resolveString(player, "animationOverrides", "spirit_helm_breaker",
                            "bettercombat:two_handed_slam");
                        float length = org.example.common.data.WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 16.666666f);
                        float upswing = org.example.common.data.WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.25f);
                        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f,
                                "spirit_helm_breaker", 14));
                        }
                    MHWeaponsMod.LOGGER.info("LS debug: Spirit Helm Breaker trigger player={} level={} target={} pos=({}, {}, {})",
                            player.getId(), weaponState.getSpiritLevel(), target.getId(),
                            String.format("%.2f", player.getX()),
                            String.format("%.2f", player.getY()),
                            String.format("%.2f", player.getZ()));
                    Vec3 lookDir = player.getLookAngle().normalize();
                    Vec3 targetDir = target.position().subtract(player.position());
                    Vec3 horizontalDir = new Vec3(targetDir.x, 0.0, targetDir.z);
                    if (horizontalDir.lengthSqr() <= 0.0001) {
                        horizontalDir = new Vec3(lookDir.x, 0.0, lookDir.z);
                    }
                    if (horizontalDir.lengthSqr() > 0.0001) {
                        horizontalDir = horizontalDir.normalize();
                    }
                    weaponState.setLongSwordHelmBreakerDir(horizontalDir);
                    // Give an immediate forward + upward kick so the dive can connect even if the player stops steering.
                    player.setDeltaMovement(horizontalDir.scale(1.05).add(0.0, 1.65, 0.0));
                    player.hurtMarked = true;
                    if (player.level() instanceof ServerLevel serverLevel) {
                        for (int i = 0; i < 8; i++) {
                            double scale = 0.6 + (i * 0.4);
                            serverLevel.sendParticles(ParticleTypes.CRIT,
                                    player.getX() + lookDir.x * scale,
                                    player.getY() + 1.1 + (i * 0.02),
                                    player.getZ() + lookDir.z * scale,
                                    2, 0.05, 0.05, 0.05, 0.01);
                        }
                    }
                    double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                    float hitDamage = (float) Math.max(1.0, base * 0.5);
                    for (int i = 0; i < 3; i++) {
                        target.hurt(player.damageSources().playerAttack(player), hitDamage);
                    }
                }
                return;
            }

            // Thrust (separate) — first strike of the thrust/rising chain (no movement, pure stab)
            if ("thrust_rising_slash".equals(currentAction) && event.getTarget() instanceof LivingEntity target) {
                Vec3 look = player.getLookAngle().normalize();
                Vec3 lookHorizontal = new Vec3(look.x, 0.0, look.z);
                if (lookHorizontal.lengthSqr() > 0.0001) {
                    lookHorizontal = lookHorizontal.normalize();
                }
                double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                float thrustDamage = (float) Math.max(1.0, base * 0.55);
                target.hurt(player.damageSources().playerAttack(player), thrustDamage);
                target.push(look.x * 0.18, 0.12, look.z * 0.18);
                weaponState.setLongSwordAltComboTicks(28); // allow a follow-up window
                state.setActionKey("rising_slash");
                state.setActionKeyTicks(10);
                if (player instanceof ServerPlayer serverPlayer) {
                    String animId = org.example.common.data.WeaponDataResolver.resolveString(player, "animationOverrides", "rising_slash",
                        "bettercombat:two_handed_slash_vertical_right");
                    float length = org.example.common.data.WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 16.666666f);
                    float upswing = org.example.common.data.WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.25f);
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, -1.0f,
                            "rising_slash", 10));
                }
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                            target.getX(), target.getY() + 0.6, target.getZ(),
                            6, 0.12, 0.12, 0.12, 0.02);
                }
                return;
            }

            // Rising Slash — second strike of the thrust/rising chain
            if ("rising_slash".equals(currentAction) && event.getTarget() instanceof LivingEntity target) {
                Vec3 look = player.getLookAngle().normalize();
                Vec3 lookHorizontal = new Vec3(look.x, 0.0, look.z);
                if (lookHorizontal.lengthSqr() > 0.0001) {
                    lookHorizontal = lookHorizontal.normalize();
                }
                double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                float risingDamage = (float) Math.max(1.0, base * 0.7);
                target.hurt(player.damageSources().playerAttack(player), risingDamage);
                target.push(lookHorizontal.x * 0.18, 0.35, lookHorizontal.z * 0.18);
                weaponState.setLongSwordAltComboTicks(28);
                // Clear action so next input can flow into Spirit Blade or Overhead chains
                state.setActionKey(null);
                state.setActionKeyTicks(0);
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                            target.getX() + lookHorizontal.x * 0.4,
                            target.getY() + 1.0,
                            target.getZ() + lookHorizontal.z * 0.4,
                            4, 0.04, 0.04, 0.04, 0.0);
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                            target.getX(), target.getY() + 0.9, target.getZ(),
                            6, 0.15, 0.15, 0.15, 0.02);
                }
                return;
            }

            // Spirit blade combo is handled exclusively by LongSwordHandler.handleChargeRelease
            // (triggered via CHARGE packets). Cancel BC default hits during spirit actions to
            // prevent double-damage and double-advancement of the combo.
            if (currentAction != null && (currentAction.startsWith("spirit_blade")
                    || "spirit_roundslash".equals(currentAction)
                    || "spirit_charge".equals(currentAction)
                    || "spinning_crimson_slash".equals(currentAction)
                    || "spinning_crimson_ready".equals(currentAction)
                    || "spirit_release_slash".equals(currentAction))) {
                event.setCanceled(true);
                return;
            }

            // Other longsword hits still build gauge
            if (event.getTarget() instanceof LivingEntity) {
                weaponState.addSpiritGauge(12.0f);
            }
            return;
        }
        if (weaponState != null && player.getMainHandItem().getItem() instanceof GeoWeaponItem weaponItem
                && "magnet_spike".equals(weaponItem.getWeaponId())) {
            if (weaponState.isChargingAttack()) {
                event.setCanceled(true);
                return;
            }
        }
        GeoWeaponItem weaponItem = player.getMainHandItem().getItem() instanceof GeoWeaponItem gi ? gi : null;
        String weaponId = weaponItem != null ? weaponItem.getWeaponId() : null;
        boolean allowMagnetOverride = "magnet_spike".equals(weaponId)
                && (state.getActionKey() == null
                    || "basic_attack".equals(state.getActionKey())
                    || "magnet_cut".equals(state.getActionKey())
                    || "magnet_impact".equals(state.getActionKey()));
        boolean allowDbOverride = "dual_blades".equals(weaponId)
                && (state.getActionKey() == null
                    || "basic_attack".equals(state.getActionKey())
                    || (state.getActionKey() != null && state.getActionKey().startsWith("db_") && state.getActionKeyTicks() <= 0));
        boolean allowHammerOverride = "hammer".equals(weaponId)
                && (state.getActionKey() == null
                    || "basic_attack".equals(state.getActionKey())
                    || (state.getActionKey() != null && state.getActionKey().startsWith("hammer_") && state.getActionKeyTicks() <= 0));
        if (state.getActionKey() != null && !allowMagnetOverride && !allowDbOverride && !allowHammerOverride) {
            return;
        }
        if (weaponState != null && weaponItem != null) {
            // Dual Blades: advance LMB combo (like MagnetSpike, server-side combo tracking)
            if ("dual_blades".equals(weaponId)) {
                // Block input while an action is still active (animation lock)
                if (state.getActionKey() != null && !"basic_attack".equals(state.getActionKey())
                        && state.getActionKeyTicks() > 0) {
                    return;
                }
                boolean inDemon = weaponState.isDemonMode();
                boolean inArch = weaponState.isArchDemon();
                int window = WeaponDataResolver.resolveInt(player, null, "comboWindowTicks", 12);
                if (inDemon) {
                    int lastTick = weaponState.getDbDemonComboTick();
                    int current = weaponState.getDbDemonComboIndex();
                    int next = (player.tickCount - lastTick) > window ? 0 : (current + 1) % 3;
                    weaponState.setDbDemonComboIndex(next);
                    weaponState.setDbDemonComboTick(player.tickCount);
                    String actionKey = switch (next) {
                        case 0 -> "db_demon_fangs";
                        case 1 -> "db_twofold_slash";
                        default -> "db_sixfold_slash";
                    };
                    int actionTicks = WeaponDataResolver.resolveInt(player, null, "comboActionTicks", 10);
                    state.setActionKey(actionKey);
                    state.setActionKeyTicks(actionTicks);
                } else if (inArch) {
                    int lastTick = weaponState.getDbComboTick();
                    int current = weaponState.getDbComboIndex();
                    int next = (player.tickCount - lastTick) > window ? 0 : (current + 1) % 3;
                    weaponState.setDbComboIndex(next);
                    weaponState.setDbComboTick(player.tickCount);
                    String actionKey = switch (next) {
                        case 0 -> "db_arch_slash_1";
                        case 1 -> "db_arch_slash_2";
                        default -> "db_arch_slash_3";
                    };
                    int actionTicks = WeaponDataResolver.resolveInt(player, null, "comboActionTicks", 10);
                    state.setActionKey(actionKey);
                    state.setActionKeyTicks(actionTicks);
                } else {
                    int lastTick = weaponState.getDbComboTick();
                    int current = weaponState.getDbComboIndex();
                    int next = (player.tickCount - lastTick) > window ? 0 : (current + 1) % 3;
                    weaponState.setDbComboIndex(next);
                    weaponState.setDbComboTick(player.tickCount);
                    String actionKey = switch (next) {
                        case 0 -> "db_double_slash";
                        case 1 -> "db_return_stroke";
                        default -> "db_circle_slash";
                    };
                    int actionTicks = WeaponDataResolver.resolveInt(player, null, "comboActionTicks", 12);
                    state.setActionKey(actionKey);
                    state.setActionKeyTicks(actionTicks);
                }
                return;
            }
            if ("switch_axe".equals(weaponId) && weaponState.isSwitchAxeSwordMode()) {
                state.setActionKey("sword_slash");
                state.setActionKeyTicks(10);
                return;
            }
            if ("tonfa".equals(weaponId) && weaponState.isTonfaShortMode()) {
                state.setActionKey("tonfa_short_attack");
                state.setActionKeyTicks(10);
                return;
            }
            if ("magnet_spike".equals(weaponId)) {
                boolean impact = weaponState.isMagnetSpikeImpactMode();
                int window = WeaponDataResolver.resolveInt(player, null, "comboWindowTicks", 24);
                int lastTick = impact ? weaponState.getMagnetImpactComboTick() : weaponState.getMagnetCutComboTick();
                int current = impact ? weaponState.getMagnetImpactComboIndex() : weaponState.getMagnetCutComboIndex();
                int delta = player.tickCount - lastTick;
                boolean timeout = lastTick <= 0 || delta < 0 || delta > window;
                int next = timeout ? 0 : current + 1;
                String[] seq = impact
                        ? new String[] {"magnet_smash_i", "magnet_smash_ii", "magnet_crush", "magnet_suplex"}
                        : new String[] {"magnet_slash_i", "magnet_slash_ii", "magnet_slash_iii", "magnet_cleave"};
                if (next >= seq.length) {
                    next = 0;
                }
                String actionKey = seq[next];
                int actionTicks = (next == seq.length - 1) ? 16 : 12;

                MHWeaponsMod.LOGGER.info("MagnetSpike LMB combo: player={} mode={} current={} lastTick={} now={} delta={} window={} timeout={} next={} action={} ticks={}",
                    player.getId(), impact ? "impact" : "cut", current, lastTick, player.tickCount, delta, window, timeout, next, actionKey, actionTicks);

                state.setActionKey(actionKey);
                state.setActionKeyTicks(actionTicks);

                if (impact) {
                    weaponState.setMagnetImpactComboIndex(next);
                    weaponState.setMagnetImpactComboTick(player.tickCount);
                } else {
                    weaponState.setMagnetCutComboIndex(next);
                    weaponState.setMagnetCutComboTick(player.tickCount);
                }
                return;
            }
            if ("accel_axe".equals(weaponId) && weaponState.getAccelDashTicks() > 0) {
                if (player.level() instanceof ServerLevel serverLevel) {
                    var dir = player.getLookAngle().normalize();
                    player.setDeltaMovement(player.getDeltaMovement().add(dir.scale(0.25)));
                    player.hurtMarked = true;
                    weaponState.addAccelFuel(-2);
                    serverLevel.sendParticles(ParticleTypes.FLAME,
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            10, 0.2, 0.2, 0.2, 0.02);
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            8, 0.2, 0.2, 0.2, 0.01);
                }
                state.setActionKey("accel_dash_attack");
                state.setActionKeyTicks(10);
                return;
            }
            if ("hunting_horn".equals(weaponId) && weaponState.getHornMelodyPlayTicks() > 0) {
                state.setActionKey("stack_attack");
                state.setActionKeyTicks(10);
                return;
            }
            // Hammer combo progression — driven by AttackEntityEvent (like DB / Magnet Spike).
            // LMB attacks go through Better Combat → AttackEntityEvent → combo tracked here.
            if ("hammer".equals(weaponId)) {
                String hammerAction = state.getActionKey();

                // Animation lock with late-input buffer so follow-up hits can advance the combo
                if (hammerAction != null && !"basic_attack".equals(hammerAction)
                        && state.getActionKeyTicks() > 0) {
                    int bufferTicks = Math.max(6,
                            WeaponDataResolver.resolveInt(player, null, "comboWindowBufferTicks", 6));
                    if (state.getActionKeyTicks() > Math.max(1, bufferTicks)) {
                        return;
                    }
                }

                // Focus Strike: Focus Mode + LMB → earthquake blow
                if (state.isFocusMode()) {
                    state.setActionKey("hammer_focus_blow_earthquake");
                    state.setActionKeyTicks(18);
                    return;
                }

                // Big Bang active — don't override, Big Bang advances only via C (SPECIAL)
                if (weaponState.getHammerBigBangStage() > 0) {
                    return;
                }

                // Charged Side Blow / Charged Upswing → follow-up chain
                if ("hammer_charged_side_blow".equals(hammerAction)
                        || "hammer_charged_upswing".equals(hammerAction)) {
                    state.setActionKey("hammer_charged_follow_up");
                    state.setActionKeyTicks(10);
                    return;
                }
                // Charged Follow-up → Overhead Smash I (restart normal combo)
                if ("hammer_charged_follow_up".equals(hammerAction)) {
                    weaponState.setHammerComboIndex(0);
                    weaponState.setHammerComboTick(player.tickCount);
                    state.setActionKey("hammer_overhead_smash_1");
                    state.setActionKeyTicks(10);
                    return;
                }

                // Offset Uppercut follow-up → Spinslam
                if ("hammer_offset_uppercut".equals(hammerAction)) {
                    state.setActionKey("hammer_follow_up_spinslam");
                    state.setActionKeyTicks(16);
                    return;
                }

                // Spinning Bludgeon: LMB during spin advances spin combo
                if (hammerAction != null && hammerAction.startsWith("hammer_spin")) {
                    String[] spinCombo = {
                        "hammer_spinning_bludgeon",
                        "hammer_spin_side_smash",
                        "hammer_spin_follow_up",
                        "hammer_spin_strong_upswing"
                    };
                    for (int i = 0; i < spinCombo.length - 1; i++) {
                        if (spinCombo[i].equals(hammerAction)) {
                            int ticks = (i + 1 == spinCombo.length - 1) ? 14 : 10;
                            state.setActionKey(spinCombo[i + 1]);
                            state.setActionKeyTicks(ticks);
                            return;
                        }
                    }
                    state.setActionKey(spinCombo[spinCombo.length - 1]);
                    state.setActionKeyTicks(14);
                    return;
                }

                // Standard combo: Overhead Smash I → II → Upswing (loop)
                int window = Math.max(40, WeaponDataResolver.resolveInt(player, null, "comboWindowTicks", 40));
                int lastTick = weaponState.getHammerComboTick();
                int current = weaponState.getHammerComboIndex();
                int delta = player.tickCount - lastTick;
                boolean timeout = lastTick <= 0 || delta < 0 || delta > window;
                String[] standardCombo = {
                    "hammer_overhead_smash_1",
                    "hammer_overhead_smash_2",
                    "hammer_upswing"
                };
                int next = timeout ? 0 : (current + 1) % standardCombo.length;
                weaponState.setHammerComboIndex(next);
                int actionTicks = (next == 2) ? 14 : 10; // Upswing slightly longer
                weaponState.setHammerComboTick(player.tickCount);
                state.setActionKey(standardCombo[next]);
                state.setActionKeyTicks(actionTicks);
                return;
            }
        }
        if (player.isShiftKeyDown()) {
            state.setActionKey("strong_charge");
        } else if (player.isSprinting()) {
            state.setActionKey("draw_slash");
        } else {
            state.setActionKey("basic_attack");
        }
        state.setActionKeyTicks(10);
    }
}
