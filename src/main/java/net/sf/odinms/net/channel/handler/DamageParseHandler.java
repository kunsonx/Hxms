/*
 造成伤害的方法
 */
package net.sf.odinms.net.channel.handler;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.sf.odinms.client.*;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.client.skills.*;
import net.sf.odinms.client.status.MapleMonsterStat;
import net.sf.odinms.client.status.MonsterStatusEffect;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.Element;
import net.sf.odinms.server.life.ElementalEffectiveness;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.*;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.WriteToFile;
import net.sf.odinms.tools.data.input.LittleEndianAccessor;

public abstract class DamageParseHandler extends AbstractMaplePacketHandler {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(DamageParseHandler.class);
    public boolean savePacket = false; //是否记录攻击错误包

    public enum AttackMode {

        CLOSE,
        MAGIC,
        RANGE,
        Passive
    }

    //攻击信息
    public class AttackInfo {
        //088需要声明个move

        public int numAttacked, numDamage, numAttackedAndDamage, move;
        public int skill, stance, direction, charge, pos, aranCombo, csstar, slot, unk1;
        public List<Pair<Integer, List<Integer>>> allDamage;
        public boolean isHH = false;
        public int speed = 4;
        public int lastAttackTickCount;
        public Point point = new Point();
        public AttackMode mode;

        public AttackInfo(AttackMode mode) {
            this.mode = mode;
        }

        public AttackInfo() {
        }

        public MapleStatEffect getAttackEffect(MapleCharacter chr, ISkill theSkill) {//获取攻击效果
            ISkill mySkill = theSkill;
            if (mySkill == null) {
                mySkill = SkillFactory.getSkill(skill);
            }
            int skillLevel = chr.getSkillLevel(mySkill);
            if (mySkill.getId() == 1009 || mySkill.getId() == 10001009) {
                skillLevel = 1;
            }

            switch (skill) {
                case 31001006:
                case 31001007:
                case 31001008:
                    skillLevel = chr.getSkillLevel(31000004);
                    break;
                case 31121010:
                    skillLevel = chr.getSkillLevel(31121000);
                    break;
            }
            if (skillLevel == 0) {//特殊处理。。
                return null;
                //return SkillFactory.getSkill(skill).getEffect(SkillFactory.getSkill(skill).getMaxLevel());
            }
            return mySkill.getEffect(skillLevel);
        }
    }

    //应用攻击
    protected void applyAttack(AttackInfo attack, MapleCharacter player, int maxDamagePerMonster, int attackCount) { //应用攻击
        player.getCheatTracker().resetHPRegen();//玩家欺骗服务器重置HP？
        player.resetAfkTimer();
        player.getCheatTracker().checkAttack(attack.skill);
        ISkill theSkill = null;
        MapleStatEffect attackEffect = null;
        if (attack.skill != 0) { //如果攻击的技能不为0
            theSkill = SkillFactory.getSkill(attack.skill);
            attackEffect = attack.getAttackEffect(player, theSkill);
            if (attackEffect != null && attackEffect.getSourceId() == 35001001 && player.getSkillLevel(35100008) > 0) {
                attackEffect = SkillFactory.getSkill(35101009).getEffect(player.getSkillLevel(35100008));
            }
            if (attackEffect == null) { //如果没有攻击效果
                log.debug(player.getName() + "使用了没有的技能 - 技能ID: (" + attack.skill + ")");
            }
            if (attack.skill != 牧师.群体治愈
                    && attack.skill != 战法.飓风
                    && attack.skill != 机械师.金属机甲_重机枪
                    && attack.skill != 机械师.金属机甲_导弹战车
                    && attack.skill != 机械师.金属机甲_重机枪_4转
                    && attack.skill != 机械师.火箭推进器
                    && attack.skill != 31121005 //黑暗变形
                    ) {
                if (player.isAlive() && attackEffect != null && attackEffect.getSourceId() != 61120007) {
                    attackEffect.applyTo(player);
                } else {
                    player.getClient().getSession().write(MaplePacketCreator.enableActions());
                }
            }
        }
        if (!player.isAlive()) { //如果玩家死了
            player.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
            return;
        }
        int totDamage = 0;
        final MapleMap map = player.getMap();
        MapleDamageHandler.MaplePvpSystemHandler(attack, player, map);
        if (attack.skill == 4211006) { // 金钱炸弹
            int delay = 0;
            for (Pair<Integer, List<Integer>> oned : attack.allDamage) {
                MapleMapObject mapobject = map.getMapObject(oned.getLeft().intValue());
                if (mapobject != null && mapobject.getType() == MapleMapObjectType.ITEM) {
                    final MapleMapItem mapitem = (MapleMapItem) mapobject;
                    if (mapitem.getMeso() > 0) {
                        synchronized (mapitem) {
                            if (mapitem.isPickedUp()) {
                                return;
                            }
                            TimerManager.getInstance().schedule(new Runnable() {
                                @Override
                                public void run() {
                                    map.removeMapObject(mapitem);
                                    map.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 4, 0), mapitem.getPosition());
                                    mapitem.setPickedUp(true);
                                }
                            }, delay);
                            delay += 100;
                        }
                    } else if (mapitem.getMeso() == 0) {
                        player.getCheatTracker().registerOffense(CheatingOffense.ETC_EXPLOSION);
                        return;
                    }
                } else if (mapobject != null && mapobject.getType() != MapleMapObjectType.MONSTER) {
                    player.getCheatTracker().registerOffense(CheatingOffense.EXPLODING_NONEXISTANT);
                    return; // etc explosion, exploding nonexistant things, etc.
                }
            }
        }

        for (Pair<Integer, List<Integer>> oned : attack.allDamage) {
            //这里开始的处理是单次攻击的
            MapleMonster monster = map.getMonsterByOid(oned.getLeft().intValue());
            if (monster != null) {
                if (!monster.isControllerHasAggro()) {
                    if (monster.getController() == player) {
                        monster.setControllerHasAggro(true);
                    } else {
                        monster.switchController(player, true);
                    }
                }

                int totDamageToOneMonster = MapleDamageHandler.CalcAllDamage(oned.getRight());
                MapleDamageHandler.GainKuangLongNuQiHandler(oned.getRight().size(), player);


                if (attack.skill == 31000004) {
                    player.addMP(5);
                }


                if (player.hasBufferStat(MapleBuffStat.吸血鬼之触_伤害转化)) {
                    player.addHP((int) Math.floor((totDamageToOneMonster * (player.getBuffedValue(MapleBuffStat.吸血鬼之触_伤害转化) / 100.0))));
                }
                totDamage += totDamageToOneMonster;
                player.checkMonsterAggro(monster);
                if (totDamageToOneMonster > attack.numDamage + 1) {
                    int dmgCheck = player.getCheatTracker().checkDamage(totDamageToOneMonster);
                    if (dmgCheck > 5 && totDamageToOneMonster < 99999 && monster.getId() < 9500317 && monster.getId() > 9500319) {
                        player.getCheatTracker().registerOffense(CheatingOffense.SAME_DAMAGE, dmgCheck + " times: " + totDamageToOneMonster);
                    }
                }
                if (attackEffect != null) {
                    checkHighDamage(player, monster, attack, theSkill, attackEffect, totDamageToOneMonster, maxDamagePerMonster);
                }
                double distance = player.getPosition().distanceSq(monster.getPosition());
                if (distance > 400000.0) { // 600^2, 550 大约是普通远程攻击的距离
                    player.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER, Double.toString(Math.sqrt(distance)));
                }

                if (attack.skill == 拳手.能量耗转
                        || attack.skill == 弩骑.利爪狂风
                        || attack.skill == 标飞.生命吸收
                        || attack.skill == 夜行者.吸血
                        || player.getBuffedValue(MapleBuffStat.连环吸血) != null) {
                    //能量耗转 利爪狂风 生命吸收
                    int skillid = attack.skill;
                    if (player.getBuffedValue(MapleBuffStat.连环吸血) != null) {
                        skillid = 战神.连环吸血;
                    }
                    ISkill skill = SkillFactory.getSkill(skillid);
                    int gainhp = (int) ((double) totDamage * (double) skill.getEffect(player.getSkillLevel(skillid)).getX() / 100.0);
                    if (attack.skill == 弩骑.利爪狂风) //利爪狂风
                    {
                        gainhp = Math.min(monster.getMaxHp(), Math.min(gainhp, player.getMaxhp() / 6)); //15%
                    } else if (player.getBuffedValue(MapleBuffStat.连环吸血) != null) {
                        gainhp = Math.min(monster.getMaxHp(), Math.min(gainhp, player.getMaxhp() / 10)); //10%
                    } else {
                        gainhp = Math.min(monster.getMaxHp(), Math.min(gainhp, player.getMaxhp() / 2)); //50%
                    }
                    player.addHP(gainhp);
                } else if ((attack.skill == 4001334 || attack.skill == 4201005 || attack.skill == 0 || attack.skill == 4211002 || attack.skill == 4211004) && player.getBuffedValue(MapleBuffStat.PICKPOCKET) != null) {
                    handlePickPocket(player, monster, oned);
                } else if (attack.skill == 2301002 && !monster.getUndead()) { //群体治愈
                    player.getCheatTracker().registerOffense(CheatingOffense.HEAL_ATTACKING_UNDEAD);
                    return;
                }
                if (player.getBuffedValue(MapleBuffStat.BLIND) != null) { //刺眼箭  弩骑-致盲
                    ISkill blind = SkillFactory.getSkill(3221006);
                    if (player.getSkillLevel(blind) == 0) {
                        blind = SkillFactory.getSkill(33111004); //致盲
                    }
                    if (blind.getEffect(player.getSkillLevel(blind)).makeChanceResult()) {
                        MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MapleMonsterStat.ACC, blind.getEffect(player.getSkillLevel(blind)).getX()), blind, false);
                        monster.applyStatus(player, monsterStatusEffect, false, blind.getEffect(player.getSkillLevel(blind)).getY() * 1000);
                    }
                } else if (player.getBuffedValue(MapleBuffStat.抗压) != null) { //抗压
                    ISkill blind = SkillFactory.getSkill(战神.抗压);
                    if (blind.getEffect(player.getSkillLevel(blind)).makeChanceResult()) {
                        MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MapleMonsterStat.抗压, blind.getEffect(player.getSkillLevel(blind)).getX()), blind, false);
                        monster.applyStatus(player, monsterStatusEffect, false, blind.getEffect(player.getSkillLevel(blind)).getY() * 1000);
                    }
                }

                if (player.getJob().isA(MapleJob.WHITEKNIGHT) || (player.isAran() && player.getBuffedValue(MapleBuffStat.WK_CHARGE) != null)) {
                    //寒冰冲击 冰雪矛
                    int[] charges = new int[]{1211006, 战神.冰雪矛};
                    for (int charge : charges) {
                        ISkill chargeSkill = SkillFactory.getSkill(charge);
                        int skilllevel = player.getSkillLevel(chargeSkill);
                        if (skilllevel == 0) {
                            continue;
                        }
                        MapleStatEffect chargeEffect = chargeSkill.getEffect(skilllevel);
                        //判断这个buff是否来自charge这个技能
                        if (player.isBuffFrom(MapleBuffStat.WK_CHARGE, chargeSkill)) {
                            if (totDamageToOneMonster > 0) {
                                MonsterStatusEffect monsterStatusEffect;
                                if (charge == 1211006) {
                                    //获取怪物对冰属性的抗性
                                    ElementalEffectiveness iceEffectiveness = monster.getEffectiveness(Element.ICE);
                                    //属性不是 抗冰 或者 免疫冰
                                    if (iceEffectiveness != ElementalEffectiveness.IMMUNE && iceEffectiveness != ElementalEffectiveness.STRONG) {
                                        monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MapleMonsterStat.FREEZE, 1), chargeSkill, false);
                                    } else {
                                        break;
                                    }
                                } else { //技能是冰雪矛
                                    monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MapleMonsterStat.SPEED, 1), chargeSkill, false);
                                }
                                monster.applyStatus(player, monsterStatusEffect, false, chargeEffect.getY() * 2000);
                                break;
                            }
                        }
                    }
                }

                if (player.getBuffedValue(MapleBuffStat.重生) != null && totDamage > monster.getHp()) { //幻灵 重生
                    ISkill chongsheng = SkillFactory.getSkill(32111006);
                    if (chongsheng.getEffect(player.getSkillLevel(chongsheng)).makeChanceResult()) {
                        MapleSummon summon = player.getSummon(32111006);
                        if (summon != null) {
                            player.getMap().broadcastMessage(MaplePacketCreator.removeSpecialMapObject(summon, true));
                            player.getMap().removeMapObject(summon);
                            player.removeVisibleMapObject(summon);
                            player.removeSummon(32111006);
                        }
                        MapleSummon tosummon = new MapleSummon(player, 32111006, player.getPosition(), SummonMovementType.UNKNOWN);
                        player.getMap().spawnSummon(tosummon);
                        player.putSummon(32111006, tosummon);
                    }
                }

                if (attack.skill == 3111008 && attack.allDamage.size() == 1) {//枯竭箭
                    int mobhp = monster.getMaxHp();
                    int damage = (int) (attack.allDamage.get(0).getRight().get(0) * (attackEffect.getX() / 100.0));
                    int pmx = player.getMaxhp() / 2;
                    if (damage > mobhp) {
                        damage = mobhp;
                    }
                    if (damage > pmx) {
                        damage = pmx;
                    }
                    player.addHP(damage);
                    player.updatePartyMemberHP();
                }


                int skillid = 0;
                if (player.getJob().getId() == 412) {
                    skillid = 4120005;
                } else if (player.getJob().getId() == 412) {
                    skillid = 4220005;
                } else if (player.getJob().getId() == 434) {
                    skillid = 4340001;
                } else if (player.getJob().getId() == 1411) {
                    skillid = 14110004;
                }
                ISkill venomNL = SkillFactory.getSkill(skillid);
                if (player.getSkillLevel(venomNL) > 0) {
                    MapleStatEffect venomEffect = venomNL.getEffect(player.getSkillLevel(venomNL));
                    for (int i = 0; i < attackCount; i++) {
                        if (venomEffect.makeChanceResult() == true) {
                            if (monster.getVenomMulti() < 3) {
                                monster.setVenomMulti(monster.getVenomMulti() + 1);
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MapleMonsterStat.POISON, 1), venomNL, false);
                                monster.applyStatus(player, monsterStatusEffect, false, venomEffect.getDuration(), true);
                            }
                        }
                    }
                }
                //给怪物BUFF
                if (totDamageToOneMonster > 0 && attackEffect != null && attackEffect.getMonsterStati().size() > 0) {
                    if (attackEffect.makeChanceResult()) {
                        MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(attackEffect.getMonsterStati(), theSkill, false);
                        monster.applyStatus(player, monsterStatusEffect, attackEffect.isPoison(), attackEffect.getDuration());
                    }
                }
                if (attack.isHH && !monster.isBoss()) {
                    map.damageMonster(player, monster, monster.getHp() - 1);
                } else {
                    map.damageMonster(player, monster, totDamageToOneMonster);
                }
            }
        }

        MapleDamageHandler.ShowKuangLongNuQiStatsHandler(player);

        if (totDamage > 1) {
            player.getCheatTracker().setAttacksWithoutHit(player.getCheatTracker().getAttacksWithoutHit() + 1);
            final int offenseLimit;
            if (attack.skill != 3121004) {//不是暴风箭雨
                offenseLimit = 100;
            } else {
                offenseLimit = 300;
            }
            if (player.getCheatTracker().getAttacksWithoutHit() > offenseLimit) {
                player.getCheatTracker().registerOffense(CheatingOffense.ATTACK_WITHOUT_GETTING_HIT, Integer.toString(player.getCheatTracker().getAttacksWithoutHit()));
            }
            if (player.hasEnergyCharge()) {
                //增加能量 Miss不增加
                player.increaseEnergyCharge(attack.numAttacked);
            }
        }
    }

    //偷盗技能
    private void handlePickPocket(MapleCharacter player, MapleMonster monster, Pair<Integer, List<Integer>> oned) { //敛财术
        ISkill pickpocket = SkillFactory.getSkill(4211003);//敛财术
        int delay = 0;
        int maxmeso = player.getBuffedValue(MapleBuffStat.PICKPOCKET).intValue();
        int reqdamage = 20000;
        Point monsterPosition = monster.getPosition();

        for (Integer eachd : oned.getRight()) {
            if (pickpocket.getEffect(player.getSkillLevel(pickpocket)).makeChanceResult()) {
                double perc = (double) eachd / (double) reqdamage;
                final int todrop = Math.min((int) Math.max(perc * (double) maxmeso, (double) 1), maxmeso);
                final MapleMap tdmap = player.getMap();
                final Point tdpos = new Point((int) (monsterPosition.getX() + (Math.random() * 100) - 50), (int) (monsterPosition.getY()));
                final MapleMonster tdmob = monster;
                final MapleCharacter tdchar = player;
                TimerManager.getInstance().schedule(new Runnable() {
                    @Override
                    public void run() {
                        tdmap.spawnMesoDrop(todrop, tdpos, tdmob, tdchar, false);
                    }
                }, delay);
                delay += 200;
            }
        }
    }

    //检查高伤害
    private void checkHighDamage(MapleCharacter player, MapleMonster monster, AttackInfo attack, ISkill theSkill, MapleStatEffect attackEffect, int damageToMonster, int maximumDamageToMonster) { //检查高攻击伤害
        int elementalMaxDamagePerMonster;
        Element element = Element.PHYSICAL;
        if (theSkill != null) {
            element = theSkill.getElement();
            int skillId = theSkill.getId();
            if (skillId == 3221007) {
                maximumDamageToMonster = 99999;
            } else if (skillId == 4221001) {
                maximumDamageToMonster = 400000;
            }
        }
        if (player.getBuffedValue(MapleBuffStat.WK_CHARGE) != null) {
            int chargeSkillId = player.getBuffSource(MapleBuffStat.WK_CHARGE);
            switch (chargeSkillId) {
                case 1211003:
                case 1211004:
                    element = Element.FIRE;
                    break;
                case 1211005:
                case 1211006:
                    element = Element.ICE;
                    break;
                case 1211007:
                case 1211008:
                    element = Element.LIGHTING;
                    break;
                case 1221003:
                case 1221004:
                    element = Element.HOLY;
                    break;
            }
            ISkill chargeSkill = SkillFactory.getSkill(chargeSkillId);
            maximumDamageToMonster *= chargeSkill.getEffect(player.getSkillLevel(chargeSkill)).getDamage() / 100.0;
        }
        if (element != Element.PHYSICAL) {
            double elementalEffect;
            if (attack.skill == 3211003 || attack.skill == 3111003) { // inferno and blizzard
                elementalEffect = attackEffect.getX() / 200.0;
            } else {
                elementalEffect = 0.5;
            }
            switch (monster.getEffectiveness(element)) {
                case IMMUNE:
                    elementalMaxDamagePerMonster = 1;
                    break;
                case NORMAL:
                    elementalMaxDamagePerMonster = maximumDamageToMonster;
                    break;
                case WEAK:
                    elementalMaxDamagePerMonster = (int) (maximumDamageToMonster * (1.0 + elementalEffect));
                    break;
                case STRONG:
                    elementalMaxDamagePerMonster = (int) (maximumDamageToMonster * (1.0 - elementalEffect));
                    break;
                default:
                    throw new RuntimeException("Unknown enum constant");
            }
        } else {
            elementalMaxDamagePerMonster = maximumDamageToMonster;
        }
        if (damageToMonster > elementalMaxDamagePerMonster) {
            player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE);
        }
    }

    //反击(抗压 飓风等)
    public AttackInfo parsePassiveEnergy(MapleCharacter chr, LittleEndianAccessor lea) { //解析伤害数值
        AttackInfo ret = new AttackInfo(AttackMode.Passive);
        try {
            int _tmp1 = lea.readByte();
            ret.numAttackedAndDamage = lea.readByte();
            ret.numAttacked = (ret.numAttackedAndDamage >>> 4) & 0xF;
            ret.numDamage = ret.numAttackedAndDamage & 0xF;
            ret.allDamage = new ArrayList<Pair<Integer, List<Integer>>>();
            ret.skill = changeSkilliId(lea.readInt());  //技能ID
            if (_tmp1 != 1) {
                lea.skip(1); //01
            }
            ret.move = lea.readByte();//移动


            ParseMovement(chr, lea, ret);

            lea.skip(4);
            lea.skip(1);
            ret.aranCombo = lea.readByte(); //战神连击数
            ret.pos = lea.readByte(); //动作
            ret.stance = lea.readByte(); //姿势 一般0x80
            lea.skip(4);
            lea.skip(1);
            ret.speed = lea.readByte();//武器攻击速度
            ret.lastAttackTickCount = lea.readInt(); // Ticks
            lea.skip(4); //0

            /*
             * log.debug("被动攻击信息如下："); log.debug("技能编号: " + ret.skill);
             * log.debug("连击数: " + ret.aranCombo); log.debug("动作：" + ret.pos);
             * log.debug("姿势：" + ret.stance); log.debug("武器攻击速度: " + ret.speed);
             * log.debug("打到怪物个数: " + ret.numAttacked);
             */

            ParseDamage(lea, ret, SkillFactory.getSkill(ret.skill), chr);

            ret.point = lea.readPos();
        } catch (Exception e) {
            log.error("转换被动攻击错误：" + "damage：" + ret.allDamage
                    + "\r\npacket：" + lea.toString()
                    + "\r\nplayerName：" + chr.getName()
                    + "\r\nskillId：" + ret.skill
                    + "\r\nskillName：" + SkillFactory.getSkillName(ret.skill)
                    + "\r\nskillLevel:" + chr.getSkillLevel(SkillFactory.getSkill(ret.skill))
                    + "\r\n是否拥有精灵的祝福:" + chr.getCygnusBless()
                    + "\r\n", e);
        }
        return ret;
    }

    /**
     * 转换攻击中的一堆麻烦的移动数据。
     *
     * @param chr
     * @param lea
     * @param attack
     */
    public void ParseMovement(MapleCharacter chr, LittleEndianAccessor lea, AttackInfo attack) {
        if (attack.move == 2 && attack.numAttacked != 0) {//
            lea.skip(12);

            Odinms(lea, chr);//解析移动语法。
            Moveme(lea);

            lea.skip(10);

        } else if (attack.move == 0x14) {
            lea.skip(1);
        }
    }

    public void ParseDamage(LittleEndianAccessor lea, AttackInfo attack, ISkill skill, MapleCharacter chr) {
        for (int i = 0; i < attack.numAttacked; i++) {
            int mobid = lea.readInt();
            /*
             * log.info("攻击前4字节：" + HexTool.toString(lea.read(4)));
             * lea.skip(8);//蛋疼的坐标。 log.info("攻击前2字节：" +
             * HexTool.toString(lea.read(2)));
             */
            lea.skip(19);
            List<Integer> allDamageNumbers = new ArrayList<Integer>();
            for (int j = 0; j < attack.numDamage; j++) {
                int damage = lea.readInt();
                if (damage >= 999999 ) {
                    damage = maxDamage(chr, attack, damage);//调用破功方法
                }
                if (mobid > 999999) {
                    savePacket = true;
                }
                MapleStatEffect effect = null;
                if (skill != null && chr != null) {
                    int skilllevel = chr.getSkillLevel(skill);
                    if (skilllevel != 0) {
                        effect = skill.getEffect(skilllevel);
                    }
                }
                if (damage != 0 && effect != null && effect.getFixedDamage() != 0) {
                    damage = effect.getFixedDamage();
                }
                allDamageNumbers.add(Integer.valueOf(damage));
            }
            //log.info("远程攻击后8字节：" + HexTool.toString(lea.read(8)));
            lea.skip(12);
            attack.allDamage.add(new Pair<Integer, List<Integer>>(Integer.valueOf(mobid), allDamageNumbers));
        }
    }

    //远程攻击
    public AttackInfo parseRange(MapleCharacter chr, LittleEndianAccessor lea) { //解析伤害数值
        AttackInfo ret = new AttackInfo(AttackMode.RANGE);
        try {
            lea.readByte();
            ret.numAttackedAndDamage = lea.readByte();
            ret.numAttacked = (ret.numAttackedAndDamage >>> 4) & 0xF;
            ret.numDamage = ret.numAttackedAndDamage & 0xF;
            ret.allDamage = new ArrayList<Pair<Integer, List<Integer>>>();
            ret.skill = changeSkilliId(lea.readInt());  //技能ID
            ISkill skill = SkillFactory.getSkill(ret.skill);


            lea.skip(1); //01
            ret.move = lea.readByte();//移动

            ParseMovement(chr, lea, ret);

            lea.skip(4); //??
            if (skill != null && skill.hasCharge()) {
                ret.charge = lea.readInt();
            } else {
                ret.charge = 0;
            }
            lea.skip(1);//02 02 
            ret.aranCombo = lea.readByte(); //战神连击数
            ret.pos = lea.readByte(); //动作
            ret.stance = lea.readByte(); //姿势
            lea.skip(4); //81 4E A7 4D
            lea.skip(1);//Weapon class
            if (ret.skill == 23111001 || ret.skill == 36111010) {
                lea.skip(12);//22 00 00 00 4F 01 00 00 B2 FF FF FF 
            }
            ret.speed = lea.readByte();//速度
            ret.lastAttackTickCount = lea.readInt(); // Ticks
            lea.skip(4); //0
            ret.slot = (byte) lea.readShort();
            ret.csstar = (byte) lea.readShort();
            ret.direction = lea.readByte();

            if (ret.skill == 3221001 || ret.skill == 35101009) {
                log.info("远程攻击特殊读取：" + lea.readInt());
            }

            /*
             * log.debug("远程攻击信息如下："); log.debug("技能编号: " + ret.skill);
             * log.debug("连击数: " + ret.aranCombo); log.debug("动作：" + ret.pos);
             * log.debug("姿势：" + ret.stance); log.debug("武器攻击速度: " + ret.speed);
             * log.debug("打到怪物个数: " + ret.numAttacked); log.debug("充电值：" +
             * ret.charge); log.debug("使用消耗格子：" + ret.slot);
             */

            ParseDamage(lea, ret, skill, chr);

            ret.point = lea.readPos();
        } catch (Exception ex) {
            log.error("转换远程攻击错误：" + "damage：" + ret.allDamage
                    + "\r\npacket：" + lea.toString()
                    + "\r\nplayerName：" + chr.getName()
                    + "\r\nskillId：" + ret.skill
                    + "\r\nskillName：" + SkillFactory.getSkillName(ret.skill)
                    + "\r\nskillLevel:" + chr.getSkillLevel(SkillFactory.getSkill(ret.skill))
                    + "\r\n是否拥有精灵的祝福:" + chr.getCygnusBless()
                    + "\r\n", ex);
        }

        return ret;
    }

    //魔法攻击
    public AttackInfo parseMagic(MapleCharacter chr, LittleEndianAccessor lea) {
        AttackInfo ret = new AttackInfo(AttackMode.MAGIC);
        try {
            lea.readByte();//对几个怪有作用
            ret.numAttackedAndDamage = lea.readByte();//打了多少次
            ret.numAttacked = (ret.numAttackedAndDamage >>> 4) & 0xF;
            ret.numDamage = ret.numAttackedAndDamage & 0xF;
            ret.allDamage = new ArrayList<Pair<Integer, List<Integer>>>();
            ret.skill = changeSkilliId(lea.readInt());  //技能ID+
            ISkill skill = SkillFactory.getSkill(ret.skill);
            lea.skip(1); //01
            ret.move = lea.readByte();//移动

            ParseMovement(chr, lea, ret);

            lea.skip(4);

            //charge对应的是用技能的时候头上有能量槽
            if (skill != null && skill.hasCharge()) {
                ret.charge = lea.readInt();
            } else {
                ret.charge = 0;

            }

            ret.aranCombo = lea.readByte(); //这是目前的连击数?
            ret.pos = lea.readByte(); //动作
            ret.stance = lea.readByte(); //姿势
            lea.readInt();
            lea.skip(1);
            ret.speed = lea.readByte();//速度
            ret.lastAttackTickCount = lea.readInt(); // Ticks
            lea.skip(4); //0

            /*
             * log.debug("魔法攻击信息如下："); log.debug("技能编号: " + ret.skill);
             * log.debug("连击数: " + ret.aranCombo); log.debug("动作：" + ret.pos);
             * log.debug("姿势：" + ret.stance); log.debug("武器攻击速度: " + ret.speed);
             * log.debug("打到怪物个数: " + ret.numAttacked); log.debug("充电值：" +
             * ret.charge);
             */

            ParseDamage(lea, ret, skill, chr);

        } catch (Exception ex) {
            log.error("转换魔法攻击错误：" + "damage：" + ret.allDamage
                    + "\r\npacket：" + lea.toString()
                    + "\r\nplayerName：" + chr.getName()
                    + "\r\nskillId：" + ret.skill
                    + "\r\nskillName：" + SkillFactory.getSkillName(ret.skill)
                    + "\r\nskillLevel:" + chr.getSkillLevel(SkillFactory.getSkill(ret.skill))
                    + "\r\n是否拥有精灵的祝福:" + chr.getCygnusBless()
                    + "\r\n", ex);
        }
        //ret.point = lea.readPos();
        return ret;
    }

    //近身攻击
    public AttackInfo parseClose(MapleCharacter chr, LittleEndianAccessor lea) { //解析伤害数值
        AttackInfo ret = new AttackInfo(AttackMode.CLOSE);
        try {
            lea.readByte();
            ret.numAttackedAndDamage = lea.readByte();//打了多少次
            ret.numAttacked = (ret.numAttackedAndDamage >>> 4) & 0xF;
            ret.numDamage = ret.numAttackedAndDamage & 0xF;
            ret.allDamage = new ArrayList<Pair<Integer, List<Integer>>>();
            ret.skill = changeSkilliId(lea.readInt());  //技能ID
            ISkill skill = SkillFactory.getSkill(ret.skill);

            lea.skip(1); //01
            ret.move = lea.readByte();//是否移动角色


            ParseMovement(chr, lea, ret);

            int sourceid = ret.skill;
            //斗气爆裂 连环吸血 终极投掷 幻影狼牙 钻石星辰 战神之盾
            if (sourceid == 战神.斗气爆裂
                    || sourceid == 战神.连环吸血
                    || sourceid == 战神.终极投掷
                    || sourceid == 战神.幻影狼牙
                    || sourceid == 战神.钻石星辰
                    || sourceid == 战神.战神之盾) {
                chr.setCombo(1);
            }

            lea.skip(4);
            if ((skill != null && skill.hasCharge()) || (ret.skill == 5301001)) {//猴子炸药桶
                ret.charge = lea.readInt();
            } else {
                ret.charge = 0;
            }
            //这段格式修改不确定有效性
            lea.readByte();
            ret.aranCombo = lea.readByte(); //这是目前的连击数?
            ret.pos = lea.readByte(); //动作
            ret.stance = lea.readByte(); //姿势 0x80
            ret.lastAttackTickCount = lea.readInt(); // Ticks
            lea.readInt();//4位
            lea.readByte();
            ret.speed = lea.readByte();//速度
        /*    if (sourceid == 4211006) { //金钱炸弹
             return parseMesoExplosion(lea, ret, chr);
             }*/

            lea.skip(8); //0

            /*
             * log.debug("近身攻击信息如下："); log.debug("技能编号: " + sourceid);
             * log.debug("连击数: " + ret.aranCombo); log.debug("动作：" + ret.pos);
             * log.debug("姿势：" + ret.stance); log.debug("武器攻击速度: " + ret.speed);
             * log.debug("打到怪物个数: " + ret.numAttacked);
             */

            ParseDamage(lea, ret, skill, chr);

            ret.point = lea.readPos();

            switch (ret.skill) {
                case 1211002:
                    ret.unk1 = 0x46;
                    break;
            }

        } catch (Exception ex) {
            log.error("转换近距离攻击错误：" + "damage：" + ret.allDamage
                    + "\r\npacket：" + lea.toString()
                    + "\r\nplayerName：" + chr.getName()
                    + "\r\nskillId：" + ret.skill
                    + "\r\nskillName：" + SkillFactory.getSkillName(ret.skill)
                    + "\r\nskillLevel:" + chr.getSkillLevel(SkillFactory.getSkill(ret.skill))
                    + "\r\n是否拥有精灵的祝福:" + chr.getCygnusBless()
                    + "\r\n", ex);
        }
        return ret;
    }

    public AttackInfo parseMesoExplosion(LittleEndianAccessor lea, AttackInfo ret, MapleCharacter chr) {
        if (ret.numAttackedAndDamage == 0) { //没打怪
            //log.debug("没打怪 只爆破钱");
            lea.skip(12);
            int bullets = lea.readByte();
            //log.debug("钱的数量1: "+bullets);
            for (int j = 0; j < bullets; j++) {
                int mesoid = lea.readInt();
                //log.debug("金钱oid1: "+mesoid);
                lea.skip(2);
                ret.allDamage.add(new Pair<Integer, List<Integer>>(Integer.valueOf(mesoid), null));
            }
            return ret;
        } else {
            lea.skip(8);
            //log.debug("打怪了");
        }
        //log.debug("ret.numAttacked + 1的值(大循环多少次)："+ret.numAttacked + 1);
        for (int i = 0; i < ret.numAttacked + 1; i++) {
            //log.debug("循环");
            int oid = lea.readInt();
            if (i < ret.numAttacked) {
                //log.debug("怪物的oid: "+oid);
                lea.skip(12);
                int bullets = lea.readByte();
                //log.debug("钱的数量2: "+bullets);
                List<Integer> allDamageNumbers = new ArrayList<Integer>();
                for (int j = 0; j < bullets; j++) {
                    int damage = lea.readInt();
                    if (oid > 999999) {
                        savePacket = true;
                    }
                    //log.debug("金钱炸伤害:  "+damage);
                    allDamageNumbers.add(Integer.valueOf(damage));
                }
                ret.allDamage.add(new Pair<Integer, List<Integer>>(Integer.valueOf(oid), allDamageNumbers));
                lea.skip(12);
            } else {
                int bullets = lea.readByte();
                //log.debug("钱的数量3: "+bullets);
                for (int j = 0; j < bullets; j++) {
                    int mesoid = lea.readInt();
                    //log.debug("金钱oid2: "+mesoid);
                    lea.skip(2);
                    ret.allDamage.add(new Pair<Integer, List<Integer>>(Integer.valueOf(mesoid), null));
                }
            }
        }
        if (savePacket && !ret.allDamage.isEmpty()) {
            WriteToFile re = new WriteToFile("ParseMesoExplosion.txt");
            re.WriteFile("damage：" + ret.allDamage
                    + "\r\npacket：" + lea.toString()
                    + "\r\nplayerName：" + chr.getName()
                    + "\r\nskillId：" + ret.skill
                    + "\r\nskillName：" + SkillFactory.getSkillName(ret.skill)
                    + "\r\nskillLevel:" + chr.getSkillLevel(SkillFactory.getSkill(ret.skill))
                    + "\r\n");
        }
        return ret;
    }

    private void Odinms(LittleEndianAccessor lea, MapleCharacter chr) {
        MovementParse.parseMovement(lea);
        //    if (chr != null) {
        //        MovePlayerHandler.MovePlayerHandler(movementlist, chr, true);
        //    }
    }

    private void Moveme(LittleEndianAccessor lea) {
        double skip = lea.readByte();
        skip = skip / 2;
        lea.skip((int) Math.ceil(skip));
    }

    private static int changeSkilliId(int skillID) {
        int changeID = skillID;
        switch (skillID) {
            case 21110007: //全力挥击
            case 21110008:
                changeID = 21110002;
                break;
            case 21120009: //战神之舞
            case 21120010:
                changeID = 21120002;
                break;
            case 32001008: //惩戒
            case 32001009:
            case 32001010:
            case 32001011:
                changeID = 32001001;
                break;
            case 35101010: //强化机枪扫射
                changeID = 35001004;
                break;
            case 35101009: //强化火焰喷射器
                changeID = 35001001;
                break;
            case 4321001: //龙卷风(攻击)
                changeID = 4321000;
                break;

            default:
                changeID = skillID;
                break;
        }

        switch (skillID) {
            case 5300007:
                changeID = 5301001;
                break;
            case 23101007:
                changeID = 23101001;
                break;

            /*
             * case 31001006://恶魔血月。 case 31001007: case 31001008: changeID =
             * 31000004; break;
             */
        }
        return changeID;
    }

    private boolean isPartyBuff(int skill1) {
        //如果技能有范围 但是不是组队技能的话就在这里false
        switch (skill1) {
            case 冰雷.快速移动精通:
            case 火毒.快速移动精通:
            case 牧师.快速移动精通:
            case 战法.快速移动精通:
                return true;
        }
        return false;
    }

    public int maxDamage(MapleCharacter chr, AttackInfo ret, int damage) {
        boolean addmaxAttack = chr.getClient().getChannelServer().isUseAddMaxAttack();
        ISkill skill = SkillFactory.getSkill(ret.skill);
        int Dm = 999999;
        /* if (skill != null) {
         Dm = skill.getEffect(1).getMDamageOver();
         } else {*/
        //   Dm = 999999;
        if (addmaxAttack) {
            Dm = 1000999999;
        }
        if (skill != null) {
            Dm += skill.getEffect(1).getMDamageOver();
        }
        //   }
        if (damage > (Dm + 14999999)) {
            return 1;
        }
        if (addmaxAttack) {
            return damage;
        }
        double randomNum = Math.random();
        randomNum = Math.max(randomNum, 0.7);
        int ak = 0;
        int str = chr.getStr(), dex = chr.getDex(), luk = chr.getLuk(), _int = chr.getInt();
        for (IItem iItem : chr.getInventory(MapleInventoryType.EQUIPPED).AllItems()) {
            if (iItem != null && iItem instanceof IEquip) {
                IEquip eqp1 = (IEquip) iItem;
                ak += (eqp1.getDex() + eqp1.getInt() + eqp1.getLuk() + eqp1.getStr()) * 1.5;
                ak += eqp1.getWatk() * 6 + eqp1.getMatk() * 6;
                str += eqp1.getStr();
                dex += eqp1.getDex();
                luk += eqp1.getLuk();
                _int += eqp1.getInt();
            }
        }
        if (chr.getVip() <= 4 && (str < 32767 || dex < 32767 || luk < 32767 || _int < 32767) && damage > 1000000000) {
            chr.弹窗("请禁止使用非法软件BUG技能否则封号处理");
            return 1;
        }
        if (chr.getClient().getChannelServer().isUseAddMaxAttack()) {
            return damage;
        }

        ak += chr.getStr();
        ak += chr.getDex();
        ak += chr.getInt();
        ak += chr.getLuk();
        ak *= 1 + (chr.getfs() * 0.1);
        int admge = ak;
        if (ret.skill != 14101006) {
            //普通职业战士      骑士团战士      战神       恶魔猎手 米哈尔

            if (chr.getJobid() >= 0 && chr.getJobid() <= 132 || chr.getJobid() >= 1100 && chr.getJobid() <= 1111 || chr.getJobid() >= 2000 && chr.getJobid() <= 2112 || chr.getJobid() >= 3100 && chr.getJobid() <= 3112
                    || chr.getJobid() >= 5000 && chr.getJobid() <= 5112) {
                damage = (int) ((double) chr.getStr() * randomNum + (double) (chr.getDex() / 2)) * 25 + admge * (chr.getVip() + 1);
            }
            //普通魔法师        骑士团魔法师        龙神        反抗者幻灵斗师
            if (chr.getJobid() >= 200 && chr.getJobid() <= 232 || chr.getJobid() >= 1200 && chr.getJobid() <= 1211 || chr.getJobid() >= 2001 && chr.getJobid() <= 2218 || chr.getJobid() >= 3200 && chr.getJobid() <= 3212) {
                damage = (int) ((double) chr.getLuk() * randomNum + (double) (chr.getDex() / 4)) * 25 + admge * (chr.getVip() + 1);
            }
            //普通弓箭手        骑士团弓箭手    反抗者弩豹游侠     双弩精灵  
            if (chr.getJobid() >= 300 && chr.getJobid() <= 322 || chr.getJobid() >= 1300 && chr.getJobid() <= 1311 || chr.getJobid() >= 3300 && chr.getJobid() <= 3312 || chr.getJobid() >= 2300 && chr.getJobid() <= 2312) {
                damage = (int) ((double) chr.getDex() * randomNum + (double) (chr.getStr() / 4)) * 25 + admge * (chr.getVip() + 1);
            }
            //普通飞侠          骑士团飞侠      暗影双刀
            if (chr.getJobid() >= 400 && chr.getJobid() <= 422 || chr.getJobid() >= 1400 && chr.getJobid() <= 1412 || chr.getJobid() >= 430 && chr.getJobid() <= 434 || chr.getJobid() >= 2400 && chr.getJobid() <= 2412) {
                damage = (int) ((double) chr.getLuk() * randomNum + (double) (chr.getDex() / 4)) * 25 + admge * (chr.getVip() + 1);
            }
            //普通海盗          反抗者机械师     火炮手        尖兵
            if (chr.getJobid() >= 500 && chr.getJobid() <= 522 || chr.getJobid() >= 1500 && chr.getJobid() <= 1511 || chr.getJobid() >= 3500 && chr.getJobid() <= 3512 || chr.getJobid() >= 501 && chr.getJobid() <= 532 || chr.getJobid() >= 3002 && chr.getJobid() <= 3612) {
                damage = (int) ((double) chr.getLuk() * randomNum + (double) (chr.getDex() / 4)) * 25 + admge * (chr.getVip() + 1);
            }
            damage *= (1 + ((chr.getVip() <= 0 ? 1 : chr.getVip()) * 0.05));
            damage += (damage * ((chr.getReborns() + chr.getfs() * 200) * 0.001));
            damage = (int) (damage * randomNum);


            if (damage > 999999) {
                //chr.公告("攻击力:" + damage);
                //chr.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "破攻实际伤害:" + damage));
                chr.getClient().getSession().write(MaplePacketCreator.sendHint(String.format("#e破攻伤害:#r%d", damage), 148, 5));
            }
        }
        //System.out.print("打怪伤害: " + damage + "\r\n");
        return damage;
    }
}
