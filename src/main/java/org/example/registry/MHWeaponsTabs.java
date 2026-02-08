package org.example.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.example.MHWeaponsMod;

public final class MHWeaponsTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MHWeaponsMod.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("mhweaponsmod", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mhweaponsmod"))
                    .icon(() -> new ItemStack(MHWeaponsItems.GREATSWORD.get()))
                    .displayItems((params, output) -> {
                        MHWeaponsItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.get()));
                        MHWeaponsBlocks.BLOCK_ITEMS.getEntries().forEach(entry -> output.accept(entry.get()));
                    })
                    .build());

    private MHWeaponsTabs() {
    }
}
