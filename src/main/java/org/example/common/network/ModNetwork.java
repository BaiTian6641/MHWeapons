package org.example.common.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.example.MHWeaponsMod;
import org.example.common.network.packet.FocusModeC2SPacket;
import org.example.common.network.packet.DamageNumberS2CPacket;
import org.example.common.network.packet.KinsectLaunchC2SPacket;
import org.example.common.network.packet.MagnetSpikeZipAnimationS2CPacket;
import org.example.common.network.packet.PlayAttackAnimationS2CPacket;
import org.example.common.network.packet.PlayerWeaponStateS2CPacket;
import org.example.common.network.packet.WeaponActionC2SPacket;
import org.example.common.network.packet.WoundStateS2CPacket;
import org.example.common.network.packet.AmmoSwitchC2SPacket;
import org.example.common.network.packet.BowgunModSyncS2CPacket;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

        public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(MHWeaponsMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ModNetwork() {
    }

    public static void init() {
        int index = 0;
        CHANNEL.registerMessage(index++, WoundStateS2CPacket.class, WoundStateS2CPacket::encode, WoundStateS2CPacket::decode, WoundStateS2CPacket::handle);
        CHANNEL.registerMessage(index++, FocusModeC2SPacket.class, FocusModeC2SPacket::encode, FocusModeC2SPacket::decode, FocusModeC2SPacket::handle);
        CHANNEL.registerMessage(index++, PlayerWeaponStateS2CPacket.class, PlayerWeaponStateS2CPacket::encode, PlayerWeaponStateS2CPacket::decode, PlayerWeaponStateS2CPacket::handle);
        CHANNEL.registerMessage(index++, MagnetSpikeZipAnimationS2CPacket.class, MagnetSpikeZipAnimationS2CPacket::encode, MagnetSpikeZipAnimationS2CPacket::decode, MagnetSpikeZipAnimationS2CPacket::handle);
        CHANNEL.registerMessage(index++, PlayAttackAnimationS2CPacket.class, PlayAttackAnimationS2CPacket::encode, PlayAttackAnimationS2CPacket::decode, PlayAttackAnimationS2CPacket::handle);
        CHANNEL.registerMessage(index++, DamageNumberS2CPacket.class, DamageNumberS2CPacket::encode, DamageNumberS2CPacket::decode, DamageNumberS2CPacket::handle);
        CHANNEL.registerMessage(index++, WeaponActionC2SPacket.class, WeaponActionC2SPacket::encode, WeaponActionC2SPacket::decode, WeaponActionC2SPacket::handle);
        CHANNEL.registerMessage(index++, KinsectLaunchC2SPacket.class, KinsectLaunchC2SPacket::encode, KinsectLaunchC2SPacket::decode, KinsectLaunchC2SPacket::handle);
        CHANNEL.registerMessage(index++, AmmoSwitchC2SPacket.class, AmmoSwitchC2SPacket::encode, AmmoSwitchC2SPacket::decode, AmmoSwitchC2SPacket::handle);
        CHANNEL.registerMessage(index++, BowgunModSyncS2CPacket.class, BowgunModSyncS2CPacket::encode, BowgunModSyncS2CPacket::decode, BowgunModSyncS2CPacket::handle);
    }
}
