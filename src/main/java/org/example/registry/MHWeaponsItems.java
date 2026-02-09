package org.example.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.example.MHWeaponsMod;
import org.example.common.combat.MHDamageType;
import org.example.item.AccessoryItem;
import org.example.item.BlockableWeaponItem;
import org.example.item.BowWeaponItem;
import org.example.item.DecorationJewelItem;
import org.example.item.GreatSwordItem;
import org.example.item.GeoWeaponItem;
import org.example.item.WhetstoneItem;
import org.example.item.KinsectItem;
import org.example.item.MHTiers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.example.common.entity.KinsectEntity;
import org.example.common.entity.KinsectPowderCloudEntity;
import org.example.common.entity.EchoBubbleEntity;
import java.util.HashMap;
import java.util.Map;

public final class MHWeaponsItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MHWeaponsMod.MODID);

            // PLACE HOLDER: Temporary weapon stats for early testing.
            // All weapons are wired to GeckoLib placeholders; replace models/animations later.
    public static final RegistryObject<Item> GREATSWORD = ITEMS.register("greatsword",
                    () -> new GreatSwordItem(Tiers.NETHERITE, 8, -3.4f, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> LONGSWORD = ITEMS.register("longsword",
            () -> new GeoWeaponItem("longsword", MHDamageType.SEVER, MHTiers.BLACK, 6, -2.8f, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SWORD_AND_SHIELD = ITEMS.register("sword_and_shield",
            () -> new BlockableWeaponItem("sword_and_shield", MHDamageType.SEVER, Tiers.NETHERITE, 5, -2.4f, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DUAL_BLADES = ITEMS.register("dual_blades",
            () -> new GeoWeaponItem("dual_blades", MHDamageType.SEVER, Tiers.NETHERITE, 4, -1.9f, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> HAMMER = ITEMS.register("hammer",
            () -> new GeoWeaponItem("hammer", MHDamageType.BLUNT, Tiers.NETHERITE, 7, -3.2f, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> HUNTING_HORN = ITEMS.register("hunting_horn",
            () -> new GeoWeaponItem("hunting_horn", MHDamageType.BLUNT, Tiers.NETHERITE, 6, -3.0f, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> LANCE = ITEMS.register("lance",
            () -> new BlockableWeaponItem("lance", MHDamageType.SEVER, Tiers.NETHERITE, 5, -3.1f, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GUNLANCE = ITEMS.register("gunlance",
            () -> new org.example.item.GunlanceItem(Tiers.NETHERITE, 6, -3.2f, new Item.Properties().stacksTo(1), org.example.item.GunlanceItem.ShellingType.NORMAL, 5));

    public static final RegistryObject<Item> SWITCH_AXE = ITEMS.register("switch_axe",
            () -> new GeoWeaponItem("switch_axe", MHDamageType.SEVER, Tiers.NETHERITE, 7, -3.0f, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> CHARGE_BLADE = ITEMS.register("charge_blade",
            () -> new BlockableWeaponItem("charge_blade", MHDamageType.SEVER, Tiers.NETHERITE, 7, -2.9f, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> INSECT_GLAIVE = ITEMS.register("insect_glaive",
            () -> new GeoWeaponItem("insect_glaive", MHDamageType.SEVER, Tiers.NETHERITE, 5, -2.7f, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> TONFA = ITEMS.register("tonfa",
            () -> new GeoWeaponItem("tonfa", MHDamageType.BLUNT, Tiers.NETHERITE, 4, -1.6f, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MAGNET_SPIKE = ITEMS.register("magnet_spike",
            () -> new GeoWeaponItem("magnet_spike", MHDamageType.SEVER, MHTiers.PURPLE, 8, -3.4f, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ACCEL_AXE = ITEMS.register("accel_axe",
            () -> new GeoWeaponItem("accel_axe", MHDamageType.SEVER, Tiers.NETHERITE, 7, -3.0f, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BOW = ITEMS.register("bow",
            () -> new BowWeaponItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> WHETSTONE = ITEMS.register("whetstone",
            () -> new WhetstoneItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> KINSECT_BASIC = ITEMS.register("kinsect_basic",
            () -> new KinsectItem(new Item.Properties().stacksTo(1), 0, 0.0, 0.0, 0.0f, null, null));

    // Curios Accessories (Placeholder system)
    public static final RegistryObject<Item> POWER_CHARM = ITEMS.register("power_charm",
            () -> new AccessoryItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DEFENSE_CHARM = ITEMS.register("defense_charm",
            () -> new AccessoryItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ELEMENT_CHARM = ITEMS.register("element_charm",
            () -> new AccessoryItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> STATUS_CHARM = ITEMS.register("status_charm",
            () -> new AccessoryItem(new Item.Properties().stacksTo(1)));

    // Decorations (Jewels)
    // Most jewels are registered in the WILDS_JEWELS map below.

    public static final Map<String, RegistryObject<Item>> WILDS_JEWELS = registerWildsJewels(new String[] {
            "artillery_jewel",
            "bandolier_jewel",
            "blastcoat_jewel",
            "blunt_jewel",
            "charge_jewel",
            "charge_up_jewel",
            "crit_element_jewel",
            "crit_status_jewel",
            "critical_jewel",
            "drain_jewel",
            "draincoat_jewel",
            "draw_jewel",
            "enhancer_jewel",
            "expert_jewel",
            "focus_jewel",
            "flight_jewel",
            "forceshot_jewel",
            "gambit_jewel",
            "grinder_jewel",
            "guardian_jewel",
            "handicraft_jewel",
            "ironwall_jewel",
            "ko_jewel",
            "magazine_jewel",
            "mastery_jewel",
            "minds_eye_jewel",
            "opener_jewel",
            "paracoat_jewel",
            "pierce_jewel",
            "poisoncoat_jewel",
            "precise_jewel",
            "quickswitch_jewel",
            "razor_sharp_jewel",
            "salvo_jewel",
            "sharp_jewel",
            "shield_jewel",
            "sleepcoat_jewel",
            "sonorous_jewel",
            "spread_jewel",
            "trueshot_jewel",
            "venom_jewel",
            "adapt_jewel",
            "ambush_jewel",
            "antiblast_jewel",
            "antidote_jewel",
            "antipara_jewel",
            "bomber_jewel",
            "botany_jewel",
            "brace_jewel",
            "chain_jewel",
            "challenger_jewel",
            "climber_jewel",
            "counter_jewel",
            "counterattack_jewel",
            "def_lock_jewel",
            "destroyer_jewel",
            "dive_jewel",
            "dragon_res_jewel",
            "earplugs_jewel",
            "enduring_jewel",
            "escape_jewel",
            "fire_res_jewel",
            "flash_jewel",
            "flawless_jewel",
            "flayer_jewel",
            "footing_jewel",
            "foray_jewel",
            "friendship_jewel",
            "fungiform_jewel",
            "furor_jewel",
            "geology_jewel",
            "gobbler_jewel",
            "growth_jewel",
            "hungerless_jewel",
            "ice_res_jewel",
            "intimidator_jewel",
            "jumping_jewel",
            "leap_jewel",
            "maintenance_jewel",
            "medicine_jewel",
            "mighty_jewel",
            "mirewalker_jewel",
            "pep_jewel",
            "perfume_jewel",
            "phoenix_jewel",
            "physique_jewel",
            "potential_jewel",
            "protection_jewel",
            "ranger_jewel",
            "recovery_jewel",
            "refresh_jewel",
            "sane_jewel",
            "satiated_jewel",
            "sheath_jewel",
            "shockproof_jewel",
            "specimen_jewel",
            "sprinter_jewel",
            "steadfast_jewel",
            "survival_jewel",
            "suture_jewel",
            "tenderizer_jewel",
            "throttle_jewel",
            "thunder_res_jewel",
            "water_res_jewel",
            "wind_resist_jewel",
            "wide_range_jewel",
            // Added jewels that were previously manually registered
            "attack_jewel",
            "blaze_jewel",
            "stream_jewel",
            "bolt_jewel",
            "frost_jewel",
            "dragon_jewel",
            "blast_jewel",
            "paralyzer_jewel",
            "sleep_jewel",
            "defense_jewel",
            "vitality_jewel",
            "evasion_jewel",
            // Missing legacy jewels brought back for compatibility
            "armor_jewel",
            "critical_eye_jewel",
            "fire_jewel",
            "poison_jewel"
    });

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MHWeaponsMod.MODID);

    public static final RegistryObject<EntityType<KinsectEntity>> KINSECT = ENTITIES.register("kinsect",
            () -> EntityType.Builder.<KinsectEntity>of(KinsectEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .build("kinsect"));

    public static final RegistryObject<EntityType<EchoBubbleEntity>> ECHO_BUBBLE = ENTITIES.register("echo_bubble",
            () -> EntityType.Builder.<EchoBubbleEntity>of(EchoBubbleEntity::new, MobCategory.MISC)
                    .sized(1.5f, 0.5f)
                    .build("echo_bubble"));

    public static final RegistryObject<EntityType<KinsectPowderCloudEntity>> KINSECT_POWDER_CLOUD = ENTITIES.register("kinsect_powder_cloud",
            () -> EntityType.Builder.<KinsectPowderCloudEntity>of(KinsectPowderCloudEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .fireImmune()
                    .build("kinsect_powder_cloud"));

        private static Map<String, RegistryObject<Item>> registerWildsJewels(String[] ids) {
                Map<String, RegistryObject<Item>> map = new HashMap<>();
                for (String id : ids) {
                        map.put(id, ITEMS.register(id, () -> new DecorationJewelItem(new Item.Properties())));
                }
                return map;
        }

    private MHWeaponsItems() {
    }
}
