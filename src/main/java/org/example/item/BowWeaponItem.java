package org.example.item;

import javax.annotation.Nonnull;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.combat.MHDamageType;
import org.example.common.combat.MHDamageTypeProvider;
import org.example.common.util.CapabilityUtil;

public class BowWeaponItem extends BowItem implements MHDamageTypeProvider, WeaponIdProvider {
    public BowWeaponItem(Properties properties) {
        super(properties);
    }

    @Override
    public String getWeaponId() {
        return "bow";
    }

    @Override
    public MHDamageType getDamageType(ItemStack stack) {
        return MHDamageType.SHOT;
    }

    @Override
    @SuppressWarnings("null")
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull LivingEntity entity, int timeLeft) {
        super.releaseUsing(stack, level, entity, timeLeft);
        if (!(entity instanceof Player player)) {
            return;
        }
        int chargeTicks = getUseDuration(stack) - timeLeft;
        float power = getPowerForTime(chargeTicks);
        PlayerWeaponState weaponState = CapabilityUtil.getPlayerWeaponState(player);
        if (weaponState != null) {
            weaponState.setBowCharge(power);
        }
        PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(player);
        if (combatState != null) {
            String actionKey = power >= 1.0f ? "bow_charge3" : (power >= 0.6f ? "bow_charge2" : "bow_charge1");
            combatState.setActionKey(actionKey);
            combatState.setActionKeyTicks(6);
        }
    }
}
