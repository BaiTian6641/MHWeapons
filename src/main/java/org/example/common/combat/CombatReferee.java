package org.example.common.combat;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import org.example.common.capability.mob.MobStatusState;
import org.example.common.capability.mob.MobWoundState;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.util.CapabilityUtil;
import org.example.common.network.ModNetwork;
import org.example.common.network.packet.DamageNumberS2CPacket;
import org.example.item.WeaponIdProvider;
import org.example.registry.MHAttributes;

public final class CombatReferee {
    public static final String MOTION_VALUE_KEY = "mh_motion_value";
    private static final String DRAW_BONUS_TICKS = "mh_draw_bonus_ticks";
    private static final String OFFENSIVE_GUARD_TICKS = "mh_offensive_guard_ticks";

    private final GuardSystem guardSystem;
    private final DodgeSystem dodgeSystem;
    private final WoundSystem woundSystem;

    public CombatReferee() {
        this.guardSystem = new GuardSystem();
        this.dodgeSystem = new DodgeSystem();
        this.woundSystem = new WoundSystem();
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (dodgeSystem.tryCancelAttack(player, event)) {
                return;
            }
            if (guardSystem.tryGuard(player, event)) {
                return;
            }
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (guardSystem.applyGuard(player, event)) {
                return;
            }
            
            // Tonfa Jet Counter (Air Clash)
            if (player.getMainHandItem().getItem() instanceof org.example.item.WeaponIdProvider weaponIdProvider 
                    && "tonfa".equals(weaponIdProvider.getWeaponId())) {
                PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(player);
                if (combatState != null && "midair_evade".equals(combatState.getActionKey()) && combatState.getActionKeyTicks() > 0) {
                     event.setCanceled(true);
                     performTonfaJetCounter(player, event.getSource().getEntity());
                     return;
                }
            }

            // Switch Axe Counter (Offset Rising Slash / Counter Rising Slash)
            if (player.getMainHandItem().getItem() instanceof org.example.item.WeaponIdProvider saWeaponId
                    && "switch_axe".equals(saWeaponId.getWeaponId())) {
                PlayerWeaponState saState = CapabilityUtil.getPlayerWeaponState(player);
                PlayerCombatState saCombatState = CapabilityUtil.getPlayerCombatState(player);
                if (saState != null && saCombatState != null && saState.getSwitchAxeCounterTicks() > 0) {
                    if (org.example.common.combat.weapon.SwitchAxeHandler.tryConsumeCounter(player, saCombatState, saState)) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }

            // Charge Blade Guard Point
            if (player.getMainHandItem().getItem() instanceof org.example.item.WeaponIdProvider cbWeaponId
                    && "charge_blade".equals(cbWeaponId.getWeaponId())) {
                PlayerWeaponState cbState = CapabilityUtil.getPlayerWeaponState(player);
                PlayerCombatState cbCombatState = CapabilityUtil.getPlayerCombatState(player);
                if (cbState != null && cbCombatState != null && cbState.getCbGuardPointTicks() > 0) {
                    if (org.example.common.combat.weapon.ChargeBladeHandler.tryConsumeGuardPoint(player, cbCombatState, cbState)) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }

            // Hammer Offset Uppercut counter: cancel incoming damage during charged upswing
            if (player.getMainHandItem().getItem() instanceof org.example.item.WeaponIdProvider hamWeaponId
                    && "hammer".equals(hamWeaponId.getWeaponId())) {
                PlayerCombatState hamCombatState = CapabilityUtil.getPlayerCombatState(player);
                if (hamCombatState != null && hamCombatState.getActionKeyTicks() > 0) {
                    String hamAction = hamCombatState.getActionKey();
                    // Offset: both standard Upswing and Charged Upswing can offset monster attacks
                    if ("hammer_charged_upswing".equals(hamAction) || "hammer_upswing".equals(hamAction)) {
                        event.setCanceled(true);
                        hamCombatState.setActionKey("hammer_offset_uppercut");
                        hamCombatState.setActionKeyTicks(10);
                        return;
                    }
                }
            }

            PlayerWeaponState weaponState = CapabilityUtil.getPlayerWeaponState(player);
            if (weaponState != null && weaponState.getHornDefenseLargeTicks() > 0) {
                event.setAmount(event.getAmount() * 0.8f);
            }
            return;
        }

        LivingEntity target = event.getEntity();
        LivingEntity attacker = resolveAttacker(event);
        if (attacker == null) {
            return;
        }

        boolean criticalHit = false;
        boolean negativeAffinityHit = false;
        float elementCritBonus = 0.0f;
        float statusCritBonus = 0.0f;

        if (attacker instanceof Player player) {
            PlayerWeaponState weaponState = CapabilityUtil.getPlayerWeaponState(player);
            if (event.getSource().getDirectEntity() instanceof AbstractArrow) {
                if (weaponState != null) {
                    applyBowCoating(target, player, weaponState.getBowCoating());
                }
            }

            ItemStack stack = player.getMainHandItem();
            boolean ranged = isRangedWeapon(stack);

            if (weaponState != null) {
                float hornMult = 1.0f;
                if (weaponState.getHornAttackSmallTicks() > 0) {
                    hornMult += 0.10f;
                }
                if (weaponState.getHornAttackLargeTicks() > 0) {
                    hornMult += 0.20f;
                }
                if (weaponState.getHornMelodyHitTicks() > 0) {
                    hornMult += 0.05f;
                }
                event.setAmount(event.getAmount() * hornMult);
                if (weaponState.getHornAffinityTicks() > 0 && player.getRandom().nextFloat() < 0.20f) {
                    event.setAmount(event.getAmount() * 1.25f);
                }

                // Hammer Power Charge buff
                if (player.getMainHandItem().getItem() instanceof org.example.item.WeaponIdProvider hamWeaponId
                        && "hammer".equals(hamWeaponId.getWeaponId())
                        && weaponState.isHammerPowerCharge()) {
                    float pcAtkMult = org.example.common.data.WeaponDataResolver.resolveFloat(player, null, "powerChargeBuff.attackMultiplier", 1.15f);
                    event.setAmount(event.getAmount() * pcAtkMult);
                }
            }

            float affinityValue = getAttrSafe(attacker, MHAttributes.AFFINITY.get());
            if (affinityValue < 0.0f) {
                float negChance = Math.min(1.0f, Math.abs(affinityValue) / 100.0f);
                if (negChance > 0.0f && player.getRandom().nextFloat() < negChance) {
                    event.setAmount(event.getAmount() * 0.75f);
                    negativeAffinityHit = true;
                }
            } else {
                float affinityChance = Math.max(0.0f, affinityValue / 100.0f);
                float drawAffinityBonus = resolveDrawAffinityBonus(player);
                if (drawAffinityBonus > 0.0f) {
                    affinityChance += (drawAffinityBonus / 100.0f);
                }
                affinityChance = Math.min(affinityChance, 1.0f);
                if (affinityChance > 0.0f && player.getRandom().nextFloat() < affinityChance) {
                    float critBonus = Math.max(0.0f, getAttrSafe(attacker, MHAttributes.CRIT_DAMAGE_BONUS.get()));
                    event.setAmount(event.getAmount() * (1.25f + critBonus));
                    criticalHit = true;
                }
            }

            if (hasOffensiveGuard(player)) {
                float offGuardBonus = Math.max(0.0f, getAttrSafe(attacker, MHAttributes.OFFENSIVE_GUARD_BONUS.get()));
                if (offGuardBonus > 0.0f) {
                    event.setAmount(event.getAmount() * (1.0f + offGuardBonus));
                }
            }

            if (!ranged) {
                float sharpnessMod = applySharpness(stack, player, criticalHit);
                event.setAmount(event.getAmount() * sharpnessMod);
                float sharpness = getSharpness(stack);
                float bluntBonus = Math.max(0.0f, getAttrSafe(attacker, MHAttributes.BLUDGEONER_BONUS.get()));
                if (bluntBonus > 0.0f && sharpness <= 25.0f) {
                    event.setAmount(event.getAmount() * (1.0f + bluntBonus));
                }
            }

            elementCritBonus = Math.max(0.0f, getAttrSafe(attacker, MHAttributes.ELEMENT_CRIT_BONUS.get()));
            statusCritBonus = Math.max(0.0f, getAttrSafe(attacker, MHAttributes.STATUS_CRIT_BONUS.get()));
        }

        MHDamageType damageType = resolveDamageType(attacker);
        float motionValue = resolveMotionValue(attacker);
        float hitzoneMultiplier = resolveHitzoneMultiplier(attacker, target, damageType);
        float woundMultiplier = woundSystem.getWoundMultiplier(target);

        float physDamage = event.getAmount() * motionValue * hitzoneMultiplier * woundMultiplier;

        float elementDamage = 0.0f;
        elementDamage += getAttrSafe(attacker, MHAttributes.FIRE_DAMAGE.get());
        elementDamage += getAttrSafe(attacker, MHAttributes.WATER_DAMAGE.get());
        elementDamage += getAttrSafe(attacker, MHAttributes.THUNDER_DAMAGE.get());
        elementDamage += getAttrSafe(attacker, MHAttributes.ICE_DAMAGE.get());
        elementDamage += getAttrSafe(attacker, MHAttributes.DRAGON_DAMAGE.get());

        elementDamage = elementDamage * motionValue * woundMultiplier;
        if (criticalHit && elementCritBonus > 0.0f) {
            elementDamage *= (1.0f + elementCritBonus);
        }

        event.setAmount(physDamage + elementDamage);

        if (attacker instanceof ServerPlayer serverPlayer) {
            int colorType = hitzoneMultiplier >= 1.0f
                ? DamageNumberS2CPacket.COLOR_YELLOW
                : DamageNumberS2CPacket.COLOR_WHITE;
            int frameType = criticalHit
                ? DamageNumberS2CPacket.FRAME_AFFINITY
                : (negativeAffinityHit ? DamageNumberS2CPacket.FRAME_NEGATIVE_AFFINITY : DamageNumberS2CPacket.FRAME_NONE);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                new DamageNumberS2CPacket(target.getId(), event.getAmount(), colorType, frameType));
        }

        applyStatusBuildup(attacker, target, motionValue, criticalHit, statusCritBonus);

        woundSystem.applyWoundLogic(event);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        dodgeSystem.tick(player);
        guardSystem.tick(player);
        woundSystem.tick(player.level());
        tickPlayerTimers(player);
    }

    private LivingEntity resolveAttacker(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity livingEntity) {
            return livingEntity;
        }
        if (event.getSource().getDirectEntity() instanceof LivingEntity directEntity) {
            return directEntity;
        }
        return null;
    }

    @SuppressWarnings("null")
    private void applyStatusBuildup(LivingEntity attacker, LivingEntity target, float motionValue, boolean criticalHit, float statusCritBonus) {
        if (attacker == null || target == null) {
            return;
        }
        MobStatusState status = CapabilityUtil.getMobStatusState(target);
        if (status == null) {
            return;
        }

        float poison = getAttrSafe(attacker, MHAttributes.POISON_BUILDUP.get());
        float paralysis = getAttrSafe(attacker, MHAttributes.PARALYSIS_BUILDUP.get());
        float sleep = getAttrSafe(attacker, MHAttributes.SLEEP_BUILDUP.get());
        float blast = getAttrSafe(attacker, MHAttributes.BLAST_BUILDUP.get());

        float scaled = Math.max(0.0f, motionValue);
        float critScale = criticalHit && statusCritBonus > 0.0f ? (1.0f + statusCritBonus) : 1.0f;

        status.setPoisonBuildup(status.getPoisonBuildup() + poison * scaled * critScale);
        status.setParalysisBuildup(status.getParalysisBuildup() + paralysis * scaled * critScale);
        status.setSleepBuildup(status.getSleepBuildup() + sleep * scaled * critScale);
        status.setBlastBuildup(status.getBlastBuildup() + blast * scaled * critScale);

        if (status.getPoisonBuildup() >= 100.0f) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        }
        if (status.getParalysisBuildup() >= 120.0f) {
            status.setParalysisBuildup(0.0f);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
        }
        if (status.getSleepBuildup() >= 140.0f) {
            status.setSleepBuildup(0.0f);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 6));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 2));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0));
        }
        if (status.getBlastBuildup() >= 110.0f) {
            status.setBlastBuildup(0.0f);
            target.hurt(target.damageSources().magic(), 6.0f);
        }
    }

    private MHDamageType resolveDamageType(LivingEntity attacker) {
        if (attacker instanceof Player player) {
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof MHDamageTypeProvider provider) {
                if (stack.getItem() instanceof WeaponIdProvider weaponIdProvider
                        && "magnet_spike".equals(weaponIdProvider.getWeaponId())) {
                    PlayerWeaponState weaponState = CapabilityUtil.getPlayerWeaponState(player);
                    if (weaponState != null && weaponState.isMagnetSpikeImpactMode()) {
                        return MHDamageType.BLUNT;
                    }
                }
                return provider.getDamageType(stack);
            }
        }
        return MHDamageType.SEVER;
    }

    private float resolveMotionValue(LivingEntity attacker) {
        if (attacker.getPersistentData().contains(MOTION_VALUE_KEY)) {
            float mv = attacker.getPersistentData().getFloat(MOTION_VALUE_KEY);
            if (mv > 0.0f) {
                return mv;
            }
        }
        return 1.0f;
    }

    @SuppressWarnings("null")
    private float resolveHitzoneMultiplier(LivingEntity attacker, LivingEntity target, MHDamageType damageType) {
        if (attacker instanceof Player player && player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider) {
            if ("tonfa".equals(weaponIdProvider.getWeaponId())) {
                PlayerWeaponState weaponState = CapabilityUtil.getPlayerWeaponState(player);
                if (weaponState != null && weaponState.isTonfaShortMode()) {
                    // Impact Conversion: Reversed Impact Mapping
                    // In Short Mode, poor blunt zones become better and strong blunt zones are normalized.
                    // This is NOT "use sever hitzone" — it's an inversion of the blunt effectiveness.
                    float blunt = resolveHitzoneMultiplierInternal(attacker, target, MHDamageType.BLUNT);
                    float sever = resolveHitzoneMultiplierInternal(attacker, target, MHDamageType.SEVER);

                    // Reversed mapping: use the better of sever/blunt ratio, capped at 1.2x
                    float conversionRatio = (blunt > 0.01f) ? Math.min(sever / blunt, 1.2f) : 1.0f;
                    float converted = blunt * Math.max(conversionRatio, 1.0f);
                    // Final cap to prevent invalidating sever weapons
                    converted = Math.min(converted, 1.2f);

                    // Wilds Synergy: Wounded parts resolve through wound-adjusted hitzone data
                    MobWoundState woundState = CapabilityUtil.getMobWoundState(target);
                    if (woundState != null && woundState.isWounded()) {
                        // Wound-adjusted: use the wound system's hitzone boost instead of flat multiplier
                        float woundedBlunt = resolveHitzoneMultiplierInternal(attacker, target, MHDamageType.BLUNT);
                        float woundBonus = Math.min(woundedBlunt * 1.15f, 1.3f); // capped wound burst
                        converted = Math.max(converted, woundBonus);
                    }
                    return converted;
                }
            }
        }
        return resolveHitzoneMultiplierInternal(attacker, target, damageType);
    }

    @SuppressWarnings("null")
    private float resolveHitzoneMultiplierInternal(LivingEntity attacker, LivingEntity target, MHDamageType damageType) {
        AABB box = target.getBoundingBox();
        double height = box.getYsize();
        double headY = box.minY + (height * 0.75);
        double lowY = box.minY + (height * 0.40);

        double hitY = attacker.getEyeY();
        Vec3 toAttacker = attacker.position().subtract(target.position());
        double facingDot = target.getLookAngle().dot(toAttacker);
        boolean isBehind = facingDot < -0.25;

        if (hitY >= headY) {
            return switch (damageType) {
                case BLUNT -> 1.20f;
                case SHOT -> 1.05f;
                default -> 1.00f;
            };
        }

        if (hitY <= lowY && isBehind) {
            return switch (damageType) {
                case SEVER -> 1.10f;
                case BLUNT -> 0.90f;
                case SHOT -> 0.95f;
                default -> 1.00f;
            };
        }

        if (hitY <= lowY) {
            return switch (damageType) {
                case SEVER -> 0.90f;
                case SHOT -> 0.85f;
                default -> 1.00f;
            };
        }

        return 1.00f;
    }

    @SuppressWarnings("null")
    private void applyBowCoating(LivingEntity target, Player player, int coating) {
        int allowed = coating;
        if (coating == 1 && getAttrSafe(player, MHAttributes.COATING_POISON.get()) <= 0.0f) {
            allowed = 0;
        }
        if (coating == 2 && getAttrSafe(player, MHAttributes.COATING_PARALYSIS.get()) <= 0.0f) {
            allowed = 0;
        }
        if (coating == 3 && getAttrSafe(player, MHAttributes.COATING_SLEEP.get()) <= 0.0f) {
            allowed = 0;
        }
        switch (allowed) {
            case 1 -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            case 2 -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            case 3 -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
            default -> {
            }
        }
    }

    @SuppressWarnings("null")
    private float applySharpness(ItemStack stack, Player player, boolean criticalHit) {
        var tag = stack.getOrCreateTag();
        int baseMax = 100;
        float bonus = Math.max(0.0f, getAttrSafe(player, MHAttributes.SHARPNESS_MAX_BONUS.get()));
        int maxSharpness = baseMax + Math.round(bonus * 100.0f);
        int sharpness = tag.contains("mh_sharpness") ? tag.getInt("mh_sharpness") : maxSharpness;
        sharpness = Math.min(sharpness, maxSharpness);
        tag.putInt("mh_sharpness", sharpness);

        float modifier = 1.0f;
        if (sharpness >= 90) {
            modifier = 1.32f;
        } else if (sharpness >= 75) {
            modifier = 1.2f;
        } else if (sharpness >= 50) {
            modifier = 1.05f;
        } else if (sharpness >= 25) {
            modifier = 1.0f;
        } else if (sharpness > 0) {
            modifier = 0.75f;
        } else {
            modifier = 0.5f;
        }

        if (sharpness > 0) {
            float lossMult = 1.0f - Math.min(1.0f, Math.max(0.0f, getAttrSafe(player, MHAttributes.SHARPNESS_LOSS_MULTIPLIER.get())));
            float protectChance = Math.min(1.0f, Math.max(0.0f, getAttrSafe(player, MHAttributes.SHARPNESS_PROTECT_CHANCE.get())));
            float critProtect = Math.min(1.0f, Math.max(0.0f, getAttrSafe(player, MHAttributes.SHARPNESS_CRIT_PROTECT_CHANCE.get())));
            float totalProtect = Math.min(1.0f, protectChance + (criticalHit ? critProtect : 0.0f));
            float roll = player.getRandom().nextFloat();
            if (roll >= totalProtect) {
                int loss = Math.max(0, Math.round(1.0f * lossMult));
                if (loss > 0) {
                    sharpness = Math.max(0, sharpness - loss);
                    tag.putInt("mh_sharpness", sharpness);
                }
            }
        }
        return modifier;
    }

    private float getSharpness(ItemStack stack) {
        if (!stack.hasTag()) {
            return 100.0f;
        }
        var tag = stack.getTag();
        if (tag == null) {
            return 100.0f;
        }
        return tag.contains("mh_sharpness") ? tag.getInt("mh_sharpness") : 100.0f;
    }

    private boolean isRangedWeapon(ItemStack stack) {
        if (!(stack.getItem() instanceof WeaponIdProvider weaponIdProvider)) {
            return false;
        }
        return isRangedWeapon(weaponIdProvider.getWeaponId());
    }

    private boolean isRangedWeapon(String weaponId) {
        return "bow".equals(weaponId)
                || "bowgun".equals(weaponId)
                || "light_bowgun".equals(weaponId)
                || "heavy_bowgun".equals(weaponId);
    }

    private boolean hasOffensiveGuard(Player player) {
        return player.getPersistentData().getInt(OFFENSIVE_GUARD_TICKS) > 0;
    }

    private float resolveDrawAffinityBonus(Player player) {
        float bonus = Math.max(0.0f, getAttrSafe(player, MHAttributes.DRAW_AFFINITY_BONUS.get()));
        if (bonus <= 0.0f) {
            return 0.0f;
        }
        PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(player);
        if (combatState != null && "draw_slash".equals(combatState.getActionKey())) {
            return bonus;
        }
        var tag = player.getPersistentData();
        if (tag.getInt(DRAW_BONUS_TICKS) > 0) {
            tag.putInt(DRAW_BONUS_TICKS, 0);
            return bonus;
        }
        return 0.0f;
    }

    private void tickPlayerTimers(Player player) {
        var tag = player.getPersistentData();
        tickTimer(tag, DRAW_BONUS_TICKS);
        tickTimer(tag, OFFENSIVE_GUARD_TICKS);
    }

    @SuppressWarnings("null")
    private void tickTimer(net.minecraft.nbt.CompoundTag tag, String key) {
        int ticks = tag.getInt(key);
        if (ticks > 0) {
            tag.putInt(key, ticks - 1);
        }
    }

    private void performTonfaJetCounter(Player player, net.minecraft.world.entity.Entity attacker) {
        // Bounce player up and over
        Vec3 bounceDir;
        if (attacker != null) {
            Vec3 toPlayer = player.position().subtract(attacker.position());
            bounceDir = new Vec3(toPlayer.x, 0, toPlayer.z).normalize();
            // Fallback if directly on top
            if (bounceDir.lengthSqr() < 0.01) {
                bounceDir = player.getLookAngle().scale(-1);
            }
        } else {
             bounceDir = player.getLookAngle().scale(-1);
        }
        
        // Apply velocity: Back/Away + Up
        player.setDeltaMovement(bounceDir.x * 0.8, 1.2, bounceDir.z * 0.8);
        player.hurtMarked = true;
        
        // Reset air dash state so they can dash again? Or just let them fly.
        // Grant i-frames
        PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(player);
        if (combatState != null) {
            combatState.setDodgeIFrameTicks(15); 
        }

        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, 
                player.getX(), player.getY() + 0.5, player.getZ(), 
                2, 0.2, 0.2, 0.2, 0.0);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), 
                net.minecraft.sounds.SoundEvents.SHIELD_BLOCK, 
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.5f);
        }
    }

    private float getAttrSafe(LivingEntity entity, Attribute attr) {
        if (entity == null || attr == null) {
            return 0.0f;
        }
        var inst = entity.getAttribute(attr);
        return inst != null ? (float) inst.getValue() : 0.0f;
    }
}
