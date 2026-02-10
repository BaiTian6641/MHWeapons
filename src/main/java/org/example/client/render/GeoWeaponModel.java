package org.example.client.render;

import net.minecraft.resources.ResourceLocation;
import org.example.MHWeaponsMod;
import org.example.item.GeoWeaponItem;
import software.bernie.geckolib.model.GeoModel;

public class GeoWeaponModel extends GeoModel<GeoWeaponItem> {
    private final String weaponId;

    public GeoWeaponModel(String weaponId) {
        this.weaponId = weaponId;
    }

    @Override
    public ResourceLocation getModelResource(GeoWeaponItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(MHWeaponsMod.MODID, "geo/" + weaponId + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GeoWeaponItem animatable) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", getPlaceholderTexture());
    }

    @Override
    public ResourceLocation getAnimationResource(GeoWeaponItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(MHWeaponsMod.MODID, "animations/" + weaponId + ".animation.json");
    }

    private String getPlaceholderTexture() {
        return switch (weaponId) {
            case "greatsword" -> "textures/item/netherite_sword.png";
            case "longsword" -> "textures/item/diamond_sword.png";
            case "sword_and_shield" -> "textures/item/iron_sword.png";
            case "dual_blades" -> "textures/item/golden_sword.png";
            case "hammer" -> "textures/item/iron_axe.png";
            case "hunting_horn" -> "textures/item/trident.png";
            case "lance" -> "textures/item/stone_sword.png";
            case "gunlance" -> "textures/item/iron_sword.png";
            case "switch_axe" -> "textures/item/diamond_axe.png";
            case "charge_blade" -> "textures/item/diamond_sword.png";
            case "insect_glaive" -> "textures/item/iron_sword.png";
            case "bowgun" -> "textures/item/crossbow_standby.png";
            default -> "textures/item/diamond_sword.png";
        };
    }
}
