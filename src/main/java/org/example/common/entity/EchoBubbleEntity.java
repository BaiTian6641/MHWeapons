package org.example.common.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.DustParticleOptions;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.util.CapabilityUtil;
import org.joml.Vector3f;

@SuppressWarnings({"null", "deprecation"})
public class EchoBubbleEntity extends Entity {
    private static final EntityDataAccessor<Integer> EFFECT_ID = SynchedEntityData.defineId(EchoBubbleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AMPLIFIER = SynchedEntityData.defineId(EchoBubbleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(EchoBubbleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(EchoBubbleEntity.class, EntityDataSerializers.FLOAT);

    public EchoBubbleEntity(EntityType<?> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public EchoBubbleEntity(EntityType<?> type, Level level, int effectId, int amplifier, int duration, float radius) {
        this(type, level);
        this.entityData.set(EFFECT_ID, effectId);
        this.entityData.set(AMPLIFIER, amplifier);
        this.entityData.set(DURATION, duration);
        this.entityData.set(RADIUS, radius);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(EFFECT_ID, BuiltInRegistries.MOB_EFFECT.getId(MobEffects.MOVEMENT_SPEED));
        this.entityData.define(AMPLIFIER, 0);
        this.entityData.define(DURATION, 200);
        this.entityData.define(RADIUS, 3.5f);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnSplashParticles();
            return;
        }
        int remaining = this.entityData.get(DURATION) - 1;
        if (remaining <= 0) {
            discard();
            return;
        }
        this.entityData.set(DURATION, remaining);

        if (remaining % 20 == 0) {
            int effectId = this.entityData.get(EFFECT_ID);
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.byId(effectId);
            if (effect == null) {
                effect = MobEffects.MOVEMENT_SPEED;
            }
            int amp = this.entityData.get(AMPLIFIER);
            float radius = this.entityData.get(RADIUS);
            AABB box = getBoundingBox().inflate(radius, 1.0, radius);
            for (Player player : level().getEntitiesOfClass(Player.class, box)) {
                if (effect == MobEffects.POISON) {
                    player.removeEffect(MobEffects.POISON);
                    continue;
                }
                if (effect == MobEffects.REGENERATION && amp >= 1) {
                    player.removeEffect(MobEffects.POISON);
                }
                if (effect == MobEffects.MOVEMENT_SPEED) {
                    PlayerWeaponState state = CapabilityUtil.getPlayerWeaponState(player);
                    if (state != null) {
                        state.setHornStaminaBoostTicks(60);
                    }
                }
                if (effect == MobEffects.DAMAGE_BOOST) {
                    PlayerWeaponState state = CapabilityUtil.getPlayerWeaponState(player);
                    if (state != null) {
                        state.setHornAffinityTicks(60);
                    }
                }
                if (effect == MobEffects.ABSORPTION) {
                    player.removeEffect(MobEffects.POISON);
                    player.removeEffect(MobEffects.WITHER);
                    player.removeEffect(MobEffects.WEAKNESS);
                    player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    player.removeEffect(MobEffects.DIG_SLOWDOWN);
                    player.removeEffect(MobEffects.BLINDNESS);
                    player.removeEffect(MobEffects.CONFUSION);
                    player.removeEffect(MobEffects.HUNGER);
                    continue;
                }
                player.addEffect(new MobEffectInstance(effect, 60, amp, false, true));
            }
        }
    }

    private void spawnSplashParticles() {
        float radius = getRadius();
        if (tickCount % 3 != 0) {
            return;
        }
        ParticleOptions tint = resolveTintParticle();
        for (int i = 0; i < 4; i++) {
            double angle = (level().random.nextDouble() * Math.PI * 2.0);
            double r = radius * (0.4 + level().random.nextDouble() * 0.6);
            double x = getX() + Math.cos(angle) * r;
            double z = getZ() + Math.sin(angle) * r;
            double y = getY() + 0.1 + level().random.nextDouble() * 0.2;
            double vx = (level().random.nextDouble() - 0.5) * 0.05;
            double vz = (level().random.nextDouble() - 0.5) * 0.05;
            level().addParticle(ParticleTypes.SPLASH, x, y, z, vx, 0.08, vz);
            if (level().random.nextFloat() < 0.35f) {
                level().addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0.0, 0.02, 0.0);
            }
            if (level().random.nextFloat() < 0.25f) {
                level().addParticle(ParticleTypes.BUBBLE, x, y, z, 0.0, 0.03, 0.0);
            }
            level().addParticle(tint, x, y + 0.15, z, 0.0, 0.02, 0.0);
        }
    }

    private ParticleOptions resolveTintParticle() {
        int effectId = this.entityData.get(EFFECT_ID);
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.byId(effectId);
        if (effect == MobEffects.DAMAGE_BOOST) {
            return new DustParticleOptions(new Vector3f(0.9f, 0.2f, 0.2f), 1.0f);
        }
        if (effect == MobEffects.MOVEMENT_SPEED) {
            return new DustParticleOptions(new Vector3f(0.2f, 0.6f, 1.0f), 1.0f);
        }
        if (effect == MobEffects.DAMAGE_RESISTANCE) {
            return new DustParticleOptions(new Vector3f(0.5f, 0.9f, 0.5f), 1.0f);
        }
        if (effect == MobEffects.ABSORPTION) {
            return new DustParticleOptions(new Vector3f(0.95f, 0.85f, 0.2f), 1.0f);
        }
        if (effect == MobEffects.POISON) {
            return new DustParticleOptions(new Vector3f(0.3f, 0.9f, 0.3f), 1.0f);
        }
        return new DustParticleOptions(new Vector3f(0.7f, 0.7f, 0.9f), 1.0f);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        this.entityData.set(EFFECT_ID, tag.getInt("effectId"));
        this.entityData.set(AMPLIFIER, tag.getInt("amplifier"));
        this.entityData.set(DURATION, tag.getInt("duration"));
        this.entityData.set(RADIUS, tag.getFloat("radius"));
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        tag.putInt("effectId", this.entityData.get(EFFECT_ID));
        tag.putInt("amplifier", this.entityData.get(AMPLIFIER));
        tag.putInt("duration", this.entityData.get(DURATION));
        tag.putFloat("radius", this.entityData.get(RADIUS));
    }
}
