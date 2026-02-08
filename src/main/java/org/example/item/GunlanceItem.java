package org.example.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.combat.MHDamageType;

public class GunlanceItem extends GeoWeaponItem {
    public enum ShellingType {
        NORMAL,
        LONG,
        WIDE
    }

    private final ShellingType shellingType;
    private final int shellLevel;

    public GunlanceItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Item.Properties properties, ShellingType shellingType, int shellLevel) {
        super("gunlance", MHDamageType.SEVER, tier, attackDamageModifier, attackSpeedModifier, properties);
        this.shellingType = shellingType;
        this.shellLevel = shellLevel;
    }

    public ShellingType getShellingType() {
        return shellingType;
    }

    public int getShellLevel() {
        return shellLevel;
    }

    public void useShell(Level level, Player player, PlayerWeaponState state, boolean charged) {
        if (state.getGunlanceShells() <= 0) {
            return;
        }

        int availableShells = state.getGunlanceShells();
        int shots = charged ? Math.max(1, Math.min(availableShells, state.getGunlanceMaxShells())) : 1;
        state.setGunlanceShells(availableShells - shots);
        state.setGunlanceCooldown(charged ? 30 : 10);

        float baseDamage = 15.0f + (shellLevel * 5.0f); // Example formula
        if (charged) baseDamage *= (0.75f + 0.35f * shots);
        if (shellingType == ShellingType.WIDE) baseDamage *= 1.2f;

        double range = resolveShellRange();
        boolean hit = performBlast(level, player, range, baseDamage);
        if (hit) {
            float perShellCharge = charged ? 0.10f : 0.06f;
            addWyvernFireCharge(state, perShellCharge * shots);
        }
        spawnShellParticles(level, player, range, charged);
    }

    public void useWyvernFire(Level level, Player player, PlayerWeaponState state) {
        if (state.getGunlanceWyvernFireGauge() < 1.0f) {
            return;
        }
        // Consume gauge immediately
        state.addGunlanceWyvernFireGauge(-1.0f);
        state.setGunlanceWyvernfireCooldown(100); // Cooldown starts
        
        // Start Charge Sequence
        state.setGunlanceCharging(true);
        state.setGunlanceChargeTicks(40); // 2 seconds charge up

        // Initial Charge Particles
        if (level instanceof ServerLevel serverLevel) {
             Vec3 look = player.getLookAngle();
             serverLevel.sendParticles(ParticleTypes.FLAME,
                     player.getX() + look.x,
                     player.getEyeY() + look.y,
                     player.getZ() + look.z,
                     20, 0.2, 0.2, 0.2, 0.05);
        }
    }

    public void useWyvernFireBlast(Level level, Player player, PlayerWeaponState state) {
        float damage = 80.0f + (shellLevel * 10.0f);
        performLineBlast(level, player, 6.0, damage, 1.6);
        
        // Blast Particles
        if (level instanceof ServerLevel serverLevel) {
             Vec3 look = player.getLookAngle();
             serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                     player.getX() + look.x * 2.5,
                     player.getEyeY() + look.y * 2.5,
                     player.getZ() + look.z * 2.5,
                     1, 0.0, 0.0, 0.0, 0.0);
             serverLevel.sendParticles(ParticleTypes.FLAME,
                     player.getX() + look.x * 2.0,
                     player.getEyeY() + look.y * 2.0,
                     player.getZ() + look.z * 2.0,
                     50, 0.5, 0.5, 0.5, 0.1);
        }
        
        // Recoil Pushback
        Vec3 look = player.getLookAngle().normalize();
        player.push(-look.x * 1.5, 0.2, -look.z * 1.5);
        player.hurtMarked = true;
    }

    private void performLineBlast(Level level, Player player, double range, float damage, double radius) {
        Vec3 start = player.getEyePosition();
        Vec3 dir = player.getLookAngle().normalize();
        Vec3 end = start.add(dir.scale(range));
        AABB box = player.getBoundingBox().expandTowards(dir.scale(range)).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)) {
            if (target.getBoundingBox().inflate(0.3).clip(start, end).isPresent()) {
                target.hurt(player.damageSources().playerAttack(player), damage);
            }
        }
    }

    public void useWyrmstake(Level level, Player player, PlayerWeaponState state) {
        if (!state.hasGunlanceStake()) {
            return;
        }
        state.setGunlanceHasStake(false);
        // In reality this should spawn an entity
        double range = shellingType == ShellingType.LONG ? 5.0 : 4.2;
        performBlast(level, player, range, 30.0f);
        spawnWyrmstakeParticles(level, player, range);
    }

    public void reload(Player player, PlayerWeaponState state, boolean full) {
        state.setGunlanceShells(state.getGunlanceMaxShells());
        if (full) {
            state.setGunlanceHasStake(true);
        }
    }

    public void quickReload(Player player, PlayerWeaponState state) {
        if (state.getGunlanceShells() >= state.getGunlanceMaxShells()) {
            return;
        }
        state.setGunlanceShells(state.getGunlanceShells() + 1);
    }

    public void useBurstFire(Level level, Player player, PlayerWeaponState state) {
        int shells = Math.min(3, state.getGunlanceShells());
        if (shells <= 0) {
            return;
        }
        state.setGunlanceShells(state.getGunlanceShells() - shells);
        state.setGunlanceCooldown(18);

        float baseDamage = 12.0f + (shellLevel * 4.0f);
        if (shellingType == ShellingType.WIDE) baseDamage *= 1.15f;
        double range = resolveShellRange();
        for (int i = 0; i < shells; i++) {
            boolean hit = performBlast(level, player, range, baseDamage);
            if (hit) {
                addWyvernFireCharge(state, 0.06f);
            }
            spawnShellParticles(level, player, range, false);
        }
    }

    private boolean performBlast(Level level, Player player, double range, float damage) {
        Vec3 start = player.getEyePosition();
        Vec3 dir = player.getLookAngle().normalize();
        Vec3 end = start.add(dir.scale(range));
        AABB box = player.getBoundingBox().expandTowards(dir.scale(range)).inflate(1.0);
        LivingEntity target = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                .stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                .orElse(null);

        if (target != null && target.getBoundingBox().clip(start, end).isPresent()) {
            target.hurt(player.damageSources().playerAttack(player), damage);
            return true;
        }
        return false;
    }

    private void addWyvernFireCharge(PlayerWeaponState state, float amount) {
        if (amount <= 0.0f) {
            return;
        }
        state.addGunlanceWyvernFireGauge(amount);
    }

    private double resolveShellRange() {
        return switch (shellingType) {
            case LONG -> 6.0;
            case WIDE -> 4.5;
            default -> 5.0;
        };
    }

    private void spawnShellParticles(Level level, Player player, double range, boolean charged) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 start = player.getEyePosition();
        Vec3 dir = player.getLookAngle().normalize();
        Vec3 muzzle = start.add(dir.scale(0.6));
        Vec3 end = start.add(dir.scale(range));

        int flameCount = charged ? 14 : 8;
        int smokeCount = charged ? 10 : 6;
        serverLevel.sendParticles(ParticleTypes.FLAME,
                muzzle.x, muzzle.y, muzzle.z,
                flameCount, 0.05, 0.05, 0.05, 0.08);
        serverLevel.sendParticles(ParticleTypes.SMOKE,
                muzzle.x, muzzle.y, muzzle.z,
                smokeCount, 0.08, 0.08, 0.08, 0.05);

        serverLevel.sendParticles(ParticleTypes.SMALL_FLAME,
                end.x, end.y, end.z,
                charged ? 6 : 3, 0.12, 0.12, 0.12, 0.05);
        if (charged) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    end.x, end.y, end.z,
                    1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private void spawnWyvernFireParticles(Level level, Player player, double range) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 start = player.getEyePosition();
        Vec3 dir = player.getLookAngle().normalize();
        Vec3 muzzle = start.add(dir.scale(0.6));
        Vec3 end = start.add(dir.scale(range));

        serverLevel.sendParticles(ParticleTypes.FLAME,
                muzzle.x, muzzle.y, muzzle.z,
                20, 0.08, 0.08, 0.08, 0.02);
        serverLevel.sendParticles(ParticleTypes.SMOKE,
                muzzle.x, muzzle.y, muzzle.z,
                16, 0.12, 0.12, 0.12, 0.02);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                end.x, end.y, end.z,
                1, 0.0, 0.0, 0.0, 0.0);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                end.x, end.y, end.z,
                10, 0.4, 0.3, 0.4, 0.01);
    }

    private void spawnWyrmstakeParticles(Level level, Player player, double range) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 start = player.getEyePosition();
        Vec3 dir = player.getLookAngle().normalize();
        Vec3 end = start.add(dir.scale(range));

        serverLevel.sendParticles(ParticleTypes.CRIT,
                end.x, end.y, end.z,
                10, 0.2, 0.2, 0.2, 0.02);
        serverLevel.sendParticles(ParticleTypes.SMOKE,
                end.x, end.y, end.z,
                8, 0.18, 0.18, 0.18, 0.01);
    }

    @Override
    @SuppressWarnings("null")
    public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        if (hand == net.minecraft.world.InteractionHand.OFF_HAND) {
             return net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        // If Shift is held, we want to trigger WSC (handled by client event -> packet).
        // Return FAIL/PASS here so we don't start the 'use' action (charging)
        if (player.isShiftKeyDown()) {
             return net.minecraft.world.InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        player.startUsingItem(hand);
        return net.minecraft.world.InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(net.minecraft.world.item.ItemStack stack) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, net.minecraft.world.item.ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof Player player)) {
            return;
        }
        PlayerWeaponState state = org.example.common.util.CapabilityUtil.getPlayerWeaponState(player);
        if (state == null) return;
        
        int chargeTicks = getUseDuration(stack) - remainingUseDuration;
        
        // Spawn charge particles
        if (chargeTicks > 5 && chargeTicks < 25) {
             if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                 serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                         player.getX(), player.getEyeY() - 0.2, player.getZ(),
                         2, 0.1, 0.1, 0.1, 0.01);
             }
        }
    }

    @Override
    public void releaseUsing(net.minecraft.world.item.ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }
        PlayerWeaponState state = org.example.common.util.CapabilityUtil.getPlayerWeaponState(player);
        if (state == null) return;

        int chargeTicks = getUseDuration(stack) - timeLeft;
        boolean charged = chargeTicks >= 20; // 1 second hold for charged shell

        useShell(level, player, state, charged);
        
        org.example.common.capability.player.PlayerCombatState combatState = org.example.common.util.CapabilityUtil.getPlayerCombatState(player);
        if (combatState != null) {
            combatState.setActionKey("shell");
            combatState.setActionKeyTicks(10);
        }
    }
}
