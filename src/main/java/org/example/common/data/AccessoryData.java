package org.example.common.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.List;
import java.util.UUID;

public final class AccessoryData {
    private final ResourceLocation id;
    private final List<AccessoryModifier> modifiers;
    private final List<String> allowedSlots;
    private final boolean unique;
    private final String uniqueGroup;
    private final int maxEquipped;
    private final int maxGroup;

    public AccessoryData(ResourceLocation id, List<AccessoryModifier> modifiers, List<String> allowedSlots, boolean unique, String uniqueGroup,
                         int maxEquipped, int maxGroup) {
        this.id = id;
        this.modifiers = modifiers;
        this.allowedSlots = allowedSlots;
        this.unique = unique;
        this.uniqueGroup = uniqueGroup;
        this.maxEquipped = maxEquipped;
        this.maxGroup = maxGroup;
    }

    public ResourceLocation getId() {
        return id;
    }

    public List<AccessoryModifier> getModifiers() {
        return modifiers;
    }

    public List<String> getAllowedSlots() {
        return allowedSlots;
    }

    public boolean isUnique() {
        return unique;
    }

    public String getUniqueGroup() {
        return uniqueGroup;
    }

    public int getMaxEquipped() {
        return maxEquipped;
    }

    public int getMaxGroup() {
        return maxGroup;
    }

    public Multimap<Attribute, AttributeModifier> buildModifiers(UUID baseUuid) {
        HashMultimap<Attribute, AttributeModifier> map = HashMultimap.create();
        if (modifiers == null || modifiers.isEmpty()) {
            return map;
        }
        for (AccessoryModifier mod : modifiers) {
            UUID derived = UUID.nameUUIDFromBytes((baseUuid + ":" + mod.name()).getBytes());
            map.put(mod.attribute(), new AttributeModifier(derived, mod.name(), mod.amount(), mod.operation()));
        }
        return map;
    }

    public record AccessoryModifier(Attribute attribute, String name, double amount, AttributeModifier.Operation operation) {
    }
}