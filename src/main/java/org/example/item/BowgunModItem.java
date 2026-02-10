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
 * Bowgun modification item. Installed into a BowgunItem via the
 * Bowgun Workbench to change weight, stats, and capabilities.
 */
public class BowgunModItem extends Item {
    private static final Logger LOG = LogUtils.getLogger();

    private final String modId;
    private final String category; // frame, barrel, stock, magazine, shield, special
    private final int weight;
    private final String description;

    public BowgunModItem(Properties properties, String modId, String category, int weight, String description) {
        super(properties.stacksTo(1));
        this.modId = modId;
        this.category = category;
        this.weight = weight;
        this.description = description;
        LOG.debug("[Bowgun] BowgunModItem registered: {} cat={} wt={}", modId, category, weight);
    }

    public String getModId() {
        return modId;
    }

    public String getCategory() {
        return category;
    }

    public int getModWeight() {
        return weight;
    }

    public String getModDescription() {
        return description;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§7Category: §e" + category));
        int w = weight;
        String sign = w >= 0 ? "+" : "";
        tooltip.add(Component.literal("§7Weight: §f" + sign + w));
        if (description != null && !description.isEmpty()) {
            tooltip.add(Component.literal("§8" + description));
        }
    }
}
