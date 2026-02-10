package org.example.common.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.example.registry.MHWeaponsItems;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

/**
 * Unified projectile entity for all Bowgun ammo types.
 * Supports: normal, pierce, spread, element, status, sticky, cluster, slicing,
 * and special ammo (wyvernheart, wyvernpiercer, wyverncounter, wyvernblast, focus blasts).
 */
public class AmmoProjectileEntity extends Projectile {
    private static final Logger LOG = LogUtils.getLogger();

    private static final EntityDataAccessor<String> AMMO_TYPE =
            SynchedEntityData.defineId(AmmoProjectileEntity.class, EntityDataSerializers.STRING);

    // Damage & physics
    private float damage = 5.0f;
    private float elementDamage = 0.0f;
    private float statusValue = 0.0f;
    private float speed = 2.0f;
    private float gravityRate = 0.05f;
    private int pierceCount = 0;       // 0 = no pierce, N = can hit N+1 targets
    private int piercesRemaining = 0;

    // Tracking
    private final Set<Integer> hitEntityIds = new HashSet<>();
    private int ticksAlive = 0;
    private static final int MAX_LIFETIME = 200; // 10 seconds

    // Special type (null for normal ammo)
    private String specialType = null;

    // Sticky/timed explosion
    private boolean sticky = false;
    private int stickyTimer = -1;
    private int attachedEntityId = -1;

    // Cluster
    private boolean cluster = false;
    private int clusterBomblets = 0;

    public AmmoProjectileEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        setNoGravity(false);
    }

    public AmmoProjectileEntity(Level level, LivingEntity owner) {
        super(MHWeaponsItems.AMMO_PROJECTILE.get(), level);
        setOwner(owner);
        setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(AMMO_TYPE, "normal_1");
    }

    /**
     * Configure projectile parameters after construction.
     */
    public void configure(String ammoType, float damage, float elementDamage,
                           float statusValue, float speed, float gravity,
                           int pelletCount, int pierceCount) {
        this.entityData.set(AMMO_TYPE, ammoType);
        this.damage = damage;
        this.elementDamage = elementDamage;
        this.statusValue = statusValue;
        this.speed = speed;
        this.gravityRate = gravity;
        this.pierceCount = pierceCount;
        this.piercesRemaining = pierceCount;

        // Auto-detect sticky/cluster from ammo type
        if (ammoType.startsWith("sticky_")) {
            this.sticky = true;
            this.stickyTimer = 60; // 3 seconds
        } else if (ammoType.startsWith("cluster_")) {
            this.cluster = true;
            this.clusterBomblets = switch (ammoType) {
                case "cluster_1" -> 3;
                case "cluster_2" -> 4;
                case "cluster_3" -> 5;
                default -> 3;
            };
        } else if ("slicing_ammo".equals(ammoType)) {
            this.sticky = true;
            this.stickyTimer = 20;
        }

        LOG.debug("[AmmoProjectile] Configured: type={} dmg={} elem={} status={} speed={} pierce={}",
                ammoType, damage, elementDamage, statusValue, speed, pierceCount);
    }

    public void setSpecialType(String type) {
        this.specialType = type;
        this.entityData.set(AMMO_TYPE, type != null ? type : "normal_1");
    }

    public String getAmmoType() {
        return this.entityData.get(AMMO_TYPE);
    }

    @Override
    public void tick() {
        super.tick();
        ticksAlive++;

        if (ticksAlive > MAX_LIFETIME) {
            discard();
            return;
        }

        // Sticky countdown
        if (sticky && stickyTimer > 0 && attachedEntityId >= 0) {
            stickyTimer--;
            // Follow attached entity
            Entity attached = level().getEntity(attachedEntityId);
            if (attached != null) {
                setPos(attached.getX(), attached.getY() + attached.getBbHeight() / 2, attached.getZ());
            }
            if (stickyTimer <= 0) {
                explode();
                discard();
                return;
            }
        }

        // Gravity
        if (!isNoGravity()) {
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x, motion.y - gravityRate, motion.z);
        }

        // Hit detection
        Vec3 pos = position();
        Vec3 nextPos = pos.add(getDeltaMovement());
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);

        if (hit.getType() != HitResult.Type.MISS) {
            onHit(hit);
        }

        // Move
        setPos(nextPos.x, nextPos.y, nextPos.z);

        // Particles based on ammo type
        if (level() instanceof ServerLevel serverLevel && ticksAlive % 2 == 0) {
            String type = getAmmoType();
            if (type.startsWith("fire_ammo") || type.startsWith("flaming_ammo")) {
                serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 2, 0.02, 0.02, 0.02, 0.01);
                if (type.contains("flaming")) {
                    serverLevel.sendParticles(ParticleTypes.LAVA, getX(), getY(), getZ(), 1, 0.01, 0.01, 0.01, 0.0);
                }
                serverLevel.sendParticles(colorDust(1.0f, 0.45f, 0.1f, 1.2f), getX(), getY(), getZ(), 2, 0.03, 0.03, 0.03, 0.01);
            } else if (type.startsWith("water_ammo")) {
                serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER, getX(), getY(), getZ(), 2, 0.02, 0.02, 0.02, 0.0);
                serverLevel.sendParticles(ParticleTypes.FALLING_WATER, getX(), getY(), getZ(), 1, 0.05, 0.05, 0.05, 0.0);
                serverLevel.sendParticles(colorDust(0.1f, 0.45f, 1.0f, 1.1f), getX(), getY(), getZ(), 2, 0.03, 0.03, 0.03, 0.01);
            } else if (type.startsWith("thunder_ammo")) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 3, 0.05, 0.05, 0.05, 0.02);
                serverLevel.sendParticles(colorDust(0.95f, 0.9f, 0.2f, 1.3f), getX(), getY(), getZ(), 3, 0.03, 0.03, 0.03, 0.01);
            } else if (type.startsWith("ice_ammo")) {
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 2, 0.02, 0.02, 0.02, 0.0);
                serverLevel.sendParticles(colorDust(0.55f, 0.9f, 1.0f, 1.0f), getX(), getY(), getZ(), 2, 0.03, 0.03, 0.03, 0.01);
            } else if (type.startsWith("dragon_ammo")) {
                serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, getX(), getY(), getZ(), 2, 0.02, 0.02, 0.02, 0.0);
                serverLevel.sendParticles(colorDust(0.6f, 0.1f, 0.9f, 1.25f), getX(), getY(), getZ(), 2, 0.03, 0.03, 0.03, 0.01);
            } else if (type.startsWith("poison_ammo")) {
                serverLevel.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY(), getZ(), 2, 0.02, 0.02, 0.02, 0.0);
                serverLevel.sendParticles(colorDust(0.2f, 0.9f, 0.3f, 1.0f), getX(), getY(), getZ(), 1, 0.02, 0.02, 0.02, 0.01);
            } else if (type.startsWith("paralysis_ammo")) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 2, 0.02, 0.02, 0.02, 0.01);
                serverLevel.sendParticles(colorDust(1.0f, 0.85f, 0.25f, 1.1f), getX(), getY(), getZ(), 1, 0.02, 0.02, 0.02, 0.01);
            } else if (type.startsWith("sleep_ammo")) {
                serverLevel.sendParticles(ParticleTypes.ENCHANT, getX(), getY(), getZ(), 2, 0.02, 0.02, 0.02, 0.0);
                serverLevel.sendParticles(colorDust(0.55f, 0.2f, 0.9f, 1.0f), getX(), getY(), getZ(), 1, 0.02, 0.02, 0.02, 0.01);
            } else if (type.startsWith("tranq_ammo")) {
                serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY(), getZ(), 2, 0.03, 0.03, 0.03, 0.0);
                serverLevel.sendParticles(colorDust(0.6f, 0.8f, 0.6f, 0.9f), getX(), getY(), getZ(), 1, 0.02, 0.02, 0.02, 0.01);
            } else if (type.startsWith("demon_ammo")) {
                serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY(), getZ(), 2, 0.03, 0.03, 0.03, 0.0);
                serverLevel.sendParticles(colorDust(0.95f, 0.2f, 0.2f, 1.1f), getX(), getY(), getZ(), 1, 0.02, 0.02, 0.02, 0.01);
            } else if (type.startsWith("armor_ammo")) {
                serverLevel.sendParticles(ParticleTypes.COMPOSTER, getX(), getY(), getZ(), 2, 0.03, 0.03, 0.03, 0.0);
                serverLevel.sendParticles(colorDust(0.2f, 0.6f, 0.9f, 1.1f), getX(), getY(), getZ(), 1, 0.02, 0.02, 0.02, 0.01);
            } else if (type.startsWith("sticky_")) {
                serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 2, 0.02, 0.02, 0.02, 0.0);
            } else if (type.startsWith("cluster_")) {
                serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 2, 0.02, 0.02, 0.02, 0.0);
                serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 1, 0.02, 0.02, 0.02, 0.0);
            } else if (type.startsWith("pierce_")) {
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY(), getZ(), 1, 0.01, 0.01, 0.01, 0.0);
            } else if (type.startsWith("spread_")) {
                serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 2, 0.02, 0.02, 0.02, 0.0);
            } else if ("wyvern_ammo".equals(type)) {
                serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 3, 0.06, 0.06, 0.06, 0.01);
                serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 2, 0.05, 0.05, 0.05, 0.01);
            } else if ("wyvernpiercer".equals(specialType)) {
                serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 3, 0.1, 0.1, 0.1, 0.02);
            } else if ("wyvernheart".equals(specialType) || "wyverncounter".equals(specialType)) {
                serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 2, 0.05, 0.05, 0.05, 0.01);
            } else if ("wyvernblast".equals(specialType) || "wyvernblast_mine".equals(specialType)) {
                serverLevel.sendParticles(ParticleTypes.SMALL_FLAME, getX(), getY(), getZ(), 2, 0.04, 0.04, 0.04, 0.01);
            } else if (type.startsWith("normal_")) {
                serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 1, 0.01, 0.01, 0.01, 0.0);
            }
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (entity == getOwner()) return false;
        if (hitEntityIds.contains(entity.getId())) return false;
        return entity instanceof LivingEntity;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide) return;
        Entity target = result.getEntity();
        Entity owner = getOwner();

        if (sticky && attachedEntityId < 0) {
            // Attach to target
            attachedEntityId = target.getId();
            setDeltaMovement(Vec3.ZERO);
            setNoGravity(true);
            LOG.debug("[AmmoProjectile] Sticky attached to entity {}", target.getId());
            return;
        }

        hitEntityIds.add(target.getId());

        // Calculate damage
        DamageSource source = owner instanceof LivingEntity livingOwner
                ? damageSources().mobProjectile(this, livingOwner)
                : damageSources().generic();

        float totalDamage = damage;
        target.hurt(source, totalDamage);

        // Element damage as secondary
        if (elementDamage > 0 && target instanceof LivingEntity living) {
            living.hurt(source, elementDamage);
            applyElementEffect(living);
        }

        // Status application
        if (statusValue > 0 && target instanceof LivingEntity living) {
            applyStatus(living);
        }

        // Element-specific effects even without status value (elemental ammo has visual/gameplay effects)
        if (elementDamage <= 0 && statusValue <= 0 && target instanceof LivingEntity living) {
            // Check if ammo type is elemental even without configured element damage
            String ammoType = getAmmoType();
            if (ammoType.startsWith("fire_ammo") || ammoType.startsWith("flaming_ammo")
                    || ammoType.startsWith("water_ammo") || ammoType.startsWith("thunder_ammo")
                    || ammoType.startsWith("ice_ammo") || ammoType.startsWith("dragon_ammo")) {
                applyElementEffect(living);
            }
        }

        LOG.debug("[AmmoProjectile] Hit entity {} for {} dmg (+ {} elem, {} status)",
                target.getId(), totalDamage, elementDamage, statusValue);

        // Pierce check
        if (piercesRemaining > 0) {
            piercesRemaining--;
            // Don't discard — continue flying
            LOG.debug("[AmmoProjectile] Pierce remaining: {}", piercesRemaining);
        } else if (cluster) {
            // Spawn cluster bomblets
            spawnClusterBomblets();
            discard();
        } else {
            // Impact particles — distinct per ammo category
            if (level() instanceof ServerLevel serverLevel) {
                String ammoType = getAmmoType();
                if (ammoType.startsWith("normal_")) {
                    serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 5, 0.1, 0.1, 0.1, 0.05);
                } else if (ammoType.startsWith("pierce_")) {
                    // Pierce should not discard here (handled above), but for non-pierce fallthrough
                    serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY(), getZ(), 8, 0.15, 0.15, 0.15, 0.1);
                } else if (ammoType.startsWith("spread_")) {
                    serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 3, 0.05, 0.05, 0.05, 0.03);
                } else if (ammoType.startsWith("fire_ammo") || ammoType.startsWith("flaming_ammo")) {
                    serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 10, 0.2, 0.2, 0.2, 0.08);
                    serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 5, 0.15, 0.15, 0.15, 0.03);
                    serverLevel.sendParticles(colorDust(1.0f, 0.45f, 0.1f, 1.4f), getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.04);
                } else if (ammoType.startsWith("water_ammo")) {
                    serverLevel.sendParticles(ParticleTypes.SPLASH, getX(), getY(), getZ(), 15, 0.3, 0.3, 0.3, 0.2);
                    serverLevel.sendParticles(colorDust(0.1f, 0.45f, 1.0f, 1.2f), getX(), getY(), getZ(), 6, 0.2, 0.2, 0.2, 0.04);
                } else if (ammoType.startsWith("thunder_ammo")) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 12, 0.2, 0.2, 0.2, 0.15);
                    serverLevel.sendParticles(colorDust(0.95f, 0.9f, 0.2f, 1.4f), getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.05);
                } else if (ammoType.startsWith("ice_ammo")) {
                    serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 10, 0.2, 0.2, 0.2, 0.05);
                    serverLevel.sendParticles(ParticleTypes.ITEM_SNOWBALL, getX(), getY(), getZ(), 5, 0.1, 0.1, 0.1, 0.05);
                    serverLevel.sendParticles(colorDust(0.55f, 0.9f, 1.0f, 1.2f), getX(), getY(), getZ(), 6, 0.2, 0.2, 0.2, 0.04);
                } else if (ammoType.startsWith("dragon_ammo")) {
                    serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, getX(), getY(), getZ(), 15, 0.3, 0.3, 0.3, 0.05);
                    serverLevel.sendParticles(colorDust(0.6f, 0.1f, 0.9f, 1.3f), getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.04);
                } else if (ammoType.startsWith("poison_ammo")) {
                    serverLevel.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY(), getZ(), 8, 0.15, 0.15, 0.15, 0.05);
                    serverLevel.sendParticles(colorDust(0.2f, 0.9f, 0.3f, 1.1f), getX(), getY(), getZ(), 6, 0.15, 0.15, 0.15, 0.03);
                } else if (ammoType.startsWith("exhaust_ammo")) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.03);
                } else if ("wyvern_ammo".equals(ammoType)) {
                    spawnWyvernFireBurst(serverLevel);
                } else if ("demon_ammo".equals(ammoType)) {
                    serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.03);
                } else if ("armor_ammo".equals(ammoType)) {
                    serverLevel.sendParticles(ParticleTypes.COMPOSTER, getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.03);
                } else if ("tranq_ammo".equals(ammoType)) {
                    serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.03);
                } else if ("slicing_ammo".equals(ammoType)) {
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY(), getZ(), 2, 0.05, 0.05, 0.05, 0.02);
                } else {
                    serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 5, 0.1, 0.1, 0.1, 0.05);
                }
            }
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level().isClientSide) return;

        if (sticky) {
            // Stick to block
            setDeltaMovement(Vec3.ZERO);
            setNoGravity(true);
            LOG.debug("[AmmoProjectile] Sticky attached to block");
            return;
        }

        if (cluster) {
            spawnClusterBomblets();
        }

        if (specialType != null && specialType.equals("wyvernblast_mine")) {
            // Place wyvernblast mine — stays active for 30 seconds
            // For now just explode on block contact
            explode();
        }

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 3, 0.05, 0.05, 0.05, 0.01);
        }
        discard();
    }

    private void explode() {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        float radius = switch (getAmmoType()) {
            case "sticky_1" -> 2.0f;
            case "sticky_2" -> 2.5f;
            case "sticky_3" -> 3.0f;
            case "cluster_1", "cluster_2", "cluster_3" -> 1.5f;
            case "slicing_ammo" -> 0.5f;
            default -> {
                if (specialType != null) {
                    yield switch (specialType) {
                        case "wyvernblast", "wyvernblast_mine" -> 4.0f;
                        case "wyverncounter" -> 3.5f;
                        case "wyvern_howl" -> 5.0f;
                        default -> 2.0f;
                    };
                }
                yield 2.0f;
            }
        };

        float explosionDamage = damage * 1.5f;
        AABB box = getBoundingBox().inflate(radius);
        for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, box, e -> e != getOwner())) {
            double dist = entity.distanceTo(this);
            if (dist <= radius) {
                float falloff = (float) Math.max(0.3, 1.0 - (dist / radius));
                DamageSource source = getOwner() instanceof LivingEntity owner
                        ? damageSources().mobProjectile(this, owner)
                        : damageSources().generic();
                entity.hurt(source, explosionDamage * falloff);
            }
        }

        serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 10, radius / 2, radius / 2, radius / 2, 0.05);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);

        LOG.debug("[AmmoProjectile] Explosion at {} radius={} dmg={}", position(), radius, explosionDamage);
    }

        private void spawnWyvernFireBurst(ServerLevel serverLevel) {
        // Reuse Gunlance Wyvern's Fire style: explosion emitter + heavy flames
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
            getX(), getY() + 0.2, getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        serverLevel.sendParticles(ParticleTypes.FLAME,
            getX(), getY() + 0.2, getZ(), 40, 0.6, 0.6, 0.6, 0.1);
        serverLevel.sendParticles(ParticleTypes.SMOKE,
            getX(), getY() + 0.2, getZ(), 25, 0.5, 0.5, 0.5, 0.05);
        }

            private static DustParticleOptions colorDust(float r, float g, float b, float size) {
            return new DustParticleOptions(new Vector3f(r, g, b), size);
            }

    private void spawnClusterBomblets() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        LOG.debug("[AmmoProjectile] Spawning {} cluster bomblets", clusterBomblets);

        LivingEntity owner = getOwner() instanceof LivingEntity lo ? lo : null;

        for (int i = 0; i < clusterBomblets; i++) {
            AmmoProjectileEntity bomblet = new AmmoProjectileEntity(MHWeaponsItems.AMMO_PROJECTILE.get(), serverLevel);
            if (owner != null) bomblet.setOwner(owner);
            bomblet.configure(getAmmoType(), damage * 0.4f, 0, 0, 0.5f, 0.1f, 0, 0);
            bomblet.sticky = true;
            bomblet.stickyTimer = 10 + (int) (Math.random() * 10);
            bomblet.cluster = false; // Prevent recursive clusters

            double offsetX = (Math.random() - 0.5) * 2.0;
            double offsetY = Math.random() * 1.5;
            double offsetZ = (Math.random() - 0.5) * 2.0;
            bomblet.setPos(getX() + offsetX, getY() + offsetY, getZ() + offsetZ);
            bomblet.setDeltaMovement(offsetX * 0.2, offsetY * 0.3, offsetZ * 0.2);
            serverLevel.addFreshEntity(bomblet);
        }
    }

    private void applyStatus(LivingEntity target) {
        String type = getAmmoType();
        int duration = 100; // 5 seconds base
        if (type.startsWith("poison_ammo")) {
            int tier = type.endsWith("2") ? 1 : 0;
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.POISON, duration + (tier * 60), tier));
        } else if (type.startsWith("paralysis_ammo")) {
            int tier = type.endsWith("2") ? 1 : 0;
            // Stun effect: complete immobilization (high slowdown + mining fatigue)
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, (duration / 2) + (tier * 20), 10));
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, (duration / 2) + (tier * 20), 5));
        } else if (type.startsWith("sleep_ammo")) {
            int tier = type.endsWith("2") ? 1 : 0;
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, duration + (tier * 40), 10));
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.BLINDNESS, duration + (tier * 40), 0));
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.WEAKNESS, duration + (tier * 40), 3));
        } else if (type.startsWith("exhaust_ammo")) {
            int tier = type.endsWith("2") ? 1 : 0;
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.WEAKNESS, duration + (tier * 40), 1 + tier));
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, duration + (tier * 40), tier));
        } else if (type.startsWith("recovery_ammo")) {
            // Support: heals the SHOOTER, not the target
            if (getOwner() instanceof LivingEntity owner) {
                float heal = type.endsWith("2") ? statusValue * 1.5f : statusValue;
                owner.heal(heal);
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.HEART,
                            owner.getX(), owner.getY() + 1.5, owner.getZ(), 3, 0.3, 0.3, 0.3, 0.05);
                }
            }
        } else if ("demon_ammo".equals(type)) {
            // Support: buffs the SHOOTER with damage boost
            if (getOwner() instanceof LivingEntity owner) {
                owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, duration * 4, 0));
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                            owner.getX(), owner.getY() + 1.5, owner.getZ(), 5, 0.3, 0.5, 0.3, 0.02);
                }
            }
        } else if ("armor_ammo".equals(type)) {
            // Support: buffs the SHOOTER with damage resistance
            if (getOwner() instanceof LivingEntity owner) {
                owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, duration * 4, 0));
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.COMPOSTER,
                            owner.getX(), owner.getY() + 1.5, owner.getZ(), 5, 0.3, 0.5, 0.3, 0.02);
                }
            }
        } else if ("tranq_ammo".equals(type)) {
            // Tranquilizer: heavy slow + weakness + blindness (for capturing)
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, duration * 3, 3));
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.WEAKNESS, duration * 3, 2));
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.BLINDNESS, duration, 0));
        }
    }

    /**
     * Apply element-specific on-hit effects beyond raw damage.
     * Called after normal damage in onHitEntity for elemental ammo.
     */
    private void applyElementEffect(LivingEntity target) {
        String type = getAmmoType();
        int duration = 80; // 4 seconds

        if (type.startsWith("fire_ammo") || type.startsWith("flaming_ammo")) {
            // Fire: ignite target (sets on fire)
            int fireTicks = type.contains("flaming") ? 100 : 60;
            target.setSecondsOnFire(fireTicks / 20);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.FLAME,
                        target.getX(), target.getY() + 0.5, target.getZ(), 8, 0.3, 0.5, 0.3, 0.05);
            }
        } else if (type.startsWith("water_ammo")) {
            // Water: knockback + brief slowdown
            Vec3 knockDir = target.position().subtract(position()).normalize().scale(0.5);
            target.push(knockDir.x, 0.2, knockDir.z);
            target.hurtMarked = true;
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SPLASH,
                        target.getX(), target.getY() + 0.5, target.getZ(), 12, 0.4, 0.4, 0.4, 0.1);
            }
        } else if (type.startsWith("thunder_ammo")) {
            // Thunder: chain stun (brief movement lock + visual sparks)
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 15, 5));
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        target.getX(), target.getY() + 0.5, target.getZ(), 15, 0.5, 0.8, 0.5, 0.1);
                // Chain lightning visual to nearby mobs
                for (LivingEntity nearby : level().getEntitiesOfClass(LivingEntity.class,
                        target.getBoundingBox().inflate(3.0), e -> e != target && e != getOwner())) {
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            nearby.getX(), nearby.getY() + 0.5, nearby.getZ(), 5, 0.2, 0.3, 0.2, 0.05);
                    break; // Just visual chain to nearest mob
                }
            }
        } else if (type.startsWith("ice_ammo")) {
            // Ice: slowdown + mining fatigue
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, duration, 2));
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, duration, 1));
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE,
                        target.getX(), target.getY() + 0.5, target.getZ(), 10, 0.4, 0.6, 0.4, 0.05);
            }
        } else if (type.startsWith("dragon_ammo")) {
            // Dragon: defense shred (amplified incoming damage)
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.WEAKNESS, duration, 1)); // Weaken attacks
            // Use unluck as a proxy for "dragon blight" — reduced defenses
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.UNLUCK, duration, 2));
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.DRAGON_BREATH,
                        target.getX(), target.getY() + 0.5, target.getZ(), 12, 0.5, 0.7, 0.5, 0.03);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("AmmoType", getAmmoType());
        tag.putFloat("Damage", damage);
        tag.putFloat("ElemDmg", elementDamage);
        tag.putFloat("StatusVal", statusValue);
        tag.putFloat("Speed", speed);
        tag.putFloat("Gravity", gravityRate);
        tag.putInt("Pierce", pierceCount);
        tag.putInt("PiercesLeft", piercesRemaining);
        tag.putBoolean("Sticky", sticky);
        tag.putInt("StickyTimer", stickyTimer);
        tag.putInt("AttachedEntity", attachedEntityId);
        tag.putBoolean("Cluster", cluster);
        tag.putInt("ClusterBomblets", clusterBomblets);
        if (specialType != null) tag.putString("SpecialType", specialType);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(AMMO_TYPE, tag.getString("AmmoType"));
        damage = tag.getFloat("Damage");
        elementDamage = tag.getFloat("ElemDmg");
        statusValue = tag.getFloat("StatusVal");
        speed = tag.getFloat("Speed");
        gravityRate = tag.getFloat("Gravity");
        pierceCount = tag.getInt("Pierce");
        piercesRemaining = tag.getInt("PiercesLeft");
        sticky = tag.getBoolean("Sticky");
        stickyTimer = tag.getInt("StickyTimer");
        attachedEntityId = tag.getInt("AttachedEntity");
        cluster = tag.getBoolean("Cluster");
        clusterBomblets = tag.getInt("ClusterBomblets");
        specialType = tag.contains("SpecialType") ? tag.getString("SpecialType") : null;
    }
}
