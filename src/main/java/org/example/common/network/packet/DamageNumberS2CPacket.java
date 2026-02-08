package org.example.common.network.packet;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.client.network.ClientPacketHandler;

public final class DamageNumberS2CPacket {
    public static final int COLOR_WHITE = 0;
    public static final int COLOR_YELLOW = 1;
    public static final int FRAME_NONE = 0;
    public static final int FRAME_AFFINITY = 1;
    public static final int FRAME_NEGATIVE_AFFINITY = 2;

    private final int targetId;
    private final float damage;
    private final int colorType;
    private final int frameType;

    public DamageNumberS2CPacket(int targetId, float damage, int colorType, int frameType) {
        this.targetId = targetId;
        this.damage = damage;
        this.colorType = colorType;
        this.frameType = frameType;
    }

    public int targetId() {
        return targetId;
    }

    public float damage() {
        return damage;
    }

    public int colorType() {
        return colorType;
    }

    public int frameType() {
        return frameType;
    }

    @SuppressWarnings("null")
    public static void encode(DamageNumberS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.targetId);
        buffer.writeFloat(packet.damage);
        buffer.writeInt(packet.colorType);
        buffer.writeInt(packet.frameType);
    }

    public static DamageNumberS2CPacket decode(FriendlyByteBuf buffer) {
        int targetId = buffer.readInt();
        float damage = buffer.readFloat();
        int colorType = buffer.readInt();
        int frameType = buffer.readInt();
        return new DamageNumberS2CPacket(targetId, damage, colorType, frameType);
    }

    public static void handle(DamageNumberS2CPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleDamageNumber(packet))
        );
        context.get().setPacketHandled(true);
    }
}