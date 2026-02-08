package org.example.common.data;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.example.MHWeaponsMod;
import org.example.item.WeaponIdProvider;

public final class WeaponDataResolver {
    private WeaponDataResolver() {
    }

    @SuppressWarnings("null")
    public static WeaponData resolve(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof WeaponIdProvider weaponIdProvider) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MHWeaponsMod.MODID, weaponIdProvider.getWeaponId());
            return WeaponDataManager.INSTANCE.get(id);
        }
        return null;
    }

    public static float resolveMotionValue(Player player, String actionKey, float fallback) {
        WeaponData data = resolve(player);
        if (data == null) {
            return fallback;
        }
        if (data.getJson().has("motionValues") && data.getJson().get("motionValues").isJsonObject()) {
            var obj = data.getJson().getAsJsonObject("motionValues");
            if (obj.has(actionKey)) {
                return obj.get(actionKey).getAsFloat();
            }
        }
        return fallback;
    }

    public static int resolveInt(Player player, String objectKey, String valueKey, int fallback) {
        WeaponData data = resolve(player);
        if (data == null) {
            return fallback;
        }
        JsonObject json = data.getJson();
        JsonObject target = json;
        if (objectKey != null) {
            if (!json.has(objectKey) || !json.get(objectKey).isJsonObject()) {
                return fallback;
            }
            target = json.getAsJsonObject(objectKey);
        }
        if (target.has(valueKey) && target.get(valueKey).isJsonPrimitive()) {
            return target.get(valueKey).getAsInt();
        }
        return fallback;
    }

    public static float resolveFloat(Player player, String objectKey, String valueKey, float fallback) {
        WeaponData data = resolve(player);
        if (data == null) {
            return fallback;
        }
        JsonObject json = data.getJson();
        JsonObject target = json;
        if (objectKey != null) {
            if (!json.has(objectKey) || !json.get(objectKey).isJsonObject()) {
                return fallback;
            }
            target = json.getAsJsonObject(objectKey);
        }
        if (target.has(valueKey) && target.get(valueKey).isJsonPrimitive()) {
            return target.get(valueKey).getAsFloat();
        }
        return fallback;
    }

    public static String resolveString(Player player, String objectKey, String valueKey, String fallback) {
        WeaponData data = resolve(player);
        if (data == null) {
            return fallback;
        }
        JsonObject json = data.getJson();
        JsonObject target = json;
        if (objectKey != null) {
            if (!json.has(objectKey) || !json.get(objectKey).isJsonObject()) {
                return fallback;
            }
            target = json.getAsJsonObject(objectKey);
        }
        if (target.has(valueKey) && target.get(valueKey).isJsonPrimitive()) {
            return target.get(valueKey).getAsString();
        }
        return fallback;
    }
}
