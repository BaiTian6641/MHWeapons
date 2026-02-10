package org.example.common.combat.bowgun;

import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.combat.weapon.WeaponActionType;
import org.example.common.entity.AmmoProjectileEntity;
import org.example.item.BowgunItem;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Central handler for all Bowgun actions: fire, reload, mode switch,
 * ammo select, guard, melee bash, focus blast, and special ammo.
 *
 * Dispatched from WeaponActionHandler when weaponId == "bowgun".
 */
public final class BowgunHandler {
    private static final Logger LOG = LogUtils.getLogger();

    // Mode constants matching PlayerWeaponState.bowgunMode
    public static final int MODE_STANDARD = 0;
    public static final int MODE_RAPID    = 1;   // Light
    public static final int MODE_VERSATILE = 2;  // Medium
    public static final int MODE_IGNITION  = 3;  // Heavy

    // Gauge limits
    public static final float GAUGE_MAX = 200.0f;
    public static final float GAUGE_REGEN_PER_TICK = 0.25f;
    public static final float GAUGE_REGEN_ON_HIT = 7.5f;
    public static final float RAPID_FIRE_GAUGE_COST = GAUGE_MAX / 20.0f; // 1/30 gauge per burst
    public static final float VERSATILE_BURST_COST = 10.0f;
    public static final float EMPOWERED_SHOT_COST = 10.0f;
    public static final float IGNITION_COST_PER_TICK = GAUGE_MAX / 60.0f; // 1/30 gauge per tick

    private BowgunHandler() {}

    // ══════════════════════════════════════════════════════════════════
    //  Main action dispatch
    // ══════════════════════════════════════════════════════════════════

    public static void handleAction(WeaponActionType action, boolean pressed,
                                     Player player, PlayerCombatState combatState,
                                     PlayerWeaponState weaponState) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof BowgunItem)) return;

        LOG.debug("[Bowgun] handleAction action={} pressed={} mode={}",
                action, pressed, weaponState.getBowgunMode());

        switch (action) {
            case CHARGE -> handleFire(player, pressed, stack, weaponState);
            case WEAPON -> handleWeaponAction(player, pressed, stack, weaponState);
            case BOWGUN_AIM -> handleAim(player, pressed, weaponState);
            case BOWGUN_RELOAD -> handleReload(player, pressed, stack, weaponState);
            case WEAPON_ALT -> handleMeleeBash(player, stack, weaponState);
            case SPECIAL -> handleSpecial(player, pressed, stack, weaponState);
            case GUARD -> handleGuard(player, pressed, stack, weaponState);
            default -> LOG.debug("[Bowgun] Unhandled action: {}", action);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  ADS / Aim Down Sights (BOWGUN_AIM — mapped from RMB)
    // ══════════════════════════════════════════════════════════════════

    private static void handleAim(Player player, boolean pressed, PlayerWeaponState state) {
        state.setBowgunAiming(pressed);
        if (pressed) {
            LOG.debug("[Bowgun] ADS started — reduced spread, +10% damage");
        } else {
            LOG.debug("[Bowgun] ADS ended");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  Fire (CHARGE action — mapped from LMB)
    // ══════════════════════════════════════════════════════════════════

    private static void handleFire(Player player, boolean pressed, ItemStack stack,
                                    PlayerWeaponState state) {
        if (!pressed) {
            // Release – stop sustained fire (Wyvernheart)
            if (state.getBowgunMode() == MODE_IGNITION && state.isBowgunFiring()) {
                state.setBowgunFiring(false);
                state.setBowgunSustainedHits(0);
                LOG.debug("[Bowgun] Stopped sustained fire");
            }
            // Release – stop rapid-fire hold
            if (state.getBowgunMode() == MODE_RAPID && state.isBowgunFiring()) {
                state.setBowgunFiring(false);
                LOG.debug("[Bowgun] Stopped rapid-fire hold");
            }
            return;
        }

        // Can't fire during reload or recoil
        if (state.getBowgunReloadTimer() > 0) {
            LOG.debug("[Bowgun] Can't fire — reloading");
            return;
        }
        if (state.getBowgunRecoilTimer() > 0) {
            LOG.debug("[Bowgun] Can't fire — recoil recovery");
            return;
        }

        int mode = state.getBowgunMode();
        switch (mode) {
            case MODE_STANDARD -> fireStandard(player, stack, state);
            case MODE_RAPID    -> {
                state.setBowgunFiring(true); // allow hold-to-rapid in tick()
                fireRapid(player, stack, state);
            }
            case MODE_VERSATILE -> fireVersatile(player, stack, state, pressed);
            case MODE_IGNITION  -> {
                if (!state.isBowgunFiring()) {
                    state.setBowgunFiring(true);
                    state.setBowgunSustainedHits(0);
                }
                fireIgnition(player, stack, state);
            }
        }
    }

    private static void fireStandard(Player player, ItemStack stack, PlayerWeaponState state) {
        String ammo = state.getBowgunCurrentAmmo();
        if (ammo.isEmpty()) {
            ammo = BowgunMagazineManager.findFirstLoadedAmmo(player, stack);
            state.setBowgunCurrentAmmo(ammo);
        }

        if (!BowgunMagazineManager.consumeRound(stack, ammo)) {
            LOG.debug("[Bowgun] Magazine empty for {}", ammo);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.5f, 1.2f);
            return;
        }

        spawnProjectile(player, stack, state, ammo, 1.0f);
        int recoil = BowgunMagazineManager.getEffectiveRecoil(stack, ammo);
        state.setBowgunRecoilTimer(BowgunMagazineManager.getRecoilRecoveryTicks(recoil));
        state.setBowgunLastAction(1); // FIRE
        LOG.debug("[Bowgun] Standard fire {} recoil={}", ammo, recoil);
    }

    private static void fireRapid(Player player, ItemStack stack, PlayerWeaponState state) {
        String ammo = state.getBowgunCurrentAmmo();
        if (ammo.isEmpty()) return;

        if (!BowgunMagazineManager.canRapidFire(stack, ammo)) {
            // Fall back to standard fire
            fireStandard(player, stack, state);
            return;
        }

        // Check gauge
        if (state.getBowgunGauge() < RAPID_FIRE_GAUGE_COST) {
            LOG.debug("[Bowgun] Rapid fire gauge too low: {}", state.getBowgunGauge());
            return;
        }

        // Rapid fire: consume ONLY ONE round, but fire the full burst
        if (!BowgunMagazineManager.consumeRound(stack, ammo)) {
            LOG.debug("[Bowgun] Rapid fire magazine empty for {}", ammo);
            return;
        }

        int burst = BowgunMagazineManager.getRapidFireBurst(ammo);
        float dmgMult = BowgunMagazineManager.getRapidFireDmgMult(ammo);

        for (int i = 0; i < burst; i++) {
            spawnProjectile(player, stack, state, ammo, dmgMult);
        }

        state.setBowgunGauge(state.getBowgunGauge() - RAPID_FIRE_GAUGE_COST);
        int recoil = BowgunMagazineManager.getEffectiveRecoil(stack, ammo);
        state.setBowgunRecoilTimer(BowgunMagazineManager.getRecoilRecoveryTicks(recoil));
        state.setBowgunLastAction(2); // RAPID_FIRE
        LOG.debug("[Bowgun] Rapid fire {} x{} (1 round consumed) dmg×{}", ammo, burst, dmgMult);
    }

    private static void fireVersatile(Player player, ItemStack stack, PlayerWeaponState state, boolean held) {
        String ammo = state.getBowgunCurrentAmmo();
        if (ammo.isEmpty()) return;

        if (state.getBowgunGauge() < VERSATILE_BURST_COST) {
            fireStandard(player, stack, state);
            return;
        }

        // 2-round burst
        int fired = 0;
        for (int i = 0; i < 2; i++) {
            if (!BowgunMagazineManager.consumeRound(stack, ammo)) break;
            spawnProjectile(player, stack, state, ammo, 0.85f);
            fired++;
        }
        if (fired > 0) {
            state.setBowgunGauge(state.getBowgunGauge() - VERSATILE_BURST_COST);
            int recoil = BowgunMagazineManager.getEffectiveRecoil(stack, ammo);
            state.setBowgunRecoilTimer(BowgunMagazineManager.getRecoilRecoveryTicks(recoil));
            state.setBowgunLastAction(3); // VERSATILE_BURST
            LOG.debug("[Bowgun] Versatile burst {} x{}", ammo, fired);
        }
    }

    private static void fireIgnition(Player player, ItemStack stack, PlayerWeaponState state) {
        if (state.getBowgunGauge() < IGNITION_COST_PER_TICK * 5) {
            LOG.debug("[Bowgun] Ignition gauge too low");
            return;
        }

        String ignType = BowgunItem.getIgnitionType(stack);
        if (ignType.isEmpty()) ignType = "wyvernheart";

        // Weight-class damage multiplier for ignition types
        int weightClass = BowgunItem.getWeightClass(stack);
        float weightMult = getIgnitionWeightMultiplier(ignType, weightClass);

        switch (ignType) {
            case "wyvernheart" -> {
                // Sustained fire — start/continue
                state.setBowgunFiring(true);
                state.setBowgunLastAction(4); // IGNITION_FIRE
                LOG.debug("[Bowgun] Wyvernheart ignition start (weightMult={})", weightMult);
            }
            case "wyvernpiercer" -> {
                state.setBowgunGauge(state.getBowgunGauge() - 40.0f);
                spawnSpecialProjectile(player, stack, state, "wyvernpiercer", weightMult);
                state.setBowgunRecoilTimer(15);
                state.setBowgunLastAction(4);
                LOG.debug("[Bowgun] Wyvernpiercer fired (weightMult={})", weightMult);
            }
            case "wyverncounter" -> {
                // Wyverncounter: close-range, hold-to-charge mechanic
                state.setBowgunGauge(state.getBowgunGauge() - 30.0f);
                spawnSpecialProjectile(player, stack, state, "wyverncounter", weightMult);
                state.setBowgunRecoilTimer(10);
                state.setBowgunLastAction(4);
                LOG.debug("[Bowgun] Wyverncounter fired (weightMult={})", weightMult);
            }
            case "wyvernblast" -> {
                // Wyvernblast: mid-range wide area detonation
                state.setBowgunGauge(state.getBowgunGauge() - 35.0f);
                spawnSpecialProjectile(player, stack, state, "wyvernblast", weightMult);
                state.setBowgunRecoilTimer(10);
                state.setBowgunLastAction(4);
                LOG.debug("[Bowgun] Wyvernblast fired (weightMult={})", weightMult);
            }
        }
    }

    /**
     * Returns a damage multiplier based on the ignition type and the bowgun's weight class.
     * Light/Medium frameworks get a different multiplier than Heavy for the same ignition core.
     * This makes each ignition plugin behave differently when installed on different weight classes.
     */
    private static float getIgnitionWeightMultiplier(String ignType, int weightClass) {
        return switch (ignType) {
            case "wyvernheart" -> switch (weightClass) {
                case 0 -> 0.6f;  // Light: weaker sustained fire, but lighter & more mobile
                case 1 -> 0.85f; // Medium: moderate
                default -> 1.2f; // Heavy: strongest sustained fire
            };
            case "wyvernpiercer" -> switch (weightClass) {
                case 0 -> 0.7f;
                case 1 -> 0.9f;
                default -> 1.3f;  // Heavy: biggest pierce
            };
            case "wyverncounter" -> switch (weightClass) {
                case 0 -> 1.1f;  // Light: actually decent — agile counter
                case 1 -> 1.0f;
                default -> 0.8f; // Heavy: less suited for counter play
            };
            case "wyvernblast" -> switch (weightClass) {
                case 0 -> 1.0f;
                case 1 -> 1.1f;  // Medium: balanced area control
                default -> 1.3f; // Heavy: biggest blast
            };
            default -> 1.0f;
        };
    }

    // ══════════════════════════════════════════════════════════════════
    //  Weapon Action (X key) — Mode switch / Chaser Shot
    // ══════════════════════════════════════════════════════════════════

    private static void handleWeaponAction(Player player, boolean pressed, ItemStack stack,
                                            PlayerWeaponState state) {
        if (!pressed) return;

        // Chaser shot after fire (Light/Medium only)
        if (state.getBowgunLastAction() == 1 && state.getBowgunRecoilTimer() < 4
                && BowgunItem.getWeightClass(stack) <= 1) {
            String ammo = state.getBowgunCurrentAmmo();
            if (!ammo.isEmpty() && BowgunMagazineManager.consumeRound(stack, ammo)) {
                spawnProjectile(player, stack, state, ammo, 1.3f);
                state.setBowgunGauge(state.getBowgunGauge() + GAUGE_REGEN_ON_HIT);
                state.setBowgunRecoilTimer(6);
                state.setBowgunLastAction(5); // CHASER_SHOT
                LOG.debug("[Bowgun] Chaser Shot fired");
                return;
            }
        }

        // Mode switch
        int weightClass = BowgunItem.getWeightClass(stack);
        int currentMode = state.getBowgunMode();
        if (currentMode == MODE_STANDARD) {
            int newMode = switch (weightClass) {
                case 0 -> MODE_RAPID;
                case 1 -> MODE_VERSATILE;
                default -> MODE_IGNITION;
            };
            state.setBowgunMode(newMode);
            state.setBowgunModeSwitchTicks(5);
            LOG.debug("[Bowgun] Switched to mode {} (weight class {})", newMode, weightClass);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CROSSBOW_LOADING_MIDDLE, SoundSource.PLAYERS, 0.7f, 1.5f);
        } else {
            state.setBowgunMode(MODE_STANDARD);
            state.setBowgunModeSwitchTicks(5);
            LOG.debug("[Bowgun] Switched back to STANDARD");
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CROSSBOW_LOADING_MIDDLE, SoundSource.PLAYERS, 0.7f, 1.0f);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  Reload (BOWGUN_RELOAD / R key)
    // ══════════════════════════════════════════════════════════════════

    private static void handleReload(Player player, boolean pressed, ItemStack stack,
                                      PlayerWeaponState state) {
        if (!pressed) return;
        if (state.getBowgunReloadTimer() > 0) return;
        if (state.getBowgunRecoilTimer() > 0) return;

        String ammo = state.getBowgunCurrentAmmo();
        if (ammo.isEmpty()) {
            ammo = BowgunMagazineManager.findFirstLoadedAmmo(player, stack);
            state.setBowgunCurrentAmmo(ammo);
        }

        if (BowgunMagazineManager.tryReload(player, stack, ammo)) {
            int reloadTicks = BowgunMagazineManager.getReloadTicks(stack, ammo);
            state.setBowgunReloadTimer(reloadTicks);
            LOG.debug("[Bowgun] Reload started for {} — {} ticks", ammo, reloadTicks);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CROSSBOW_LOADING_START, SoundSource.PLAYERS, 0.5f, 1.0f);
        } else {
            LOG.debug("[Bowgun] Reload failed — full or no ammo");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  Special (F key) — Special Ammo / Focus Blast
    // ══════════════════════════════════════════════════════════════════

    private static void handleSpecial(Player player, boolean pressed, ItemStack stack,
                                       PlayerWeaponState state) {
        if (!pressed) return;

        int weightClass = BowgunItem.getWeightClass(stack);

        if (state.isBowgunAiming()) {
            // Focus Blast while aiming
            float cost = 30.0f;
            if (state.getBowgunGauge() < cost) return;
            state.setBowgunGauge(state.getBowgunGauge() - cost);

            String focusType = switch (weightClass) {
                case 0 -> "eagle_strike";
                case 1 -> "hawk_barrage";
                default -> "wyvern_howl";
            };
            spawnSpecialProjectile(player, stack, state, focusType);
            state.setBowgunRecoilTimer(15);
            state.setBowgunLastAction(6); // FOCUS_BLAST
            LOG.debug("[Bowgun] Focus Blast: {}", focusType);
            return;
        }

        // Special Ammo (Light/Medium only)
        if (weightClass <= 1) {
            if (state.getBowgunSpecialAmmoTimer() > 0) {
                LOG.debug("[Bowgun] Special ammo on cooldown: {}", state.getBowgunSpecialAmmoTimer());
                return;
            }
            if (weightClass == 0) {
                // Wyvernblast mine
                spawnSpecialProjectile(player, stack, state, "wyvernblast_mine");
                state.setBowgunSpecialAmmoTimer(200); // 10s cooldown
                LOG.debug("[Bowgun] Wyvernblast Mine placed");
            } else {
                // Adhesive Ammo
                spawnSpecialProjectile(player, stack, state, "adhesive");
                state.setBowgunSpecialAmmoTimer(200);
                LOG.debug("[Bowgun] Adhesive Ammo placed");
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.6f, 0.8f);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  Guard (GUARD action)
    // ══════════════════════════════════════════════════════════════════

    private static void handleGuard(Player player, boolean pressed, ItemStack stack,
                                     PlayerWeaponState state) {
        int weightClass = BowgunItem.getWeightClass(stack);
        List<String> mods = BowgunItem.getInstalledMods(stack);
        boolean guardEnabled = BowgunModResolver.resolveGuardEnabled(mods);

        if (!guardEnabled || weightClass == 0) {
            LOG.debug("[Bowgun] Guard not available (weight={}, guardEnabled={})", weightClass, guardEnabled);
            // Melee bash fallback
            if (pressed) {
                handleMeleeBash(player, stack, state);
            }
            return;
        }

        BowgunGuardHandler.handleGuard(player, pressed, stack, state);
    }

    // ══════════════════════════════════════════════════════════════════
    //  Melee Bash (WEAPON_ALT / C key)
    // ══════════════════════════════════════════════════════════════════

    static void handleMeleeBash(Player player, ItemStack stack, PlayerWeaponState state) {
        if (state.getBowgunRecoilTimer() > 0 || state.getBowgunReloadTimer() > 0) return;
        state.setBowgunRecoilTimer(10);
        state.setBowgunLastAction(7); // MELEE_BASH
        // Melee damage is handled via BC or direct entity sweep
        LOG.debug("[Bowgun] Melee Bash");
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.5f, 1.3f);
    }

    // ══════════════════════════════════════════════════════════════════
    //  Tick (called from WeaponStateEvents every server tick)
    // ══════════════════════════════════════════════════════════════════

    public static void tick(Player player, PlayerWeaponState state) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof BowgunItem)) return;

        // Recoil timer countdown
        if (state.getBowgunRecoilTimer() > 0) {
            state.setBowgunRecoilTimer(state.getBowgunRecoilTimer() - 1);
        }

        // Reload timer countdown
        if (state.getBowgunReloadTimer() > 0) {
            state.setBowgunReloadTimer(state.getBowgunReloadTimer() - 1);
            if (state.getBowgunReloadTimer() == 0) {
                LOG.debug("[Bowgun] Reload complete");
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 0.5f, 1.0f);
            }
        }

        // Mode switch animation
        if (state.getBowgunModeSwitchTicks() > 0) {
            state.setBowgunModeSwitchTicks(state.getBowgunModeSwitchTicks() - 1);
        }

        // Special ammo cooldown
        if (state.getBowgunSpecialAmmoTimer() > 0) {
            state.setBowgunSpecialAmmoTimer(state.getBowgunSpecialAmmoTimer() - 1);
        }

        // Guard tick (must be called every tick to advance perfect guard window)
        BowgunGuardHandler.tickGuard(player, state);

        // Gauge regeneration
        int mode = state.getBowgunMode();
        if (mode == MODE_STANDARD) {
            // Passive regen in standard mode
            if (state.getBowgunGauge() < GAUGE_MAX) {
                state.setBowgunGauge(Math.min(GAUGE_MAX,
                        state.getBowgunGauge() + GAUGE_REGEN_PER_TICK));
            }
        }

        // Wyvernheart sustained fire tick — constant fire (every tick)
        if (mode == MODE_IGNITION && state.isBowgunFiring()) {
            if (state.getBowgunGauge() < IGNITION_COST_PER_TICK) {
                // Gauge depleted — stop firing
                state.setBowgunFiring(false);
                state.setBowgunSustainedHits(0);
                state.setBowgunMode(MODE_STANDARD);
                LOG.debug("[Bowgun] Wyvernheart gauge depleted, back to standard");
            } else {
                state.setBowgunGauge(state.getBowgunGauge() - IGNITION_COST_PER_TICK);
                // Constant fire — every tick for true sustained damage
                if (player.level() instanceof ServerLevel) {
                    int sustained = state.getBowgunSustainedHits();
                    float dmgMult = Math.min(2.5f, 0.4f + sustained * 0.03f);
                    String ignType = BowgunItem.getIgnitionType(stack);
                    if (ignType.isEmpty()) ignType = "wyvernheart";
                    // Apply weight-class multiplier for ignition type
                    float weightMult = getIgnitionWeightMultiplier(ignType, BowgunItem.getWeightClass(stack));
                    dmgMult *= weightMult;
                    // Use current ammo type (ignition enhances whatever is loaded)
                    String ignAmmo = state.getBowgunCurrentAmmo();
                    if (ignAmmo.isEmpty()) ignAmmo = "normal_1";
                    spawnProjectile(player, stack, state, ignAmmo, dmgMult);
                    state.setBowgunSustainedHits(sustained + 1);
                }
            }
        }

        // Rapid fire hold tick (long-press)
        if (mode == MODE_RAPID && state.isBowgunFiring()) {
            if (state.getBowgunGauge() < RAPID_FIRE_GAUGE_COST) {
                state.setBowgunFiring(false);
                LOG.debug("[Bowgun] Rapid fire gauge depleted, stop hold");
            } else if (state.getBowgunReloadTimer() == 0 && state.getBowgunRecoilTimer() == 0) {
                if (player.tickCount % 3 == 0) {
                    fireRapid(player, stack, state);
                }
            }
        }

        // Auto-guard check (Heavy, weight >= 70)
        int weight = BowgunItem.getWeight(stack);
        boolean autoGuard = weight >= 70
                && BowgunModResolver.resolveGuardEnabled(BowgunItem.getInstalledMods(stack))
                && state.getBowgunRecoilTimer() == 0
                && state.getBowgunReloadTimer() == 0
                && !state.isBowgunFiring();
        state.setBowgunAutoGuard(autoGuard);

        // Reset last action after window expires
        if (state.getBowgunLastAction() > 0 && state.getBowgunRecoilTimer() <= 0
                && state.getBowgunReloadTimer() <= 0) {
            state.setBowgunLastAction(0);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  On-hit gauge gain (called from WeaponStateEvents.onHit)
    // ══════════════════════════════════════════════════════════════════

    public static void onHit(Player player, PlayerWeaponState state) {
        state.setBowgunGauge(Math.min(GAUGE_MAX,
                state.getBowgunGauge() + GAUGE_REGEN_ON_HIT));
    }

    // ══════════════════════════════════════════════════════════════════
    //  Projectile spawning
    // ══════════════════════════════════════════════════════════════════

    private static void spawnProjectile(Player player, ItemStack bowgun,
                                         PlayerWeaponState state, String ammoId,
                                         float damageMultiplier) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        BowgunMagazineManager.AmmoData data = BowgunMagazineManager.AMMO_TABLE.get(ammoId);
        if (data == null) {
            LOG.warn("[Bowgun] Unknown ammo type for projectile: {}", ammoId);
            return;
        }

        // Damage resolution
        List<String> mods = BowgunItem.getInstalledMods(bowgun);
        float modDmgMult = BowgunModResolver.resolveDamageMultiplier(mods);
        // ADS bonus: +10% damage when aiming
        float aimBonus = state.isBowgunAiming() ? 1.10f : 1.0f;
        float finalDamage = data.baseDamage * damageMultiplier * modDmgMult * aimBonus;

        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();

        if (data.pelletCount > 1) {
            // Spread ammo — spawn multiple pellets
            for (int i = 0; i < data.pelletCount; i++) {
                float spreadRad = (float) Math.toRadians(data.spreadAngle);
                float yaw = (float) ((Math.random() - 0.5) * 2 * spreadRad);
                float pitch = (float) ((Math.random() - 0.5) * 2 * spreadRad);
                Vec3 dir = look.yRot(yaw).xRot(pitch).normalize();

                AmmoProjectileEntity proj = new AmmoProjectileEntity(serverLevel, player);
                proj.configure(ammoId, finalDamage / data.pelletCount,
                        data.elementDamage / data.pelletCount, data.statusValue,
                        data.speed, data.gravity, 0, data.pierceCount);
                proj.setPos(eyePos.x, eyePos.y - 0.1, eyePos.z);
                proj.shoot(dir.x, dir.y, dir.z, data.speed, 0.0f);
                serverLevel.addFreshEntity(proj);
            }
        } else {
            AmmoProjectileEntity proj = new AmmoProjectileEntity(serverLevel, player);
            proj.configure(ammoId, finalDamage, data.elementDamage, data.statusValue,
                    data.speed, data.gravity, data.pelletCount, data.pierceCount);
            proj.setPos(eyePos.x, eyePos.y - 0.1, eyePos.z);

            float spread = state.isBowgunAiming() ? 0.0f : 1.5f;
            proj.shoot(look.x, look.y, look.z, data.speed, spread);
            serverLevel.addFreshEntity(proj);
        }

        // Sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.8f, 1.2f);

        LOG.debug("[Bowgun] Spawned projectile ammo={} dmg={} pellets={}",
                ammoId, finalDamage, data.pelletCount);
    }

    private static void spawnSpecialProjectile(Player player, ItemStack bowgun,
                                                PlayerWeaponState state, String type) {
        spawnSpecialProjectile(player, bowgun, state, type, 1.0f);
    }

    private static void spawnSpecialProjectile(Player player, ItemStack bowgun,
                                                PlayerWeaponState state, String type,
                                                float weightMultiplier) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();

        // All special projectiles use the same base entity with type flags
        float damage = switch (type) {
            case "wyvernpiercer" -> 12.0f;
            case "wyverncounter" -> 15.0f;
            case "wyvernblast" -> 18.0f;
            case "wyvernblast_mine" -> 14.0f;
            case "adhesive" -> 10.0f;
            case "eagle_strike" -> 16.0f;
            case "hawk_barrage" -> 8.0f;
            case "wyvern_howl" -> 25.0f;
            default -> 10.0f;
        };

        List<String> mods = BowgunItem.getInstalledMods(bowgun);
        damage *= BowgunModResolver.resolveDamageMultiplier(mods) * weightMultiplier;

        AmmoProjectileEntity proj = new AmmoProjectileEntity(serverLevel, player);
        proj.configure(type, damage, 0, 0, 2.0f, 0.01f, 1, type.equals("wyvernpiercer") ? 8 : 0);
        proj.setSpecialType(type);
        proj.setPos(eyePos.x, eyePos.y - 0.1, eyePos.z);
        proj.shoot(look.x, look.y, look.z, 2.0f, 0.0f);
        serverLevel.addFreshEntity(proj);

        LOG.debug("[Bowgun] Spawned special projectile type={} dmg={}", type, damage);
    }

    // ══════════════════════════════════════════════════════════════════
    //  Ammo selection (called from AmmoSwitchPacket)
    // ══════════════════════════════════════════════════════════════════

    public static void switchAmmo(Player player, PlayerWeaponState state, String ammoId) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof BowgunItem)) return;

        Set<String> compatible = BowgunMagazineManager.getCompatibleAmmo(stack);
        if (!compatible.contains(ammoId)) {
            LOG.debug("[Bowgun] Ammo {} not compatible with current loadout", ammoId);
            return;
        }
        state.setBowgunCurrentAmmo(ammoId);
        BowgunItem.setCurrentAmmo(stack, ammoId);
        LOG.debug("[Bowgun] Switched ammo to {}", ammoId);
    }

    /** Cycle to next available ammo type */
    public static void cycleAmmo(Player player, PlayerWeaponState state, boolean forward) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof BowgunItem)) return;

        Set<String> compatible = BowgunMagazineManager.getCompatibleAmmo(stack);
        if (compatible.isEmpty()) return;

        String current = state.getBowgunCurrentAmmo();
        java.util.List<String> list = new java.util.ArrayList<>(compatible);
        int idx = list.indexOf(current);
        if (idx < 0) idx = 0;

        if (forward) {
            idx = (idx + 1) % list.size();
        } else {
            idx = (idx - 1 + list.size()) % list.size();
        }

        String next = list.get(idx);
        state.setBowgunCurrentAmmo(next);
        BowgunItem.setCurrentAmmo(stack, next);
        LOG.debug("[Bowgun] Cycled ammo to {} (index {})", next, idx);
    }
}
