package org.example.item;

import net.minecraft.world.item.Item;
import org.example.common.combat.MHDamageType;
import org.example.common.config.MHWeaponsConfig;

public class KinsectItem extends Item {
    private final int maxExtracts;
    private final double speed;
    private final double range;
    private final float damage;
    private final MHDamageType damageType;
    private final String element;
    private final int powderType; // 0=None, 1=Blast, 2=Poison, 3=Paralysis, 4=Heal

    public KinsectItem(Properties properties, int maxExtracts, double speed, double range, float damage, MHDamageType damageType, String element) {
        this(properties, maxExtracts, speed, range, damage, damageType, element, 1);
    }

    public KinsectItem(Properties properties, int maxExtracts, double speed, double range, float damage, MHDamageType damageType, String element, int powderType) {
        super(properties);
        this.maxExtracts = maxExtracts;
        this.speed = speed;
        this.range = range;
        this.damage = damage;
        this.damageType = damageType;
        this.element = element;
        this.powderType = powderType;
    }

    public int getMaxExtracts() {
        return maxExtracts > 0 ? maxExtracts : MHWeaponsConfig.KINSECT_MAX_EXTRACTS.get();
    }

    public double getSpeed() {
        return speed > 0 ? speed : MHWeaponsConfig.KINSECT_SPEED.get();
    }

    public double getRange() {
        return range > 0 ? range : MHWeaponsConfig.KINSECT_RANGE.get();
    }

    public float getDamage() {
        return damage > 0 ? damage : MHWeaponsConfig.KINSECT_DAMAGE.get().floatValue();
    }

    public MHDamageType getDamageType() {
        return damageType != null ? damageType : MHWeaponsConfig.KINSECT_DAMAGE_TYPE.get();
    }

    public String getElement() {
        return element != null ? element : MHWeaponsConfig.KINSECT_ELEMENT.get();
    }

    public int getPowderType() {
        return powderType;
    }
}
