package org.example.common.combat;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.combat.StaminaHelper;
import org.example.common.util.CapabilityUtil;
import org.example.item.GeoWeaponItem;
import org.example.registry.MHAttributes;

public final class GuardSystem {
    public boolean tryGuard(Player player, LivingAttackEvent event) {
        return false;
    }

    public boolean applyGuard(Player player, LivingHurtEvent event) {
        PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(player);
        PlayerWeaponState weaponState = CapabilityUtil.getPlayerWeaponState(player);
        boolean guardActive = player.isBlocking();
        if (combatState != null) {
            guardActive |= combatState.isGuardPointActive();
        }
        if (weaponState != null) {
            guardActive |= weaponState.isLanceGuardActive();
        }
        if (!guardActive) {
            return false;
        }

        if (weaponState != null) {
            if (weaponState.getStamina() <= 0.0f) {
                return false;
            }
            float cost = Math.max(6.0f, event.getAmount() * 2.0f);
            cost = StaminaHelper.applyCost(player, cost);
            weaponState.addStamina(-cost);
            weaponState.setStaminaRecoveryDelay(20);
        }

        if (weaponState != null && weaponState.getLancePerfectGuardTicks() > 0) {
            weaponState.setLancePerfectGuardTicks(0);
            event.setAmount(0.0f);
            applyOffensiveGuard(player, true);
            return true;
        }

        float reduced = event.getAmount() * 0.55f;
        float guardStrength = getAttrSafe(player, MHAttributes.GUARD_STRENGTH.get());
        float guardUp = getAttrSafe(player, MHAttributes.GUARD_UP.get());
        if (guardStrength > 0.0f) {
            reduced *= (1.0f - Math.min(guardStrength, 0.35f));
        }
        if (guardUp > 0.0f) {
            reduced *= (1.0f - Math.min(guardUp, 0.25f));
        }
        if (weaponState != null && weaponState.isLancePowerGuard()) {
            reduced = event.getAmount() * 0.35f;
            player.causeFoodExhaustion(0.1f);
        }
        event.setAmount(reduced);

        applyOffensiveGuard(player, combatState != null && combatState.isGuardPointActive());

        if (player.getMainHandItem().getItem() instanceof GeoWeaponItem geoItem
                && "greatsword".equals(geoItem.getWeaponId())) {
            var stack = player.getMainHandItem();
            var tag = stack.getOrCreateTag();
            int sharpness = tag.contains("mh_sharpness") ? tag.getInt("mh_sharpness") : 100;
            sharpness = Math.max(0, sharpness - 2);
            tag.putInt("mh_sharpness", sharpness);
        }
        return true;
    }

    public void tick(Player player) {
    }

    private void applyOffensiveGuard(Player player, boolean perfectGuard) {
        float bonus = getAttrSafe(player, MHAttributes.OFFENSIVE_GUARD_BONUS.get());
        if (bonus <= 0.0f || !perfectGuard) {
            return;
        }
        player.getPersistentData().putInt("mh_offensive_guard_ticks", 60);
    }

    private float getAttrSafe(Player player, net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        if (player == null || attribute == null) {
            return 0.0f;
        }
        var inst = player.getAttribute(attribute);
        return inst == null ? 0.0f : (float) inst.getValue();
    }
}
