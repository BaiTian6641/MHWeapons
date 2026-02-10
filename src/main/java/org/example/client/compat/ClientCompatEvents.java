package org.example.client.compat;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.example.MHWeaponsMod;
import net.minecraft.client.gui.screens.MenuScreens;
import org.example.client.ui.DecorationWorkbenchScreen;
import org.example.client.ui.BowgunWorkbenchScreen;
import org.example.registry.MHWeaponsMenus;

@Mod.EventBusSubscriber(modid = MHWeaponsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientCompatEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            BetterCombatClientAdapter.register();
            MenuScreens.register(MHWeaponsMenus.DECORATION_WORKBENCH.get(), DecorationWorkbenchScreen::new);
            MenuScreens.register(MHWeaponsMenus.BOWGUN_WORKBENCH.get(), BowgunWorkbenchScreen::new);
        });
    }
}
