package org.example.common.data.compat;

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

public final class BetterCombatAnimationMapManager extends SimpleJsonResourceReloadListener {
    public static final BetterCombatAnimationMapManager INSTANCE = new BetterCombatAnimationMapManager();

    private final Map<String, String> animationMap = new HashMap<>();

    private BetterCombatAnimationMapManager() {
        super(new Gson(), "compat/bettercombat");
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> objects, @Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        animationMap.clear();
        objects.forEach((id, element) -> {
            if (element.isJsonObject()) {
                JsonObject json = element.getAsJsonObject();
                if (json.has("animations") && json.get("animations").isJsonObject()) {
                    JsonObject animations = json.getAsJsonObject("animations");
                    animations.entrySet().forEach(entry -> animationMap.put(entry.getKey(), entry.getValue().getAsString()));
                }
            } else {
                MHWeaponsMod.LOGGER.warn("Invalid Better Combat animation map json for {}", id);
            }
        });
        MHWeaponsMod.LOGGER.info("Loaded {} Better Combat animation mappings", animationMap.size());
    }

    public Map<String, String> getAll() {
        return Collections.unmodifiableMap(animationMap);
    }

    public String resolveActionKey(String animationId) {
        return animationMap.get(animationId);
    }
}
