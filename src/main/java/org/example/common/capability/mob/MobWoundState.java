package org.example.common.capability.mob;

public final class MobWoundState {
    private float woundValue;
    private boolean wounded;
    private int woundTicksRemaining;

    public float getWoundValue() {
        return woundValue;
    }

    public void setWoundValue(float woundValue) {
        this.woundValue = woundValue;
    }

    public boolean isWounded() {
        return wounded;
    }

    public void setWounded(boolean wounded) {
        this.wounded = wounded;
    }

    public int getWoundTicksRemaining() {
        return woundTicksRemaining;
    }

    public void setWoundTicksRemaining(int woundTicksRemaining) {
        this.woundTicksRemaining = woundTicksRemaining;
    }
}
