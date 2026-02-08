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
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.example.MHWeaponsMod;

public final class AccessoryDataManager extends SimpleJsonResourceReloadListener {
    public static final AccessoryDataManager INSTANCE = new AccessoryDataManager();

    private final Map<ResourceLocation, AccessoryData> data = new HashMap<>();

    private AccessoryDataManager() {
        super(new Gson(), "accessories");
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> objects, @Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        data.clear();
        objects.forEach((id, element) -> {
            if (!element.isJsonObject()) {
                MHWeaponsMod.LOGGER.warn("Invalid accessory json for {}", id);
                return;
            }
            JsonObject json = element.getAsJsonObject();
            List<AccessoryData.AccessoryModifier> modifiers = parseModifiers(json, id);
            List<String> allowedSlots = parseAllowedSlots(json);
            boolean unique = json.has("unique") && json.get("unique").getAsBoolean();
            String uniqueGroup = json.has("unique_group") ? json.get("unique_group").getAsString() : "";
            int maxEquipped = json.has("max_equipped") ? json.get("max_equipped").getAsInt() : -1;
            int maxGroup = json.has("max_group") ? json.get("max_group").getAsInt() : -1;
            data.put(id, new AccessoryData(id, modifiers, allowedSlots, unique, uniqueGroup, maxEquipped, maxGroup));
        });
        MHWeaponsMod.LOGGER.info("Loaded {} accessory data files", data.size());
    }

    public Map<ResourceLocation, AccessoryData> getAll() {
        return Collections.unmodifiableMap(data);
    }

    public AccessoryData get(ResourceLocation id) {
        return data.get(id);
    }

    @SuppressWarnings("null")
    private List<AccessoryData.AccessoryModifier> parseModifiers(JsonObject json, ResourceLocation id) {
        List<AccessoryData.AccessoryModifier> list = new ArrayList<>();
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
            list.add(new AccessoryData.AccessoryModifier(attribute, name, amount, operation));
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

    private List<String> parseAllowedSlots(JsonObject json) {
        List<String> slots = new ArrayList<>();
        if (!json.has("allowed_slots") || !json.get("allowed_slots").isJsonArray()) {
            return slots;
        }
        for (JsonElement el : json.getAsJsonArray("allowed_slots")) {
            if (el.isJsonPrimitive()) {
                slots.add(el.getAsString());
            }
        }
        return slots;
    }
}