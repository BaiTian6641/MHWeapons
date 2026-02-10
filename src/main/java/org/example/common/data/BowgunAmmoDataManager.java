package org.example.common.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.example.MHWeaponsMod;
import org.example.common.combat.bowgun.BowgunMagazineManager;

public final class BowgunAmmoDataManager extends SimpleJsonResourceReloadListener {
    public static final BowgunAmmoDataManager INSTANCE = new BowgunAmmoDataManager();

    private BowgunAmmoDataManager() {
        super(new Gson(), "bowgun_ammo");
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> objects,
                         @Nonnull ResourceManager resourceManager,
                         @Nonnull ProfilerFiller profiler) {
        Map<String, BowgunMagazineManager.AmmoData> ammoData = new HashMap<>();
        Map<String, BowgunMagazineManager.RangeProfile> ranges = new HashMap<>();

        objects.forEach((id, element) -> {
            if (!element.isJsonObject()) {
                MHWeaponsMod.LOGGER.warn("Invalid bowgun ammo json for {}", id);
                return;
            }
            JsonObject root = element.getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("ammo");
            if (arr == null) {
                MHWeaponsMod.LOGGER.warn("Missing 'ammo' array in {}", id);
                return;
            }
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject obj = el.getAsJsonObject();
                String ammoId = obj.get("id").getAsString();
                float dmg = obj.get("damage").getAsFloat();
                float elem = obj.get("element_damage").getAsFloat();
                float status = obj.get("status_value").getAsFloat();
                float speed = obj.get("speed").getAsFloat();
                float gravity = obj.get("gravity").getAsFloat();
                int pierce = obj.get("pierce").getAsInt();
                int pellets = obj.get("pellets").getAsInt();
                float spread = obj.get("spread").getAsFloat();
                int recoil = obj.get("recoil").getAsInt();
                String reload = obj.get("reload").getAsString();
                int magCap = obj.get("magazine" ).getAsInt();
                int maxStack = obj.get("max_stack").getAsInt();
                int rapidBurst = obj.get("rapid_burst").getAsInt();
                float rapidMult = obj.get("rapid_mult").getAsFloat();

                BowgunMagazineManager.AmmoData data = new BowgunMagazineManager.AmmoData(
                        ammoId, dmg, elem, status, speed, gravity, pierce, pellets,
                        spread, recoil, reload, magCap, maxStack, rapidBurst, rapidMult);
                ammoData.put(ammoId, data);

                JsonObject range = obj.getAsJsonObject("range_decay");
                if (range != null) {
                    float min = range.get("min").getAsFloat();
                    float optimal = range.get("optimal").getAsFloat();
                    float max = range.get("max").getAsFloat();
                    ranges.put(ammoId, new BowgunMagazineManager.RangeProfile(min, optimal, max));
                }
            }
        });

        if (!ammoData.isEmpty()) {
            BowgunMagazineManager.loadFromJson(ammoData, ranges);
            MHWeaponsMod.LOGGER.info("Loaded {} bowgun ammo entries", ammoData.size());
        }
    }
}