package org.example.common.compat;

import org.example.MHWeaponsMod;

public final class BetterCombatEventsPlaceholder {
    private BetterCombatEventsPlaceholder() {
    }

    public static void register() {
        if (!BetterCombatCompat.isLoaded()) {
            return;
        }
        MHWeaponsMod.LOGGER.info("Better Combat detected. Placeholder animation bridge is ready.");
    }
}
