package org.example.common.combat;

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.resources.ResourceLocation;
import org.example.MHWeaponsMod;
import org.example.common.capability.mob.MobWoundState;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.data.RulesetDataManager;
import org.example.common.network.ModNetwork;
import org.example.common.network.packet.WoundStateS2CPacket;
import org.example.common.util.CapabilityUtil;

public final class WoundSystem {
    private static final float DEFAULT_WOUND_THRESHOLD = 30.0f;
    private static final int DEFAULT_WOUND_DURATION_TICKS = 200;
    private static final float DEFAULT_WOUND_DAMAGE_MULTIPLIER = 1.20f;

    private final Map<LivingEntity, MobWoundState> wounds = new WeakHashMap<>();

    public void applyWoundLogic(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        if (event.getAmount() <= 0.0f) {
            return;
        }

        MobWoundState state = getOrCreateState(target);
        if (state == null) {
            return;
        }

        if (event.getSource().getEntity() instanceof Player player) {
            if (tryFocusStrikePop(player, target, state, event)) {
                return;
            }
        }
        if (state.isWounded()) {
            return;
        }

        float nextValue = state.getWoundValue() + event.getAmount();
        state.setWoundValue(nextValue);
        if (nextValue >= getWoundThreshold()) {
            state.setWounded(true);
            state.setWoundTicksRemaining(getWoundDurationTicks());
            state.setWoundValue(0.0f);
            syncWoundState(target, state);
        }
    }

    public void tick(Level level) {
        if (level.isClientSide()) {
            return;
        }

        wounds.forEach((entity, state) -> {
            if (!state.isWounded()) {
                return;
            }
            int remaining = state.getWoundTicksRemaining() - 1;
            state.setWoundTicksRemaining(remaining);
            if (remaining <= 0) {
                state.setWounded(false);
                syncWoundState(entity, state);
            }
        });
    }

    public float getWoundMultiplier(LivingEntity target) {
        MobWoundState state = getOrCreateState(target);
        if (state == null || !state.isWounded()) {
            return 1.0f;
        }
        return getWoundMultiplier();
    }

    private float getWoundThreshold() {
        JsonObject ruleset = getDefaultRuleset();
        if (ruleset != null && ruleset.has("woundThreshold")) {
            return ruleset.get("woundThreshold").getAsFloat();
        }
        return DEFAULT_WOUND_THRESHOLD;
    }

    private int getWoundDurationTicks() {
        JsonObject ruleset = getDefaultRuleset();
        if (ruleset != null && ruleset.has("woundDurationTicks")) {
            return ruleset.get("woundDurationTicks").getAsInt();
        }
        return DEFAULT_WOUND_DURATION_TICKS;
    }

    private float getWoundMultiplier() {
        JsonObject ruleset = getDefaultRuleset();
        if (ruleset != null && ruleset.has("woundMultiplier")) {
            return ruleset.get("woundMultiplier").getAsFloat();
        }
        return DEFAULT_WOUND_DAMAGE_MULTIPLIER;
    }

    private float getFocusStrikeBonus() {
        JsonObject ruleset = getDefaultRuleset();
        if (ruleset != null && ruleset.has("focusStrikeBonus")) {
            return ruleset.get("focusStrikeBonus").getAsFloat();
        }
        return 0.25f;
    }

    private JsonObject getDefaultRuleset() {
        return RulesetDataManager.INSTANCE.get(ResourceLocation.fromNamespaceAndPath(MHWeaponsMod.MODID, "default"));
    }

    private MobWoundState getOrCreateState(LivingEntity target) {
        MobWoundState state = CapabilityUtil.getMobWoundState(target);
        if (state == null) {
            return null;
        }
        wounds.putIfAbsent(target, state);
        return state;
    }

    private void syncWoundState(LivingEntity target, MobWoundState state) {
        if (!(target.level() instanceof ServerLevel)) {
            return;
        }
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target),
                new WoundStateS2CPacket(target.getId(), state.isWounded(), state.getWoundTicksRemaining()));
    }

    private boolean tryFocusStrikePop(Player player, LivingEntity target, MobWoundState state, LivingHurtEvent event) {
        if (!state.isWounded()) {
            return false;
        }
        PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(player);
        if (combatState == null || combatState.getActionKey() == null) {
            return false;
        }
        if (!"focus_strike".equals(combatState.getActionKey())) {
            return false;
        }

        float bonus = getFocusStrikeBonus();
        event.setAmount(event.getAmount() * (1.0f + bonus));
        state.setWounded(false);
        state.setWoundTicksRemaining(0);
        syncWoundState(target, state);
        return true;
    }
}
