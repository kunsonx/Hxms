

package net.sf.odinms.client;

import net.sf.odinms.net.IntValueHolder;

public enum MapleStat implements IntValueHolder {

    SKIN(0x1),//1位数
    FACE(0x2),//4位数
    HAIR(0x4),//4位数
    LEVEL(0x10),//1位数  92..40
    JOB(0x20),//1位数  92..80
    STR(0x40),//2位数  92..100
    DEX(0x80),//2位数  92..200
    INT(0x100),//2位数  92..400
    LUK(0x200),//2位数  92..800
    HP(0x400),//4位数  92..1000
    MAXHP(0x800),//4位数  92..2000
    MP(0x1000),//4位数  92..4000
    MAXMP(0x2000),//4位数  92..8000
    AVAILABLEAP(0x4000),//2位数  92..10000
    AVAILABLESP(0x8000),//2位数  92..20000
    EXP(0x10000),//4位数  92..40000
    FAME(0x20000),//4位数  92..80000
    MESO(0x40000),//4位数  92..100000
    //EXPBOOK(0x200000),//兵法书 083取消枚举
    //PET(0x400000), //093取消枚举
    
    //int
    领袖气质  (0x100000),
    洞察力    (0x200000),
    意志      (0x400000),
    手技      (0x800000),
    感性      (0x1000000),
    魅力      (0x2000000),
    战斗经验值(0x8000000),
    //byte
    Pk等级    (0x10000000),
    //int
    保有BP    (0x20000000),
    ;
    private final int i;

    private MapleStat(int i) {
        this.i = i;
    }

    @Override
    public int getValue() {
        return i;
    }

    public static MapleStat getByValue(int value) {
        for (MapleStat stat : MapleStat.values()) {
            if (stat.getValue() == value) {
                return stat;
            }
        }
        return null;
    }

    public String getStatString() {
        MapleStat stat = this;
        if(stat == SKIN) {
            return "皮肤更新:";
        }else if(stat == FACE) {
            return "面部更新:";
        }else if(stat == HAIR) {
            return "头发更新:";
        }else if(stat == LEVEL) {
            return "等级更新:";
        }else if(stat == JOB) {
            return "职业更新:";
        }else if(stat == STR) {
            return "力量更新:";
        }else if(stat == DEX) {
            return "敏捷更新:";
        }else if(stat == INT) {
            return "智力更新:";
        }else if(stat == LUK) {
            return "运气更新:";
        }else if(stat == HP) {
            return "血量更新:";
        }else if(stat == MAXHP) {
            return "更新MAXHP:";
        }else if(stat == MP) {
            return "更新MP:";
        }else if(stat == MAXMP) {
            return "更新MAXMP:";
        }else if (stat == AVAILABLEAP) {
            return "更新能力值:";
        }else if (stat == AVAILABLESP) {
            return "更新技能点:";
        }else if(stat == EXP) {
            return "更新经验值:";
        }else if(stat == FAME) {
            return "更新人气:";
        }else if(stat == MESO) {
            return "更新金币:";
            /*
        }else if(stat == EXPBOOK) {
            return "更新兵法书:";
        }else if(stat == PET) {
            return "更新宠物:";
            */
        }else{
            return "更新值出错了!!";
        }
    }
}
