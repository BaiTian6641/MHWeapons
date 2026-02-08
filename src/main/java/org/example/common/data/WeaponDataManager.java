package org.example.common.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.example.MHWeaponsMod;

public final class WeaponDataManager extends SimpleJsonResourceReloadListener {
    public static final WeaponDataManager INSTANCE = new WeaponDataManager();

    private final Map<ResourceLocation, WeaponData> data = new HashMap<>();

    private WeaponDataManager() {
        super(new Gson(), "weapons");
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> objects, @Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        data.clear();
        objects.forEach((id, element) -> {
            if (element.isJsonObject()) {
                data.put(id, new WeaponData(id, element.getAsJsonObject()));
            } else {
                MHWeaponsMod.LOGGER.warn("Invalid weapon data json for {}", id);
            }
        });
        MHWeaponsMod.LOGGER.info("Loaded {} weapon data files", data.size());
    }

    public Map<ResourceLocation, WeaponData> getAll() {
        return Collections.unmodifiableMap(data);
    }

    public WeaponData get(ResourceLocation id) {
        return data.get(id);
    }
}
