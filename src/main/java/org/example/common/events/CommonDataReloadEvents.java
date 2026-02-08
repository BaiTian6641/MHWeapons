package org.example.common.events;

import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.example.common.data.AccessoryDataManager;
import org.example.common.data.DecorationDataManager;
import org.example.common.data.GearDecorationDataManager;
import org.example.common.data.compat.BetterCombatAnimationMapManager;
import org.example.common.data.RulesetDataManager;
import org.example.common.data.WeaponDataManager;

public final class CommonDataReloadEvents {
    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(AccessoryDataManager.INSTANCE);
        event.addListener(DecorationDataManager.INSTANCE);
        event.addListener(GearDecorationDataManager.INSTANCE);
        event.addListener(WeaponDataManager.INSTANCE);
        event.addListener(RulesetDataManager.INSTANCE);
        event.addListener(BetterCombatAnimationMapManager.INSTANCE);
    }
}
