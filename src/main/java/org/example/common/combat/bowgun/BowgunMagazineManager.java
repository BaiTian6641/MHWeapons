package org.example.common.combat.bowgun;

import java.util.*;
import com.google.gson.JsonArray;
import net.minecraft.resources.ResourceLocation;
import org.example.MHWeaponsMod;
import org.example.common.data.WeaponData;
import org.example.common.data.WeaponDataManager;
import org.example.item.WeaponIdProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.example.item.AmmoItem;
import org.example.item.BowgunItem;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Manages per-ammo magazine counts, reload logic, ammo switching, and
 * compatibility checks for the Bowgun weapon system.
 */
@SuppressWarnings("null")
public final class BowgunMagazineManager {
    private static final Logger LOG = LogUtils.getLogger();

    // ── Base ammo data (hard-coded; mirrors ammo_types.json) ─────────
    // This could be made data-driven later.

    public static final Map<String, AmmoData> AMMO_TABLE = new LinkedHashMap<>();
    private static final Map<String, RangeProfile> RANGE_PROFILES = new HashMap<>();

    // Default ammo that every bowgun can load (no mod required)
    public static final Set<String> DEFAULT_AMMO = new LinkedHashSet<>(Arrays.asList(
            "normal_1", "normal_2", "normal_3",
            "pierce_1", "pierce_2", "pierce_3",
            "spread_1", "spread_2", "spread_3",
            "sticky_1", "sticky_2",
            "cluster_1",
            "wyvern_ammo",
            "tranq_ammo"
    ));

    // Ammo that can rapid-fire by default (on Light builds)
    public static final Set<String> DEFAULT_RAPID_FIRE = new LinkedHashSet<>(Arrays.asList(
            "normal_1", "normal_2", "pierce_1", "spread_1", "fire_ammo", "water_ammo"
    ));

    static {
        // Normal (defaults, overridden by JSON if present)
        a("normal_1",  4.0f,  0, 0, 3.0f, 0.01f,  0, 1, 0f,   1, "fast",     8, 99,  3, 0.6f);
        a("normal_2",  7.0f,  0, 0, 3.2f, 0.008f, 0, 1, 0f,   2, "normal",   6, 99,  3, 0.55f);
        a("normal_3", 10.0f,  0, 0, 3.5f, 0.005f, 0, 1, 0f,   3, "normal",   4, 60,  0, 0f);
        // Pierce
        a("pierce_1",  3.0f,  0, 0, 3.5f, 0.005f, 3, 1, 0f,   2, "normal",   5, 60,  3, 0.5f);
        a("pierce_2",  4.5f,  0, 0, 3.8f, 0.003f, 4, 1, 0f,   3, "slow",     4, 60,  0, 0f);
        a("pierce_3",  6.0f,  0, 0, 4.0f, 0.002f, 5, 1, 0f,   4, "slow",     3, 40,  0, 0f);
        // Spread (per-pellet damage; total = dmg × pelletCount)
        a("spread_1",  4.0f,  0, 0, 2.5f, 0.02f,  0, 3,15f,   2, "fast",     6, 60,  2, 0.65f);
        a("spread_2",  5.0f,  0, 0, 2.5f, 0.02f,  0, 5,18f,   3, "normal",   4, 60,  0, 0f);
        a("spread_3",  6.5f,  0, 0, 2.5f, 0.02f,  0, 7,22f,   4, "slow",     3, 40,  0, 0f);
        // Sticky
        a("sticky_1",  6.0f,  0, 0, 2.8f, 0.015f, 0, 1, 0f,   3, "slow",     3, 30,  0, 0f);
        a("sticky_2",  9.0f,  0, 0, 2.8f, 0.015f, 0, 1, 0f,   4, "slow",     2, 20,  0, 0f);
        a("sticky_3", 12.0f,  0, 0, 2.8f, 0.015f, 0, 1, 0f,   5, "very_slow",1, 10,  0, 0f);
        // Cluster
        a("cluster_1", 4.0f,  0, 0, 2.0f, 0.04f,  0, 1, 0f,   4, "very_slow",2, 10,  0, 0f);
        a("cluster_2", 5.0f,  0, 0, 2.0f, 0.04f,  0, 1, 0f,   5, "very_slow",1,  5,  0, 0f);
        a("cluster_3", 6.0f,  0, 0, 2.0f, 0.04f,  0, 1, 0f,   5, "very_slow",1,  3,  0, 0f);
        // Elemental
        a("fire_ammo",    2.0f, 8.0f, 0, 3.0f, 0.01f, 0, 1, 0f, 2, "normal", 5, 60, 3, 0.6f);
        a("water_ammo",   2.0f, 8.0f, 0, 3.0f, 0.01f, 0, 1, 0f, 2, "normal", 5, 60, 3, 0.6f);
        a("thunder_ammo", 2.0f, 8.0f, 0, 3.5f, 0.005f,0, 1, 0f, 2, "normal", 5, 60, 3, 0.55f);
        a("ice_ammo",     2.0f, 8.0f, 0, 3.0f, 0.01f, 0, 1, 0f, 2, "normal", 5, 60, 3, 0.55f);
        a("dragon_ammo",  3.0f,12.0f, 0, 3.5f, 0.005f,2, 1, 0f, 4, "very_slow",3,20, 0, 0f);
        // Status
        a("poison_ammo_1",    2.0f, 0, 15, 3.0f, 0.01f, 0, 1, 0f, 2, "normal", 4, 30, 0, 0f);
        a("poison_ammo_2",    2.5f, 0, 25, 3.0f, 0.01f, 0, 1, 0f, 3, "slow",   3, 20, 0, 0f);
        a("paralysis_ammo_1", 2.0f, 0, 12, 3.0f, 0.01f, 0, 1, 0f, 2, "normal", 3, 20, 0, 0f);
        a("paralysis_ammo_2", 2.5f, 0, 20, 3.0f, 0.01f, 0, 1, 0f, 3, "slow",   2, 12, 0, 0f);
        a("sleep_ammo_1",     2.0f, 0, 15, 3.0f, 0.01f, 0, 1, 0f, 2, "normal", 3, 20, 0, 0f);
        a("sleep_ammo_2",     2.5f, 0, 25, 3.0f, 0.01f, 0, 1, 0f, 3, "slow",   2, 12, 0, 0f);
        a("exhaust_ammo_1",   3.0f, 0, 10, 2.8f, 0.015f,0, 1, 0f, 2, "normal", 4, 30, 0, 0f);
        a("exhaust_ammo_2",   3.5f, 0, 18, 2.8f, 0.015f,0, 1, 0f, 3, "slow",   3, 20, 0, 0f);
        // Support
        a("recovery_ammo_1",  0f, 0, 0, 3.0f, 0.01f, 0, 1, 0f, 1, "fast", 5, 30, 0, 0f);
        a("recovery_ammo_2",  0f, 0, 0, 3.0f, 0.01f, 0, 1, 0f, 2, "normal", 3, 20, 0, 0f);
        a("demon_ammo",       0f, 0, 0, 3.0f, 0.01f, 0, 1, 0f, 1, "fast", 3, 10, 0, 0f);
        a("armor_ammo",       0f, 0, 0, 3.0f, 0.01f, 0, 1, 0f, 1, "fast", 3, 10, 0, 0f);
        a("tranq_ammo",       0f, 0, 0, 3.0f, 0.01f, 0, 1, 0f, 1, "fast", 4, 10, 0, 0f);
        // Wyvern
        a("wyvern_ammo",     25.0f, 0, 0, 1.5f, 0.0f, 0, 1, 0f, 5, "very_slow", 1, 5, 0, 0f);
        // Slicing & Flaming
        a("slicing_ammo",     5.0f, 0, 0, 3.0f, 0.01f, 0, 1, 0f, 3, "slow",     3, 30, 0, 0f);
        a("flaming_ammo",     3.0f, 10.0f, 0, 3.0f, 0.01f, 0, 1, 0f, 3, "normal", 4, 30, 0, 0f);

        // Range profiles (min, optimal, max in blocks)
        // Normal
        addRange("normal_1", 4f, 12f, 24f);
        addRange("normal_2", 4f, 12f, 24f);
        addRange("normal_3", 4f, 12f, 24f);
        // Pierce
        addRange("pierce_1", 8f, 20f, 36f);
        addRange("pierce_2", 8f, 20f, 36f);
        addRange("pierce_3", 8f, 20f, 36f);
        // Spread (close range)
        addRange("spread_1", 0f, 6f, 12f);
        addRange("spread_2", 0f, 6f, 12f);
        addRange("spread_3", 0f, 6f, 12f);
        // Sticky / Cluster
        addRange("sticky_1", 6f, 14f, 26f);
        addRange("sticky_2", 6f, 14f, 26f);
        addRange("sticky_3", 6f, 14f, 26f);
        addRange("cluster_1", 6f, 16f, 28f);
        addRange("cluster_2", 6f, 16f, 28f);
        addRange("cluster_3", 6f, 16f, 28f);
        // Elemental
        addRange("fire_ammo", 6f, 14f, 28f);
        addRange("water_ammo", 6f, 14f, 28f);
        addRange("thunder_ammo", 6f, 14f, 28f);
        addRange("ice_ammo", 6f, 14f, 28f);
        addRange("dragon_ammo", 8f, 16f, 30f);
        // Status
        addRange("poison_ammo_1", 6f, 14f, 28f);
        addRange("poison_ammo_2", 6f, 14f, 28f);
        addRange("paralysis_ammo_1", 6f, 14f, 28f);
        addRange("paralysis_ammo_2", 6f, 14f, 28f);
        addRange("sleep_ammo_1", 6f, 14f, 28f);
        addRange("sleep_ammo_2", 6f, 14f, 28f);
        addRange("exhaust_ammo_1", 6f, 14f, 28f);
        addRange("exhaust_ammo_2", 6f, 14f, 28f);
        // Support
        addRange("recovery_ammo_1", 0f, 10f, 20f);
        addRange("recovery_ammo_2", 0f, 10f, 20f);
        addRange("demon_ammo", 0f, 10f, 20f);
        addRange("armor_ammo", 0f, 10f, 20f);
        addRange("tranq_ammo", 0f, 10f, 20f);
        // Wyvern
        addRange("wyvern_ammo", 8f, 18f, 30f);
        // Slicing / Flaming
        addRange("slicing_ammo", 6f, 14f, 26f);
        addRange("flaming_ammo", 6f, 14f, 26f);
    }

    private static void a(String id, float dmg, float elemDmg, float statusVal,
                           float speed, float gravity, int pierce, int pellets, float spread,
                           int recoil, String reload, int magCap, int maxStack,
                           int rapidBurst, float rapidMult) {
        AMMO_TABLE.put(id, new AmmoData(id, dmg, elemDmg, statusVal, speed, gravity,
                pierce, pellets, spread, recoil, reload, magCap, maxStack, rapidBurst, rapidMult));
    }

    private BowgunMagazineManager() {}

    public static void loadFromJson(Map<String, AmmoData> ammoData,
                                    Map<String, RangeProfile> ranges) {
        AMMO_TABLE.clear();
        AMMO_TABLE.putAll(ammoData);
        RANGE_PROFILES.clear();
        RANGE_PROFILES.putAll(ranges);
        LOG.info("[Bowgun] Loaded ammo table from JSON: {} entries", AMMO_TABLE.size());
    }

    public static RangeProfile getRangeProfile(String ammoId) {
        return RANGE_PROFILES.getOrDefault(ammoId, RangeProfile.DEFAULT);
    }

    private static void addRange(String id, float min, float optimal, float max) {
        RANGE_PROFILES.put(id, new RangeProfile(min, optimal, max));
    }

    // ── Public API ───────────────────────────────────────────────────

    /**
     * Returns the set of ammo types the bowgun can load based on
     * its installed mods. If no mods are installed (placeholder bowgun),
     * all ammo types are available.
     */
    public static Set<String> getCompatibleAmmo(ItemStack bowgunStack) {
        if (bowgunStack.getItem() instanceof WeaponIdProvider weaponIdProvider) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MHWeaponsMod.MODID, weaponIdProvider.getWeaponId());
            WeaponData data = WeaponDataManager.INSTANCE.get(id);
            if (data != null && data.getJson().has("compatibleAmmo") && data.getJson().get("compatibleAmmo").isJsonArray()) {
                JsonArray arr = data.getJson().getAsJsonArray("compatibleAmmo");
                Set<String> result = new LinkedHashSet<>();
                for (int i = 0; i < arr.size(); i++) {
                    String ammoId = arr.get(i).getAsString();
                    if (AMMO_TABLE.containsKey(ammoId)) {
                        result.add(ammoId);
                    }
                }
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }
        // Fallback: all ammo types available
        return new LinkedHashSet<>(AMMO_TABLE.keySet());
    }

    /**
     * Get the effective magazine capacity for an ammo type on this bowgun.
     */
    public static int getEffectiveMagCapacity(ItemStack bowgunStack, String ammoId) {
        AmmoData data = AMMO_TABLE.get(ammoId);
        if (data == null) return 0;
        List<String> mods = BowgunItem.getInstalledMods(bowgunStack);
        int bonus = BowgunModResolver.resolveCapacityBonus(mods);
        return Math.max(1, data.magazineCapacity + bonus);
    }

    /**
     * Check if an ammo type can rapid-fire on this bowgun.
     */
    public static boolean canRapidFire(ItemStack bowgunStack, String ammoId) {
        AmmoData data = AMMO_TABLE.get(ammoId);
        if (data == null || data.rapidFireBurst <= 0) {
            // Check mod-added rapid fire
            List<String> mods = BowgunItem.getInstalledMods(bowgunStack);
            return BowgunModResolver.resolveRapidFireExtras(mods).contains(ammoId);
        }
        return true; // has native rapid fire
    }

    /**
     * Get rapid fire burst count for ammo type.
     */
    public static int getRapidFireBurst(String ammoId) {
        AmmoData data = AMMO_TABLE.get(ammoId);
        if (data == null) return 1;
        return data.rapidFireBurst > 0 ? data.rapidFireBurst : 3; // default 3 for mod-added
    }

    /**
     * Get rapid fire damage multiplier per round.
     */
    public static float getRapidFireDmgMult(String ammoId) {
        AmmoData data = AMMO_TABLE.get(ammoId);
        if (data == null) return 0.6f;
        return data.rapidFireDmgMult > 0 ? data.rapidFireDmgMult : 0.6f;
    }

    /**
     * Get reload time in ticks for ammo type with bowgun mods applied.
     */
    public static int getReloadTicks(ItemStack bowgunStack, String ammoId) {
        AmmoData data = AMMO_TABLE.get(ammoId);
        if (data == null) return 20;
        int base = switch (data.reloadSpeed) {
            case "fast" -> 12;
            case "normal" -> 20;
            case "slow" -> 30;
            case "very_slow" -> 45;
            default -> 20;
        };
        List<String> mods = BowgunItem.getInstalledMods(bowgunStack);
        float mult = BowgunModResolver.resolveReloadMultiplier(mods);
        // Weight penalty: heavier = slower
        int weight = BowgunItem.getWeight(bowgunStack);
        float weightMult = 1.0f + (weight - 40) * 0.005f; // slight penalty/bonus
        return Math.max(6, Math.round(base * mult * weightMult));
    }

    /**
     * Get effective recoil level for ammo type on this bowgun.
     */
    public static int getEffectiveRecoil(ItemStack bowgunStack, String ammoId) {
        AmmoData data = AMMO_TABLE.get(ammoId);
        if (data == null) return 3;
        List<String> mods = BowgunItem.getInstalledMods(bowgunStack);
        int recoilMod = BowgunModResolver.resolveRecoilModifier(mods);
        int weight = BowgunItem.getWeight(bowgunStack);
        int weightPenalty = weight / 25; // 0-4 based on weight
        int effective = data.recoil + weightPenalty + recoilMod;
        return Math.max(1, Math.min(5, effective));
    }

    /** Recovery ticks from recoil level */
    public static int getRecoilRecoveryTicks(int recoilLevel) {
        return switch (recoilLevel) {
            case 1 -> 2;
            case 2 -> 4;
            case 3 -> 8;
            case 4 -> 12;
            case 5 -> 16;
            default -> 8;
        };
    }

    /**
     * Try to reload current ammo. Returns true if reload started.
     */
    public static boolean tryReload(Player player, ItemStack bowgunStack, String ammoId) {
        if (ammoId == null || ammoId.isEmpty()) return false;
        if (!getCompatibleAmmo(bowgunStack).contains(ammoId)) return false;

        int current = BowgunItem.getMagazineCount(bowgunStack, ammoId);
        int max = getEffectiveMagCapacity(bowgunStack, ammoId);
        if (current >= max) return false;

        // Check inventory for ammo items
        int available = countAmmoInInventory(player, ammoId);
        if (available <= 0) return false;

        int needed = max - current;
        int toLoad = Math.min(needed, available);
        consumeAmmoFromInventory(player, ammoId, toLoad);
        BowgunItem.setMagazineCount(bowgunStack, ammoId, current + toLoad);
        LOG.debug("[Bowgun] Reloaded {} x{} (now {}/{})", ammoId, toLoad, current + toLoad, max);
        return true;
    }

    /**
     * Consume one round from the magazine. Returns true if successful.
     */
    public static boolean consumeRound(ItemStack bowgunStack, String ammoId) {
        int current = BowgunItem.getMagazineCount(bowgunStack, ammoId);
        if (current <= 0) return false;
        BowgunItem.setMagazineCount(bowgunStack, ammoId, current - 1);
        return true;
    }

    // ── Inventory helpers ────────────────────────────────────────────

    public static int countAmmoInInventory(Player player, String ammoId) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (!slot.isEmpty() && slot.getItem() instanceof AmmoItem ai && ai.getAmmoTypeId().equals(ammoId)) {
                count += slot.getCount();
            }
        }
        return count;
    }

    public static void consumeAmmoFromInventory(Player player, String ammoId, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (!slot.isEmpty() && slot.getItem() instanceof AmmoItem ai && ai.getAmmoTypeId().equals(ammoId)) {
                int take = Math.min(remaining, slot.getCount());
                slot.shrink(take);
                remaining -= take;
            }
        }
        if (remaining > 0) {
            LOG.warn("[Bowgun] Could not consume {} of ammo {}, {} remaining", amount, ammoId, remaining);
        }
    }

    /** Returns the first compatible ammo ID that has rounds in inventory. */
    public static String findFirstLoadedAmmo(Player player, ItemStack bowgunStack) {
        for (String ammoId : getCompatibleAmmo(bowgunStack)) {
            if (BowgunItem.getMagazineCount(bowgunStack, ammoId) > 0) return ammoId;
        }
        // Fallback: find first ammo in inventory
        for (String ammoId : getCompatibleAmmo(bowgunStack)) {
            if (countAmmoInInventory(player, ammoId) > 0) return ammoId;
        }
        return "normal_1";
    }

    // ── Ammo data record ─────────────────────────────────────────────

    public static class AmmoData {
        public final String id;
        public final float baseDamage;
        public final float elementDamage;
        public final float statusValue;
        public final float speed;
        public final float gravity;
        public final int pierceCount;
        public final int pelletCount;
        public final float spreadAngle;
        public final int recoil;
        public final String reloadSpeed;
        public final int magazineCapacity;
        public final int maxStack;
        public final int rapidFireBurst;
        public final float rapidFireDmgMult;

        public AmmoData(String id, float baseDamage, float elemDmg, float statusVal,
                         float speed, float gravity, int pierce, int pellets, float spread,
                         int recoil, String reload, int magCap, int maxStack,
                         int rapidBurst, float rapidMult) {
            this.id = id;
            this.baseDamage = baseDamage;
            this.elementDamage = elemDmg;
            this.statusValue = statusVal;
            this.speed = speed;
            this.gravity = gravity;
            this.pierceCount = pierce;
            this.pelletCount = pellets;
            this.spreadAngle = spread;
            this.recoil = recoil;
            this.reloadSpeed = reload;
            this.magazineCapacity = magCap;
            this.maxStack = maxStack;
            this.rapidFireBurst = rapidBurst;
            this.rapidFireDmgMult = rapidMult;
        }
    }

    public static class RangeProfile {
        public final float min;
        public final float optimal;
        public final float max;
        public static final RangeProfile DEFAULT = new RangeProfile(4f, 12f, 24f);

        public RangeProfile(float min, float optimal, float max) {
            this.min = min;
            this.optimal = optimal;
            this.max = max;
        }
    }
}
