package org.example.common.network.packet;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public final class KinsectRecallC2SPacket {
    public KinsectRecallC2SPacket() {
    }

    public static void encode(KinsectRecallC2SPacket packet, FriendlyByteBuf buffer) {
    }

    public static KinsectRecallC2SPacket decode(FriendlyByteBuf buffer) {
        return new KinsectRecallC2SPacket();
    }

    public static void handle(KinsectRecallC2SPacket packet, Supplier<NetworkEvent.Context> context) {
        // Deprecated: Recall is handled via WeaponActionC2SPacket to avoid duplicate logic paths.
        context.get().setPacketHandled(true);
    }
}
