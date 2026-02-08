package org.example.mixin;

import net.bettercombat.network.Packets.AttackAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AttackAnimation.class)
public interface AttackAnimationPacketAccessor {
    @Accessor("animationName")
    String getAnimationName();

    @Accessor("animationName")
    void setAnimationName(String animationName);
}
