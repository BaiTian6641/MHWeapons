package org.example.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.example.common.data.AccessoryData;
import org.example.common.data.AccessoryDataManager;
import org.example.common.data.DecorationData;
import org.example.common.data.DecorationDataManager;
import org.example.common.util.DecorationUtil;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.UUID;

public class AccessoryItem extends Item implements ICurioItem {
    public AccessoryItem(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("null")
    public void onCraftedBy(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player) {
        super.onCraftedBy(stack, level, player);
        DecorationUtil.ensureDefaultSlots(stack);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        HashMultimap<Attribute, AttributeModifier> map = HashMultimap.create();
        var id = ForgeRegistries.ITEMS.getKey(this);
        if (id == null) {
            return map;
        }
        AccessoryData data = AccessoryDataManager.INSTANCE.get(id);
        if (data == null) {
            return map;
        }
        map.putAll(data.buildModifiers(uuid));
        var decos = DecorationUtil.getDecorations(stack);
        for (var deco : decos) {
            DecorationData decoData = DecorationDataManager.INSTANCE.get(deco.id());
            if (decoData != null) {
                map.putAll(decoData.buildModifiers(uuid));
            }
        }
        return map;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        var id = ForgeRegistries.ITEMS.getKey(this);
        if (id == null) {
            return true;
        }
        AccessoryData data = AccessoryDataManager.INSTANCE.get(id);
        if (data == null) {
            return true;
        }
        if (!data.getAllowedSlots().isEmpty() && !data.getAllowedSlots().contains(slotContext.identifier())) {
            return false;
        }
        if (data.getMaxEquipped() > 0) {
            int count = CuriosApi.getCuriosHelper().findCurios(slotContext.entity(), s -> s.getItem() == this).size();
            if (count >= data.getMaxEquipped()) {
                return false;
            }
        }
        if (data.isUnique()) {
            return CuriosApi.getCuriosHelper().findEquippedCurio(this, slotContext.entity()).isEmpty();
        }
        if (data.getUniqueGroup() != null && !data.getUniqueGroup().isBlank()) {
            if (!CuriosApi.getCuriosHelper().findCurios(slotContext.entity(), itemStack -> {
                var itemId = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
                if (itemId == null) {
                    return false;
                }
                AccessoryData other = AccessoryDataManager.INSTANCE.get(itemId);
                return other != null && data.getUniqueGroup().equals(other.getUniqueGroup());
            }).isEmpty()) {
                return false;
            }
        }
        if (data.getMaxGroup() > 0 && data.getUniqueGroup() != null && !data.getUniqueGroup().isBlank()) {
            int count = CuriosApi.getCuriosHelper().findCurios(slotContext.entity(), itemStack -> {
                var itemId = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
                if (itemId == null) {
                    return false;
                }
                AccessoryData other = AccessoryDataManager.INSTANCE.get(itemId);
                return other != null && data.getUniqueGroup().equals(other.getUniqueGroup());
            }).size();
            if (count >= data.getMaxGroup()) {
                return false;
            }
        }
        return true;
    }
}
