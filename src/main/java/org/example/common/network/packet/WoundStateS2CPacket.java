package org.example.common.network.packet;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.client.network.ClientPacketHandler;

public final class WoundStateS2CPacket {
    private final int entityId;
    private final boolean wounded;
    private final int woundTicks;

    public WoundStateS2CPacket(int entityId, boolean wounded, int woundTicks) {
        this.entityId = entityId;
        this.wounded = wounded;
        this.woundTicks = woundTicks;
    }

    public int entityId() {
        return entityId;
    }

    public boolean wounded() {
        return wounded;
    }

    public int woundTicks() {
        return woundTicks;
    }

    public static void encode(WoundStateS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId);
        buffer.writeBoolean(packet.wounded);
        buffer.writeInt(packet.woundTicks);
    }

    public static WoundStateS2CPacket decode(FriendlyByteBuf buffer) {
        return new WoundStateS2CPacket(buffer.readInt(), buffer.readBoolean(), buffer.readInt());
    }

    public static void handle(WoundStateS2CPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleWoundState(packet));
        });
        context.get().setPacketHandled(true);
    }
}
