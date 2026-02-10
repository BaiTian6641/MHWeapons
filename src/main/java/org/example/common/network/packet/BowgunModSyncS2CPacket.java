package org.example.common.network.packet;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.example.item.BowgunItem;

/**
 * S2C packet: sync bowgun mod list & magazine state to client for HUD rendering.
 */
public final class BowgunModSyncS2CPacket {
    private final int entityId;
    private final CompoundTag bowgunData;

    public BowgunModSyncS2CPacket(int entityId, CompoundTag bowgunData) {
        this.entityId = entityId;
        this.bowgunData = bowgunData;
    }

    @SuppressWarnings("null")
    public static void encode(BowgunModSyncS2CPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.entityId);
        buf.writeNbt(pkt.bowgunData);
    }

    public static BowgunModSyncS2CPacket decode(FriendlyByteBuf buf) {
        return new BowgunModSyncS2CPacket(buf.readInt(), buf.readNbt());
    }

    public static void handle(BowgunModSyncS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            Entity entity = mc.level.getEntity(pkt.entityId);
            if (entity instanceof Player player) {
                ItemStack stack = player.getMainHandItem();
                if (stack.getItem() instanceof BowgunItem && pkt.bowgunData != null) {
                    CompoundTag stackTag = stack.getOrCreateTag();
                    stackTag.put("BowgunSync", pkt.bowgunData);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
