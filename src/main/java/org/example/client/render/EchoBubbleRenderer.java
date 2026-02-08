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
import org.example.common.entity.EchoBubbleEntity;

@SuppressWarnings({"null", "deprecation"})
public class EchoBubbleRenderer extends EntityRenderer<EchoBubbleEntity> {
    public EchoBubbleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EchoBubbleEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/light_blue_stained_glass.png");
    }

    @Override
    protected int getBlockLightLevel(EchoBubbleEntity entity, net.minecraft.core.BlockPos pos) {
        return 15;
    }

    @Override
    public void render(EchoBubbleEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        poseStack.pushPose();
        float base = Math.max(0.5f, entity.getRadius());
        float pulse = 0.85f + 0.1f * (float) Math.sin((entity.tickCount + partialTicks) * 0.2f);
        poseStack.scale(base * pulse, 0.2f, base * pulse);
        dispatcher.renderSingleBlock(Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
