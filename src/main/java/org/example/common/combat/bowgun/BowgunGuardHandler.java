package org.example.common.combat.bowgun;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.item.BowgunItem;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Handles guard/block logic for the Bowgun.
 *  - Only available with Shield mod installed and weight class >= Medium.
 *  - Auto-guard for weight >= 70.
 *  - Perfect-guard window (first 6 ticks of guard press).
 */
public final class BowgunGuardHandler {
    private static final Logger LOG = LogUtils.getLogger();

    public static final int PERFECT_GUARD_WINDOW = 6; // ticks
    public static final float PERFECT_GUARD_GAUGE_GAIN = 15.0f;

    private BowgunGuardHandler() {}

    /**
     * Called when GUARD action is pressed/released.
     */
    public static void handleGuard(Player player, boolean pressed, ItemStack stack,
                                    PlayerWeaponState state) {
        if (pressed) {
            // Start guarding
            state.setBowgunGuarding(true);
            state.setBowgunGuardTicks(0);
            LOG.debug("[Bowgun] Guard started");
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.4f, 1.2f);
        } else {
            // Stop guarding
            state.setBowgunGuarding(false);
            state.setBowgunGuardTicks(0);
            LOG.debug("[Bowgun] Guard released");
        }
    }

    /**
     * Tick guard state — increment guard ticks, manage perfect guard window.
     */
    public static void tickGuard(Player player, PlayerWeaponState state) {
        if (!state.isBowgunGuarding()) return;
        int ticks = state.getBowgunGuardTicks();
        state.setBowgunGuardTicks(ticks + 1);
    }

    /**
     * Check if the current guard is within the perfect-guard window.
     */
    public static boolean isPerfectGuard(PlayerWeaponState state) {
        return state.isBowgunGuarding() && state.getBowgunGuardTicks() <= PERFECT_GUARD_WINDOW;
    }

    /**
     * Called when the player is attacked while guarding.
     * Returns true if the attack should be blocked.
     */
    public static boolean onIncomingAttack(Player player, PlayerWeaponState state,
                                            float damage, ItemStack stack) {
        List<String> mods = BowgunItem.getInstalledMods(stack);
        boolean guardEnabled = BowgunModResolver.resolveGuardEnabled(mods);
        if (!guardEnabled) return false;

        // Auto-guard check
        if (state.isBowgunAutoGuard() && !state.isBowgunGuarding()) {
            // Auto-guard triggers for heavy bowguns — reduced effectiveness
            float guardStrength = BowgunModResolver.resolveGuardStrength(mods) / 3.0f; // Normalize 0-3 to 0-1
            float reduced = damage * (1.0f - (guardStrength * 0.5f)); // 50% effectiveness
            LOG.debug("[Bowgun] Auto-guard blocked: raw={} reduced={}", damage, reduced);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.5f, 0.8f);
            return reduced <= 0;
        }

        if (!state.isBowgunGuarding()) return false;

        boolean perfect = isPerfectGuard(state);
        float guardStrength = BowgunModResolver.resolveGuardStrength(mods);

        if (perfect) {
            // Perfect guard — negate all damage, gain gauge
            state.setBowgunGauge(Math.min(BowgunHandler.GAUGE_MAX,
                    state.getBowgunGauge() + PERFECT_GUARD_GAUGE_GAIN));
            LOG.debug("[Bowgun] PERFECT GUARD! Gauge +{}", PERFECT_GUARD_GAUGE_GAIN);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.5f);
            return true;
        }

        // Normal guard — reduce damage based on guard strength
        float knockback = damage * (1.0f - (guardStrength / 3.0f));
        LOG.debug("[Bowgun] Guard blocked: raw={} guardStrength={} knock={}",
                damage, guardStrength, knockback);

        if (knockback > 0) {
            // Small knockback / stamina cost
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.0f);
        }

        return guardStrength >= 2; // Block fully only with high guard strength (2 or 3)
    }
}
