/*
 * 大巨变的新职业可以查看WZ的Skill.wz排序看到是从3000——3512
 * 3000是反抗者的新手—预备兵
 * 3200 3210 3211 3212也是反抗者的职业
 * 3300 3310 3311 3312也是反抗者的职业
 * 3500 3510 3511 3512也是反抗者的职业
 * 就以上这些反抗者,然后添加到下面的枚举中，名字可以自己随便取，只要后面的职业ID不错
 */
package net.sf.odinms.client;

public enum MapleJob {

    BEGINNER(0),//新手

    WARRIOR(100),//战士

    FIGHTER(110),//剑客
    CRUSADER(111),//勇士
    HERO(112),//英雄

    PAGE(120),
    WHITEKNIGHT(121),
    PALADIN(122),
    SPEARMAN(130),
    DRAGONKNIGHT(131),
    DARKKNIGHT(132),
    MAGICIAN(200),
    FP_WIZARD(210),
    FP_MAGE(211),
    FP_ARCHMAGE(212),
    IL_WIZARD(220),
    IL_MAGE(221),
    IL_ARCHMAGE(222),
    CLERIC(230),
    PRIEST(231),
    BISHOP(232),
    BOWMAN(300),
    HUNTER(310),
    RANGER(311),
    BOWMASTER(312),
    CROSSBOWMAN(320),
    SNIPER(321),
    CROSSBOWMASTER(322),
    THIEF(400),
    ASSASSIN(410),
    HERMIT(411),
    NIGHTLORD(412),//隐之侠士

    BANDIT(420),
    CHIEFBANDIT(421),
    SHADOWER(422),//盗之飞侠
    //双刀
    Dual_Blade_1(430),
    Dual_Blade_2(431),
    Dual_Blade_3(432),
    Dual_Blade_4(433),
    Dual_Blade_5(434),
    PIRATE(500),
    BRAWLER(510),
    MARAUDER(511),
    BUCCANEER(512),
    GUNSLINGER(520),
    OUTLAW(521),
    CORSAIR(522),
    龙之传人_1(508),
    龙之传人_2(570),
    龙之传人_3(571),
    龙之传人_4(572),
    /**
     * 炮手
     */
    GODGUNNER(501),
    GODGUNNER_2(530),
    GODGUNNER_3(531),
    GODGUNNER_4(532),
    GM(900),
    //骑士团
    KNIGHT(1000),
    /**
     * 魂骑士
     */
    GHOST_KNIGHT(1100),
    GHOST_KNIGHT_2(1110),
    GHOST_KNIGHT_3(1111),
    /**
     * 炎术士
     */
    FIRE_KNIGHT(1200),
    FIRE_KNIGHT_2(1210),
    FIRE_KNIGHT_3(1211),
    /*
     * 风灵使者
     */
    WIND_KNIGHT(1300),
    WIND_KNIGHT_2(1310),
    WIND_KNIGHT_3(1311),
    /**
     * 夜行者
     */
    NIGHT_KNIGHT(1400),
    NIGHT_KNIGHT_2(1410),
    NIGHT_KNIGHT_3(1411),
    /**
     * 奇袭者
     */
    THIEF_KNIGHT(1500),
    THIEF_KNIGHT_2(1510),
    THIEF_KNIGHT_3(1511),
    //战神
    Aran(2000),
    Aran_1(2100),
    Aran_2(2110),
    Aran_3(2111),
    Aran_4(2112),
    //添加龙神新职业
    /**
     * 小不点
     */
    Evan(2001, 0),
    /**
     * 龙神
     */
    Evan_1(2200, 1),
    Evan_2(2210, 2),
    Evan_3(2211, 3),
    Evan_4(2212, 4),
    Evan_5(2213, 5),
    Evan_6(2214, 6),
    Evan_7(2215, 7),
    Evan_8(2216, 8),
    Evan_9(2217, 9),
    Evan_10(2218, 10),
    /**
     * 双弩
     */
    精灵的基础(2002, 0),
    DoubleCrossbows(2300, 1),
    DoubleCrossbows_2(2310, 2),
    DoubleCrossbows_3(2311, 3),
    DoubleCrossbows_4(2312, 4),
    幻影(2003, 0),
    幻影_1(2400, 1),
    幻影_2(2410, 2),
    幻影_3(2411, 3),
    幻影_4(2412, 4),
    WuMingShaoNian(5000, 0),
    MiHaEr(5100, 1),
    MihaEr1(5110, 2),
    MihaEr2(5111, 3),
    MihaEr3(5112, 4),
    /**
     * 恶魔猎手
     */
    恶魔猎手的基础(3001, 0),
    DemonHunter(3100, 1),
    DemonHunter_2(3110, 2),//30
    DemonHunter_3(3111, 3),//70
    DemonHunter_4(3112, 4),//120
    DemonAvenger_1(3101, 1),//1012361 干净的脸
    DemonAvenger_2(3120, 2),
    DemonAvenger_3(3121, 3),
    DemonAvenger_4(3122, 4),
    fuck1(800),
    fuck2(9000),
    fuck3(9100),
    //添加反抗者职业,然后再去登陆程序中创建人物那添加即:CreateCharhandle这个类
    Resistance(3000, 0), //预备兵

    Battlemage_1(3200, 1), //幻灵斗师
    Battlemage_2(3210, 2),
    Battlemage_3(3211, 3),
    Battlemage_4(3212, 4),
    wildhunter_1(3300, 1),//弩豹游侠
    wildhunter_2(3310, 2),
    wildhunter_3(3311, 3),
    wildhunter_4(3312, 4),
    mechinic_1(3500, 1),//机械师
    mechinic_2(3510, 2),
    mechinic_3(3511, 3),
    mechinic_4(3512, 4),
    trailblazer(3002, 0),//尖兵
    trailblazer_1(3600, 0),
    trailblazer_2(3610, 0),
    trailblazer_3(3611, 0),
    trailblazer_4(3612, 0),
    //副职业
    采矿(9200),
    采药(9201),
    装备制作(9202),
    饰品制作(9203),
    炼金术(9204),
    LUMINOUSMAGE(2004, 0),
    LUMINOUSMAGE_1(2700, 1),
    LUMINOUSMAGE_2(2710, 2),
    LUMINOUSMAGE_3(2711, 3),
    LUMINOUSMAGE_4(2712, 4),
    KuangLong(6000, 0),
    KuangLong_1(6100, 1),
    KuangLong_2(6110, 2),
    KuangLong_3(6111, 3),
    KuangLong_4(6112, 4),
    MentTianShi(6001, 0),
    MentTianShi_1(6500, 1),
    MentTianShi_2(6510, 2),
    MentTianShi_3(6511, 3),
    MentTianShi_4(6512, 4),;
    final int jobid;
    final int maxspslots;

    /**
     * 狂龙职业系
     *
     * @return 是否是狂龙职业系
     */
    public boolean IsKuanglong() {
        return this == KuangLong_1 || this == KuangLong_2 || this == KuangLong_3 || this == KuangLong_4;
    }

    public boolean IsTrailblazer() {
        return this == trailblazer || this == trailblazer_1 || this == trailblazer_2 || this == trailblazer_3 || this == trailblazer_4;
    }

    /**
     * 恶魔复仇者系列
     *
     * @return
     */
    public boolean IsDemonAvenger() {
        return this == DemonAvenger_1 || this == DemonAvenger_2 || this == DemonAvenger_3 || this == DemonAvenger_4;
    }

    public boolean NoBeginner() {
        return (this != BEGINNER)
                && (this != KNIGHT)
                && (this != Evan)
                && (this != Aran)
                && (this != 精灵的基础)
                && (this != 恶魔猎手的基础)
                && (this != Resistance)
                && (this != 龙之传人_1)
                && (this != 幻影)
                && (this != LUMINOUSMAGE)
                && (this != PIRATE)
                && (this != KuangLong)
                && (this != trailblazer);//
    }

    public boolean 能否学习勇士的意志() {
        return (this == HERO)
                || (this == PALADIN)
                || (this == DARKKNIGHT)
                || (this == FP_ARCHMAGE)
                || (this == IL_ARCHMAGE)
                || (this == BISHOP)
                || (this == BOWMASTER)
                || (this == CROSSBOWMASTER)
                || (this == NIGHTLORD)
                || (this == SHADOWER)
                || (this == Dual_Blade_5)
                || (this == BUCCANEER)
                || (this == CORSAIR)
                || (this == GODGUNNER_4)
                || (this == GHOST_KNIGHT_3)
                || (this == FIRE_KNIGHT_3)
                || (this == WIND_KNIGHT_3)
                || (this == NIGHT_KNIGHT_3)
                || (this == THIEF_KNIGHT_3)
                || (this == Aran_4)
                || (this == Evan_10)
                || (this == DemonHunter_4)
                || (this == Battlemage_4)
                || (this == wildhunter_4)
                || (this == mechinic_4);
    }

    /**
     * 双弩精灵职业系。
     *
     * @return
     */
    public boolean IsDoubleCrossbows() {
        return (this == DoubleCrossbows)
                || (this == DoubleCrossbows_2)
                || (this == DoubleCrossbows_3)
                || (this == DoubleCrossbows_4)
                || (this == 精灵的基础);
    }

    /**
     * 恶魔猎手职业系。
     *
     * @return
     */
    public boolean IsDemonHunter() {
        return (this == DemonHunter)
                || (this == DemonHunter_2)
                || (this == DemonHunter_3)
                || (this == DemonHunter_4)
                || (this == 恶魔猎手的基础)
                || (this == DemonAvenger_1)
                || (this == DemonAvenger_2)
                || (this == DemonAvenger_3)
                || (this == DemonAvenger_4);
    }

    private MapleJob(int id) {
        this(id, -1);
    }

    private MapleJob(int id, int maxslots) {
        jobid = id;
        maxspslots = maxslots;
    }

    /**
     * 是否是多组SP职业.
     *
     * @return
     */
    public boolean IsExtendSPJob() {
        return maxspslots > -1;
    }

    /**
     * 最大SP组.
     *
     * @return
     */
    public int GetMaxSpSlots() {
        return maxspslots;
    }

    public int getId() {
        return jobid;
    }

    public static MapleJob getById(int id) {
        for (MapleJob l : MapleJob.values()) {
            if (l.getId() == id) {
                return l;
            }
        }
        return null;
    }

    public static MapleJob getBy5ByteEncoding(int encoded) {
        switch (encoded) {
            case 2:
                return WARRIOR;
            case 4:
                return MAGICIAN;
            case 8:
                return BOWMAN;
            case 16:
                return THIEF;
            case 32:
                return PIRATE;
            default:
                return BEGINNER;
        }
    }

    public boolean isA(MapleJob basejob) {
        return getId() >= basejob.getId() && getId() / 100 == basejob.getId() / 100;
    }

    public String getJobNameAsString() {
        MapleJob job = this;
        if (job == BEGINNER) {
            return "新手";
        } else if (job == THIEF) {
            return "飞侠";
        } else if (job == WARRIOR) {
            return "战士";
        } else if (job == MAGICIAN) {
            return "魔法师";
        } else if (job == BOWMAN) {
            return "弓箭手";
        } else if (job == PIRATE) {
            return "海盗";
        } else if (job == BANDIT) {
            return "侠客";
        } else if (job == ASSASSIN) {
            return "刺客";
        } else if (job == SPEARMAN) {
            return "枪战士";
        } else if (job == PAGE) {
            return "准骑士";
        } else if (job == FIGHTER) {
            return "剑客";
        } else if (job == CLERIC) {
            return "牧师";
        } else if (job == IL_WIZARD) {
            return "冰雷法师";
        } else if (job == FP_WIZARD) {
            return "火毒法师";
        } else if (job == HUNTER) {
            return "猎人";
        } else if (job == CROSSBOWMAN) {
            return "弩弓手";
        } else if (job == GUNSLINGER) {
            return "Gunslinger";
        } else if (job == BRAWLER) {
            return "Brawler";
        } else if (job == CHIEFBANDIT) {
            return "独行客";
        } else if (job == HERMIT) {
            return "无影人";
        } else if (job == DRAGONKNIGHT) {
            return "黑骑士";
        } else if (job == WHITEKNIGHT) {
            return "骑士";
        } else if (job == CRUSADER) {
            return "勇士";
        } else if (job == PALADIN) {
            return "圣骑士";
        } else if (job == PRIEST) {
            return "祭祀";
        } else if (job == IL_MAGE) {
            return "冰雷/巫师";
        } else if (job == FP_MAGE) {
            return "火毒/巫师";
        } else if (job == RANGER) {
            return "射手";
        } else if (job == SNIPER) {
            return "游侠";
        } else if (job == MARAUDER) {
            return "Marauder";
        } else if (job == OUTLAW) {
            return "Outlaw";
        } else if (job == SHADOWER) {
            return "侠盗";
        } else if (job == GODGUNNER) {
            return "炮手";
        } else if (job == GODGUNNER_2) {
            return "火炮手";
        } else if (job == GODGUNNER_3) {
            return "毁灭炮手";
        } else if (job == GODGUNNER_4) {
            return "神炮手";
        } else if (job == NIGHTLORD) {
            return "隐士";
        } else if (job == DARKKNIGHT) {
            return "Dark Knight";
        } else if (job == HERO) {
            return "英雄";
        } else if (job == PALADIN) {
            return "圣骑士";
        } else if (job == IL_ARCHMAGE) {
            return "魔导师/冰雷";
        } else if (job == FP_ARCHMAGE) {
            return "魔导师/火毒";
        } else if (job == BOWMASTER) {
            return "神射手";
        } else if (job == CROSSBOWMASTER) {
            return "箭神";
        } else if (job == BUCCANEER) {
            return "Buccaneer";
        } else if (job == CORSAIR) {
            return "Corsair";
        } else if (job == Resistance) {
            return "预备兵";
        } else if (job == 采矿) {
            return "采矿";
        } else if (job == 采药) {
            return "采药";
        } else if (job == 装备制作) {
            return "装备制作";
        } else if (job == 饰品制作) {
            return "饰品制作";
        } else if (job == 炼金术) {
            return "炼金术";
        } else if (job == DoubleCrossbows) {
            return "双弩";
        } else if (job == DoubleCrossbows_2) {
            return "双弩";
        } else if (job == DoubleCrossbows_3) {
            return "双弩";
        } else if (job == DoubleCrossbows_4) {
            return "双弩";
        } else if (job == DemonHunter) {
            return "恶魔猎手";
        } else if (job == DemonHunter_2) {
            return "恶魔猎手";
        } else if (job == DemonHunter_3) {
            return "恶魔猎手";
        } else if (job == DemonHunter_4) {
            return "恶魔猎手";
        } else {
            return "管理员";
        }
    }

    public static boolean isExtendSPJob(MapleJob job) {
        return isExtendSPJob(job.getId());
    }

    public static boolean isEvan(int jobid) {
        return (jobid == 2001) || (jobid / 100 == 22);
    }

    public static boolean isExtendSPJob(int jobId) {
        return (jobId / 1000 == 3) || (jobId / 100 == 22) || (jobId == 2001);
    }

    public static boolean isExtendSPJobEx(int jobId) {
        return ((jobId >= 3200) && (jobId <= 3512)) || (jobId / 100 == 22);
    }

    public static int GetSkillAtSlot(int skillid) {
        int jobid = skillid / 10000;
        int ret = 0;
        for (MapleJob mapleJob : values()) {
            if (mapleJob.getId() == jobid) {
                ret = (0 > mapleJob.GetMaxSpSlots() ? 0 : mapleJob.GetMaxSpSlots());
            }
        }

        return ret;
    }
}