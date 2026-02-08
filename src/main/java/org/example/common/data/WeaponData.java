package org.example.common.data;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public final class WeaponData {
    private final ResourceLocation id;
    private final JsonObject json;

    public WeaponData(ResourceLocation id, JsonObject json) {
        this.id = id;
        this.json = json;
    }

    public ResourceLocation getId() {
        return id;
    }

    public JsonObject getJson() {
        return json;
    }

    public float getFloat(String key, float fallback) {
        if (json.has(key) && json.get(key).isJsonPrimitive()) {
            return json.get(key).getAsFloat();
        }
        return fallback;
    }
}
