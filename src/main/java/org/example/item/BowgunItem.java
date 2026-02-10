package org.example.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.example.client.render.GeoWeaponModel;
import org.example.common.combat.MHDamageType;
import org.example.common.combat.MHDamageTypeProvider;
import org.example.common.combat.bowgun.BowgunModResolver;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Unified Bowgun item. Replaces LBG and HBG with a single customisable
 * ranged weapon whose weight (and therefore behaviour) is determined by
 * installed modifications stored in NBT.
 */
public class BowgunItem extends GeoWeaponItem {
    private static final Logger LOG = LogUtils.getLogger();

    // NBT keys
    public static final String TAG_MODS = "bowgun_mods";         // ListTag of mod IDs (strings)
    public static final String TAG_WEIGHT = "bowgun_weight";     // cached computed weight
    public static final String TAG_MAGAZINES = "bowgun_magazines"; // CompoundTag: ammoId -> count
    public static final String TAG_CURRENT_AMMO = "bowgun_current_ammo";
    public static final String TAG_IGNITION_TYPE = "bowgun_ignition_type";

    // Weight class thresholds
    public static final int WEIGHT_LIGHT_MAX = 30;
    public static final int WEIGHT_MEDIUM_MAX = 60;
    public static final int WEIGHT_MAX = 100;
    public static final int DEFAULT_WEIGHT = 40; // Medium-ish default with no mods

    public BowgunItem(Item.Properties properties) {
        super("bowgun", MHDamageType.SHOT, MHTiers.BLACK, 3, -2.8f, properties);
        LOG.debug("[Bowgun] BowgunItem registered");
    }

    // ── Weight helpers ───────────────────────────────────────────────

    /** Compute the weapon weight from installed mods and cache in NBT. */
    public static int computeWeight(ItemStack stack) {
        List<String> mods = getInstalledMods(stack);
        int weight = DEFAULT_WEIGHT;
        for (String modId : mods) {
            weight += BowgunModResolver.getModWeight(modId);
        }
        weight = Math.max(0, Math.min(WEIGHT_MAX, weight));
        stack.getOrCreateTag().putInt(TAG_WEIGHT, weight);
        LOG.debug("[Bowgun] computeWeight mods={} result={}", mods, weight);
        return weight;
    }

    public static int getWeight(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_WEIGHT)) {
            return computeWeight(stack);
        }
        return tag.getInt(TAG_WEIGHT);
    }

    /** 0 = LIGHT, 1 = MEDIUM, 2 = HEAVY */
    public static int getWeightClass(ItemStack stack) {
        int w = getWeight(stack);
        if (w <= WEIGHT_LIGHT_MAX) return 0;
        if (w <= WEIGHT_MEDIUM_MAX) return 1;
        return 2;
    }

    public static String getWeightClassName(ItemStack stack) {
        return switch (getWeightClass(stack)) {
            case 0 -> "Light";
            case 1 -> "Medium";
            default -> "Heavy";
        };
    }

    // ── Mod storage ──────────────────────────────────────────────────

    public static List<String> getInstalledMods(ItemStack stack) {
        List<String> list = new ArrayList<>();
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains(TAG_MODS, Tag.TAG_LIST)) {
            ListTag listTag = tag.getList(TAG_MODS, Tag.TAG_STRING);
            for (int i = 0; i < listTag.size(); i++) {
                list.add(listTag.getString(i));
            }
        }
        return list;
    }

    public static void setInstalledMods(ItemStack stack, List<String> mods) {
        ListTag listTag = new ListTag();
        for (String id : mods) {
            listTag.add(StringTag.valueOf(id));
        }
        stack.getOrCreateTag().put(TAG_MODS, listTag);
        computeWeight(stack); // re-cache weight
        // re-resolve ignition type
        String ignition = BowgunModResolver.resolveIgnitionType(mods);
        stack.getOrCreateTag().putString(TAG_IGNITION_TYPE, ignition);
        LOG.debug("[Bowgun] setInstalledMods mods={} weight={} ignition={}",
                mods, getWeight(stack), ignition);
    }

    public static void addMod(ItemStack stack, String modId) {
        List<String> mods = getInstalledMods(stack);
        mods.add(modId);
        setInstalledMods(stack, mods);
    }

    public static void removeMod(ItemStack stack, String modId) {
        List<String> mods = getInstalledMods(stack);
        mods.remove(modId);
        setInstalledMods(stack, mods);
    }

    // ── Ammo helpers ─────────────────────────────────────────────────

    public static String getCurrentAmmo(ItemStack stack) {
        return stack.getOrCreateTag().getString(TAG_CURRENT_AMMO);
    }

    public static void setCurrentAmmo(ItemStack stack, String ammoId) {
        stack.getOrCreateTag().putString(TAG_CURRENT_AMMO, ammoId);
    }

    public static String getIgnitionType(ItemStack stack) {
        return stack.getOrCreateTag().getString(TAG_IGNITION_TYPE);
    }

    // ── Magazine NBT ─────────────────────────────────────────────────

    public static CompoundTag getMagazinesTag(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_MAGAZINES, Tag.TAG_COMPOUND)) {
            tag.put(TAG_MAGAZINES, new CompoundTag());
        }
        return tag.getCompound(TAG_MAGAZINES);
    }

    public static int getMagazineCount(ItemStack stack, String ammoId) {
        return getMagazinesTag(stack).getInt(ammoId);
    }

    public static void setMagazineCount(ItemStack stack, String ammoId, int count) {
        getMagazinesTag(stack).putInt(ammoId, Math.max(0, count));
    }

    // ── Tooltip ──────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        int weight = getWeight(stack);
        String cls = getWeightClassName(stack);
        tooltip.add(Component.literal("§7Weight: §f" + weight + " §7[§e" + cls + "§7]"));
        List<String> mods = getInstalledMods(stack);
        if (!mods.isEmpty()) {
            tooltip.add(Component.literal("§7Mods: §f" + String.join(", ", mods)));
        }
        String ammo = getCurrentAmmo(stack);
        if (!ammo.isEmpty()) {
            tooltip.add(Component.literal("§7Ammo: §f" + ammo));
        }
    }

    // ── Override sharpness for bowgun (not applicable) ────────────────

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false; // No sharpness bar for ranged weapon
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Melee bash – no sharpness loss
        return true;
    }
}
