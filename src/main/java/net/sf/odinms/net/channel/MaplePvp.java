/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel;

import net.sf.odinms.client.*;
import net.sf.odinms.net.channel.handler.DamageParseHandler;
import net.sf.odinms.net.channel.handler.DamageParseHandler.AttackInfo;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.tools.MaplePacketCreator;

/**
 *
 * @since 1.6
 * @author Administrator
 */
public class MaplePvp {

    public static int PVP_CHANNEL;
    public static int PVP_MAP;
    private static int pvpDamage;
    private static int maxDis;
    private static int maxHeight;
    private static boolean isAoe = false;
    public static boolean isLeft = false;
    public static boolean isRight = false;

    /**
     *
     * @param attack
     * @return
     */
    private static boolean isMeleeAttack(DamageParseHandler.AttackInfo attack) {
        switch (attack.skill) {
            case 1001004://强力攻击
            case 1001005://群体攻击
            case 1111003://恐慌
            //case 1111004:
            case 1121006://突进
            case 1121008://勇猛劈砍
            case 1221007://突进
            case 1221009://连环环破
            case 1311001://龙连击
            //case 1311002:
            case 1311003://龙挥砍
            //case 1311004:
            case 1311005://龙之献祭
            case 1321003://突进
            case 4001334://二连击
            case 4201005://回旋斩
            case 4221001://暗杀
            case 4321004://悬浮地刺
            case 4331000://血雨腥风
            case 4331006://地狱锁链
            case 4341004://暴怒刀阵
            case 4341009://幽灵一击
            case 5001002://半月踢
            case 5101003://升龙连击
            case 5101004://贯骨击
                return true;
        }
        return false;
    }

    private static boolean isRangeAttack(DamageParseHandler.AttackInfo attack) {
        switch (attack.skill) {
            case 2001004://魔法弹
            case 2001005://魔法双击
            case 2101004://火焰箭
            case 2101005://毒雾术
            case 2111006://火凤球
            case 2121003://迷雾爆发
            case 2201004://冰冻术
            case 2211002://冰咆哮
            case 2211003://落雷枪
            case 2211006://冰凤球
            case 2221003://冰河锁链
            case 2221006://链环闪电
            case 2301005://圣箭术
            case 2321007://光芒飞箭
            case 3001004://断魂箭
            case 3001005://二连射
            case 3111006://箭扫射
            //case 3121003:
            case 3121004://暴风箭雨
            case 3211006://箭扫射
            case 3221001://穿透箭
            //case 3221003:
            case 3221007://一击要害箭
            case 4001344://双飞斩
            case 4101005://生命吸收
            case 4111004://金钱攻击
            case 4111005://多重飞镖
            case 4121003://挑衅
            case 4121007://三连环光击破
            case 4211002://炼狱
            case 4221003://挑衅
            case 4221007://一出双击
            case 5001003://双弹射击
            case 5101002://回马
            case 5201001://麦林弹
            case 5201002://投弹攻击
            case 5201004://迷惑射击
            case 5201006://激退射杀
            case 5211004://烈焰喷射
            case 5211005://寒冰喷射
            case 5221004://金属风暴
            case 11101004://灵魂之刃
            case 12101002://火焰箭
            case 12111006://火风暴
            case 23121000://伊师塔之环
            case 33121009://奥义箭乱舞
                return true;
        }
        return false;
    }

    private static boolean isAoeAttack(DamageParseHandler.AttackInfo attack) {
        switch (attack.skill) {
            case 1111005://昏迷
            //case 1111006:
            case 1211002://属性攻击
            case 1221011://圣域
            case 1311006://龙咆哮
            case 2111002://末日烈焰
            case 2111003://致命毒雾
            case 2121001://创世之破
            case 2121006://美杜莎之眼
            case 2121007://天降落星
            case 2201005://雷电术
            case 2221001://创世之破
            case 2221007://落霜冰破
            case 2311004://圣光
            case 2321001://创世之破
            case 2321008://圣光普照
            case 3101005://爆炸箭
            case 3111003://烈火箭
            case 3111004://箭雨
            case 3201005://穿透箭
            case 3211003://寒冰箭
            case 3211004://升龙弩
            case 4121004://忍者伏击
            case 4121008://忍者冲击
            case 4211004://分身术
            case 4221004://忍者伏击
            case 4311003://双刀风暴
            case 4331005://暗影飞跃斩
            case 5111006://碎石乱击
            case 5121001://潜龙出渊
            case 12101006://火柱
            case 12111003://天降落星
            case 15111007://鲨鱼波
            case 23121002://传说之矛
            case 33121001://闪光箭雨
            case 33121002://音速震波
                return true;
        }
        return false;
    }

    private static void getDirection(DamageParseHandler.AttackInfo attack) {
        if (isAoe) {
            isRight = true;
            isLeft = true;
        } else if (attack.direction <= 0 && attack.stance <= 0) {
            isRight = false;
            isLeft = true;
        } else {
            isRight = true;
            isLeft = false;
        }
    }

    private static void DamageBalancer(AttackInfo attack) {
        if (attack.skill == 0) {
            pvpDamage = (int) Math.floor(Math.random() * 30 + 5);
            maxDis = 130;
            maxHeight = 35;
        } else if (isMeleeAttack(attack)) {
            maxDis = 130;
            maxHeight = 45;
            isAoe = false;
            if (attack.skill == 4201005) {//回旋斩
                pvpDamage = (int) Math.floor(Math.random() * 30 + 5);
            } else if (attack.skill == 1121008) {//勇猛劈砍
                pvpDamage = (int) Math.floor(Math.random() * 70 + 90);
                maxHeight = 50;
            } else if (attack.skill == 4221001) {//暗杀
                pvpDamage = (int) Math.floor(Math.random() * 50 + 50);
            } else if ((attack.skill == 1121006) || (attack.skill == 1221007) || (attack.skill == 1321003)) {//突进 
                pvpDamage = (int) Math.floor(Math.random() * 60 + 80);
            } else {
                pvpDamage = (int) Math.floor(Math.random() * 60 + 50);
            }
        } else if (isRangeAttack(attack)) {
            maxDis = 300;
            maxHeight = 40;
            isAoe = false;
            if (attack.skill == 4201005) {//回旋斩
                pvpDamage = (int) Math.floor(Math.random() * 40 + 30);
            } else if (attack.skill == 4121007) {//三连环光击破
                pvpDamage = (int) Math.floor(Math.random() * 65 + 60);
            } else if ((attack.skill == 4001344) || (attack.skill == 2001005)) {//双飞斩    魔法双击
                pvpDamage = (int) Math.floor(Math.random() * 45 + 40);
            } else if (attack.skill == 4221007) {//一出双击
                pvpDamage = (int) Math.floor(Math.random() * 70 + 60);
            } else if ((attack.skill == 3121004) || (attack.skill == 5221004)) {//暴风箭雨  金属风暴
                maxDis = 450;
                pvpDamage = (int) Math.floor(Math.random() * 50 + 30);
            } else if ((attack.skill == 3111006) || (attack.skill == 3211006)) {//箭扫射
                maxDis = 450;
                pvpDamage = (int) Math.floor(Math.random() * 60 + 60);
            } else if ((attack.skill == 2121003) || (attack.skill == 2221003)) {//迷雾爆发  冰河锁链
                pvpDamage = (int) Math.floor(Math.random() * 100 + 80);
            } else {
                pvpDamage = (int) Math.floor(Math.random() * 70 + 70);
            }
        } else if (isAoeAttack(attack)) {
            maxDis = 350;
            maxHeight = 350;
            isAoe = true;
            if ((attack.skill == 2121001) || (attack.skill == 2221001) || (attack.skill == 2321001) || (attack.skill == 2121006)) {//创世之破
                maxDis = 175;
                maxHeight = 175;
                pvpDamage = (int) Math.floor(Math.random() * 70 + 70);
            } else {
                pvpDamage = (int) Math.floor(Math.random() * 60 + 70);
            }
        } else {
            pvpDamage = (int) Math.floor(Math.random() * 50 + 60);
        }
    }

    /**
     *
     * @param player 玩家
     * @param attackedPlayers 攻击对象
     * @param map 地图
     * @param attack 攻击力
     */
    private static void monsterBomb(MapleCharacter player, MapleCharacter attackedPlayers, MapleMap map, AttackInfo attack) {
        //转生次数
        if (player.getReborns() > 0) {
            pvpDamage *= player.getReborns() * 0.01;
        }

        //VIP攻击提成
        if (player.getVip() == 1 || player.getVip() == 2) {
            pvpDamage *= 1;
        } else if (player.getVip() == 3 || player.getVip() == 4) {
            pvpDamage *= 1.1;
        } else if (player.getVip() == 5 || player.getVip() == 6) {
            pvpDamage *= 1.2;
        }

        //飞升提成
        if (player.getfs() == 1) {
            pvpDamage *= 1.3;
        } else if (player.getfs() == 2) {
            pvpDamage *= 1.4;
        } else if (player.getfs() == 3) {
            pvpDamage *= 1.5;
        } else if (player.getfs() == 4) {
            pvpDamage *= 1.6;
        } else if (player.getfs() == 5) {
            pvpDamage *= 1.7;
        }

        //GM提成
        if (player.getGm() >= 100) {
            pvpDamage *= 2000;
        }

        //字母W的攻击提成
        if (player.haveItem(3994081, 1, false, false)) {
            pvpDamage *= 2;
        }
        //同时拥有字母W和字母X的提成
        if (player.haveItem(3994082, 1, false, false)) {
            if (player.haveItem(3994081, 1, false, false)) {
                pvpDamage *= 2;
            } else {
                pvpDamage *= 4;
            }
        }

        pvpDamage += player.getTotalMagic() + player.getTotalWatk();//获取角色的物理与魔法攻击

        Integer mguard = attackedPlayers.getBuffedValue(MapleBuffStat.MAGIC_GUARD);
        Integer mesoguard = attackedPlayers.getBuffedValue(MapleBuffStat.MESOGUARD);
        if (mguard != null) {
            int mploss = (int) (pvpDamage / 0.5);
            pvpDamage = (int) (pvpDamage * 0.7);
            if (mploss > attackedPlayers.getMp()) {
                pvpDamage = (int) (pvpDamage / 0.7);
                attackedPlayers.cancelBuffStats(MapleBuffStat.MAGIC_GUARD);
            } else {
                attackedPlayers.setMp(attackedPlayers.getMp() - mploss);
                attackedPlayers.updateSingleStat(MapleStat.MP, attackedPlayers.getMp());
            }
        } else if (mesoguard != null) {
            int mesoloss = (int) (pvpDamage * 0.75);
            pvpDamage = (int) (pvpDamage * 0.75);
            if (mesoloss > attackedPlayers.getMeso()) {
                pvpDamage = (int) (pvpDamage / 0.75);
                attackedPlayers.cancelBuffStats(MapleBuffStat.MESOGUARD);
            } else {
                attackedPlayers.gainMeso(-mesoloss, false);
            }
        }

        pvpDamage /= 100;

        int selec = (int) Math.floor(Math.random() * 4) + 1;
        int randomed = (int) Math.floor(Math.random() * 88) + 1;

        MapleMonster pvpMob = MapleLifeFactory.getMonster(9400711);
        map.spawnMonsterOnGroundBelow(pvpMob, attackedPlayers.getPosition());
        int attackedDamage = 0;
        int attackedPlayerHp = attackedPlayers.getHp();
        for (int attacks = 0; attacks < attack.numDamage; attacks++) {
            map.broadcastMessage(MaplePacketCreator.damagePlayer(attack.numDamage, pvpMob.getId(), attackedPlayers.getId(), pvpDamage));
            attackedPlayers.addHP_(-pvpDamage);
            attackedDamage += pvpDamage;
        }

        //int attackedDamage = pvpDamage * attack.numDamage;//??这里 减去玩家的血只是pvpDamage 而这里还多了 会不会显示和扣的玩家血不一致？
        attackedPlayers.getClient().getSession().write(MaplePacketCreator.serverNotice(5, player.getName() + " 正在向你攻击了 " + attackedDamage + " 血!"));
        map.killMonster(pvpMob, player, false);
        if (attackedDamage >= attackedPlayerHp) {//当被攻击者死亡
            player.gainPvpKill();//增加攻击者的PK值
            player.getClient().getSession().write(MaplePacketCreator.serverNotice(6, "[注意]你勇猛的杀死了玩家： " + attackedPlayers.getName() + "!获得1点PK值."));
            attackedPlayers.gainPvpDeath();//增加被攻击者的死亡值
            attackedPlayers.getClient().getSession().write(MaplePacketCreator.serverNotice(6, "[注意]玩家：" + player.getName() + " 已经残忍的将你杀死!"));

//            player.saveToDB();
            //          attackedPlayers.saveToDB();
        }
        //  player.gainPvpKill();//增加攻击者的PK值
        //player.getClient().getSession().write(MaplePacketCreator.serverNotice(6, "[注意]你勇猛的杀死了玩家： " + attackedPlayers.getName() + "!获得1点PK值."));
        //   attackedPlayers.gainPvpDeath();//增加被攻击者的死亡值
        //attackedPlayers.getClient().getSession().write(MaplePacketCreator.serverNotice(6, "[注意]玩家：" + player.getName() + " 已经残忍的将你杀死!"));

        /*   if (hasBoot) {//当被攻击者死亡
         int gpReward = (int) Math.floor(Math.random() * 150 + 50);

          

         MapleInventory att = attackedPlayers.getInventory(MapleInventoryType.CASH);
         if (selec == 1) {
         //爆装备栏物品
         if (attackedPlayers.getInventory(MapleInventoryType.EQUIP).getItem((byte) randomed) != null) {
         IItem itemedid = attackedPlayers.getInventory(MapleInventoryType.EQUIP).getItem((byte) randomed).copy();
         if (!itemedid.友谊戒指() && !itemedid.恋人戒指() && !itemedid.结婚戒指()) {
         if (!itemedid.HasFlag(InventoryConstants.Items.Flags.锁定)) {
         if (att.findById(5060001) != null) {//封印之锁
         player.getClient().getSession().write(MaplePacketCreator.serverNotice(6, "很遗憾,对方有装备附身符,无法爆出装备!"));
         attackedPlayers.getClient().getSession().write(MaplePacketCreator.serverNotice(6, " 护身符保佑，您的装备免遭爆，损失一张护身符!"));
         MapleInventoryManipulator.removeById(attackedPlayers.getClient(), MapleItemInformationProvider.getInstance().getInventoryType(5060001), 5060001, 1, true, false);
         } else {
         MapleInventoryManipulator.removeFromSlot(attackedPlayers.getClient(), MapleInventoryType.EQUIP, (byte) randomed, (short) 1, true);
         attackedPlayers.getMap().spawnItemDrop(attackedPlayers, attackedPlayers, itemedid, attackedPlayers.getPosition(), true, true);
         }
         }
         }
         }
         } else {
         //爆设置栏物品
         MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
         if (attackedPlayers.getInventory(MapleInventoryType.ETC).getItem((byte) randomed) != null) {
         IItem itemedid = attackedPlayers.getInventory(MapleInventoryType.ETC).getItem((byte) randomed);
         if ((itemedid.getOwner().equals("") == true) && (itemedid.getOwner().equals("?????") == true)) {
         MapleInventoryManipulator.removeFromSlot(attackedPlayers.getClient(), MapleInventoryType.ETC, (byte) randomed, (short) 1, true);
         attackedPlayers.getMap().spawnItemDrop(attackedPlayers, attackedPlayers, ii.getEquipById(itemedid.getItemId()), attackedPlayers.getPosition(), true, true);
         }
         }
         }
         int random = (int) Math.floor(Math.random() * 30000000.0) + 1000000;

         //玩家掉钱
         if (attackedPlayers.getMeso() >= random && attackedPlayers.getMeso() > 0) {
         attackedPlayers.gainMeso(-random, true);
         attackedPlayers.getMap().spawnMesoDrop(random, attackedPlayers.getPosition(), attackedPlayers, attackedPlayers, true);
         }

         }*/
    }

    public static void doPvP(MapleCharacter player, MapleMap map, AttackInfo attack) {
        DamageBalancer(attack);
        getDirection(attack);
        player.getCheatTracker().resetHPRegen();
        player.resetAfkTimer();

        for (MapleCharacter attackedPlayers : player.getMap().getNearestPvpChar(player.getPosition(), maxDis, maxHeight, player.getMap().getCharacters())) {
            if (attackedPlayers.isAlive() && player.getParty() == null || player.getParty() != attackedPlayers.getParty() && !attackedPlayers.isGM()) {
                monsterBomb(player, attackedPlayers, map, attack);
            }
        }
    }

    public static void doGuildPvP(MapleCharacter player, MapleMap map, AttackInfo attack) {
        DamageBalancer(attack);
        getDirection(attack);
        player.getCheatTracker().checkAttack(attack.skill);
        for (MapleCharacter attackedPlayers : player.getMap().getNearestPvpChar(player.getPosition(), maxDis, maxHeight, player.getMap().getCharacters())) {
            if (attackedPlayers.isAlive() && !attackedPlayers.isGM() && player.getGuildid() != attackedPlayers.getGuildid()) {
                monsterBomb(player, attackedPlayers, map, attack);
            }
        }
    }

    public static void doGodwar(MapleCharacter player, MapleMap map, AttackInfo attack) {
        DamageBalancer(attack);
        getDirection(attack);
        player.getCheatTracker().checkAttack(attack.skill);
        for (MapleCharacter attackedPlayers : player.getMap().getNearestPvpChar(player.getPosition(), maxDis, maxHeight, player.getMap().getCharacters())) {
            if (attackedPlayers.isAlive() && !attackedPlayers.isGM() && attackedPlayers.haveItem(5010063, 1, false, false) != true) {
                monsterBomb(player, attackedPlayers, map, attack);
            }
        }
    }
}
