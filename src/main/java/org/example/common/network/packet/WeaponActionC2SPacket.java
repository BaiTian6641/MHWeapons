package org.example.common.network.packet;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.common.combat.weapon.WeaponActionHandler;
import org.example.common.combat.weapon.WeaponActionType;

public final class WeaponActionC2SPacket {
    private final WeaponActionType action;
    private final boolean pressed;
    private final float inputX;
    private final float inputZ;

    public WeaponActionC2SPacket(WeaponActionType action, boolean pressed, float inputX, float inputZ) {
        this.action = action;
        this.pressed = pressed;
        this.inputX = inputX;
        this.inputZ = inputZ;
    }
    
    // Legacy constructor for convenience (defaults to 0 input)
    public WeaponActionC2SPacket(WeaponActionType action, boolean pressed) {
        this(action, pressed, 0.0f, 0.0f);
    }

    public WeaponActionType action() {
        return action;
    }

    public boolean pressed() {
        return pressed;
    }
    
    public float inputX() {
        return inputX;
    }

    public float inputZ() {
        return inputZ;
    }

    @SuppressWarnings("null")
    public static void encode(WeaponActionC2SPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
        buffer.writeBoolean(packet.pressed);
        buffer.writeFloat(packet.inputX);
        buffer.writeFloat(packet.inputZ);
    }

    public static WeaponActionC2SPacket decode(FriendlyByteBuf buffer) {
        return new WeaponActionC2SPacket(buffer.readEnum(WeaponActionType.class), buffer.readBoolean(), buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(WeaponActionC2SPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            var sender = context.get().getSender();
            if (sender != null) {
                WeaponActionHandler.handleAction(sender, packet.action(), packet.pressed(), packet.inputX(), packet.inputZ());
            }
        });
        context.get().setPacketHandled(true);
    }
}
