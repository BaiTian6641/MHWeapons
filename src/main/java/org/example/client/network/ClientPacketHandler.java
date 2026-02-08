package org.example.client.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.logic.AnimatedHand;
import net.minecraft.world.entity.player.Player;
import org.example.client.compat.MagnetSpikeZipClientAnimationTracker;
import org.example.client.fx.DamageNumberClientTracker;
import org.example.MHWeaponsMod;
import org.example.common.network.packet.DamageNumberS2CPacket;
import org.example.common.network.packet.MagnetSpikeZipAnimationS2CPacket;
import org.example.common.network.packet.PlayAttackAnimationS2CPacket;
import org.example.common.data.WeaponDataResolver;
import org.example.common.capability.mob.MobWoundState;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.compat.BetterCombatAnimationBridge;
import org.example.common.network.packet.PlayerWeaponStateS2CPacket;
import org.example.common.network.packet.WoundStateS2CPacket;
import org.example.common.util.CapabilityUtil;

public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void handleWoundState(WoundStateS2CPacket packet) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Entity entity = level.getEntity(packet.entityId());
        if (entity instanceof LivingEntity livingEntity) {
            MobWoundState state = CapabilityUtil.getMobWoundState(livingEntity);
            if (state != null) {
                state.setWounded(packet.wounded());
                state.setWoundTicksRemaining(packet.woundTicks());
            }
        }
    }

    public static void handleWeaponState(PlayerWeaponStateS2CPacket packet) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null || player.getId() != packet.playerId()) {
            return;
        }
        PlayerWeaponState state = CapabilityUtil.getPlayerWeaponState(player);
        if (state != null && packet.data() != null) {
            state.deserializeNBT(packet.data());
            if (MagnetSpikeZipClientAnimationTracker.isActive(player.getId()) || state.getMagnetZipAnimTicks() > 0) {
                BetterCombatAnimationBridge.updateMagnetSpikeZipAnimation(player, true);
            } else {
                BetterCombatAnimationBridge.updateMagnetSpikeAttackAnimation(player, state.isMagnetSpikeImpactMode());
            }
        }
    }

    public static void handleMagnetZipAnimation(MagnetSpikeZipAnimationS2CPacket packet) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        var entity = level.getEntity(packet.playerId());
        if (entity instanceof PlayerAttackAnimatable animatable) {
            int zipAnimTicks = WeaponDataResolver.resolveInt((Player) entity, "magnetZip", "animTicks", 22);
            MagnetSpikeZipClientAnimationTracker.start(packet.playerId(), zipAnimTicks);
            BetterCombatAnimationBridge.updateMagnetSpikeZipAnimation((Player) entity, true);
            animatable.playAttackAnimation("bettercombat:two_handed_spin", AnimatedHand.TWO_HANDED, 1.5f, 0.6f);
            MHWeaponsMod.LOGGER.info("BC play magnet_spike zip animation");
        }
    }

    public static void handlePlayAttackAnimation(PlayAttackAnimationS2CPacket packet) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        var entity = level.getEntity(packet.playerId());
        if (entity instanceof PlayerAttackAnimatable animatable) {
            if (entity instanceof Player player) {
                int durationTicks = Math.max(6, (int) Math.ceil(packet.length()));
                BetterCombatAnimationBridge.registerForcedAnimation(player, packet.animationId(), durationTicks, packet.speed());
                if (packet.actionKey() != null && !packet.actionKey().isBlank()) {
                    PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(player);
                    if (combatState != null) {
                        combatState.setActionKey(packet.actionKey());
                        combatState.setActionKeyTicks(Math.max(1, packet.actionKeyTicks()));
                    }
                }
            }
            animatable.playAttackAnimation(packet.animationId(), AnimatedHand.TWO_HANDED, packet.length(), packet.upswing());
            MHWeaponsMod.LOGGER.info("BC play forced animation {} for player {}", packet.animationId(), packet.playerId());
        }
    }

    public static void handleDamageNumber(DamageNumberS2CPacket packet) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        DamageNumberClientTracker.spawn(packet.targetId(), packet.damage(), packet.colorType(), packet.frameType());
    }
}
