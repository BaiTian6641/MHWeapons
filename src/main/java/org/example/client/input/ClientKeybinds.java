package org.example.client.input;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ClientKeybinds {
    public static final String CATEGORY = "key.categories.mhweaponsmod";
    public static final KeyMapping FOCUS_MODE = new KeyMapping(
            "key.mhweaponsmod.focus_mode",
            GLFW.GLFW_KEY_LEFT_ALT,
            CATEGORY
    );
    public static final KeyMapping DODGE = new KeyMapping(
        "key.mhweaponsmod.dodge",
        GLFW.GLFW_KEY_SPACE,
        CATEGORY
    );
        public static final KeyMapping GUARD = new KeyMapping(
        "key.mhweaponsmod.guard",
                GLFW.GLFW_KEY_LEFT_CONTROL,
        CATEGORY
    );
    public static final KeyMapping SHEATHE = new KeyMapping(
        "key.mhweaponsmod.sheathe",
        GLFW.GLFW_KEY_R,
        CATEGORY
    );
        public static final KeyMapping SPECIAL_ACTION = new KeyMapping(
        "key.mhweaponsmod.special_action",
                GLFW.GLFW_KEY_F,
        CATEGORY
    );
        public static final KeyMapping WEAPON_ACTION = new KeyMapping(
        "key.mhweaponsmod.weapon_action",
                GLFW.GLFW_KEY_X,
        CATEGORY
    );
        public static final KeyMapping WEAPON_ACTION_ALT = new KeyMapping(
        "key.mhweaponsmod.weapon_action_alt",
                GLFW.GLFW_KEY_C,
        CATEGORY
    );
        public static final KeyMapping KINSECT_LAUNCH = new KeyMapping(
            "key.mhweaponsmod.kinsect_launch",
                    GLFW.GLFW_KEY_V,
            CATEGORY
        );
        public static final KeyMapping KINSECT_RECALL = new KeyMapping(
            "key.mhweaponsmod.kinsect_recall",
                    GLFW.GLFW_KEY_B,
            CATEGORY
        );

    private ClientKeybinds() {
    }
}
