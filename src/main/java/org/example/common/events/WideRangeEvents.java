package org.example.common.events;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.example.registry.MHAttributes;

/**
 * Shares beneficial effects to nearby allies based on the Wide-Range attribute.
 */
public final class WideRangeEvents {

    private static final String TAG_SHARING = "mh_wide_range_sharing";

    @SubscribeEvent
    @SuppressWarnings("null")
    public void onEffectAdded(MobEffectEvent.Added event) {
        LivingEntity living = event.getEntity();
        if (!(living instanceof Player source)) {
            return;
        }
        MobEffectInstance incoming = event.getEffectInstance();
        if (incoming == null || !incoming.getEffect().isBeneficial()) {
            return;
        }
        if (incoming.isAmbient()) {
            return; // Treat ambient effects (beacons/shared) as non-shareable to avoid loops.
        }
        var tag = source.getPersistentData();
        if (tag.getBoolean(TAG_SHARING)) {
            return; // Prevent loops from re-applied shared effects.
        }

        float level = getAttrSafe(source, MHAttributes.WIDE_RANGE.get());
        if (level < 1.0f) {
            return;
        }

        int stage = Mth.clamp(Mth.floor(level), 1, 4); // 4-stage cap.
        float shareFactor;
        double radius;
        switch (stage) {
            case 1 -> { shareFactor = 0.15f; radius = 6.0; }
            case 2 -> { shareFactor = 0.45f; radius = 12.0; }
            case 3 -> { shareFactor = 0.75f; radius = 16.0; }
            default -> { shareFactor = 1.0f; radius = 24.0; }
        }

        int sharedDuration = Math.max(20, Math.round(incoming.getDuration() * shareFactor));
        if (sharedDuration <= 20) {
            return; // Too short to matter.
        }
        int sharedAmplifier = Math.max(0, incoming.getAmplifier());

        AABB box = new AABB(source.blockPosition()).inflate(radius, radius, radius);

        tag.putBoolean(TAG_SHARING, true);
        try {
            for (Player ally : source.level().getEntitiesOfClass(Player.class, box)) {
                if (ally == source || !ally.isAlive()) {
                    continue;
                }
                // Avoid overwriting stronger/longer existing buffs.
                MobEffectInstance existing = ally.getEffect(incoming.getEffect());
                if (existing != null && existing.getAmplifier() >= sharedAmplifier && existing.getDuration() >= sharedDuration) {
                    continue;
                }
                MobEffectInstance copy = new MobEffectInstance(
                    incoming.getEffect(),
                    sharedDuration,
                    sharedAmplifier,
                    true, // ambient marks this as shared to prevent re-sharing chains.
                    incoming.isVisible(),
                    incoming.showIcon()
                );
                ally.addEffect(copy);
            }
        } finally {
            tag.remove(TAG_SHARING);
        }
    }

    private float getAttrSafe(LivingEntity entity, Attribute attribute) {
        if (entity == null || attribute == null) {
            return 0.0f;
        }
        var inst = entity.getAttribute(attribute);
        return inst == null ? 0.0f : (float) inst.getValue();
    }
}
