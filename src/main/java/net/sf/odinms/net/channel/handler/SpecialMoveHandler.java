//召唤兽移动?
package net.sf.odinms.net.channel.handler;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import net.sf.odinms.client.MapleCharacter.CancelCooldownAction;
import net.sf.odinms.client.*;
import net.sf.odinms.client.messages.ServerNoticeMapleClientMessageCallback;
import net.sf.odinms.client.skills.弩骑;
import net.sf.odinms.client.skills.战法;
import net.sf.odinms.client.skills.战神;
import net.sf.odinms.client.skills.机械师;
import net.sf.odinms.client.status.MonsterStatusEffect;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class SpecialMoveHandler extends AbstractMaplePacketHandler {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SpecialMoveHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        //System.out.println("SpecialMove："+slea.toString());
        slea.readInt();
        int skillid = changeSkillId(slea.readInt(), c.getPlayer());//技能ID
        MapleCharacter chr = c.getPlayer();
        Point pos = null;
        int __skillLevel = slea.readByte();//技能等级
        if (skillid == 23111008 && (Math.random() > 0.5)) {
            skillid++;
            if (Math.random() > 0.5) {
                skillid++;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("请求使用的ID：" + skillid);
        }
        ISkill skill = SkillFactory.getSkill(skillid);

        if (c.getPlayer().getMapId() >= 910000021 && c.getPlayer().getMapId() <= 910000022 && !c.getPlayer().isGM()) {
            c.getPlayer().dropMessage(1, "洞内无法使用技能.....");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }


        if (c.getPlayer().getMapId() == 910000000 && !c.getPlayer().isGM() && c.getChannel() != 4) {
            //   c.getPlayer().dropMessage(1, "市场无法使用技能.....");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        int skillLevel = chr.getSkillLevel(skill);
        skillLevel = __skillLevel;
        MapleStatEffect effect = skill.getEffect(skillLevel);
        chr.resetAfkTimer();
        int beforeMp = chr.getMp();
        int 磁场数量 = 0;
        if (skillLevel == 0) {
            log.info("[异常] " + chr.getName() + " 玩家使用了等级异常的技能");
            c.disconnect();
            return;
        } else if (!chr.is能否使用进阶灵气()) {
            chr.cancelEffect(effect, true, -1);
            chr.set能否使用进阶灵气(true);
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        } else {
            if (skillid % 10000000 == 1010 || skillid % 10000000 == 1011) {
                skillLevel = 1;
                chr.setDojoEnergy(0);
                c.getSession().write(MaplePacketCreator.getEnergy(0));
            }
            if (effect.getCooldown() > 0) { //如果有冷却时间
                if (chr.skillisCooling(effect.getColldownId())) {
                    return;
                } else {
                    if (skillid != 5221006
                            || (skillid == 机械师.磁场
                            && chr.getSummons().get(机械师.磁场).size() == 2)) { //武装不设置冷却时间
                        c.getSession().write(MaplePacketCreator.skillCooldown(effect.getColldownId(), effect.getCooldown()));
                        ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(chr, effect.getColldownId()), effect.getCooldown() * 1000);
                        chr.addCooldown(effect.getColldownId(), System.currentTimeMillis(), effect.getCooldown() * 1000, timer);
                    }
                }
            }

            switch (skillid) {
                case 1121001://磁石
                case 1221001://磁石
                case 1321001://磁石
                    int num = slea.readInt();
                    int mobId;
                    byte success;
                    for (int i = 0; i < num; i++) {
                        mobId = slea.readInt();
                        success = slea.readByte();
                        chr.getMap().broadcastMessage(chr, MaplePacketCreator.showMagnet(mobId, success), false);
                        MapleMonster monster = chr.getMap().getMonsterByOid(mobId);
                        if (monster != null) {
                            monster.switchController(chr, monster.isControllerHasAggro());
                        }
                    }
                    byte direction = slea.readByte();
                    chr.getMap().broadcastMessage(chr, MaplePacketCreator.showBuffeffect(chr.getId(), skillid, 1, direction, 1), false);
                    c.getSession().write(MaplePacketCreator.enableActions());
                    break;
            }

            if (MapleStatEffect.isMonsterRiding(skillid)) {
                //log.info("执行了SpecialMove 属于骑宠技能");
                slea.readShort();
            }
            if (skillid == 4341003 || skillid == 4121004 || skillid == 4221004) { //怪物炸弹 忍者伏击
                int hasMonster = slea.readByte();
                boolean isPoison = false;
                if (skillid == 4121004 || skillid == 4221004) {
                    isPoison = true;
                }
                if (hasMonster > 0) {
                    for (int i = 0; i < hasMonster; i++) {
                        int mobid = slea.readInt();//怪的Object id
                        MapleMonster monster = c.getPlayer().getMap().getMonsterByOid(mobid);
                        MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(effect.getMonsterStati(), skill, false);
                        monster.applyStatus(chr, monsterStatusEffect, isPoison, effect.getDuration());
                    }
                }
            } else if (skillid == 弩骑.吞噬) {
                //log.info("吞噬技能");
                MapleMonster monster = c.getPlayer().getMap().getMonsterByOid(c.getPlayer().获得吞噬的怪的id());
                if (monster != null) {
                    monster.switchController(chr, monster.isControllerHasAggro());
                }
                c.getPlayer().getMap().killMonster2(monster, chr);
            } else if (slea.available() >= 5) {
                if (skillid == 机械师.磁场) {
                    磁场数量 = slea.readByte(); //0 - 第一个磁场 1 - 第二个磁场 2 - 第三个磁场[召唤磁场效果]
                }
                /*       if (磁场数量 == 2) {
                 pos = new Point(c.getPlayer().getPosition());
                 } else {
                 pos = new Point(slea.readShort(), slea.readShort());
                 }*/

                //log.info("执行了SpecialMove的坐标设置："+pos);
                //log.info("实际的人物坐标："+c.getPlayer().getPosition());
            }

            if (skillid == 机械师.加速器) {
                MapleMap map = c.getPlayer().getMap();
                double range = Double.POSITIVE_INFINITY;
                List<MapleMapObject> monsters = map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER));
                for (MapleMapObject monstermo : monsters) {
                    MapleMonster monster = (MapleMonster) monstermo;
                    MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(effect.getMonsterStati(), skill, false);
                    monster.applyStatus(chr, monsterStatusEffect, false, effect.getDuration());
                }
            }
            if (chr.isAlive()) {

                if (skillid == 9001004 && chr.isGM()) {
                    chr.setHidden(!chr.isHidden());
                } else if (skillid == 战法.进阶黑暗灵气 || skillid == 战法.进阶蓝色灵气 || skillid == 战法.进阶黄色灵气) {
                    chr.set能否使用进阶灵气(false);
                }
                if (skill.getId() != 2311002 || chr.canDoor()) {
                    if (pos == null && slea.available() > 4) {
                        pos = slea.readPos();
                    }
                    effect.applyTo(chr, pos); //给buff
                    if (skillid == 机械师.磁场 && 磁场数量 == 2) {
                        //产生磁场
                        chr.getMap().broadcastMessage(MaplePacketCreator.召唤磁场(chr.getId(),
                                chr.getSummons().get(机械师.磁场).get(0).getObjectId(),
                                chr.getSummons().get(机械师.磁场).get(1).getObjectId(),
                                chr.getSummons().get(机械师.磁场).get(2).getObjectId()));
                    } else if (skillid == 4341006) { //傀儡召唤 用时会取消镜像分身
                        skill = SkillFactory.getSkill(4331002);
                        effect = skill.getEffect(chr.getSkillLevel(skill));
                        chr.cancelEffect(effect, false, -1);
                    } else if (skillid == 战神.斗气重生) {
                        //y项 50 + 5 * x
                        int combo = 5 + skillLevel / 2;
                        int comboCount = 50 + 5 * skillLevel;
                        SkillFactory.getSkill(21000000).getEffect(combo > 10 ? 10 : combo).applyTo(chr);
                        chr.setCombo(comboCount);
                        chr.getClient().getSession().write(MaplePacketCreator.Combo_Effect(comboCount));
                    } else if (skillid == 36001005) {//火箭炮
                        chr.reducePower(1);
                        int count = slea.readByte();
                        ArrayList<Integer> monsters = new ArrayList<Integer>();
                        for (int i = 0; i < count; i++) {
                            int oid = slea.readInt();
                            if (chr.getMap().getCore().contains(oid)) {
                                monsters.add(oid);
                            }
                        }
                        if (!monsters.isEmpty()) {
                            chr.getMap().broadcastMessage(MaplePacketCreator.getAttackEffect(c.getPlayer().getId(), skillid, monsters, 6));
                        }
                    }
                    if (skill.getId() != 2301002 && effect != null && effect.getMpCon() != 0) { //群体治愈
                        if (chr.getMp() - beforeMp < skill.getEffect(skillLevel).getMpCon()) {
                            int remainingMp = beforeMp - skill.getEffect(skillLevel).getMpCon();
                            chr.setMp(remainingMp);
                            chr.updateSingleStat(MapleStat.MP, remainingMp);
                        }
                    }
                } else if (!chr.canDoor()) {
                    new ServerNoticeMapleClientMessageCallback(5, c).dropMessage("请等候5秒再使用时空门!");
                    c.getSession().write(MaplePacketCreator.enableActions());
                }
            }
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public int changeSkillId(int skillid, MapleCharacter chr) {
        int a = skillid;
        if (skillid == 机械师.金属机甲_重机枪 && chr.getBuffedValue(MapleBuffStat.机械师) != null) {
            a = 35121013;
        } else if (skillid == 战法.黑暗灵气 && chr.getSkillLevel(战法.进阶黑暗灵气) > 0) {
            a = 战法.进阶黑暗灵气;
        } else if (skillid == 战法.蓝色灵气 && chr.getSkillLevel(战法.进阶蓝色灵气) > 0) {
            a = 战法.进阶蓝色灵气;
        } else if (skillid == 战法.黄色灵气 && chr.getSkillLevel(战法.进阶黄色灵气) > 0) {
            a = 战法.进阶黄色灵气;
        }
        return a;
    }
}
