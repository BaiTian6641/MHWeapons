package org.example.common.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.registry.MHAttributes;

/**
 * Centralised helpers for stamina math so skills like Marathon Runner / Constitution can hook cleanly.
 */
public final class StaminaHelper {
    private StaminaHelper() {
    }

    /**
     * Applies cost reduction attributes to a stamina cost. Returns the adjusted cost (always >= 0).
     */
    public static float applyCost(Player player, float baseCost) {
        if (player == null) {
            return Math.max(0.0f, baseCost);
        }
        float reduction = clamp01(getAttrSafe(player, MHAttributes.STAMINA_COST_REDUCTION.get()));
        reduction = Math.min(reduction, 0.9f); // Never drop costs to zero entirely.
        float adjusted = baseCost * (1.0f - reduction);
        return Math.max(0.0f, adjusted);
    }

    /**
     * Resolves the player's effective max stamina (base + bonuses).
     */
    public static float resolveMax(Player player, float baseMax) {
        float bonus = player == null ? 0.0f : getAttrSafe(player, MHAttributes.STAMINA_MAX_BONUS.get());
        return Math.max(1.0f, baseMax + bonus);
    }

    /**
     * Gets regen per tick using the base value plus any bonuses and temporary horn boosts.
     */
    public static float resolveRegenPerTick(Player player, PlayerWeaponState state, float baseRegen) {
        float regen = baseRegen;
        if (player != null) {
            regen += getAttrSafe(player, MHAttributes.STAMINA_REGEN_BONUS.get());
        }
        if (state != null && state.getHornStaminaBoostTicks() > 0) {
            regen += 1.0f;
        }
        return Math.max(0.0f, regen);
    }

    private static float getAttrSafe(LivingEntity entity, Attribute attribute) {
        if (entity == null || attribute == null) {
            return 0.0f;
        }
        var inst = entity.getAttribute(attribute);
        return inst == null ? 0.0f : (float) inst.getValue();
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
