package org.example.common.combat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.combat.weapon.DualBladesHandler;
import org.example.common.combat.weapon.LongSwordHandler;
import org.example.common.util.CapabilityUtil;
import org.example.item.WeaponIdProvider;

public final class DodgeSystem {
    public boolean tryCancelAttack(Player player, LivingAttackEvent event) {
        PlayerCombatState state = CapabilityUtil.getPlayerCombatState(player);
        if (state == null || state.getDodgeIFrameTicks() <= 0) {
            return false;
        }
        event.setCanceled(true);

        // Foresight Slash counter reward: only when the current action is foresight and weapon is Long Sword
        if ("foresight_slash".equals(state.getActionKey())) {
            PlayerWeaponState weaponState = CapabilityUtil.getPlayerWeaponState(player);
            ItemStack stack = player.getMainHandItem();
            if (weaponState != null && stack.getItem() instanceof WeaponIdProvider weaponIdProvider
                    && "longsword".equals(weaponIdProvider.getWeaponId())) {
                LongSwordHandler.handleForesightCounterSuccess(player, state, weaponState);
            }
        }
        
        // Iai Spirit Slash counter reward
        if ("iai_spirit_slash".equals(state.getActionKey())) {
            PlayerWeaponState weaponState = CapabilityUtil.getPlayerWeaponState(player);
            ItemStack stack = player.getMainHandItem();
            if (weaponState != null && stack.getItem() instanceof WeaponIdProvider weaponIdProvider
                    && "longsword".equals(weaponIdProvider.getWeaponId())) {
                LongSwordHandler.handleIaiSpiritCounterSuccess(player, state, weaponState);
            }
        }

        // Dual Blades: Perfect Evade in Demon Dodge → Demon Boost Mode
        if ("demon_dodge".equals(state.getActionKey())) {
            PlayerWeaponState weaponState = CapabilityUtil.getPlayerWeaponState(player);
            ItemStack stack = player.getMainHandItem();
            if (weaponState != null && stack.getItem() instanceof WeaponIdProvider weaponIdProvider
                    && "dual_blades".equals(weaponIdProvider.getWeaponId())
                    && (weaponState.isDemonMode() || weaponState.isArchDemon())) {
                DualBladesHandler.activateDemonBoost(player, state, weaponState);
            }
        }
        return true;
    }

    public void tick(Player player) {
        PlayerCombatState state = CapabilityUtil.getPlayerCombatState(player);
        if (state == null) {
            return;
        }
        int next = state.getDodgeIFrameTicks() - 1;
        state.setDodgeIFrameTicks(Math.max(0, next));
    }

    public void grantIFrames(Player player, int ticks) {
        if (ticks <= 0) {
            return;
        }
        PlayerCombatState state = CapabilityUtil.getPlayerCombatState(player);
        if (state != null) {
            state.setDodgeIFrameTicks(ticks);
        }
    }
}
