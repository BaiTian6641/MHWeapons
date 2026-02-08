package org.example.common.combat.action;

import net.minecraft.world.entity.LivingEntity;
import org.example.common.combat.CombatReferee;

public final class ActionMotionValue {
    private ActionMotionValue() {
    }

    public static void set(LivingEntity entity, float motionValue) {
        if (motionValue <= 0.0f) {
            return;
        }
        entity.getPersistentData().putFloat(CombatReferee.MOTION_VALUE_KEY, motionValue);
    }

    public static void clear(LivingEntity entity) {
        entity.getPersistentData().remove(CombatReferee.MOTION_VALUE_KEY);
    }
}
