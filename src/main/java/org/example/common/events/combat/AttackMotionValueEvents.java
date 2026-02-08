package org.example.common.events.combat;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.example.common.combat.action.ActionMotionValue;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.data.WeaponDataResolver;
import org.example.common.util.CapabilityUtil;

public final class AttackMotionValueEvents {
    private static final String DEFAULT_ACTION = "basic_attack";

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        PlayerCombatState state = CapabilityUtil.getPlayerCombatState(player);
        String actionKey = state != null && state.getActionKey() != null ? state.getActionKey() : DEFAULT_ACTION;
        float motionValue = WeaponDataResolver.resolveMotionValue(player, actionKey, 1.0f);
        ActionMotionValue.set(player, motionValue);
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            ActionMotionValue.clear(player);
            PlayerCombatState state = CapabilityUtil.getPlayerCombatState(player);
            if (state != null) {
                state.setActionKey(null);
            }
        }
    }
}
