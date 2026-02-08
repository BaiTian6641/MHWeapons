package org.example.common.events.combat;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.util.CapabilityUtil;

public final class FocusStrikeEvents {
    private static final String FOCUS_STRIKE_KEY = "focus_strike";

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        PlayerCombatState state = CapabilityUtil.getPlayerCombatState(player);
        if (state == null) {
            return;
        }
        if (!state.isFocusMode()) {
            return;
        }
        state.setActionKey(FOCUS_STRIKE_KEY);
        state.setActionKeyTicks(10);
    }
}
