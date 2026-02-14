package org.example.common.events.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.combat.weapon.HammerHandler;
import org.example.common.compat.BetterCombatAnimationBridge;
import org.example.common.network.ModNetwork;
import org.example.common.network.packet.PlayerWeaponStateS2CPacket;
import org.example.common.util.CapabilityUtil;
import net.minecraftforge.network.PacketDistributor;
import org.example.item.WeaponIdProvider;

public final class ActionKeyTickEvents {
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        PlayerCombatState state = CapabilityUtil.getPlayerCombatState(event.player);
        if (state == null) {
            return;
        }
        int remaining = state.getActionKeyTicks();
        if (remaining <= 0) {
            return;
        }
        remaining -= 1;
        if (remaining <= 0) {
            state.setActionKey(null);
            state.setActionKeyTicks(0);
            return;
        }
        state.setActionKeyTicks(remaining);

        PlayerWeaponState weaponState = CapabilityUtil.getPlayerWeaponState(event.player);
        if (weaponState != null && weaponState.getMagnetZipAnimTicks() > 0) {
            int zipRemaining = weaponState.getMagnetZipAnimTicks() - 1;
            weaponState.setMagnetZipAnimTicks(zipRemaining);
            if (zipRemaining <= 0) {
                BetterCombatAnimationBridge.updateMagnetSpikeZipAnimation(event.player, false);
                if (event.player instanceof ServerPlayer serverPlayer) {
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new PlayerWeaponStateS2CPacket(serverPlayer.getId(), weaponState.serializeNBT()));
                    weaponState.clearDirty();
                }
            }
        }

        if (weaponState != null && weaponState.getMagnetZipCooldownTicks() > 0) {
            weaponState.setMagnetZipCooldownTicks(weaponState.getMagnetZipCooldownTicks() - 1);
        }

        if (weaponState != null && weaponState.getMagnetTargetTicks() > 0) {
            weaponState.setMagnetTargetTicks(weaponState.getMagnetTargetTicks() - 1);
            if (weaponState.getMagnetTargetTicks() <= 0) {
                weaponState.setMagnetTargetId(-1);
                if (event.player instanceof ServerPlayer serverPlayer) {
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new PlayerWeaponStateS2CPacket(serverPlayer.getId(), weaponState.serializeNBT()));
                    weaponState.clearDirty();
                }
            }
        }

        if (weaponState != null && weaponState.isChargingAttack()) {
            int maxCharge = org.example.common.data.WeaponDataResolver.resolveInt(event.player, null, "chargeMaxTicks", 40);
            int next = Math.min(maxCharge, weaponState.getChargeAttackTicks() + 1);
            weaponState.setChargeAttackTicks(next);
            if (event.player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider
                    && "longsword".equals(weaponIdProvider.getWeaponId())) {
                if ("spirit_helm_breaker".equals(state.getActionKey())) {
                    return;
                }
                
                if (next < 5) return; // Prevent short taps from entering charge logic

                int stage = resolveLongSwordChargeStage(next, maxCharge);
                String key = switch (stage) {
                    case 1 -> "spirit_blade_1";
                    case 2 -> "spirit_blade_2";
                    case 3 -> "spirit_blade_3";
                    case 4 -> "spirit_roundslash";
                    default -> "spirit_charge";
                };
                state.setActionKey(key);
                state.setActionKeyTicks(2);
                
                if (maxCharge > 0 && next > 10) { // Only build gauge if actually charging (past tap threshold)
                    weaponState.addSpiritGauge(20.0f / maxCharge); // Reduced speed (was 100.0f)
                }
                
            } else if (event.player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider
                    && "magnet_spike".equals(weaponIdProvider.getWeaponId())
                    && weaponState.isMagnetSpikeImpactMode()) {
                int stage = resolveMagnetChargeStage(next, maxCharge);
                String key = switch (stage) {
                    case 1 -> "impact_charge_i";
                    case 2 -> "impact_charge_ii";
                    case 3 -> "impact_charge_iii";
                    default -> "charge_start";
                };
                state.setActionKey(key);
                state.setActionKeyTicks(2);
            } else if (event.player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider
                    && "hammer".equals(weaponIdProvider.getWeaponId())) {
                // Drain stamina while charging (MH canon: charging expends stamina)
                if (weaponState.getStamina() <= 0.0f) {
                    // Out of stamina — force release
                    weaponState.setChargingAttack(false);
                    int chargeTicks = weaponState.getChargeAttackTicks();
                    weaponState.setChargeAttackTicks(0);
                    HammerHandler.handleChargeRelease(event.player,
                            state, weaponState, chargeTicks, maxCharge);
                    if (event.player instanceof ServerPlayer sp) {
                        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                                new PlayerWeaponStateS2CPacket(sp.getId(), weaponState.serializeNBT()));
                        weaponState.clearDirty();
                    }
                    return;
                }
                weaponState.addStamina(-0.5f); // Slow drain: ~10 stamina per second
                weaponState.setStaminaRecoveryDelay(10);

                int stage = resolveHammerChargeStage(next, maxCharge);
                String key = switch (stage) {
                    case 1 -> "hammer_charge_1";
                    case 2 -> "hammer_charge_2";
                    case 3 -> "hammer_charge_3";
                    default -> "charge_start";
                };
                state.setActionKey(key);
                state.setActionKeyTicks(2);

                // Sound cue on level transition
                int prevStage = resolveHammerChargeStage(next - 1, maxCharge);
                if (stage > prevStage && stage > 0 && event.player instanceof ServerPlayer sp) {
                    float pitch = 0.8f + stage * 0.2f;
                    sp.playNotifySound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, pitch);
                }
            }
        }
    }

    private static int resolveMagnetChargeStage(int chargeTicks, int maxCharge) {
        if (maxCharge <= 0) {
            return 0;
        }
        if (chargeTicks >= maxCharge) {
            return 3;
        }
        if (chargeTicks >= (maxCharge * 2 / 3)) {
            return 2;
        }
        if (chargeTicks >= (maxCharge / 3)) {
            return 1;
        }
        return 0;
    }

    private static int resolveHammerChargeStage(int chargeTicks, int maxCharge) {
        if (maxCharge <= 0) return 0;
        if (chargeTicks >= maxCharge) return 3;
        if (chargeTicks >= (maxCharge * 2 / 3)) return 2;
        if (chargeTicks >= (maxCharge / 3)) return 1;
        return 0;
    }

    private static int resolveLongSwordChargeStage(int chargeTicks, int maxCharge) {
        if (maxCharge <= 0) {
            return 0;
        }
        if (chargeTicks >= maxCharge) {
            return 4;
        }
        if (chargeTicks >= (maxCharge * 2 / 3)) {
            return 3;
        }
        if (chargeTicks >= (maxCharge / 3)) {
            return 2;
        }
        return 1;
    }
}
