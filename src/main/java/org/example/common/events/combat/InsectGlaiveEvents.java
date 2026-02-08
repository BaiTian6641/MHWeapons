package org.example.common.events.combat;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.util.CapabilityUtil;
import org.example.item.WeaponIdProvider;

import java.util.UUID;

public class InsectGlaiveEvents {
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("d8f3b2a1-5c6e-4f7a-9b8c-1d2e3f4a5b6c");
    private static final UUID ATTACK_MODIFIER_UUID = UUID.fromString("e9a4c3b2-6d7e-5f8a-0c9d-2e3f4a5b6c7d");
    private static final UUID DEFENSE_MODIFIER_UUID = UUID.fromString("f0b5d4c3-7e8f-6a9b-1d0e-3f4a5b6c7d8e");

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;

        PlayerWeaponState state = CapabilityUtil.getPlayerWeaponState(player);
        if (state == null) return;

        // Tick down aerial state
        if (state.getInsectAerialTicks() > 0) {
            state.setInsectAerialTicks(state.getInsectAerialTicks() - 1);
            if (state.getInsectAerialTicks() > 0 && !player.onGround()) {
                player.fallDistance = 0; // Prevent fall damage while in aerial state
            }
        }

        // Tick down extracts
        if (state.getInsectExtractTicks() > 0) {
            state.setInsectExtractTicks(state.getInsectExtractTicks() - 1);
            if (state.getInsectExtractTicks() == 0) {
                state.setInsectRed(false);
                state.setInsectWhite(false);
                state.setInsectOrange(false);
            }
        }

        // Apply Buffs
        boolean isGlaive = player.getMainHandItem().getItem() instanceof WeaponIdProvider p && "insect_glaive".equals(p.getWeaponId());
        
        // Speed (White)
        var speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_MODIFIER_UUID);
            if (state.isInsectWhite() && isGlaive) {
                speedAttr.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_UUID, "IG Speed", 0.2, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }

        // Attack (Red + White or Triple)
        // Note: Better Combat handles damage via motion values, but we can add raw attack for the buff
        var attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.removeModifier(ATTACK_MODIFIER_UUID);
            if (state.isInsectRed() && state.isInsectWhite() && isGlaive) {
                double boost = (state.isInsectOrange()) ? 0.15 : 0.10; // Triple up gives more
                attackAttr.addTransientModifier(new AttributeModifier(ATTACK_MODIFIER_UUID, "IG Attack", boost, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }

        // Defense (Orange)
        var armorAttr = player.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.removeModifier(DEFENSE_MODIFIER_UUID);
            if (state.isInsectOrange() && isGlaive) {
                armorAttr.addTransientModifier(new AttributeModifier(DEFENSE_MODIFIER_UUID, "IG Defense", 10.0, AttributeModifier.Operation.ADDITION));
            }
        }
    }
}
