package org.example.common.events.combat;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.entity.KinsectPowderCloudEntity;
import org.example.common.util.CapabilityUtil;
import org.example.item.WeaponIdProvider;

import java.util.List;
import java.util.UUID;

public class InsectGlaiveEvents {
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("d8f3b2a1-5c6e-4f7a-9b8c-1d2e3f4a5b6c");
    private static final UUID ATTACK_MODIFIER_UUID = UUID.fromString("e9a4c3b2-6d7e-5f8a-0c9d-2e3f4a5b6c7d");
    private static final UUID DEFENSE_MODIFIER_UUID = UUID.fromString("f0b5d4c3-7e8f-6a9b-1d0e-3f4a5b6c7d8e");
    private static final UUID KNOCKBACK_MODIFIER_UUID = UUID.fromString("a1b2c3d4-5e6f-7a8b-9c0d-1e2f3a4b5c6d");

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;

        PlayerWeaponState state = CapabilityUtil.getPlayerWeaponState(player);
        if (state == null) return;

        boolean isGlaive = player.getMainHandItem().getItem() instanceof WeaponIdProvider p && "insect_glaive".equals(p.getWeaponId());

        // Tick down aerial state
        if (state.getInsectAerialTicks() > 0) {
            state.setInsectAerialTicks(state.getInsectAerialTicks() - 1);
            if (state.getInsectAerialTicks() > 0 && !player.onGround()) {
                player.fallDistance = 0; // Prevent fall damage while in aerial state
            }
        }

        // Reset aerial bounce level when player lands
        if (player.onGround() && state.getInsectAerialBounceLevel() > 0) {
            state.setInsectAerialBounceLevel(0);
        }

        // Tick down charge (charge builds UP via insectChargeTicks, capped at 40)
        if (state.isInsectCharging() && isGlaive && state.isInsectRed()) {
            state.setInsectChargeTicks(Math.min(state.getInsectChargeTicks() + 1, 40));
        } else if (state.isInsectCharging()) {
            // Lost red extract or swapped weapon while charging — cancel
            state.setInsectCharging(false);
            state.setInsectChargeTicks(0);
        }

        // Tick down triple finisher window
        if (state.getInsectTripleFinisherTicks() > 0) {
            state.setInsectTripleFinisherTicks(state.getInsectTripleFinisherTicks() - 1);
            if (state.getInsectTripleFinisherTicks() == 0) {
                state.setInsectTripleFinisherStage(0);
            }
        }

        // Tick down extracts
        if (state.getInsectExtractTicks() > 0) {
            state.setInsectExtractTicks(state.getInsectExtractTicks() - 1);
            if (state.getInsectExtractTicks() == 0) {
                state.setInsectRed(false);
                state.setInsectWhite(false);
                state.setInsectOrange(false);
                // Cancel charge if extracts expire
                state.setInsectCharging(false);
                state.setInsectChargeTicks(0);
            }
        }

        // Tick down white jump boost cooldown
        if (player.onGround()) {
            if (state.getInsectWhiteJumpBoostCooldown() != 0) {
                state.setInsectWhiteJumpBoostCooldown(0);
            }
        } else if (state.getInsectWhiteJumpBoostCooldown() > 0) {
            state.setInsectWhiteJumpBoostCooldown(state.getInsectWhiteJumpBoostCooldown() - 1);
        }

        // Tick down mark target timer
        if (state.getKinsectMarkedTicks() > 0) {
            state.setKinsectMarkedTicks(state.getKinsectMarkedTicks() - 1);
            if (state.getKinsectMarkedTicks() == 0) {
                state.setKinsectMarkedTargetId(-1);
            }
        }

        // Detonate nearby powder clouds when player attacks (action key is set)
        if (isGlaive) {
            var combatState = CapabilityUtil.getPlayerCombatState(player);
            if (combatState != null && combatState.getActionKeyTicks() > 0) {
                String actionKey = combatState.getActionKey();
                // Only detonate on actual attacks, not utility actions
                if (actionKey != null && !actionKey.equals("kinsect_harvest") && !actionKey.equals("kinsect_recall")
                        && !actionKey.equals("kinsect_mark") && !actionKey.equals("vault")) {
                    // Check if this is the first tick of the action (actionKeyTicks just set)
                    if (combatState.getActionKeyTicks() >= 8) { // Most attacks set 10-24 ticks; check near start
                        detonatePowderCloudsNearby(player);
                    }
                }
            }
        }

        // ——— Apply Attribute Modifiers ———

        boolean hasRed = state.isInsectRed() && isGlaive;
        boolean hasWhite = state.isInsectWhite() && isGlaive;
        boolean hasOrange = state.isInsectOrange() && isGlaive;
        boolean tripleUp = hasRed && hasWhite && hasOrange;

        // Speed (White): +20% movement speed
        var speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_MODIFIER_UUID);
            if (hasWhite) {
                speedAttr.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_UUID, "IG Speed", 0.2, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }

        // Jump Height (White): boost via regular jump velocity
        // Forge 1.20 doesn't have JUMP_STRENGTH attribute, so we apply a velocity boost
        // when the player is jumping (just left ground, positive Y velocity, on-ground was true last tick)
        if (hasWhite && !player.onGround() && state.getInsectWhiteJumpBoostCooldown() == 0
                && player.getDeltaMovement().y > 0.38
                && player.getDeltaMovement().y < 0.50
                && state.getInsectAerialTicks() <= 0) {
            // Boost normal jumps by ~25%
            player.setDeltaMovement(player.getDeltaMovement().add(0, 0.1, 0));
            player.hurtMarked = true;
            state.setInsectWhiteJumpBoostCooldown(8);
        }

        // Attack (Red, Red+White, Triple Up)
        var attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.removeModifier(ATTACK_MODIFIER_UUID);
            if (hasRed) {
                double boost;
                if (tripleUp) {
                    boost = 0.15; // Triple Up: +15% attack
                } else if (hasWhite) {
                    boost = 0.10; // Red + White: +10% attack
                } else {
                    boost = 0.05; // Red only: +5% attack (enables charge mechanic as primary benefit)
                }
                attackAttr.addTransientModifier(new AttributeModifier(ATTACK_MODIFIER_UUID, "IG Attack", boost, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }

        // Defense (Orange): +10 armor. White+Orange or Triple: +15 armor
        var armorAttr = player.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.removeModifier(DEFENSE_MODIFIER_UUID);
            if (hasOrange) {
                double armorBonus = (hasWhite || tripleUp) ? 15.0 : 10.0;
                armorAttr.addTransientModifier(new AttributeModifier(DEFENSE_MODIFIER_UUID, "IG Defense", armorBonus, AttributeModifier.Operation.ADDITION));
            }
        }

        // Flinch Free (Orange): Knockback resistance
        var knockbackAttr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackAttr != null) {
            knockbackAttr.removeModifier(KNOCKBACK_MODIFIER_UUID);
            if (hasOrange) {
                double kbResist = tripleUp ? 1.0 : 0.5; // Triple Up = full flinch free
                knockbackAttr.addTransientModifier(new AttributeModifier(KNOCKBACK_MODIFIER_UUID, "IG Flinch Free", kbResist, AttributeModifier.Operation.ADDITION));
            }
        }
    }

    /**
     * Detonate all Kinsect Powder Clouds within attack range of the player.
     */
    private void detonatePowderCloudsNearby(Player player) {
        double range = 5.0;
        AABB searchBox = player.getBoundingBox().inflate(range);
        List<KinsectPowderCloudEntity> clouds = player.level().getEntitiesOfClass(
                KinsectPowderCloudEntity.class, searchBox,
                cloud -> cloud.isInDetonationRange(player));
        for (KinsectPowderCloudEntity cloud : clouds) {
            cloud.detonate(player);
        }
    }
}
