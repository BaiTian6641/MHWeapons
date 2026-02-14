package org.example.common.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.example.MHWeaponsMod;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.data.WeaponData;
import org.example.common.data.WeaponDataResolver;
import org.example.common.data.compat.BetterCombatAnimationMapManager;
import org.example.common.util.CapabilityUtil;
import org.example.item.WeaponIdProvider;

public final class BetterCombatAnimationBridge {
    private static Field attackAnimationField;
    private static final Map<Integer, ComboState> clientComboState = new HashMap<>();
    private static final Map<Integer, ComboState> serverComboState = new HashMap<>();
    private static final Map<Integer, ForcedAnimation> forcedAnimations = new HashMap<>();
    private static final int DEFAULT_COMBO_WINDOW_TICKS = 12;
    private static final int DEFAULT_COMBO_BUFFER_TICKS = 12;

    private BetterCombatAnimationBridge() {
    }
    
    // New accessor for combo state
    public static int resolveCurrentComboIndex(Player player, boolean isClient) {
        if (player == null) return 0;
        Map<Integer, ComboState> map = isClient ? clientComboState : serverComboState;
        ComboState state = map.get(player.getId());
        if (state == null) return 0;
        return state.comboIndex;
    }

    public static void registerForcedAnimation(Player player, String animationId, int durationTicks, float speed) {
        if (player == null || animationId == null || animationId.isBlank()) {
            return;
        }
        int ttl = Math.max(1, durationTicks);
        forcedAnimations.put(player.getId(), new ForcedAnimation(animationId, player.tickCount + ttl, speed));
    }

    public static void registerForcedAnimation(Player player, String animationId, int durationTicks) {
        registerForcedAnimation(player, animationId, durationTicks, 1.0f);
    }
    
    public static boolean isAttackAnimationActive(Player player) {
        if (player == null) return false;
        ForcedAnimation anim = forcedAnimations.get(player.getId());
        return anim != null && player.tickCount < anim.expiresAtTick;
    }

    public static void updateMagnetSpikeAttackAnimation(Player player, boolean impactMode) {
        if (player == null) {
            return;
        }
        WeaponData data = WeaponDataResolver.resolve(player);
        if (data != null && data.getJson().has("animationSeries")) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof WeaponIdProvider weaponIdProvider)
                || !"magnet_spike".equals(weaponIdProvider.getWeaponId())) {
            return;
        }
        String targetAnim = impactMode
                ? "bettercombat:two_handed_slam"
                : "bettercombat:two_handed_slash_horizontal_right";
        updateWeaponAttributesAnimation(stack, targetAnim);
    }

    public static void updateMagnetSpikeZipAnimation(Player player, boolean active) {
        if (player == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof WeaponIdProvider weaponIdProvider)
                || !"magnet_spike".equals(weaponIdProvider.getWeaponId())) {
            return;
        }
        if (active) {
            updateWeaponAttributesAnimation(stack, "bettercombat:two_handed_spin");
            return;
        }
        PlayerWeaponState weaponState = CapabilityUtil.getPlayerWeaponState(player);
        boolean impactMode = weaponState != null && weaponState.isMagnetSpikeImpactMode();
        updateMagnetSpikeAttackAnimation(player, impactMode);
    }

    public static void onBetterCombatAttack(Player player, String animationId) {
        if (player == null) {
            return;
        }
        PlayerCombatState state = CapabilityUtil.getPlayerCombatState(player);
        if (state == null) {
            return;
        }

        if (player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider
                && "hunting_horn".equals(weaponIdProvider.getWeaponId())) {
            String current = state.getActionKey();
            if (current != null && !current.isBlank() && state.getActionKeyTicks() > 0) {
                return;
            }
            state.setActionKey("basic_attack");
            state.setActionKeyTicks(8);
            return;
        }

        // Magnet Spike: route to mode-specific action key (cut vs impact) so motion values/FX align
        PlayerWeaponState weaponState = CapabilityUtil.getPlayerWeaponState(player);
        if (weaponState != null && player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider
                && "magnet_spike".equals(weaponIdProvider.getWeaponId())) {
            if (weaponState.isChargingAttack()) {
                return;
            }
            if ("bettercombat:attack_start".equals(animationId)) {
                return;
            }
            boolean impact = weaponState.isMagnetSpikeImpactMode();
            int window = WeaponDataResolver.resolveInt(player, null, "comboWindowTicks", 24);
            int lastTick = impact ? weaponState.getMagnetImpactComboTick() : weaponState.getMagnetCutComboTick();
            int current = impact ? weaponState.getMagnetImpactComboIndex() : weaponState.getMagnetCutComboIndex();
            int delta = player.tickCount - lastTick;
            boolean timeout = lastTick <= 0 || delta < 0 || delta > window;
            int next = timeout ? 0 : current + 1;
            String[] seq = impact
                    ? new String[] {"magnet_smash_i", "magnet_smash_ii", "magnet_crush", "magnet_suplex"}
                    : new String[] {"magnet_slash_i", "magnet_slash_ii", "magnet_slash_iii", "magnet_cleave"};
            if (next >= seq.length) {
                next = 0;
            }
            String actionKey = seq[next];
            int actionTicks = (next == seq.length - 1) ? 16 : 12;

                MHWeaponsMod.LOGGER.info("MagnetSpike BC combo: player={} mode={} current={} lastTick={} now={} delta={} window={} timeout={} next={} action={} ticks={} anim={}",
                    player.getId(), impact ? "impact" : "cut", current, lastTick, player.tickCount, delta, window, timeout, next, actionKey, actionTicks, animationId);

            state.setActionKey(actionKey);
            state.setActionKeyTicks(actionTicks);

            if (impact) {
                weaponState.setMagnetImpactComboIndex(next);
                weaponState.setMagnetImpactComboTick(player.tickCount);
            } else {
                weaponState.setMagnetCutComboIndex(next);
                weaponState.setMagnetCutComboTick(player.tickCount);
            }
            return;
        }

        // Hammer: drive LMB combo off Better Combat attack start so it advances on input (not hit-only)
        if (weaponState != null && player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider
                && "hammer".equals(weaponIdProvider.getWeaponId())) {
            if (weaponState.isChargingAttack()) {
                return;
            }
            if ("bettercombat:attack_start".equals(animationId)) {
                return;
            }

            String current = state.getActionKey();
            if (current != null && !current.isBlank() && state.getActionKeyTicks() > 0) {
                int bufferTicks = Math.max(6,
                        WeaponDataResolver.resolveInt(player, null, "comboWindowBufferTicks", 6));
                if (state.getActionKeyTicks() > Math.max(1, bufferTicks)) {
                    return;
                }
            }

            // Charged Side Blow / Charged Upswing follow-up chain
            if ("hammer_charged_side_blow".equals(current)
                    || "hammer_charged_upswing".equals(current)) {
                state.setActionKey("hammer_charged_follow_up");
                state.setActionKeyTicks(10);
                return;
            }
            if ("hammer_charged_follow_up".equals(current)) {
                weaponState.setHammerComboIndex(0);
                weaponState.setHammerComboTick(player.tickCount);
                state.setActionKey("hammer_overhead_smash_1");
                state.setActionKeyTicks(10);
                return;
            }

            // Offset Uppercut follow-up
            if ("hammer_offset_uppercut".equals(current)) {
                state.setActionKey("hammer_follow_up_spinslam");
                state.setActionKeyTicks(16);
                return;
            }

            // Spinning Bludgeon chain: LMB during spin advances the sub-combo
            if (current != null && current.startsWith("hammer_spin")) {
                String[] spinCombo = {
                    "hammer_spinning_bludgeon",
                    "hammer_spin_side_smash",
                    "hammer_spin_follow_up",
                    "hammer_spin_strong_upswing"
                };
                for (int i = 0; i < spinCombo.length - 1; i++) {
                    if (spinCombo[i].equals(current)) {
                        int ticks = (i + 1 == spinCombo.length - 1) ? 14 : 10;
                        state.setActionKey(spinCombo[i + 1]);
                        state.setActionKeyTicks(ticks);
                        return;
                    }
                }
                state.setActionKey(spinCombo[spinCombo.length - 1]);
                state.setActionKeyTicks(14);
                return;
            }

            // Standard combo sequence
            int window = Math.max(40, WeaponDataResolver.resolveInt(player, null, "comboWindowTicks", 40));
            int lastTick = weaponState.getHammerComboTick();
            int currentIndex = weaponState.getHammerComboIndex();
            int delta = player.tickCount - lastTick;
            boolean timeout = lastTick <= 0 || delta < 0 || delta > window;
            String[] standardCombo = {
                "hammer_overhead_smash_1",
                "hammer_overhead_smash_2",
                "hammer_upswing"
            };
            int next = timeout ? 0 : (currentIndex + 1) % standardCombo.length;
            weaponState.setHammerComboIndex(next);
            weaponState.setHammerComboTick(player.tickCount);
            int actionTicks = (next == 2) ? 14 : 10;
            state.setActionKey(standardCombo[next]);
            state.setActionKeyTicks(actionTicks);
            return;
        }

        // Dual Blades: route LMB through combo system (like MagnetSpike)
        if (weaponState != null && player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider
                && "dual_blades".equals(weaponIdProvider.getWeaponId())) {
            // Block if action is still playing (animation lock)
            if (state.getActionKey() != null && !"basic_attack".equals(state.getActionKey())
                    && state.getActionKeyTicks() > 0) {
                return;
            }
            boolean inDemon = weaponState.isDemonMode();
            boolean inArch = weaponState.isArchDemon();
            int window = WeaponDataResolver.resolveInt(player, null, "comboWindowTicks", 12);
            if (inDemon) {
                int lastTick = weaponState.getDbDemonComboTick();
                int current = weaponState.getDbDemonComboIndex();
                int next = (player.tickCount - lastTick) > window ? 0 : (current + 1) % 3;
                weaponState.setDbDemonComboIndex(next);
                weaponState.setDbDemonComboTick(player.tickCount);
                String actionKey = switch (next) {
                    case 0 -> "db_demon_fangs";
                    case 1 -> "db_twofold_slash";
                    default -> "db_sixfold_slash";
                };
                int actionTicks = WeaponDataResolver.resolveInt(player, null, "comboActionTicks", 10);
                state.setActionKey(actionKey);
                state.setActionKeyTicks(actionTicks);
            } else if (inArch) {
                int lastTick = weaponState.getDbComboTick();
                int current = weaponState.getDbComboIndex();
                int next = (player.tickCount - lastTick) > window ? 0 : (current + 1) % 3;
                weaponState.setDbComboIndex(next);
                weaponState.setDbComboTick(player.tickCount);
                String actionKey = switch (next) {
                    case 0 -> "db_arch_slash_1";
                    case 1 -> "db_arch_slash_2";
                    default -> "db_arch_slash_3";
                };
                int actionTicks = WeaponDataResolver.resolveInt(player, null, "comboActionTicks", 10);
                state.setActionKey(actionKey);
                state.setActionKeyTicks(actionTicks);
            } else {
                int lastTick = weaponState.getDbComboTick();
                int current = weaponState.getDbComboIndex();
                int next = (player.tickCount - lastTick) > window ? 0 : (current + 1) % 3;
                weaponState.setDbComboIndex(next);
                weaponState.setDbComboTick(player.tickCount);
                String actionKey = switch (next) {
                    case 0 -> "db_double_slash";
                    case 1 -> "db_return_stroke";
                    default -> "db_circle_slash";
                };
                int actionTicks = WeaponDataResolver.resolveInt(player, null, "comboActionTicks", 12);
                state.setActionKey(actionKey);
                state.setActionKeyTicks(actionTicks);
            }
            return;
        }

        if (state.getActionKey() != null && !state.getActionKey().isBlank() && state.getActionKeyTicks() > 0) {
            return;
        }

        if (weaponState != null && player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider
                && "longsword".equals(weaponIdProvider.getWeaponId())) {
            String current = state.getActionKey();
            if ((current != null && ("spirit_thrust".equals(current)
                    || "spirit_helm_breaker".equals(current)
                    || "spirit_helm_breaker_followup_left".equals(current)
                    || "spirit_helm_breaker_followup_right".equals(current)
                    || "helm_breaker_followup".equals(current)
                    || "thrust_rising_slash".equals(current)
                    || "rising_slash".equals(current)))
                    || weaponState.getLongSwordThrustLockTicks() > 0) {
                return;
            }
        }

        String actionKey = BetterCombatAnimationMapManager.INSTANCE.resolveActionKey(animationId);
        if (actionKey == null) {
            return;
        }
        if (actionKey.startsWith("magnet_")) {
            String weaponId = null;
            if (player.getMainHandItem().getItem() instanceof WeaponIdProvider weaponIdProvider) {
                weaponId = weaponIdProvider.getWeaponId();
            }
            if (!"magnet_spike".equals(weaponId)) {
                if ("insect_glaive".equals(weaponId) && "magnet_zip".equals(actionKey)) {
                    actionKey = "double_slash";
                } else {
                    actionKey = "basic_attack";
                }
            }
        }
        state.setActionKey(actionKey);
        state.setActionKeyTicks(10);
    }

    public static String resolveComboAnimation(Player player, int comboCount, String fallbackAnimation) {
        if (player == null) {
            return fallbackAnimation;
        }
        WeaponData data = WeaponDataResolver.resolve(player);
        if (data == null) {
            return fallbackAnimation;
        }
        JsonObject json = data.getJson();
        if (!json.has("animationSeries") || !json.get("animationSeries").isJsonObject()) {
            return fallbackAnimation;
        }
        JsonObject series = json.getAsJsonObject("animationSeries");
        String modeKey = resolveModeKey(player, json, series);
        if (!series.has(modeKey) || !series.get(modeKey).isJsonArray()) {
            return fallbackAnimation;
        }
        JsonArray animations = series.getAsJsonArray(modeKey);
        if (animations.isEmpty()) {
            return fallbackAnimation;
        }
        int index = Math.max(0, comboCount - 1) % animations.size();
        if (!animations.get(index).isJsonPrimitive()) {
            return fallbackAnimation;
        }
        String resolved = animations.get(index).getAsString();
        return resolved == null || resolved.isBlank() ? fallbackAnimation : resolved;
    }

    public static String resolveComboAnimationServer(Player player, int comboCount, String fallbackAnimation) {
        if (comboCount > 0) {
            return resolveComboAnimation(player, comboCount, fallbackAnimation);
        }
        boolean advance = player != null && player.swinging && player.swingTime == 0;
        return resolveComboAnimationWithState(player, fallbackAnimation, serverComboState, "server", advance, advance);
    }

    public static String resolveComboAnimationClient(Player player, String incomingAnimation) {
        if (player == null) {
            return incomingAnimation;
        }
        return resolveComboAnimationWithState(player, incomingAnimation, clientComboState, "client", true, true);
    }

    private static String resolveComboAnimationWithState(Player player,
                                                         String incomingAnimation,
                                                         Map<Integer, ComboState> stateMap,
                                                         String source,
                                                         boolean advance,
                                                         boolean log) {
        if (player == null) {
            return incomingAnimation;
        }
        if ("client".equals(source)) {
            ForcedAnimation forced = forcedAnimations.get(player.getId());
            if (forced != null) {
                if (player.tickCount > forced.expiresAtTick) {
                    forcedAnimations.remove(player.getId());
                } else {
                    return forced.animationId;
                }
            }
        }
        WeaponData data = WeaponDataResolver.resolve(player);
        if (data == null) {
            stateMap.remove(player.getId());
            return incomingAnimation;
        }
        JsonObject json = data.getJson();
        if (!json.has("animationSeries") || !json.get("animationSeries").isJsonObject()) {
            stateMap.remove(player.getId());
            return incomingAnimation;
        }
        JsonObject series = json.getAsJsonObject("animationSeries");
        String modeKey = resolveModeKey(player, json, series);
        if (!series.has(modeKey) || !series.get(modeKey).isJsonArray()) {
            stateMap.remove(player.getId());
            return incomingAnimation;
        }
        JsonArray animations = series.getAsJsonArray(modeKey);
        if (animations.isEmpty()) {
            stateMap.remove(player.getId());
            return incomingAnimation;
        }
        int window = WeaponDataResolver.resolveInt(player, null, "comboWindowTicks", DEFAULT_COMBO_WINDOW_TICKS);
        int attackDelay = (int) Math.ceil(player.getCurrentItemAttackStrengthDelay());
        int buffer = WeaponDataResolver.resolveInt(player, null, "comboWindowBufferTicks", DEFAULT_COMBO_BUFFER_TICKS);
        if (attackDelay > 0) {
            window = Math.max(window, attackDelay + buffer);
        }
        PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(player);
        if (combatState != null) {
            String actionKey = combatState.getActionKey();
            if (actionKey != null && ("spirit_thrust".equals(actionKey)
                    || "spirit_helm_breaker".equals(actionKey)
                    || "spirit_helm_breaker_followup_left".equals(actionKey)
                    || "spirit_helm_breaker_followup_right".equals(actionKey)
                    || "thrust_rising_slash".equals(actionKey)
                    || "rising_slash".equals(actionKey))) {
                return incomingAnimation;
            }
        }
        ComboState state = stateMap.computeIfAbsent(player.getId(), id -> new ComboState());
        String weaponKey = resolveWeaponKey(player, modeKey);
        boolean reset = !weaponKey.equals(state.weaponKey) || (player.tickCount - state.lastAttackTick) > window;
        int index;
        if (reset) {
            index = 0;
        } else if (advance) {
            index = (state.comboIndex + 1) % animations.size();
        } else {
            index = state.comboIndex;
        }
        if (advance) {
            state.comboIndex = index;
            state.lastAttackTick = player.tickCount;
            state.weaponKey = weaponKey;
        }
        if ("client".equals(source)) {
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof WeaponIdProvider weaponIdProvider
                    && "longsword".equals(weaponIdProvider.getWeaponId())
                    && "light".equals(modeKey)) {
                if (combatState != null) {
                    String actionKey = switch (index) {
                        case 0 -> "spirit_blade_1";
                        case 1 -> "spirit_blade_2";
                        case 2 -> "spirit_blade_3";
                        default -> "spirit_roundslash";
                    };
                    combatState.setActionKey(actionKey);
                    combatState.setActionKeyTicks(6);
                }
                // Override special longsword actions to explicit animations
                String actionKey = combatState != null ? combatState.getActionKey() : null;
                if ("spirit_thrust".equals(actionKey) || "thrust_rising_slash".equals(actionKey)) {
                    return "bettercombat:two_handed_stab_right";
                }
                if ("spirit_helm_breaker".equals(actionKey)) {
                    return "bettercombat:two_handed_slam";
                }
                if ("spirit_helm_breaker_followup_left".equals(actionKey)) {
                    return "bettercombat:two_handed_slash_horizontal_left";
                }
                if ("spirit_helm_breaker_followup_right".equals(actionKey)) {
                    return "bettercombat:two_handed_slash_horizontal_right";
                }
                if ("rising_slash".equals(actionKey)) {
                    return "bettercombat:two_handed_slash_vertical_right";
                }
            }
        }
        if (!animations.get(index).isJsonPrimitive()) {
            return incomingAnimation;
        }
        String resolved = animations.get(index).getAsString();
        if (log) {
            MHWeaponsMod.LOGGER.info(
                    "BC combo({}): player={} weapon={} key={} reset={} advance={} index={}/{} window={} incoming={} resolved={}",
                    source,
                    player.getId(),
                    weaponKey,
                    modeKey,
                    reset,
                    advance,
                    index,
                    animations.size(),
                    window,
                    incomingAnimation,
                    resolved
            );
        }
        return resolved == null || resolved.isBlank() ? incomingAnimation : resolved;
    }

    private static String resolveWeaponKey(Player player, String modeKey) {
        ItemStack stack = player.getMainHandItem();
        String weaponId = stack.getItem() instanceof WeaponIdProvider weaponIdProvider
                ? weaponIdProvider.getWeaponId()
                : "unknown";
        return weaponId + "|" + modeKey;
    }

    private static final class ComboState {
        private int comboIndex;
        private int lastAttackTick;
        private String weaponKey = "";
    }

    private static final class ForcedAnimation {
        private final String animationId;
        private final int expiresAtTick;
        private final float speed;

        private ForcedAnimation(String animationId, int expiresAtTick, float speed) {
            this.animationId = animationId;
            this.expiresAtTick = expiresAtTick;
            this.speed = speed;
        }
    }

    private static String resolveModeKey(Player player, JsonObject json, JsonObject series) {
        String baseKey = "light";
        boolean modeAnimation = json.has("modeAnimation") && json.get("modeAnimation").isJsonPrimitive()
                && json.get("modeAnimation").getAsBoolean();
        String category = json.has("category") && json.get("category").isJsonPrimitive()
                ? json.get("category").getAsString()
                : null;
        if (modeAnimation) {
            PlayerWeaponState weaponState = CapabilityUtil.getPlayerWeaponState(player);
            if (weaponState != null) {
                if ("magnet_spike".equals(category)) {
                    baseKey = weaponState.isMagnetSpikeImpactMode() ? "heavy" : "light";
                } else if ("switch_axe".equals(category)) {
                    baseKey = weaponState.isSwitchAxeSwordMode() ? "light" : "heavy";
                } else if ("charge_blade".equals(category)) {
                    baseKey = weaponState.isChargeBladeSwordMode() ? "light" : "heavy";
                }
            }
        }
        PlayerCombatState combatState = CapabilityUtil.getPlayerCombatState(player);
        if (combatState != null) {
            String actionKey = combatState.getActionKey();
            if (actionKey != null) {
                if (("overhead_smash".equals(actionKey) || "wide_sweep".equals(actionKey)) && series.has(actionKey)) {
                    return actionKey;
                }
                if (actionKey.startsWith("overhead") || "crescent_slash".equals(actionKey)) {
                    if (series.has("overhead")) {
                        return "overhead";
                    }
                }
                if ("thrust_rising_slash".equals(actionKey)) {
                    if (series.has("rising")) {
                        return "rising";
                    }
                }
                if ("morph".equals(actionKey)) {
                    String switchKey = baseKey + "_switch";
                    if (series.has(switchKey)) {
                        return switchKey;
                    }
                }
            }
        }
        return baseKey;
    }

    private static void updateWeaponAttributesAnimation(ItemStack stack, String animationId) {
        WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
        if (attributes == null || attributes.attacks() == null) {
            return;
        }
        Field field = getAttackAnimationField();
        if (field == null) {
            return;
        }
        for (WeaponAttributes.Attack attack : attributes.attacks()) {
            if (attack == null) {
                continue;
            }
            try {
                Object current = field.get(attack);
                if (!animationId.equals(current)) {
                    field.set(attack, animationId);
                    MHWeaponsMod.LOGGER.info("BC set magnet_spike attack animation to {}", animationId);
                }
            } catch (IllegalAccessException e) {
                MHWeaponsMod.LOGGER.warn("Failed to update Better Combat attack animation", e);
            }
        }
    }

    private static Field getAttackAnimationField() {
        if (attackAnimationField != null) {
            return attackAnimationField;
        }
        try {
            Field field = WeaponAttributes.Attack.class.getDeclaredField("animation");
            field.setAccessible(true);
            attackAnimationField = field;
            return field;
        } catch (NoSuchFieldException e) {
            MHWeaponsMod.LOGGER.warn("Better Combat attack animation field not found", e);
            return null;
        }
    }
}
