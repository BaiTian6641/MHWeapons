package org.example.common.network.packet;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.combat.bowgun.BowgunHandler;
import org.example.common.util.CapabilityUtil;

/**
 * C2S packet: client requests ammo type switch.
 * Supports direct set or cycle (forward/backward).
 */
public final class AmmoSwitchC2SPacket {
    private final String ammoId;   // non-empty = direct switch, empty = cycle
    private final boolean forward; // used when ammoId is empty

    public AmmoSwitchC2SPacket(String ammoId, boolean forward) {
        this.ammoId = ammoId;
        this.forward = forward;
    }

    /** Cycle constructor */
    public AmmoSwitchC2SPacket(boolean forward) {
        this("", forward);
    }

    /** Direct switch constructor */
    public AmmoSwitchC2SPacket(String ammoId) {
        this(ammoId, true);
    }

    @SuppressWarnings("null")
    public static void encode(AmmoSwitchC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.ammoId);
        buf.writeBoolean(pkt.forward);
    }

    public static AmmoSwitchC2SPacket decode(FriendlyByteBuf buf) {
        return new AmmoSwitchC2SPacket(buf.readUtf(), buf.readBoolean());
    }

    public static void handle(AmmoSwitchC2SPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var sender = ctx.get().getSender();
            if (sender == null) return;
            PlayerWeaponState state = CapabilityUtil.getPlayerWeaponState(sender);
            if (state == null) return;

            if (pkt.ammoId.isEmpty()) {
                BowgunHandler.cycleAmmo(sender, state, pkt.forward);
            } else {
                BowgunHandler.switchAmmo(sender, state, pkt.ammoId);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
