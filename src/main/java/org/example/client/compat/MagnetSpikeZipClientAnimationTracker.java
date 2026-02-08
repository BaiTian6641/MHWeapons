package org.example.client.compat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.logic.AnimatedHand;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.example.common.compat.BetterCombatAnimationBridge;

public final class MagnetSpikeZipClientAnimationTracker {
    private static final Map<Integer, Integer> ZIP_TICKS = new HashMap<>();

    private MagnetSpikeZipClientAnimationTracker() {
    }

    public static void start(int playerId, int ticks) {
        if (ticks <= 0) {
            return;
        }
        ZIP_TICKS.put(playerId, ticks);
    }

    public static boolean isActive(int playerId) {
        Integer remaining = ZIP_TICKS.get(playerId);
        return remaining != null && remaining > 0;
    }

    public static void tick() {
        if (ZIP_TICKS.isEmpty()) {
            return;
        }
        var level = Minecraft.getInstance().level;
        if (level == null) {
            ZIP_TICKS.clear();
            return;
        }
        Iterator<Map.Entry<Integer, Integer>> iterator = ZIP_TICKS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                iterator.remove();
                Entity entity = level.getEntity(entry.getKey());
                if (entity instanceof Player player) {
                    BetterCombatAnimationBridge.updateMagnetSpikeZipAnimation(player, false);
                }
            } else {
                entry.setValue(remaining);
                if (remaining % 8 == 0) {
                    Entity entity = level.getEntity(entry.getKey());
                    if (entity instanceof PlayerAttackAnimatable animatable && entity instanceof Player player) {
                        BetterCombatAnimationBridge.updateMagnetSpikeZipAnimation(player, true);
                        animatable.playAttackAnimation("bettercombat:two_handed_spin", AnimatedHand.TWO_HANDED, 1.5f, 0.6f);
                    }
                }
            }
        }
    }
}