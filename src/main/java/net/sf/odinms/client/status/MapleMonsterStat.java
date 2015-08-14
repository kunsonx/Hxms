//怪物状态
package net.sf.odinms.client.status;

import java.io.Serializable;

public enum MapleMonsterStat implements Serializable {

    WATK(0x100000000L),
    WDEF(0x200000000L),
    MATK(0x400000000L),
    MDEF(0x800000000L),
    ACC(0x1000000000L),
    AVOID(0x2000000000L),
    SPEED(0x4000000000L),
    /**
     * 晕
     */
    STUN(8, 1),
    /**
     * 冰冻
     */
    FREEZE(8, 1),
    /**
     * 毒。
     */
    POISON(27, 1), //第一个完善后的怪物BUFF
    /*
     * 封条
     */
    SEAL(0x40000000000L),
    /*
     * 嘲笑
     */
    TAUNT(0x80000000000L),
    WEAPON_ATTACK_UP(0x100000000000L),
    WEAPON_DEFENSE_UP(0x200000000000L),
    MAGIC_ATTACK_UP(0x400000000000L),
    MAGIC_DEFENSE_UP(0x800000000000L),
    DOOM(0x1000000000000L),
    SHADOW_WEB(0x2000000000000L),
    WEAPON_IMMUNITY(0x4000000000000L),
    MAGIC_IMMUNITY(0x8000000000000L),
    忍者伏击(0x40000000000000L),
    抗压(0x2L),
    鬼刻符(0x4L),
    怪物炸弹(0x8L),
    魔击无效(0x10L),
    //战神之审判(0x800000000000000L),
    HYPNOTIZED(0x1000000000000000L);
    static final long serialVersionUID = 0L;
    private final long i;
    private int position;

    private MapleMonsterStat(long i) {
        this.i = i;
        this.position = -1;
    }

    private MapleMonsterStat(long i, int position) {
        this.i = i;
        this.position = position;
    }

    public long getValue() {
        return i;
    }

    public int getPosition() {
        return position;
    }

    public int getMarkValue() {
        return 1 << i;
    }
}