package org.example.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import org.example.common.entity.KinsectEntity;

@SuppressWarnings({"null", "deprecation"})
public class KinsectRenderer extends EntityRenderer<KinsectEntity> {
    public KinsectRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(KinsectEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/diamond.png"); // Placeholder
    }

    @Override
    public void render(KinsectEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        poseStack.pushPose();
        poseStack.scale(0.35f, 0.35f, 0.35f);
        var state = switch (entity.getColor()) {
            case 1 -> Blocks.RED_WOOL.defaultBlockState();
            case 2 -> Blocks.WHITE_WOOL.defaultBlockState();
            case 3 -> Blocks.ORANGE_WOOL.defaultBlockState();
            default -> Blocks.LIGHT_GRAY_WOOL.defaultBlockState();
        };
        dispatcher.renderSingleBlock(state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
