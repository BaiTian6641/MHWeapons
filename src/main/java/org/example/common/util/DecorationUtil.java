package org.example.common.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.registries.ForgeRegistries;
import org.example.common.data.DecorationData;
import org.example.common.data.DecorationDataManager;
import org.example.common.data.GearDecorationDataManager;
import org.example.item.AccessoryItem;
import org.example.item.WeaponIdProvider;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("null")
public final class DecorationUtil {
    private static final String SLOTS_KEY = "mh_deco_slots";
    private static final String DECOS_KEY = "mh_decorations";

    private DecorationUtil() {
    }

    public static int[] getSlots(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(SLOTS_KEY, Tag.TAG_INT_ARRAY)) {
            return new int[0];
        }
        return tag.getIntArray(SLOTS_KEY);
    }

    public static int[] getSlotsWithDefaults(ItemStack stack) {
        int[] slots = getSlots(stack);
        if (slots.length > 0) {
            return slots;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return new int[0];
        }
        int[] defaults = GearDecorationDataManager.INSTANCE.getSlots(id);
        if (defaults.length > 0) {
            setSlots(stack, defaults);
            return defaults;
        }
        if (!"mhweaponsmod".equals(id.getNamespace())) {
            ResourceLocation fallback = ResourceLocation.fromNamespaceAndPath("mhweaponsmod", id.getPath());
            int[] fb = GearDecorationDataManager.INSTANCE.getSlots(fallback);
            if (fb.length > 0) {
                setSlots(stack, fb);
                return fb;
            }
        }
        // Last resort: allow a single tier-1 slot on recognizable gear (armor/accessory/weapon) so decorations are not blocked.
        if (stack.getItem() instanceof ArmorItem || stack.getItem() instanceof AccessoryItem || stack.getItem() instanceof WeaponIdProvider) {
            int[] fb = new int[] { 1 };
            setSlots(stack, fb);
            return fb;
        }
        return defaults;
    }

    public static void ensureDefaultSlots(ItemStack stack) {
        int[] current = getSlots(stack);
        int[] defaults = getSlotsWithDefaults(stack);
        if (current.length == 0) {
            if (defaults.length > 0) {
                setSlots(stack, defaults);
            }
            return;
        }
        if (defaults.length > current.length && isLegacyPlaceholderSlots(current)) {
            setSlots(stack, defaults);
        }
    }

    private static boolean isLegacyPlaceholderSlots(int[] slots) {
        if (slots == null || slots.length == 0) {
            return false;
        }
        for (int slot : slots) {
            if (slot != 1) {
                return false;
            }
        }
        return true;
    }

    public static void setSlots(ItemStack stack, int[] slots) {
        if (slots == null) {
            return;
        }
        stack.getOrCreateTag().putIntArray(SLOTS_KEY, slots);
    }

    public static List<DecorationInstance> getDecorations(ItemStack stack) {
        List<DecorationInstance> list = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(DECOS_KEY, Tag.TAG_LIST)) {
            return list;
        }
        ListTag decos = tag.getList(DECOS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < decos.size(); i++) {
            CompoundTag decoTag = decos.getCompound(i);
            int slot = decoTag.getInt("slot");
            String id = decoTag.getString("id");
            ResourceLocation decoId = ResourceLocation.tryParse(id);
            if (decoId != null) {
                list.add(new DecorationInstance(slot, decoId));
            }
        }
        return list;
    }

    public static boolean installDecoration(ItemStack stack, int slotIndex, ResourceLocation decoId) {
        int[] slots = getSlotsWithDefaults(stack);
        if (slotIndex < 0 || slotIndex >= slots.length) {
            return false;
        }
        DecorationData data = DecorationDataManager.INSTANCE.get(decoId);
        if (data == null) {
            return false;
        }
        if (!isCategoryCompatible(stack, data)) {
            return false;
        }
        int tier = Math.max(1, data.getTier());
        if (tier > slots[slotIndex]) {
            return false;
        }
        if (findDecorationAt(stack, slotIndex) != null) {
            return false;
        }
        ensureDefaultSlots(stack);
        CompoundTag tag = stack.getOrCreateTag();
        ListTag list = tag.contains(DECOS_KEY, Tag.TAG_LIST) ? tag.getList(DECOS_KEY, Tag.TAG_COMPOUND) : new ListTag();
        CompoundTag decoTag = new CompoundTag();
        decoTag.putInt("slot", slotIndex);
        decoTag.putString("id", decoId.toString());
        list.add(decoTag);
        tag.put(DECOS_KEY, list);
        return true;
    }

    public static int findFirstCompatibleSlot(ItemStack stack, ResourceLocation decoId) {
        int[] slots = getSlotsWithDefaults(stack);
        DecorationData data = DecorationDataManager.INSTANCE.get(decoId);
        if (data == null) {
            return -1;
        }
        if (!isCategoryCompatible(stack, data)) {
            return -1;
        }
        for (int i = 0; i < slots.length; i++) {
            if (findDecorationAt(stack, i) != null) {
                continue;
            }
            int tier = Math.max(1, data.getTier());
            if (tier <= slots[i]) {
                return i;
            }
        }
        return -1;
    }

    public static String getGearCategory(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem) {
            return "armor";
        }
        if (stack.getItem() instanceof AccessoryItem) {
            return "armor";
        }
        return "weapon";
    }

    private static boolean isCategoryCompatible(ItemStack gear, DecorationData data) {
        return true;
    }

    public static boolean removeDecoration(ItemStack stack, int slotIndex) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(DECOS_KEY, Tag.TAG_LIST)) {
            return false;
        }
        ListTag list = tag.getList(DECOS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag decoTag = list.getCompound(i);
            if (decoTag.getInt("slot") == slotIndex) {
                list.remove(i);
                tag.put(DECOS_KEY, list);
                return true;
            }
        }
        return false;
    }

    public static void clearDecorations(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(DECOS_KEY);
        }
    }

    public static DecorationInstance findDecorationAt(ItemStack stack, int slotIndex) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(DECOS_KEY, Tag.TAG_LIST)) {
            return null;
        }
        ListTag list = tag.getList(DECOS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag decoTag = list.getCompound(i);
            if (decoTag.getInt("slot") == slotIndex) {
                ResourceLocation id = ResourceLocation.tryParse(decoTag.getString("id"));
                if (id != null) {
                    return new DecorationInstance(slotIndex, id);
                }
            }
        }
        return null;
    }

    public record DecorationInstance(int slot, ResourceLocation id) {
    }
}