package org.example.client.input;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.MHWeaponsMod;
import org.example.client.render.KinsectRenderer;
import org.example.client.render.EchoBubbleRenderer;
import org.example.registry.MHWeaponsItems;

@Mod.EventBusSubscriber(modid = MHWeaponsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
@SuppressWarnings("null")
public final class ClientModEvents {
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ClientKeybinds.FOCUS_MODE);
        event.register(ClientKeybinds.DODGE);
        event.register(ClientKeybinds.GUARD);
        event.register(ClientKeybinds.SHEATHE);
        event.register(ClientKeybinds.SPECIAL_ACTION);
        event.register(ClientKeybinds.WEAPON_ACTION);
        event.register(ClientKeybinds.WEAPON_ACTION_ALT);
        event.register(ClientKeybinds.KINSECT_LAUNCH);
        event.register(ClientKeybinds.KINSECT_RECALL);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MHWeaponsItems.KINSECT.get(), KinsectRenderer::new);
        event.registerEntityRenderer(MHWeaponsItems.ECHO_BUBBLE.get(), EchoBubbleRenderer::new);
    }
}
