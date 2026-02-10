package org.example.common.util;

import com.mojang.logging.LogUtils;
import org.example.common.config.MHWeaponsConfig;
import org.slf4j.Logger;

public final class DebugLogger {
    private static final Logger LOGGER = LogUtils.getLogger();

    private DebugLogger() {}

    public static void logCombat(String message, Object... params) {
        if (MHWeaponsConfig.LOG_COMBAT_EVENTS.get()) {
            LOGGER.info("[COMBAT] " + message, params);
        }
    }

    public static void logInput(String message, Object... params) {
        if (MHWeaponsConfig.LOG_INPUT_EVENTS.get()) {
            LOGGER.info("[INPUT] " + message, params);
        }
    }

    public static void logState(String message, Object... params) {
        if (MHWeaponsConfig.LOG_STATE_CHANGES.get()) {
            LOGGER.info("[STATE] " + message, params);
        }
    }

    public static void logWeapon(String message, Object... params) {
        if (MHWeaponsConfig.LOG_WEAPON_ACTIONS.get()) {
            LOGGER.info("[WEAPON] " + message, params);
        }
    }
}
