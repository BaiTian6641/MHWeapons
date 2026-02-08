package org.example.client.input;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import org.example.common.capability.mob.MobWoundState;
import org.example.common.combat.FocusModeHelper;
import org.example.common.util.CapabilityUtil;

public final class FocusModeClient {
    private static final Set<Integer> highlighted = new HashSet<>();
    private static boolean enabled;
    private static int focusCrosshairTicks;
    private static FocusModeHelper.RangeTier focusRangeTier = FocusModeHelper.RangeTier.NONE;

    private FocusModeClient() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggle() {
        enabled = !enabled;
        focusCrosshairTicks = enabled ? 40 : 0; // 2s flash
        focusRangeTier = FocusModeHelper.RangeTier.NONE;
    }

    public static void clearHighlights() {
        if (Minecraft.getInstance().level == null) {
            highlighted.clear();
            return;
        }
        highlighted.forEach(id -> {
            var entity = Minecraft.getInstance().level.getEntity(id);
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.setGlowingTag(false);
            }
        });
        highlighted.clear();
    }

    public static void updateHighlights() {
        if (!enabled) {
            clearHighlights();
            focusRangeTier = FocusModeHelper.RangeTier.NONE;
            return;
        }
        var player = Minecraft.getInstance().player;
        var level = Minecraft.getInstance().level;
        if (player == null || level == null) {
            return;
        }

        AABB range = player.getBoundingBox().inflate(16.0);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, range, e -> e != player)) {
            MobWoundState state = CapabilityUtil.getMobWoundState(entity);
            boolean shouldGlow = state != null && state.isWounded();
            if (shouldGlow) {
                entity.setGlowingTag(true);
                highlighted.add(entity.getId());
            } else if (highlighted.remove(entity.getId())) {
                entity.setGlowingTag(false);
            }
        }

        if (focusCrosshairTicks > 0) {
            focusCrosshairTicks--;
        }

        focusRangeTier = FocusModeHelper.RangeTier.NONE;
        var hitResult = Minecraft.getInstance().hitResult;
        if (hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living && living != player) {
            focusRangeTier = FocusModeHelper.classify(player, living);
        }
    }

    public static boolean shouldHighlightCrosshair() {
        return enabled && focusCrosshairTicks > 0;
    }

    public static FocusModeHelper.RangeTier getRangeTier() {
        return enabled ? focusRangeTier : FocusModeHelper.RangeTier.NONE;
    }
}
