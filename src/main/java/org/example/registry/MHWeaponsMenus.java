package org.example.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.example.MHWeaponsMod;
import org.example.common.menu.DecorationWorkbenchMenu;

public final class MHWeaponsMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MHWeaponsMod.MODID);

        public static final RegistryObject<MenuType<DecorationWorkbenchMenu>> DECORATION_WORKBENCH = MENUS.register(
            "decoration_workbench",
            () -> IForgeMenuType.create((id, inv, data) -> new DecorationWorkbenchMenu(id, inv))
        );

    private MHWeaponsMenus() {
    }
}