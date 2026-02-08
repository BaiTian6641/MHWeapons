package org.example.common.capability.mob;

public final class MobStatusState {
    private float poisonBuildup;
    private float paralysisBuildup;
    private float sleepBuildup;
    private float blastBuildup;

    public float getPoisonBuildup() {
        return poisonBuildup;
    }

    public void setPoisonBuildup(float poisonBuildup) {
        this.poisonBuildup = poisonBuildup;
    }

    public float getParalysisBuildup() {
        return paralysisBuildup;
    }

    public void setParalysisBuildup(float paralysisBuildup) {
        this.paralysisBuildup = paralysisBuildup;
    }

    public float getSleepBuildup() {
        return sleepBuildup;
    }

    public void setSleepBuildup(float sleepBuildup) {
        this.sleepBuildup = sleepBuildup;
    }

    public float getBlastBuildup() {
        return blastBuildup;
    }

    public void setBlastBuildup(float blastBuildup) {
        this.blastBuildup = blastBuildup;
    }
}