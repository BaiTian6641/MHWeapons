package org.example.common.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.util.CapabilityUtil;
import org.example.item.WeaponIdProvider;

@SuppressWarnings("null")
public class KinsectEntity extends Projectile {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(KinsectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(KinsectEntity.class, EntityDataSerializers.INT);
    private static final double FLY_SPEED = 0.9;
    private static final double RETURN_SPEED = 1.1;
    private static final int MAX_EXTRACT_TICKS = 1200;

    private KinsectState state = KinsectState.HOVERING;
    private int targetEntityId = -1;
    private Vec3 targetPos;
    private Vec3 targetHitPos;
    private Vec3 hoverPos;
    private int collectedMask;
    private int maxExtracts = 1;
    private double flySpeed = FLY_SPEED;
    private double returnSpeed = RETURN_SPEED;
    private double maxRange = 20.0;
    private float damage = 2.0f;
    private float hoverAngle;

    public KinsectEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public KinsectEntity(EntityType<? extends Projectile> type, Level level, LivingEntity owner) {
        super(type, level);
        setOwner(owner);
        setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        setNoGravity(true);
        configureFromOwner(owner);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(COLOR, 0);
        this.entityData.define(STATE, 0);
    }

    public int getColor() {
        return this.entityData.get(COLOR);
    }

    public void setColor(int color) {
        this.entityData.set(COLOR, color);
    }

    public String getStateName() {
        int id = this.entityData.get(STATE);
        return switch (id) {
            case 1 -> "Flying";
            case 2 -> "Returning";
            default -> "Hovering";
        };
    }

    public int getTargetEntityId() {
        return targetEntityId;
    }

    public Vec3 getTargetPos() {
        return targetPos;
    }

    @Override
    public void tick() {
        if (this.level().isClientSide) {
            super.tick();
            return;
        }
        switch (state) {
            case FLYING -> tickFlying();
            case HOVERING -> tickHovering();
            case RETURNING -> tickReturning();
        }
        super.tick();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (state != KinsectState.FLYING) {
            return;
        }
        if (result.getEntity() instanceof LivingEntity target) {
            collectExtract(target, result.getLocation());
            enterHover(result.getLocation());
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        // Ignore block hits to keep moving toward target
    }

    public void launchTo(Vec3 pos, LivingEntity target, Vec3 hitPos) {
        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
        Vec3 finalPos = pos;
        if (owner != null && maxRange > 0) {
            Vec3 origin = owner.position().add(0, owner.getBbHeight() * 0.6, 0);
            Vec3 delta = pos.subtract(origin);
            double dist = delta.length();
            if (dist > maxRange) {
                finalPos = origin.add(delta.normalize().scale(maxRange));
            }
        }
        this.targetHitPos = hitPos != null ? hitPos : finalPos;
        this.targetPos = this.targetHitPos;
        this.targetEntityId = target != null ? target.getId() : -1;
        this.hoverPos = null;
        setState(KinsectState.FLYING);
    }

    public void recall() {
        this.hoverPos = null;
        setState(KinsectState.RETURNING);
    }

    public boolean isHovering() {
        return state == KinsectState.HOVERING;
    }

    private void returnToOwner() {
        if (getOwner() instanceof ServerPlayer player) {
            PlayerWeaponState state = CapabilityUtil.getPlayerWeaponState(player);
            if (state != null) {
                if ((collectedMask & 1) != 0) state.setInsectRed(true);
                if ((collectedMask & 2) != 0) state.setInsectWhite(true);
                if ((collectedMask & 4) != 0) state.setInsectOrange(true);
                if (collectedMask != 0) {
                    state.setInsectExtractTicks(MAX_EXTRACT_TICKS);
                }
                state.setKinsectEntityId(-1);
            }
        }
        discard();
    }

    private void configureFromOwner(LivingEntity owner) {
        if (owner instanceof ServerPlayer player) {
            if (player.getOffhandItem().getItem() instanceof org.example.item.KinsectItem kinsectItem) {
                maxExtracts = kinsectItem.getMaxExtracts();
                flySpeed = kinsectItem.getSpeed();
                returnSpeed = Math.max(flySpeed * 1.1, RETURN_SPEED);
                maxRange = kinsectItem.getRange();
                damage = kinsectItem.getDamage();
                return;
            }
            if (player.getMainHandItem().getItem() instanceof WeaponIdProvider) {
                int multi = player.getMainHandItem().getOrCreateTag().getInt("mh_kinsect_multi");
                if (multi >= 2) {
                    maxExtracts = 2;
                }
            }
        }
    }

    private void tickFlying() {
        Vec3 target = resolveTargetPosition();
        if (target == null) {
            enterHover(position());
            return;
        }
        Vec3 delta = target.subtract(position());
        double distance = delta.length();
        if (distance < 0.6) {
            LivingEntity targetEntity = resolveTargetEntity();
            if (targetEntity != null) {
                collectExtract(targetEntity, targetHitPos != null ? targetHitPos : target);
            }
            enterHover(target);
            return;
        }
        Vec3 desired = delta.normalize().scale(flySpeed);
        Vec3 velocity = getDeltaMovement().scale(0.6).add(desired.scale(0.4));
        setDeltaMovement(velocity);
        setPos(getX() + velocity.x, getY() + velocity.y, getZ() + velocity.z);
    }

    private void tickHovering() {
        Vec3 hoverCenter = targetHitPos != null ? targetHitPos : targetPos;
        if (hoverCenter != null) {
            hoverAngle += 0.15f;
            double radius = 0.8;
            double x = Math.cos(hoverAngle) * radius;
            double z = Math.sin(hoverAngle) * radius;
            double y = Math.sin(hoverAngle * 0.5) * 0.15;
            Vec3 pos = hoverCenter.add(x, y, z);
            setDeltaMovement(Vec3.ZERO);
            setPos(pos.x, pos.y, pos.z);
            return;
        }
        if (hoverPos != null) {
            setDeltaMovement(Vec3.ZERO);
            setPos(hoverPos.x, hoverPos.y, hoverPos.z);
        }
    }

    private void tickReturning() {
        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }
        Vec3 target = owner.position().add(0, owner.getBbHeight() * 0.7, 0);
        Vec3 delta = target.subtract(position());
        if (delta.length() < 1.2) {
            returnToOwner();
            return;
        }
        Vec3 desired = delta.normalize().scale(returnSpeed);
        Vec3 velocity = getDeltaMovement().scale(0.6).add(desired.scale(0.4));
        setDeltaMovement(velocity);
        setPos(getX() + velocity.x, getY() + velocity.y, getZ() + velocity.z);
    }

    private Vec3 resolveTargetPosition() {
        if (targetHitPos != null) {
            return targetHitPos;
        }
        if (targetPos != null) {
            return targetPos;
        }
        LivingEntity target = resolveTargetEntity();
        if (target != null) {
            return target.position().add(0, target.getBbHeight() * 0.6, 0);
        }
        return null;
    }

    private LivingEntity resolveTargetEntity() {
        if (targetEntityId <= 0) {
            return null;
        }
        Entity entity = level().getEntity(targetEntityId);
        if (entity instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private void collectExtract(LivingEntity target, Vec3 hitPos) {
        int extractColor = resolveExtractColor(target, hitPos);
        addExtractColor(extractColor);
        setColor(extractColor);
        if (damage > 0.0f && getOwner() instanceof LivingEntity owner) {
            target.hurt(owner.damageSources().mobAttack(owner), damage);
        }
    }

    private void addExtractColor(int color) {
        if (color == 1 && (collectedMask & 1) == 0 && countExtracts() < maxExtracts) {
            collectedMask |= 1;
        } else if (color == 2 && (collectedMask & 2) == 0 && countExtracts() < maxExtracts) {
            collectedMask |= 2;
        } else if (color == 3 && (collectedMask & 4) == 0 && countExtracts() < maxExtracts) {
            collectedMask |= 4;
        }
    }

    private int countExtracts() {
        int count = 0;
        if ((collectedMask & 1) != 0) count++;
        if ((collectedMask & 2) != 0) count++;
        if ((collectedMask & 4) != 0) count++;
        return count;
    }

    private void enterHover(Vec3 pos) {
        hoverPos = pos;
        targetPos = pos;
        setState(KinsectState.HOVERING);
        setDeltaMovement(Vec3.ZERO);
        setPos(pos.x, pos.y, pos.z);
    }

    private void setState(KinsectState next) {
        this.state = next;
        this.entityData.set(STATE, next.ordinal());
    }

    private int resolveExtractColor(LivingEntity target, Vec3 hitPos) {
        double height = target.getBbHeight();
        double headY = target.getY() + (height * 0.75);
        double lowY = target.getY() + (height * 0.40);
        double y = hitPos != null ? hitPos.y : getY();
        if (y >= headY) return 1;
        if (y <= lowY) return 2;
        return 3;
    }

    private enum KinsectState {
        FLYING,
        HOVERING,
        RETURNING
    }
}
