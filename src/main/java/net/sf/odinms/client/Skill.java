/*
 已转移至  SkillFactory类
 */
package net.sf.odinms.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.life.Element;

public class Skill implements ISkill {

    public int id;
    public List<MapleStatEffect> effects = new ArrayList<MapleStatEffect>();
    public Element element;
    public int animationTime;
    public boolean charge;
    public boolean isCommon;
    public int maxLevel;
    public int masterLevel;
    public boolean hasCharge;
    public Map<String, String> infos = new HashMap<String, String>();
    private String name;

    public Skill(int id) {
        super();
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MapleStatEffect getEffect(int level) {
        if (level > getMaxLevel()) {
            return this.effects.get(getMaxLevel() - 1);
        }
        return this.effects.get(Math.max(0, level - 1));
    }

    public int getMaxLevel() {
        return effects.size();
    }

    public boolean canBeLearnedBy(MapleJob job) {
        int jid = job.getId();
        int skillForJob = id / 10000;
        if (jid / 100 != skillForJob / 100 && skillForJob / 100 != 0) { // wrong job
            return false;
        }
        if ((skillForJob / 10) % 10 > (jid / 10) % 10) { // wrong 2nd job
            return false;
        }
        if (skillForJob % 10 > jid % 10) { // wrong 3rd/4th job
            return false;
        }
        return true;
    }

    public Element getElement() {
        return element;
    }

    public int getAnimationTime() {
        return animationTime;
    }

    public boolean isBeginnerSkill() {
        boolean output = false;
        String idString = String.valueOf(id);
        if (idString.length() == 4 || idString.length() == 1) {
            output = true;
        }
        return output;
    }

    public boolean hasCharge() {
        if (((infos.containsKey("knockbackLimit")
                || infos.containsKey("chargingSkill")
                || infos.containsKey("keydownThrowing"))
                && (id != 5721007)
                && (id != 35111004))
                || (id == 61111100 || id == 61111111 || id == 5081001)) {
            return true;
        }
        return false;
        //return charge;
    }

    public boolean getisCommon() {
        return isCommon;
    }

    //超级技能判断
    public boolean IsSuper() {
        switch (id) {
            case 24121054://最终审判
            case 24121053://英雄奥斯
            case 24121052://卡片终结
                return true;
            default:
                return false;
        }
    }

    public boolean hasMastery() {
        return getMasterLevel() > 0 || IsSuper();

        /*
         * int jobid = this.id / 10000; switch (jobid) { case 112: int[]
         * heskills = {1120003, 1120004, 1121000, 1121001, 1121002, 1121006,
         * 1121008, 1121010, 1121011}; for (int gskill : heskills) { if (this.id
         * == gskill) { return true; } } break; case 122: int[] paskills =
         * {1220005, 1220006, 1220010, 1221000, 1221002, 1221004, 1221007,
         * 1221009, 1221011, 1221012}; for (int gskill : paskills) { if (this.id
         * == gskill) { return true; } } break; case 132: int[] daskills =
         * {1320005, 1320006, 1320008, 1320009, 1321000, 1321001, 1321002,
         * 1321003, 1321007, 1321010}; for (int gskill : daskills) { if (this.id
         * == gskill) { return true; } } break; case 212: int[] fpskills =
         * {2121000, 2121001, 2121003, 2121004, 2121005, 2121006, 2121007,
         * 2121008}; for (int gskill : fpskills) { if (this.id == gskill) {
         * return true; } } break; case 222: int ilskills[] = {2221000, 2221001,
         * 2221004, 2221005, 2221006, 2221007, 2221008, 2221009}; for (int
         * gskill : ilskills) { if (this.id == gskill) { return true; } } break;
         * case 232: int[] biskills = {2321000, 2321001, 2321003, 2321004,
         * 2321005, 2321006, 2321007, 2321008, 2321009}; for (int gskill :
         * biskills) { if (this.id == gskill) { return true; } } break; case
         * 312: int[] boskills = {3120005, 3121000, 3121002, 3121003, 3121004,
         * 3221003, 3121006, 3121007, 3121008, 3121009}; for (int gskill :
         * boskills) { if (this.id == gskill) { return true; } } break; case
         * 322: int[] crskills = {3221003, 3220004, 3221000, 3221001, 3221002,
         * 3221005, 3221006, 3221007, 3221008}; for (int gskill : crskills) { if
         * (this.id == gskill) { return true; } } break; case 412: int[]
         * inskills = {4120002, 4120005, 4121000, 4121003, 4121004, 4121006,
         * 4121007, 4121008, 4121009}; for (int gskill : inskills) { if (this.id
         * == gskill) { return true; } } break; case 422: int[] shskills =
         * {4220002, 4220005, 4221000, 4221001, 4221003, 4221004, 4221006,
         * 4221007, 4221008}; for (int gskill : shskills) { if (this.id ==
         * gskill) { return true; } } break; case 431: if (this.id != 4311003) {
         * break; } return true; case 432: if (this.id != 4321000) { break; }
         * return true; case 433: int[] du3skills = {4331002, 4331005}; for (int
         * gskill : du3skills) { if (this.id == gskill) { return true; } }
         * break; case 434: int[] du4skills = {4341000, 4340001, 4341002,
         * 4341003, 4341004, 4341005, 4341006, 4341007, 4341008}; for (int
         * gskill : du4skills) { if (this.id == gskill) { return true; } }
         * break; case 512: int[] buskills = {5121000, 5121001, 5121002,
         * 5121003, 5121004, 5121005, 5121007, 5121008, 5121009, 5121010}; for
         * (int gskill : buskills) { if (this.id == gskill) { return true; } }
         * break; case 522: int[] coskills = {5221000, 5220001, 5220002,
         * 5221003, 5221004, 5221006, 5221007, 5221008, 5221009, 5221010,
         * 5220011}; for (int gskill : coskills) { if (this.id == gskill) {
         * return true; } } break; case 2112: int[] ar4skills = {21121000,
         * 21120001, 21120002, 21121003, 21120004, 21120005, 21120006, 21120007,
         * 21121008}; for (int gskill : ar4skills) { if (this.id == gskill) {
         * return true; } } break; case 2217: int[] ev9skills = {22171000,
         * 22170001, 22171002, 22171003, 22171004}; for (int gskill : ev9skills)
         * { if (this.id == gskill) { return true; } } break; case 2218: int[]
         * ev10skills = {22181000, 22181001, 22181002, 22181003}; for (int
         * gskill : ev10skills) { if (this.id == gskill) { return true; } }
         * break; case 3212: int[] hlskills = {32120000, 32120001, 32121003,
         * 32121004, 32121005, 32121006, 32121007, 32121008, 32121002}; for (int
         * gskill : hlskills) { if (this.id == gskill) { return true; } } }
         */
        //   return false;
    }

    public int getMasterLevel() {
        switch (id) {
            //幽灵一击
            case 4341009:
            //金属机甲：重机枪
            case 35121013:
            //终极斩
            case 4341002:
            //黑暗迷雾
            case 22181002:
            //灵魂之石
            case 22181003:
            //万佛归一破
            case 1220010:
            //进阶斗气
            case 1120003:
            //双刀风暴
            case 4311003:
            //三连环光击破
            case 4121007:
            //光芒飞箭
            case 2321007:
            // 火焰轮
            case 22171003:
                return maxLevel;
            default: {
                //勇士的意志
                if (GameConstants.勇士的意志技能系(id)) {
                    return maxLevel;
                }
                //冒险岛勇士
                if (GameConstants.冒险岛勇士技能系(id)) {
                    return maxLevel;
                }
                return masterLevel;
            }

        }
    }

    public boolean isFourthJob() {
        switch (id) {
            case 1120012://战斗精通
            case 1220013://祝福护甲
            case 1320011://灵魂复仇
            case 2120009://魔力精通
            case 2220009://魔力精通
            case 2320010://魔力精通
            case 3120011://射术精修
            case 3220009://射术精修
            case 4120010://娴熟飞镖术
            case 4220009://贪婪
            case 4321002://闪光弹
            case 4321003://暗影二段跳
            case 4321004://悬浮地刺
            case 5120011://反制攻击
            case 5220012://反制攻击
            case 21120011://迅捷移动
            case 22121000://冰点寒气
            case 22121001://自然力重置
            case 22120002://咒语精通
            case 32120009://活力激化
            case 33120010://野性本能
                return false;
        }

        return ((id / 10000) % 10) == 2;
    }

    public boolean isEvanFourthJob() {
        boolean flage = false;
        switch (id) {
            case 22171000:
            case 22170001:
            case 22171002:
            case 22171003:
            case 22171004:
            case 22181000:
            case 22181001:
            case 22181002:
            case 22181003:
                flage = true;
                break;
        }
        return flage;
    }

    @Override
    public boolean isDualFourthJob() {
        boolean flage = false;
        switch (id) {
            case 4311003:
            case 4321000:
            case 4331002:
            case 4331005:
            case 4341000:
            case 4340001:
            case 4341002:
            case 4341003:
            case 4341004:
            case 4341005:
            case 4341006:
            case 4341007:
            case 4341008:
            case 4341009://幽灵一击
                flage = true;
                break;
        }
        return flage;
    }

    @Override
    public boolean isBigBangFourthJob() {
        boolean flage = false;
        switch (id) {
            case 32120000://进阶黑暗灵气
            case 32120001://进阶黄色灵气
            case 32121003://飓风
            case 32121004://黑暗创世
            case 32121005://稳如泰山
            case 32121006://避难所
            case 32121007://冒险岛勇士
            //32121008 勇士的意志

            case 33120000://神弩手
            case 33121001://闪光箭雨
            case 33121002://音速震波
            case 33121004://火眼晶晶
            case 33121007://冒险岛勇士
            case 33121005://神经毒气
            case 33121006://暴走形态
            case 33121009://奥义箭乱舞
            //33121008:勇士的意志

            case 35120000: //终极机甲
            case 35120001://机器人精通
            case 35121003://战争机器：泰坦
            case 35121005://金属机甲：导弹战车
            case 35121006://卫星防护
            case 35121007://冒险岛勇士
            case 35121009://机器人工厂：RM1
            case 35121010://放大器：AF-11
            case 35121012://激光爆破
                //35121008:勇士的意志
                flage = true;
                break;
        }
        return flage;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Skill) {
            Skill skill = (Skill) obj;
            if (skill.id == id) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 71 * hash + this.id;
        hash = 71 * hash + this.maxLevel;
        hash = 71 * hash + this.masterLevel;
        hash = 71 * hash + (this.hasCharge ? 1 : 0);
        return hash;
    }

    /**
     * 一些是4转技能WZ也有精通等级。但又不写mastery等级的。
     *
     * @return
     */
    @Override
    public boolean NoMastery() {
        switch (id) {
            case 33120010: //野性本能
            case 4340010: //锋利
            case 33121005://神经毒气
            case 33111007://暴走形态
            case 32111014://稳如泰山
            case 80001130:
            case 4340012:
            case 4320005:
            case 21120011:
                return true;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return "Skill{" + "id=" + id + ", name=" + name + '}';
    }
}
