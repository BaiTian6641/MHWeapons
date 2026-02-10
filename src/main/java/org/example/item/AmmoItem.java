package org.example.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Bowgun ammo item. Each ammo type is a separate registered item.
 * Stackable – carried in inventory and consumed on fire/reload.
 */
public class AmmoItem extends Item {
    private static final Logger LOG = LogUtils.getLogger();

    private final String ammoTypeId;
    private final int maxStackOverride;

    public AmmoItem(Properties properties, String ammoTypeId, int maxStack) {
        super(properties.stacksTo(maxStack));
        this.ammoTypeId = ammoTypeId;
        this.maxStackOverride = maxStack;
        LOG.debug("[Bowgun] AmmoItem registered: {}", ammoTypeId);
    }

    public AmmoItem(Properties properties, String ammoTypeId) {
        this(properties, ammoTypeId, 99);
    }

    public String getAmmoTypeId() {
        return ammoTypeId;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§7Ammo Type: §f" + ammoTypeId));
    }
}
