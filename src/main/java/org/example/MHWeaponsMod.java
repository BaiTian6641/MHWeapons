package org.example;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.common.MinecraftForge;
import org.example.registry.MHWeaponsItems;
import org.example.common.combat.CombatReferee;
import org.example.common.events.CommonEvents;
import org.example.common.events.CommonDataReloadEvents;
import org.example.common.events.combat.AttackMotionValueEvents;
import org.example.common.events.combat.ActionKeyEvents;
import org.example.common.events.combat.ActionKeyTickEvents;
import org.example.common.events.combat.FocusModeDamageEvents;
import org.example.common.events.combat.FocusStrikeEvents;
import org.example.common.combat.weapon.WeaponStateEvents;
import org.example.common.compat.BetterCombatEventsPlaceholder;
import org.example.common.compat.BetterCombatCompatEvents;
import org.example.common.events.WideRangeEvents;
import org.example.common.network.ModNetwork;
import org.example.common.config.MHWeaponsConfig;
import software.bernie.geckolib.GeckoLib;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(MHWeaponsMod.MODID)
public class MHWeaponsMod {
    public static final String MODID = "mhweaponsmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("removal")
    public MHWeaponsMod() {
        // GeckoLib bootstrap for animated weapon models (placeholder for future usage).
        GeckoLib.initialize();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        MHWeaponsItems.ITEMS.register(modBus);
        MHWeaponsItems.ENTITIES.register(modBus);
        org.example.registry.MHWeaponsBlocks.BLOCKS.register(modBus);
        org.example.registry.MHWeaponsBlocks.BLOCK_ITEMS.register(modBus);
        org.example.registry.MHWeaponsMenus.MENUS.register(modBus);
        org.example.registry.MHWeaponsTabs.TABS.register(modBus);
        org.example.registry.MHAttributes.register(modBus);
        modBus.addListener(this::addCreativeTabs);

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, MHWeaponsConfig.CLIENT_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MHWeaponsConfig.COMMON_SPEC);

        ModNetwork.init();
        MinecraftForge.EVENT_BUS.register(new CombatReferee());
        MinecraftForge.EVENT_BUS.register(new CommonEvents());
        MinecraftForge.EVENT_BUS.register(new org.example.common.events.DecorationAttributeEvents());
        MinecraftForge.EVENT_BUS.register(new org.example.common.events.EnvironmentalResistanceEvents());
        MinecraftForge.EVENT_BUS.register(new WideRangeEvents());
        MinecraftForge.EVENT_BUS.register(new org.example.common.events.CommandEvents());
        MinecraftForge.EVENT_BUS.register(new CommonDataReloadEvents());
        MinecraftForge.EVENT_BUS.register(new AttackMotionValueEvents());
        MinecraftForge.EVENT_BUS.register(new ActionKeyEvents());
        MinecraftForge.EVENT_BUS.register(new ActionKeyTickEvents());
        MinecraftForge.EVENT_BUS.register(new FocusModeDamageEvents());
        MinecraftForge.EVENT_BUS.register(new FocusStrikeEvents());
        MinecraftForge.EVENT_BUS.register(new WeaponStateEvents());
        MinecraftForge.EVENT_BUS.register(new org.example.common.events.combat.InsectGlaiveEvents());
        MinecraftForge.EVENT_BUS.register(new org.example.common.events.combat.TonfaCombatEvents());
        MinecraftForge.EVENT_BUS.register(new BetterCombatCompatEvents());

        BetterCombatEventsPlaceholder.register();
    }

    @SuppressWarnings("null")
    private void addCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            MHWeaponsItems.ITEMS.getEntries().forEach(entry -> event.accept(entry.get()));
            org.example.registry.MHWeaponsBlocks.BLOCK_ITEMS.getEntries().forEach(entry -> event.accept(entry.get()));
        }
    }

    // private void setup(final FMLCommonSetupEvent event) { }
}
