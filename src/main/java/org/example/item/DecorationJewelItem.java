package org.example.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class DecorationJewelItem extends Item {
    public DecorationJewelItem(Properties properties) {
        super(properties);
    }

    public ResourceLocation getDecorationId(ItemStack stack) {
        return ForgeRegistries.ITEMS.getKey(this);
    }
}