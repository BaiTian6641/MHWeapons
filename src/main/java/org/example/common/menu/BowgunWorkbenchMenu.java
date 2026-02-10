package org.example.common.menu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.example.common.combat.bowgun.BowgunModResolver;
import org.example.item.BowgunItem;
import org.example.item.BowgunModItem;
import org.example.registry.MHWeaponsMenus;
import org.example.registry.MHWeaponsItems;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu for the Bowgun Modification Workbench.
 * Layout:
 *  - Slot 0: Bowgun weapon slot
 *  - Slots 1-9: Modification slots (frame, barrel, stock, magazine, shield, special, accessory, accessory+, cosmetic)
 *  - Slots 8+: Player inventory (3x9 + hotbar)
 *
 * Category names: frame, barrel, stock, magazine, shield, special, accessory, accessory, cosmetic
 */
public class BowgunWorkbenchMenu extends AbstractContainerMenu {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int MOD_SLOT_COUNT = 9;
    private static final int BOWGUN_SLOT = 0;
    private static final int MOD_SLOT_START = 1;
                private static final String[] MOD_CATEGORIES = {
                    "frame", "barrel", "stock", "magazine", "shield", "special", "accessory", "accessory", "cosmetic"
                };

    private final Container container;
    private final Slot[] modSlots = new Slot[MOD_SLOT_COUNT];
    private boolean showAccessory2 = false;

    public BowgunWorkbenchMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(1 + MOD_SLOT_COUNT));
    }

    public BowgunWorkbenchMenu(int containerId, Inventory inventory, Container container) {
        super(MHWeaponsMenus.BOWGUN_WORKBENCH.get(), containerId);
        this.container = container;

        // Bowgun slot
        this.addSlot(new Slot(container, BOWGUN_SLOT, 26, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof BowgunItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                onBowgunChanged();
            }
        });

        // Mod slots
        for (int i = 0; i < MOD_SLOT_COUNT; i++) {
            final int catIndex = i;
            // Row 1 (0-3): y=29, Row 2 (4-7): y=55, Row 3 (8): y=81
            int x = 80 + (i % 4) * 20;
            int y = 29 + (i / 4) * 26;
            Slot modSlot = new Slot(container, MOD_SLOT_START + i, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    if (!(stack.getItem() instanceof BowgunModItem modItem)) return false;
                    if (!MOD_CATEGORIES[catIndex].equals(modItem.getCategory())) return false;
                    // Disable shield slot for Light framework
                    if ("shield".equals(modItem.getCategory())) {
                        ItemStack bowgun = container.getItem(BOWGUN_SLOT);
                        if (bowgun.getItem() instanceof BowgunItem) {
                            return BowgunItem.getWeightClass(bowgun) != 0;
                        }
                        return false;
                    }
                    // Second accessory slot only available on Heavy framework
                    if ("accessory".equals(modItem.getCategory()) && catIndex == 7) {
                        ItemStack bowgun = container.getItem(BOWGUN_SLOT);
                        if (bowgun.getItem() instanceof BowgunItem) {
                            return BowgunItem.getWeightClass(bowgun) == 2;
                        }
                        return false;
                    }
                    return true;
                }

                @Override
                public boolean isActive() {
                    if (catIndex == 7) {
                        return showAccessory2;
                    }
                    return super.isActive();
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    onModChanged();
                }
            };
            this.addSlot(modSlot);
            modSlots[i] = modSlot;
        }

        // Player inventory
        int slotSize = 18;
        int startX = 8;
        int startY = 146;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, 9 + row * 9 + col,
                        startX + col * slotSize, startY + row * slotSize));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, startX + col * slotSize, startY + 58));
        }
    }

    /**
     * When the bowgun is placed in the slot, load its installed mods into the mod slots.
     */
    private void onBowgunChanged() {
        ItemStack bowgun = container.getItem(BOWGUN_SLOT);
        // Clear mod slots first
        for (int i = 0; i < MOD_SLOT_COUNT; i++) {
            container.setItem(MOD_SLOT_START + i, ItemStack.EMPTY);
        }
        updateAccessorySlotVisibility(bowgun);
        if (bowgun.isEmpty() || !(bowgun.getItem() instanceof BowgunItem)) return;

        // Load existing mods from NBT and resolve them to item instances
        List<String> mods = BowgunItem.getInstalledMods(bowgun);
        LOG.debug("[BowgunWorkbench] Loaded {} mods from bowgun: {}", mods.size(), mods);

        // Build reverse lookup: modId -> registered mod item
        Map<String, ItemStack> modIdToItem = buildModIdLookup();

        for (String modId : mods) {
            ItemStack modStack = modIdToItem.get(modId);
            if (modStack == null) {
                LOG.warn("[BowgunWorkbench] No registered item for mod ID: {}", modId);
                continue;
            }
            // Find the correct category slot (supports multiple slots per category)
            String category = BowgunModResolver.getModCategory(modId);
            int slotIndex = getFirstEmptyCategorySlot(category);
            if (slotIndex >= 0) {
                container.setItem(MOD_SLOT_START + slotIndex, modStack.copy());
                LOG.debug("[BowgunWorkbench] Restored mod '{}' to slot {} ({})", modId, slotIndex, category);
            }
        }
    }

    private void updateAccessorySlotVisibility(ItemStack bowgun) {
        // Second accessory slot is index 7 in MOD_CATEGORIES/modSlots
        int accessory2Index = 7;
        if (accessory2Index < modSlots.length && modSlots[accessory2Index] != null) {
            boolean heavy = bowgun.getItem() instanceof BowgunItem
                    && BowgunItem.getWeightClass(bowgun) == 2;
            showAccessory2 = heavy;
            if (!heavy) {
                container.setItem(MOD_SLOT_START + accessory2Index, ItemStack.EMPTY);
            }
        }
    }

    /**
     * Build a lookup map from mod ID to ItemStack by scanning all registered BowgunModItems.
     */
    private static Map<String, ItemStack> buildModIdLookup() {
        Map<String, ItemStack> map = new HashMap<>();
        // Scan all registered items in the mod's deferred register
        net.minecraftforge.registries.ForgeRegistries.ITEMS.getValues().forEach(item -> {
            if (item instanceof BowgunModItem modItem) {
                map.put(modItem.getModId(), new ItemStack(modItem));
            }
        });
        return map;
    }

    /**
     * Get the slot index (0-6) for a given mod category.
     */
    private int getFirstEmptyCategorySlot(String category) {
        for (int i = 0; i < MOD_CATEGORIES.length; i++) {
            if (MOD_CATEGORIES[i].equals(category)
                    && container.getItem(MOD_SLOT_START + i).isEmpty()) {
                // If this is the second accessory slot, enforce heavy framework
                if ("accessory".equals(category) && i == 7) {
                    ItemStack bowgun = container.getItem(BOWGUN_SLOT);
                    if (bowgun.getItem() instanceof BowgunItem) {
                        if (BowgunItem.getWeightClass(bowgun) != 2) {
                            continue;
                        }
                    }
                }
                return i;
            }
        }
        return -1;
    }

    /**
     * When a mod is placed/removed, update the bowgun's NBT.
     */
    private void onModChanged() {
        ItemStack bowgun = container.getItem(BOWGUN_SLOT);
        if (bowgun.isEmpty() || !(bowgun.getItem() instanceof BowgunItem)) return;

        List<String> modIds = new ArrayList<>();
        for (int i = 0; i < MOD_SLOT_COUNT; i++) {
            ItemStack modStack = container.getItem(MOD_SLOT_START + i);
            if (modStack.getItem() instanceof BowgunModItem modItem) {
                modIds.add(modItem.getModId());
            }
        }

        BowgunItem.setInstalledMods(bowgun, modIds);
        LOG.debug("[BowgunWorkbench] Updated bowgun with {} mods: {}", modIds.size(), modIds);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            // "Apply" button — write mods to bowgun and close
            onModChanged();
            LOG.debug("[BowgunWorkbench] Applied modifications");
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            int containerSlots = 1 + MOD_SLOT_COUNT;
            int playerStart = containerSlots;
            int playerEnd = playerStart + 36;

            if (index < containerSlots) {
                // Move from workbench to player
                if (!moveItemStackTo(slotStack, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player to workbench
                if (slotStack.getItem() instanceof BowgunItem) {
                    if (!moveItemStackTo(slotStack, BOWGUN_SLOT, BOWGUN_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotStack.getItem() instanceof BowgunModItem) {
                    if (!moveItemStackTo(slotStack, MOD_SLOT_START, MOD_SLOT_START + MOD_SLOT_COUNT, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /**
     * On close, return items to player if not applied.
     */
    @Override
    public void removed(Player player) {
        super.removed(player);
        // Return all container items to player
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (!player.addItem(stack)) {
                    player.drop(stack, false);
                }
                container.setItem(i, ItemStack.EMPTY);
            }
        }
    }
}
