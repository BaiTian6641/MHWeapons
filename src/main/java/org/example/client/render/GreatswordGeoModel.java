package org.example.client.render;

import net.minecraft.resources.ResourceLocation;
import org.example.MHWeaponsMod;
import org.example.item.GreatswordGeoItem;
import software.bernie.geckolib.model.GeoModel;

public class GreatswordGeoModel extends GeoModel<GreatswordGeoItem> {
    @Override
    public ResourceLocation getModelResource(GreatswordGeoItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(MHWeaponsMod.MODID, "geo/greatsword.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GreatswordGeoItem animatable) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/netherite_sword.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GreatswordGeoItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(MHWeaponsMod.MODID, "animations/greatsword.animation.json");
    }
}
