package org.example.common.compat;

import net.minecraftforge.fml.ModList;

public final class BetterCombatCompat {
    private static final String MOD_ID = "bettercombat";
    private static final boolean LOADED = ModList.get().isLoaded(MOD_ID);

    private BetterCombatCompat() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }
}
