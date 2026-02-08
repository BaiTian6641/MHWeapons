package org.example.common.network.packet;

import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.client.network.ClientPacketHandler;

public final class PlayerWeaponStateS2CPacket {
    private final int playerId;
    private final CompoundTag data;

    public PlayerWeaponStateS2CPacket(int playerId, CompoundTag data) {
        this.playerId = playerId;
        this.data = data;
    }

    public int playerId() {
        return playerId;
    }

    public CompoundTag data() {
        return data;
    }

    public static void encode(PlayerWeaponStateS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.playerId);
        buffer.writeNbt(packet.data);
    }

    public static PlayerWeaponStateS2CPacket decode(FriendlyByteBuf buffer) {
        return new PlayerWeaponStateS2CPacket(buffer.readInt(), buffer.readNbt());
    }

    public static void handle(PlayerWeaponStateS2CPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleWeaponState(packet));
        });
        context.get().setPacketHandled(true);
    }
}
