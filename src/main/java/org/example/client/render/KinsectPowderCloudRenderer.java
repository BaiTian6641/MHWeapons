package org.example.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.example.common.entity.KinsectPowderCloudEntity;

/**
 * Renderer for Kinsect Powder Clouds.
 * The clouds are entirely particle-based (handled in the entity's tick method),
 * so this renderer is intentionally minimal — no model, just particles.
 */
@SuppressWarnings("null")
public class KinsectPowderCloudRenderer extends EntityRenderer<KinsectPowderCloudEntity> {
    public KinsectPowderCloudRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(KinsectPowderCloudEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/particle/generic_0.png");
    }

    @Override
    public void render(KinsectPowderCloudEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Powder clouds are purely particle-based, no model rendering needed.
        // The entity's tick() spawns idle particles client-side.
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
