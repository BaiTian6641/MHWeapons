package org.example.common.network.packet;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.example.common.combat.weapon.WeaponActionHandler;

public final class KinsectLaunchC2SPacket {
    private final int targetEntityId;
    private final Vec3 targetPos;

    public KinsectLaunchC2SPacket(int targetEntityId, Vec3 targetPos) {
        this.targetEntityId = targetEntityId;
        this.targetPos = targetPos;
    }

    public int targetEntityId() {
        return targetEntityId;
    }

    public Vec3 targetPos() {
        return targetPos;
    }

    public static void encode(KinsectLaunchC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.targetEntityId);
        buffer.writeBoolean(packet.targetPos != null);
        if (packet.targetPos != null) {
            buffer.writeDouble(packet.targetPos.x);
            buffer.writeDouble(packet.targetPos.y);
            buffer.writeDouble(packet.targetPos.z);
        }
    }

    public static KinsectLaunchC2SPacket decode(FriendlyByteBuf buffer) {
        int id = buffer.readInt();
        Vec3 pos = null;
        if (buffer.readBoolean()) {
            pos = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        }
        return new KinsectLaunchC2SPacket(id, pos);
    }

    public static void handle(KinsectLaunchC2SPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            var sender = context.get().getSender();
            if (sender != null) {
                WeaponActionHandler.handleKinsectLaunch(sender, packet.targetEntityId(), packet.targetPos());
            }
        });
        context.get().setPacketHandled(true);
    }
}
