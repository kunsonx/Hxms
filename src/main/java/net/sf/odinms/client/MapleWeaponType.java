//武器类型
package net.sf.odinms.client;

public enum MapleWeaponType {

    NOT_A_WEAPON(0),//赤手空拳
    BOW(3.4),//弓
    CLAW(3.6),//拳套?
    DAGGER(4),//短剑
    CROSSBOW(3.6),//弩
    AXE1H(4.4),//单手斧头
    SWORD1H(4.0),//单手剑
    BLUNT1H(4.4),//单手钝器
    AXE2H(4.8),//双手斧头
    SWORD2H(4.6),//双手剑
    BLUNT2H(4.8),//双手钝器
    POLE_ARM(5.0),//长枪?
    SPEAR(5.0),//矛
    STAFF(3.6),//短杖
    WAND(3.6),//长杖
    KNUCKLE(4.8),//指节
    GUN(3.6),
    CANNON(6.0),
    双弩枪(3.8),
    能量剑(6.0),
    通用武器(3.8);//手炮
    private double damageMultiplier;

    private MapleWeaponType(double maxDamageMultiplier) {
        this.damageMultiplier = maxDamageMultiplier;
    }

    public double getMaxDamageMultiplier() {
        return damageMultiplier;
    }
};
