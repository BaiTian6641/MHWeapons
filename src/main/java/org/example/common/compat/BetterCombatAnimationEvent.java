package org.example.common.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

public final class BetterCombatAnimationEvent extends Event {
    private final Player player;
    private final String animationId;

    public BetterCombatAnimationEvent(Player player, String animationId) {
        this.player = player;
        this.animationId = animationId;
    }

    public Player getPlayer() {
        return player;
    }

    public String getAnimationId() {
        return animationId;
    }
}
