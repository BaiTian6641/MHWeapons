package org.example.mixin;

import net.bettercombat.client.ClientNetwork;
import net.bettercombat.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientNetwork.class)
public abstract class BetterCombatClientNetworkMixin {
    static {
        org.example.MHWeaponsMod.LOGGER.info("BC mixin loaded: BetterCombatClientNetworkMixin");
    }

    @Inject(
            method = "lambda$initializeHandlers$0(Lnet/minecraft/client/Minecraft;Lnet/bettercombat/network/Packets$AttackAnimation;)V",
            at = @At("HEAD")
    )
    private static void mhweapons$onHandleAttackAnimation(Minecraft minecraft, Packets.AttackAnimation packet, CallbackInfo ci) {
        var level = minecraft.level;
        if (level == null) {
            return;
        }
        Entity entity = level.getEntity(packet.playerId());
        if (entity instanceof Player player) {
            // Avoid advancing combo state from packet handling; playAttackAnimation mixin will resolve combos.
            var item = player.getMainHandItem().getItem();
            String weaponId = item instanceof org.example.item.WeaponIdProvider weaponIdProvider
                    ? weaponIdProvider.getWeaponId()
                    : "unknown";
            org.example.MHWeaponsMod.LOGGER.info(
                    "BC packet recv player={} weapon={} anim={}",
                    player.getId(),
                    weaponId,
                    packet.animationName()
            );
        }
    }
}
