package net.sf.odinms.server;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import net.sf.odinms.client.*;
import net.sf.odinms.client.skills.*;
import net.sf.odinms.client.status.MapleMonsterStat;
import net.sf.odinms.client.status.MonsterStatusEffect;
import net.sf.odinms.net.world.PlayerCoolDownValueHolder;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.*;
import net.sf.odinms.server.skill.*;
import net.sf.odinms.server.skill.MapleForeignBuffSkill;
import net.sf.odinms.server.skill.MapleForeignBuffStat;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.output.MaplePacketLittleEndianWriter;
import org.apache.log4j.Logger;

/**
 * @author Matze
 * @author Frz
 */
public class MapleStatEffect implements Serializable {

    public static final List<Pair<MapleBuffStat, Integer>> 恶魔复仇者 = Collections.singletonList(Pair.Create(MapleBuffStat.恶魔复仇者, 3));
    public static final List<Pair<MapleBuffStat, Integer>> 侠盗本能 = Collections.singletonList(Pair.Create(MapleBuffStat.侠盗本能_击杀点, 0));
    // public static List<MapleBuffStat> 骑宠BUFF = Arrays.asList(MapleBuffStat.坐骑状态);
    public static final List<Integer> IsIntValue = new CopyOnWriteArrayList<Integer>();
    static final long serialVersionUID = 9179541993413738569L;
    private static Logger log = Logger.getLogger(MapleStatEffect.class);
    private static MapleStatEffect instance = null;
    private short watk, matk, wdef, mdef, acc, avoid, hands, speed, jump;
    private short hp, mp;
    private double hpR, mpR;
    private short mpCon, hpCon;
    private int duration;
    private boolean overTime;
    private int sourceid;
    private int moveTo;
    private boolean skill;
    public List<Pair<MapleBuffStat, Integer>> statups;
    private Map<MapleMonsterStat, Integer> monsterStatus;
    private int x, y, z;
    private double prop;
    private int itemCon, itemConNo;
    private int fixDamage;
    private int damage, attackCount, bulletCount, bulletConsume;
    private Point lt, rb;
    private int mobCount;
    private int moneyCon;
    private int cooldown;
    private boolean isMorph = false;
    private int morphId = 0;
    private List<MapleDisease> cureDebuffs;
    private int mastery, range;
    private String remark;
    private int maxLevel;
    private int t;
    private int dex;
    private int indieMhp;
    //private int damR;
    private boolean isGhost;
    /*
     * private int fatigue; private int addMagic; private int decDamage; private
     * int ignoreMobwdef; private int subDuration; private int terR; private int
     * asrR;
     */
    private int epad;
    private int epdd;
    private int emdd;
    private int emhp;
    private int emmp;
    /*
     * private int cr; private int criticaldamageMin; private int
     * criticaldamageMax; private int dotDamage; private int dotInterval;
     * private int dotTime; private int mesoR; private int u; private int v;
     * private int w; private int pddR; private int expR; private int
     * itemConsume;
     */
    private boolean firstDoor = true; //机械师
    private double mmpR, mhpR;
    private int expinc;
    private boolean refreshstyle = false;//是否需要刷新外观到其他玩家
    private boolean onoffSkill = false;//是否为开关技能
    private int MDamageOver;
    private MapleForeignBuffSkill foreign;
    private byte powerCon;

    public MapleStatEffect() {
    }

    public short getAcc() {
        return acc;
    }

    public boolean isRefreshstyle() {
        return refreshstyle;
    }

    public void setAcc(short acc) {
        this.acc = acc;
    }

    public short getAvoid() {
        return avoid;
    }

    public void setAvoid(short avoid) {
        this.avoid = avoid;
    }

    public List<MapleDisease> getCureDebuffs() {
        return cureDebuffs;
    }

    public boolean isOnoffSkill() {
        return onoffSkill;
    }

    public void setCureDebuffs(List<MapleDisease> cureDebuffs) {
        this.cureDebuffs = cureDebuffs;
    }

    public int getEmdd() {
        return emdd;
    }

    public void setEmdd(int emdd) {
        this.emdd = emdd;
    }

    public int getEmhp() {
        return emhp;
    }

    public void setEmhp(int emhp) {
        this.emhp = emhp;
    }

    public int getEmmp() {
        return emmp;
    }

    public void setEmmp(int emmp) {
        this.emmp = emmp;
    }

    public int getEpad() {
        return epad;
    }

    public void setEpad(int epad) {
        this.epad = epad;
    }

    public int getEpdd() {
        return epdd;
    }

    public void setEpdd(int epdd) {
        this.epdd = epdd;
    }

    public boolean isFirstDoor() {
        return firstDoor;
    }

    public void setFirstDoor(boolean firstDoor) {
        this.firstDoor = firstDoor;
    }

    public int getFixDamage() {
        return fixDamage;
    }

    public void setFixDamage(int fixDamage) {
        this.fixDamage = fixDamage;
    }

    public short getHands() {
        return hands;
    }

    public void setHands(short hands) {
        this.hands = hands;
    }

    public short getHp() {
        return hp;
    }

    public void setHp(short hp) {
        this.hp = hp;
    }

    public short getHpCon() {
        return hpCon;
    }

    public void setHpCon(short hpCon) {
        this.hpCon = hpCon;
    }

    public double getHpR() {
        return hpR;
    }

    public void setHpR(double hpR) {
        this.hpR = hpR;
    }

    public boolean isIsGhost() {
        return isGhost;
    }

    public void setIsGhost(boolean isGhost) {
        this.isGhost = isGhost;
    }

    public boolean isIsMorph() {
        return isMorph;
    }

    public void setIsMorph(boolean isMorph) {
        this.isMorph = isMorph;
    }

    public int getItemCon() {
        return itemCon;
    }

    public void setItemCon(int itemCon) {
        this.itemCon = itemCon;
    }

    public int getItemConNo() {
        return itemConNo;
    }

    public void setItemConNo(int itemConNo) {
        this.itemConNo = itemConNo;
    }

    public short getJump() {
        return jump;
    }

    public void setJump(short jump) {
        this.jump = jump;
    }

    public static Logger getLog() {
        return log;
    }

    public static void setLog(Logger log) {
        MapleStatEffect.log = log;
    }

    public short getMatk() {
        return matk;
    }

    public void setMatk(short matk) {
        this.matk = matk;
    }

    public short getMdef() {
        return mdef;
    }

    public void setMdef(short mdef) {
        this.mdef = mdef;
    }

    public double getMhpR() {
        return mhpR;
    }

    public void setMhpR(double mhpR) {
        this.mhpR = mhpR;
    }

    public double getMmpR() {
        return mmpR;
    }

    public void setMmpR(double mmpR) {
        this.mmpR = mmpR;
    }

    public Map<MapleMonsterStat, Integer> getMonsterStatus() {
        return monsterStatus;
    }

    public void setMonsterStatus(Map<MapleMonsterStat, Integer> monsterStatus) {
        this.monsterStatus = monsterStatus;
    }

    public int getMorphId() {
        return morphId;
    }

    public void setMorphId(int morphId) {
        this.morphId = morphId;
    }

    public int getMoveTo() {
        return moveTo;
    }

    public void setMoveTo(int moveTo) {
        this.moveTo = moveTo;
    }

    public short getMp() {
        return mp;
    }

    public void setMp(short mp) {
        this.mp = mp;
    }

    public short getMpCon() {
        return mpCon;
    }

    public void setMpCon(short mpCon) {
        this.mpCon = mpCon;
    }

    public double getMpR() {
        return mpR;
    }

    public void setMpR(double mpR) {
        this.mpR = mpR;
    }

    public double getProp() {
        return prop;
    }

    public void setProp(double prop) {
        this.prop = prop;
    }

    public int getSourceid() {
        return sourceid;
    }

    public void setSourceid(int sourceid) {
        this.sourceid = sourceid;
    }

    public short getSpeed() {
        return speed;
    }

    public void setSpeed(short speed) {
        this.speed = speed;
    }

    public short getWatk() {
        return watk;
    }

    public void setWatk(short watk) {
        this.watk = watk;
    }

    public short getWdef() {
        return wdef;
    }

    public void setWdef(short wdef) {
        this.wdef = wdef;
    }

    public static boolean HasIntValue(int id) {
        return IsIntValue.contains(id);
    }

    public List<MapleBuffStat> buffStats() {
        List<MapleBuffStat> ret = new ArrayList<MapleBuffStat>();
        for (Pair<MapleBuffStat, Integer> pair : statups) {
            ret.add(pair.getLeft());
        }
        return ret;
    }

    public String getRemark() {
        return remark;
    }

    public static MapleStatEffect loadItemEffectFromData(MapleData source, int itemid) {
        return loadFromData(source, itemid, false, false);
    }

    private static void addBuffStatPairToListIfNotZero(List<Pair<MapleBuffStat, Integer>> list, MapleBuffStat buffstat, Integer val) {
        if (val.intValue() != 0) {
            list.add(new Pair<MapleBuffStat, Integer>(buffstat, val));
        }
    }

    //读取skill.xml， 貌似都是技能等级里面的
    public static MapleStatEffect loadSkillEffectFromData(MapleData source, int sourceid, int level, boolean beginner, boolean overTime) {
        MapleStatEffect ret = new MapleStatEffect();//定义角色效果ret
        if (level == 0 && beginner) {
            return loadFromData(source, sourceid, true, overTime);
        }

        ret.sourceid = sourceid;
        ret.watk = (short) ret.技能解析(MapleDataTool.getString("pad", source, null), level, 0);
        ret.matk = (short) ret.技能解析(MapleDataTool.getString("mad", source, null), level, 0);
        ret.wdef = (short) ret.技能解析(MapleDataTool.getString("pdd", source, null), level, 0);
        ret.mdef = (short) ret.技能解析(MapleDataTool.getString("mdd", source, null), level, 0);
        ret.duration = ret.技能解析(MapleDataTool.getString("time", source, null), level, -1);
        ret.epad = ret.技能解析(MapleDataTool.getString("epad", source, null), level, 0);
        ret.epdd = ret.技能解析(MapleDataTool.getString("epdd", source, null), level, 0);
        ret.emdd = ret.技能解析(MapleDataTool.getString("emdd", source, null), level, 0);
        ret.acc = (short) ret.技能解析(MapleDataTool.getString("acc", source, null), level, 0);
        ret.avoid = (short) ret.技能解析(MapleDataTool.getString("eva", source, null), level, 0);
        ret.speed = (short) ret.技能解析(MapleDataTool.getString("speed", source, null), level, 0);
        ret.jump = (short) ret.技能解析(MapleDataTool.getString("jump", source, null), level, 0);
        ret.morphId = ret.技能解析(MapleDataTool.getString("morph", source, null), level, 0);
        ret.emhp = ret.技能解析(MapleDataTool.getString("emhp", source, null), level, 0);
        ret.emmp = ret.技能解析(MapleDataTool.getString("emmp", source, null), level, 0);
        ret.hp = (short) ret.技能解析(MapleDataTool.getString("hp", source, null), level, 0);
        ret.mp = (short) ret.技能解析(MapleDataTool.getString("mp", source, null), level, 0);
        ret.mpR = (ret.技能解析(MapleDataTool.getString("mpR", source, null), level, 0) / 100.0);
        ret.hpR = (ret.技能解析(MapleDataTool.getString("hpR", source, null), level, 0) / 100.0);
        ret.mpCon = (short) ret.技能解析(MapleDataTool.getString("mpCon", source, null), level, 0);
        ret.hpCon = (short) ret.技能解析(MapleDataTool.getString("hpCon", source, null), level, 0);
        ret.x = ret.技能解析(MapleDataTool.getString("x", source, null), level, 0);
        ret.y = ret.技能解析(MapleDataTool.getString("y", source, null), level, 0);
        ret.z = ret.技能解析(MapleDataTool.getString("z", source, null), level, 0);
        ret.powerCon = (byte) ret.技能解析(MapleDataTool.getString("powerCon", source, "0"), level, 0);
        ret.damage = ret.技能解析(MapleDataTool.getString("damage", source, null), level, 100);
        int iprop = ret.技能解析(MapleDataTool.getString("prop", source, null), level, 100);
        ret.prop = iprop / 100.0;
        //技能提升Hp Mp上限
        ret.mhpR = (ret.技能解析(MapleDataTool.getString("mhpR", source, null), level, 0) / 100.0);
        ret.mmpR = (ret.技能解析(MapleDataTool.getString("mmpR", source, null), level, 0) / 100.0);
        //常用取值结束

        ret.range = ret.技能解析(MapleDataTool.getString("range", source, null), level, 0);
        ret.dex = ret.技能解析(MapleDataTool.getString("dex", source, null), level, 0);
        ret.mobCount = ret.技能解析(MapleDataTool.getString("mobCount", source, null), level, 1);
        ret.mastery = ret.技能解析(MapleDataTool.getString("mastery", source, null), level, 0);
        ret.attackCount = ret.技能解析(MapleDataTool.getString("attackCount", source, null), level, 1);
        ret.mobCount = ret.技能解析(MapleDataTool.getString("mobCount", source, null), level, 1);
        ret.cooldown = ret.技能解析(MapleDataTool.getString("cooltime", source, null), level, 0);
        ret.bulletCount = ret.技能解析(MapleDataTool.getString("bulletCount", source, null), level, 1);
        ret.itemCon = ret.技能解析(MapleDataTool.getString("itemCon", source, null), level, 0);
        ret.itemConNo = ret.技能解析(MapleDataTool.getString("itemConNo", source, null), level, 0);
        ret.moneyCon = ret.技能解析(MapleDataTool.getString("moneyCon", source, null), level, 0);
        ret.bulletConsume = ret.技能解析(MapleDataTool.getString("bulletConsume", source, null), level, 0);
        ret.MDamageOver = ret.技能解析(MapleDataTool.getString("MDamageOver", source, null), level, 999999);

        ArrayList<Pair<MapleBuffStat, Integer>> statups = new ArrayList<Pair<MapleBuffStat, Integer>>();
        if ((ret.overTime) && (ret.getSummonMovementType() == null)) {
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.WATK, Integer.valueOf(ret.watk));
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.WDEF, Integer.valueOf(ret.wdef));
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.MATK, Integer.valueOf(ret.matk));
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.MDEF, Integer.valueOf(ret.mdef));
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.ACC, Integer.valueOf(ret.acc));
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.AVOID, Integer.valueOf(ret.avoid));
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.SPEED, Integer.valueOf(ret.speed));
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.JUMP, Integer.valueOf(ret.jump));
        }
        List cure = new ArrayList(5);
        if (MapleDataTool.getInt("poison", source, 0) > 0) {
            cure.add(MapleDisease.POISON);
        }
        if (MapleDataTool.getInt("seal", source, 0) > 0) {
            cure.add(MapleDisease.SEAL);
        }
        if (MapleDataTool.getInt("darkness", source, 0) > 0) {
            cure.add(MapleDisease.DARKNESS);
        }
        if (MapleDataTool.getInt("weakness", source, 0) > 0) {
            cure.add(MapleDisease.WEAKEN);
        }
        if (MapleDataTool.getInt("curse", source, 0) > 0) {
            cure.add(MapleDisease.CURSE);
        }
        ret.cureDebuffs = cure;
        MapleData ltd = source.getChildByPath("lt");
        if (ltd != null) {
            ret.lt = ((Point) ltd.getData());
            ret.rb = ((Point) source.getChildByPath("rb").getData());
        }

        ret.skill = true;
        if (!ret.skill && ret.duration > -1) {
            ret.overTime = true;
        } else {
            ret.duration *= 1000;
            ret.overTime = overTime;
        }
        ret.moveTo = MapleDataTool.getInt("moveTo", source, -1);
        Map monsterStatus = new HashMap();
        //Map<MonsterStatus, Integer> monsterStatus = new ArrayMap<MonsterStatus, Integer>();
        if (ret.skill) { // hack because we can't get from the datafile...无法从数据文件获取的技能列表
            switch (sourceid) {
                case 2001002: // 魔法盾
                case 12001001://魔法盾
                case 22111001://魔法盾
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MAGIC_GUARD, Integer.valueOf(ret.x)));
                    break;
                case 2301003: // 神之保护
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.INVINCIBLE, Integer.valueOf(ret.x)));
                    break;
                case 9001004: // 隐藏术
                    ret.duration = 60 * 120 * 1000;
                    ret.overTime = true;
                case 4001003: // 隐身术
                case 夜行者.隐身术:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.DARKSIGHT, Integer.valueOf(ret.x)));
                    break;
                /*
                 * case 4211005: // 金钱护盾 statups.add(new Pair<MapleBuffStat,
                 * Integer>(MapleBuffStat.MESOGUARD, Integer.valueOf(ret.x)));
                 * break;
                 */
                case 4111001: // 聚财术
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MESOUP, Integer.valueOf(ret.x)));
                    break;
                case 4111002: //影分身
                case 4211008://影分身
                case 14111000://影分身
                case 4331002: //镜像分身
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SHADOWPARTNER, Integer.valueOf(ret.x)));
                    IsIntValue.add(sourceid);
                    ret.initForeign(new MapleForeignBuffShortStat(MapleBuffStat.SHADOWPARTNER));
                    break;
                case 3101004: // 无形箭
                case 3201004: //无形箭
                case 2311002: // 时空门
                case 13101003: //精灵使者-无形箭
                case 33101003: //弩骑 无形箭：弩
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SOULARROW, Integer.valueOf(ret.x)));
                    break;
                case 1211004: //火焰冲击
                case 1211006: // 寒冰冲击
                case 1211008: //雷鸣冲击
                case 1221004: //神圣冲击
                case 15101006: //雷鸣
                case 战神.冰雪矛: //冰雪矛
                case 魂骑士.灵魂属性:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.WK_CHARGE, Integer.valueOf(ret.x)));
                    ret.initForeign(new MapleForeignBuffShortStat(MapleBuffStat.WK_CHARGE));
                    break;
                case 1101004: // 剑客 - 快速武器
                case 1201004: // 准骑士 - 快速武器
                case 1301004: // 枪战士 - 快速武器
                case 2111005: // 火毒巫师 - 魔法狂暴
                case 2211005: // 冰雷巫师 - 魔法狂暴
                case 2311006:// 牧师 - 魔法狂暴
                case 3101002: // 猎人 - 快速箭
                case 3201002: // 弩弓手 - 快速弩
                case 4101003: // 刺客 - 快速暗器
                case 4201002: // 侠客 - 快速短刀
                case 4301002: // 刀客 - 快速双刀
                case 5101006: // 拳手 - 急速拳
                case 5201003: // 火枪手 - 速射
                case 11101001: // 魂骑士 - 快速剑
                case 12101004: // 炎术士 - 魔法狂暴
                case 13101001: // 风灵使者 - 快速箭
                case 14101002: // 夜行者 - 快速暗器
                case 15101002: // 奇袭者 - 急速拳
                case 21001003: // 战神 - 快速矛
                case 22141002: // 龙神 - 魔法狂暴
                case 32101005: // 幻灵斗师 - 快速长杖
                case 33001003: // 豹弩游侠 - 快速弩
                case 35101006: // 机械师 - 机械加速
                case 5301002://大炮加速
                case 23101002://快速双弩枪
                case 31001001:
                case 5701005://速射
                case 24101005://快速手杖
                case 4311009://快速双刀
                case 51101003://快速剑
                case 31201002://恶魔加速
                case 36101004://尖兵加速
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.BOOSTER, Integer.valueOf(ret.x)));
                    break;
                case 5121009://极速领域
                case 奇袭者.极速领域:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.极速领域, Integer.valueOf(ret.x)));
                    break;
                case 1101006: // 愤怒之火
                case 11101003: // 愤怒之火
                //以上2个技能093取消防御减少
                case 1121010: // 葵花宝典  以上技能都更换了mask
                    //            statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.WATK, Integer.valueOf(ret.watk)));
                    break;
                case 1301006: // 极限防御
                case 2001003: //魔法铠甲
                case 炎术士.魔法铠甲:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.WDEF, Integer.valueOf(ret.wdef)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MDEF, Integer.valueOf(ret.mdef)));
                    break;
                case 1001003: // 圣甲术
                case 11001001:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.WDEF, Integer.valueOf(ret.wdef)));
                    break;
                case 2101001: // 精神力
                case 2201001: // 精神力
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MATK, Integer.valueOf(ret.matk)));
                    break;
                case 4101004: // 轻功
                case 4201003: // 轻功
                case 9001001: // gm 轻功
                case 4311001: //暗影轻功
                case 4001005://轻功
                case 4301003://暗影轻功
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SPEED, Integer.valueOf(ret.speed)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.JUMP, Integer.valueOf(ret.jump)));
                    break;
                case 2301004: // 祝福
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.祝福, Integer.valueOf(level)));
                    break;
                case 3001003: // 集中术
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.ACC, Integer.valueOf(ret.acc)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.AVOID, Integer.valueOf(ret.avoid)));
                    break;
                case 9001003: // gm 枫印祝福
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MATK, Integer.valueOf(ret.matk)));
                case 3111000: // 集中精力
                case 3211000:
                case 13111001:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.EWatk, Integer.valueOf(ret.epad)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.CONCENTRATE, Integer.valueOf(ret.x)));
                    break;
                case 5001005: // 疾驰
                case 4321000: // 龙卷风
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.疾驰_龙卷风, Integer.valueOf(ret.x)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.疾驰_龙卷风, Integer.valueOf(ret.y)));
                    break;
                case 1101007: // 伤害反击
                case 1201007:// 伤害反击
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.POWERGUARD, Integer.valueOf(ret.x)));
                    break;
                case 1301007://神圣之火
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.HYPERBODYHP, Integer.valueOf(ret.x)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.HYPERBODYMP, Integer.valueOf(ret.y)));
                    break;
                case 1120003:
                case 1111002: // 斗气集中
                case 11111001:// 斗气集中
                    ret.initForeign(new MapleForeignBuffNoSkill(MapleBuffStat.COMBO) {
                        @Override
                        public void writePacket(MaplePacketLittleEndianWriter mplew, int value) {
                            mplew.writeShort(value);
                            mplew.write();
                            mplew.write(5);
                        }
                    });
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.COMBO, Integer.valueOf(1)));
                    break;
                case 1011://勇士的意志
                case 20001011://勇士的意志
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.BERSERK_FURY, Integer.valueOf(1)));
                    break;
                case 1004: // 骑兽技能
                case 10001004:// 骑兽技能
                case 20001004:// 骑兽技能
                case 5221006: // 骑兽技能
                case 20011004:// 骑兽技能
                case 33001001://美洲豹骑士
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.骑宠1, Integer.valueOf(0)));
                    break;
                case 1311006: //龙咆哮
                case 21111009:
                    ret.hpR = -ret.x / 100.0;
                    break;
                case 1311008: // 龙之力
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.龙之力, Integer.valueOf(ret.技能解析(MapleDataTool.getString("str", source, null), level, 0))));
                    break;
                case 1121000: // 冒险岛勇士
                case 1221000: // 冒险岛勇士
                case 1321000: // 冒险岛勇士
                case 2121000: // 冒险岛勇士
                case 2221000: // 冒险岛勇士
                case 2321000: // 冒险岛勇士
                case 3121000: // 冒险岛勇士
                case 3221000: // 冒险岛勇士
                case 4121000: // 冒险岛勇士
                case 4221000: // 冒险岛勇士
                case 4341000: // 冒险岛勇士
                case 5121000: // 冒险岛勇士
                case 5221000: // 冒险岛勇士
                case 21121000: // 冒险岛勇士
                case 龙神.冒险岛勇士:
                case 33121007: //弩骑勇士
                case 35121007: //机械勇士
                case 32121007: //幻灵勇士
                case 23121005://双弩勇士
                case 31121004://恶魔勇士
                case 5721000://传人勇士
                case 24121008://幻影勇士
                case 51121005://米哈尔勇士
                case 31221008://恶魔复仇者勇士
                case 36121008://尖兵勇士
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MAPLE_WARRIOR, Integer.valueOf(ret.x)));
                    break;
                case 3121002: // 火眼晶晶
                case 3221002: // 火眼晶晶
                case 33121004:// 弩骑火眼
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SHARP_EYES, Integer.valueOf(ret.x << 8 | ret.y)));
                    break;
                case 1321007: //灵魂助力
                    statups.add(Pair.Create(MapleBuffStat.灵魂助力, level));
                    break;
                case 2221005: //火魔兽
                //case 2311006: //圣龙召唤
                case 2321003: // 强化圣龙
                case 3111005: // 火凤凰
                case 3211005://冰凤凰
                case 5211001: //章鱼炮台
                case 5211002: //海鸥空袭
                case 5220002: // 超级章鱼炮台
                case 11001004://魂精灵
                case 12001004://炎精灵
                case 13001004://风精灵
                case 14001005://夜精灵
                case 15001004://雷精灵
                case 12111004://火魔兽
                    //           statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SUMMON, Integer.valueOf(1)));
                    break;
                case 2311003: // 神圣祈祷
                case 9001002: // GM 神圣祈祷
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.HOLY_SYMBOL, Integer.valueOf(ret.x)));
                    break;
                case 4111009: // 暗器伤人
                case 14111007:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SHADOW_CLAW, Integer.valueOf(level)));
                    break;
                case 2121004:// 终极无限
                case 2221004:// 终极无限
                case 2321004: // 终极无限
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.STANCE, Integer.valueOf((int) (ret.prop * 100))));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.INFINITY, Integer.valueOf(ret.x)));
                    ret.t = ret.duration / 4000;
                    break;
                case 1121002: // 稳如泰山
                case 1221002: // 稳如泰山
                case 1321002: // 稳如泰山
                case 21121003: //战神的意志
                case 32121005: //战法 稳如泰山
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.STANCE, Integer.valueOf(ret.x))); //ret.x定值为1
                    break;
                case 1005: // 英雄之回声
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.ECHO_OF_HERO, Integer.valueOf(ret.x)));
                    break;
                /*case 2121002: // 魔法反击
                 case 2221002: // 魔法反击
                 case 2321002: // 魔法反击
                 statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MANA_REFLECTION, Integer.valueOf(1)));
                 break;*/
                case 2321005: // (现 进阶祝福)圣灵之盾
                    ret.indieMhp = ret.技能解析(MapleDataTool.getString("indieMhp", source, null), level, 0);
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.进阶祝福, Integer.valueOf(level)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.EMaxHp, Integer.valueOf(ret.indieMhp)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.EMaxMp, Integer.valueOf(ret.indieMhp)));
                    break;
                case 3111002: // 射手 - 替身术
                case 3211002: // 游侠 - 替身术
                case 13111004: //风灵使者 - 替身术
                    /*
                 * case 35111002: // 机械师 - 磁场 case 35111005: // 机械师 - 加速器：EX-7
                 * case 35111011: // 机械师 - 治疗机器人：H-LX case 35121003: // 机械师 -
                 * 战争机器：泰坦 case 35121010: // 机械师 - 放大器：AF-11
                 */
                case 33111003: //野性陷阱
                    //       statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.PUPPET, Integer.valueOf(1)));
                    break;
                /*
                 * case 4341006: // 暗影双刀 - 傀儡召唤 statups.add(new
                 * Pair<MapleBuffStat, Integer>(MapleBuffStat.傀儡召唤,
                 * Integer.valueOf(1))); break;
                 */
                case 2111007: //快速移动精通 开关技能
                case 2211007:
                case 2311007:
                case 32111010:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.快速移动精通, Integer.valueOf(ret.x)));
                    ret.duration = 999999999;
                    break;
                case 32121003: //飓风
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.飓风, Integer.valueOf(1)));
                    break;
                case 32111004: //转化
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.转化, Integer.valueOf(ret.x)));
                    break;
                case 32001003: //黑暗灵气
                case 32101002: //蓝色灵气
                case 32101003: //黄色灵气
                case 战法.进阶黑暗灵气:
                case 战法.进阶蓝色灵气:
                case 战法.进阶黄色灵气:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.灵气, Integer.valueOf(ret.x)));
                    ret.duration = 999999999;
                    break;
                case 21111001: //灵巧击退
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.灵巧击退, Integer.valueOf(ret.x)));
                    break;
                case 32101004: //伤害吸收
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.伤害吸收, Integer.valueOf(ret.x)));
                    break;
                case 21101003: //抗压
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.抗压, Integer.valueOf(ret.x)));
                    break;
                case 22131001: //魔法屏障
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.魔法屏障, Integer.valueOf(ret.x)));
                    break;
                case 4341007: //荆棘 093更改 变为2个buff叠加 原有buff取消
                    //statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.荆棘, Integer.valueOf(ret.x)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.EWatk, Integer.valueOf(ret.epad)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.STANCE, Integer.valueOf(ret.x)));
                    break;
                case 龙神.缓速术: //缓速术
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.缓速术, Integer.valueOf(ret.x)));
                    break;
                case 32111005: //霸体
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.霸体, Integer.valueOf(ret.x)));
                    break;
                case 35121003:
                    ret.duration = 2100000000;
                    break;
                case 机械师.火焰喷射器:
                case 35101009:
                case 机械师.金属机甲_导弹战车:
                case 机械师.金属机甲_重机枪:
                case 机械师.金属机甲_重机枪_4转:
                    ret.duration = 2100000000;
                    ret.overTime = true;
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.机械师, Integer.valueOf(level)));
                    break;
                case 21100005: //连环吸血
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.连环吸血, Integer.valueOf(ret.x)));
                    break;
                case 32111006: //重生
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.重生, Integer.valueOf(ret.x)));
                    break;
                case 33121006: //暴走状态
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.暴走HP, Integer.valueOf(ret.x)));
                    //这个决定是否出现翅膀
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.暴走攻击, Integer.valueOf(ret.y)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SPEED, Integer.valueOf(ret.z)));
                    break;
                case 33111004:// 弩骑 致盲
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.BLIND, Integer.valueOf(ret.x)));
                    break;
                case 3221006: // 幻影步
                case 3121007: // 幻影步
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.幻影步_敏捷, Integer.valueOf(ret.dex)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.幻影步_回避几率, Integer.valueOf(ret.x)));
                    break;
                /*
                 * case 33101006: // 吞噬 消化技能 statups.add(new Pair<MapleBuffStat,
                 * Integer>(MapleBuffStat.吞噬1, Integer.valueOf(ret.x)));
                 * statups.add(new Pair<MapleBuffStat,
                 * Integer>(MapleBuffStat.吞噬2, Integer.valueOf(ret.x)));
                 * statups.add(new Pair<MapleBuffStat,
                 * Integer>(MapleBuffStat.吞噬3, Integer.valueOf(ret.x)));
                 * statups.add(new Pair<MapleBuffStat,
                 * Integer>(MapleBuffStat.吞噬4, Integer.valueOf(ret.x)));
                 * statups.add(new Pair<MapleBuffStat,
                 * Integer>(MapleBuffStat.吞噬5, Integer.valueOf(ret.x))); break;
                 */
                case 33101004: //地雷
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.地雷, Integer.valueOf(ret.x)));
                    break;
                case 5111005: //超人变形
                case 5121003: //超级变身
                case 13111005://风灵使者 - 信天翁
                case 15111002://奇袭者 - 超级变身
                    //     statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.EWatk, Integer.valueOf(ret.epad)));
                    //    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.EWdef, Integer.valueOf(ret.epdd)));
                    //     statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.EMdef, Integer.valueOf(ret.emdd)));
                    //     statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SPEED, Integer.valueOf(ret.speed)));
                    //      statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.JUMP, Integer.valueOf(ret.jump)));
                    //根据封包发送顺序 这个值要放在最后发送
                    //statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MORPH, Integer.valueOf(ret.morphId)));
                    //2002是冰骑士 换着玩
                    //     statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MORPH, Integer.valueOf(ret.morphId)));
                    break;
                case 4341002: //终极斩
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.终极斩, Integer.valueOf(ret.y)));
                    break;
                case 4331003: //死亡猫头鹰 奖励次数 5+u(x/4) 次
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.死亡猫头鹰, Integer.valueOf(ret.y)));
                    ret.duration = 2100000000;
                    break;
                case 21120007: //战神之盾
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.战神之盾, Integer.valueOf(ret.x)));
                    break;
                case 21000000: //矛连击强化
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.矛连击强化, Integer.valueOf(level * 10)));
                    ret.duration = 2100000000;
                    break;
                case 22181003: //灵魂之石
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.灵魂之石, Integer.valueOf(ret.x)));
                    break;
                case 22151003: //抗魔领域
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.抗魔领域, Integer.valueOf(ret.x)));
                    break;
                case 22181000: //玛瑙的祝福
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.WDEF, Integer.valueOf(ret.wdef)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MDEF, Integer.valueOf(ret.mdef)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MATK, Integer.valueOf(ret.matk)));
                    break;
                case 1211011: //战斗命令
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.战斗命令, Integer.valueOf(ret.x)));
                    break;
                case 1220013: //祝福护甲
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.祝福护甲_防御次数, Integer.valueOf(ret.x))); //防御次数
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.祝福护甲_物理攻击力, Integer.valueOf(ret.epad))); //增加的攻击力
                    break;
                case 5110001: //能量获得
                case 15100004:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.能量获得, Integer.valueOf(10000))); //满能量时才能发动
                    break;
                case 2211008: //自然力重置
                case 2111008:
                case 12101005:
                case 22121001:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.自然力重置, Integer.valueOf(ret.x)));
                    break;
                case 13101006: //风影漫步
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SPEED, Integer.valueOf(ret.speed)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.风影漫步, Integer.valueOf(1))); //也可以写ret.x 不过x节点定值为1
                    break;
                case 机械师.完美机甲:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.完美机甲, Integer.valueOf(ret.x)));
                    ret.duration = 2100000000;
                    break;
                case 机械师.卫星防护:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.卫星防护, Integer.valueOf(ret.x)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.完美机甲, Integer.valueOf(ret.y)));
                    ret.duration = 2100000000;
                    break;
                case 35111001: // 机械师 - 人造卫星
                case 35111009: // 机械师 - 人造卫星
                case 35111010: // 机械师 - 人造卫星
                    ret.duration = 2100000000;
                    ret.overTime = true;
                    break;
                case 风灵使者.终极弓: //骑士团的终极技能比较特殊 是Buff
                case 魂骑士.终极剑:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.骑士团主动终极, Integer.valueOf(1))); //也可以写ret.x 不过x节点定值为1
                    break;
                case 枪手.幸运骰子:
                case 拳手.幸运骰子:
                case 机械师.幸运骰子:
                case 5311005:
                case 5320007:
                    //这里固定1点数 具体还要去下面处理随机点数
                    ret.overTime = true;
                    ret.skill = true;
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.幸运骰子, Integer.valueOf(1)));
                    break;
                case 奇袭者.闪光击:
                    //         statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.闪光击, Integer.valueOf(1))); //也可以写ret.x 不过x节点定值为1
                    break;
                case 龙神.玛瑙的保佑:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.玛瑙的保佑, Integer.valueOf(ret.x)));
                    break;
                case 龙神.玛瑙的意志:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.玛瑙的意志, Integer.valueOf(ret.x)));
                    break;




                //给予怪物状态
                case 战神.战神之审判:
                    monsterStatus.put(MapleMonsterStat.FREEZE, Integer.valueOf(1));
                    monsterStatus.put(MapleMonsterStat.POISON, Integer.valueOf(ret.damage));
                    break;

                case 机械师.加速器:
                    monsterStatus.put(MapleMonsterStat.WDEF, Integer.valueOf(ret.y));
                    monsterStatus.put(MapleMonsterStat.SPEED, Integer.valueOf(ret.x));
                    break;
                case 1111007: //魔击无效
                case 1211009:
                case 1311007:
                    //定值
                    monsterStatus.put(MapleMonsterStat.魔击无效, Integer.valueOf(1));
                    break;
                case 22161002: //鬼刻符
                    monsterStatus.put(MapleMonsterStat.鬼刻符, Integer.valueOf(ret.x));
                    break;
                case 4341003: //怪物炸弹
                    monsterStatus.put(MapleMonsterStat.怪物炸弹, Integer.valueOf(ret.damage));
                    break;
                case 4001002: // 诅咒术
                    monsterStatus.put(MapleMonsterStat.WATK, Integer.valueOf(ret.x));
                    monsterStatus.put(MapleMonsterStat.WDEF, Integer.valueOf(ret.y));
                    break;
                case 1201006: // 压制术
                    monsterStatus.put(MapleMonsterStat.WATK, Integer.valueOf(ret.x));
                    monsterStatus.put(MapleMonsterStat.WDEF, Integer.valueOf(ret.y));
                    break;
                case 1211002: // 迷惑攻击
                case 1111008: // 虎咆哮
                case 4211002: // 落叶斩
                case 3101005: // 爆炸箭
                case 1111005: // 昏迷
                case 1111006: // 灵魂突刺
                case 4221007: // 一出双击
                case 5101002: // 回马
                case 5101003: // 升龙连击
                case 5121004: // 金手指
                case 5121005: // 索命
                case 5121007: // 光速拳
                case 5201004: // 迷惑射击
                case 11111003: //昏迷
                case 22151001: // 龙神 - 火焰喷射
                case 33101001: //炸裂箭 
                case 33101002: //美洲豹怒吼
                case 33111002: //十字攻击
                case 33121002: //音速震波
                case 机械师.磁场:
                case 龙神.魔力闪爆:
                    monsterStatus.put(MapleMonsterStat.STUN, Integer.valueOf(1));
                    break;

                case 4121003://挑衅
                case 4221003://挑衅
                    monsterStatus.put(MapleMonsterStat.TAUNT, Integer.valueOf(ret.x));
                    monsterStatus.put(MapleMonsterStat.MDEF, Integer.valueOf(ret.x));
                    monsterStatus.put(MapleMonsterStat.WDEF, Integer.valueOf(ret.x));
                    break;
                case 4121004: // 忍者伏击
                case 4221004: // 忍者伏击
                    monsterStatus.put(MapleMonsterStat.忍者伏击, Integer.valueOf(ret.damage));
                    break;
                case 2201004: // 冰冻术
                case 2211002: // 冰咆哮
                case 3211003: // 寒冰箭
                case 2211006: // 冰雷合击
                case 2221007: // 落霜冰破
                case 5211005: // 寒冰喷射
                case 2121006: // 美杜莎之眼
                case 战神.钻石星辰:
                    monsterStatus.put(MapleMonsterStat.FREEZE, Integer.valueOf(1)); //赋予冰冻属性
                    ret.duration *= 2; // freezing skills are a little strange
                    break;
                case 2121003: // 火凤球
                case 2221003: // 冰凤球
                    monsterStatus.put(MapleMonsterStat.POISON, Integer.valueOf(1));
                    monsterStatus.put(MapleMonsterStat.FREEZE, Integer.valueOf(1));
                    break;
                case 2101003: // fp slow
                case 2201003: // 缓速术
                    monsterStatus.put(MapleMonsterStat.SPEED, Integer.valueOf(ret.x));
                    break;
                case 2101005: // 毒雾术
                    ret.duration = (int) (5 + Math.floor(level / 4));
                case 2111006: // fp elemental compo
                    monsterStatus.put(MapleMonsterStat.POISON, Integer.valueOf(1));
                    break;
                case 2311005: //巫毒术
                    monsterStatus.put(MapleMonsterStat.DOOM, Integer.valueOf(1));
                    break;
                case 3101007: // 银鹰召唤
                case 3201007: // 金鹰召唤
                case 33111005: // 银鹰召唤
                    //                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SUMMON, Integer.valueOf(1)));
                    monsterStatus.put(MapleMonsterStat.STUN, Integer.valueOf(1));
                    break;
                case 2121005: // 火魔兽
                case 3221005: //冰凤凰
                    //             statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SUMMON, Integer.valueOf(1)));
                    monsterStatus.put(MapleMonsterStat.FREEZE, Integer.valueOf(1));
                    break;
                case 2111004: //封印术
                case 2211004: //封印术
                case 12111002://封印术
                    monsterStatus.put(MapleMonsterStat.SEAL, 1);
                    break;
                case 4111003: // 影网术
                case 14111001://影网术
                    monsterStatus.put(MapleMonsterStat.SHADOW_WEB, 1);
                    break;
                case 5221009://心灵控制
                    monsterStatus.put(MapleMonsterStat.HYPNOTIZED, 1);
                    break;
                case 1211010: {//元气恢复
                    ret.hpR = ret.x / 100.0;
                    break;
                }
                case 3120006: {//火冰凤凰。
                    ret.mhpR = ret.x / 100.0;
                    //statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.射手_精神连接, level));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.射手_精神连接, 3111005));
                    ret.overTime = true;
                    ret.skill = true;
                    break;
                }
                case 3220005: {
                    ret.mhpR = ret.x / 100.0;
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.射手_精神连接, 3211005));
                    ret.overTime = true;
                    ret.skill = true;
                    break;
                }
                case 2121009:
                case 2221009:
                case 2321010: {//魔力精通
                    ret.overTime = true;
                    ret.duration = Integer.MAX_VALUE;
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.魔力精通, 1));
                    break;
                }
                case 2311009: {//神圣魔法盾
                    ret.hpR = (ret.技能解析(MapleDataTool.getString("z", source, null), level, 0) / 100.0);
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.神圣魔法盾, ret.x));
                    break;
                }
                case 2120010:
                case 2220010:
                case 2320011: {//神秘瞄准术
                    ret.overTime = true;
                    ret.duration = 5000;
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.神秘瞄准术, ret.x));
                    break;
                }
                case 5301003: {//猴子魔法
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.EMaxHp, ret.技能解析(MapleDataTool.getString("indieMhp", source, null), level, 0)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.EMaxMp, ret.技能解析(MapleDataTool.getString("indieMmp", source, null), level, 0)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.EAcc, ret.技能解析(MapleDataTool.getString("indieAcc", source, null), level, 0)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.EAvoid, ret.技能解析(MapleDataTool.getString("indieEva", source, null), level, 0)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.EJump, ret.技能解析(MapleDataTool.getString("indieJump", source, null), level, 0)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.ESpeed, ret.技能解析(MapleDataTool.getString("indieSpeed", source, null), level, 0)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.EAllStat, ret.技能解析(MapleDataTool.getString("indieAllStat", source, null), level, 0)));
                    break;
                }
                case 5320008: {//超级猴子魔法
                    ret.overTime = true;
                    ret.skill = true;
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.EMaxHp, ret.技能解析(MapleDataTool.getString("indieMhp", source, null), level, 0)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.EMaxMp, ret.技能解析(MapleDataTool.getString("indieMmp", source, null), level, 0)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.EAcc, ret.技能解析(MapleDataTool.getString("indieAcc", source, null), level, 0)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.EAvoid, ret.技能解析(MapleDataTool.getString("indieEva", source, null), level, 0)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.EJump, ret.技能解析(MapleDataTool.getString("indieJump", source, null), level, 0)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.ESpeed, ret.技能解析(MapleDataTool.getString("indieSpeed", source, null), level, 0)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.EAllStat, ret.技能解析(MapleDataTool.getString("indieAllStat", source, null), level, 0)));
                    break;
                }
                case 51121004://稳如泰山。米哈尔
                case 5321010: {//海盗精神。
                    ret.overTime = true;
                    ret.skill = true;
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.STANCE, Integer.valueOf((int) (ret.prop * 100))));
                    break;
                }
                case 23101003://精神注入
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.精神注入_伤害, Integer.valueOf(ret.damage)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.精神注入_暴击, Integer.valueOf(ret.x)));
                    IsIntValue.add(ret.getSourceId());
                    break;
                case 23111004://火焰咆哮　
                {
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.火焰咆哮_攻击力, ret.技能解析(MapleDataTool.getString("indiePad", source, null), level, 0)));
                    break;
                }
                case 23111005://水盾
                {
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.水盾_ASR, ret.技能解析(MapleDataTool.getString("asrR", source, null), level, 0)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.水盾_TER, ret.技能解析(MapleDataTool.getString("terR", source, null), level, 0)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.水盾_伤害减少, ret.x));
                    break;
                }
                case 23121004: {//古老意志
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.古老意志_体力, ret.emhp));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.古老意志_攻击, ret.技能解析(MapleDataTool.getString("damR", source, null), level, 0)));
                    break;
                }
                case 31101003: {//黑暗复仇
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.POWERGUARD, ret.y));
                    break;
                }
                case 31111004: {//黑暗忍耐
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.水盾_ASR, ret.y));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.水盾_TER, ret.y));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.黑暗忍耐_防御力, ret.x));
                    break;
                }
                case 31121005: {//黑暗变形
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.黑暗变形_攻击力, ret.技能解析(MapleDataTool.getString("damR", source, null), level, 0)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.古老意志_攻击, level));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.黑暗变形_HP增加, ret.技能解析(MapleDataTool.getString("indieMhpR", source, null), level, 0)));
                    ret.overTime = true;
                    ret.skill = true;
                    break;
                }
                case 31121002: {//吸血鬼之触
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.吸血鬼之触_伤害转化, ret.x));
                    ret.overTime = true;
                    ret.skill = true;
                    break;
                }
                case 31121007: {//无限精气
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.无限精气_主MASK, 1));
                    ret.overTime = true;
                    ret.skill = true;
                    break;
                }
                case 5701006://冥想　
                {
                    statups.add(Pair.Create(MapleBuffStat.ACC, new Integer(ret.acc)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.火焰咆哮_攻击力, ret.技能解析(MapleDataTool.getString("indiePad", source, null), level, 0)));
                    break;
                }
                case 5721009://海盗精神
                {
                    statups.add(Pair.Create(MapleBuffStat.AVOID, ret.技能解析(MapleDataTool.getString("eva", source, null), level, 0)));
                    statups.add(Pair.Create(MapleBuffStat.STANCE, ret.x));//
                    statups.add(Pair.Create(MapleBuffStat.古老意志_攻击, ret.技能解析(MapleDataTool.getString("damR", source, null), level, 0)));
                    statups.add(Pair.Create(MapleBuffStat.水盾_ASR, ret.x));
                    statups.add(Pair.Create(MapleBuffStat.水盾_TER, ret.x));
                    break;
                }
                /*
                 * 幻影职业系技能
                 */
                case 20031205: //幻影屏障
                {
                    statups.add(Pair.Create(MapleBuffStat.幻影屏障, 300));
                    ret.initForeign(new MapleForeignBuffShortStat(MapleBuffStat.幻影屏障));
                    break;
                }

                /*
                 * 刀飞职业系
                 */
                case 4311005://双刀-命运
                case 4201009: {//命运
                    statups.add(Pair.Create(MapleBuffStat.WATK, (int) ret.watk));
                    break;
                }
                case 4201011: {//金钱护盾
                    statups.add(Pair.Create(MapleBuffStat.MESOGUARD, Integer.valueOf(ret.x)));
                    break;
                }
                /*    case 4211008: //刀飞的影分身
                 statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SHADOWPARTNER, Integer.valueOf(ret.x)));
                 break;*/
                case 4211003: // 敛财术
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.PICKPOCKET, Integer.valueOf(ret.x)));
                    ret.duration = 999999999;
                    break;
                case 4221013: {//侠盗本能
                    statups.add(Pair.Create(MapleBuffStat.火焰咆哮_攻击力, ret.x));
                    break;
                }
                case 24111002: {//神秘的运气
                    ret.duration = Integer.MAX_VALUE;
                    statups.add(Pair.Create(MapleBuffStat.神秘的运气, ret.x));
                    break;
                }
                case 24111003: {//幸运的保护
                    statups.add(Pair.Create(MapleBuffStat.水盾_ASR, ret.x));
                    statups.add(Pair.Create(MapleBuffStat.水盾_TER, ret.y));
                    statups.add(Pair.Create(MapleBuffStat.幸运的保护_HP, ret.技能解析(MapleDataTool.getString("indieMhpR", source, null), level, 0)));
                    statups.add(Pair.Create(MapleBuffStat.幸运的保护_MP, ret.技能解析(MapleDataTool.getString("indieMmpR", source, null), level, 0)));
                    break;
                }
                case 24111005: {//月光祝福
                    statups.add(Pair.Create(MapleBuffStat.火焰咆哮_攻击力, ret.技能解析(MapleDataTool.getString("indieAcc", source, null), level, 0)));
                    statups.add(Pair.Create(MapleBuffStat.月光祝福_命中率, ret.技能解析(MapleDataTool.getString("indiePad", source, null), level, 0)));
                    break;
                }
                case 24121004: {//圣歌祈祷
                    statups.add(Pair.Create(MapleBuffStat.古老意志_攻击, ret.x));
                    statups.add(Pair.Create(MapleBuffStat.圣歌祈祷_无视防御力, ret.x));
                    break;
                }
                case 51101004: {//愤怒
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.火焰咆哮_攻击力, ret.技能解析(MapleDataTool.getString("indiePad", source, null), level, 0)));
                    break;
                }
                case 51111003: {//闪耀之光
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.闪耀之光, ret.技能解析(MapleDataTool.getString("indieDamR", source, null), level, 0)));
                    break;
                }
                case 51111004: {//灵魂恢复术FS
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.水盾_ASR, ret.x));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.水盾_TER, ret.y));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.黑暗忍耐_防御力, ret.z));
                    break;
                }
                case 51121006: {//灵魂之怒
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.灵魂之怒_伤害增加, ret.x * 100 + 1));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.灵魂之怒_最大暴击, ret.z));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.灵魂之怒_最小暴击, ret.y));
                    break;
                }
                case 61120008: {//终极变身
                    statups.add(Pair.Create(MapleBuffStat.SPEED, new Integer(ret.speed)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MORPH, Integer.valueOf(ret.getMorph())));
                    statups.add(Pair.Create(MapleBuffStat.KUANGLONG_WS, ret.技能解析(MapleDataTool.getString("prop", source, null), level, 0)));
                    statups.add(Pair.Create(MapleBuffStat.KUANGLONG_BL, ret.技能解析(MapleDataTool.getString("cr", source, null), level, 0)));
                    statups.add(Pair.Create(MapleBuffStat.KUANGLONG_GJSD, ret.技能解析(MapleDataTool.getString("indieBooster", source, null), level, 0)));
                    statups.add(Pair.Create(MapleBuffStat.KUANGLONG_GJL, ret.技能解析(MapleDataTool.getString("indieDamR", source, null), level, 0)));
                    ret.overTime = true;
                    ret.skill = true;
                    ret.initForeign(new MapleForeignBuffByteStat(MapleBuffStat.SPEED), new MapleForeignBuffShortStat(MapleBuffStat.MORPH));
                    break;
                }
                case 61120007: {
                    statups.add(Pair.Create(MapleBuffStat.KUANGLONG_JIANBI, ret.技能解析(MapleDataTool.getString("padX", source, null), level, 0)));
                    ret.onoffSkill = true;
                    ret.overTime = true;
                    ret.skill = true;
                    ret.initForeign(new MapleForeignBuffShortStat(MapleBuffStat.KUANGLONG_JIANBI));
                    break;
                }//ret.技能解析(MapleDataTool.getString("damR", source, null), level, 0)
                case 31201003: {// 深渊之怒 
                    statups.add(Pair.Create(MapleBuffStat.火焰咆哮_攻击力, ret.技能解析(MapleDataTool.getString("indiePad", source, null), level, 0)));
                    break;
                }
                case 31211003: { //驱邪
                    statups.add(Pair.Create(MapleBuffStat.驱邪_异常状态抗性, ret.y));
                    statups.add(Pair.Create(MapleBuffStat.驱邪_所有属性抗性, ret.z));
                    statups.add(Pair.Create(MapleBuffStat.驱邪_伤害减少, ret.x));
                    break;
                }
                case 31211004: {// 恶魔恢复
                    statups.add(Pair.Create(MapleBuffStat.幸运的保护_HP, ret.技能解析(MapleDataTool.getString("indieMhpR", source, null), level, 0)));
                    statups.add(Pair.Create(MapleBuffStat.恶魔恢复_定时恢复百分比, ret.x));
                    break;
                }
                case 31221004: {// 惊天之力 
                    statups.add(Pair.Create(MapleBuffStat.KUANGLONG_GJL, ret.技能解析(MapleDataTool.getString("indieDamR", source, null), level, 0)));
                    statups.add(Pair.Create(MapleBuffStat.KUANGLONG_GJSD, ret.技能解析(MapleDataTool.getString("indieBooster", source, null), level, 0)));
                    break;
                }
                case 36001002: {//超能力量 
                    statups.add(Pair.Create(MapleBuffStat.火焰咆哮_攻击力, ret.技能解析(MapleDataTool.getString("indiePad", source, null), level, 0)));
                    break;
                }
                case 36101002: {//直线透视
                    statups.add(Pair.Create(MapleBuffStat.精神注入_暴击, ret.x));
                    break;
                }
                case 36101003: {//高效输能
                    statups.add(Pair.Create(MapleBuffStat.幸运的保护_HP, ret.技能解析(MapleDataTool.getString("indieMhpR", source, null), level, 0)));
                    statups.add(Pair.Create(MapleBuffStat.幸运的保护_MP, ret.技能解析(MapleDataTool.getString("indieMmpR", source, null), level, 0)));
                    break;
                }
                case 36111006: {//全息投影
                    statups.add(Pair.Create(MapleBuffStat.SHADOWPARTNER, 53130));
                    IsIntValue.add(sourceid);
                    ret.onoffSkill = true;
                    ret.duration = -89577;
                    ret.initForeign(new MapleForeignBuffFixedShort(MapleBuffStat.SHADOWPARTNER, (short) -12406));
                    break;
                }
                case 36121004: {//攻击矩形
                    statups.add(Pair.Create(MapleBuffStat.STANCE, ret.x));
                    statups.add(Pair.Create(MapleBuffStat.圣歌祈祷_无视防御力, ret.y));
                    break;
                }
                case 36121003: {//神秘代码 
                    statups.add(Pair.Create(MapleBuffStat.神秘代码_BOSS伤害, ret.x));
                    statups.add(Pair.Create(MapleBuffStat.神秘代码_总伤害增加, ret.技能解析(MapleDataTool.getString("indieDamR", source, null), level, 0)));
                    break;
                }
                default:
                    break;
            }
        }
        /**
         * 特殊判断。
         */
        if (GameConstants.isAngelRingSkill(sourceid)) {
            ret.skill = true;
            ret.overTime = true;
            ret.duration = 999999999;
        }
        if (ret.getSummonMovementType() != null) {
            ret.overTime = true;
            ret.duration += 3000;
        }

        if (ret.isMorph() && !ret.isPirateMorph()) {
            boolean hasAdd = true;
            for (Pair<MapleBuffStat, Integer> e : statups) {
                if (e.getLeft().equals(MapleBuffStat.MORPH)) {
                    hasAdd = false;
                    break;
                }
            }
            if (hasAdd) {
                statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MORPH, Integer.valueOf(ret.getMorph())));
            }
        }
        ret.monsterStatus = monsterStatus;
        statups.trimToSize();
        ret.statups = statups;
        return ret;
    }

    private static MapleStatEffect loadFromData(MapleData source, int sourceid, boolean skill, boolean overTime) {
        MapleStatEffect ret = new MapleStatEffect();
        ret.sourceid = sourceid;
        ret.skill = skill;

        ret.duration = MapleDataTool.getIntConvert("time", source, -1);
        ret.hp = (short) MapleDataTool.getInt("hp", source, 0);
        ret.hpR = (MapleDataTool.getInt("hpR", source, 0) / 100.0D);
        ret.mp = (short) MapleDataTool.getInt("mp", source, 0);
        ret.mpR = (MapleDataTool.getInt("mpR", source, 0) / 100.0D);
        ret.mpCon = (short) MapleDataTool.getInt("mpCon", source, 0);
        ret.hpCon = (short) MapleDataTool.getInt("hpCon", source, 0);
        int iprop = MapleDataTool.getInt("prop", source, 100);
        ret.expinc = MapleDataTool.getInt("expinc", source, 0);
        ret.prop = (iprop / 100.0D);
        ret.mobCount = MapleDataTool.getInt("mobCount", source, 1);
        ret.cooldown = MapleDataTool.getInt("cooltime", source, 0);
        ret.morphId = MapleDataTool.getInt("morph", source, 0);
        ret.isGhost = (MapleDataTool.getInt("ghost", source, 0) != 0);
        //ret.fatigue = MapleDataTool.getInt("incFatigue", source, 0);

        if ((!ret.skill) && (ret.duration > -1)) {
            ret.overTime = true;
        } else {
            ret.duration *= 1000;
            ret.overTime = overTime;
        }
        ArrayList<Pair<MapleBuffStat, Integer>> statups = new ArrayList<Pair<MapleBuffStat, Integer>>();
        ret.watk = (short) MapleDataTool.getInt("pad", source, 0);
        ret.wdef = (short) MapleDataTool.getInt("pdd", source, 0);
        ret.matk = (short) MapleDataTool.getInt("mad", source, 0);
        ret.mdef = (short) MapleDataTool.getInt("mdd", source, 0);
        ret.acc = (short) MapleDataTool.getIntConvert("acc", source, 0);
        ret.avoid = (short) MapleDataTool.getInt("eva", source, 0);
        ret.speed = (short) MapleDataTool.getInt("speed", source, 0);
        ret.jump = (short) MapleDataTool.getInt("jump", source, 0);
        List cure = new ArrayList(5);
        if (MapleDataTool.getInt("poison", source, 0) > 0) {
            cure.add(MapleDisease.POISON);
        }
        if (MapleDataTool.getInt("seal", source, 0) > 0) {
            cure.add(MapleDisease.SEAL);
        }
        if (MapleDataTool.getInt("darkness", source, 0) > 0) {
            cure.add(MapleDisease.DARKNESS);
        }
        if (MapleDataTool.getInt("weakness", source, 0) > 0) {
            cure.add(MapleDisease.WEAKEN);
        }
        if (MapleDataTool.getInt("curse", source, 0) > 0) {
            cure.add(MapleDisease.CURSE);
        }
        ret.cureDebuffs = cure;
        if ((ret.overTime) && (ret.getSummonMovementType() == null) && !GameConstants.isAngelRingBuffer(sourceid)) {
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.WATK, Integer.valueOf(ret.watk));
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.WDEF, Integer.valueOf(ret.wdef));
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.MATK, Integer.valueOf(ret.matk));
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.MDEF, Integer.valueOf(ret.mdef));
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.ACC, Integer.valueOf(ret.acc));
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.AVOID, Integer.valueOf(ret.avoid));
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.SPEED, Integer.valueOf(ret.speed));
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.JUMP, Integer.valueOf(ret.jump));
        }
        if (GameConstants.isAngelRingBuffer(sourceid)) {
            statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.天使戒指, 1));
            statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.E_Watk, Integer.valueOf(ret.watk)));
            statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.E_Matk, Integer.valueOf(ret.matk)));
        }
        MapleData ltd = source.getChildByPath("lt");
        if (ltd != null) {
            ret.lt = ((Point) ltd.getData());
            ret.rb = ((Point) source.getChildByPath("rb").getData());
        }
        int x = MapleDataTool.getInt("x", source, 0);
        ret.x = x;
        ret.y = MapleDataTool.getInt("y", source, 0);
        ret.damage = MapleDataTool.getIntConvert("damage", source, 100);
        ret.attackCount = MapleDataTool.getIntConvert("attackCount", source, 1);
        ret.bulletCount = MapleDataTool.getIntConvert("bulletCount", source, 1);
        ret.bulletConsume = MapleDataTool.getIntConvert("bulletConsume", source, 0);
        ret.moneyCon = MapleDataTool.getIntConvert("moneyCon", source, 0);
        ret.itemCon = MapleDataTool.getInt("itemCon", source, 0);
        ret.itemConNo = MapleDataTool.getInt("itemConNo", source, 0);
        ret.moveTo = MapleDataTool.getInt("moveTo", source, -1);
        Map monsterStatus = new HashMap();
        if (skill) {
            switch (sourceid) {
                case 9001008://gm神圣之火
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.HYPERBODYHP, Integer.valueOf(ret.x)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.HYPERBODYMP, Integer.valueOf(ret.y)));
                    break;
                case 1001: // 新手 - 团队治疗
                case 10001001:
                case 20001001:
                case 20011001:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.RECOVERY, Integer.valueOf(ret.x)));
                    break;
                case 1002: // 新手 - 疾风步
                case 10001002:
                case 20001002:
                case 20011002:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SPEED, Integer.valueOf(ret.x)));
                    break;
                case 1004: // 新手 - 骑兽技能
                case 10001004:
                case 20001004:
                case 20011004:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.骑宠1, Integer.valueOf(1)));
                    break;
                case 1005: // 新手 - 英雄之回声
                case 10001005:
                case 20001005:
                case 20011005:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.ECHO_OF_HERO, Integer.valueOf(ret.x)));
                    break;
                case 1010: // 新手 - 金刚霸体
                case 10001010:
                case 20001010:
                case 20011010:
                    //statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.DIVINE_BODY, 1));
                    break;
                case 1011: // 新手 - 狂暴战魂
                case 10001011:
                case 20001011:
                case 20011011:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.BERSERK_FURY, Integer.valueOf(1)));
                    break;
                case 9001001: // 管理员 - 上乘轻功
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SPEED, Integer.valueOf(ret.speed)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.JUMP, Integer.valueOf(ret.jump)));
                    break;
                case 9001002: // 管理员 - 神圣祈祷
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.HOLY_SYMBOL, Integer.valueOf(ret.x)));
                    break;
                case 9001003: // 管理员 - 封印祝福
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.ACC, Integer.valueOf(ret.acc)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.AVOID, Integer.valueOf(ret.avoid)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.WATK, Integer.valueOf(ret.watk)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MATK, Integer.valueOf(ret.matk)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.WDEF, Integer.valueOf(ret.wdef)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MDEF, Integer.valueOf(ret.mdef)));
                    break;
                case 9001004: // 管理员 - 隐身术
                    ret.duration = 7200000;
                    ret.overTime = true;
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.DARKSIGHT, Integer.valueOf(ret.x)));
                    break;
                //潜入
                case 30001001:
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.潜入, Integer.valueOf(ret.x)));
                    statups.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SPEED, Integer.valueOf(ret.speed)));
                    break;
            }
        } else {//特殊物品关联技能效果。
            switch (sourceid) {
                case 2022125://灵魂祝福-物理防御力
                {
                    ret.duration = 100000;
                    ret.overTime = true;
                    statups.add(Pair.Create(MapleBuffStat.EWdef, 0));
                    break;
                }
                case 2022126://灵魂祝福-魔法防御力
                {
                    ret.duration = 100000;
                    ret.overTime = true;
                    statups.add(Pair.Create(MapleBuffStat.EMdef, 0));
                    break;
                }
                case 2022127://灵魂祝福-命中
                {
                    ret.duration = 100000;
                    ret.overTime = true;
                    statups.add(Pair.Create(MapleBuffStat.ACC, 0));
                    break;
                }
                case 2022128://灵魂祝福-回避
                {
                    ret.duration = 100000;
                    ret.overTime = true;
                    statups.add(Pair.Create(MapleBuffStat.AVOID, 0));
                    break;
                }
                case 2022129://灵魂祝福-攻击
                {
                    ret.duration = 100000;
                    ret.overTime = true;
                    statups.add(Pair.Create(MapleBuffStat.EWatk, 0));
                    break;
                }
                case 2003516: {
                    statups.add(
                            Pair.Create(MapleBuffStat.巨人药水,
                            (int) MapleDataTool.getInt("inflation", source, 0)));
                    ret.overTime = true;
                    ret.initForeign(new MapleForeignBuffByteStat(MapleBuffStat.SPEED), new MapleForeignBuffShortStat(MapleBuffStat.巨人药水));
                }
                break;
            }
        }

        if (ret.isMorph()) {
            statups.add(new Pair(MapleBuffStat.MORPH, Integer.valueOf(ret.getMorph())));
        }
        if ((ret.isGhost) && (!skill)) {
            statups.add(new Pair(MapleBuffStat.GHOST_MORPH, Integer.valueOf(1)));
        }
        ret.monsterStatus = monsterStatus;
        statups.trimToSize();
        ret.statups = statups;


        return ret;
    }

    /**
     * 魔力吸收。
     *
     * @param applyto
     * @param obj
     * @param attack damage done by the skill
     */
    public void applyPassive(MapleCharacter applyto, MapleMapObject obj, int attack) {
        if (makeChanceResult()) {
            switch (sourceid) {
                // MP eater
                case 2100000:
                case 2200000:
                case 2300000:
                    if (obj == null || obj.getType() != MapleMapObjectType.MONSTER) {
                        return;
                    }
                    MapleMonster mob = (MapleMonster) obj;
                    // x is absorb percentage
                    if (!mob.isBoss()) {
                        int absorbMp = Math.min((int) (mob.getMaxMp() * (getX() / 100.0)), mob.getMp());
                        if (absorbMp > 0) {
                            mob.setMp(mob.getMp() - absorbMp);
                            applyto.addMP(absorbMp);
                            //applyto.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(sourceid, 1));
                            //applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.showBuffeffect(applyto.getId(), sourceid, 1, (byte) 3, 1), false);
                        }
                    }
                    break;
            }
        }
    }

    public boolean applyTo(MapleCharacter chr) {
        return applyTo(chr, chr, true, null);
    }

    public boolean applyTo(MapleCharacter chr, Point pos) {
        return applyTo(chr, chr, true, pos);
    }

    private boolean applyTo(MapleCharacter applyfrom, MapleCharacter applyto, boolean primary, Point pos) {
        int hpchange = calcHPChange(applyto, applyfrom, primary);
        int mpchange = calcMPChange(applyto, applyfrom, primary);
        if (applyto.getJob().IsDemonHunter() && !skill) {
            mpchange = 0;
        }
        //log.debug("效果为玩家增加 HP ：" + hpchange + " ：倍率：" + hpR + " ：源ID：" + sourceid);
        if (expinc > 0) {
            applyto.gainExp(expinc, true, true);
        }
        if (primary) {
            if (itemConNo != 0) {
                MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(itemCon);
                MapleInventoryManipulator.removeById(applyto.getClient(), type, itemCon, itemConNo, false, true);
            }
        }
        if (cureDebuffs.size() > 0) {
            for (MapleDisease debuff : cureDebuffs) {
                applyfrom.dispelDebuff(debuff);
            }
        }
        List<Pair<MapleStat, Number>> hpmpupdate = new ArrayList<Pair<MapleStat, Number>>(2);
        if (!primary && isResurrection()) {
            hpchange = applyto.getMaxhp();
            applyto.setStance(0);
        }
        if (isDispel() && makeChanceResult()) {
            applyto.dispelDebuffs();
        }
        if (isHeroWill()) {
            applyto.cancelAllDebuffs();
        }
        if (hpchange != 0) {
            if (hpchange < 0 && (-hpchange) > applyto.getHp()) {
                return false;
            }
            int newHp = applyto.getHp() + hpchange;
            if (newHp < 1) {
                newHp = 1;
            }
            applyto.setHp(newHp);
            hpmpupdate.add(new Pair<MapleStat, Number>(MapleStat.HP, Integer.valueOf(applyto.getHp())));
        }
        if (mpchange != 0) {
            if (mpchange < 0 && (-mpchange) > applyto.getMp()) {
                return false;
            }
            applyto.setMp(applyto.getMp() + mpchange);
            hpmpupdate.add(new Pair<MapleStat, Number>(MapleStat.MP, Integer.valueOf(applyto.getMp())));
        }
        applyto.getClient().getSession().write(MaplePacketCreator.updatePlayerStats(hpmpupdate, true, applyto));

        if (moveTo != -1 && !skill) {
            MapleMap target = null;
            boolean nearest = false;
            if (moveTo == 999999999) {
                nearest = true;
                if (applyto.getMap().getReturnMapId() != 999999999) {
                    target = applyto.getMap().getReturnMap();
                }
            } else {
                target = applyto.getClient().getChannelServer().getMapFactory().getMap(moveTo);
                int targetMapId = target.getId() / 10000000;
                int charMapId = applyto.getMapId() / 10000000;
                if (targetMapId != 60 && charMapId != 61) {
                    if (targetMapId != 21 && charMapId != 20) {
                        if (targetMapId != 12 && charMapId != 10) {
                            if (targetMapId != 10 && charMapId != 12) {
                                if (targetMapId != charMapId) {
                                    log.info("人物 " + applyto.getName() + " 尝试回到一个非法的位置 ('" + applyto.getMapId() + "'->'" + target.getId() + "')");
                                    applyto.getClient().disconnect();
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
            if (target == applyto.getMap() || nearest && applyto.getMap().isTown()) {
                return false;
            }
        }

        if (isShadowClaw()) {
            MapleInventory use = applyto.getInventory(MapleInventoryType.USE);
            MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
            int projectile = 0;
            for (int i = 0; i < 255; i++) { // impose order...
                IItem item = use.getItem((byte) i);
                if (item != null) {
                    boolean isStar = mii.isThrowingStar(item.getItemId());
                    if (isStar && item.getQuantity() >= 200) {
                        projectile = item.getItemId();
                        break;
                    }
                }
            }
            if (projectile == 0) {
                return false;
            } else {
                MapleInventoryManipulator.removeById(applyto.getClient(), MapleInventoryType.USE, projectile, 200, false, true);
            }
        }

        if (overTime) {
            applyBuffEffect(applyfrom, applyto, primary);
        }

        if (primary && (overTime || isHeal())) {
            //组队处理
            applyBuff(applyfrom);
        }

        if (primary && isMonsterBuff()) {
            //给一定范围内的怪物施加buff
            applyMonsterBuff(applyfrom);
        }

        SummonMovementType summonMovementType = getSummonMovementType();
        if (summonMovementType != null && pos != null) {
            //如果存在这个召唤兽就先取消一次
            MapleSummon summon = applyfrom.getSummon(sourceid);
            if (!(summon == null
                    || summon.getSkill() == 机械师.磁场 && applyfrom.getSummonAmount(机械师.磁场) < 3
                    || summon.getSkill() == 弩骑.地雷_自爆
                    || summon.getSkill() == 机械师.机器人工厂_机器人)) {
                ////log.debug("在召唤召唤兽的时候检测到已经存在此召唤兽 先进行取消");
                applyfrom.getMap().broadcastMessage(MaplePacketCreator.removeSpecialMapObject(summon, true));
                applyfrom.getMap().removeMapObject(summon);
                applyfrom.removeVisibleMapObject(summon);
                applyfrom.getSummons().remove(summon.getSkill());
            }
            if (applyfrom.getMainsumons() != null && summonMovementType.ISFOLLOW() && (!GameConstants.isAngelRingSkill(sourceid))) {
                applyfrom.getMap().broadcastMessage(MaplePacketCreator.removeSpecialMapObject(applyfrom.getMainsumons(), true));
                applyfrom.getMap().removeMapObject(applyfrom.getMainsumons());
                applyfrom.removeVisibleMapObject(applyfrom.getMainsumons());
                applyfrom.getSummons().remove(applyfrom.getMainsumons().getSkill());
                applyfrom.setMainsumons(null);
            }
            if ((GameConstants.isAngelRingSkill(sourceid)) && applyfrom.getRingsumons() != null) {//戒指BB。
                applyfrom.getMap().broadcastMessage(MaplePacketCreator.removeSpecialMapObject(applyfrom.getRingsumons(), true));
                applyfrom.getMap().removeMapObject(applyfrom.getRingsumons());
                applyfrom.removeVisibleMapObject(applyfrom.getRingsumons());
                applyfrom.getSummons().remove(applyfrom.getRingsumons().getSkill());
                applyfrom.setRingsumons(null);
            }
            MapleSummon tosummon = new MapleSummon(applyfrom, sourceid, pos, summonMovementType);
            tosummon.setEffect(this);
            if (!tosummon.isPuppet()) {
                applyfrom.getCheatTracker().resetSummonAttack();
            }
            if (GameConstants.isAngelRingSkill(sourceid)) {
                tosummon.setStatus(0);
                applyfrom.setRingsumons(tosummon);
            }
            applyfrom.getMap().spawnSummon(tosummon);
            applyfrom.putSummon(sourceid, tosummon);
            if (summonMovementType.ISFOLLOW() && (!GameConstants.isAngelRingSkill(sourceid))) {//跟随类型。
                applyfrom.setMainsumons(tosummon);
            }
            tosummon.addHP(x);
            if (isBeholder()) {//灵魂助力
                tosummon.addHP(1);
            }
        } else if (isMagicDoor()) {
            Point doorPosition = new Point(applyto.getPosition());
            MapleDoor door = new MapleDoor(applyto, doorPosition);
            applyto.getMap().spawnDoor(door);
            applyto.addDoor(door);
            door = new MapleDoor(door);
            applyto.addDoor(door);
            door.getTown().spawnDoor(door);
            if (applyto.getParty() != null) {
                //更新组队
                applyto.silentPartyUpdate();
            }
            applyto.disableDoor();
        } else if (isMist()) {
            Rectangle bounds = calculateBoundingBox(applyfrom.getPosition(), applyfrom.isFacingLeft());
            MapleMist mist = new MapleMist(bounds, applyfrom, this);
            applyfrom.getMap().spawnMist(mist, getDuration(), false);
        } else if (isTimeLeap()) {
            for (PlayerCoolDownValueHolder i : applyto.getAllCooldowns()) {
                if (i.skillId != 5121010) {
                    applyto.removeCooldown(i.skillId);
                }
            }
        } else if (is传送门()) {
            if (applyto.getDoors2().size() >= 2) {
                for (MapleDoor2 olddoor2 : applyto.getDoors2()) {
                    if (olddoor2.isFirst() == firstDoor) {
                        if (olddoor2.isFirst()) {
                            applyto.cancelDoor2_1();
                        } else {
                            applyto.cancelDoor2_2();
                        }
                        applyto.getMap().broadcastMessage(MaplePacketCreator.取消传送门(applyto.getId(), olddoor2.isFirst()));
                        //log.info("取消的门："+olddoor2.isFirst());
                        applyto.getMap().removeMapObject(olddoor2);
                        applyto.getDoors2().remove(olddoor2);
                        break;
                    }
                }
            }
            MapleDoor2 door2 = new MapleDoor2(applyto, applyto.getPosition(), firstDoor);
            applyto.addDoor2(door2);
            applyto.getMap().spawnDoor2(door2);
            applyto.cancelDoor2(door2, this.duration); //设置多久时间后取消
            if (firstDoor) {
                firstDoor = false;
            } else {
                firstDoor = true;
            }
        }

        if (isHide()) {
            if (applyto.isHidden()) {
                // applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.removePlayerFromMap(applyto.getId()), false);
                for (MapleCharacter mapleCharacter : applyto.getMap().getCharacters()) {
                    applyto.sendDestroyData(mapleCharacter.getClient());
                }
            } else {
                /*
                 * applyto.getMap().broadcastMessage(applyto,
                 * MaplePacketCreator.spawnPlayerMapobject(applyto), false); for
                 * (MaplePet pet : applyto.getPets()) { if (pet != null) {
                 * applyto.getMap().broadcastMessage(applyto,
                 * MaplePacketCreator.showPet(applyto, pet, false, false),
                 * false); } }
                 */
                for (MapleCharacter mapleCharacter : applyto.getMap().getCharacters()) {
                    applyto.sendSpawnData(mapleCharacter.getClient());
                }
            }
        }
        return true;
    }

    public boolean applyReturnScroll(MapleCharacter applyto) {
        if (moveTo != -1 && !skill) {
            //if (applyto.getMap().getReturnMapId() != applyto.getMapId()) {
            MapleMap target;
            if (moveTo == 999999999) {
                target = applyto.getMap().getReturnMap();
            } else {
                target = applyto.getClient().getChannelServer().getMapFactory().getMap(moveTo);
                if (target.getId() / 10000000 != 60 && applyto.getMapId() / 10000000 != 61) {
                    if (target.getId() / 10000000 != 21 && applyto.getMapId() / 10000000 != 20) {
                        if (target.getId() / 10000000 != applyto.getMapId() / 10000000) {
                            return false;
                        }
                    }
                }
            }
            applyto.changeMap(target, target.getPortal(0));
            return true;
            // }
        }
        return false;
    }

    private List<MapleMapObject> getMonsterFormBouningBox(MapleCharacter applyfrom) {
        Rectangle bounds = calculateBoundingBox(applyfrom.getPosition(), applyfrom.isFacingLeft());
        return applyfrom.getMap().getMapObjectsInRect(bounds, MapleMapObjectType.MONSTER);
    }

    private void applyBuff(MapleCharacter applyfrom) {
        if (isPartyBuff() && (applyfrom.getParty() != null || isGMBuff())) {
            Rectangle bounds = calculateBoundingBox(applyfrom.getPosition(), applyfrom.isFacingLeft());
            List<MapleMapObject> affecteds = applyfrom.getMap().getMapObjectsInRect(bounds, MapleMapObjectType.PLAYER);
            List<MapleCharacter> affectedp = new ArrayList<MapleCharacter>(affecteds.size());
            for (MapleMapObject affectedmo : affecteds) {
                MapleCharacter affected = (MapleCharacter) affectedmo;
                //this is new and weird...
                if (affected != null && isHeal() && affected != applyfrom && affected.getParty() == applyfrom.getParty() && affected.isAlive()) {
                    int expadd = (int) ((calcHPChange(null, applyfrom, true) / 10) * (applyfrom.getClient().getChannelServer().getExpRate() + ((Math.random() * 10) + 30)) * (Math.floor(Math.random() * (applyfrom.getSkillLevel(SkillFactory.getSkill(2301002))) / 100) * (applyfrom.getLevel() / 30)));
                    if (affected.getHp() < affected.getMaxhp() - affected.getMaxhp() / 20) {
                        applyfrom.gainExp(expadd, true, false, false);
                    }
                }
                if (affected != applyfrom && (isGMBuff() || applyfrom.getParty().equals(affected.getParty()))) {
                    boolean isRessurection = isResurrection();
                    if ((isRessurection && !affected.isAlive()) || (!isRessurection && affected.isAlive())) {
                        affectedp.add(affected);
                    }
                    if (isTimeLeap()) {
                        for (PlayerCoolDownValueHolder i : affected.getAllCooldowns()) {
                            if (i.skillId != 5121010) { //伺机待发
                                affected.removeCooldown(i.skillId);
                            }
                        }
                    }
                }
            }
            for (MapleCharacter affected : affectedp) {
                // TODO actually heal (and others) shouldn't recalculate everything
                // for heal this is an actual bug since heal hp is decreased with the number
                // of affected players
                applyTo(applyfrom, affected, false, null);
                affected.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(sourceid, 2));
                affected.getMap().broadcastMessage(affected, MaplePacketCreator.showBuffeffect(affected.getId(), sourceid, 2, (byte) 3, 1), false);
            }
        }
    }

    private void applyMonsterBuff(MapleCharacter applyfrom) {
        Rectangle bounds = calculateBoundingBox(applyfrom.getPosition(), applyfrom.isFacingLeft());
        List<MapleMapObject> affected = applyfrom.getMap().getMapObjectsInRect(bounds, Arrays.asList(MapleMapObjectType.MONSTER));
        ISkill skill_ = SkillFactory.getSkill(sourceid);
        int i = 0;
        for (MapleMapObject mo : affected) {
            MapleMonster monster = (MapleMonster) mo;
            if (makeChanceResult()) {
                monster.applyStatus(applyfrom, new MonsterStatusEffect(getMonsterStati(), skill_, false), isPoison(), getDuration());
            }
            i++;
            if (i >= mobCount) {
                break;
            }
        }
    }

    public Rectangle calculateBoundingBox(Point posFrom, boolean facingLeft) {
        Point mylt;
        Point myrb;
        if (facingLeft) {
            //log.debug("facingLeft为真");
            mylt = new Point(lt.x + posFrom.x, lt.y + posFrom.y);
            myrb = new Point(rb.x + posFrom.x, rb.y + posFrom.y);
        } else {
            //log.debug("facingLeft为假");
            myrb = new Point(lt.x * -1 + posFrom.x, rb.y + posFrom.y);
            mylt = new Point(rb.x * -1 + posFrom.x, lt.y + posFrom.y);
        }
        Rectangle bounds = new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
        return bounds;
    }

    public void silentApplyBuff(MapleCharacter chr, long starttime, List<Pair<MapleBuffStat, Integer>> localstat) {
        int localDuration = duration;
        localDuration = alchemistModifyVal(chr, localDuration, false);

        /*    CancelEffectAction cancelAction = new CancelEffectAction(chr, this, starttime);
         ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, ((starttime + localDuration) - System.currentTimeMillis()));
         chr.registerEffect(this, starttime, schedule, localstat);*/
        registryToPlayer(chr, ((starttime + localDuration) - System.currentTimeMillis()), localstat);

        SummonMovementType summonMovementType = getSummonMovementType();
        if (summonMovementType != null) {
            final MapleSummon tosummon = new MapleSummon(chr, sourceid, chr.getPosition(), summonMovementType);
            if (!tosummon.isPuppet()) {
                chr.getCheatTracker().resetSummonAttack();
                //chr.getSummons().put(sourceid, tosummon);
                chr.putSummon(sourceid, tosummon);
                tosummon.addHP(x);
            }
        }
    }

    public boolean isMonsterRiding() {
        return isMonsterRiding(sourceid);
    }

    public static boolean isMonsterRiding(int id) {
        return isNotItemMount(id) || isNormalMonsterRiding(id);
    }

    public static boolean isNormalMonsterRiding(int sourceid) {
        return sourceid == 1004
                || sourceid == 10001004
                || sourceid == 20001004
                || sourceid == 20011004
                || sourceid == 30001004;
    }

    public boolean isNotCancelBuffFirst(int skillid) {
        switch (skillid) {
            case 3211002: // 射手 - 替身术
            case 3111002: // 游侠 - 替身术
            case 13111004:// 风灵使者 - 替身术
            case 5211001: // 大幅 - 章鱼炮台
            case 5220002: // 船长 - 超级章鱼炮台
            case 4341006: // 暗影双刀 - 傀儡召唤
            case 4211007: // 独行客 - 黑暗杂耍
            case 4111007: // 无影人 - 黑暗杂耍
            case 35111002: // 机械师 - 磁场
            case 35111005: // 机械师 - 加速器：EX-7
            case 35111011: // 机械师 - 治疗机器人：H-LX
            case 35121003: // 机械师 - 战争机器：泰坦
            case 35121009: // 机械师 - 机器人工厂：RM1
            case 35121010: // 机械师 - 放大器：AF-11
            case 33101008: // 弩骑 - 地雷 自爆
            case 33111003: // 弩骑 野性陷阱
            case 3201007: // 射手 - 金鹰召唤
            case 3101007: // 游侠 - 银鹰召唤
            //case 2311006: // 祭司 - 圣龙召唤
            case 3221005: // 神射手 - 冰凤凰
            case 3121006: // 箭神 - 火凤凰
            case 5211002: // 大幅 - 海鸥空袭
            case 33111005: // 弩骑 银鹰
            case 35121011: // 机器人工厂召唤技能
            case 32111006: //幻灵 重生
            case 1321007: // 黑骑士 - 灵魂助力
            case 2121005: // 火毒导师 - 冰破魔兽
            case 2221005: // 冰雷导师 - 火魔兽
            case 2321003: // 主教 - 强化圣龙
            case 11001004: // 魂骑士 - 魂精灵
            case 12001004: // 炎术士 - 炎精灵
            case 13001004: // 风灵使者 - 风精灵
            case 14001005: // 夜行者  - 夜精灵
            case 15001004: // 奇袭者 - 雷精灵
            case 12111004: // 炎术士 - 火魔兽
            case 35111001: // 机械师 - 人造卫星
            case 35111009: // 机械师 - 人造卫星
            case 35111010: // 机械师 - 人造卫星
            case 35001001://喷火器
            case 战法.进阶黑暗灵气:
            case 战法.进阶蓝色灵气:
            case 战法.进阶黄色灵气:
            case 1085:
            case 1087:
            case 1090:
            case 1179:
                return true;
        }
        return false;
    }

    public boolean isGiveForeignBuff() {
        return isDs()
                || isCombo()
                || isShadowPartner()
                || isSoulArrow()
                || isMorph()
                || isPirateMorph() //|| isLingQi()
                ;
    }

    /**
     * 技能系骑宠
     *
     * @param id
     * @return
     */
    public static boolean isNotItemMount(int id) {
        if (getMountID(id) == -1) {
            return false;
        }
        return true;
    }

    public static int getMountID(int skillmod) {
        switch (skillmod) {
            case 1013:
            case 10001014:
            case 20001046:
            case 20011046:
            case 30001013:
                return 1932001; //宇宙船
            case 1015:
            case 10001016:
            case 30001015:
                return 1932002; //太空射线
            case 1017:
            case 1018:
            case 1050:
            case 10001019:
            case 10001022:
            case 10001050:
            case 20001019:
            case 20001022:
            case 20001050:
            case 20011018:
            case 30001017:
            case 30001018:
                return 1932003; //白雪人骑宠
            case 1019:
            case 10001023:
            case 20001023:
            case 20011019:
            case 30001019:
            case 80001026:
                return 1932005; //魔女的扫把
            case 1025:
            case 10001025:
            case 20001025:
            case 20011025:
            case 30001025:
            case 80001003:
                return 1932006; //突击！木马
            case 1027:
            case 10001027:
            case 20001027:
            case 20011027:
            case 30001027:
            case 80001004:
                return 1932007; //鳄鱼
            case 1028:
            case 10001028:
            case 20001028:
            case 20011028:
            case 30001028:
                return 1932008; //透明自行车
            case 1029:
            case 10001029:
            case 20001029:
            case 20011029:
            case 30001029:
                return 1932009; //粉色电单车
            case 1030:
            case 10001030:
            case 20001030:
            case 20011030:
            case 30001030:
            case 80001007:
                return 1932011; //筋斗云
            case 1031:
            case 10001031:
            case 20001031:
            case 20011031:
            case 30001031:
            case 80001008:
                return 1932010; //蝙蝠怪
            case 1033:
            case 10001033:
            case 20001033:
            case 20011033:
            case 30001033:
            case 80001009:
                return 1932013; //可以开着赛车移动。
            case 1034:
            case 10001034:
            case 20001034:
            case 20011034:
            case 30001034:
            case 80001010:
                return 1932014; //虎哥
            case 1035:
            case 10001035:
            case 20001035:
            case 20011035:
            case 30001035:
            case 80001011:
                return 1932012; //蝙蝠魔先生
            case 1036:
            case 10001036:
            case 20001036:
            case 20011036:
            case 30001036:
            case 80001045:
                return 1932017; //狮子王
            case 1037:
            case 10001037:
            case 20001037:
            case 20011037:
            case 30001037:
                return 1932018; //独角兽
            case 1039:
            case 10001039:
            case 20001039:
            case 20011039:
            case 30001039:
            case 80001048:
                return 1932020; //田园红卡车
            case 1040:
            case 10001040:
            case 20001040:
            case 20011040:
            case 30001040:
            case 80001049:
                return 1932021; //恶魔石像
            case 1042:
            case 10001042:
            case 20001042:
            case 20011042:
            case 30001042:
            case 80001012:
                return 1932022; //圣兽 提拉奥斯
            case 1044:
            case 10001044:
            case 20001044:
            case 20011044:
            case 30001044:
            case 80001013:
                return 1932023; //花蘑菇
            case 1049:
            case 10001049:
            case 20001049:
            case 20011049:
            case 30001049:
            case 80001014:
                return 1932025; //梦魇
            case 1051:
            case 10001051:
            case 20001051:
            case 20011051:
            case 30001051:
            case 80001015:
                return 1932026; //鸵鸟
            case 1052:
            case 10001052:
            case 20001052:
            case 20011052:
            case 30001052:
            case 80001016:
                return 1932027; //热气球
            case 1053:
            case 10001053:
            case 20001053:
            case 20011053:
            case 30001053:
            case 80001017:
                return 1932028; //变形金刚
            case 1063:
            case 10001063:
            case 20001063:
            case 20011063:
            case 30001063:
            case 80001018:
                return 1932034; //摩托车
            case 1064:
            case 10001064:
            case 20001064:
            case 20011064:
            case 30001064:
            case 80001019:
                return 1932035; //超能套装
            case 1069:
            case 10001069:
            case 20001069:
            case 20011069:
            case 30001069:
            case 80001031:
                return 1932038; //猫头鹰
            case 1096:
            case 10001096:
            case 20001096:
            case 20011096:
            case 30001096:
            case 80001054:
                return 1932045; //巨无霸兔子
            case 1101:
            case 10001101:
            case 20001101:
            case 20011101:
            case 30001101:
            case 80001055:
                return 1932046; //兔兔加油
            case 1102:
            case 10001102:
            case 20001102:
            case 20011102:
            case 30001102:
            case 80001056:
                return 1932047; //兔子车夫
            case 1106:
            case 10001106:
            case 20001106:
            case 20011106:
            case 30001106:
            case 80001023:
                return 1932048; //福袋
            case 1118:
            case 10001118:
            case 20001118:
            case 20011118:
            case 30001118:
                return 1932060; //妮娜的魔法阵
            case 1054:
            case 10001054:
            case 20001054:
            case 20011054:
            case 30001054:
                return 1932062; //走路鸡
            case 1121:
            case 10001121:
            case 20001121:
            case 20011121:
            case 30001121:
            case 80001059:
                return 1932063; //青蛙
            case 1122:
            case 10001122:
            case 20001122:
            case 20011122:
            case 30001122:
            case 80001060:
                return 1932064; //小龟龟
            case 1123:
            case 10001123:
            case 20001123:
            case 20011123:
            case 30001123:
                return 1932065; //无辜水牛
            case 1124:
            case 10001124:
            case 20001124:
            case 20011124:
            case 30001124:
                return 1932066; //玩具坦克
            case 1129:
            case 10001129:
            case 20001129:
            case 20011129:
            case 30001129:
            case 80001061:
                return 1932071; //维京战车
            case 1130:
            case 10001130:
            case 20001130:
            case 20011130:
            case 30001130:
                return 1932072; //打豆豆机器人
            case 1136:
            case 10001136:
            case 20001136:
            case 20011136:
            case 30001136:
                return 1932078; //莱格斯的豺犬
            case 1138:
            case 10001138:
            case 20001138:
            case 20011138:
            case 30001138:
            case 80001032:
                return 1932080; //跑车
            case 1139:
            case 10001139:
            case 20001139:
            case 20011139:
            case 30001139:
            case 80001062:
                return 1932081; //拿破仑的白马
            case 1158:
            case 10001158:
            case 20001158:
            case 20011158:
            case 30001158:
                return 1932083; //机动巡逻车(准乘4人)
            case 1148:
            case 10001148:
            case 20001148:
            case 20011148:
            case 30001148:
                return 1992005; //暗光龙
            case 5221006:
                return 1932000; //武装
            case 35001002:
                return 1932016; //金属机甲 原型
            case 33001001:
                return 1932015; //美洲豹骑士
            case 20021160://希比迪亚
                return 1932086;
            case 30011109://恶魔之翼
                return 1932051;
            case 80001005://男男机车
                return 1932008;
            case 80001006://女女机车
                return 1932009;
            case 80001020://雄狮骑宠
                return 1932041;
            case 80001021://蓝色电单车
                return 1932043;
            case 80001022://圣诞雪橇
                return 1932044;
            case 80001027://木飞机
                return 1932049;
            case 80001028://红飞机
                return 1932050;
            case 80001029://骑士团战车
                return 1932053;
            case 80001030://走路鸡
                return 1932029;
            case 80001033://休彼德蔓的热气球
                return 1932057;
            case 80001037://比约
                return 1932084;
            case 80001038://毛线骑士团告诉战车
                return 1902031;
            case 80001117:
                return 1932072;
            case 80001075:
                return 1902042;
            case 80001090:
                return 1932096;
            case 80001118:
                return 1902048;
            case 80001051:
                return 1932055;
            case 80001025:
                return 1932004;
            case 80001067:
                return 1992004;
            case 80001068:
                return 1992005;
            case 80001115:
                return 1932093;
            case 80001066:
                return 1992003;
            case 80001121:
                return 1932092;
            case 80001120:
                return 1992015;
            case 80001112:
                return 1932097;
            case 80001113:
                return 1932098;
            case 80001114:
                return 1932099;
            case 80001124:
                return 1932105;
            case 80001127:
                return 1932108;
            case 80001131:
                return 1932109;
            case 80001142:
                return 1932112;
            case 80001077:
                return 1902014;
            case 80001116:
                return 1932066;
            case 80001058:
                return 1932060;
            case 80001039:
                return 1932089;
            case 80001078:
                return 1932083;
            case 80001119:
                return 1932080;
            case 80001181:
                return 1932091;
            case 80001144:
                return 1932113;
            case 80001148:
                return 1932114;
            case 80001149:
                return 1932115;
            default:
                return -1;
        }
    }

    private void applyBuffEffect(final MapleCharacter applyfrom, final MapleCharacter applyto, boolean primary) {
        if (!MapleStatEffect.isMonsterRiding(sourceid) && !isNotCancelBuffFirst(sourceid) && !(sourceid >= 2022125 && sourceid <= 2022129)) {
            ////log.debug("在givebuff之前先cancel一下 防止已有buff又被give一次");
            applyto.cancelEffect(this, true, -1);
        }

        List<Pair<MapleBuffStat, Integer>> localstatups = statups;
        int localDuration = duration;
        int localsourceid = sourceid;
        int skillid;


        MapleSkillEffectEvent.EffectEventData effectEventData = new MapleSkillEffectEvent.EffectEventData(localstatups, localDuration);
        MapleSkillEffectEvent.OnApplyBuffEffect(this, applyfrom, applyto, primary, effectEventData);
        localstatups = effectEventData.getStatups();
        localDuration = effectEventData.getDuration();

        if (powerCon > 0) {
            applyfrom.reducePower(powerCon);
        }

        if (GameConstants.isAngelRingSkill(sourceid)) {
            if (applyto.getRingFuture() != null) {
                applyto.getRingFuture().cancel(true);
                applyto.setRingFuture(null);
            }
            final MapleStatEffect.DelayAngelRingEffectAction dr = new MapleStatEffect.DelayAngelRingEffectAction(applyto, sourceid);
            dr.SetScheduledFuture(TimerManager.getInstance().register(new Runnable() {
                @Override
                public void run() {
                    dr.run();
                }
            }, 630000));
            applyto.setRingFuture(dr.getSf());
        }

        MapleStatEffect effect = applyto.getStatForBuff(MapleBuffStat.魔力精通);
        if (effect != null) {
            localDuration *= (1 + (effect.x / 100.0));
        }


        if (isMonsterRiding(sourceid)) {
            int ridingitemid = 0; //骑宠的itemid
            IItem mount = applyfrom.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18);
            if (mount != null && isNormalMonsterRiding(sourceid)) {
                ridingitemid = mount.getItemId();
            } else if (isNotItemMount(sourceid)) {
                ridingitemid = getMountID(sourceid);
            }
            int itemid = ridingitemid;
            skillid = sourceid;
            localDuration = 2100000000; //让持续时间无限长
            applyto.setUsingMount(itemid, skillid);
            initForeign(new MapleForeignBuffNoStat(MapleBuffStat.坐骑状态));//让其他玩家看见
            onoffSkill = true;
            //applyto.getMount().startSchedule(); //骑宠疲劳
            localstatups = Collections.singletonList(Pair.Create(MapleBuffStat.坐骑状态, itemid));
        } else if (is骰子()) {
            int type = new Random().nextInt(6) + 1;
            if (type == 1) {
                applyto.dropMessage("骰子摇出了1点。什么都没有变化。");
                return;
            }
            localstatups = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.幸运骰子, type));
        }

        if (primary) {
            localDuration = alchemistModifyVal(applyfrom, localDuration, false);
        }

        if (localstatups.size() > 0 && !isSummon())//Send Packet CreateBuff
        {
            if ((isDash() || isTornado()) && applyfrom.getBuffedValue(MapleBuffStat.疾驰_龙卷风) == null) {
                ////log.debug("龙卷风/疾驰"+localstatups);
                // applyto.getClient().getSession().write(MaplePacketCreator.giveDashOrTornado(localstatups, sourceid, localDuration / 1000));
            } else if (isInfusion()) {
                //log.debug("极速领域");
                //    applyto.getClient().getSession().write(MaplePacketCreator.giveInfusion(sourceid, localDuration / 1000, x));
            } /*
             * else if (isMonsterRiding(sourceid)) { //log.debug("骑宠");
             * applyto.getClient().getSession().write(MaplePacketCreator.giveMonsterRidingBuff(applyto,
             * itemid, skillid, localstatups, localDuration)); }
             */ else if (isSkillBianShen()) {
                //log.debug("变身技能");
                //         applyto.getClient().getSession().write(MaplePacketCreator.giveSkillBianShen(applyto, localsourceid, localDuration, localstatups));
            } else if (is能量获得()) {
                //log.debug("能量获得技能");
                //        applyto.getClient().getSession().write(MaplePacketCreator.givePirateBuff(0, localDuration, localstatups));
            } else if (is骰子()) {
                ////log.debug("幸运骰子技能 点数："+localstatups.get(0).getRight());
                //  applyto.getClient().getSession().write(MaplePacketCreator.骰子(localsourceid, localstatups, localDuration, localstatups.get(0).getRight(), applyto.getSkillLevel(5320007), applyto));
            } else {
                ////log.debug("普通技能");
                //      if (!skill || isMonsterRiding(sourceid)) {
                applyto.getClient().getSession().write(MaplePacketCreator.giveBuff((skill ? localsourceid : -localsourceid), localDuration, localstatups, applyto));
                //      }
            }
        }

        /*
         * if (isMonsterRiding(sourceid)) { List<Pair<MapleBuffStat, Integer>>
         * stat = Collections.singletonList(new Pair<MapleBuffStat,
         * Integer>(MapleBuffStat.骑宠1, 1));
         * applyto.getMap().broadcastMessage(applyto,
         * MaplePacketCreator.showMonsterRiding(applyto, stat, itemid, skillid,
         * localDuration), false); } else
         */
        if (isGiveForeignBuff()) { //总判断 记得要在下面出现的判断记得先在这里加
            List<Pair<MapleBuffStat, Integer>> stat = null;
            if (isDs()) {
                stat = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.DARKSIGHT, 0));
            } else if (isCombo()) {
                stat = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.COMBO, 1));
            } else if (isShadowPartner()) {
                stat = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SHADOWPARTNER, 0));
            } else if (isSoulArrow()) {
                stat = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SOULARROW, 0));
            } else if (isMorph()) {
                stat = Collections.singletonList(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MORPH, morphId));
            } else if (isPirateMorph()) {
                stat = new ArrayList<Pair<MapleBuffStat, Integer>>();
                stat.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.SPEED, Integer.valueOf(speed)));
                stat.add(new Pair<MapleBuffStat, Integer>(MapleBuffStat.MORPH, Integer.valueOf(morphId)));
            }
            if (stat != null) //log.debug("stat == null");
            {
                //           applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.giveForeignBuff(applyto, stat, this), false);
            }
            //     localstatups = stat;
        } else if (isEnrage()) {
            applyto.handleOrbconsume();
        } else if (isTimeLeap()) {
            for (PlayerCoolDownValueHolder i : applyto.getAllCooldowns()) {
                if (i.skillId != 5121010) {
                    applyto.removeCooldown(i.skillId);
                }
            }
        } else if (isLingQi()) {
            MapleBuffStat stat2 = null;
            if (sourceid == 战法.黑暗灵气 || sourceid == 战法.进阶黑暗灵气) {
                stat2 = MapleBuffStat.黑暗灵气;
            } else if (sourceid == 战法.蓝色灵气 || sourceid == 战法.进阶蓝色灵气) {
                stat2 = MapleBuffStat.蓝色灵气;
            } else if (sourceid == 战法.黄色灵气 || sourceid == 战法.进阶黄色灵气) {
                stat2 = MapleBuffStat.黄色灵气;
            }
            //           applyto.getClient().getSession().write(MaplePacketCreator.giveBuff(localsourceid, localDuration, Collections.singletonList(new Pair<MapleBuffStat, Integer>(stat2, x))));
        }

        if ((localstatups != null && localstatups.size() > 0) && (!isInfusion())) //广播效果
        {
            //        applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.giveForeignBuff(applyto, localstatups, this), false);
        }
        if ((localstatups != null && localstatups.size() > 0) && refreshstyle) {
            //       if (!skill || isMonsterRiding(sourceid)) {
            applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.giveForeignBuff(applyto, localstatups, this), false);
            //       }
        }

        if (((localstatups.size() > 0 || getSummonMovementType() != null) //&& !isMonsterRiding(sourceid) //   && !skill
                ) // && !isMonsterRiding(sourceid)
                ) {
            registryToPlayer(applyto, localDuration, localstatups);
        }



        if (primary && !isHide()) {
            if (isDash()) {
                //applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.showDashEffecttoOthers(applyto.getId(), localstatups, localDuration / 1000), false);
            }/*
             * else if (isInfusion()) { log.info("编写封包。");
             * //applyto.getMap().broadcastMessage(applyto,
             * MaplePacketCreator.giveForeignInfusion(applyto.getId(), sourceid,
             * x, localDuration / 1000), false);
             * applyto.getMap().broadcastMessage(applyto,
             * MaplePacketCreator.综合技能状态(applyto.getId(), 1, sourceid,
             * applyto.getSkillLevel(sourceid), 0x94), false); }
             */ else if (is骰子()) {
                //      applyto.getClient().getSession().write(MaplePacketCreator.机械技能特效(3, localstatups.get(0).getRight(), sourceid, applyfrom.getSkillLevel(sourceid), 0, -1));
                //        applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.机械技能特效(applyto.getId(), 3, localstatups.get(0).getRight(), sourceid, applyto.getSkillLevel(sourceid), 0, -1));
            } else {
                applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.showBuffeffect(applyto.getId(), sourceid, 1, (byte) 3, 1), false);
            }
        }
    }

    private void registryToPlayer(MapleCharacter applyto, long localDuration, List<Pair<MapleBuffStat, Integer>> localstatups) {
        long starttime = System.currentTimeMillis();
        ScheduledFuture<?> schedule = null;
        if (!onoffSkill) {
            CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime);
            schedule = TimerManager.getInstance().schedule(cancelAction, localDuration);
        }
        applyto.registerEffect(this, starttime, schedule, localstatups);
    }

    private int calcHPChange(MapleCharacter applyto, MapleCharacter applyfrom, boolean primary) {
        int hpchange = 0;
        if (hp != 0) {
            if (!skill) {
                if (primary) {
                    hpchange += alchemistModifyVal(applyfrom, hp, true);
                } else {
                    hpchange += hp;
                }
            } else { // assumption: this is heal
                hpchange += makeHealHP(hp / 100.0, applyfrom.getTotalMagic(), 3, 5);
            }
        }
        if (hpR != 0) {
            hpchange += (int) (applyfrom.getCurrentMaxHp() * hpR);
            //applyfrom.checkBerserk();
        }
        if (applyto != null && applyfrom.hasBufferStat(MapleBuffStat.神圣魔法盾) && !primary) {
            hpchange = (int) (applyto.getCurrentMaxHp() * hpR);
        }
        if (primary) {
            if (hpCon != 0) {
                hpchange -= hpCon;
            }
        }
        if (isChakra()) {
            hpchange += makeHealHP(getY() / 100.0, applyfrom.getTotalLuk(), 2.3, 3.5);
        }
        if (isPirateMpRecovery()) {
            hpchange -= ((getX() / 100.0) * applyfrom.getCurrentMaxHp());
        }
        return hpchange;
    }

    private int makeHealHP(double rate, double stat, double lowerfactor, double upperfactor) {
        int maxHeal = (int) (stat * upperfactor * rate);
        int minHeal = (int) (stat * lowerfactor * rate);
        return (int) ((Math.random() * (maxHeal - minHeal + 1)) + minHeal);
    }

    private int calcMPChange(MapleCharacter applyto, MapleCharacter applyfrom, boolean primary) {
        int mpchange = 0;
        if (mp != 0) {
            if (primary) {
                mpchange += alchemistModifyVal(applyfrom, mp, true);
            } else {
                mpchange += mp;
            }
        }
        if (mpR != 0) {
            mpchange += (int) (applyfrom.getCurrentMaxMp() * mpR);
        }
        if (primary) {
            if (mpCon != 0) {
                double mod = 1.0;
                boolean isAFpMage = applyfrom.getJob().isA(MapleJob.FP_MAGE);
                if (isAFpMage || applyfrom.getJob().isA(MapleJob.IL_MAGE)) {
                    ISkill amp;
                    if (isAFpMage) {
                        amp = SkillFactory.getSkill(2110001);
                    } else {
                        amp = SkillFactory.getSkill(2210001);
                    }
                    int ampLevel = applyfrom.getSkillLevel(amp);
                    if (ampLevel > 0) {
                        MapleStatEffect ampStat = amp.getEffect(ampLevel);
                        mod = ampStat.getX() / 100.0;
                    }
                }
                mpchange -= mpCon * mod;
                if (applyfrom.getBuffedValue(MapleBuffStat.INFINITY) != null) {
                    mpchange = 0;
                }
            }
        }
        if (isPirateMpRecovery()) {
            mpchange += (((getY() * getX()) / 10000.0) * applyfrom.getCurrentMaxHp());
        }
        return mpchange;
    }

    private int alchemistModifyVal(MapleCharacter chr, int val, boolean withX) {
        if (!skill && (chr.getJob().isA(MapleJob.HERMIT) || chr.getJob().isA(MapleJob.NIGHTLORD))) {
            MapleStatEffect alchemistEffect = getAlchemistEffect(chr);
            if (alchemistEffect != null) {
                return (int) (val * ((withX ? alchemistEffect.getX() : alchemistEffect.getY()) / 100.0));
            }
        }
        return val;
    }

    private MapleStatEffect getAlchemistEffect(MapleCharacter chr) {
        ISkill alchemist = SkillFactory.getSkill(4110000);
        int alchemistLevel = chr.getSkillLevel(alchemist);
        if (alchemistLevel == 0) {
            return null;
        }
        return alchemist.getEffect(alchemistLevel);
    }

    public void setSourceId(int newid) {
        sourceid = newid;
    }

    public void setLt(Point Lt) {
        lt = Lt;
    }

    public void setRb(Point Rb) {
        rb = Rb;
    }

    private boolean isGMBuff() {
        if (!skill) {
            return false;
        }
        switch (sourceid) {
            case 1005: // echo of hero acts like a gm buff
            case 9001000:
            case 9001001:
            case 9001002:
            case 9001003:
            case 9001005:
            case 9001008:
                return true;
            default:
                return false;
        }
    }

    private boolean isMonsterBuff() {
        //一定范围内给怪物Buff的技能 而且要有几率 即prop节点 否则直接在specialMove类获取Monster oid处理
        if (!skill) {
            return false;
        }
        switch (sourceid) {
            case 1201006: // threaten
            case 2101003: // fp slow
            case 2201003: // il slow
            case 2211004: // il seal
            case 2111004: // fp seal
            case 2311005: // doom
            case 4111003: // shadow web
            //case 4121004: // Ninja ambush
            //case 4421004: // Ninja ambush
            case 1111007: //魔击无效
            case 1211009:
            case 1311007:
                return true;
        }
        return false;
    }

    private boolean isPartyBuff() {
        if (lt == null || rb == null || !skill) {
            return false;
        }
        //如果技能有范围 但是不是组队技能的话就在这里false
        switch (sourceid) {
            case 骑士.火焰冲击:
            case 骑士.寒冰冲击:
            case 骑士.雷鸣冲击:
            case 骑士.神圣冲击:
            case 冰雷.快速移动精通:
            case 火毒.快速移动精通:
            case 牧师.快速移动精通:
            case 战法.快速移动精通:
            case 机械师.火焰喷射器:
            case 机械师.强化火焰喷射器:
            case 机械师.金属机甲_导弹战车:
            case 双刀.暗影轻功:
            case 双刀.终极斩:
            case 双刀.荆棘:
            case 双刀.死亡猫头鹰:
            case 战神.冰雪矛:
            case 弩骑.吞噬_消化:
            case 魂骑士.灵魂属性:
            case 战法.飓风:
            case 奇袭者.雷鸣:
            case 61120007:
                return false;
        }
        return true;
    }

    public boolean isHeal() {
        return skill && (sourceid == 2301002 || sourceid == 9001000);
    }

    public boolean isResurrection() {
        return skill && (sourceid == 9001005 || sourceid == 2321006);
    }

    public boolean isTimeLeap() {
        return skill && (sourceid == 5121010);
    }

    private boolean isInfusion() {
        return skill && (sourceid == 5121009 || sourceid == 15111005);
    }

    public int getDuration() {
        return duration;
    }

    public boolean isOverTime() {
        return overTime;
    }

    public int getEWatk() {
        return epad;
    }

    public int getEWdef() {
        return epdd;
    }

    public int getEMdef() {
        return emdd;
    }

    public List<Pair<MapleBuffStat, Integer>> getStatups() {
        return statups;
    }

    public boolean sameSource(MapleStatEffect effect) {
        return this.sourceid == effect.sourceid && this.skill == effect.skill;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public double getZR() {
        return z / 100;
    }

    public int getT() {
        return t;
    }

    public int getDamage() {
        return damage;
    }

    public int getAttackCount() {
        return attackCount;
    }

    public int getMobCount() {
        return mobCount;
    }

    public int getBulletCount() {
        return bulletCount;
    }

    public int getBulletConsume() {
        return bulletConsume;
    }

    public int getMoneyCon() {
        return moneyCon;
    }

    public int getCooldown() {
        return cooldown;
    }

    public int getColldownId() {
        switch (sourceid) {
            case 61120007:
                return 61101002;
            default:
                return sourceid;
        }
    }

    public Map<MapleMonsterStat, Integer> getMonsterStati() {
        return monsterStatus;
    }

    public boolean isHide() {
        return skill && sourceid == 9001004;
    }

    public boolean isDragonBlood() {
        return skill && sourceid == 1311008;
    }

    public boolean isBerserk() {
        return skill && sourceid == 1320006;
    }

    private boolean isDs() {
        return skill && sourceid == 4001003;
    }

    private boolean isCombo() {
        return (skill && sourceid == 1111002) || (skill && sourceid == 11111001);
    }

    private boolean isEnrage() {
        return skill && sourceid == 1121010;
    }

    public boolean isBeholder() {
        return skill && sourceid == 1321007;
    }

    public boolean isShadowPartner() {
        return skill
                && sourceid == 4111002
                || sourceid == 夜行者.影分身
                || sourceid == 双刀.镜像分身;
    }

    private boolean isChakra() {
        return skill && sourceid == 4211001;
    }

    private boolean isPirateMpRecovery() {
        return skill && sourceid == 5101005;
    }

    public boolean isMagicDoor() {
        return skill && sourceid == 2311002;
    }

    public boolean isMesoGuard() {
        return skill && sourceid == 4211005;
    }

    public boolean isCharge() {
        return skill && sourceid >= 1211003 && sourceid <= 1211008;
    }

    public boolean isPoison() {
        return skill && (sourceid == 2111003
                || sourceid == 2101005
                || sourceid == 2111006
                || sourceid == 14111006
                || sourceid == 炎术士.火牢术屏障
                || sourceid == 战神.战神之审判);
    }

    private boolean isMist() {
        return skill && (sourceid == 2111003
                || sourceid == 4221006
                || sourceid == 32121006 //避难所
                || sourceid == 龙神.极光恢复
                || sourceid == 机械师.放大器_AF_11
                || sourceid == 炎术士.火牢术屏障);
    }

    private boolean isSoulArrow() {
        return skill && (sourceid == 3101004 || sourceid == 3201004 || sourceid == 13101003); // bow and crossbow
    }

    private boolean isShadowClaw() {
        return skill && sourceid == 4121006;
    }

    public boolean isComboMove() {
        return skill && (sourceid == 21100004 || sourceid == 21100005 || sourceid == 21110003 || sourceid == 21120006 || sourceid == 21120007);
    }

    private boolean isDispel() {
        return skill && (sourceid == 2311001 || sourceid == 9001000);
    }

    private boolean isHeroWill() {
        return skill && (sourceid == 1121011 || sourceid == 1221012 || sourceid == 1321010 || sourceid == 2121008 || sourceid == 2221008 || sourceid == 2321009 || sourceid == 3121009 || sourceid == 3221008 || sourceid == 4121009 || sourceid == 4221008 || sourceid == 5121008 || sourceid == 5221010);
    }

    private boolean isDash() {
        return skill && sourceid == 5001005;
    }

    public boolean isPirateMorph() { //海盗变身
        return false;
    }

    public boolean isMorph() {
        return morphId > 0;
    }

    public int getMorph() {
        return morphId;
    }

    public boolean isSummon() {
        return getSummonMovementType() != null && !isBeholder();
    }

    public SummonMovementType getSummonMovementType() {
        if (!skill) {
            return null;
        }
        if (GameConstants.isAngelRingSkill(sourceid)) {
            return SummonMovementType.FOLLOW;
        }
        switch (sourceid) {
            case 3211002: // 射手 - 替身术
            case 3111002: // 游侠 - 替身术
            case 13111004:// 风灵使者 - 替身术
            case 3120012://射手 - 精英替身
            case 3220012://游侠 - 精英替身
            case 5211001: // 大幅 - 章鱼炮台
            case 5220002: // 船长 - 超级章鱼炮台
            case 4341006: // 暗影双刀 - 傀儡召唤
            case 4211007: // 独行客 - 黑暗杂耍
            case 4111007: // 无影人 - 黑暗杂耍
            case 35111002: // 机械师 - 磁场
            case 35111005: // 机械师 - 加速器：EX-7
            case 35111011: // 机械师 - 治疗机器人：H-LX
            case 35121003: // 机械师 - 战争机器：泰坦
            case 35121009: // 机械师 - 机器人工厂：RM1
            case 35121010: // 机械师 - 放大器：AF-11
            case 33101008: // 弩骑 - 地雷 自爆
            case 33111003: // 弩骑 野性陷阱
            case 5321003://船锚
            case 5321004:
            case 5711001://滑膛炮
            case 36121002://36121002	全息力场 ：穿透	制造力场后，生成能量球。能量球每次碰到怪物时，给该怪物造成伤害。能量球不会受到敌人的攻击反射技能的伤害。可以通过多模式链接切换模式。
                return SummonMovementType.STATIONARY;//固定
            case 3201007: // 射手 - 金鹰召唤
            case 3101007: // 游侠 - 银鹰召唤
            //case 2311006: // 祭司 - 圣龙召唤
            case 3211005: // 神射手 - 冰凤凰
            case 3111005: // 箭神 - 火凤凰
            case 5211002: // 大幅 - 海鸥空袭
            case 33111005: // 弩骑 银鹰
            case 23111008://精灵 - 冰
            case 23111009://精灵 - 火
            case 23111010://精灵 - 暗。
                return SummonMovementType.CIRCLE_FOLLOW;//盘旋跟随。
            case 35121011: // 机器人工厂召唤技能
            case 32111006: //幻灵 重生
                return SummonMovementType.UNKNOWN;
            case 1321007: // 黑骑士 - 灵魂助力
            case 2121005: // 火毒导师 - 冰破魔兽
            case 2221005: // 冰雷导师 - 火魔兽
            case 2321003: // 主教 - 强化圣龙
            case 11001004: // 魂骑士 - 魂精灵
            case 12001004: // 炎术士 - 炎精灵
            case 13001004: // 风灵使者 - 风精灵
            case 14001005: // 夜行者  - 夜精灵
            case 15001004: // 奇袭者 - 雷精灵
            case 12111004: // 炎术士 - 火魔兽
            case 35111001: // 机械师 - 人造卫星
            case 35111009: // 机械师 - 人造卫星
            case 35111010: // 机械师 - 人造卫星
                return SummonMovementType.FOLLOW;//跟随
            default:
                return null;
        }
    }

    public boolean isSkill() {
        return skill;
    }

    public boolean OverTime() {
        return overTime;
    }

    public int getSourceId() {
        return sourceid;
    }

    public int getMastery() {
        return mastery;
    }

    public int getRange() {
        return range;
    }

    public int getFixedDamage() {
        return fixDamage;
    }

    private int getMaxLevel() {
        return maxLevel;
    }

    public String getBuffString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WATK: ");
        sb.append(this.watk);
        sb.append(", ");
        sb.append("WDEF: ");
        sb.append(this.wdef);
        sb.append(", ");
        sb.append("MATK: ");
        sb.append(this.matk);
        sb.append(", ");
        sb.append("MDEF: ");
        sb.append(this.mdef);
        sb.append(", ");
        sb.append("ACC: ");
        sb.append(this.acc);
        sb.append(", ");
        sb.append("AVOID: ");
        sb.append(this.avoid);
        sb.append(", ");
        sb.append("SPEED: ");
        sb.append(this.speed);
        sb.append(", ");
        sb.append("JUMP: ");
        sb.append(this.jump);
        sb.append(".");

        return sb.toString();
    }

    /**
     *
     * @return true if the effect should happen based on it's probablity, false
     * otherwise
     */
    public boolean makeChanceResult() {
        return prop == 1.0 || Math.random() < prop;
    }

    public static class DelayEffectAction implements Runnable {

        private WeakReference<MapleCharacter> target;
        private int count;
        private double rate;
        private ScheduledFuture<?> sf;

        public DelayEffectAction(int count, double rate, MapleCharacter chr) {
            this.count = count;
            this.rate = rate;
            this.target = new WeakReference<MapleCharacter>(chr);
        }

        @Override
        public void run() {
            if (count == 0) {
                sf.cancel(true);
                return;
            }
            count--;
            MapleCharacter realTarget = target.get();
            if (realTarget != null) {
                realTarget.addMPHP((int) (realTarget.getMaxhp() * rate), (int) (realTarget.getMaxmp() * rate));
                //log.debug("为玩家增加HP MP 。");
            } else {
                sf.cancel(true);
            }
        }

        public void SetScheduledFuture(ScheduledFuture<?> sf) {
            this.sf = sf;
        }
    }

    public static class DelayAngelRingEffectAction implements Runnable {

        private WeakReference<MapleCharacter> target;
        private ScheduledFuture<?> sf;
        private int skillid;

        public DelayAngelRingEffectAction(MapleCharacter chr, int skilld) {
            this.target = new WeakReference<MapleCharacter>(chr);
            this.skillid = skilld;
            if (skillid > 10000) {
                this.skillid = this.skillid % 10000;
            }
        }

        @Override
        public void run() {
            MapleCharacter chr = target.get();
            if (chr != null && chr.inCS()) {
                return;
            }
            if (chr != null && !chr.hasBufferStat(MapleBuffStat.天使戒指)) {
                switch (skillid) {
                    case 1179:
                        chr.giveItemBuff(2022823);
                        break;
                    case 1087:
                        chr.giveItemBuff(2022747);
                        break;
                    case 1085:
                        chr.giveItemBuff(2022746);
                        break;
                    case 1090:
                        chr.giveItemBuff(2022764);
                        break;
                }
                return;
            }
            if (sf != null) {
                sf.cancel(true);
            }
        }

        public void SetScheduledFuture(ScheduledFuture<?> sf) {
            this.sf = sf;
        }

        public ScheduledFuture<?> getSf() {
            return sf;
        }
    }

    public static class CancelEffectAction implements Runnable {

        private MapleStatEffect effect;
        private WeakReference<MapleCharacter> target;
        private long startTime;

        public CancelEffectAction(MapleCharacter target, MapleStatEffect effect, long startTime) {
            this.effect = effect;
            this.target = new WeakReference<MapleCharacter>(target);
            this.startTime = startTime;
        }

        @Override
        public void run() {
            MapleCharacter realTarget = target.get();
            if (realTarget != null) {
                if (realTarget.inCS() || realTarget.inMTS()) {
                    realTarget.addToCancelBuffPackets(effect, startTime);
                } else {
                    realTarget.cancelEffect(effect, false, startTime);
                }
            }
        }
    }

    public boolean isTornado() {
        return (this.sourceid == 4321000) || (this.sourceid == 4321001);
    }

    public static MapleStatEffect getInstance() {
        if (instance == null) {
            instance = new MapleStatEffect();
        }
        return instance;
    }

    public boolean isLingQi() {
        return skill && (sourceid == 32001003
                || sourceid == 32101002
                || sourceid == 32101003
                || sourceid == 战法.进阶黑暗灵气
                || sourceid == 战法.进阶蓝色灵气
                || sourceid == 战法.进阶黄色灵气);
    }

    public boolean isSkillBianShen() {
        return skill && (sourceid == 5111005 || sourceid == 5121003 || sourceid == 13111005 || sourceid == 15111002);
    }

    public boolean is能量获得() {
        return skill && (sourceid == 5110001 || sourceid == 15100004);
    }

    public boolean is骰子() {
        return skill && (sourceid == 枪手.幸运骰子 || sourceid == 拳手.幸运骰子 || sourceid == 机械师.幸运骰子 || sourceid == 5311005 || sourceid == 5320007);
    }

    public boolean is传送门() {
        return skill && (sourceid == 机械师.传送门_GX_9);
    }

    public int 技能解析(String 技能解析式, int Int型技能等级, int 返回的默认值) {
        /*
         * int 返回的最终结果; String String型技能等级 = String.valueOf(Int型技能等级); if (技能解析式
         * == null) { 返回的最终结果 = 返回的默认值; } else { ScriptEngineManager mgr = new
         * ScriptEngineManager(); ScriptEngine engine =
         * mgr.getEngineByExtension("js"); // x u d 处理 技能解析式 =
         * 技能解析式.replace("x", String型技能等级); 技能解析式 = 技能解析式.replace("u",
         * "Math.ceil"); 技能解析式 = 技能解析式.replace("d", "Math.floor"); 技能解析式 =
         * 技能解析式.replace("=", ""); //盛大的xml有些加了个"=" 垃圾盛大
         * ////log.debug("替换式："+技能解析式); String result = null; try { result =
         * engine.eval(技能解析式).toString(); } catch (ScriptException ex) {
         * //log.debug("技能解析报错："+ex); } //输出的结果 是xx.0 这样是为了去除小数点以及后面的部分 String
         * theFinalResult = result.substring(0, result.length() - 2); //String
         * theFinalResult = result; 返回的最终结果 = Integer.valueOf(theFinalResult);
         * ////log.debug("结果输出："+theFinalResult); } return 返回的最终结果;
         */
        return ScriptEngineEval.Eval(Int型技能等级, 技能解析式, 返回的默认值, sourceid);
    }

    public double getmHpR() {
        return mhpR;
    }

    public double getmMpR() {
        return mmpR;
    }

    public int geteMHp() {
        return emhp;
    }

    public int geteMMp() {
        return emmp;
    }

    public int getMDamageOver() {
        return MDamageOver;
    }

    public MapleForeignBuffSkill getForeign() {
        /*      if (refreshstyle && foreign == null) {
         foreign = MapleForeignBuffHelp.getForeignBuffSkill(this);
         }*/
        return foreign;
    }

    public void initForeign(MapleForeignBuffStat... stat) {
        if (!refreshstyle) {
            refreshstyle = true;
            foreign = new MapleForeignBuffSkill(this);
            foreign.getStats().addAll(Arrays.asList(stat));
        }
    }
}
