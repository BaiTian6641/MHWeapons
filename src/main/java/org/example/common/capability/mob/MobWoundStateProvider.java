package org.example.common.capability.mob;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.example.common.capability.CommonCapabilities;
import javax.annotation.Nullable;

public final class MobWoundStateProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    private final MobWoundState state = new MobWoundState();
    private final LazyOptional<MobWoundState> optional = LazyOptional.of(() -> state);

    @Override
    @SuppressWarnings("null")
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == CommonCapabilities.MOB_WOUND) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("woundValue", state.getWoundValue());
        tag.putBoolean("wounded", state.isWounded());
        tag.putInt("woundTicks", state.getWoundTicksRemaining());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        state.setWoundValue(nbt.getFloat("woundValue"));
        state.setWounded(nbt.getBoolean("wounded"));
        state.setWoundTicksRemaining(nbt.getInt("woundTicks"));
    }
}
