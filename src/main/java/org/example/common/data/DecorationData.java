package org.example.common.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.List;
import java.util.UUID;

public final class DecorationData {
    private final ResourceLocation id;
    private final int size;
    private final int tier;
    private final double tierMultiplier;
    private final String rarity;
    private final String category;
    private final List<DecorationModifier> modifiers;
    private final List<String> tags;

    public DecorationData(ResourceLocation id, int size, int tier, double tierMultiplier, String rarity, String category, List<DecorationModifier> modifiers, List<String> tags) {
        this.id = id;
        this.size = size;
        this.tier = tier;
        this.tierMultiplier = tierMultiplier;
        this.rarity = rarity;
        this.category = category;
        this.modifiers = modifiers;
        this.tags = tags;
    }

    public ResourceLocation getId() {
        return id;
    }

    public int getSize() {
        return size;
    }

    public int getTier() {
        return tier;
    }

    public double getTierMultiplier() {
        return tierMultiplier;
    }

    public String getRarity() {
        return rarity;
    }

    public String getCategory() {
        return category;
    }

    public List<DecorationModifier> getModifiers() {
        return modifiers;
    }

    public List<String> getTags() {
        return tags;
    }

    @SuppressWarnings("null")
    public Multimap<Attribute, AttributeModifier> buildModifiers(UUID baseUuid) {
        HashMultimap<Attribute, AttributeModifier> map = HashMultimap.create();
        if (modifiers == null || modifiers.isEmpty()) {
            return map;
        }
        for (DecorationModifier mod : modifiers) {
            UUID derived = UUID.nameUUIDFromBytes((baseUuid + ":" + mod.name()).getBytes());
            map.put(mod.attribute(), new AttributeModifier(derived, mod.name(), mod.amount() * tierMultiplier, mod.operation()));
        }
        return map;
    }

    public record DecorationModifier(Attribute attribute, String name, double amount, AttributeModifier.Operation operation) {
    }
}