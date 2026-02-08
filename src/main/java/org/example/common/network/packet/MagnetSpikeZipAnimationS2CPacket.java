package org.example.common.network.packet;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.client.network.ClientPacketHandler;

public final class MagnetSpikeZipAnimationS2CPacket {
    private final int playerId;

    public MagnetSpikeZipAnimationS2CPacket(int playerId) {
        this.playerId = playerId;
    }

    public int playerId() {
        return playerId;
    }

    public static void encode(MagnetSpikeZipAnimationS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.playerId);
    }

    public static MagnetSpikeZipAnimationS2CPacket decode(FriendlyByteBuf buffer) {
        return new MagnetSpikeZipAnimationS2CPacket(buffer.readInt());
    }

    public static void handle(MagnetSpikeZipAnimationS2CPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleMagnetZipAnimation(packet))
        );
        context.get().setPacketHandled(true);
    }
}