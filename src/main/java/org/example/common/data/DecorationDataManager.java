package org.example.common.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.example.MHWeaponsMod;

public final class DecorationDataManager extends SimpleJsonResourceReloadListener {
    public static final DecorationDataManager INSTANCE = new DecorationDataManager();

    private final Map<ResourceLocation, DecorationData> data = new HashMap<>();

    private DecorationDataManager() {
        super(new Gson(), "decorations");
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> objects, @Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        data.clear();
        objects.forEach((id, element) -> {
            if (element.isJsonObject()) {
                JsonObject json = element.getAsJsonObject();
                if (json.has("weapon") || json.has("armor")) {
                    parseBulkDecorations(json);
                    return;
                }
                parseAndStoreDecoration(id, json);
                return;
            }
            if (element.isJsonArray()) {
                element.getAsJsonArray().forEach(entry -> {
                    if (!entry.isJsonObject()) {
                        return;
                    }
                    JsonObject json = entry.getAsJsonObject();
                    if (!json.has("id")) {
                        MHWeaponsMod.LOGGER.warn("Decoration entry missing id in {}", id);
                        return;
                    }
                    ResourceLocation entryId = ResourceLocation.tryParse(json.get("id").getAsString());
                    if (entryId == null) {
                        MHWeaponsMod.LOGGER.warn("Invalid decoration id '{}' in {}", json.get("id").getAsString(), id);
                        return;
                    }
                    parseAndStoreDecoration(entryId, json);
                });
                return;
            }
            MHWeaponsMod.LOGGER.warn("Invalid decoration json for {}", id);
        });
        MHWeaponsMod.LOGGER.info("Loaded {} decoration data files", data.size());
    }

    private void parseAndStoreDecoration(ResourceLocation id, JsonObject json) {
        int size = json.has("size") ? json.get("size").getAsInt() : 1;
        if (size < 1) {
            size = 1;
        }
        int tier = json.has("tier") ? json.get("tier").getAsInt() : size;
        if (tier < 1) {
            tier = 1;
        }
        double tierMultiplier = json.has("tier_multiplier") ? json.get("tier_multiplier").getAsDouble() : 1.0D;
        String rarity = json.has("rarity") ? json.get("rarity").getAsString() : "common";
        String category = json.has("category") ? json.get("category").getAsString() : "armor";
        List<DecorationData.DecorationModifier> modifiers = parseModifiers(json, id);
        List<String> tags = parseTags(json);
        data.put(id, new DecorationData(id, size, tier, tierMultiplier, rarity, category, modifiers, tags));
    }

    private void parseBulkDecorations(JsonObject json) {
        JsonObject overrides = json.has("overrides") && json.get("overrides").isJsonObject()
            ? json.getAsJsonObject("overrides") : new JsonObject();
        parseBulkCategory(json, overrides, "weapon");
        parseBulkCategory(json, overrides, "armor");
    }

    private void parseBulkCategory(JsonObject json, JsonObject overrides, String category) {
        if (!json.has(category) || !json.get(category).isJsonArray()) {
            return;
        }
        json.getAsJsonArray(category).forEach(entry -> {
            if (!entry.isJsonPrimitive()) {
                return;
            }
            String rawId = entry.getAsString();
            ResourceLocation decoId = ResourceLocation.tryParse(rawId);
            if (decoId == null) {
                decoId = ResourceLocation.fromNamespaceAndPath(MHWeaponsMod.MODID, rawId);
            }
            JsonObject base = new JsonObject();
            base.addProperty("size", 1);
            base.addProperty("tier", 1);
            base.addProperty("tier_multiplier", 1.0D);
            base.addProperty("rarity", "common");
            base.addProperty("category", category);
            base.add("attributes", new com.google.gson.JsonArray());
            com.google.gson.JsonArray tags = new com.google.gson.JsonArray();
            tags.add("wilds");
            tags.add(category);
            base.add("tags", tags);
            if (overrides.has(decoId.getPath()) && overrides.get(decoId.getPath()).isJsonObject()) {
                JsonObject override = overrides.getAsJsonObject(decoId.getPath());
                override.entrySet().forEach(e -> base.add(e.getKey(), e.getValue()));
            }
            parseAndStoreDecoration(decoId, base);
        });
    }

    public Map<ResourceLocation, DecorationData> getAll() {
        return Collections.unmodifiableMap(data);
    }

    public DecorationData get(ResourceLocation id) {
        return data.get(id);
    }

    @SuppressWarnings("null")
    private List<DecorationData.DecorationModifier> parseModifiers(JsonObject json, ResourceLocation id) {
        List<DecorationData.DecorationModifier> list = new ArrayList<>();
        if (!json.has("attributes") || !json.get("attributes").isJsonArray()) {
            return list;
        }
        for (JsonElement el : json.getAsJsonArray("attributes")) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject obj = el.getAsJsonObject();
            if (!obj.has("attribute") || !obj.get("attribute").isJsonPrimitive()) {
                continue;
            }
            String attrId = obj.get("attribute").getAsString();
            ResourceLocation attrKey = ResourceLocation.tryParse(attrId);
            if (attrKey == null) {
                MHWeaponsMod.LOGGER.warn("Invalid attribute id '{}' in {}", attrId, id);
                continue;
            }
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attrKey);
            if (attribute == null) {
                MHWeaponsMod.LOGGER.warn("Unknown attribute '{}' in {}", attrId, id);
                continue;
            }
            double amount = obj.has("amount") ? obj.get("amount").getAsDouble() : 0.0D;
            String op = obj.has("operation") ? obj.get("operation").getAsString() : "add";
            AttributeModifier.Operation operation = parseOperation(op, id, attrId);
            String name = obj.has("name") ? obj.get("name").getAsString() : (id.getPath() + "." + attrKey.getPath());
            list.add(new DecorationData.DecorationModifier(attribute, name, amount, operation));
        }
        return list;
    }

    private AttributeModifier.Operation parseOperation(String op, ResourceLocation id, String attrId) {
        return switch (op.toLowerCase()) {
            case "add", "addition" -> AttributeModifier.Operation.ADDITION;
            case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> {
                MHWeaponsMod.LOGGER.warn("Unknown operation '{}' for {} in {}. Using ADDITION.", op, attrId, id);
                yield AttributeModifier.Operation.ADDITION;
            }
        };
    }

    private List<String> parseTags(JsonObject json) {
        List<String> tags = new ArrayList<>();
        if (!json.has("tags") || !json.get("tags").isJsonArray()) {
            return tags;
        }
        for (JsonElement el : json.getAsJsonArray("tags")) {
            if (el.isJsonPrimitive()) {
                tags.add(el.getAsString());
            }
        }
        return tags;
    }
}