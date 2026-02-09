package org.example.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import org.example.common.entity.KinsectEntity;
import org.example.common.entity.KinsectPowderCloudEntity;

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

        // Spawn powder trail particles behind the kinsect while flying
        int powderType = entity.getPowderTypeData();
        if (powderType > 0 && entity.tickCount % 2 == 0) {
            ParticleOptions particle = KinsectPowderCloudEntity.resolvePowderDustParticle(powderType);
            double x = entity.getX() + (entity.level().random.nextDouble() - 0.5) * 0.3;
            double y = entity.getY() + (entity.level().random.nextDouble() - 0.5) * 0.2;
            double z = entity.getZ() + (entity.level().random.nextDouble() - 0.5) * 0.3;
            entity.level().addParticle(particle, x, y, z, 0.0, -0.01, 0.0);
        }

        // Mark mode glow indicator
        if (entity.isMarkMode() && entity.tickCount % 4 == 0) {
            entity.level().addParticle(
                    net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    entity.getX(), entity.getY() + 0.3, entity.getZ(),
                    0.0, 0.1, 0.0);
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
