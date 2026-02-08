package org.example.common.events;

import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.example.common.data.DecorationData;
import org.example.common.data.DecorationDataManager;
import org.example.common.util.DecorationUtil;

public final class DecorationAttributeEvents {
    @SubscribeEvent
    @SuppressWarnings("null")
    public void onItemAttributeModifiers(ItemAttributeModifierEvent event) {
        var stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        var decos = DecorationUtil.getDecorations(stack);
        if (decos.isEmpty()) {
            return;
        }
        for (var deco : decos) {
            DecorationData data = DecorationDataManager.INSTANCE.get(deco.id());
            if (data != null) {
                java.util.UUID baseUuid = java.util.UUID.nameUUIDFromBytes(stack.getDescriptionId().getBytes());
                var modifiers = data.buildModifiers(baseUuid);
                for (var entry : modifiers.entries()) {
                    event.addModifier(entry.getKey(), entry.getValue());
                }
            }
        }
    }
}