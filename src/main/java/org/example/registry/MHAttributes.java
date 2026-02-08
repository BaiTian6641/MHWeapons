package org.example.registry;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MHAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, "mhweaponsmod");

    // Elemental Damage Attributes
    // These behave like Attack Damage but for specific elements.
    // They are "RangedAttribute" (min 0, default 0, max 2048).
    
    public static final RegistryObject<Attribute> FIRE_DAMAGE = ATTRIBUTES.register("fire_damage",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.fire_damage", 0.0D, 0.0D, 2048.0D).setSyncable(true));
            
    public static final RegistryObject<Attribute> WATER_DAMAGE = ATTRIBUTES.register("water_damage",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.water_damage", 0.0D, 0.0D, 2048.0D).setSyncable(true));
            
    public static final RegistryObject<Attribute> THUNDER_DAMAGE = ATTRIBUTES.register("thunder_damage",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.thunder_damage", 0.0D, 0.0D, 2048.0D).setSyncable(true));
            
    public static final RegistryObject<Attribute> ICE_DAMAGE = ATTRIBUTES.register("ice_damage",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.ice_damage", 0.0D, 0.0D, 2048.0D).setSyncable(true));
            
    public static final RegistryObject<Attribute> DRAGON_DAMAGE = ATTRIBUTES.register("dragon_damage",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.dragon_damage", 0.0D, 0.0D, 2048.0D).setSyncable(true));

    // Affinity and critical bonuses
    public static final RegistryObject<Attribute> AFFINITY = ATTRIBUTES.register("affinity",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.affinity", 0.0D, 0.0D, 100.0D).setSyncable(true));

    public static final RegistryObject<Attribute> CRIT_DAMAGE_BONUS = ATTRIBUTES.register("crit_damage_bonus",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.crit_damage_bonus", 0.0D, 0.0D, 2.0D).setSyncable(true));

    public static final RegistryObject<Attribute> ELEMENT_CRIT_BONUS = ATTRIBUTES.register("element_crit_bonus",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.element_crit_bonus", 0.0D, 0.0D, 2.0D).setSyncable(true));

    public static final RegistryObject<Attribute> STATUS_CRIT_BONUS = ATTRIBUTES.register("status_crit_bonus",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.status_crit_bonus", 0.0D, 0.0D, 2.0D).setSyncable(true));

    // Guard and draw related bonuses
    public static final RegistryObject<Attribute> GUARD_STRENGTH = ATTRIBUTES.register("guard_strength",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.guard_strength", 0.0D, 0.0D, 1.0D).setSyncable(true));

    public static final RegistryObject<Attribute> GUARD_UP = ATTRIBUTES.register("guard_up",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.guard_up", 0.0D, 0.0D, 1.0D).setSyncable(true));

    public static final RegistryObject<Attribute> OFFENSIVE_GUARD_BONUS = ATTRIBUTES.register("offensive_guard_bonus",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.offensive_guard_bonus", 0.0D, 0.0D, 1.0D).setSyncable(true));

    public static final RegistryObject<Attribute> DRAW_AFFINITY_BONUS = ATTRIBUTES.register("draw_affinity_bonus",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.draw_affinity_bonus", 0.0D, 0.0D, 100.0D).setSyncable(true));

    // Mobility and flow
    public static final RegistryObject<Attribute> DODGE_DISTANCE_BONUS = ATTRIBUTES.register("dodge_distance_bonus",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.dodge_distance_bonus", 0.0D, 0.0D, 5.0D).setSyncable(true));

    public static final RegistryObject<Attribute> SHEATHE_SPEED_BONUS = ATTRIBUTES.register("sheathe_speed_bonus",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.sheathe_speed_bonus", 0.0D, 0.0D, 5.0D).setSyncable(true));

    // Sharpness and weapon flow
    public static final RegistryObject<Attribute> SHARPNESS_MAX_BONUS = ATTRIBUTES.register("sharpness_max_bonus",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.sharpness_max_bonus", 0.0D, 0.0D, 1.0D).setSyncable(true));

    public static final RegistryObject<Attribute> SHARPNESS_LOSS_MULTIPLIER = ATTRIBUTES.register("sharpness_loss_multiplier",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.sharpness_loss_multiplier", 0.0D, 0.0D, 1.0D).setSyncable(true));

    public static final RegistryObject<Attribute> SHARPNESS_PROTECT_CHANCE = ATTRIBUTES.register("sharpness_protect_chance",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.sharpness_protect_chance", 0.0D, 0.0D, 1.0D).setSyncable(true));

    public static final RegistryObject<Attribute> SHARPNESS_CRIT_PROTECT_CHANCE = ATTRIBUTES.register("sharpness_crit_protect_chance",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.sharpness_crit_protect_chance", 0.0D, 0.0D, 1.0D).setSyncable(true));

    public static final RegistryObject<Attribute> BLUDGEONER_BONUS = ATTRIBUTES.register("bludgeoner_bonus",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.bludgeoner_bonus", 0.0D, 0.0D, 1.0D).setSyncable(true));

    // Environmental resistances and stamina tuning
    public static final RegistryObject<Attribute> EARPLUGS = ATTRIBUTES.register("earplugs",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.earplugs", 0.0D, 0.0D, 5.0D).setSyncable(true));

    public static final RegistryObject<Attribute> WINDPROOF = ATTRIBUTES.register("windproof",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.windproof", 0.0D, 0.0D, 5.0D).setSyncable(true));

    public static final RegistryObject<Attribute> TREMOR_RESIST = ATTRIBUTES.register("tremor_resist",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.tremor_resist", 0.0D, 0.0D, 5.0D).setSyncable(true));

    public static final RegistryObject<Attribute> STAMINA_MAX_BONUS = ATTRIBUTES.register("stamina_max_bonus",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.stamina_max_bonus", 0.0D, 0.0D, 200.0D).setSyncable(true));

    public static final RegistryObject<Attribute> STAMINA_REGEN_BONUS = ATTRIBUTES.register("stamina_regen_bonus",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.stamina_regen_bonus", 0.0D, 0.0D, 10.0D).setSyncable(true));

    public static final RegistryObject<Attribute> STAMINA_COST_REDUCTION = ATTRIBUTES.register("stamina_cost_reduction",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.stamina_cost_reduction", 0.0D, 0.0D, 1.0D).setSyncable(true));

    // Support skills that share buffs with allies (Wide-Range style)
    public static final RegistryObject<Attribute> WIDE_RANGE = ATTRIBUTES.register("wide_range",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.wide_range", 0.0D, 0.0D, 5.0D).setSyncable(true));

    // Bow coating enables
    public static final RegistryObject<Attribute> COATING_POISON = ATTRIBUTES.register("coating_poison",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.coating_poison", 0.0D, 0.0D, 1.0D).setSyncable(true));

    public static final RegistryObject<Attribute> COATING_PARALYSIS = ATTRIBUTES.register("coating_paralysis",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.coating_paralysis", 0.0D, 0.0D, 1.0D).setSyncable(true));

    public static final RegistryObject<Attribute> COATING_SLEEP = ATTRIBUTES.register("coating_sleep",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.coating_sleep", 0.0D, 0.0D, 1.0D).setSyncable(true));

    // Status Ailment Buildup Attributes
    public static final RegistryObject<Attribute> POISON_BUILDUP = ATTRIBUTES.register("poison_buildup",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.poison_buildup", 0.0D, 0.0D, 2048.0D).setSyncable(true));

    public static final RegistryObject<Attribute> PARALYSIS_BUILDUP = ATTRIBUTES.register("paralysis_buildup",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.paralysis_buildup", 0.0D, 0.0D, 2048.0D).setSyncable(true));

    public static final RegistryObject<Attribute> SLEEP_BUILDUP = ATTRIBUTES.register("sleep_buildup",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.sleep_buildup", 0.0D, 0.0D, 2048.0D).setSyncable(true));

    public static final RegistryObject<Attribute> BLAST_BUILDUP = ATTRIBUTES.register("blast_buildup",
            () -> new RangedAttribute("attribute.name.mhweaponsmod.blast_buildup", 0.0D, 0.0D, 2048.0D).setSyncable(true));

    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
    }
}
