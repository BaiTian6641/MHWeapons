package org.example.common.capability.player;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.example.common.capability.CommonCapabilities;
import javax.annotation.Nullable;

public final class PlayerCombatStateProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    private final PlayerCombatState state = new PlayerCombatState();
    private final LazyOptional<PlayerCombatState> optional = LazyOptional.of(() -> state);

    @Override
    @SuppressWarnings("null")
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == CommonCapabilities.PLAYER_COMBAT) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("dodgeIFrames", state.getDodgeIFrameTicks());
        tag.putBoolean("guardPoint", state.isGuardPointActive());
        tag.putBoolean("focusMode", state.isFocusMode());
        String actionKey = state.getActionKey();
        if (actionKey != null) {
            tag.putString("actionKey", actionKey);
        }
        tag.putInt("actionKeyTicks", state.getActionKeyTicks());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        state.setDodgeIFrameTicks(nbt.getInt("dodgeIFrames"));
        state.setGuardPointActive(nbt.getBoolean("guardPoint"));
        state.setFocusMode(nbt.getBoolean("focusMode"));
        state.setActionKey(nbt.contains("actionKey") ? nbt.getString("actionKey") : null);
        state.setActionKeyTicks(nbt.getInt("actionKeyTicks"));
    }
}
