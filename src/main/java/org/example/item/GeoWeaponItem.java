package org.example.item;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.example.client.render.GeoWeaponModel;
import org.example.common.combat.MHDamageType;
import org.example.common.combat.MHDamageTypeProvider;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GeoWeaponItem extends SwordItem implements GeoItem, MHDamageTypeProvider, WeaponIdProvider {
    private final String weaponId;
    private final MHDamageType damageType;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    private static final String SHARPNESS_TAG = "mh_sharpness";
    private static final int MAX_SHARPNESS = 100;

    public GeoWeaponItem(String weaponId, MHDamageType damageType, Tier tier, int attackDamageModifier, float attackSpeedModifier, Item.Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
        this.weaponId = weaponId;
        this.damageType = damageType;
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }
    
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int sharpness = stack.getOrCreateTag().contains(SHARPNESS_TAG) ? stack.getOrCreateTag().getInt(SHARPNESS_TAG) : MAX_SHARPNESS;
        return Math.round((float)sharpness * 13.0F / MAX_SHARPNESS);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int sharpness = stack.getOrCreateTag().contains(SHARPNESS_TAG) ? stack.getOrCreateTag().getInt(SHARPNESS_TAG) : MAX_SHARPNESS;
        float ratio = Math.max(0.0f, Math.min(1.0f, sharpness / (float) MAX_SHARPNESS));

        if (stack.getItem() instanceof net.minecraft.world.item.TieredItem tieredItem) {
            net.minecraft.world.item.Tier tier = tieredItem.getTier();
            boolean isHighTier = tier == org.example.item.MHTiers.BLACK || tier == org.example.item.MHTiers.PURPLE;
            if (isHighTier) {
                if (ratio >= 0.85f) {
                    return 0xFF9C27B0; // Purple
                }
                if (ratio >= 0.70f) {
                    return 0xFFFFFFFF; // White
                }
                if (ratio >= 0.55f) {
                    return 0xFF4FC3F7; // Blue
                }
            }
        }

        if (ratio >= 0.8f) {
            return 0xFF4FC3F7; // Blue
        }
        if (ratio >= 0.6f) {
            return 0xFF66BB6A; // Green
        }
        if (ratio >= 0.4f) {
            return 0xFFFFEE58; // Yellow
        }
        if (ratio >= 0.2f) {
            return 0xFFFFA726; // Orange
        }
        return 0xFFEF5350; // Red
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, net.minecraft.world.entity.LivingEntity target, net.minecraft.world.entity.LivingEntity attacker) {
         net.minecraft.nbt.CompoundTag tag = stack.getOrCreateTag();
         int current = tag.contains(SHARPNESS_TAG) ? tag.getInt(SHARPNESS_TAG) : MAX_SHARPNESS;
         if (current > 0) {
             tag.putInt(SHARPNESS_TAG, current - 1);
         }
         return true; 
    }

    @Override
    public boolean mineBlock(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos, net.minecraft.world.entity.LivingEntity entityLiving) {
        if (state.getDestroySpeed(level, pos) != 0.0F) {
             net.minecraft.nbt.CompoundTag tag = stack.getOrCreateTag();
             int current = tag.contains(SHARPNESS_TAG) ? tag.getInt(SHARPNESS_TAG) : MAX_SHARPNESS;
             if (current > 0) {
                 tag.putInt(SHARPNESS_TAG, current - 1);
             }
        }
        return true;
    }

    // Adjust attributes based on sharpness
    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot slot, ItemStack stack) {
        com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> modifiers = com.google.common.collect.LinkedHashMultimap.create(super.getAttributeModifiers(slot, stack));
        
        if (slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND) {
             int sharpness = stack.getOrCreateTag().contains(SHARPNESS_TAG) ? stack.getOrCreateTag().getInt(SHARPNESS_TAG) : MAX_SHARPNESS;
             if (sharpness <= 0) {
                  // Sharpness penalty (50% damage reduction)
                  modifiers.put(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 
                      new net.minecraft.world.entity.ai.attributes.AttributeModifier(java.util.UUID.fromString("c07b4694-8456-42f0-9366-224424363290"), "Sharpness penalty", -0.5, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
             }
        }
        return modifiers;
    }

    public String getWeaponId() {
        return weaponId;
    }

    @Override
    public MHDamageType getDamageType(ItemStack stack) {
        return damageType;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.mhweaponsmod." + weaponId + ".idle"));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void initializeClient(@Nonnull Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GeoItemRenderer<GeoWeaponItem> renderer;

            @Override
            public GeoItemRenderer<GeoWeaponItem> getCustomRenderer() {
                if (renderer == null) {
                    renderer = new GeoItemRenderer<>(new GeoWeaponModel(weaponId));
                }
                return renderer;
            }
        });
    }
}
