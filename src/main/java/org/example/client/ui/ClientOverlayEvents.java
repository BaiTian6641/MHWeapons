package org.example.client.ui;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.MHWeaponsMod;

@Mod.EventBusSubscriber(modid = MHWeaponsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientOverlayEvents {
    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("mhweaponsmod_weapon_hud", WeaponHudOverlay.OVERLAY);
        event.registerAboveAll("mhweaponsmod_ammo_select", (gui, guiGraphics, partialTick, width, height) -> {
            AmmoSelectOverlay.render(guiGraphics, partialTick);
        });
    }

    private ClientOverlayEvents() {
    }
}