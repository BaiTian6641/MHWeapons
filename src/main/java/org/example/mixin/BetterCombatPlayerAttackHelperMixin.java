package org.example.mixin;

import net.bettercombat.api.AttackHand;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.world.entity.player.Player;
import org.example.MHWeaponsMod;
import org.example.common.compat.BetterCombatAnimationBridge;
import org.example.item.WeaponIdProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerAttackHelper.class, remap = false)
public abstract class BetterCombatPlayerAttackHelperMixin {
    @Inject(method = "getCurrentAttack", at = @At("RETURN"), remap = false)
    private static void mhweapons$swapMagnetSpikeAttack(Player player, int comboCount, CallbackInfoReturnable<AttackHand> cir) {
        if (player.level().isClientSide) {
            return;
        }
        AttackHand hand = cir.getReturnValue();
        if (hand == null || hand.itemStack() == null) {
            return;
        }
        WeaponAttributes.Attack attack = hand.attack();
        if (attack == null) {
            return;
        }
        WeaponAttributesAttackAccessor accessor = (WeaponAttributesAttackAccessor) (Object) attack;
        String original = accessor.getAnimation();
        String targetAnim = BetterCombatAnimationBridge.resolveComboAnimationServer(player, comboCount, original);
        if (!targetAnim.equals(original)) {
            accessor.setAnimation(targetAnim);
            var item = hand.itemStack().getItem();
            String weaponId = item instanceof WeaponIdProvider weaponIdProvider ? weaponIdProvider.getWeaponId() : "unknown";
            MHWeaponsMod.LOGGER.info("BC swap (getCurrentAttack) weapon={} combo={} from={} to={}", weaponId, comboCount, original, targetAnim);
        }
    }
}