package org.example.common.network.packet;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.util.CapabilityUtil;

public final class FocusModeC2SPacket {
    private final boolean enabled;

    public FocusModeC2SPacket(boolean enabled) {
        this.enabled = enabled;
    }

    public static void encode(FocusModeC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.enabled);
    }

    public static FocusModeC2SPacket decode(FriendlyByteBuf buffer) {
        return new FocusModeC2SPacket(buffer.readBoolean());
    }

    public static void handle(FocusModeC2SPacket packet, Supplier<Context> contextSupplier) {
        Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() == null) {
                return;
            }
            PlayerCombatState state = CapabilityUtil.getPlayerCombatState(context.getSender());
            if (state != null) {
                state.setFocusMode(packet.enabled);
            }
        });
        context.setPacketHandled(true);
    }
}
