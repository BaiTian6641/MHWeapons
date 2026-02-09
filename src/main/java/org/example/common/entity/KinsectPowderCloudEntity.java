package org.example.common.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.List;

/**
 * Kinsect Powder Cloud — left behind by the Kinsect along its flight path and at hover locations.
 * Powder types: 0=None, 1=Blast, 2=Poison, 3=Paralysis, 4=Heal
 * 
 * The player detonates clouds by attacking near them (within range).
 * Each cloud has a short lifetime and visual particle effects.
 */
@SuppressWarnings("null")
public class KinsectPowderCloudEntity extends Entity {
    private static final EntityDataAccessor<Integer> POWDER_TYPE = SynchedEntityData.defineId(KinsectPowderCloudEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> REMAINING_TICKS = SynchedEntityData.defineId(KinsectPowderCloudEntity.class, EntityDataSerializers.INT);

    private static final int DEFAULT_LIFETIME = 300; // 15 seconds
    private static final double DETONATE_RADIUS = 2.5;
    private static final double EFFECT_RADIUS = 3.5;

    private int ownerEntityId = -1;

    public KinsectPowderCloudEntity(EntityType<?> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public KinsectPowderCloudEntity(EntityType<?> type, Level level, int powderType, int ownerEntityId) {
        this(type, level);
        this.entityData.set(POWDER_TYPE, powderType);
        this.entityData.set(REMAINING_TICKS, DEFAULT_LIFETIME);
        this.ownerEntityId = ownerEntityId;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(POWDER_TYPE, 1);
        this.entityData.define(REMAINING_TICKS, DEFAULT_LIFETIME);
    }

    public int getPowderType() {
        return this.entityData.get(POWDER_TYPE);
    }

    public int getRemainingTicks() {
        return this.entityData.get(REMAINING_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnIdleParticles();
            return;
        }
        int remaining = this.entityData.get(REMAINING_TICKS) - 1;
        if (remaining <= 0) {
            discard();
            return;
        }
        this.entityData.set(REMAINING_TICKS, remaining);
    }

    /**
     * Called when a player attacks near this cloud to detonate it.
     * Applies the powder effect based on type.
     */
    public void detonate(Player attacker) {
        if (level().isClientSide || isRemoved()) return;

        int type = getPowderType();
        AABB effectBox = getBoundingBox().inflate(EFFECT_RADIUS);

        if (level() instanceof ServerLevel serverLevel) {
            // Spawn detonation particles
            ParticleOptions particle = resolveDetonationParticle(type);
            double cx = getX();
            double cy = getY() + 0.5;
            double cz = getZ();
            serverLevel.sendParticles(particle, cx, cy, cz, 20, 1.0, 0.5, 1.0, 0.05);
            serverLevel.sendParticles(ParticleTypes.POOF, cx, cy, cz, 8, 0.8, 0.4, 0.8, 0.02);

            List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, effectBox,
                    e -> e != attacker && !(e instanceof Player));

            switch (type) {
                case 1 -> { // Blast — explosion damage to enemies
                    for (LivingEntity target : targets) {
                        target.hurt(attacker.damageSources().playerAttack(attacker), 15.0f);
                    }
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION, cx, cy, cz, 3, 0.5, 0.3, 0.5, 0.0);
                }
                case 2 -> { // Poison — apply poison to enemies
                    for (LivingEntity target : targets) {
                        target.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1, false, true));
                        target.hurt(attacker.damageSources().playerAttack(attacker), 3.0f);
                    }
                }
                case 3 -> { // Paralysis — apply slowness + mining fatigue to enemies
                    for (LivingEntity target : targets) {
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2, false, true));
                        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1, false, true));
                        target.hurt(attacker.damageSources().playerAttack(attacker), 2.0f);
                    }
                }
                case 4 -> { // Heal — heal nearby allies
                    List<Player> allies = level().getEntitiesOfClass(Player.class, effectBox);
                    for (Player ally : allies) {
                        ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, true));
                        ally.heal(4.0f);
                    }
                }
            }
        }
        discard();
    }

    /**
     * Check if a player's attack position is close enough to detonate this cloud.
     */
    public boolean isInDetonationRange(Player player) {
        return player.distanceToSqr(this) <= DETONATE_RADIUS * DETONATE_RADIUS;
    }

    /**
     * Spawn ambient particles while the cloud exists (client-side).
     */
    private void spawnIdleParticles() {
        if (tickCount % 3 != 0) return;

        int type = getPowderType();
        ParticleOptions particle = resolvePowderDustParticle(type);
        double cx = getX();
        double cy = getY() + 0.3;
        double cz = getZ();

        for (int i = 0; i < 3; i++) {
            double ox = (level().random.nextDouble() - 0.5) * 1.2;
            double oy = (level().random.nextDouble() - 0.5) * 0.6;
            double oz = (level().random.nextDouble() - 0.5) * 1.2;
            level().addParticle(particle, cx + ox, cy + oy, cz + oz, 0.0, 0.01, 0.0);
        }

        // Occasional extra particles for visual interest
        if (level().random.nextFloat() < 0.3f) {
            level().addParticle(ParticleTypes.AMBIENT_ENTITY_EFFECT,
                    cx + (level().random.nextDouble() - 0.5) * 0.8,
                    cy + level().random.nextDouble() * 0.4,
                    cz + (level().random.nextDouble() - 0.5) * 0.8,
                    getPowderColorR(type), getPowderColorG(type), getPowderColorB(type));
        }
    }

    /**
     * Resolve colored dust particle for the powder type.
     */
    public static ParticleOptions resolvePowderDustParticle(int powderType) {
        return switch (powderType) {
            case 1 -> new DustParticleOptions(new Vector3f(1.0f, 0.4f, 0.1f), 1.2f);  // Blast — orange-red
            case 2 -> new DustParticleOptions(new Vector3f(0.5f, 0.1f, 0.8f), 1.0f);  // Poison — purple
            case 3 -> new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.2f), 1.0f);  // Paralysis — yellow
            case 4 -> new DustParticleOptions(new Vector3f(0.2f, 1.0f, 0.4f), 1.0f);  // Heal — green
            default -> new DustParticleOptions(new Vector3f(0.7f, 0.7f, 0.7f), 0.8f); // None — gray
        };
    }

    private static ParticleOptions resolveDetonationParticle(int powderType) {
        return switch (powderType) {
            case 1 -> new DustParticleOptions(new Vector3f(1.0f, 0.3f, 0.0f), 2.0f);  // Blast — big orange
            case 2 -> new DustParticleOptions(new Vector3f(0.6f, 0.0f, 0.9f), 1.8f);  // Poison — big purple
            case 3 -> new DustParticleOptions(new Vector3f(1.0f, 1.0f, 0.0f), 1.8f);  // Paralysis — big yellow
            case 4 -> new DustParticleOptions(new Vector3f(0.0f, 1.0f, 0.3f), 1.8f);  // Heal — big green
            default -> new DustParticleOptions(new Vector3f(0.8f, 0.8f, 0.8f), 1.5f);
        };
    }

    private static double getPowderColorR(int type) {
        return switch (type) { case 1 -> 1.0; case 2 -> 0.5; case 3 -> 1.0; case 4 -> 0.2; default -> 0.7; };
    }

    private static double getPowderColorG(int type) {
        return switch (type) { case 1 -> 0.4; case 2 -> 0.1; case 3 -> 0.9; case 4 -> 1.0; default -> 0.7; };
    }

    private static double getPowderColorB(int type) {
        return switch (type) { case 1 -> 0.1; case 2 -> 0.8; case 3 -> 0.2; case 4 -> 0.4; default -> 0.7; };
    }

    /**
     * Get display name for powder type.
     */
    public static String getPowderName(int type) {
        return switch (type) {
            case 1 -> "Blast";
            case 2 -> "Poison";
            case 3 -> "Paralysis";
            case 4 -> "Heal";
            default -> "None";
        };
    }

    /**
     * Get HUD color for powder type.
     */
    public static int getPowderHudColor(int type) {
        return switch (type) {
            case 1 -> 0xFFFF6600; // Orange
            case 2 -> 0xFF9900CC; // Purple
            case 3 -> 0xFFFFCC00; // Yellow
            case 4 -> 0xFF33FF66; // Green
            default -> 0xFFAAAAAA; // Gray
        };
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(POWDER_TYPE, tag.getInt("powderType"));
        this.entityData.set(REMAINING_TICKS, tag.getInt("remainingTicks"));
        this.ownerEntityId = tag.getInt("ownerEntityId");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("powderType", this.entityData.get(POWDER_TYPE));
        tag.putInt("remainingTicks", this.entityData.get(REMAINING_TICKS));
        tag.putInt("ownerEntityId", ownerEntityId);
    }
}
