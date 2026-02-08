package org.example.mixin;

import net.bettercombat.api.WeaponAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = WeaponAttributes.Attack.class, remap = false)
public interface WeaponAttributesAttackAccessor {
    @Accessor("animation")
    String getAnimation();

    @Accessor("animation")
    void setAnimation(String animation);
}