package org.example.common.combat.weapon;

import net.minecraft.world.entity.player.Player;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.data.WeaponDataResolver;

/**
 * Hammer combat handler.
 *
 * <h3>Controls (per official MH Wilds manual)</h3>
 * <ul>
 *   <li><b>LMB (Left Click)</b>: Standard Attack combo — driven by Better Combat → ActionKeyEvents.
 *       Overhead Smash I → II → Upswing (loop).</li>
 *   <li><b>RMB (Right Click, Hold)</b>: Charge — hold to accumulate charge levels 1/2/3.
 *       Release for charged attack. Handled by {@code ClientForgeEvents.handleHammerRmbInput}
 *       → CHARGE packet → {@link #handleChargeRelease}.</li>
 *   <li><b>C Key (Special)</b>:
 *       <ul>
 *         <li>While idle → Big Bang combo (I→II→III→IV→Finisher)</li>
 *         <li>While charging (Lvl 1-2) → Charged Step (evasive hop preserving charge)</li>
 *         <li>While charging (Lvl 3) → Mighty Charge activation</li>
 *         <li>After an attack → Spinning Bludgeon</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h3>Charge Releases</h3>
 * <ul>
 *   <li>Lvl 1 → Charged Side Blow → LMB → Charged Follow-up → Overhead Smash I</li>
 *   <li>Lvl 2 → Charged Upswing → LMB → Charged Follow-up</li>
 *   <li>Lvl 3 → Charged Big Bang (or Mighty Charge Slam if Power Charged)</li>
 * </ul>
 */
public final class HammerHandler {

    // ── Standard combo action keys ──────────────────────────────────────
    private static final String[] STANDARD_COMBO = {
            "hammer_overhead_smash_1",
            "hammer_overhead_smash_2",
            "hammer_upswing"
    };

    // ── Big Bang combo action keys ──────────────────────────────────────
    private static final String[] BIG_BANG_COMBO = {
            "hammer_big_bang_1",
            "hammer_big_bang_2",
            "hammer_big_bang_3",
            "hammer_big_bang_4",
            "hammer_big_bang_finisher"
    };

    // ── Spinning Bludgeon follow-up action keys ─────────────────────────
    private static final String[] SPINNING_COMBO = {
            "hammer_spinning_bludgeon",
            "hammer_spin_side_smash",
            "hammer_spin_follow_up",
            "hammer_spin_strong_upswing"
    };

    private HammerHandler() {}

    /**
     * Main entry point – called from {@link WeaponActionHandler}.
     * <p>
     * WEAPON: fallback for keyboard (X key) combo advance.
     * Primary LMB combo is driven by BC → ActionKeyEvents.
     * SPECIAL: Big Bang / Charged Step / Spinning Bludgeon.
     */
    public static void handleAction(WeaponActionType action, boolean pressed,
                                    Player player, PlayerCombatState combatState,
                                    PlayerWeaponState weaponState) {
        if (!pressed) return;

        switch (action) {
            case WEAPON        -> handleLmbCombo(player, combatState, weaponState);
            case SPECIAL       -> handleSpecial(player, combatState, weaponState);
            default -> {}
        }
    }

    // ── LMB: Standard combo (keyboard X fallback) or charge follow-ups ──

    private static void handleLmbCombo(Player player, PlayerCombatState combatState,
                                       PlayerWeaponState weaponState) {
        String currentAction = combatState.getActionKey();

        // Animation lock — reject if current action still active
        if (currentAction != null && currentAction.startsWith("hammer_") && combatState.getActionKeyTicks() > 0) {
            return;
        }

        // Focus Strike: Focus Mode + LMB → high-damage earthquake blow
        if (combatState.isFocusMode()) {
            setAction(combatState, "hammer_focus_blow_earthquake", 18);
            return;
        }

        // If Big Bang is active, don't override — Big Bang advances via SPECIAL only
        if (weaponState.getHammerBigBangStage() > 0) {
            return;
        }

        // If a charge follow-up is active, chain into Overhead Smash I
        if ("hammer_charged_follow_up".equals(currentAction)) {
            weaponState.setHammerComboIndex(0);
            weaponState.setHammerComboTick(player.tickCount);
            setAction(combatState, STANDARD_COMBO[0], 10);
            return;
        }

        // Charged Side Blow / Charged Upswing → follow-up step
        if ("hammer_charged_side_blow".equals(currentAction)
                || "hammer_charged_upswing".equals(currentAction)) {
            setAction(combatState, "hammer_charged_follow_up", 10);
            return;
        }

        // After Offset Uppercut → Follow-up Spinslam
        if ("hammer_offset_uppercut".equals(currentAction)) {
            setAction(combatState, "hammer_follow_up_spinslam", 16);
            return;
        }

        // Spinning Bludgeon: LMB during spin advances sub-combo
        if (currentAction != null && currentAction.startsWith("hammer_spin")) {
            advanceSpinningCombo(combatState, currentAction);
            return;
        }

        // Standard combo sequencing
        int window = WeaponDataResolver.resolveInt(player, null, "comboWindowTicks", 24);
        int lastTick = weaponState.getHammerComboTick();
        int current = weaponState.getHammerComboIndex();
        int delta = player.tickCount - lastTick;
        boolean timeout = lastTick <= 0 || delta > window;
        int next = timeout ? 0 : (current + 1) % STANDARD_COMBO.length;

        weaponState.setHammerComboIndex(next);
        int actionTicks = (next == 2) ? 14 : 10;
        weaponState.setHammerComboTick(player.tickCount);
        setAction(combatState, STANDARD_COMBO[next], actionTicks);
    }

    // ── Special key (C): Big Bang / Charged Step / Spinning Bludgeon ────

    private static void handleSpecial(Player player, PlayerCombatState combatState,
                                      PlayerWeaponState weaponState) {
        String currentAction = combatState.getActionKey();

        // If currently charging → Charged Step / Mighty Charge
        if (weaponState.isChargingAttack()) {
            int maxCharge = WeaponDataResolver.resolveInt(player, null, "chargeMaxTicks", 40);
            int chargeTicks = weaponState.getChargeAttackTicks();
            int stage = resolveChargeStage(chargeTicks, maxCharge);

            if (stage >= 3) {
                // At max charge: activate Mighty Charge (Power Charge)
                weaponState.setHammerPowerCharge(true);
                setAction(combatState, "hammer_mighty_charge", 10);
            } else {
                // Charged Step: dodge without losing charge
                float cost = org.example.common.combat.StaminaHelper.applyCost(player, 15.0f);
                if (weaponState.getStamina() >= cost) {
                    combatState.setDodgeIFrameTicks(6);
                    setAction(combatState, "hammer_charged_step", 6);
                    weaponState.addStamina(-cost);
                    weaponState.setStaminaRecoveryDelay(16);
                    net.minecraft.world.phys.Vec3 dash = player.getLookAngle().normalize().scale(0.45);
                    player.setDeltaMovement(dash.x, player.getDeltaMovement().y + 0.05, dash.z);
                    player.hurtMarked = true;
                }
            }
            return;
        }

        // After an attack → Spinning Bludgeon (manual: LMB+C after attacking)
        if (currentAction != null && (currentAction.startsWith("hammer_overhead_smash")
                || "hammer_upswing".equals(currentAction)
                || "hammer_charged_follow_up".equals(currentAction)
                || "hammer_charged_side_blow".equals(currentAction)
                || "hammer_charged_upswing".equals(currentAction))) {
            setAction(combatState, SPINNING_COMBO[0], 14);
            return;
        }

        // During Spinning Bludgeon → pressing C again advances spin
        if (currentAction != null && currentAction.startsWith("hammer_spin")) {
            advanceSpinningCombo(combatState, currentAction);
            return;
        }

        // Animation lock for non-Big Bang, non-charge, non-spinning actions
        if (currentAction != null
                && currentAction.startsWith("hammer_")
                && combatState.getActionKeyTicks() > 0
                && weaponState.getHammerBigBangStage() <= 0) {
            return;
        }

        // If Big Bang is already active, advance it
        if (weaponState.getHammerBigBangStage() > 0) {
            advanceBigBang(player, combatState, weaponState);
            return;
        }

        // Start Big Bang combo (stage 1)
        weaponState.setHammerBigBangStage(1);
        weaponState.setHammerBigBangTick(40);
        setAction(combatState, BIG_BANG_COMBO[0], 12);
    }

    // ── Big Bang advancement ────────────────────────────────────────────

    private static void advanceBigBang(Player player, PlayerCombatState combatState,
                                       PlayerWeaponState weaponState) {
        if (weaponState.getHammerBigBangTick() <= 0) {
            weaponState.setHammerBigBangStage(0);
            return;
        }

        int stage = weaponState.getHammerBigBangStage();
        if (stage >= BIG_BANG_COMBO.length) {
            weaponState.setHammerBigBangStage(0);
            weaponState.setHammerBigBangTick(0);
            return;
        }

        weaponState.setHammerBigBangStage(stage + 1);
        weaponState.setHammerBigBangTick(40);

        int idx = Math.min(stage, BIG_BANG_COMBO.length - 1);
        int actionTicks = (idx == BIG_BANG_COMBO.length - 1) ? 18 : 12;
        setAction(combatState, BIG_BANG_COMBO[idx], actionTicks);
    }

    // ── Spinning Bludgeon advancement ───────────────────────────────────

    private static void advanceSpinningCombo(PlayerCombatState combatState, String currentAction) {
        for (int i = 0; i < SPINNING_COMBO.length - 1; i++) {
            if (SPINNING_COMBO[i].equals(currentAction)) {
                int ticks = (i + 1 == SPINNING_COMBO.length - 1) ? 14 : 10;
                setAction(combatState, SPINNING_COMBO[i + 1], ticks);
                return;
            }
        }
        setAction(combatState, SPINNING_COMBO[SPINNING_COMBO.length - 1], 14);
    }

    // ── Charge release (called from WeaponActionHandler.handleCharge) ───

    /**
     * Called when RMB is released after charging.
     */
    public static void handleChargeRelease(Player player, PlayerCombatState combatState,
                                           PlayerWeaponState weaponState,
                                           int chargeTicks, int maxCharge) {
        int level;
        if (maxCharge > 0) {
            if (chargeTicks >= maxCharge) {
                level = 3;
            } else if (chargeTicks >= (maxCharge * 2 / 3)) {
                level = 2;
            } else if (chargeTicks >= (maxCharge / 3)) {
                level = 1;
            } else {
                level = 0;
            }
        } else {
            level = 0;
        }

        weaponState.setHammerChargeLevel(level);
        weaponState.setHammerChargeTicks(60);

        switch (level) {
            case 1 -> setAction(combatState, "hammer_charged_side_blow", 10);
            case 2 -> setAction(combatState, "hammer_charged_upswing", 12);
            case 3 -> {
                if (weaponState.isHammerPowerCharge()) {
                    setAction(combatState, "hammer_mighty_charge_slam", 16);
                } else if (!player.onGround()) {
                    setAction(combatState, "hammer_aerial_spin", 14);
                } else {
                    setAction(combatState, "hammer_charged_big_bang", 14);
                }
            }
            default -> setAction(combatState, "hammer_charged_side_blow", 8);
        }
    }

    // ── Tick decay for Big Bang combo timeout ───────────────────────────

    public static void tick(PlayerWeaponState state) {
        // hammerBigBangTick and hammerChargeTicks decay handled by WeaponStateEvents
    }

    // ── Utility ─────────────────────────────────────────────────────────

    private static int resolveChargeStage(int chargeTicks, int maxCharge) {
        if (maxCharge <= 0) return 0;
        if (chargeTicks >= maxCharge) return 3;
        if (chargeTicks >= (maxCharge * 2 / 3)) return 2;
        if (chargeTicks >= (maxCharge / 3)) return 1;
        return 0;
    }

    private static void setAction(PlayerCombatState combatState, String key, int ticks) {
        combatState.setActionKey(key);
        combatState.setActionKeyTicks(ticks);
    }
}
