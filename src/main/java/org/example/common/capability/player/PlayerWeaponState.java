package org.example.common.capability.player;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class PlayerWeaponState {
    private boolean dirty = true;

    private float spiritGauge;
    private int spiritLevel;
    private int spiritLevelTicks;

    private float demonGauge;
    private boolean demonMode;
    private boolean archDemon;
    private int dbComboIndex;
    private int dbComboTick;
    private int dbDemonComboIndex;
    private int dbDemonComboTick;
    private int dbBladeDanceLockTicks;
    private int dbDemonBoostTicks;

    private int hammerChargeLevel;
    private int hammerChargeTicks;
    private boolean hammerPowerCharge;
    private int hammerComboIndex;
    private int hammerComboTick;
    private int hammerBigBangStage;
    private int hammerBigBangTick;

    private int hornNoteA;
    private int hornNoteB;
    private int hornNoteC;
    private int hornNoteD;
    private int hornNoteE;
    private int hornNoteCount;
    private int hornBuffTicks;

    private int hornMelodyA;
    private int hornMelodyB;
    private int hornMelodyC;
    private int hornMelodyCount;
    private int hornMelodyIndex;
    private int hornSpecialMelody;
    private int hornSpecialGuardTicks;
    private int hornMelodyPlayTicks;
    private int hornLastMelodyId;
    private int hornLastMelodyEnhanceTicks;
    private int hornAttackSmallTicks;
    private int hornAttackLargeTicks;
    private int hornDefenseLargeTicks;
    private int hornMelodyHitTicks;
    private int hornStaminaBoostTicks;
    private int hornAffinityTicks;

    private boolean lanceGuardActive;
    private int lancePerfectGuardTicks;
    private boolean lancePowerGuard;

    private int gunlanceShells = 5;
    private int gunlanceMaxShells = 5;
    private int gunlanceCooldown;
    private boolean gunlanceHasStake = true;
    private int gunlanceWyvernfireCooldown;
    private float gunlanceWyvernFireGauge = 2.0f;
    private int gunlanceComboIndex;
    private int gunlanceComboTick;
    private boolean gunlanceCharging;
    private int gunlanceChargeTicks;

    private boolean switchAxeSwordMode;
    private float switchAxeAmpGauge;
    private int switchAxeFrcCooldown;
    private float switchAxeSwitchGauge = 100.0f;
    private boolean switchAxePowerAxe;
    private int switchAxePowerAxeTicks;
    private boolean switchAxeAmped;
    private int switchAxeAmpedTicks;
    private int switchAxeComboIndex;
    private int switchAxeComboTick;
    private int switchAxeWildSwingCount;
    private int switchAxeCounterTicks;

    private boolean chargeBladeSwordMode;
    private int chargeBladePhials;
    private int chargeBladeCharge;
    private boolean cbShieldCharged;
    private int cbShieldChargeTicks;
    private boolean cbSwordBoosted;
    private int cbSwordBoostTicks;
    private boolean cbPowerAxe;
    private int cbPowerAxeTicks;
    private int cbComboIndex;
    private int cbComboTick;
    private int cbGuardPointTicks;
    private int cbDischargeStage; // 0=none, 1=ED1, 2=ED2, 3=AED/SAED

    private boolean insectRed;
    private boolean insectWhite;
    private boolean insectOrange;
    private int insectExtractTicks;
    private int insectTripleFinisherStage; // 0 = none, 1 = tornado started, 2 = descending started
    private int insectTripleFinisherTicks; // Timeout for finisher input window
    private int insectAerialTicks;
    private int insectAerialBounceLevel; // 0-2, increases aerial attack power per Vaulting Dance
    private boolean insectCharging; // true while holding RMB with Red extract
    private int insectChargeTicks; // charge buildup counter
    private int insectComboIndex;
    private int insectComboTick;
    private int insectWhiteJumpBoostCooldown;
    private int kinsectPowderType; // 0=None, 1=Blast, 2=Poison, 3=Paralysis, 4=Heal
    private int kinsectMarkedTargetId = -1;
    private int kinsectMarkedTicks;

    private boolean tonfaShortMode;
    private float tonfaComboGauge;
    private int tonfaFlyingTicks;
    private boolean tonfaDoubleJumped;
    private int tonfaComboIndex;
    private int tonfaComboTick;
    private int tonfaAirActionCount;
    private int tonfaLastHitTick;
    private int tonfaBufferedWeaponInputCount;

    private boolean magnetSpikeImpactMode;
    private float magnetGauge;
    private int magnetTargetId = -1;
    private int magnetTargetTicks;
    private int magnetZipAnimTicks;
    private int magnetZipCooldownTicks;
    private int magnetCutComboIndex;
    private int magnetCutComboTick;
    private int magnetImpactComboIndex;
    private int magnetImpactComboTick;
    private transient final Set<Integer> magnetZipHitIds = new HashSet<>();

    private int accelFuel = 100;
    private int accelDashTicks;
    private int accelParryTicks;

    private float bowCharge;
    private int bowCoating;

    // ── Bowgun fields ──
    private int bowgunMode;               // 0=Standard, 1=Rapid, 2=Versatile, 3=Ignition
    private float bowgunGauge;            // 0-100 gauge for special abilities
    private int bowgunRecoilTimer;        // ticks remaining in recoil recovery
    private int bowgunReloadTimer;        // ticks remaining in reload animation
    private String bowgunCurrentAmmo = "";// current ammo type id
    private int bowgunLastAction;         // last action enum (0=none, 1=fire, 2=rapid, etc.)
    private int bowgunModeSwitchTicks;    // animation timer for mode switch
    private boolean bowgunAiming;         // true while ADS/focus mode
    private boolean bowgunFiring;         // true during sustained fire (Wyvernheart)
    private int bowgunSustainedHits;      // sustained fire hit counter for damage ramp
    private boolean bowgunGuarding;       // true while actively blocking
    private int bowgunGuardTicks;         // ticks since guard started (for perfect guard)
    private boolean bowgunAutoGuard;      // true if auto-guard is active (heavy builds)
    private int bowgunSpecialAmmoTimer;   // cooldown for special ammo (wyvernblast/adhesive)
    private int bowgunWeight;             // cached computed weight (synced)

    private boolean chargingAttack;
    private int chargeAttackTicks;

    private float stamina = 100.0f;
    private float maxStamina = 100.0f;
    private int staminaRecoveryDelay;

    private boolean longSwordSpecialSheathe;
    private int longSwordSheatheTicks;
    private int longSwordSpiritComboIndex;
    private int longSwordSpiritComboTick;
    private int longSwordOverheadComboIndex;
    private int longSwordOverheadComboTick;
    private boolean longSwordChargeReady;
    private int longSwordFadeSlashTicks;
    private float longSwordHelmBreakerDirX;
    private float longSwordHelmBreakerDirY;
    private float longSwordHelmBreakerDirZ;
    private int longSwordAltComboTicks;
    private int longSwordHelmBreakerFollowupTicks;
    private int longSwordHelmBreakerFollowupStage;
    private int longSwordHelmBreakerSpiritLevel;
    private int longSwordThrustLockTicks;
    private int longSwordThrustComboIndex;
    private int longSwordThrustComboTick;
    private int longSwordHelmBreakerCooldown;

    private int kinsectEntityId = -1;

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
    }

    private void markDirty() {
        dirty = true;
    }

    public float getSpiritGauge() {
        return spiritGauge;
    }

    public boolean isChargingAttack() {
        return chargingAttack;
    }

    public void setChargingAttack(boolean chargingAttack) {
        if (this.chargingAttack != chargingAttack) {
            this.chargingAttack = chargingAttack;
            markDirty();
        }
    }

    public int getChargeAttackTicks() {
        return chargeAttackTicks;
    }

    public void setChargeAttackTicks(int chargeAttackTicks) {
        int clamped = Math.max(0, chargeAttackTicks);
        if (this.chargeAttackTicks != clamped) {
            this.chargeAttackTicks = clamped;
            markDirty();
        }
    }

    public void setSpiritGauge(float spiritGauge) {
        float clamped = Math.max(0.0f, Math.min(100.0f, spiritGauge));
        if (this.spiritGauge != clamped) {
            this.spiritGauge = clamped;
            markDirty();
        }
    }

    public void addSpiritGauge(float delta) {
        setSpiritGauge(spiritGauge + delta);
    }

    public int getSpiritLevel() {
        return spiritLevel;
    }

    public void setSpiritLevel(int spiritLevel) {
        int clamped = Math.max(0, Math.min(3, spiritLevel));
        if (this.spiritLevel != clamped) {
            this.spiritLevel = clamped;
            markDirty();
        }
    }

    public int getSpiritLevelTicks() {
        return spiritLevelTicks;
    }

    public void setSpiritLevelTicks(int spiritLevelTicks) {
        int clamped = Math.max(0, spiritLevelTicks);
        if (this.spiritLevelTicks != clamped) {
            this.spiritLevelTicks = clamped;
            markDirty();
        }
    }

    public float getDemonGauge() {
        return demonGauge;
    }

    public void setDemonGauge(float demonGauge) {
        float clamped = Math.max(0.0f, Math.min(100.0f, demonGauge));
        if (this.demonGauge != clamped) {
            this.demonGauge = clamped;
            markDirty();
        }
    }

    public void addDemonGauge(float delta) {
        setDemonGauge(demonGauge + delta);
    }

    public boolean isDemonMode() {
        return demonMode;
    }

    public void setDemonMode(boolean demonMode) {
        if (this.demonMode != demonMode) {
            this.demonMode = demonMode;
            markDirty();
        }
    }

    public boolean isArchDemon() {
        return archDemon;
    }

    public void setArchDemon(boolean archDemon) {
        if (this.archDemon != archDemon) {
            this.archDemon = archDemon;
            markDirty();
        }
    }

    public int getDbComboIndex() {
        return dbComboIndex;
    }

    public void setDbComboIndex(int dbComboIndex) {
        if (this.dbComboIndex != dbComboIndex) {
            this.dbComboIndex = dbComboIndex;
            markDirty();
        }
    }

    public int getDbComboTick() {
        return dbComboTick;
    }

    public void setDbComboTick(int dbComboTick) {
        if (this.dbComboTick != dbComboTick) {
            this.dbComboTick = dbComboTick;
            markDirty();
        }
    }

    public int getDbDemonComboIndex() {
        return dbDemonComboIndex;
    }

    public void setDbDemonComboIndex(int dbDemonComboIndex) {
        if (this.dbDemonComboIndex != dbDemonComboIndex) {
            this.dbDemonComboIndex = dbDemonComboIndex;
            markDirty();
        }
    }

    public int getDbDemonComboTick() {
        return dbDemonComboTick;
    }

    public void setDbDemonComboTick(int dbDemonComboTick) {
        if (this.dbDemonComboTick != dbDemonComboTick) {
            this.dbDemonComboTick = dbDemonComboTick;
            markDirty();
        }
    }

    public int getDbBladeDanceLockTicks() {
        return dbBladeDanceLockTicks;
    }

    public void setDbBladeDanceLockTicks(int dbBladeDanceLockTicks) {
        int clamped = Math.max(0, dbBladeDanceLockTicks);
        if (this.dbBladeDanceLockTicks != clamped) {
            this.dbBladeDanceLockTicks = clamped;
            markDirty();
        }
    }

    public int getDbDemonBoostTicks() {
        return dbDemonBoostTicks;
    }

    public void setDbDemonBoostTicks(int dbDemonBoostTicks) {
        int clamped = Math.max(0, dbDemonBoostTicks);
        if (this.dbDemonBoostTicks != clamped) {
            this.dbDemonBoostTicks = clamped;
            markDirty();
        }
    }

    public int getHammerChargeLevel() {
        return hammerChargeLevel;
    }

    public void setHammerChargeLevel(int hammerChargeLevel) {
        int clamped = Math.max(0, Math.min(3, hammerChargeLevel));
        if (this.hammerChargeLevel != clamped) {
            this.hammerChargeLevel = clamped;
            markDirty();
        }
    }

    public int getHammerChargeTicks() {
        return hammerChargeTicks;
    }

    public void setHammerChargeTicks(int hammerChargeTicks) {
        int clamped = Math.max(0, hammerChargeTicks);
        if (this.hammerChargeTicks != clamped) {
            this.hammerChargeTicks = clamped;
            markDirty();
        }
    }

    public boolean isHammerPowerCharge() {
        return hammerPowerCharge;
    }

    public void setHammerPowerCharge(boolean hammerPowerCharge) {
        if (this.hammerPowerCharge != hammerPowerCharge) {
            this.hammerPowerCharge = hammerPowerCharge;
            markDirty();
        }
    }

    public int getHammerComboIndex() { return hammerComboIndex; }
    public void setHammerComboIndex(int v) { if (this.hammerComboIndex != v) { this.hammerComboIndex = v; markDirty(); } }

    public int getHammerComboTick() { return hammerComboTick; }
    public void setHammerComboTick(int v) { if (this.hammerComboTick != v) { this.hammerComboTick = v; markDirty(); } }

    public int getHammerBigBangStage() { return hammerBigBangStage; }
    public void setHammerBigBangStage(int v) { int c = Math.max(0, Math.min(5, v)); if (this.hammerBigBangStage != c) { this.hammerBigBangStage = c; markDirty(); } }

    public int getHammerBigBangTick() { return hammerBigBangTick; }
    public void setHammerBigBangTick(int v) { if (this.hammerBigBangTick != v) { this.hammerBigBangTick = v; markDirty(); } }

    public int getHornNoteA() {
        return hornNoteA;
    }

    public int getHornNoteB() {
        return hornNoteB;
    }

    public int getHornNoteC() {
        return hornNoteC;
    }

    public int getHornNoteD() {
        return hornNoteD;
    }

    public int getHornNoteE() {
        return hornNoteE;
    }

    public int getHornNoteCount() {
        return hornNoteCount;
    }

    public void addHornNote(int note) {
        if (note < 1 || note > 3) {
            return;
        }
        if (hornNoteCount < 5) {
            if (hornNoteCount == 0) {
                hornNoteA = note;
            } else if (hornNoteCount == 1) {
                hornNoteB = note;
            } else if (hornNoteCount == 2) {
                hornNoteC = note;
            } else if (hornNoteCount == 3) {
                hornNoteD = note;
            } else {
                hornNoteE = note;
            }
            hornNoteCount += 1;
        } else {
            hornNoteA = hornNoteB;
            hornNoteB = hornNoteC;
            hornNoteC = hornNoteD;
            hornNoteD = hornNoteE;
            hornNoteE = note;
        }
        markDirty();
    }

    public void clearHornNotes() {
        if (hornNoteCount > 0 || hornNoteA != 0 || hornNoteB != 0 || hornNoteC != 0 || hornNoteD != 0 || hornNoteE != 0) {
            hornNoteA = 0;
            hornNoteB = 0;
            hornNoteC = 0;
            hornNoteD = 0;
            hornNoteE = 0;
            hornNoteCount = 0;
            markDirty();
        }
    }

    public int getHornBuffTicks() {
        return hornBuffTicks;
    }

    public void setHornBuffTicks(int hornBuffTicks) {
        int clamped = Math.max(0, hornBuffTicks);
        if (this.hornBuffTicks != clamped) {
            this.hornBuffTicks = clamped;
            markDirty();
        }
    }

    public int getHornMelodyA() {
        return hornMelodyA;
    }

    public int getHornMelodyB() {
        return hornMelodyB;
    }

    public int getHornMelodyC() {
        return hornMelodyC;
    }

    public int getHornMelodyCount() {
        return hornMelodyCount;
    }

    public int getHornMelodyIndex() {
        return hornMelodyIndex;
    }

    public void setHornMelodyIndex(int hornMelodyIndex) {
        int clamped = Math.max(0, Math.min(2, hornMelodyIndex));
        if (this.hornMelodyIndex != clamped) {
            this.hornMelodyIndex = clamped;
            markDirty();
        }
    }

    public void addHornMelody(int melodyId) {
        if (melodyId <= 0) {
            return;
        }
        if (hornMelodyCount < 3) {
            if (hornMelodyCount == 0) {
                hornMelodyA = melodyId;
            } else if (hornMelodyCount == 1) {
                hornMelodyB = melodyId;
            } else {
                hornMelodyC = melodyId;
            }
            hornMelodyCount += 1;
        } else {
            hornMelodyA = hornMelodyB;
            hornMelodyB = hornMelodyC;
            hornMelodyC = melodyId;
        }
        markDirty();
    }

    public void clearHornMelodies() {
        if (hornMelodyCount > 0 || hornMelodyA != 0 || hornMelodyB != 0 || hornMelodyC != 0) {
            hornMelodyA = 0;
            hornMelodyB = 0;
            hornMelodyC = 0;
            hornMelodyCount = 0;
            hornMelodyIndex = 0;
            markDirty();
        }
    }

    public int consumeHornMelodyAt(int index) {
        if (hornMelodyCount <= 0) {
            return 0;
        }
        int clamped = Math.max(0, Math.min(index, hornMelodyCount - 1));
        int melodyId = switch (clamped) {
            case 0 -> hornMelodyA;
            case 1 -> hornMelodyB;
            default -> hornMelodyC;
        };
        if (hornMelodyCount == 1) {
            hornMelodyA = 0;
            hornMelodyB = 0;
            hornMelodyC = 0;
            hornMelodyCount = 0;
            hornMelodyIndex = 0;
        } else if (hornMelodyCount == 2) {
            if (clamped == 0) {
                hornMelodyA = hornMelodyB;
            }
            hornMelodyB = 0;
            hornMelodyC = 0;
            hornMelodyCount = 1;
            hornMelodyIndex = Math.min(hornMelodyIndex, 0);
        } else {
            if (clamped == 0) {
                hornMelodyA = hornMelodyB;
                hornMelodyB = hornMelodyC;
            } else if (clamped == 1) {
                hornMelodyB = hornMelodyC;
            }
            hornMelodyC = 0;
            hornMelodyCount = 2;
            hornMelodyIndex = Math.min(hornMelodyIndex, hornMelodyCount - 1);
        }
        markDirty();
        return melodyId;
    }

    public int getHornSpecialMelody() {
        return hornSpecialMelody;
    }

    public void setHornSpecialMelody(int hornSpecialMelody) {
        if (this.hornSpecialMelody != hornSpecialMelody) {
            this.hornSpecialMelody = hornSpecialMelody;
            markDirty();
        }
    }

    public int getHornSpecialGuardTicks() {
        return hornSpecialGuardTicks;
    }

    public void setHornSpecialGuardTicks(int hornSpecialGuardTicks) {
        int clamped = Math.max(0, hornSpecialGuardTicks);
        if (this.hornSpecialGuardTicks != clamped) {
            this.hornSpecialGuardTicks = clamped;
            markDirty();
        }
    }

    public int getHornMelodyPlayTicks() {
        return hornMelodyPlayTicks;
    }

    public void setHornMelodyPlayTicks(int hornMelodyPlayTicks) {
        int clamped = Math.max(0, hornMelodyPlayTicks);
        if (this.hornMelodyPlayTicks != clamped) {
            this.hornMelodyPlayTicks = clamped;
            markDirty();
        }
    }

    public int getHornLastMelodyId() {
        return hornLastMelodyId;
    }

    public void setHornLastMelodyId(int hornLastMelodyId) {
        int clamped = Math.max(0, hornLastMelodyId);
        if (this.hornLastMelodyId != clamped) {
            this.hornLastMelodyId = clamped;
            markDirty();
        }
    }

    public int getHornLastMelodyEnhanceTicks() {
        return hornLastMelodyEnhanceTicks;
    }

    public void setHornLastMelodyEnhanceTicks(int hornLastMelodyEnhanceTicks) {
        int clamped = Math.max(0, hornLastMelodyEnhanceTicks);
        if (this.hornLastMelodyEnhanceTicks != clamped) {
            this.hornLastMelodyEnhanceTicks = clamped;
            markDirty();
        }
    }

    public int getHornAttackSmallTicks() {
        return hornAttackSmallTicks;
    }

    public void setHornAttackSmallTicks(int hornAttackSmallTicks) {
        int clamped = Math.max(0, hornAttackSmallTicks);
        if (this.hornAttackSmallTicks != clamped) {
            this.hornAttackSmallTicks = clamped;
            markDirty();
        }
    }

    public int getHornAttackLargeTicks() {
        return hornAttackLargeTicks;
    }

    public void setHornAttackLargeTicks(int hornAttackLargeTicks) {
        int clamped = Math.max(0, hornAttackLargeTicks);
        if (this.hornAttackLargeTicks != clamped) {
            this.hornAttackLargeTicks = clamped;
            markDirty();
        }
    }

    public int getHornDefenseLargeTicks() {
        return hornDefenseLargeTicks;
    }

    public void setHornDefenseLargeTicks(int hornDefenseLargeTicks) {
        int clamped = Math.max(0, hornDefenseLargeTicks);
        if (this.hornDefenseLargeTicks != clamped) {
            this.hornDefenseLargeTicks = clamped;
            markDirty();
        }
    }

    public int getHornMelodyHitTicks() {
        return hornMelodyHitTicks;
    }

    public void setHornMelodyHitTicks(int hornMelodyHitTicks) {
        int clamped = Math.max(0, hornMelodyHitTicks);
        if (this.hornMelodyHitTicks != clamped) {
            this.hornMelodyHitTicks = clamped;
            markDirty();
        }
    }

    public int getHornStaminaBoostTicks() {
        return hornStaminaBoostTicks;
    }

    public void setHornStaminaBoostTicks(int hornStaminaBoostTicks) {
        int clamped = Math.max(0, hornStaminaBoostTicks);
        if (this.hornStaminaBoostTicks != clamped) {
            this.hornStaminaBoostTicks = clamped;
            markDirty();
        }
    }

    public int getHornAffinityTicks() {
        return hornAffinityTicks;
    }

    public void setHornAffinityTicks(int hornAffinityTicks) {
        int clamped = Math.max(0, hornAffinityTicks);
        if (this.hornAffinityTicks != clamped) {
            this.hornAffinityTicks = clamped;
            markDirty();
        }
    }

    public boolean isLanceGuardActive() {
        return lanceGuardActive;
    }

    public void setLanceGuardActive(boolean lanceGuardActive) {
        if (this.lanceGuardActive != lanceGuardActive) {
            this.lanceGuardActive = lanceGuardActive;
            markDirty();
        }
    }

    public int getLancePerfectGuardTicks() {
        return lancePerfectGuardTicks;
    }

    public void setLancePerfectGuardTicks(int lancePerfectGuardTicks) {
        int clamped = Math.max(0, lancePerfectGuardTicks);
        if (this.lancePerfectGuardTicks != clamped) {
            this.lancePerfectGuardTicks = clamped;
            markDirty();
        }
    }

    public boolean isLancePowerGuard() {
        return lancePowerGuard;
    }

    public void setLancePowerGuard(boolean lancePowerGuard) {
        if (this.lancePowerGuard != lancePowerGuard) {
            this.lancePowerGuard = lancePowerGuard;
            markDirty();
        }
    }

    public int getGunlanceShells() {
        return gunlanceShells;
    }

    public void setGunlanceShells(int gunlanceShells) {
        int clamped = Math.max(0, Math.min(gunlanceMaxShells, gunlanceShells));
        if (this.gunlanceShells != clamped) {
            this.gunlanceShells = clamped;
            markDirty();
        }
    }

    public int getGunlanceMaxShells() {
        return gunlanceMaxShells;
    }

    public void setGunlanceMaxShells(int gunlanceMaxShells) {
        int clamped = Math.max(1, gunlanceMaxShells);
        if (this.gunlanceMaxShells != clamped) {
            this.gunlanceMaxShells = clamped;
            if (gunlanceShells > clamped) {
                gunlanceShells = clamped;
            }
            markDirty();
        }
    }

    public int getGunlanceCooldown() {
        return gunlanceCooldown;
    }

    public void setGunlanceCooldown(int gunlanceCooldown) {
        int clamped = Math.max(0, gunlanceCooldown);
        if (this.gunlanceCooldown != clamped) {
            this.gunlanceCooldown = clamped;
            markDirty();
        }
    }

    public boolean hasGunlanceStake() {
        return gunlanceHasStake;
    }

    public void setGunlanceHasStake(boolean gunlanceHasStake) {
        if (this.gunlanceHasStake != gunlanceHasStake) {
            this.gunlanceHasStake = gunlanceHasStake;
            markDirty();
        }
    }

    public int getGunlanceWyvernfireCooldown() {
        return gunlanceWyvernfireCooldown;
    }

    public void setGunlanceWyvernfireCooldown(int gunlanceWyvernfireCooldown) {
        int clamped = Math.max(0, gunlanceWyvernfireCooldown);
        if (this.gunlanceWyvernfireCooldown != clamped) {
            this.gunlanceWyvernfireCooldown = clamped;
            markDirty();
        }
    }

    public float getGunlanceWyvernFireGauge() {
        return gunlanceWyvernFireGauge;
    }

    public void setGunlanceWyvernFireGauge(float gunlanceWyvernFireGauge) {
        float clamped = Math.max(0.0f, Math.min(2.0f, gunlanceWyvernFireGauge));
        if (this.gunlanceWyvernFireGauge != clamped) {
            this.gunlanceWyvernFireGauge = clamped;
            markDirty();
        }
    }

    public void addGunlanceWyvernFireGauge(float delta) {
        setGunlanceWyvernFireGauge(this.gunlanceWyvernFireGauge + delta);
    }

    public boolean isSwitchAxeSwordMode() {
        return switchAxeSwordMode;
    }

    public void setSwitchAxeSwordMode(boolean switchAxeSwordMode) {
        if (this.switchAxeSwordMode != switchAxeSwordMode) {
            this.switchAxeSwordMode = switchAxeSwordMode;
            markDirty();
        }
    }

    public float getSwitchAxeAmpGauge() {
        return switchAxeAmpGauge;
    }

    public void setSwitchAxeAmpGauge(float switchAxeAmpGauge) {
        float clamped = Math.max(0.0f, Math.min(100.0f, switchAxeAmpGauge));
        if (this.switchAxeAmpGauge != clamped) {
            this.switchAxeAmpGauge = clamped;
            markDirty();
        }
    }

    public void addSwitchAxeAmpGauge(float delta) {
        setSwitchAxeAmpGauge(switchAxeAmpGauge + delta);
    }

    public int getSwitchAxeFrcCooldown() {
        return switchAxeFrcCooldown;
    }

    public void setSwitchAxeFrcCooldown(int switchAxeFrcCooldown) {
        int clamped = Math.max(0, switchAxeFrcCooldown);
        if (this.switchAxeFrcCooldown != clamped) {
            this.switchAxeFrcCooldown = clamped;
            markDirty();
        }
    }

    public float getSwitchAxeSwitchGauge() {
        return switchAxeSwitchGauge;
    }

    public void setSwitchAxeSwitchGauge(float value) {
        float clamped = Math.max(0.0f, Math.min(100.0f, value));
        if (this.switchAxeSwitchGauge != clamped) {
            this.switchAxeSwitchGauge = clamped;
            markDirty();
        }
    }

    public void addSwitchAxeSwitchGauge(float delta) {
        setSwitchAxeSwitchGauge(switchAxeSwitchGauge + delta);
    }

    public boolean isSwitchAxePowerAxe() {
        return switchAxePowerAxe;
    }

    public void setSwitchAxePowerAxe(boolean value) {
        if (this.switchAxePowerAxe != value) {
            this.switchAxePowerAxe = value;
            markDirty();
        }
    }

    public int getSwitchAxePowerAxeTicks() {
        return switchAxePowerAxeTicks;
    }

    public void setSwitchAxePowerAxeTicks(int value) {
        if (this.switchAxePowerAxeTicks != value) {
            this.switchAxePowerAxeTicks = value;
            markDirty();
        }
    }

    public boolean isSwitchAxeAmped() {
        return switchAxeAmped;
    }

    public void setSwitchAxeAmped(boolean value) {
        if (this.switchAxeAmped != value) {
            this.switchAxeAmped = value;
            markDirty();
        }
    }

    public int getSwitchAxeAmpedTicks() {
        return switchAxeAmpedTicks;
    }

    public void setSwitchAxeAmpedTicks(int value) {
        if (this.switchAxeAmpedTicks != value) {
            this.switchAxeAmpedTicks = value;
            markDirty();
        }
    }

    public int getSwitchAxeComboIndex() {
        return switchAxeComboIndex;
    }

    public void setSwitchAxeComboIndex(int value) {
        if (this.switchAxeComboIndex != value) {
            this.switchAxeComboIndex = value;
            markDirty();
        }
    }

    public int getSwitchAxeComboTick() {
        return switchAxeComboTick;
    }

    public void setSwitchAxeComboTick(int value) {
        if (this.switchAxeComboTick != value) {
            this.switchAxeComboTick = value;
            markDirty();
        }
    }

    public int getSwitchAxeWildSwingCount() {
        return switchAxeWildSwingCount;
    }

    public void setSwitchAxeWildSwingCount(int value) {
        if (this.switchAxeWildSwingCount != value) {
            this.switchAxeWildSwingCount = value;
            markDirty();
        }
    }

    public int getSwitchAxeCounterTicks() {
        return switchAxeCounterTicks;
    }

    public void setSwitchAxeCounterTicks(int value) {
        if (this.switchAxeCounterTicks != value) {
            this.switchAxeCounterTicks = value;
            markDirty();
        }
    }

    public boolean isChargeBladeSwordMode() {
        return chargeBladeSwordMode;
    }

    public void setChargeBladeSwordMode(boolean chargeBladeSwordMode) {
        if (this.chargeBladeSwordMode != chargeBladeSwordMode) {
            this.chargeBladeSwordMode = chargeBladeSwordMode;
            markDirty();
        }
    }

    public int getChargeBladePhials() {
        return chargeBladePhials;
    }

    public void setChargeBladePhials(int chargeBladePhials) {
        int clamped = Math.max(0, Math.min(5, chargeBladePhials));
        if (this.chargeBladePhials != clamped) {
            this.chargeBladePhials = clamped;
            markDirty();
        }
    }

    public int getChargeBladeCharge() {
        return chargeBladeCharge;
    }

    public void setChargeBladeCharge(int chargeBladeCharge) {
        int clamped = Math.max(0, Math.min(100, chargeBladeCharge));
        if (this.chargeBladeCharge != clamped) {
            this.chargeBladeCharge = clamped;
            markDirty();
        }
    }

    public boolean isCbShieldCharged() { return cbShieldCharged; }
    public void setCbShieldCharged(boolean v) { if (this.cbShieldCharged != v) { this.cbShieldCharged = v; markDirty(); } }

    public int getCbShieldChargeTicks() { return cbShieldChargeTicks; }
    public void setCbShieldChargeTicks(int v) { if (this.cbShieldChargeTicks != v) { this.cbShieldChargeTicks = v; markDirty(); } }

    public boolean isCbSwordBoosted() { return cbSwordBoosted; }
    public void setCbSwordBoosted(boolean v) { if (this.cbSwordBoosted != v) { this.cbSwordBoosted = v; markDirty(); } }

    public int getCbSwordBoostTicks() { return cbSwordBoostTicks; }
    public void setCbSwordBoostTicks(int v) { if (this.cbSwordBoostTicks != v) { this.cbSwordBoostTicks = v; markDirty(); } }

    public boolean isCbPowerAxe() { return cbPowerAxe; }
    public void setCbPowerAxe(boolean v) { if (this.cbPowerAxe != v) { this.cbPowerAxe = v; markDirty(); } }

    public int getCbPowerAxeTicks() { return cbPowerAxeTicks; }
    public void setCbPowerAxeTicks(int v) { if (this.cbPowerAxeTicks != v) { this.cbPowerAxeTicks = v; markDirty(); } }

    public int getCbComboIndex() { return cbComboIndex; }
    public void setCbComboIndex(int v) { if (this.cbComboIndex != v) { this.cbComboIndex = v; markDirty(); } }

    public int getCbComboTick() { return cbComboTick; }
    public void setCbComboTick(int v) { if (this.cbComboTick != v) { this.cbComboTick = v; markDirty(); } }

    public int getCbGuardPointTicks() { return cbGuardPointTicks; }
    public void setCbGuardPointTicks(int v) { if (this.cbGuardPointTicks != v) { this.cbGuardPointTicks = v; markDirty(); } }

    public int getCbDischargeStage() { return cbDischargeStage; }
    public void setCbDischargeStage(int v) { if (this.cbDischargeStage != v) { this.cbDischargeStage = v; markDirty(); } }

    public boolean isInsectRed() {
        return insectRed;
    }

    public void setInsectRed(boolean insectRed) {
        if (this.insectRed != insectRed) {
            this.insectRed = insectRed;
            markDirty();
        }
    }

    public boolean isInsectWhite() {
        return insectWhite;
    }

    public void setInsectWhite(boolean insectWhite) {
        if (this.insectWhite != insectWhite) {
            this.insectWhite = insectWhite;
            markDirty();
        }
    }

    public boolean isInsectOrange() {
        return insectOrange;
    }

    public void setInsectOrange(boolean insectOrange) {
        if (this.insectOrange != insectOrange) {
            this.insectOrange = insectOrange;
            markDirty();
        }
    }

    public int getInsectExtractTicks() {
        return insectExtractTicks;
    }

    public void setInsectExtractTicks(int insectExtractTicks) {
        int clamped = Math.max(0, insectExtractTicks);
        if (this.insectExtractTicks != clamped) {
            this.insectExtractTicks = clamped;
            markDirty();
        }
    }

    public int getInsectTripleFinisherStage() {
        return insectTripleFinisherStage;
    }

    public void setInsectTripleFinisherStage(int insectTripleFinisherStage) {
        int clamped = Math.max(0, insectTripleFinisherStage);
        if (this.insectTripleFinisherStage != clamped) {
            this.insectTripleFinisherStage = clamped;
            markDirty();
        }
    }

    public int getInsectTripleFinisherTicks() {
        return insectTripleFinisherTicks;
    }

    public void setInsectTripleFinisherTicks(int insectTripleFinisherTicks) {
        int clamped = Math.max(0, insectTripleFinisherTicks);
        if (this.insectTripleFinisherTicks != clamped) {
            this.insectTripleFinisherTicks = clamped;
            markDirty();
        }
    }

    public int getInsectAerialTicks() {
        return insectAerialTicks;
    }

    public void setInsectAerialTicks(int insectAerialTicks) {
        int clamped = Math.max(0, insectAerialTicks);
        if (this.insectAerialTicks != clamped) {
            this.insectAerialTicks = clamped;
            markDirty();
        }
    }

    public int getInsectComboIndex() {
        return insectComboIndex;
    }

    public void setInsectComboIndex(int insectComboIndex) {
        int clamped = Math.max(0, insectComboIndex);
        if (this.insectComboIndex != clamped) {
            this.insectComboIndex = clamped;
            markDirty();
        }
    }

    public int getInsectComboTick() {
        return insectComboTick;
    }

    public void setInsectComboTick(int insectComboTick) {
        int clamped = Math.max(0, insectComboTick);
        if (this.insectComboTick != clamped) {
            this.insectComboTick = clamped;
            markDirty();
        }
    }

    public int getInsectAerialBounceLevel() {
        return insectAerialBounceLevel;
    }

    public void setInsectAerialBounceLevel(int insectAerialBounceLevel) {
        int clamped = Math.max(0, Math.min(2, insectAerialBounceLevel));
        if (this.insectAerialBounceLevel != clamped) {
            this.insectAerialBounceLevel = clamped;
            markDirty();
        }
    }

    public boolean isInsectCharging() {
        return insectCharging;
    }

    public void setInsectCharging(boolean insectCharging) {
        if (this.insectCharging != insectCharging) {
            this.insectCharging = insectCharging;
            markDirty();
        }
    }

    public int getInsectChargeTicks() {
        return insectChargeTicks;
    }

    public void setInsectChargeTicks(int insectChargeTicks) {
        int clamped = Math.max(0, insectChargeTicks);
        if (this.insectChargeTicks != clamped) {
            this.insectChargeTicks = clamped;
            markDirty();
        }
    }

    public boolean isTonfaShortMode() {
        return tonfaShortMode;
    }

    public void setTonfaShortMode(boolean tonfaShortMode) {
        if (this.tonfaShortMode != tonfaShortMode) {
            this.tonfaShortMode = tonfaShortMode;
            markDirty();
        }
    }

    public float getTonfaComboGauge() {
        return tonfaComboGauge;
    }

    public void setTonfaComboGauge(float tonfaComboGauge) {
        float clamped = Math.max(0.0f, Math.min(100.0f, tonfaComboGauge));
        if (this.tonfaComboGauge != clamped) {
            this.tonfaComboGauge = clamped;
            markDirty();
        }
    }

    public void addTonfaComboGauge(float delta) {
        setTonfaComboGauge(tonfaComboGauge + delta);
    }

    public int getTonfaFlyingTicks() {
        return tonfaFlyingTicks;
    }

    public void setTonfaFlyingTicks(int tonfaFlyingTicks) {
        int clamped = Math.max(0, tonfaFlyingTicks);
        if (this.tonfaFlyingTicks != clamped) {
            this.tonfaFlyingTicks = clamped;
            markDirty();
        }
    }

    public boolean isTonfaDoubleJumped() {
        return tonfaDoubleJumped;
    }

    public void setTonfaDoubleJumped(boolean tonfaDoubleJumped) {
        if (this.tonfaDoubleJumped != tonfaDoubleJumped) {
            this.tonfaDoubleJumped = tonfaDoubleJumped;
            markDirty();
        }
    }

    public int getTonfaComboIndex() {
        return tonfaComboIndex;
    }

    public void setTonfaComboIndex(int tonfaComboIndex) {
        int clamped = Math.max(0, tonfaComboIndex);
        if (this.tonfaComboIndex != clamped) {
            this.tonfaComboIndex = clamped;
            markDirty();
        }
    }

    public int getTonfaComboTick() {
        return tonfaComboTick;
    }

    public void setTonfaComboTick(int tonfaComboTick) {
        int clamped = Math.max(0, tonfaComboTick);
        if (this.tonfaComboTick != clamped) {
            this.tonfaComboTick = clamped;
            markDirty();
        }
    }

    public int getTonfaAirActionCount() {
        return tonfaAirActionCount;
    }

    public void setTonfaAirActionCount(int tonfaAirActionCount) {
        int clamped = Math.max(0, tonfaAirActionCount);
        if (this.tonfaAirActionCount != clamped) {
            this.tonfaAirActionCount = clamped;
            markDirty();
        }
    }

    public int getTonfaLastHitTick() {
        return tonfaLastHitTick;
    }

    public void setTonfaLastHitTick(int tonfaLastHitTick) {
        if (this.tonfaLastHitTick != tonfaLastHitTick) {
            this.tonfaLastHitTick = tonfaLastHitTick;
            markDirty();
        }
    }

    public int getTonfaBufferedWeaponInputCount() {
        return tonfaBufferedWeaponInputCount;
    }

    public void setTonfaBufferedWeaponInputCount(int tonfaBufferedWeaponInputCount) {
        int clamped = Math.max(0, Math.min(2, tonfaBufferedWeaponInputCount));
        if (this.tonfaBufferedWeaponInputCount != clamped) {
            this.tonfaBufferedWeaponInputCount = clamped;
            markDirty();
        }
    }

    public void addTonfaBufferedWeaponInput(int delta) {
        if (delta == 0) {
            return;
        }
        setTonfaBufferedWeaponInputCount(tonfaBufferedWeaponInputCount + delta);
    }

    public boolean hasTonfaBufferedWeaponInput() {
        return tonfaBufferedWeaponInputCount > 0;
    }

    public void consumeTonfaBufferedWeaponInput() {
        if (tonfaBufferedWeaponInputCount > 0) {
            tonfaBufferedWeaponInputCount--;
            markDirty();
        }
    }

    public boolean isMagnetSpikeImpactMode() {
        return magnetSpikeImpactMode;
    }

    public void setMagnetSpikeImpactMode(boolean magnetSpikeImpactMode) {
        if (this.magnetSpikeImpactMode != magnetSpikeImpactMode) {
            this.magnetSpikeImpactMode = magnetSpikeImpactMode;
            markDirty();
        }
    }

    public int getMagnetZipAnimTicks() {
        return magnetZipAnimTicks;
    }

    public void setMagnetZipAnimTicks(int magnetZipAnimTicks) {
        int clamped = Math.max(0, magnetZipAnimTicks);
        if (this.magnetZipAnimTicks != clamped) {
            this.magnetZipAnimTicks = clamped;
            markDirty();
        }
    }

    public int getMagnetZipCooldownTicks() {
        return magnetZipCooldownTicks;
    }

    public void setMagnetZipCooldownTicks(int magnetZipCooldownTicks) {
        int clamped = Math.max(0, magnetZipCooldownTicks);
        if (this.magnetZipCooldownTicks != clamped) {
            this.magnetZipCooldownTicks = clamped;
            markDirty();
        }
    }

    public float getMagnetGauge() {
        return magnetGauge;
    }

    public void setMagnetGauge(float magnetGauge) {
        float clamped = Math.max(0.0f, Math.min(100.0f, magnetGauge));
        if (this.magnetGauge != clamped) {
            this.magnetGauge = clamped;
            markDirty();
        }
    }

    public void addMagnetGauge(float delta) {
        setMagnetGauge(magnetGauge + delta);
    }

    public int getMagnetTargetId() {
        return magnetTargetId;
    }

    public void setMagnetTargetId(int magnetTargetId) {
        if (this.magnetTargetId != magnetTargetId) {
            this.magnetTargetId = magnetTargetId;
            markDirty();
        }
    }

    public int getMagnetTargetTicks() {
        return magnetTargetTicks;
    }

    public void setMagnetTargetTicks(int magnetTargetTicks) {
        int clamped = Math.max(0, magnetTargetTicks);
        if (this.magnetTargetTicks != clamped) {
            this.magnetTargetTicks = clamped;
            markDirty();
        }
    }

    public int getMagnetCutComboIndex() {
        return magnetCutComboIndex;
    }

    public void setMagnetCutComboIndex(int magnetCutComboIndex) {
        if (this.magnetCutComboIndex != magnetCutComboIndex) {
            this.magnetCutComboIndex = magnetCutComboIndex;
            markDirty();
        }
    }

    public int getMagnetCutComboTick() {
        return magnetCutComboTick;
    }

    public void setMagnetCutComboTick(int magnetCutComboTick) {
        if (this.magnetCutComboTick != magnetCutComboTick) {
            this.magnetCutComboTick = magnetCutComboTick;
            markDirty();
        }
    }

    public int getMagnetImpactComboIndex() {
        return magnetImpactComboIndex;
    }

    public void setMagnetImpactComboIndex(int magnetImpactComboIndex) {
        if (this.magnetImpactComboIndex != magnetImpactComboIndex) {
            this.magnetImpactComboIndex = magnetImpactComboIndex;
            markDirty();
        }
    }

    public int getMagnetImpactComboTick() {
        return magnetImpactComboTick;
    }

    public void setMagnetImpactComboTick(int magnetImpactComboTick) {
        if (this.magnetImpactComboTick != magnetImpactComboTick) {
            this.magnetImpactComboTick = magnetImpactComboTick;
            markDirty();
        }
    }

    public int getAccelFuel() {
        return accelFuel;
    }

    public void setAccelFuel(int accelFuel) {
        int clamped = Math.max(0, Math.min(100, accelFuel));
        if (this.accelFuel != clamped) {
            this.accelFuel = clamped;
            markDirty();
        }
    }

    public void addAccelFuel(int delta) {
        setAccelFuel(accelFuel + delta);
    }

    public int getAccelDashTicks() {
        return accelDashTicks;
    }

    public void setAccelDashTicks(int accelDashTicks) {
        int clamped = Math.max(0, accelDashTicks);
        if (this.accelDashTicks != clamped) {
            this.accelDashTicks = clamped;
            markDirty();
        }
    }

    public int getAccelParryTicks() {
        return accelParryTicks;
    }

    public void setAccelParryTicks(int accelParryTicks) {
        int clamped = Math.max(0, accelParryTicks);
        if (this.accelParryTicks != clamped) {
            this.accelParryTicks = clamped;
            markDirty();
        }
    }

    public void clearMagnetZipHits() {
        magnetZipHitIds.clear();
    }

    public boolean registerMagnetZipHit(int entityId) {
        return magnetZipHitIds.add(entityId);
    }

    public float getBowCharge() {
        return bowCharge;
    }

    public void setBowCharge(float bowCharge) {
        float clamped = Math.max(0.0f, Math.min(1.0f, bowCharge));
        if (this.bowCharge != clamped) {
            this.bowCharge = clamped;
            markDirty();
        }
    }

    public int getBowCoating() {
        return bowCoating;
    }

    public void setBowCoating(int bowCoating) {
        int clamped = Math.max(0, Math.min(3, bowCoating));
        if (this.bowCoating != clamped) {
            this.bowCoating = clamped;
            markDirty();
        }
    }

    public void copyFrom(PlayerWeaponState other) {
        if (other == null) {
            return;
        }
        spiritGauge = other.spiritGauge;
        spiritLevel = other.spiritLevel;
        spiritLevelTicks = other.spiritLevelTicks;
        demonGauge = other.demonGauge;
        demonMode = other.demonMode;
        archDemon = other.archDemon;
        dbComboIndex = other.dbComboIndex;
        dbComboTick = other.dbComboTick;
        dbDemonComboIndex = other.dbDemonComboIndex;
        dbDemonComboTick = other.dbDemonComboTick;
        dbBladeDanceLockTicks = other.dbBladeDanceLockTicks;
        dbDemonBoostTicks = other.dbDemonBoostTicks;
        hammerChargeLevel = other.hammerChargeLevel;
        hammerChargeTicks = other.hammerChargeTicks;
        hammerPowerCharge = other.hammerPowerCharge;
        hammerComboIndex = other.hammerComboIndex;
        hammerComboTick = other.hammerComboTick;
        hammerBigBangStage = other.hammerBigBangStage;
        hammerBigBangTick = other.hammerBigBangTick;
        hornNoteA = other.hornNoteA;
        hornNoteB = other.hornNoteB;
        hornNoteC = other.hornNoteC;
        hornNoteD = other.hornNoteD;
        hornNoteE = other.hornNoteE;
        hornNoteCount = other.hornNoteCount;
        hornBuffTicks = other.hornBuffTicks;
        hornMelodyA = other.hornMelodyA;
        hornMelodyB = other.hornMelodyB;
        hornMelodyC = other.hornMelodyC;
        hornMelodyCount = other.hornMelodyCount;
        hornMelodyIndex = other.hornMelodyIndex;
        hornSpecialMelody = other.hornSpecialMelody;
        hornSpecialGuardTicks = other.hornSpecialGuardTicks;
        hornMelodyPlayTicks = other.hornMelodyPlayTicks;
        hornLastMelodyId = other.hornLastMelodyId;
        hornLastMelodyEnhanceTicks = other.hornLastMelodyEnhanceTicks;
        hornAttackSmallTicks = other.hornAttackSmallTicks;
        hornAttackLargeTicks = other.hornAttackLargeTicks;
        hornDefenseLargeTicks = other.hornDefenseLargeTicks;
        hornMelodyHitTicks = other.hornMelodyHitTicks;
        hornStaminaBoostTicks = other.hornStaminaBoostTicks;
        hornAffinityTicks = other.hornAffinityTicks;
        lanceGuardActive = other.lanceGuardActive;
        lancePerfectGuardTicks = other.lancePerfectGuardTicks;
        lancePowerGuard = other.lancePowerGuard;
        gunlanceShells = other.gunlanceShells;
        gunlanceMaxShells = other.gunlanceMaxShells;
        gunlanceCooldown = other.gunlanceCooldown;
        gunlanceHasStake = other.gunlanceHasStake;
        gunlanceWyvernfireCooldown = other.gunlanceWyvernfireCooldown;
        gunlanceWyvernFireGauge = other.gunlanceWyvernFireGauge;
        switchAxeSwordMode = other.switchAxeSwordMode;
        switchAxeAmpGauge = other.switchAxeAmpGauge;
        switchAxeFrcCooldown = other.switchAxeFrcCooldown;
        switchAxeSwitchGauge = other.switchAxeSwitchGauge;
        switchAxePowerAxe = other.switchAxePowerAxe;
        switchAxePowerAxeTicks = other.switchAxePowerAxeTicks;
        switchAxeAmped = other.switchAxeAmped;
        switchAxeAmpedTicks = other.switchAxeAmpedTicks;
        switchAxeComboIndex = other.switchAxeComboIndex;
        switchAxeComboTick = other.switchAxeComboTick;
        switchAxeWildSwingCount = other.switchAxeWildSwingCount;
        switchAxeCounterTicks = other.switchAxeCounterTicks;
        chargeBladeSwordMode = other.chargeBladeSwordMode;
        chargeBladePhials = other.chargeBladePhials;
        chargeBladeCharge = other.chargeBladeCharge;
        cbShieldCharged = other.cbShieldCharged;
        cbShieldChargeTicks = other.cbShieldChargeTicks;
        cbSwordBoosted = other.cbSwordBoosted;
        cbSwordBoostTicks = other.cbSwordBoostTicks;
        cbPowerAxe = other.cbPowerAxe;
        cbPowerAxeTicks = other.cbPowerAxeTicks;
        cbComboIndex = other.cbComboIndex;
        cbComboTick = other.cbComboTick;
        cbGuardPointTicks = other.cbGuardPointTicks;
        cbDischargeStage = other.cbDischargeStage;
        insectRed = other.insectRed;
        insectWhite = other.insectWhite;
        insectOrange = other.insectOrange;
        insectExtractTicks = other.insectExtractTicks;
        insectTripleFinisherStage = other.insectTripleFinisherStage;
        insectTripleFinisherTicks = other.insectTripleFinisherTicks;
        insectAerialTicks = other.insectAerialTicks;
        insectAerialBounceLevel = other.insectAerialBounceLevel;
        insectCharging = other.insectCharging;
        insectChargeTicks = other.insectChargeTicks;
        insectComboIndex = other.insectComboIndex;
        insectComboTick = other.insectComboTick;
        insectWhiteJumpBoostCooldown = other.insectWhiteJumpBoostCooldown;
        kinsectPowderType = other.kinsectPowderType;
        kinsectMarkedTargetId = other.kinsectMarkedTargetId;
        kinsectMarkedTicks = other.kinsectMarkedTicks;
        tonfaShortMode = other.tonfaShortMode;
        tonfaComboGauge = other.tonfaComboGauge;
        tonfaFlyingTicks = other.tonfaFlyingTicks;
        tonfaDoubleJumped = other.tonfaDoubleJumped;
        tonfaComboIndex = other.tonfaComboIndex;
        tonfaComboTick = other.tonfaComboTick;
        tonfaAirActionCount = other.tonfaAirActionCount;
        tonfaLastHitTick = other.tonfaLastHitTick;
        tonfaBufferedWeaponInputCount = other.tonfaBufferedWeaponInputCount;
        magnetSpikeImpactMode = other.magnetSpikeImpactMode;
        magnetGauge = other.magnetGauge;
        magnetTargetId = other.magnetTargetId;
        magnetTargetTicks = other.magnetTargetTicks;
        magnetZipAnimTicks = other.magnetZipAnimTicks;
        magnetZipCooldownTicks = other.magnetZipCooldownTicks;
        magnetCutComboIndex = other.magnetCutComboIndex;
        magnetCutComboTick = other.magnetCutComboTick;
        magnetImpactComboIndex = other.magnetImpactComboIndex;
        magnetImpactComboTick = other.magnetImpactComboTick;
        accelFuel = other.accelFuel;
        accelDashTicks = other.accelDashTicks;
        accelParryTicks = other.accelParryTicks;
        bowCharge = other.bowCharge;
        bowCoating = other.bowCoating;
        chargingAttack = other.chargingAttack;
        chargeAttackTicks = other.chargeAttackTicks;
        stamina = other.stamina;
        maxStamina = other.maxStamina;
        staminaRecoveryDelay = other.staminaRecoveryDelay;
        longSwordSpecialSheathe = other.longSwordSpecialSheathe;
        longSwordSheatheTicks = other.longSwordSheatheTicks;
        longSwordSpiritComboIndex = other.longSwordSpiritComboIndex;
        longSwordSpiritComboTick = other.longSwordSpiritComboTick;
        longSwordOverheadComboIndex = other.longSwordOverheadComboIndex;
        longSwordOverheadComboTick = other.longSwordOverheadComboTick;
        longSwordChargeReady = other.longSwordChargeReady;
        longSwordHelmBreakerDirX = other.longSwordHelmBreakerDirX;
        longSwordHelmBreakerDirY = other.longSwordHelmBreakerDirY;
        longSwordHelmBreakerDirZ = other.longSwordHelmBreakerDirZ;
        longSwordAltComboTicks = other.longSwordAltComboTicks;
        longSwordHelmBreakerFollowupTicks = other.longSwordHelmBreakerFollowupTicks;
        longSwordHelmBreakerFollowupStage = other.longSwordHelmBreakerFollowupStage;
        longSwordHelmBreakerSpiritLevel = other.longSwordHelmBreakerSpiritLevel;
        longSwordThrustLockTicks = other.longSwordThrustLockTicks;
        longSwordThrustComboIndex = other.longSwordThrustComboIndex;
        longSwordThrustComboTick = other.longSwordThrustComboTick;
        longSwordHelmBreakerCooldown = other.longSwordHelmBreakerCooldown;
        kinsectEntityId = other.kinsectEntityId;
        gunlanceComboIndex = other.gunlanceComboIndex;
        gunlanceComboTick = other.gunlanceComboTick;
        gunlanceCharging = other.gunlanceCharging;
        gunlanceChargeTicks = other.gunlanceChargeTicks;
        markDirty();
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("spiritGauge", spiritGauge);
        tag.putInt("spiritLevel", spiritLevel);
        tag.putInt("spiritLevelTicks", spiritLevelTicks);
        tag.putFloat("demonGauge", demonGauge);
        tag.putBoolean("demonMode", demonMode);
        tag.putBoolean("archDemon", archDemon);
        tag.putInt("dbComboIndex", dbComboIndex);
        tag.putInt("dbComboTick", dbComboTick);
        tag.putInt("dbDemonComboIndex", dbDemonComboIndex);
        tag.putInt("dbDemonComboTick", dbDemonComboTick);
        tag.putInt("dbBladeDanceLockTicks", dbBladeDanceLockTicks);
        tag.putInt("dbDemonBoostTicks", dbDemonBoostTicks);
        tag.putInt("hammerChargeLevel", hammerChargeLevel);
        tag.putInt("hammerChargeTicks", hammerChargeTicks);
        tag.putBoolean("hammerPowerCharge", hammerPowerCharge);
        tag.putInt("hammerComboIndex", hammerComboIndex);
        tag.putInt("hammerComboTick", hammerComboTick);
        tag.putInt("hammerBigBangStage", hammerBigBangStage);
        tag.putInt("hammerBigBangTick", hammerBigBangTick);
        tag.putInt("hornNoteA", hornNoteA);
        tag.putInt("hornNoteB", hornNoteB);
        tag.putInt("hornNoteC", hornNoteC);
        tag.putInt("hornNoteD", hornNoteD);
        tag.putInt("hornNoteE", hornNoteE);
        tag.putInt("hornNoteCount", hornNoteCount);
        tag.putInt("hornBuffTicks", hornBuffTicks);
        tag.putInt("hornMelodyA", hornMelodyA);
        tag.putInt("hornMelodyB", hornMelodyB);
        tag.putInt("hornMelodyC", hornMelodyC);
        tag.putInt("hornMelodyCount", hornMelodyCount);
        tag.putInt("hornMelodyIndex", hornMelodyIndex);
        tag.putInt("hornSpecialMelody", hornSpecialMelody);
        tag.putInt("hornSpecialGuardTicks", hornSpecialGuardTicks);
        tag.putInt("hornMelodyPlayTicks", hornMelodyPlayTicks);
        tag.putInt("hornLastMelodyId", hornLastMelodyId);
        tag.putInt("hornLastMelodyEnhanceTicks", hornLastMelodyEnhanceTicks);
        tag.putInt("hornAttackSmallTicks", hornAttackSmallTicks);
        tag.putInt("hornAttackLargeTicks", hornAttackLargeTicks);
        tag.putInt("hornDefenseLargeTicks", hornDefenseLargeTicks);
        tag.putInt("hornMelodyHitTicks", hornMelodyHitTicks);
        tag.putInt("hornStaminaBoostTicks", hornStaminaBoostTicks);
        tag.putInt("hornAffinityTicks", hornAffinityTicks);
        tag.putBoolean("lanceGuardActive", lanceGuardActive);
        tag.putInt("lancePerfectGuardTicks", lancePerfectGuardTicks);
        tag.putBoolean("lancePowerGuard", lancePowerGuard);
        tag.putInt("gunlanceShells", gunlanceShells);
        tag.putInt("gunlanceMaxShells", gunlanceMaxShells);
        tag.putInt("gunlanceCooldown", gunlanceCooldown);
        tag.putBoolean("gunlanceHasStake", gunlanceHasStake);
        tag.putInt("gunlanceWyvernfireCooldown", gunlanceWyvernfireCooldown);
        tag.putFloat("gunlanceWyvernFireGauge", gunlanceWyvernFireGauge);
        tag.putBoolean("switchAxeSwordMode", switchAxeSwordMode);
        tag.putFloat("switchAxeAmpGauge", switchAxeAmpGauge);
        tag.putInt("switchAxeFrcCooldown", switchAxeFrcCooldown);
        tag.putFloat("switchAxeSwitchGauge", switchAxeSwitchGauge);
        tag.putBoolean("switchAxePowerAxe", switchAxePowerAxe);
        tag.putInt("switchAxePowerAxeTicks", switchAxePowerAxeTicks);
        tag.putBoolean("switchAxeAmped", switchAxeAmped);
        tag.putInt("switchAxeAmpedTicks", switchAxeAmpedTicks);
        tag.putInt("switchAxeComboIndex", switchAxeComboIndex);
        tag.putInt("switchAxeComboTick", switchAxeComboTick);
        tag.putInt("switchAxeWildSwingCount", switchAxeWildSwingCount);
        tag.putInt("switchAxeCounterTicks", switchAxeCounterTicks);
        tag.putBoolean("chargeBladeSwordMode", chargeBladeSwordMode);
        tag.putInt("chargeBladePhials", chargeBladePhials);
        tag.putInt("chargeBladeCharge", chargeBladeCharge);
        tag.putBoolean("cbShieldCharged", cbShieldCharged);
        tag.putInt("cbShieldChargeTicks", cbShieldChargeTicks);
        tag.putBoolean("cbSwordBoosted", cbSwordBoosted);
        tag.putInt("cbSwordBoostTicks", cbSwordBoostTicks);
        tag.putBoolean("cbPowerAxe", cbPowerAxe);
        tag.putInt("cbPowerAxeTicks", cbPowerAxeTicks);
        tag.putInt("cbComboIndex", cbComboIndex);
        tag.putInt("cbComboTick", cbComboTick);
        tag.putInt("cbGuardPointTicks", cbGuardPointTicks);
        tag.putInt("cbDischargeStage", cbDischargeStage);
        tag.putBoolean("insectRed", insectRed);
        tag.putBoolean("insectWhite", insectWhite);
        tag.putBoolean("insectOrange", insectOrange);
        tag.putInt("insectExtractTicks", insectExtractTicks);
        tag.putInt("insectTripleFinisherStage", insectTripleFinisherStage);
        tag.putInt("insectTripleFinisherTicks", insectTripleFinisherTicks);
        tag.putInt("insectAerialTicks", insectAerialTicks);
        tag.putInt("insectAerialBounceLevel", insectAerialBounceLevel);
        tag.putBoolean("insectCharging", insectCharging);
        tag.putInt("insectChargeTicks", insectChargeTicks);
        tag.putInt("insectComboIndex", insectComboIndex);
        tag.putInt("insectComboTick", insectComboTick);
        tag.putInt("insectWhiteJumpBoostCooldown", insectWhiteJumpBoostCooldown);
        tag.putInt("kinsectPowderType", kinsectPowderType);
        tag.putInt("kinsectMarkedTargetId", kinsectMarkedTargetId);
        tag.putInt("kinsectMarkedTicks", kinsectMarkedTicks);
        tag.putBoolean("tonfaShortMode", tonfaShortMode);
        tag.putFloat("tonfaComboGauge", tonfaComboGauge);
        tag.putInt("tonfaFlyingTicks", tonfaFlyingTicks);
        tag.putBoolean("tonfaDoubleJumped", tonfaDoubleJumped);
        tag.putInt("tonfaComboIndex", tonfaComboIndex);
        tag.putInt("tonfaComboTick", tonfaComboTick);
        tag.putInt("tonfaAirActionCount", tonfaAirActionCount);
        tag.putInt("tonfaLastHitTick", tonfaLastHitTick);
        tag.putInt("tonfaBufferedWeaponInputCount", tonfaBufferedWeaponInputCount);
        tag.putBoolean("magnetSpikeImpactMode", magnetSpikeImpactMode);
        tag.putFloat("magnetGauge", magnetGauge);
        tag.putInt("magnetTargetId", magnetTargetId);
        tag.putInt("magnetTargetTicks", magnetTargetTicks);
        tag.putInt("magnetZipAnimTicks", magnetZipAnimTicks);
        tag.putInt("magnetZipCooldownTicks", magnetZipCooldownTicks);
        tag.putInt("magnetCutComboIndex", magnetCutComboIndex);
        tag.putInt("magnetCutComboTick", magnetCutComboTick);
        tag.putInt("magnetImpactComboIndex", magnetImpactComboIndex);
        tag.putInt("magnetImpactComboTick", magnetImpactComboTick);
        tag.putInt("accelFuel", accelFuel);
        tag.putInt("accelDashTicks", accelDashTicks);
        tag.putInt("accelParryTicks", accelParryTicks);
        tag.putFloat("bowCharge", bowCharge);
        tag.putInt("bowCoating", bowCoating);
        tag.putInt("bowgunMode", bowgunMode);
        tag.putFloat("bowgunGauge", bowgunGauge);
        tag.putInt("bowgunRecoilTimer", bowgunRecoilTimer);
        tag.putInt("bowgunReloadTimer", bowgunReloadTimer);
        tag.putString("bowgunCurrentAmmo", bowgunCurrentAmmo);
        tag.putInt("bowgunLastAction", bowgunLastAction);
        tag.putInt("bowgunModeSwitchTicks", bowgunModeSwitchTicks);
        tag.putBoolean("bowgunAiming", bowgunAiming);
        tag.putBoolean("bowgunFiring", bowgunFiring);
        tag.putInt("bowgunSustainedHits", bowgunSustainedHits);
        tag.putBoolean("bowgunGuarding", bowgunGuarding);
        tag.putInt("bowgunGuardTicks", bowgunGuardTicks);
        tag.putBoolean("bowgunAutoGuard", bowgunAutoGuard);
        tag.putInt("bowgunSpecialAmmoTimer", bowgunSpecialAmmoTimer);
        tag.putInt("bowgunWeight", bowgunWeight);
        tag.putBoolean("chargingAttack", chargingAttack);
        tag.putInt("chargeAttackTicks", chargeAttackTicks);
        tag.putFloat("stamina", stamina);
        tag.putFloat("maxStamina", maxStamina);
        tag.putInt("staminaRecoveryDelay", staminaRecoveryDelay);
        tag.putBoolean("longSwordSpecialSheathe", longSwordSpecialSheathe);
        tag.putInt("longSwordSheatheTicks", longSwordSheatheTicks);
        tag.putInt("longSwordSpiritComboIndex", longSwordSpiritComboIndex);
        tag.putInt("longSwordSpiritComboTick", longSwordSpiritComboTick);
        tag.putInt("longSwordOverheadComboIndex", longSwordOverheadComboIndex);
        tag.putInt("longSwordOverheadComboTick", longSwordOverheadComboTick);
        tag.putBoolean("longSwordChargeReady", longSwordChargeReady);
        tag.putInt("longSwordFadeSlashTicks", longSwordFadeSlashTicks);
        tag.putFloat("longSwordHelmBreakerDirX", longSwordHelmBreakerDirX);
        tag.putFloat("longSwordHelmBreakerDirY", longSwordHelmBreakerDirY);
        tag.putFloat("longSwordHelmBreakerDirZ", longSwordHelmBreakerDirZ);
        tag.putInt("longSwordAltComboTicks", longSwordAltComboTicks);
        tag.putInt("longSwordHelmBreakerFollowupTicks", longSwordHelmBreakerFollowupTicks);
        tag.putInt("longSwordHelmBreakerFollowupStage", longSwordHelmBreakerFollowupStage);
        tag.putInt("longSwordHelmBreakerSpiritLevel", longSwordHelmBreakerSpiritLevel);
        tag.putInt("longSwordThrustLockTicks", longSwordThrustLockTicks);
        tag.putInt("longSwordThrustComboIndex", longSwordThrustComboIndex);
        tag.putInt("longSwordThrustComboTick", longSwordThrustComboTick);
        tag.putInt("kinsectEntityId", kinsectEntityId);
        tag.putInt("gunlanceComboIndex", gunlanceComboIndex);
        tag.putInt("gunlanceComboTick", gunlanceComboTick);
        tag.putBoolean("gunlanceCharging", gunlanceCharging);
        tag.putInt("gunlanceChargeTicks", gunlanceChargeTicks);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        spiritGauge = tag.getFloat("spiritGauge");
        spiritLevel = tag.getInt("spiritLevel");
        spiritLevelTicks = tag.getInt("spiritLevelTicks");
        demonGauge = tag.getFloat("demonGauge");
        demonMode = tag.getBoolean("demonMode");
        archDemon = tag.getBoolean("archDemon");
        dbComboIndex = tag.getInt("dbComboIndex");
        dbComboTick = tag.getInt("dbComboTick");
        dbDemonComboIndex = tag.getInt("dbDemonComboIndex");
        dbDemonComboTick = tag.getInt("dbDemonComboTick");
        dbBladeDanceLockTicks = tag.getInt("dbBladeDanceLockTicks");
        dbDemonBoostTicks = tag.getInt("dbDemonBoostTicks");
        hammerChargeLevel = tag.getInt("hammerChargeLevel");
        hammerChargeTicks = tag.getInt("hammerChargeTicks");
        hammerPowerCharge = tag.getBoolean("hammerPowerCharge");
        hammerComboIndex = tag.getInt("hammerComboIndex");
        hammerComboTick = tag.getInt("hammerComboTick");
        hammerBigBangStage = tag.getInt("hammerBigBangStage");
        hammerBigBangTick = tag.getInt("hammerBigBangTick");
        hornNoteA = tag.getInt("hornNoteA");
        hornNoteB = tag.getInt("hornNoteB");
        hornNoteC = tag.getInt("hornNoteC");
        hornNoteD = tag.getInt("hornNoteD");
        hornNoteE = tag.getInt("hornNoteE");
        hornNoteCount = tag.getInt("hornNoteCount");
        hornBuffTicks = tag.getInt("hornBuffTicks");
        hornMelodyA = tag.getInt("hornMelodyA");
        hornMelodyB = tag.getInt("hornMelodyB");
        hornMelodyC = tag.getInt("hornMelodyC");
        hornMelodyCount = tag.getInt("hornMelodyCount");
        hornMelodyIndex = tag.getInt("hornMelodyIndex");
        hornSpecialMelody = tag.getInt("hornSpecialMelody");
        hornSpecialGuardTicks = tag.getInt("hornSpecialGuardTicks");
        hornMelodyPlayTicks = tag.getInt("hornMelodyPlayTicks");
        hornLastMelodyId = tag.getInt("hornLastMelodyId");
        hornLastMelodyEnhanceTicks = tag.getInt("hornLastMelodyEnhanceTicks");
        hornAttackSmallTicks = tag.getInt("hornAttackSmallTicks");
        hornAttackLargeTicks = tag.getInt("hornAttackLargeTicks");
        hornDefenseLargeTicks = tag.getInt("hornDefenseLargeTicks");
        hornMelodyHitTicks = tag.getInt("hornMelodyHitTicks");
        hornStaminaBoostTicks = tag.getInt("hornStaminaBoostTicks");
        hornAffinityTicks = tag.getInt("hornAffinityTicks");
        lanceGuardActive = tag.getBoolean("lanceGuardActive");
        lancePerfectGuardTicks = tag.getInt("lancePerfectGuardTicks");
        lancePowerGuard = tag.getBoolean("lancePowerGuard");
        gunlanceShells = tag.getInt("gunlanceShells");
        gunlanceMaxShells = tag.getInt("gunlanceMaxShells");
        gunlanceCooldown = tag.getInt("gunlanceCooldown");
        gunlanceHasStake = tag.getBoolean("gunlanceHasStake");
        gunlanceWyvernFireGauge = tag.getFloat("gunlanceWyvernFireGauge");
        gunlanceWyvernfireCooldown = tag.getInt("gunlanceWyvernfireCooldown");
        switchAxeSwordMode = tag.getBoolean("switchAxeSwordMode");
        switchAxeAmpGauge = tag.getFloat("switchAxeAmpGauge");
        switchAxeFrcCooldown = tag.getInt("switchAxeFrcCooldown");
        switchAxeSwitchGauge = tag.contains("switchAxeSwitchGauge") ? tag.getFloat("switchAxeSwitchGauge") : 100.0f;
        switchAxePowerAxe = tag.getBoolean("switchAxePowerAxe");
        switchAxePowerAxeTicks = tag.getInt("switchAxePowerAxeTicks");
        switchAxeAmped = tag.getBoolean("switchAxeAmped");
        switchAxeAmpedTicks = tag.getInt("switchAxeAmpedTicks");
        switchAxeComboIndex = tag.getInt("switchAxeComboIndex");
        switchAxeComboTick = tag.getInt("switchAxeComboTick");
        switchAxeWildSwingCount = tag.getInt("switchAxeWildSwingCount");
        switchAxeCounterTicks = tag.getInt("switchAxeCounterTicks");
        chargeBladeSwordMode = tag.getBoolean("chargeBladeSwordMode");
        chargeBladePhials = tag.getInt("chargeBladePhials");
        chargeBladeCharge = tag.getInt("chargeBladeCharge");
        cbShieldCharged = tag.getBoolean("cbShieldCharged");
        cbShieldChargeTicks = tag.contains("cbShieldChargeTicks") ? tag.getInt("cbShieldChargeTicks") : 0;
        cbSwordBoosted = tag.getBoolean("cbSwordBoosted");
        cbSwordBoostTicks = tag.contains("cbSwordBoostTicks") ? tag.getInt("cbSwordBoostTicks") : 0;
        cbPowerAxe = tag.getBoolean("cbPowerAxe");
        cbPowerAxeTicks = tag.contains("cbPowerAxeTicks") ? tag.getInt("cbPowerAxeTicks") : 0;
        cbComboIndex = tag.contains("cbComboIndex") ? tag.getInt("cbComboIndex") : 0;
        cbComboTick = tag.contains("cbComboTick") ? tag.getInt("cbComboTick") : 0;
        cbGuardPointTicks = tag.contains("cbGuardPointTicks") ? tag.getInt("cbGuardPointTicks") : 0;
        cbDischargeStage = tag.contains("cbDischargeStage") ? tag.getInt("cbDischargeStage") : 0;
        insectRed = tag.getBoolean("insectRed");
        insectWhite = tag.getBoolean("insectWhite");
        insectOrange = tag.getBoolean("insectOrange");
        insectExtractTicks = tag.getInt("insectExtractTicks");
        insectTripleFinisherStage = tag.getInt("insectTripleFinisherStage");
        insectTripleFinisherTicks = tag.getInt("insectTripleFinisherTicks");
        insectAerialTicks = tag.getInt("insectAerialTicks");
        insectAerialBounceLevel = tag.contains("insectAerialBounceLevel", Tag.TAG_INT) ? tag.getInt("insectAerialBounceLevel") : 0;
        insectCharging = tag.getBoolean("insectCharging");
        insectChargeTicks = tag.contains("insectChargeTicks", Tag.TAG_INT) ? tag.getInt("insectChargeTicks") : 0;
        insectComboIndex = tag.contains("insectComboIndex", Tag.TAG_INT) ? tag.getInt("insectComboIndex") : 0;
        insectComboTick = tag.contains("insectComboTick", Tag.TAG_INT) ? tag.getInt("insectComboTick") : 0;
        insectWhiteJumpBoostCooldown = tag.contains("insectWhiteJumpBoostCooldown", Tag.TAG_INT) ? tag.getInt("insectWhiteJumpBoostCooldown") : 0;
        kinsectPowderType = tag.contains("kinsectPowderType", Tag.TAG_INT) ? tag.getInt("kinsectPowderType") : 0;
        kinsectMarkedTargetId = tag.contains("kinsectMarkedTargetId", Tag.TAG_INT) ? tag.getInt("kinsectMarkedTargetId") : -1;
        kinsectMarkedTicks = tag.contains("kinsectMarkedTicks", Tag.TAG_INT) ? tag.getInt("kinsectMarkedTicks") : 0;
        tonfaShortMode = tag.getBoolean("tonfaShortMode");
        tonfaComboGauge = tag.getFloat("tonfaComboGauge");
        tonfaFlyingTicks = tag.getInt("tonfaFlyingTicks");
        tonfaDoubleJumped = tag.getBoolean("tonfaDoubleJumped");
        tonfaComboIndex = tag.contains("tonfaComboIndex", Tag.TAG_INT) ? tag.getInt("tonfaComboIndex") : 0;
        tonfaComboTick = tag.contains("tonfaComboTick", Tag.TAG_INT) ? tag.getInt("tonfaComboTick") : 0;
        tonfaAirActionCount = tag.contains("tonfaAirActionCount", Tag.TAG_INT) ? tag.getInt("tonfaAirActionCount") : 0;
        tonfaLastHitTick = tag.contains("tonfaLastHitTick", Tag.TAG_INT) ? tag.getInt("tonfaLastHitTick") : 0;
        tonfaBufferedWeaponInputCount = tag.contains("tonfaBufferedWeaponInputCount", Tag.TAG_INT)
            ? Math.max(0, Math.min(2, tag.getInt("tonfaBufferedWeaponInputCount")))
            : (tag.getBoolean("tonfaBufferedWeaponInput") ? 1 : 0);
        magnetSpikeImpactMode = tag.getBoolean("magnetSpikeImpactMode");
        magnetGauge = tag.getFloat("magnetGauge");
        magnetTargetId = tag.getInt("magnetTargetId");
        magnetTargetTicks = tag.getInt("magnetTargetTicks");
        magnetZipAnimTicks = tag.getInt("magnetZipAnimTicks");
        magnetZipCooldownTicks = tag.getInt("magnetZipCooldownTicks");
        magnetCutComboIndex = tag.getInt("magnetCutComboIndex");
        magnetCutComboTick = tag.getInt("magnetCutComboTick");
        magnetImpactComboIndex = tag.getInt("magnetImpactComboIndex");
        magnetImpactComboTick = tag.getInt("magnetImpactComboTick");
        accelFuel = tag.getInt("accelFuel");
        accelDashTicks = tag.getInt("accelDashTicks");
        accelParryTicks = tag.contains("accelParryTicks", Tag.TAG_INT) ? tag.getInt("accelParryTicks") : 0;
        bowCharge = tag.getFloat("bowCharge");
        bowCoating = tag.getInt("bowCoating");
        bowgunMode = tag.contains("bowgunMode", Tag.TAG_INT) ? tag.getInt("bowgunMode") : 0;
        bowgunGauge = tag.contains("bowgunGauge", Tag.TAG_FLOAT) ? tag.getFloat("bowgunGauge") : 0.0f;
        bowgunRecoilTimer = tag.contains("bowgunRecoilTimer", Tag.TAG_INT) ? tag.getInt("bowgunRecoilTimer") : 0;
        bowgunReloadTimer = tag.contains("bowgunReloadTimer", Tag.TAG_INT) ? tag.getInt("bowgunReloadTimer") : 0;
        bowgunCurrentAmmo = tag.contains("bowgunCurrentAmmo") ? tag.getString("bowgunCurrentAmmo") : "";
        bowgunLastAction = tag.contains("bowgunLastAction", Tag.TAG_INT) ? tag.getInt("bowgunLastAction") : 0;
        bowgunModeSwitchTicks = tag.contains("bowgunModeSwitchTicks", Tag.TAG_INT) ? tag.getInt("bowgunModeSwitchTicks") : 0;
        bowgunAiming = tag.getBoolean("bowgunAiming");
        bowgunFiring = tag.getBoolean("bowgunFiring");
        bowgunSustainedHits = tag.contains("bowgunSustainedHits", Tag.TAG_INT) ? tag.getInt("bowgunSustainedHits") : 0;
        bowgunGuarding = tag.getBoolean("bowgunGuarding");
        bowgunGuardTicks = tag.contains("bowgunGuardTicks", Tag.TAG_INT) ? tag.getInt("bowgunGuardTicks") : 0;
        bowgunAutoGuard = tag.getBoolean("bowgunAutoGuard");
        bowgunSpecialAmmoTimer = tag.contains("bowgunSpecialAmmoTimer", Tag.TAG_INT) ? tag.getInt("bowgunSpecialAmmoTimer") : 0;
        bowgunWeight = tag.contains("bowgunWeight", Tag.TAG_INT) ? tag.getInt("bowgunWeight") : 0;
        chargingAttack = tag.getBoolean("chargingAttack");
        chargeAttackTicks = tag.getInt("chargeAttackTicks");
        float loadedMaxStamina = tag.contains("maxStamina", Tag.TAG_FLOAT) ? tag.getFloat("maxStamina") : 100.0f;
        maxStamina = loadedMaxStamina <= 0.0f ? 100.0f : loadedMaxStamina;
        stamina = tag.contains("stamina", Tag.TAG_FLOAT) ? tag.getFloat("stamina") : maxStamina;
        staminaRecoveryDelay = tag.getInt("staminaRecoveryDelay");
        longSwordSpecialSheathe = tag.getBoolean("longSwordSpecialSheathe");
        longSwordSheatheTicks = tag.getInt("longSwordSheatheTicks");
        longSwordSpiritComboIndex = tag.getInt("longSwordSpiritComboIndex");
        longSwordSpiritComboTick = tag.getInt("longSwordSpiritComboTick");
        longSwordOverheadComboIndex = tag.getInt("longSwordOverheadComboIndex");
        longSwordOverheadComboTick = tag.getInt("longSwordOverheadComboTick");
        longSwordChargeReady = tag.getBoolean("longSwordChargeReady");
        longSwordFadeSlashTicks = tag.getInt("longSwordFadeSlashTicks");
        longSwordHelmBreakerDirX = tag.getFloat("longSwordHelmBreakerDirX");
        longSwordHelmBreakerDirY = tag.getFloat("longSwordHelmBreakerDirY");
        longSwordHelmBreakerDirZ = tag.getFloat("longSwordHelmBreakerDirZ");
        longSwordAltComboTicks = tag.getInt("longSwordAltComboTicks");
        longSwordHelmBreakerFollowupTicks = tag.getInt("longSwordHelmBreakerFollowupTicks");
        longSwordHelmBreakerFollowupStage = tag.getInt("longSwordHelmBreakerFollowupStage");
        longSwordHelmBreakerSpiritLevel = tag.getInt("longSwordHelmBreakerSpiritLevel");
        longSwordThrustLockTicks = tag.getInt("longSwordThrustLockTicks");
        longSwordThrustComboIndex = tag.getInt("longSwordThrustComboIndex");
        longSwordThrustComboTick = tag.getInt("longSwordThrustComboTick");
        kinsectEntityId = tag.contains("kinsectEntityId", Tag.TAG_INT) ? tag.getInt("kinsectEntityId") : -1;
        gunlanceComboIndex = tag.getInt("gunlanceComboIndex");
        gunlanceComboTick = tag.getInt("gunlanceComboTick");
        gunlanceCharging = tag.getBoolean("gunlanceCharging");
        gunlanceChargeTicks = tag.getInt("gunlanceChargeTicks");
    }

    public float getStamina() {
        return stamina;
    }

    public void setStamina(float stamina) {
        float clamped = Math.max(0.0f, Math.min(maxStamina, stamina));
        if (this.stamina != clamped) {
            this.stamina = clamped;
            markDirty();
        }
    }

    public void addStamina(float delta) {
        setStamina(stamina + delta);
    }

    public float getMaxStamina() {
        return maxStamina;
    }

    public void setMaxStamina(float maxStamina) {
        if (this.maxStamina != maxStamina) {
            this.maxStamina = maxStamina;
            markDirty();
        }
    }

    public int getStaminaRecoveryDelay() {
        return staminaRecoveryDelay;
    }

    public void setStaminaRecoveryDelay(int staminaRecoveryDelay) {
        if (this.staminaRecoveryDelay != staminaRecoveryDelay) {
            this.staminaRecoveryDelay = staminaRecoveryDelay;
            markDirty();
        }
    }

    public boolean isLongSwordSpecialSheathe() {
        return longSwordSpecialSheathe;
    }

    public void setLongSwordSpecialSheathe(boolean longSwordSpecialSheathe) {
        if (this.longSwordSpecialSheathe != longSwordSpecialSheathe) {
            this.longSwordSpecialSheathe = longSwordSpecialSheathe;
            markDirty();
        }
    }

    public int getLongSwordSheatheTicks() {
        return longSwordSheatheTicks;
    }

    public void setLongSwordSheatheTicks(int longSwordSheatheTicks) {
        if (this.longSwordSheatheTicks != longSwordSheatheTicks) {
            this.longSwordSheatheTicks = longSwordSheatheTicks;
            markDirty();
        }
    }

    public int getLongSwordSpiritComboIndex() {
        return longSwordSpiritComboIndex;
    }

    public void setLongSwordSpiritComboIndex(int longSwordSpiritComboIndex) {
        if (this.longSwordSpiritComboIndex != longSwordSpiritComboIndex) {
            this.longSwordSpiritComboIndex = longSwordSpiritComboIndex;
            markDirty();
        }
    }

    public int getLongSwordSpiritComboTick() {
        return longSwordSpiritComboTick;
    }

    public void setLongSwordSpiritComboTick(int longSwordSpiritComboTick) {
        if (this.longSwordSpiritComboTick != longSwordSpiritComboTick) {
            this.longSwordSpiritComboTick = longSwordSpiritComboTick;
            markDirty();
        }
    }

    public int getLongSwordOverheadComboIndex() {
        return longSwordOverheadComboIndex;
    }

    public void setLongSwordOverheadComboIndex(int longSwordOverheadComboIndex) {
        if (this.longSwordOverheadComboIndex != longSwordOverheadComboIndex) {
            this.longSwordOverheadComboIndex = longSwordOverheadComboIndex;
            markDirty();
        }
    }

    public int getLongSwordOverheadComboTick() {
        return longSwordOverheadComboTick;
    }

    public void setLongSwordOverheadComboTick(int longSwordOverheadComboTick) {
        if (this.longSwordOverheadComboTick != longSwordOverheadComboTick) {
            this.longSwordOverheadComboTick = longSwordOverheadComboTick;
            markDirty();
        }
    }

    public boolean isLongSwordChargeReady() {
        return longSwordChargeReady;
    }

    public void setLongSwordChargeReady(boolean longSwordChargeReady) {
        if (this.longSwordChargeReady != longSwordChargeReady) {
            this.longSwordChargeReady = longSwordChargeReady;
            markDirty();
        }
    }

    public int getLongSwordFadeSlashTicks() {
        return longSwordFadeSlashTicks;
    }

    public void setLongSwordFadeSlashTicks(int ticks) {
        if (this.longSwordFadeSlashTicks != ticks) {
            this.longSwordFadeSlashTicks = ticks;
            markDirty();
        }
    }

    public void setLongSwordHelmBreakerDir(net.minecraft.world.phys.Vec3 dir) {
        float x = dir == null ? 0.0f : (float) dir.x;
        float y = dir == null ? 0.0f : (float) dir.y;
        float z = dir == null ? 0.0f : (float) dir.z;
        if (this.longSwordHelmBreakerDirX != x || this.longSwordHelmBreakerDirY != y || this.longSwordHelmBreakerDirZ != z) {
            this.longSwordHelmBreakerDirX = x;
            this.longSwordHelmBreakerDirY = y;
            this.longSwordHelmBreakerDirZ = z;
            markDirty();
        }
    }

    public net.minecraft.world.phys.Vec3 getLongSwordHelmBreakerDir() {
        return new net.minecraft.world.phys.Vec3(longSwordHelmBreakerDirX, longSwordHelmBreakerDirY, longSwordHelmBreakerDirZ);
    }

    public int getLongSwordAltComboTicks() {
        return longSwordAltComboTicks;
    }

    public void setLongSwordAltComboTicks(int longSwordAltComboTicks) {
        int clamped = Math.max(0, longSwordAltComboTicks);
        if (this.longSwordAltComboTicks != clamped) {
            this.longSwordAltComboTicks = clamped;
            markDirty();
        }
    }

    public int getLongSwordHelmBreakerFollowupTicks() {
        return longSwordHelmBreakerFollowupTicks;
    }

    public void setLongSwordHelmBreakerFollowupTicks(int longSwordHelmBreakerFollowupTicks) {
        int clamped = Math.max(0, longSwordHelmBreakerFollowupTicks);
        if (this.longSwordHelmBreakerFollowupTicks != clamped) {
            this.longSwordHelmBreakerFollowupTicks = clamped;
            markDirty();
        }
    }

    public int getLongSwordHelmBreakerFollowupStage() {
        return longSwordHelmBreakerFollowupStage;
    }

    public void setLongSwordHelmBreakerFollowupStage(int longSwordHelmBreakerFollowupStage) {
        int clamped = Math.max(0, longSwordHelmBreakerFollowupStage);
        if (this.longSwordHelmBreakerFollowupStage != clamped) {
            this.longSwordHelmBreakerFollowupStage = clamped;
            markDirty();
        }
    }

    public int getLongSwordHelmBreakerSpiritLevel() {
        return longSwordHelmBreakerSpiritLevel;
    }

    public void setLongSwordHelmBreakerSpiritLevel(int longSwordHelmBreakerSpiritLevel) {
        int clamped = Math.max(0, Math.min(3, longSwordHelmBreakerSpiritLevel));
        if (this.longSwordHelmBreakerSpiritLevel != clamped) {
            this.longSwordHelmBreakerSpiritLevel = clamped;
            markDirty();
        }
    }

    public int getLongSwordThrustLockTicks() {
        return longSwordThrustLockTicks;
    }

    public void setLongSwordThrustLockTicks(int longSwordThrustLockTicks) {
        int clamped = Math.max(0, longSwordThrustLockTicks);
        if (this.longSwordThrustLockTicks != clamped) {
            this.longSwordThrustLockTicks = clamped;
            markDirty();
        }
    }

    public int getLongSwordThrustComboIndex() {
        return longSwordThrustComboIndex;
    }

    public void setLongSwordThrustComboIndex(int longSwordThrustComboIndex) {
        if (this.longSwordThrustComboIndex != longSwordThrustComboIndex) {
            this.longSwordThrustComboIndex = longSwordThrustComboIndex;
            markDirty();
        }
    }

    public int getLongSwordThrustComboTick() {
        return longSwordThrustComboTick;
    }

    public void setLongSwordThrustComboTick(int longSwordThrustComboTick) {
        if (this.longSwordThrustComboTick != longSwordThrustComboTick) {
            this.longSwordThrustComboTick = longSwordThrustComboTick;
            markDirty();
        }
    }

    public int getLongSwordHelmBreakerCooldown() {
        return longSwordHelmBreakerCooldown;
    }

    public void setLongSwordHelmBreakerCooldown(int longSwordHelmBreakerCooldown) {
        int clamped = Math.max(0, longSwordHelmBreakerCooldown);
        if (this.longSwordHelmBreakerCooldown != clamped) {
            this.longSwordHelmBreakerCooldown = clamped;
            markDirty();
        }
    }

    public int getKinsectEntityId() {
        return kinsectEntityId;
    }

    public void setKinsectEntityId(int kinsectEntityId) {
        if (this.kinsectEntityId != kinsectEntityId) {
            this.kinsectEntityId = kinsectEntityId;
            markDirty();
        }
    }

    public int getKinsectPowderType() {
        return kinsectPowderType;
    }

    public void setKinsectPowderType(int kinsectPowderType) {
        if (this.kinsectPowderType != kinsectPowderType) {
            this.kinsectPowderType = kinsectPowderType;
            markDirty();
        }
    }

    public int getKinsectMarkedTargetId() {
        return kinsectMarkedTargetId;
    }

    public void setKinsectMarkedTargetId(int kinsectMarkedTargetId) {
        if (this.kinsectMarkedTargetId != kinsectMarkedTargetId) {
            this.kinsectMarkedTargetId = kinsectMarkedTargetId;
            markDirty();
        }
    }

    public int getKinsectMarkedTicks() {
        return kinsectMarkedTicks;
    }

    public void setKinsectMarkedTicks(int kinsectMarkedTicks) {
        if (this.kinsectMarkedTicks != kinsectMarkedTicks) {
            this.kinsectMarkedTicks = kinsectMarkedTicks;
            markDirty();
        }
    }

    public int getInsectWhiteJumpBoostCooldown() {
        return insectWhiteJumpBoostCooldown;
    }

    public void setInsectWhiteJumpBoostCooldown(int insectWhiteJumpBoostCooldown) {
        if (this.insectWhiteJumpBoostCooldown != insectWhiteJumpBoostCooldown) {
            this.insectWhiteJumpBoostCooldown = insectWhiteJumpBoostCooldown;
            markDirty();
        }
    }

    public int getGunlanceComboIndex() {
        return gunlanceComboIndex;
    }

    public void setGunlanceComboIndex(int gunlanceComboIndex) {
        if (this.gunlanceComboIndex != gunlanceComboIndex) {
            this.gunlanceComboIndex = gunlanceComboIndex;
            markDirty();
        }
    }

    public int getGunlanceComboTick() {
        return gunlanceComboTick;
    }

    public void setGunlanceComboTick(int gunlanceComboTick) {
        if (this.gunlanceComboTick != gunlanceComboTick) {
            this.gunlanceComboTick = gunlanceComboTick;
            markDirty();
        }
    }

    public boolean isGunlanceCharging() {
        return gunlanceCharging;
    }

    public void setGunlanceCharging(boolean gunlanceCharging) {
        if (this.gunlanceCharging != gunlanceCharging) {
            this.gunlanceCharging = gunlanceCharging;
            markDirty();
        }
    }

    public int getGunlanceChargeTicks() {
        return gunlanceChargeTicks;
    }

    public void setGunlanceChargeTicks(int gunlanceChargeTicks) {
        if (this.gunlanceChargeTicks != gunlanceChargeTicks) {
            this.gunlanceChargeTicks = gunlanceChargeTicks;
            markDirty();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  Bowgun getters/setters
    // ══════════════════════════════════════════════════════════════════

    public int getBowgunMode() { return bowgunMode; }
    public void setBowgunMode(int mode) { if (this.bowgunMode != mode) { this.bowgunMode = mode; markDirty(); } }

    public float getBowgunGauge() { return bowgunGauge; }
    public void setBowgunGauge(float gauge) {
        float clamped = Math.max(0.0f, Math.min(100.0f, gauge));
        if (this.bowgunGauge != clamped) { this.bowgunGauge = clamped; markDirty(); }
    }

    public int getBowgunRecoilTimer() { return bowgunRecoilTimer; }
    public void setBowgunRecoilTimer(int t) { if (this.bowgunRecoilTimer != t) { this.bowgunRecoilTimer = t; markDirty(); } }

    public int getBowgunReloadTimer() { return bowgunReloadTimer; }
    public void setBowgunReloadTimer(int t) { if (this.bowgunReloadTimer != t) { this.bowgunReloadTimer = t; markDirty(); } }

    public String getBowgunCurrentAmmo() { return bowgunCurrentAmmo; }
    public void setBowgunCurrentAmmo(String ammo) {
        if (ammo == null) ammo = "";
        if (!this.bowgunCurrentAmmo.equals(ammo)) { this.bowgunCurrentAmmo = ammo; markDirty(); }
    }

    public int getBowgunLastAction() { return bowgunLastAction; }
    public void setBowgunLastAction(int a) { if (this.bowgunLastAction != a) { this.bowgunLastAction = a; markDirty(); } }

    public int getBowgunModeSwitchTicks() { return bowgunModeSwitchTicks; }
    public void setBowgunModeSwitchTicks(int t) { if (this.bowgunModeSwitchTicks != t) { this.bowgunModeSwitchTicks = t; markDirty(); } }

    public boolean isBowgunAiming() { return bowgunAiming; }
    public void setBowgunAiming(boolean v) { if (this.bowgunAiming != v) { this.bowgunAiming = v; markDirty(); } }

    public boolean isBowgunFiring() { return bowgunFiring; }
    public void setBowgunFiring(boolean v) { if (this.bowgunFiring != v) { this.bowgunFiring = v; markDirty(); } }

    public int getBowgunSustainedHits() { return bowgunSustainedHits; }
    public void setBowgunSustainedHits(int h) { if (this.bowgunSustainedHits != h) { this.bowgunSustainedHits = h; markDirty(); } }

    public boolean isBowgunGuarding() { return bowgunGuarding; }
    public void setBowgunGuarding(boolean v) { if (this.bowgunGuarding != v) { this.bowgunGuarding = v; markDirty(); } }

    public int getBowgunGuardTicks() { return bowgunGuardTicks; }
    public void setBowgunGuardTicks(int t) { if (this.bowgunGuardTicks != t) { this.bowgunGuardTicks = t; markDirty(); } }

    public boolean isBowgunAutoGuard() { return bowgunAutoGuard; }
    public void setBowgunAutoGuard(boolean v) { if (this.bowgunAutoGuard != v) { this.bowgunAutoGuard = v; markDirty(); } }

    public int getBowgunSpecialAmmoTimer() { return bowgunSpecialAmmoTimer; }
    public void setBowgunSpecialAmmoTimer(int t) { if (this.bowgunSpecialAmmoTimer != t) { this.bowgunSpecialAmmoTimer = t; markDirty(); } }

    public int getBowgunWeight() { return bowgunWeight; }
    public void setBowgunWeight(int w) { if (this.bowgunWeight != w) { this.bowgunWeight = w; markDirty(); } }
}
