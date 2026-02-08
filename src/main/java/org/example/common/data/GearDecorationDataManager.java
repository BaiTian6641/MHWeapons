package org.example.common.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.example.MHWeaponsMod;

public final class GearDecorationDataManager extends SimpleJsonResourceReloadListener {
    public static final GearDecorationDataManager INSTANCE = new GearDecorationDataManager();

    private final Map<ResourceLocation, int[]> slots = new HashMap<>();

    private GearDecorationDataManager() {
        super(new Gson(), "gear_decorations");
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> objects, @Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        slots.clear();
        objects.forEach((id, element) -> {
            if (!element.isJsonObject()) {
                MHWeaponsMod.LOGGER.warn("Invalid gear decoration json for {}", id);
                return;
            }
            JsonObject json = element.getAsJsonObject();
            if (!json.has("slots") || !json.get("slots").isJsonArray()) {
                return;
            }
            var arr = json.getAsJsonArray("slots");
            int[] values = new int[arr.size()];
            int count = 0;
            for (int i = 0; i < arr.size(); i++) {
                if (arr.get(i).isJsonPrimitive()) {
                    int v = arr.get(i).getAsInt();
                    if (v > 0) {
                        values[count++] = v;
                    }
                }
            }
            if (count > 0) {
                slots.put(id, java.util.Arrays.copyOf(values, count));
            }
        });
        MHWeaponsMod.LOGGER.info("Loaded {} gear decoration defaults", slots.size());
    }

    public Map<ResourceLocation, int[]> getAll() {
        return Collections.unmodifiableMap(slots);
    }

    public int[] getSlots(ResourceLocation id) {
        return slots.getOrDefault(id, new int[0]);
    }
}