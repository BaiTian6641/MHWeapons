package org.example.common.events.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.combat.FocusModeHelper;
import org.example.common.util.CapabilityUtil;

public final class FocusModeDamageEvents {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        PlayerCombatState state = CapabilityUtil.getPlayerCombatState(player);
        if (state == null || !state.isFocusMode()) {
            return;
        }
        LivingEntity target = event.getEntity();
        FocusModeHelper.RangeTier tier = FocusModeHelper.classify(player, target);
        float bonus = FocusModeHelper.damageBonus(tier);
        if (bonus <= 0.0f) {
            return;
        }
        event.setAmount(event.getAmount() * (1.0f + bonus));
    }
}
