package net.sf.odinms.net.channel.handler;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.client.skills.机械师;
import net.sf.odinms.client.status.MonsterStatusEffect;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.MapleSummon;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class SummonDamageHandler extends AbstractMaplePacketHandler {

    public class SummonAttackEntry {

        private int monsterOid;
        private int damage;

        public SummonAttackEntry(int monsterOid, int damage) {
            this.monsterOid = monsterOid;
            this.damage = damage;
        }

        public int getMonsterOid() {
            return monsterOid;
        }

        public int getDamage() {
            return damage;
        }
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        //System.out.println("SummonDamage 封包: "+slea.toString());

        int oid = slea.readInt();
        slea.skip(5);
        MapleCharacter player = c.getPlayer();
        if (!player.isAlive()) {
            return;
        }
        MapleSummon summon = null;
        boolean is磁场 = false;
        boolean is磁场攻击 = false;
        //System.out.println("接收到的召唤兽的oid"+oid);
        /*for (List<MapleSummon> sums : c.getPlayer().getSummons().values()) {
         for (MapleSummon sum : sums) {
         //System.out.println("召唤兽的oid"+sum.getObjectId());
         if (sum.getObjectId() == oid) {
         summon = sum;
         break;
         }
         }
         }*/
        Object obj = c.getPlayer().getMap().getMapObject(oid);
        if (obj instanceof MapleSummon) {
            summon = (MapleSummon) obj;
        }
        if (summon == null) {
            //System.out.println("召唤兽伤害被拦截");
            return;
        }
        int skillid;
        int skilllevel;
        Point pos;
        skillid = summon.getSkill();
        skilllevel = summon.getSkillLevel();
        pos = summon.getPosition();
        ISkill summonSkill = SkillFactory.getSkill(skillid);
        MapleStatEffect summonEffect = summonSkill.getEffect(skilllevel);
        List<SummonAttackEntry> allDamage = new ArrayList<SummonAttackEntry>();
        int numAttacked = changeNumAttacked(slea.readByte(), skillid); //这里还读了一个byte
        int numAccackMonster = numAttacked >> 4;
        int numAccackMonsterCount = numAttacked & 0xf;
        //System.out.println("攻击怪的个数: " + numAttacked);
        player.getCheatTracker().checkSummonAttack();
        int oid1 = slea.readInt(); //第一个磁场的oid
        if (skillid == 机械师.磁场) {
            //is磁场 = true;
            for (List<MapleSummon> sums : c.getPlayer().getSummons().values()) {
                for (MapleSummon sum : sums) {
                    if (sum.getObjectId() == oid1) {
                        is磁场攻击 = true;
                        oid = oid1;
                        break;
                    }
                }
            }
            if (is磁场攻击) {
                slea.skip(4); //第二个磁场的oid
                slea.skip(4); //第三个磁场的oid
                slea.skip(4); //没用的
            }
        }
        slea.skip(4);
        slea.skip(4);//00
        for (int x = 0; x < numAccackMonster; x++) {
            int monsterOid = slea.readInt(); // attacked oid
            slea.skip(4); //mobid 对应的怪物在WZ里的id
            slea.skip(19);
            int damage = slea.readInt();

            for (int i = 0; i < numAccackMonsterCount; i++) {
            }//以后召唤兽可能会多重攻击

            slea.skip(8);
            //System.out.println("mobid: " + monsterOid);
            //System.out.println("打怪伤害: " + damage);
            allDamage.add(new SummonAttackEntry(monsterOid, damage));
        }
        if (!player.isAlive()) {
            player.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
            return;
        }
        player.getMap().broadcastMessage(player, MaplePacketCreator.summonAttack(player, oid, 4, allDamage, numAttacked), pos);
        for (SummonAttackEntry attackEntry : allDamage) {
            int damage = attackEntry.getDamage();
            //System.out.println("遍历回来的伤害："+damage);
            MapleMonster target = player.getMap().getMonsterByOid(attackEntry.getMonsterOid());
            if (target != null) {
                if (damage > 0 && summonEffect.getMonsterStati().size() > 0) {
                    if (summonEffect.makeChanceResult()) {
                        MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(summonEffect.getMonsterStati(), summonSkill, false);
                        target.applyStatus(player, monsterStatusEffect, summonEffect.isPoison(), 4000);
                    }
                }
                if (damage > 30000) {
                    damage = 30000;
                }
                player.getMap().damageMonster(player, target, damage);
                player.checkMonsterAggro(target);
                //System.out.println("target == null");
            }
        }
        if (是自爆召唤兽(skillid) || is磁场 && !is磁场攻击) {
            //System.out.println("机械召唤兽的removeSpecialMapObject特殊处理");
            player.getMap().broadcastMessage(MaplePacketCreator.removeSpecialMapObject(summon, true));
            player.getMap().removeMapObject(summon);
            player.removeVisibleMapObject(summon);
            //player.getSummons().remove(skillid);
            player.removeSummon(skillid);
        }
    }

    private int changeNumAttacked(int numAttacked, int skillid) {
        int a = numAttacked;
        if (skillid == 机械师.人造卫星2) {
            a = 2;
        } else if (skillid == 机械师.人造卫星3) {
            a = 3;
        }
        return a;
    }

    private boolean 是自爆召唤兽(int skillid) {
        switch (skillid) {
            //case 35111002: // 机械师 - 磁场
            case 35111005: // 机械师 - 加速器：EX-7
            case 35111011: // 机械师 - 治疗机器人：H-LX
            //case 35121003: // 机械师 - 战争机器：泰坦
            case 35121009: // 机械师 - 机器人工厂：RM1
            case 35121010: // 机械师 - 放大器：AF-11
            case 33101008: // 弩骑 - 地雷 自爆
            case 35121011: // 机器人工厂召唤技能
                return true;
        }
        return false;
    }
}
