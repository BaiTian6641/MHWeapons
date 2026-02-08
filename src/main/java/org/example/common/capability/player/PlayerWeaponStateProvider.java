package org.example.common.capability.player;

import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.example.common.capability.CommonCapabilities;

public final class PlayerWeaponStateProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    private final PlayerWeaponState state = new PlayerWeaponState();
    private final LazyOptional<PlayerWeaponState> optional = LazyOptional.of(() -> state);

    @Override
    @SuppressWarnings("null")
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == CommonCapabilities.PLAYER_WEAPON) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return state.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        state.deserializeNBT(nbt);
    }
}
