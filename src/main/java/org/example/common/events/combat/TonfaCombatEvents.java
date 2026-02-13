package org.example.common.events.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.example.common.capability.mob.MobWoundState;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.util.CapabilityUtil;
import org.example.item.WeaponIdProvider;

public class TonfaCombatEvents {

    /** Base gauge gain per hit. Fast attacks grant less, heavy attacks grant more. */
    private static final float BASE_GAUGE_GAIN = 5.0f;
    /** EX state threshold (gauge >= this triggers EX bonuses). */
    private static final float EX_THRESHOLD = 95.0f;
    /** Damage multiplier during EX state. */
    private static final float EX_DAMAGE_BONUS = 1.15f;

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (!(player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponProvider) || !"tonfa".equals(weaponProvider.getWeaponId())) {
            return;
        }

        PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(player);
        if (combatState == null) {
            return;
        }

        PlayerWeaponState weaponState = CapabilityUtil.getPlayerWeaponState(player);
        if (weaponState == null) {
            return;
        }

        String actionKey = combatState.getActionKey();

        // --- Rhythm Gauge accumulation on every hit ---
        float gaugeGain = resolveGaugeGain(actionKey);
        weaponState.addTonfaComboGauge(gaugeGain);
        weaponState.setTonfaLastHitTick((int) player.level().getGameTime());

        // --- Linear damage scaling from Rhythm Gauge (up to +20% at max gauge) ---
        float gauge = weaponState.getTonfaComboGauge();
        float gaugeBonus = 1.0f + (gauge / 100.0f) * 0.20f; // 0% gauge = 1.0x, 100% gauge = 1.20x
        event.setAmount(event.getAmount() * gaugeBonus);

        // --- EX state bonus ---
        if (gauge >= EX_THRESHOLD) {
            event.setAmount(event.getAmount() * EX_DAMAGE_BONUS);
        }

        // --- Handle Pinpoint Drill (Focus Strike) ---
        if ("tonfa_drill".equals(actionKey)) {
            LivingEntity target = event.getEntity();
            MobWoundState woundState = CapabilityUtil.getMobWoundState(target);
            
            if (woundState != null && woundState.isWounded()) {
                // Destroy Wound (Wilds Mechanic: Focus Strike consumes wound for damage)
                woundState.setWounded(false);
                woundState.setWoundTicksRemaining(0);
                
                // Massive Damage Boost (Destroying a wound is a finisher)
                event.setAmount(event.getAmount() * 2.5f);
                
            } else {
                // Multi-hit drill is still strong, but standard
                event.setAmount(event.getAmount() * 1.1f);
            }
        }
    }

    /**
     * Resolve gauge gain based on the action key.
     * Heavy/slow attacks grant more gauge, fast/weak attacks grant less.
     */
    private float resolveGaugeGain(String actionKey) {
        if (actionKey == null) return BASE_GAUGE_GAIN;
        return switch (actionKey) {
            // Long Mode: slower, bigger gauge gain
            case "tonfa_long_1" -> 6.0f;
            case "tonfa_long_2" -> 7.0f;
            case "tonfa_long_3" -> 9.0f;
            case "tonfa_long_ex" -> 12.0f;
            case "tonfa_long_sweep" -> 5.0f;
            case "tonfa_charge" -> 15.0f;
            // Short Mode: rapid, small gauge gain per hit
            case "tonfa_short_rise" -> 4.0f;
            case "tonfa_short_1" -> 3.0f;
            case "tonfa_short_2" -> 3.0f;
            case "tonfa_short_flurry" -> 2.0f; // hits 3 times, so effective ~6
            case "tonfa_short_ex" -> 10.0f;
            // Aerial
            case "tonfa_air_slash" -> 3.0f;
            case "tonfa_dive" -> 10.0f;
            // Drill: gauge is being consumed, don't refill much
            case "tonfa_drill" -> 1.0f;
            default -> BASE_GAUGE_GAIN;
        };
    }
}
