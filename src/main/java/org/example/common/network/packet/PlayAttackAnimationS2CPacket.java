package org.example.common.network.packet;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.client.network.ClientPacketHandler;

public final class PlayAttackAnimationS2CPacket {
    private final int playerId;
    private final String animationId;
    private final float length;
    private final float upswing;
    private final float speed;
    private final String actionKey;
    private final int actionKeyTicks;

    public PlayAttackAnimationS2CPacket(int playerId, String animationId, float length, float upswing,
                                        float speed, String actionKey, int actionKeyTicks) {
        this.playerId = playerId;
        this.animationId = animationId;
        this.length = length;
        this.upswing = upswing;
        this.speed = speed;
        this.actionKey = actionKey;
        this.actionKeyTicks = actionKeyTicks;
    }

    public int playerId() {
        return playerId;
    }

    public String animationId() {
        return animationId;
    }

    public float length() {
        return length;
    }

    public float upswing() {
        return upswing;
    }

    public float speed() {
        return speed;
    }

    public String actionKey() {
        return actionKey;
    }

    public int actionKeyTicks() {
        return actionKeyTicks;
    }

    @SuppressWarnings("null")
    public static void encode(PlayAttackAnimationS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.playerId);
        buffer.writeUtf(packet.animationId);
        buffer.writeFloat(packet.length);
        buffer.writeFloat(packet.upswing);
        buffer.writeFloat(packet.speed);
        boolean hasActionKey = packet.actionKey != null && !packet.actionKey.isBlank();
        buffer.writeBoolean(hasActionKey);
        if (hasActionKey) {
            buffer.writeUtf(packet.actionKey);
            buffer.writeInt(packet.actionKeyTicks);
        }
    }

    public static PlayAttackAnimationS2CPacket decode(FriendlyByteBuf buffer) {
        int playerId = buffer.readInt();
        String animationId = buffer.readUtf();
        float length = buffer.readFloat();
        float upswing = buffer.readFloat();
        float speed = buffer.readFloat();
        String actionKey = null;
        int actionKeyTicks = 0;
        if (buffer.readBoolean()) {
            actionKey = buffer.readUtf();
            actionKeyTicks = buffer.readInt();
        }
        return new PlayAttackAnimationS2CPacket(playerId, animationId, length, upswing, speed, actionKey, actionKeyTicks);
    }

    public static void handle(PlayAttackAnimationS2CPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handlePlayAttackAnimation(packet))
        );
        context.get().setPacketHandled(true);
    }
}
