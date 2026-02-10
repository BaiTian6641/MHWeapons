package org.example.common.menu;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.example.common.data.DecorationDataManager;
import org.example.common.util.DecorationUtil;
import org.example.registry.MHWeaponsMenus;

@SuppressWarnings("null")
public class DecorationWorkbenchMenu extends AbstractContainerMenu {
    private static final int MAX_DECORATION_SLOTS = 12;
    private static final int GEAR_SLOT_INDEX = 0;
    private static final int DECORATION_SLOT_START = 1;

    private final Container container;
    private int filterMode = 0; // 0=any, 1=weapon, 2=armor

    public DecorationWorkbenchMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(DECORATION_SLOT_START + MAX_DECORATION_SLOTS));
    }

    public DecorationWorkbenchMenu(int containerId, Inventory inventory, Container container) {
        super(MHWeaponsMenus.DECORATION_WORKBENCH.get(), containerId);
        this.container = container;

        this.addSlot(new Slot(container, GEAR_SLOT_INDEX, 26, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return DecorationUtil.getSlotsWithDefaults(stack).length > 0;
            }
        });

        int decoStartX = 80;
        int decoStartY = 24;
        int slotSize = 18;
        int cols = 4;
        for (int i = 0; i < MAX_DECORATION_SLOTS; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = decoStartX + col * slotSize;
            int y = decoStartY + row * slotSize;
            this.addSlot(new DecorationSlot(container, DECORATION_SLOT_START + i, x, y, i));
        }

        int startX = 8;
        int startY = 138;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, 9 + row * 9 + col, startX + col * slotSize, startY + row * slotSize));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, startX + col * slotSize, startY + 58));
        }
    }

    public ItemStack getGearStack() {
        return container.getItem(GEAR_SLOT_INDEX);
    }

    public int getFilterMode() {
        return filterMode;
    }

    private boolean isDecorationItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation decoId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return decoId != null && DecorationDataManager.INSTANCE.get(decoId) != null;
    }

    private boolean isCategoryCompatible(ItemStack gear, org.example.common.data.DecorationData data) {
        return true;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        ItemStack gear = container.getItem(0);
        ItemStack jewel = ItemStack.EMPTY;
        if (id == 0) {
            if (gear.isEmpty()) {
                return false;
            }
            DecorationUtil.ensureDefaultSlots(gear);
            int[] slots = DecorationUtil.getSlotsWithDefaults(gear);
            boolean installedAny = false;
            for (int i = 0; i < MAX_DECORATION_SLOTS && i < slots.length; i++) {
                ItemStack candidate = container.getItem(DECORATION_SLOT_START + i);
                if (candidate.isEmpty() || !isDecorationItem(candidate)) {
                    continue;
                }
                ResourceLocation decoId = ForgeRegistries.ITEMS.getKey(candidate.getItem());
                if (decoId == null) {
                    continue;
                }
                var decoData = DecorationDataManager.INSTANCE.get(decoId);
                if (decoData == null || !filterAllowsForGear(gear) || !isCategoryCompatible(gear, decoData)) {
                    continue;
                }
                if (DecorationUtil.findDecorationAt(gear, i) != null) {
                    continue;
                }
                int tier = decoData == null ? 1 : Math.max(1, decoData.getTier());
                if (tier > slots[i]) {
                    continue;
                }
                boolean ok = DecorationUtil.installDecoration(gear, i, decoId);
                if (ok) {
                    installedAny = true;
                    if (!player.getAbilities().instabuild) {
                        candidate.shrink(1);
                    }
                    if (player.getAbilities().instabuild || candidate.isEmpty()) {
                        container.setItem(DECORATION_SLOT_START + i, ItemStack.EMPTY);
                    }
                }
            }
            return installedAny;
        }
        if (id == 1) {
            if (gear.isEmpty()) {
                return false;
            }
            var decos = DecorationUtil.getDecorations(gear);
            if (decos.isEmpty()) {
                return false;
            }
            var last = decos.get(decos.size() - 1);
            boolean removed = DecorationUtil.removeDecoration(gear, last.slot());
            if (removed) {
                ItemStack decoItem = new ItemStack(ForgeRegistries.ITEMS.getValue(last.id()));
                if (!decoItem.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(decoItem);
                }
            }
            return removed;
        }
        if (id == 2) {
            this.filterMode = 0;
            return true;
        }
        if (id == 3) {
            this.filterMode = 1;
            return true;
        }
        if (id == 4) {
            this.filterMode = 2;
            return true;
        }
        return false;
    }

    private boolean filterAllowsForGear(ItemStack gear) {
        if (filterMode == 0) {
            return true;
        }
        if (gear.isEmpty()) {
            return false;
        }
        String gearCategory = DecorationUtil.getGearCategory(gear);
        if (filterMode == 1) {
            return "weapon".equalsIgnoreCase(gearCategory);
        }
        return "armor".equalsIgnoreCase(gearCategory);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            this.clearContainer(player, container);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < DECORATION_SLOT_START + MAX_DECORATION_SLOTS) {
                if (!this.moveItemStackTo(stack, DECORATION_SLOT_START + MAX_DECORATION_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (isDecorationItem(stack)) {
                    boolean moved = false;
                    for (int i = 0; i < MAX_DECORATION_SLOTS; i++) {
                        if (this.moveItemStackTo(stack, DECORATION_SLOT_START + i, DECORATION_SLOT_START + i + 1, false)) {
                            moved = true;
                            break;
                        }
                    }
                    if (!moved) {
                        return ItemStack.EMPTY;
                    }
                } else if (DecorationUtil.getSlotsWithDefaults(stack).length > 0) {
                    if (!this.moveItemStackTo(stack, GEAR_SLOT_INDEX, GEAR_SLOT_INDEX + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(stack, DECORATION_SLOT_START + MAX_DECORATION_SLOTS, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    private class DecorationSlot extends Slot {
        private final int decorationIndex;

        public DecorationSlot(Container container, int slot, int x, int y, int decorationIndex) {
            super(container, slot, x, y);
            this.decorationIndex = decorationIndex;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            ItemStack gear = getGearStack();
            if (gear.isEmpty()) {
                return false;
            }
            DecorationUtil.ensureDefaultSlots(gear);
            int[] slots = DecorationUtil.getSlotsWithDefaults(gear);
            if (decorationIndex >= slots.length) {
                return false;
            }
            if (!isDecorationItem(stack)) {
                return false;
            }
            ResourceLocation decoId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (decoId == null) {
                return false;
            }
            var decoData = DecorationDataManager.INSTANCE.get(decoId);
            if (decoData == null || !filterAllowsForGear(gear) || !isCategoryCompatible(gear, decoData)) {
                return false;
            }
            if (DecorationUtil.findDecorationAt(gear, decorationIndex) != null) {
                return false;
            }
            return Math.max(1, decoData.getTier()) <= slots[decorationIndex];
        }
    }
}