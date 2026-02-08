package org.example.common.util;

import net.minecraft.world.phys.Vec3;

public final class MathUtil {
    private MathUtil() {
    }

    public static float dot(Vec3 a, Vec3 b) {
        return (float) (a.x * b.x + a.y * b.y + a.z * b.z);
    }
}
