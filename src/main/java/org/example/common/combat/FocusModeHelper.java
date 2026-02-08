package org.example.common.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Shared Focus Mode helpers so client HUD and server damage use identical tuning.
 */
public final class FocusModeHelper {
    private FocusModeHelper() {
    }

    public enum RangeTier {
        NONE,
        TOO_CLOSE,
        SWEET_SPOT,
        LONG
    }

    // Distance thresholds tuned to feel like Wilds: reward mid-range spacing.
    private static final double TOO_CLOSE_MAX = 2.8; // inside this feels cramped
    private static final double SWEET_MAX = 4.6; // ideal window
    private static final double LONG_MAX = 6.4; // still in control, lighter bonus

    public static RangeTier classify(Player player, LivingEntity target) {
        if (player == null || target == null) {
            return RangeTier.NONE;
        }
        return classifyDistance(player.distanceTo(target));
    }

    public static RangeTier classifyDistance(double distance) {
        if (distance <= 0.0) {
            return RangeTier.NONE;
        }
        if (distance < TOO_CLOSE_MAX) {
            return RangeTier.TOO_CLOSE;
        }
        if (distance <= SWEET_MAX) {
            return RangeTier.SWEET_SPOT;
        }
        if (distance <= LONG_MAX) {
            return RangeTier.LONG;
        }
        return RangeTier.NONE;
    }

    public static float damageBonus(RangeTier tier) {
        return switch (tier) {
            case SWEET_SPOT -> 0.08f; // meaningful but modest bump
            case LONG -> 0.03f; // slight reward if you stay just outside sweet spot
            default -> 0.0f;
        };
    }
}
