package org.example.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.example.common.combat.MHDamageTypeProvider;

public class WhetstoneItem extends Item {
    private static final String SHARPNESS_TAG = "mh_sharpness";
    private static final int MAX_SHARPNESS = 100;
    private static final int COOLDOWN_TICKS = 75;

    public WhetstoneItem(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("null")
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack whetstone = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(whetstone);
        }

        if (!level.isClientSide) {
            boolean sharpened = false;
            sharpened |= sharpenInventory(player);
            sharpened |= sharpenOffhand(player);
            if (!sharpened) {
                return InteractionResultHolder.pass(whetstone);
            }
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }

        return InteractionResultHolder.sidedSuccess(whetstone, level.isClientSide);
    }

    private boolean sharpenInventory(Player player) {
        boolean sharpened = false;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack target = inventory.getItem(slot);
            if (trySharpen(player, target)) {
                sharpened = true;
            }
        }
        return sharpened;
    }

    private boolean sharpenOffhand(Player player) {
        boolean sharpened = false;
        for (ItemStack target : player.getInventory().offhand) {
            if (trySharpen(player, target)) {
                sharpened = true;
            }
        }
        return sharpened;
    }

    private boolean trySharpen(Player player, ItemStack target) {
        if (target.isEmpty() || !(target.getItem() instanceof MHDamageTypeProvider)) {
            return false;
        }
        CompoundTag tag = target.getOrCreateTag();
        int current = tag.contains(SHARPNESS_TAG) ? tag.getInt(SHARPNESS_TAG) : MAX_SHARPNESS;
        if (current >= MAX_SHARPNESS) {
            return false;
        }
        int restore = Math.max(1, MAX_SHARPNESS / 4);
        int next = Math.min(MAX_SHARPNESS, current + restore);
        tag.putInt(SHARPNESS_TAG, next);
        player.getCooldowns().addCooldown(target.getItem(), COOLDOWN_TICKS);
        return true;
    }
}
