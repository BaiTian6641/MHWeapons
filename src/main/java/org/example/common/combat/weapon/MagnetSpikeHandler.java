package org.example.common.combat.weapon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.example.MHWeaponsMod;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.combat.StaminaHelper;
import org.example.common.compat.BetterCombatAnimationBridge;
import org.example.common.data.WeaponDataResolver;
import org.example.common.network.ModNetwork;
import org.example.common.network.packet.MagnetSpikeZipAnimationS2CPacket;
import org.example.common.network.packet.PlayAttackAnimationS2CPacket;
import org.example.common.network.packet.PlayerWeaponStateS2CPacket;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

public final class MagnetSpikeHandler {
    private MagnetSpikeHandler() {}

    private static void setAction(PlayerCombatState combatState, String key, int ticks) {
        combatState.setActionKey(key);
        combatState.setActionKeyTicks(ticks);
    }

    private static void spendStamina(Player player, PlayerWeaponState state, float baseCost, int recoveryDelayTicks) {
        if (state == null) return;
        float cost = StaminaHelper.applyCost(player, baseCost);
        state.addStamina(-cost);
        state.setStaminaRecoveryDelay(recoveryDelayTicks);
    }

    @Nullable
    private static LivingEntity resolveEntityById(Player player, int entityId) {
        if (player.level() instanceof ServerLevel serverLevel) {
            var entity = serverLevel.getEntity(entityId);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    @Nullable
    private static LivingEntity findTargetInFront(Player player, double range) {
        Vec3 look = player.getLookAngle();
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(look.scale(range));
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        
        return player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())
                .stream()
                .filter(e -> e.getBoundingBox().intersects(start, end))
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);
    }

    public static void handleAction(WeaponActionType action, boolean pressed, Player player, PlayerCombatState combatState, PlayerWeaponState weaponState) {
        if (!pressed) return;

        if (player != null && weaponState != null) {
            MHWeaponsMod.LOGGER.info("MagnetSpike action: player={} action={} shift={} impactMode={} targetTicks={} gauge={} actionKey={} actionTicks={}",
                    player.getId(), action, player.isShiftKeyDown(), weaponState.isMagnetSpikeImpactMode(),
                    weaponState.getMagnetTargetTicks(), weaponState.getMagnetGauge(),
                    combatState != null ? combatState.getActionKey() : null,
                    combatState != null ? combatState.getActionKeyTicks() : -1);
        }
        
        // SPECIAL: Mode Switch or Magnet Burst
        if (action == WeaponActionType.SPECIAL) {
            handleSpecialAction(player, combatState, weaponState);
            return;
        }

        // WEAPON (R): Magnet Tag
        if (action == WeaponActionType.WEAPON) {
            handleMagnetTag(player, combatState, weaponState);
            return;
        }

        // WEAPON_ALT: Magnetic Approach (zip toward target)
        if (action == WeaponActionType.WEAPON_ALT) {
            handleMagnetZip(player, combatState, weaponState, false);
            return;
        }

        // WEAPON_ALT_REPEL: Magnetic Repel (zip away)
        if (action == WeaponActionType.WEAPON_ALT_REPEL) {
            handleMagnetZip(player, combatState, weaponState, true);
            return;
        }
    }

    public static void handleChargeRelease(Player player, PlayerCombatState combatState, PlayerWeaponState weaponState, int chargeTicks, int maxCharge) {
        if (player == null || combatState == null || weaponState == null) return;

        MHWeaponsMod.LOGGER.info("MagnetSpike charge release: player={} impactMode={} chargeTicks={} maxCharge={} actionKey={} actionTicks={}",
            player.getId(), weaponState.isMagnetSpikeImpactMode(), chargeTicks, maxCharge,
            combatState.getActionKey(), combatState.getActionKeyTicks());

        // If in Impact Mode, perform Heavy Slam
        if (weaponState.isMagnetSpikeImpactMode()) {
            int level = 0;
            if (chargeTicks >= maxCharge) level = 3;
            else if (chargeTicks >= (maxCharge * 2 / 3)) level = 2;
            else if (chargeTicks >= (maxCharge / 3)) level = 1;

            if (level > 0) {
                // Modernized naming: impact_charge_i, ii, iii
                String action = "impact_charge_" + (level == 1 ? "i" : level == 2 ? "ii" : "iii");
                float motionValue = WeaponDataResolver.resolveMotionValue(player, action, 1.0f + (level * 0.35f));
                int animTicks = WeaponDataResolver.resolveInt(player, "magnetCharge", "animTicks", 20);
                setAction(combatState, action, animTicks);

                applyImpactSlam(player, motionValue, level >= 3);

                if (player instanceof ServerPlayer serverPlayer) {
                    String animId = WeaponDataResolver.resolveString(player, "animationOverrides", action,
                            "bettercombat:two_handed_slam");
                    float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", animTicks);
                    float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.35f);
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f, action, animTicks));
                }
            } else {
                // Zero charge tap in impact mode - Regular bash hit
                String action = "impact_bash";
                setAction(combatState, action, 10);
                applyImpactSlam(player, WeaponDataResolver.resolveMotionValue(player, action, 0.9f), false);
            }
        } else {
            // Cut Mode Charge - "Magnet Pile" prep or strong slash
            String action = "cut_heavy_slash";
            int animTicks = WeaponDataResolver.resolveInt(player, "magnetCharge", "animTicksCut", 16);
            setAction(combatState, action, animTicks);
            applyCutHeavySlash(player, action);

            if (player instanceof ServerPlayer serverPlayer) {
                String animId = WeaponDataResolver.resolveString(player, "animationOverrides", action,
                        "bettercombat:two_handed_slash_horizontal_right");
                float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", animTicks);
                float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.25f);
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new PlayAttackAnimationS2CPacket(player.getId(), animId, length, upswing, 1.0f, action, animTicks));
            }
        }

        // Reset combo state so follow-up chains restart after a charge finisher
        weaponState.setMagnetCutComboIndex(0);
        weaponState.setMagnetImpactComboIndex(0);
        weaponState.setMagnetCutComboTick(player.tickCount);
        weaponState.setMagnetImpactComboTick(player.tickCount);
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

    private static void handleSpecialAction(Player player, PlayerCombatState combatState, PlayerWeaponState weaponState) {
        // Shift + Special = Magnet Burst / Pile Bunker (Finisher)
        if (player.isShiftKeyDown()) {
            // Check for Pile Bunker conditions (Wilds Ultimate)
            // 1. Magnetized Target
            // 2. High Gauge
            // 3. Close Range
            int gauge = (int) weaponState.getMagnetGauge();
            int targetId = weaponState.getMagnetTargetId();
            LivingEntity target = resolveEntityById(player, targetId);
            
            boolean pileBunkerReady = target != null 
                    && gauge >= 80 
                    && target.distanceToSqr(player) < 16.0 // 4 blocks
                    && weaponState.getMagnetTargetTicks() > 0;

            int cost = WeaponDataResolver.resolveInt(player, "magnetBurst", "cost", 50);

            if (pileBunkerReady) {
                // Execute Pile Bunker (Pin + Drill)
                executePileBunker(player, combatState, weaponState, target);
            } else if (gauge >= cost) {
                // Standard Magnet Burst
                executeMagnetBurst(player, combatState, weaponState, cost);
            }
            return;
        }

        // Regular Special = Transform Mode
        weaponState.setMagnetSpikeImpactMode(!weaponState.isMagnetSpikeImpactMode());
        setAction(combatState, weaponState.isMagnetSpikeImpactMode() ? "magnet_impact" : "magnet_cut", 10);
        BetterCombatAnimationBridge.updateMagnetSpikeAttackAnimation(player, weaponState.isMagnetSpikeImpactMode());
        syncWeaponState(player, weaponState);
    }

    private static void executePileBunker(Player player, PlayerCombatState combatState, PlayerWeaponState weaponState, LivingEntity target) {
        if (target == null) return;
        
        // Consume State
        weaponState.addMagnetGauge(-80);
        
        // State 1: Pin (Move to target)
        weaponState.setMagnetTargetId(-1);
        weaponState.setMagnetTargetTicks(0);
        
        // Teleport/Zip logic for Pin
        Vec3 targetPos = target.position().add(target.getLookAngle().scale(0.5)); // slightly in front
        Vec3 dir = targetPos.subtract(player.position()).normalize();
        player.setDeltaMovement(dir.scale(1.2));
        player.hurtMarked = true;
        
        setAction(combatState, "magnet_pile_bunker_drill", 40);

        if (player.level() instanceof ServerLevel serverLevel) {
            // Initial Hit (Pin)
            target.hurt(player.damageSources().playerAttack(player), 10.0f);
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 10)); // pseudo-pin
            
            // Continuous Drilling Effect (Logic handles damage over time elsewhere or we schedule it)
            // For simplicity, we deal chunk damage here and improved FX
            serverLevel.sendParticles(ParticleTypes.CRIT, 
                    target.getX(), target.getEyeY(), target.getZ(), 
                    20, 0.5, 0.5, 0.5, 0.1);
            
            // Final Explosion scheduled for later ticks? 
            // BetterCombat handles "upswing/downswing", but Pile Bunker is complex.
            // We'll apply the big burst immediately for now to ensure feedback.
            target.hurt(player.damageSources().playerAttack(player), 50.0f); // Massive Fixed Damage
            
            // Explosion FX
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, 
                   target.getX(), target.getEyeY(), target.getZ(), 
                   1, 0, 0, 0, 0);
        }
        syncWeaponState(player, weaponState);
    }

    private static void executeMagnetBurst(Player player, PlayerCombatState combatState, PlayerWeaponState weaponState, int cost) {
        LivingEntity targetEntity = resolveEntityById(player, weaponState.getMagnetTargetId());
        if (targetEntity == null) {
            float tagRange = WeaponDataResolver.resolveFloat(player, "magnetZip", "targetRange", 6.0f);
            targetEntity = findTargetInFront(player, tagRange);
        }
        final LivingEntity target = targetEntity;
        
        weaponState.addMagnetGauge(-cost);
        weaponState.setMagnetTargetId(-1);
        weaponState.setMagnetTargetTicks(0);
        setAction(combatState, "magnet_burst", 14);
        
        if (player.level() instanceof ServerLevel serverLevel) {
            boolean impact = weaponState.isMagnetSpikeImpactMode();
            float baseDamage = WeaponDataResolver.resolveFloat(player, "magnetBurst", "baseDamage", impact ? 8.0f : 6.5f);
            float gaugeBonus = 1.0f + 0.3f * (weaponState.getMagnetGauge() / 100.0f);
            float damage = baseDamage * gaugeBonus;
            float radius = WeaponDataResolver.resolveFloat(player, "magnetBurst", "radius", impact ? 3.2f : 2.6f);
            
            if (target != null) {
                Vec3 pull = player.position().subtract(target.position()).normalize();
                if (impact) {
                    target.push(-pull.x * 0.4, 0.2, -pull.z * 0.4); // Knockback
                 } else {
                    target.push(pull.x * 0.25, 0.1, pull.z * 0.25); // Pull in
                }
                target.hurt(player.damageSources().playerAttack(player), damage);
                
                // AoE
                AABB burst = target.getBoundingBox().inflate(radius);
                for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, burst, e -> e != player && e != target)) {
                    entity.hurt(player.damageSources().playerAttack(player), damage * 0.6f);
                }
                
                serverLevel.sendParticles(impact ? ParticleTypes.EXPLOSION : ParticleTypes.ELECTRIC_SPARK,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        impact ? 12 : 18, 0.4, 0.4, 0.4, 0.05);
            } else {
                // Whiff bust
                Vec3 center = player.position().add(0, player.getBbHeight() * 0.6, 0);
                AABB burst = new AABB(center, center).inflate(radius);
                for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, burst, e -> e != player)) {
                    entity.hurt(player.damageSources().playerAttack(player), damage * 0.6f);
                }
                serverLevel.sendParticles(impact ? ParticleTypes.EXPLOSION : ParticleTypes.ELECTRIC_SPARK,
                        center.x, center.y, center.z,
                        impact ? 10 : 14, 0.4, 0.4, 0.4, 0.05);
            }
        }
        syncWeaponState(player, weaponState);
    }

    private static void handleMagnetTag(Player player, PlayerCombatState combatState, PlayerWeaponState weaponState) {
        float tagRange = WeaponDataResolver.resolveFloat(player, "magnetZip", "targetRange", 12.0f); // Wilds: increased range
        LivingEntity target = findTargetInFront(player, tagRange);
        if (target != null) {
            weaponState.setMagnetTargetId(target.getId());
            weaponState.setMagnetTargetTicks(900); // 45 seconds (Wilds)
            setAction(combatState, "magnet_tag", 10);
            
            if (player.level() instanceof ServerLevel serverLevel) {
                // Visuals: Magnetic Shell Impact
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        20, 0.35, 0.35, 0.35, 0.02);
                serverLevel.sendParticles(ParticleTypes.SCULK_SOUL,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        5, 0.2, 0.2, 0.2, 0.05);
            }
        }
    }

    private static void handleMagnetZip(Player player, PlayerCombatState combatState, PlayerWeaponState weaponState, boolean repel) {
        if (weaponState.getMagnetTargetTicks() <= 0) return;
        if (weaponState.getMagnetZipCooldownTicks() > 0) return;
        
        LivingEntity target = resolveEntityById(player, weaponState.getMagnetTargetId());
        if (target != null) {
            float staminaCost = StaminaHelper.applyCost(player, 16.0f);
            if (weaponState.getStamina() < staminaCost) return;
            
            float zipAttractSpeed = WeaponDataResolver.resolveFloat(player, "magnetZip", "attractSpeed", 1.6f);
            float zipRepelSpeed = WeaponDataResolver.resolveFloat(player, "magnetZip", "repelSpeed", 1.2f);
            int zipAnimTicks = WeaponDataResolver.resolveInt(player, "magnetZip", "animTicks", 22);
            int zipCooldownTicks = WeaponDataResolver.resolveInt(player, "magnetZip", "cooldownTicks", 20);
            
            // Precision Update: Aim for center mass
            Vec3 targetCenter = target.getBoundingBox().getCenter();
            Vec3 playerCenter = player.getBoundingBox().getCenter();
            Vec3 dir = targetCenter.subtract(playerCenter).normalize();
            
            spendStamina(player, weaponState, 16.0f, 24);
            
            if (repel) {
                player.setDeltaMovement(dir.scale(-zipRepelSpeed));
            } else {
                // Attract: Move towards target with precision
                double dist = targetCenter.distanceTo(playerCenter);
                float speed = zipAttractSpeed;
                // Snap faster at close range to ensure hit
                if (dist < 5.0) speed *= 1.25f; 
                
                player.setDeltaMovement(dir.scale(speed));
                
                // turn to face target for follow-up accuracy
                player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, targetCenter);
                player.hurtMarked = true;
            }
            player.hurtMarked = true;
            
            if (combatState != null) {
                // High I-Frames for Zip
                int iFrames = repel ? 12 : 8;
                
                // Wilds Integration: Magnetic Reflex (Just Evade/Clash)
                // If the target is actively targeting the player during a repel, trigger Reflex.
                if (repel && target instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == player) {
                     iFrames = 25; // Extended invulnerability
                     weaponState.addMagnetGauge(20.0f); // Instant refill bonus
                     
                     if (player.level() instanceof ServerLevel sl) {
                          sl.sendParticles(ParticleTypes.ENCHANTED_HIT, 
                                  player.getX(), player.getY() + 1.0, player.getZ(), 
                                  15, 0.5, 0.5, 0.5, 0.1);
                     }
                }
                combatState.setDodgeIFrameTicks(iFrames); 
            }
            
            weaponState.setMagnetZipAnimTicks(zipAnimTicks);
            weaponState.setMagnetZipCooldownTicks(zipCooldownTicks);
            BetterCombatAnimationBridge.updateMagnetSpikeZipAnimation(player, true);
            syncWeaponState(player, weaponState);
            
            if (player instanceof ServerPlayer serverPlayer) {
                ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer),
                        new MagnetSpikeZipAnimationS2CPacket(serverPlayer.getId()));
            }
            
            // Visuals
            if (player.level() instanceof ServerLevel serverLevel) {
                 Vec3 start = player.position().add(0, player.getBbHeight() * 0.6, 0);
                 serverLevel.sendParticles(repel ? ParticleTypes.FLAME : ParticleTypes.ELECTRIC_SPARK,
                         start.x, start.y, start.z,
                         14, 0.2, 0.2, 0.2, 0.05);
                 // Trail
                 Vec3 path = dir.scale(repel ? -1.4 : 1.4);
                 for (int i = 0; i <= 6; i++) {
                     double t = i / 6.0;
                     Vec3 p = start.add(path.scale(t));
                     serverLevel.sendParticles(repel ? ParticleTypes.FLAME : ParticleTypes.ELECTRIC_SPARK,
                             p.x, p.y, p.z,
                             2, 0.05, 0.05, 0.05, 0.02);
                 }
            }
            weaponState.clearMagnetZipHits();
            setAction(combatState, repel ? "magnet_repel" : "magnet_approach", 10);
        }
    }

    @SuppressWarnings("null")
    private static void applyImpactSlam(Player player, float damageMultiplier, boolean isFullCharge) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        
        double radius = isFullCharge ? 3.0 : 2.0;
        Vec3 center = player.position().add(player.getLookAngle().scale(1.5));
        AABB box = new AABB(center, center).inflate(radius);
        
        double base = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        float damage = (float) (Math.max(1.0, base) * damageMultiplier);

        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)) {
            entity.hurt(player.damageSources().playerAttack(player), damage);
            // Stun effect (Impact)
            if (isFullCharge) {
                entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
            }
        }
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 5, 1.0, 0.5, 1.0, 0.0);
    }

    private static void applyCutHeavySlash(Player player, String actionKey) {
        LivingEntity target = findTargetInFront(player, 3.8);
        float motionValue = WeaponDataResolver.resolveMotionValue(player, actionKey, 1.3f);
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            if (target != null) {
                target.hurt(player.damageSources().playerAttack(player), motionValue *  (float)player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
            }
            return;
        }

        Vec3 center = player.getEyePosition().add(player.getLookAngle().scale(2.0));
        AABB box = new AABB(center, center).inflate(1.6);
        double base = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        float damage = (float) Math.max(1.0, base * motionValue);

        boolean hit = false;
        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)) {
            entity.hurt(player.damageSources().playerAttack(player), damage);
            entity.push(player.getLookAngle().x * 0.25, 0.2, player.getLookAngle().z * 0.25);
            hit = true;
        }
        if (target != null && !hit) {
            target.hurt(player.damageSources().playerAttack(player), damage);
            target.push(player.getLookAngle().x * 0.25, 0.2, player.getLookAngle().z * 0.25);
            hit = true;
        }
        if (hit) {
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    center.x, center.y, center.z,
                    10, 0.3, 0.2, 0.3, 0.01);
        }
    }
}
