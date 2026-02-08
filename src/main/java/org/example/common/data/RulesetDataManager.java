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

public final class RulesetDataManager extends SimpleJsonResourceReloadListener {
    public static final RulesetDataManager INSTANCE = new RulesetDataManager();

    private final Map<ResourceLocation, JsonObject> data = new HashMap<>();

    private RulesetDataManager() {
        super(new Gson(), "rulesets");
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> objects, @Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        data.clear();
        objects.forEach((id, element) -> {
            if (element.isJsonObject()) {
                data.put(id, element.getAsJsonObject());
            } else {
                MHWeaponsMod.LOGGER.warn("Invalid ruleset json for {}", id);
            }
        });
        MHWeaponsMod.LOGGER.info("Loaded {} rulesets", data.size());
    }

    public Map<ResourceLocation, JsonObject> getAll() {
        return Collections.unmodifiableMap(data);
    }

    public JsonObject get(ResourceLocation id) {
        return data.get(id);
    }
}
