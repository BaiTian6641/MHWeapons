package org.example.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.MHWeaponsMod;

@Mod.EventBusSubscriber(modid = MHWeaponsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DamageNumberRenderEvents {
    private static final int FULL_BRIGHT = 0xF000F0;

    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        if (DamageNumberClientTracker.getEntries().isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        Font font = mc.font;
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        for (DamageNumberClientTracker.Entry entry : DamageNumberClientTracker.getEntries()) {
            Vec3 pos = entry.getPos();
            double x = pos.x - camPos.x;
            double y = pos.y - camPos.y;
            double z = pos.z - camPos.z;

            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.mulPose(camera.rotation());
            float scale = 0.025f;
            poseStack.scale(-scale, -scale, scale);
            int width = font.width(entry.getText());

            font.drawInBatch(entry.getText(), -width / 2.0f, 0.0f, 0xFFFFFF, false,
                poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, FULL_BRIGHT);
            font.drawInBatch(entry.getText(), -width / 2.0f, 0.0f, 0xFFFFFF, false,
                poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);

            poseStack.popPose();
        }
        poseStack.popPose();
        buffer.endBatch();
    }

    private DamageNumberRenderEvents() {
    }
}