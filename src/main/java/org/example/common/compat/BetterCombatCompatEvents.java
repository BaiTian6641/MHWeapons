package org.example.common.compat;

import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BetterCombatCompatEvents {
    @SubscribeEvent
    public void onBetterCombatAnimation(BetterCombatAnimationEvent event) {
        BetterCombatAnimationBridge.onBetterCombatAttack(event.getPlayer(), event.getAnimationId());
    }
}
