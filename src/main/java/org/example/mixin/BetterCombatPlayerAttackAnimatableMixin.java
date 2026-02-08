package org.example.mixin;

import net.bettercombat.logic.AnimatedHand;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import org.example.MHWeaponsMod;
import org.example.common.compat.BetterCombatAnimationBridge;
import org.example.item.WeaponIdProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractClientPlayer.class)
public abstract class BetterCombatPlayerAttackAnimatableMixin {
    static {
        MHWeaponsMod.LOGGER.info("BC mixin loaded: BetterCombatPlayerAttackAnimatableMixin");
    }

    @Inject(
            method = "playAttackAnimation(Ljava/lang/String;Lnet/bettercombat/logic/AnimatedHand;FF)V",
            at = @At("HEAD"),
            remap = false
    )
    private void mhweapons$logAttackAnimation(String animationName, AnimatedHand hand, float length, float upswing, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        var item = player.getMainHandItem().getItem();
        String weaponId = item instanceof WeaponIdProvider weaponIdProvider ? weaponIdProvider.getWeaponId() : "unknown";
        MHWeaponsMod.LOGGER.info(
                "BC playAttackAnimation (raw) player={} weapon={} anim={} hand={} length={} upswing={}",
                player.getId(),
                weaponId,
                animationName,
                hand,
                length,
                upswing
        );
    }

    @ModifyVariable(
            method = "playAttackAnimation(Ljava/lang/String;Lnet/bettercombat/logic/AnimatedHand;FF)V",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true,
            remap = false
    )
    private String mhweapons$swapMagnetSpikeAnimation(String animationName) {
        Player player = (Player) (Object) this;
        String resolved = BetterCombatAnimationBridge.resolveComboAnimationClient(player, animationName);
        var item = player.getMainHandItem().getItem();
        String weaponId = item instanceof WeaponIdProvider weaponIdProvider ? weaponIdProvider.getWeaponId() : "unknown";
        if (!resolved.equals(animationName)) {
            MHWeaponsMod.LOGGER.info(
                    "BC playAttackAnimation swap player={} weapon={} from={} to={}",
                    player.getId(),
                    weaponId,
                    animationName,
                    resolved
            );
        }
        BetterCombatAnimationBridge.onBetterCombatAttack(player, resolved);
        return resolved;
    }
}