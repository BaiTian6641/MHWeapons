package org.example.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.example.MHWeaponsMod;
import org.example.block.DecorationWorkbenchBlock;

@SuppressWarnings("null")
public final class MHWeaponsBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MHWeaponsMod.MODID);

    public static final RegistryObject<Block> DECORATION_WORKBENCH = BLOCKS.register("decoration_workbench",
            () -> new DecorationWorkbenchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5f).sound(SoundType.METAL)));

    public static final DeferredRegister<Item> BLOCK_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MHWeaponsMod.MODID);

    public static final RegistryObject<Item> DECORATION_WORKBENCH_ITEM = BLOCK_ITEMS.register("decoration_workbench",
            () -> new BlockItem(DECORATION_WORKBENCH.get(), new Item.Properties()));

    private MHWeaponsBlocks() {
    }
}