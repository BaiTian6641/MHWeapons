package org.example.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import org.example.common.entity.AmmoProjectileEntity;

/**
 * Placeholder renderer for bowgun ammo projectiles.
 * Renders as a small colored block matching the ammo type.
 */
@SuppressWarnings({"null", "deprecation"})
public class AmmoProjectileRenderer extends EntityRenderer<AmmoProjectileEntity> {
    public AmmoProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(AmmoProjectileEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/arrow.png");
    }

    @Override
    public void render(AmmoProjectileEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        poseStack.pushPose();
        poseStack.scale(0.15f, 0.15f, 0.15f);

        var state = resolveBlockForAmmo(entity.getAmmoType());
        dispatcher.renderSingleBlock(state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static net.minecraft.world.level.block.state.BlockState resolveBlockForAmmo(String ammoType) {
        if (ammoType == null) return Blocks.IRON_BLOCK.defaultBlockState();
        if (ammoType.startsWith("fire_") || ammoType.startsWith("flaming_"))
            return Blocks.ORANGE_WOOL.defaultBlockState();
        if (ammoType.startsWith("water_")) return Blocks.BLUE_WOOL.defaultBlockState();
        if (ammoType.startsWith("thunder_")) return Blocks.YELLOW_WOOL.defaultBlockState();
        if (ammoType.startsWith("ice_")) return Blocks.LIGHT_BLUE_WOOL.defaultBlockState();
        if (ammoType.startsWith("dragon_")) return Blocks.PURPLE_WOOL.defaultBlockState();
        if (ammoType.startsWith("poison_")) return Blocks.LIME_WOOL.defaultBlockState();
        if (ammoType.startsWith("paralysis_")) return Blocks.YELLOW_WOOL.defaultBlockState();
        if (ammoType.startsWith("sleep_")) return Blocks.CYAN_WOOL.defaultBlockState();
        if (ammoType.startsWith("sticky_")) return Blocks.RED_WOOL.defaultBlockState();
        if (ammoType.startsWith("cluster_")) return Blocks.RED_WOOL.defaultBlockState();
        if (ammoType.contains("wyvern")) return Blocks.MAGENTA_WOOL.defaultBlockState();
        return Blocks.IRON_BLOCK.defaultBlockState();
    }
}
