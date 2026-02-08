package org.example.common.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import org.example.common.capability.mob.MobWoundState;
import org.example.common.capability.mob.MobStatusState;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;

public final class CommonCapabilities {
    public static final Capability<PlayerCombatState> PLAYER_COMBAT = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final Capability<PlayerWeaponState> PLAYER_WEAPON = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final Capability<MobWoundState> MOB_WOUND = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final Capability<MobStatusState> MOB_STATUS = CapabilityManager.get(new CapabilityToken<>() {
    });

    private CommonCapabilities() {
    }
}
