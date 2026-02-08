package org.example.common.capability.mob;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.example.common.capability.CommonCapabilities;

import javax.annotation.Nullable;

public final class MobStatusStateProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    private final MobStatusState state = new MobStatusState();
    private final LazyOptional<MobStatusState> optional = LazyOptional.of(() -> state);

    @Override
    @SuppressWarnings("null")
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == CommonCapabilities.MOB_STATUS) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("poisonBuildup", state.getPoisonBuildup());
        tag.putFloat("paralysisBuildup", state.getParalysisBuildup());
        tag.putFloat("sleepBuildup", state.getSleepBuildup());
        tag.putFloat("blastBuildup", state.getBlastBuildup());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        state.setPoisonBuildup(nbt.getFloat("poisonBuildup"));
        state.setParalysisBuildup(nbt.getFloat("paralysisBuildup"));
        state.setSleepBuildup(nbt.getFloat("sleepBuildup"));
        state.setBlastBuildup(nbt.getFloat("blastBuildup"));
    }
}