package org.example.common.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.example.common.capability.CommonCapabilities;
import org.example.common.capability.mob.MobWoundState;
import org.example.common.capability.mob.MobStatusState;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;

public final class CapabilityUtil {
    private CapabilityUtil() {
    }

    @SuppressWarnings("null")
    public static PlayerCombatState getPlayerCombatState(Player player) {
        return player.getCapability(CommonCapabilities.PLAYER_COMBAT).orElse(null);
    }

    @SuppressWarnings("null")
    public static PlayerWeaponState getPlayerWeaponState(Player player) {
        return player.getCapability(CommonCapabilities.PLAYER_WEAPON).orElse(null);
    }

    @SuppressWarnings("null")
    public static MobWoundState getMobWoundState(LivingEntity entity) {
        return entity.getCapability(CommonCapabilities.MOB_WOUND).orElse(null);
    }

    @SuppressWarnings("null")
    public static MobStatusState getMobStatusState(LivingEntity entity) {
        return entity.getCapability(CommonCapabilities.MOB_STATUS).orElse(null);
    }
}
