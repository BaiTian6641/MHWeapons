package org.example.common.combat;

import net.minecraft.world.item.ItemStack;

public interface MHDamageTypeProvider {
    MHDamageType getDamageType(ItemStack stack);
}
