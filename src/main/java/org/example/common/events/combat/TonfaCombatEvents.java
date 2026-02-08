package org.example.common.events.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.example.common.capability.mob.MobWoundState;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.util.CapabilityUtil;
import org.example.item.WeaponIdProvider;

public class TonfaCombatEvents {

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (!(player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponProvider) || !"tonfa".equals(weaponProvider.getWeaponId())) {
            return;
        }

        PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(player);
        if (combatState == null) {
            return;
        }

        // Handle Pinpoint Drill (Focus Strike)
        if ("tonfa_drill".equals(combatState.getActionKey())) {
            LivingEntity target = event.getEntity();
            MobWoundState woundState = CapabilityUtil.getMobWoundState(target);
            
            if (woundState != null && woundState.isWounded()) {
                // Destroy Wound (Wilds Mechanic: Focus Strike consumes wound for damage)
                woundState.setWounded(false);
                woundState.setWoundTicksRemaining(0);
                
                // Massive Damage Boost (Destroying a wound is a finisher)
                event.setAmount(event.getAmount() * 2.5f);
                
            } else {
                // Multi-hit drill is still strong, but standard
                event.setAmount(event.getAmount() * 1.1f);
            }
        }
    }
}
