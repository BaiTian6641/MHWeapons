package org.example.common.capability.player;

public final class PlayerCombatState {
    private int dodgeIFrameTicks;
    private boolean guardPointActive;
    private boolean focusMode;
    private String actionKey;
    private int actionKeyTicks;

    public int getDodgeIFrameTicks() {
        return dodgeIFrameTicks;
    }

    public void setDodgeIFrameTicks(int dodgeIFrameTicks) {
        this.dodgeIFrameTicks = dodgeIFrameTicks;
    }

    public boolean isGuardPointActive() {
        return guardPointActive;
    }

    public void setGuardPointActive(boolean guardPointActive) {
        this.guardPointActive = guardPointActive;
    }

    public boolean isFocusMode() {
        return focusMode;
    }

    public void setFocusMode(boolean focusMode) {
        this.focusMode = focusMode;
    }

    public String getActionKey() {
        return actionKey;
    }

    public void setActionKey(String actionKey) {
        this.actionKey = actionKey;
    }

    public int getActionKeyTicks() {
        return actionKeyTicks;
    }

    public void setActionKeyTicks(int actionKeyTicks) {
        this.actionKeyTicks = actionKeyTicks;
    }
}
