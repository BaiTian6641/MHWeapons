package org.example.common.combat.bowgun;

import java.util.*;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Resolves aggregate stats from installed bowgun modifications.
 * All data is static/hard-coded for now; a data-driven JSON version
 * can be layered on top later.
 */
public final class BowgunModResolver {
    private static final Logger LOG = LogUtils.getLogger();

    // ── Mod weight table ─────────────────────────────────────────────
    private static final Map<String, Integer> MOD_WEIGHTS = new HashMap<>();
    private static final Map<String, String> MOD_CATEGORIES = new HashMap<>();
    private static final Map<String, String> MOD_IGNITION_TYPES = new HashMap<>();
    private static final Map<String, Set<String>> MOD_AMMO_UNLOCKS = new HashMap<>();
    private static final Map<String, List<String>> MOD_RAPID_FIRE_ADD = new HashMap<>();
    private static final Map<String, Float> MOD_RELOAD_MULT = new HashMap<>();
    private static final Map<String, Integer> MOD_RECOIL_MOD = new HashMap<>();
    private static final Map<String, Float> MOD_DAMAGE_MULT = new HashMap<>();
    private static final Map<String, Integer> MOD_CAPACITY_BONUS = new HashMap<>();
    private static final Map<String, Integer> MOD_GUARD_STRENGTH = new HashMap<>();
    private static final Map<String, Boolean> MOD_GUARD_ENABLED = new HashMap<>();
    private static final Map<String, Float> MOD_ADS_SWAY_REDUCTION = new HashMap<>();

    static {
        // Frame mods
        reg("light_frame",    "frame",  -20, 0, 0.85f, 0.95f, 0, false, 0.0f, null, null, null);
        reg("balanced_frame", "frame",    0, 0, 1.0f,  1.0f,  0, false, 0.0f, null, null, null);
        reg("heavy_frame",    "frame",   20,-1, 1.15f, 1.1f,  0, false, 0.0f, null, null, null);

        // Barrel mods
        reg("long_barrel",    "barrel",   5,-1, 1.0f,  1.0f,  0, false, 0.0f, null, null, null);
        reg("short_barrel",   "barrel",  -3, 0, 1.0f,  1.0f,  0, false, 0.0f, null, null, null);
        reg("silencer",       "barrel",  -1,-1, 1.0f,  0.95f, 0, false, 0.0f, null, null, null);

        // Stock mods
        reg("stabilizer_stock","stock",   4,-1, 1.0f,  1.0f,  0, false, 0.5f, null, null, null);
        reg("quick_stock",     "stock",  -2, 0, 0.8f,  1.0f,  0, false, 0.1f, null, null, null);

        // Magazine mods
        reg("extended_magazine","magazine", 3, 0, 1.0f, 1.0f, 2, false, 0.0f, null, null, null);
        reg("speed_loader",    "magazine",-1, 0, 0.7f,  1.0f,-1, false, 0.0f, null, null, null);

        // Shield mods
        reg("shield_mod_1", "shield",  8, 0, 1.0f, 1.0f, 0, true,  0.0f, null, null, null);
        MOD_GUARD_STRENGTH.put("shield_mod_1", 1);
        reg("shield_mod_2", "shield", 14, 0, 1.0f, 1.0f, 0, true,  0.0f, null, null, null);
        MOD_GUARD_STRENGTH.put("shield_mod_2", 2);
        reg("shield_mod_3", "shield", 20, 0, 1.0f, 1.0f, 0, true,  0.0f, null, null, null);
        MOD_GUARD_STRENGTH.put("shield_mod_3", 3);

        // Accessory mods – ignition cores
        reg("wyvernheart_core",   "accessory", 6, 0, 1.0f, 1.0f, 0, false, 0.0f, "wyvernheart",  null, null);
        reg("wyvernpiercer_core", "accessory", 6, 0, 1.0f, 1.0f, 0, false, 0.0f, "wyvernpiercer", null, null);
        reg("wyverncounter_core", "accessory", 4, 0, 1.0f, 1.0f, 0, false, 0.0f, "wyverncounter", null, null);
        reg("wyvernblast_core",   "accessory", 4, 0, 1.0f, 1.0f, 0, false, 0.0f, "wyvernblast",   null, null);

        // Special mods – scope
        reg("scope", "special", 2, 0, 1.0f, 1.0f, 0, false, 0.3f, null, null, null);

        // Special mods – rapid fire enabler
        reg("rapid_fire_enabler", "special", -5, 0, 1.0f, 1.0f, 0, false, 0.0f, null,
                null, Arrays.asList("normal_3", "pierce_2", "spread_2"));

        // Ammo expansion mods
        reg("elemental_barrel", "special", 2, 0, 1.0f, 1.0f, 0, false, 0.0f, null,
                new HashSet<>(Arrays.asList("fire_ammo", "water_ammo", "thunder_ammo", "ice_ammo", "dragon_ammo")), null);
        reg("status_loader", "special", 1, 0, 1.0f, 1.0f, 0, false, 0.0f, null,
                new HashSet<>(Arrays.asList("poison_ammo_1", "paralysis_ammo_1", "sleep_ammo_1", "exhaust_ammo_1")), null);
        reg("support_kit", "special", 0, 0, 1.0f, 1.0f, 0, false, 0.0f, null,
                new HashSet<>(Arrays.asList("recovery_ammo_1", "recovery_ammo_2", "demon_ammo", "armor_ammo")), null);

        LOG.debug("[Bowgun] BowgunModResolver static init: {} mods registered", MOD_WEIGHTS.size());
    }

    private static void reg(String id, String cat, int weight, int recoilMod, float reloadMult,
                             float dmgMult, int capBonus, boolean guardEnabled, float adsSway,
                             String ignitionType, Set<String> ammoUnlocks, List<String> rapidAdd) {
        MOD_WEIGHTS.put(id, weight);
        MOD_CATEGORIES.put(id, cat);
        MOD_RECOIL_MOD.put(id, recoilMod);
        MOD_RELOAD_MULT.put(id, reloadMult);
        MOD_DAMAGE_MULT.put(id, dmgMult);
        MOD_CAPACITY_BONUS.put(id, capBonus);
        MOD_GUARD_ENABLED.put(id, guardEnabled);
        MOD_ADS_SWAY_REDUCTION.put(id, adsSway);
        if (ignitionType != null) MOD_IGNITION_TYPES.put(id, ignitionType);
        if (ammoUnlocks != null) MOD_AMMO_UNLOCKS.put(id, ammoUnlocks);
        if (rapidAdd != null) MOD_RAPID_FIRE_ADD.put(id, rapidAdd);
    }

    private BowgunModResolver() {}

    // ── Query methods ────────────────────────────────────────────────

    public static int getModWeight(String modId) {
        return MOD_WEIGHTS.getOrDefault(modId, 0);
    }

    public static String getModCategory(String modId) {
        return MOD_CATEGORIES.getOrDefault(modId, "unknown");
    }

    /** Determine effective ignition type from list of installed mods.
     *  First ignition core found wins. */
    public static String resolveIgnitionType(List<String> mods) {
        for (String m : mods) {
            String t = MOD_IGNITION_TYPES.get(m);
            if (t != null) return t;
        }
        return "wyvernheart"; // default
    }

    public static int resolveRecoilModifier(List<String> mods) {
        int total = 0;
        for (String m : mods) {
            total += MOD_RECOIL_MOD.getOrDefault(m, 0);
        }
        return total;
    }

    public static float resolveReloadMultiplier(List<String> mods) {
        float mult = 1.0f;
        for (String m : mods) {
            mult *= MOD_RELOAD_MULT.getOrDefault(m, 1.0f);
        }
        return mult;
    }

    public static float resolveDamageMultiplier(List<String> mods) {
        float mult = 1.0f;
        for (String m : mods) {
            mult *= MOD_DAMAGE_MULT.getOrDefault(m, 1.0f);
        }
        return mult;
    }

    public static int resolveCapacityBonus(List<String> mods) {
        int total = 0;
        for (String m : mods) {
            total += MOD_CAPACITY_BONUS.getOrDefault(m, 0);
        }
        return total;
    }

    public static boolean resolveGuardEnabled(List<String> mods) {
        for (String m : mods) {
            if (MOD_GUARD_ENABLED.getOrDefault(m, false)) return true;
        }
        return false;
    }

    public static int resolveGuardStrength(List<String> mods) {
        int max = 0;
        for (String m : mods) {
            max = Math.max(max, MOD_GUARD_STRENGTH.getOrDefault(m, 0));
        }
        return max;
    }

    public static float resolveAdsSwayReduction(List<String> mods) {
        float total = 0.0f;
        for (String m : mods) {
            total += MOD_ADS_SWAY_REDUCTION.getOrDefault(m, 0.0f);
        }
        return Math.min(1.0f, total);
    }

    /** Returns all ammo types unlocked by installed special mods. */
    public static Set<String> resolveAmmoUnlocks(List<String> mods) {
        Set<String> set = new HashSet<>();
        for (String m : mods) {
            Set<String> u = MOD_AMMO_UNLOCKS.get(m);
            if (u != null) set.addAll(u);
        }
        return set;
    }

    /** Returns extra ammo types that gain rapid-fire from mods. */
    public static Set<String> resolveRapidFireExtras(List<String> mods) {
        Set<String> set = new HashSet<>();
        for (String m : mods) {
            List<String> r = MOD_RAPID_FIRE_ADD.get(m);
            if (r != null) set.addAll(r);
        }
        return set;
    }

    /** Returns all known mod IDs for iteration. */
    public static Set<String> getAllModIds() {
        return Collections.unmodifiableSet(MOD_WEIGHTS.keySet());
    }
}
