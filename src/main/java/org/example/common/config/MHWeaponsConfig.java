package org.example.common.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MHWeaponsConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_ATTACK_HUD;
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.IntValue KINSECT_MAX_EXTRACTS;
    public static final ForgeConfigSpec.DoubleValue KINSECT_SPEED;
    public static final ForgeConfigSpec.DoubleValue KINSECT_RANGE;
    public static final ForgeConfigSpec.DoubleValue KINSECT_DAMAGE;
    public static final ForgeConfigSpec.EnumValue<org.example.common.combat.MHDamageType> KINSECT_DAMAGE_TYPE;
    public static final ForgeConfigSpec.ConfigValue<String> KINSECT_ELEMENT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("hud");
        SHOW_ATTACK_HUD = builder
                .comment("Show attack HUD on the top-right")
                .define("showAttackHud", true);
        builder.pop();
        CLIENT_SPEC = builder.build();

        ForgeConfigSpec.Builder commonBuilder = new ForgeConfigSpec.Builder();
        commonBuilder.push("kinsect");
        KINSECT_MAX_EXTRACTS = commonBuilder
            .comment("How many extracts a kinsect can hold")
            .defineInRange("maxExtracts", 1, 1, 3);
        KINSECT_SPEED = commonBuilder
            .comment("Kinsect flight speed")
            .defineInRange("speed", 0.9, 0.1, 5.0);
        KINSECT_RANGE = commonBuilder
            .comment("Max distance kinsect can fly from player")
            .defineInRange("range", 20.0, 2.0, 64.0);
        KINSECT_DAMAGE = commonBuilder
            .comment("Kinsect damage on hit")
            .defineInRange("damage", 2.0, 0.0, 20.0);
        KINSECT_DAMAGE_TYPE = commonBuilder
            .comment("Kinsect damage type")
            .defineEnum("damageType", org.example.common.combat.MHDamageType.SEVER);
        KINSECT_ELEMENT = commonBuilder
            .comment("Kinsect element (placeholder)")
            .define("element", "none");
        commonBuilder.pop();
        COMMON_SPEC = commonBuilder.build();
    }

    private MHWeaponsConfig() {
    }
}
