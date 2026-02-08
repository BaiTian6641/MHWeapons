package org.example.common.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.example.MHWeaponsMod;
import org.example.common.capability.CommonCapabilities;
import org.example.common.capability.mob.MobStatusState;
import org.example.common.capability.mob.MobStatusStateProvider;
import org.example.common.capability.mob.MobWoundStateProvider;
import org.example.common.capability.player.PlayerCombatStateProvider;
import org.example.common.capability.player.PlayerWeaponStateProvider;
import org.example.common.combat.MHDamageTypeProvider;
import org.example.common.util.CapabilityUtil;

public final class CommonEvents {
    private static final ResourceLocation PLAYER_COMBAT_ID = ResourceLocation.fromNamespaceAndPath(MHWeaponsMod.MODID, "player_combat");
    private static final ResourceLocation PLAYER_WEAPON_ID = ResourceLocation.fromNamespaceAndPath(MHWeaponsMod.MODID, "player_weapon");
    private static final ResourceLocation MOB_WOUND_ID = ResourceLocation.fromNamespaceAndPath(MHWeaponsMod.MODID, "mob_wound");
    private static final ResourceLocation MOB_STATUS_ID = ResourceLocation.fromNamespaceAndPath(MHWeaponsMod.MODID, "mob_status");

    private static final float STATUS_DECAY_PER_TICK = 0.15f;

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof Player) {
            event.addCapability(PLAYER_COMBAT_ID, new PlayerCombatStateProvider());
            event.addCapability(PLAYER_WEAPON_ID, new PlayerWeaponStateProvider());
            return;
        }
        if (entity instanceof LivingEntity) {
            event.addCapability(MOB_WOUND_ID, new MobWoundStateProvider());
            event.addCapability(MOB_STATUS_ID, new MobStatusStateProvider());
        }
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (event.getEntity() instanceof Player) {
            return;
        }
        MobStatusState status = CapabilityUtil.getMobStatusState(event.getEntity());
        if (status == null) {
            return;
        }
        status.setPoisonBuildup(decay(status.getPoisonBuildup()));
        status.setParalysisBuildup(decay(status.getParalysisBuildup()));
        status.setSleepBuildup(decay(status.getSleepBuildup()));
        status.setBlastBuildup(decay(status.getBlastBuildup()));
    }

    private float decay(float value) {
        float v = value - STATUS_DECAY_PER_TICK;
        return Math.max(0.0f, v);
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(CommonCapabilities.PLAYER_COMBAT).ifPresent(original ->
                event.getEntity().getCapability(CommonCapabilities.PLAYER_COMBAT).ifPresent(clone -> {
                    clone.setDodgeIFrameTicks(original.getDodgeIFrameTicks());
                    clone.setGuardPointActive(original.isGuardPointActive());
                }));
        event.getOriginal().getCapability(CommonCapabilities.PLAYER_WEAPON).ifPresent(original ->
            event.getEntity().getCapability(CommonCapabilities.PLAYER_WEAPON).ifPresent(clone -> clone.copyFrom(original)));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() == InteractionHand.MAIN_HAND && tryUseOffhandWhetstone(event)) {
            return;
        }
        if (event.getHand() != InteractionHand.OFF_HAND) {
            return;
        }
        Player player = event.getEntity();
        if (player.getMainHandItem().getItem() instanceof MHDamageTypeProvider
                && !isAllowedOffhandItem(player)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() == InteractionHand.MAIN_HAND && tryUseOffhandWhetstone(event)) {
            return;
        }
        if (event.getHand() != InteractionHand.OFF_HAND) {
            return;
        }
        Player player = event.getEntity();
        if (player.getMainHandItem().getItem() instanceof MHDamageTypeProvider
                && !isAllowedOffhandItem(player)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    private boolean isAllowedOffhandItem(Player player) {
        var offhandItem = player.getOffhandItem().getItem();
        return offhandItem instanceof org.example.item.WhetstoneItem
                || offhandItem instanceof org.example.item.KinsectItem;
    }

    private boolean tryUseOffhandWhetstone(PlayerInteractEvent event) {
        Player player = event.getEntity();
        if (!(player.getMainHandItem().getItem() instanceof MHDamageTypeProvider)) {
            return false;
        }
        if (!(player.getOffhandItem().getItem() instanceof org.example.item.WhetstoneItem whetstone)) {
            return false;
        }
        var result = whetstone.use(event.getLevel(), player, InteractionHand.OFF_HAND);
        event.setCanceled(true);
        event.setCancellationResult(result.getResult());
        return true;
    }
}
