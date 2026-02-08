package org.example.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import javax.annotation.Nonnull;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.util.CapabilityUtil;
import org.example.common.combat.MHDamageType;

public class GreatSwordItem extends GeoWeaponItem {
    private static final int CHARGE_LEVEL_1 = 20;
    private static final int CHARGE_LEVEL_2 = 40;
    private static final int CHARGE_LEVEL_3 = 60;

    public GreatSwordItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Item.Properties properties) {
        super("greatsword", MHDamageType.SEVER, tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    @Override
    public int getUseDuration(@Nonnull ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(@Nonnull ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    @SuppressWarnings("null")
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }
        int chargeTicks = getUseDuration(stack) - timeLeft;
        String actionKey = resolveChargeActionKey(chargeTicks);
        PlayerCombatState state = CapabilityUtil.getPlayerCombatState(player);
        if (state != null) {
            state.setActionKey(actionKey);
            state.setActionKeyTicks(20);
        }
    }

    @Override
    @SuppressWarnings("null")
    public void onUseTick(@Nonnull Level level, @Nonnull LivingEntity entity, @Nonnull ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof Player player)) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 1, false, false));
    }

    private String resolveChargeActionKey(int chargeTicks) {
        if (chargeTicks >= CHARGE_LEVEL_3) {
            return "tcs_hit2";
        }
        if (chargeTicks >= CHARGE_LEVEL_2) {
            return "strong_charge";
        }
        if (chargeTicks >= CHARGE_LEVEL_1) {
            return "draw_slash";
        }
        return "basic_attack";
    }
}
