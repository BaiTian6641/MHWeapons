package org.example.common.events;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.example.registry.MHAttributes;

public final class EnvironmentalResistanceEvents {

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        var source = event.getSource();
        float earplugs = getAttrSafe(player, MHAttributes.EARPLUGS.get());
        float windproof = getAttrSafe(player, MHAttributes.WINDPROOF.get());
        float tremor = getAttrSafe(player, MHAttributes.TREMOR_RESIST.get());

        if (source.is(DamageTypes.SONIC_BOOM)) {
            if (earplugs >= 1.0f) {
                event.setCanceled(true);
                return;
            }
            if (earplugs > 0.0f) {
                float reduction = Math.min(0.8f, earplugs * 0.25f);
                event.setAmount(event.getAmount() * (1.0f - reduction));
            }
        }

        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            if (windproof > 0.0f) {
                float reduction = Math.min(0.6f, windproof * 0.15f);
                event.setAmount(event.getAmount() * (1.0f - reduction));
            }
        }

        if (source.is(DamageTypeTags.IS_FALL)) {
            if (tremor > 0.0f) {
                float reduction = Math.min(0.75f, tremor * 0.20f);
                event.setAmount(event.getAmount() * (1.0f - reduction));
            }
        }
    }

    @SubscribeEvent
    public void onKnockBack(LivingKnockBackEvent event) {
        LivingEntity living = event.getEntity();
        if (!(living instanceof Player player)) {
            return;
        }
        float earplugs = getAttrSafe(player, MHAttributes.EARPLUGS.get());
        float windproof = getAttrSafe(player, MHAttributes.WINDPROOF.get());
        float tremor = getAttrSafe(player, MHAttributes.TREMOR_RESIST.get());

        float multiplier = 1.0f;
        if (earplugs > 0.0f) {
            multiplier *= (1.0f - Math.min(0.35f, earplugs * 0.08f));
        }
        if (windproof > 0.0f) {
            multiplier *= (1.0f - Math.min(0.50f, windproof * 0.12f));
        }
        if (tremor > 0.0f && living.onGround()) {
            multiplier *= (1.0f - Math.min(0.40f, tremor * 0.10f));
        }

        if (multiplier <= 0.05f) {
            event.setStrength(0.0f);
            event.setCanceled(true);
            return;
        }
        event.setStrength(event.getStrength() * multiplier);
    }

    private float getAttrSafe(LivingEntity entity, Attribute attribute) {
        if (entity == null || attribute == null) {
            return 0.0f;
        }
        var inst = entity.getAttribute(attribute);
        return inst == null ? 0.0f : (float) inst.getValue();
    }
}
