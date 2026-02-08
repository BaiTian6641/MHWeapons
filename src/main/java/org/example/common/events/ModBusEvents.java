package org.example.common.events;

import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.MHWeaponsMod;
import org.example.registry.MHAttributes;

@Mod.EventBusSubscriber(modid = MHWeaponsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBusEvents {
    @SubscribeEvent
    public static void onAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, MHAttributes.FIRE_DAMAGE.get());
        event.add(EntityType.PLAYER, MHAttributes.WATER_DAMAGE.get());
        event.add(EntityType.PLAYER, MHAttributes.THUNDER_DAMAGE.get());
        event.add(EntityType.PLAYER, MHAttributes.ICE_DAMAGE.get());
        event.add(EntityType.PLAYER, MHAttributes.DRAGON_DAMAGE.get());
        event.add(EntityType.PLAYER, MHAttributes.POISON_BUILDUP.get());
        event.add(EntityType.PLAYER, MHAttributes.PARALYSIS_BUILDUP.get());
        event.add(EntityType.PLAYER, MHAttributes.SLEEP_BUILDUP.get());
        event.add(EntityType.PLAYER, MHAttributes.BLAST_BUILDUP.get());
        event.add(EntityType.PLAYER, MHAttributes.AFFINITY.get());
        event.add(EntityType.PLAYER, MHAttributes.CRIT_DAMAGE_BONUS.get());
        event.add(EntityType.PLAYER, MHAttributes.ELEMENT_CRIT_BONUS.get());
        event.add(EntityType.PLAYER, MHAttributes.STATUS_CRIT_BONUS.get());
        event.add(EntityType.PLAYER, MHAttributes.GUARD_STRENGTH.get());
        event.add(EntityType.PLAYER, MHAttributes.GUARD_UP.get());
        event.add(EntityType.PLAYER, MHAttributes.OFFENSIVE_GUARD_BONUS.get());
        event.add(EntityType.PLAYER, MHAttributes.DRAW_AFFINITY_BONUS.get());
        event.add(EntityType.PLAYER, MHAttributes.SHARPNESS_MAX_BONUS.get());
        event.add(EntityType.PLAYER, MHAttributes.SHARPNESS_LOSS_MULTIPLIER.get());
        event.add(EntityType.PLAYER, MHAttributes.SHARPNESS_PROTECT_CHANCE.get());
        event.add(EntityType.PLAYER, MHAttributes.SHARPNESS_CRIT_PROTECT_CHANCE.get());
        event.add(EntityType.PLAYER, MHAttributes.BLUDGEONER_BONUS.get());
        event.add(EntityType.PLAYER, MHAttributes.DODGE_DISTANCE_BONUS.get());
        event.add(EntityType.PLAYER, MHAttributes.SHEATHE_SPEED_BONUS.get());
        event.add(EntityType.PLAYER, MHAttributes.EARPLUGS.get());
        event.add(EntityType.PLAYER, MHAttributes.WINDPROOF.get());
        event.add(EntityType.PLAYER, MHAttributes.TREMOR_RESIST.get());
        event.add(EntityType.PLAYER, MHAttributes.STAMINA_MAX_BONUS.get());
        event.add(EntityType.PLAYER, MHAttributes.STAMINA_REGEN_BONUS.get());
        event.add(EntityType.PLAYER, MHAttributes.STAMINA_COST_REDUCTION.get());
        event.add(EntityType.PLAYER, MHAttributes.WIDE_RANGE.get());
        event.add(EntityType.PLAYER, MHAttributes.COATING_POISON.get());
        event.add(EntityType.PLAYER, MHAttributes.COATING_PARALYSIS.get());
        event.add(EntityType.PLAYER, MHAttributes.COATING_SLEEP.get());
    }
}
