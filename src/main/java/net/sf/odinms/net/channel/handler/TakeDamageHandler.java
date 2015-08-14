package net.sf.odinms.net.channel.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleCharacter.CancelCooldownAction;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.client.status.MapleMonsterStat;
import net.sf.odinms.client.status.MonsterStatusEffect;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.life.MobAttackInfo;
import net.sf.odinms.server.life.MobAttackInfoFactory;
import net.sf.odinms.server.life.MobSkill;
import net.sf.odinms.server.life.MobSkillFactory;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class TakeDamageHandler extends AbstractMaplePacketHandler {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TakeDamageHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter player = c.getPlayer();
        //System.out.println("TakeDamage封包：" + slea.toString());
        //2F 00 [A4 2B 54 00] [FF] [00] [06 00 00 00] [B4 34 03 00] [EC 4B CB 09] 00 00 00 00 00 //089
        //31 00 [F4 9D 10 02] [FF] [00] [E3 01 00 00] [84 CD 6C 00] [AD D6 1C 04] 00 00 00 00 00 //092
        //31 00 [F5 9E 86 00] [FF] [00] [4D 05 00 00] [13 7C 92 00] [65 00 00 00] 00 8C 00 00 01 65 00 00 00 07 E4 02 40 FF DF 02 6C FF 00
        //38 00 [73 0A EE 01] [FF] [00] [14 00 00 00] [00 00] [5C 4A 31 00] [75 00 00 00] 00 00 00 00 00
        //4B 00 [09 4A 52 00] [FD] [00] [14 00 00 00] 00 00 00 00 00
        //4B 00 [1D 14 47 01] [FF] [00] [01 00 00 00] 00 00 A1 86 01 00 68 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
        //4B 00 [41 0E 47 01] FF 00 01 00 00 00 00 00 A1 86 01 00 68 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
        //4B 00 [47 08 47 01] FF 00 01 00 00 00 00 00 A1 86 01 00 68 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
        //4B 00 [6B 02 47 01] [FF] [00] [01 00 00 00] [00 00] [A1 86 01 00] [68 00 00 00] 00 00 00 00 00 00 00 00 00 00 00 00
        //4B 00 [F3 5D CA F0] [0D 2F 4B 00] [FD 00] [18 00 00 00] 00 00 00 00 00
        //4B 00 [AB DF E1 C7] [B3 30 1F 01] [FF 00] [01 00 00 00] 00 00 A1 86 01 00 7C 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
        /**
         * 4B 00 50 F0 80 29 43 49 23 00 FF 00 00 00 00 00 00 00 A1 86 01 00 64
         * 00 00 00 01 00 00 00 00 00 00 00 00 01 00 00
         */
        slea.skip(8);
        int damagefrom = slea.readByte();
        slea.skip(1);
        int damage = slea.readInt();//受伤数值 06 00 00 00
        int oid = 0;
        int monsteridfrom = 0;
        int direction = 0;
        int fake = 0;
        int mpattack = 0;
        MapleMonster attacker = null;
        //System.out.println("damagefrom类型：" + damagefrom + "，受伤：" + damage);
        if (damagefrom != -3) {
            slea.readShort(); //093新增
            if (slea.available() >= 4) { //防止除了毒和别的减血方法
                monsteridfrom = slea.readInt();//怪物ID
                oid = slea.readInt();// mob object id
                if (c.getPlayer().getMap() == null || c.getPlayer().getMap().getMapObject(oid) == null || !c.getPlayer().getMap().getMapObject(oid).getType().equals(MapleMapObjectType.MONSTER)) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                if (player.getMap().getMapObject(oid).getType() == MapleMapObjectType.MONSTER) {
                    attacker = (MapleMonster) player.getMap().getMapObject(oid);
                }
                direction = slea.readByte();
            }
        }
        //先更新次Hp
        List<Pair<MapleStat, Number>> stats = new ArrayList<Pair<MapleStat, Number>>();
        stats.add(new Pair<MapleStat, Number>(MapleStat.HP, Integer.valueOf(player.getHp())));
        player.getClient().getSession().write(MaplePacketCreator.updatePlayerStats(stats, player));

        if (damagefrom != -1 && damagefrom != -2 && attacker != null) {
            final MobAttackInfo attackInfo = MobAttackInfoFactory.getInstance().getMobAttackInfo(attacker, damagefrom);
            if (damage != -1) {
                if (attackInfo.isDeadlyAttack()) {
                    mpattack = player.getMp() - 1;
                } else {
                    mpattack += attackInfo.getMpBurn();
                }
            }
            MobSkill skill = MobSkillFactory.getMobSkill(attackInfo.getDiseaseSkill(), attackInfo.getDiseaseLevel());
            if (skill != null && damage > 0 && attacker != null) {
                skill.applyEffect(player, attacker, false);
            }
            if (attacker != null) {
                attacker.setMp(attacker.getMp() - attackInfo.getMpCon());
            }
        }
        if (damage == -1) { //受伤的数值 == -1
            int job = player.getJob().getId() / 10 - 40;
            fake = 4020002 + (job * 100000);
            if (damagefrom == -1 && damagefrom != -2
                    && player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10) != null //玩家有盾牌才发动守护之神
                    ) {
                //System.out.println("damage == -1 守护之神发动");
                ISkill guardianSkill = SkillFactory.getSkill(1220006); //守护之神(寒冰掌)
                if (player.getSkillLevel(guardianSkill) > 0 && attacker != null) {
                    MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MapleMonsterStat.STUN, 1), guardianSkill, false);
                    attacker.applyStatus(player, monsterStatusEffect, false, 2 * 1000);
                }
            }
        }
        if (damage == 0
                && c.getPlayer().getJob().IsDemonHunter()
                && player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10) != null
                && attacker != null
                && player.getSkillLevel(31110008) > 0
                && damagefrom == -1) {//有盾牌并且是恶魔
            c.getPlayer().addHP((int) (c.getPlayer().getCurrentMaxHp()
                    * (SkillFactory.getSkill(31110008).getEffect(player.getSkillLevel(31110008)).getY() / 100.0)));
            c.getPlayer().addMP(SkillFactory.getSkill(31110008).getEffect(player.getSkillLevel(31110008)).getZ());
            //c.getSession().write(MaplePacketCreator.GiveDf(attacker.getObjectId(), SkillFactory.getSkill(31110008).getEffect(player.getSkillLevel(31110008)).getZ()));
        }
        if ((damage < -1 || damage > 99999) && !player.isGM()) {
            log.info("[异常]" + player.getName() + " 接收到异常的怪物攻击数值 " + monsteridfrom + ": " + damage);
            c.disconnect();
            return;
        }
        player.getCheatTracker().checkTakeDamage();
        if (damage > 0) { //正常受伤
            player.getCheatTracker().setAttacksWithoutHit(0);
            player.getCheatTracker().resetHPRegen();
            player.getCheatTracker().resetMPRegen();
            player.resetAfkTimer();
        }
        if (damage == 1) {
            player.getCheatTracker().registerOffense(CheatingOffense.ALWAYS_ONE_HIT);
        }
        if (player.isHidden() || !player.isAlive()) {
            ////System.out.println("伤害未处理 可能角色已死或者是处于GM隐身状态");
            return;
        }
        ////System.out.println("执行伤害");
        //取消变身状态的
        if (player.getBuffedValue(MapleBuffStat.MORPH) != null && damage > 0) {
            player.cancelMorphs();
        }
        if (player.hasBattleShip()) {
            player.handleBattleShipHpLoss(damage);
            player.getMap().broadcastMessage(player, MaplePacketCreator.damagePlayer(damagefrom, monsteridfrom, player.getId(), damage, fake, direction, false, 0, true, oid, 0, 0), false);
            //player.checkBerserk();
        }
        if (damagefrom == -1) {
            Integer pguard = player.getBuffedValue(MapleBuffStat.POWERGUARD);
            if (pguard == null) {
                pguard = player.getBuffedValue(MapleBuffStat.完美机甲);
            }
            if (pguard != null) {
                if (attacker != null) {
                    //System.out.println("伤害反击发动");
                    int bouncedamage = (int) (damage * (pguard.doubleValue() / 100));
                    bouncedamage = Math.min(bouncedamage, attacker.getMaxHp() / 10);
                    player.getMap().damageMonster(player, attacker, bouncedamage);
                    if (pguard != player.getBuffedValue(MapleBuffStat.完美机甲)) {
                        damage -= bouncedamage;
                    }
                    //System.out.println("11111111111..");
                    player.getMap().broadcastMessage(player, MaplePacketCreator.damageMonster(oid, bouncedamage), false, true);
                    player.checkMonsterAggro(attacker);
                }
            }
            int[] achillesSkillId = {1120004, 1220005, 1320005, 21120004};//阿基里斯 防守策略
            for (int achilles : achillesSkillId) {
                ISkill achillesSkill = SkillFactory.getSkill(achilles);
                if (player.getSkillLevel(achillesSkill) > 0) {
                    //System.out.println("阿基里斯/防守策略发动");
                    double multiplier = 1 - achillesSkill.getEffect(player.getSkillLevel(achillesSkill)).getT() / 100.0;
                    int newdamage = (int) (multiplier * damage);
                    damage = newdamage;
                    break;
                }
            }
        }
        //魔法反击
        if (damagefrom == 0 && attacker != null) {
            Integer manaReflection = player.getBuffedValue(MapleBuffStat.MANA_REFLECTION);
            if (manaReflection != null) {
                int skillId = player.getBuffSource(MapleBuffStat.MANA_REFLECTION);
                ISkill manaReflectSkill = SkillFactory.getSkill(skillId);
                if (manaReflectSkill.getEffect(player.getSkillLevel(manaReflectSkill)).makeChanceResult()) {
                    int bouncedamage = (int) (damage * (manaReflection.doubleValue() / 100.0));
                    if (bouncedamage > attacker.getMaxHp() * .2) {
                        bouncedamage = (int) (attacker.getMaxHp() * .2);
                    }
                    player.getMap().damageMonster(player, attacker, bouncedamage);
                    player.getMap().broadcastMessage(player, MaplePacketCreator.damageMonster(oid, bouncedamage), false, true);
                    player.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(skillId, 5));
                    player.getMap().broadcastMessage(player, MaplePacketCreator.showBuffeffect(player.getId(), skillId, 5, (byte) 3, 1), false);
                }
            }
        }

        if (player.getBuffedValue(MapleBuffStat.MAGIC_GUARD) != null && mpattack == 0) {//魔法盾
            int mploss = (int) (damage * (player.getBuffedValue(MapleBuffStat.MAGIC_GUARD).doubleValue() / 100.0));
            int hploss = damage - mploss;
            if (mploss > player.getMp()) {
                hploss += mploss - player.getMp();
                mploss = player.getMp();
            }
            player.addMPHP(-hploss, -mploss);
        } else if (player.getBuffedValue(MapleBuffStat.MESOGUARD) != null) {
            damage = (damage % 2 == 0) ? damage / 2 : (damage / 2) + 1;
            int mesoloss = (int) (damage * (player.getBuffedValue(MapleBuffStat.MESOGUARD).doubleValue() / 100.0));
            if (player.getMeso() < mesoloss) {
                player.gainMeso(-player.getMeso(), false);
            } //金钱没有并不会取消金钱护盾buff 092实测
            else {
                player.gainMeso(-mesoloss, false);
            }
            player.addMPHP(-damage, -mpattack);
        } else {
            player.addMPHP(-damage, -mpattack);
        }

        if (damagefrom == -2) //9400711 - 透明怪物 显示怪物血条用
        {
            player.getMap().broadcastMessage(player, MaplePacketCreator.damagePlayer(-1, 9400711, player.getId(), damage, 0, 0, false, 0, false, 0, 0, 0), false);
        } else {
            player.getMap().broadcastMessage(player, MaplePacketCreator.damagePlayer(damagefrom, monsteridfrom, player.getId(), damage, fake, direction, false, 0, true, oid, 0, 0), false);
        }
        //player.checkBerserk();

        ISkill achillesSkill = SkillFactory.getSkill(1220013);
        int a = player.getSkillLevel(achillesSkill);
        if (a > 0) {
            MapleStatEffect effect = achillesSkill.getEffect(a);
            if (player.getBuffedValue(MapleBuffStat.祝福护甲_防御次数) == null) {
                if (achillesSkill.getEffect(a).makeChanceResult() && !player.skillisCooling(1220013)) {
                    //System.out.println("祝福护甲发动");
                    effect.applyTo(player);
                    c.getSession().write(MaplePacketCreator.skillCooldown(1220013, effect.getCooldown()));
                    ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(player, 1220013), effect.getCooldown() * 1000);
                    player.addCooldown(1220013, System.currentTimeMillis(), effect.getCooldown() * 1000, timer);
                    //System.out.println("祝福护甲冷却时间："+effect.getCooldown());
                }
            } else {
                if (player.getArmorTimes() < effect.getX()) { //护甲的防御次数是x节点
                    player.gainArmorTimes(1);
                    //System.out.println("护甲防御次数："+player.getArmorTimes());
                } else {
                    player.setArmorTimes(0);
                    player.cancelEffect(effect, false, -1); //大于5次时取消护甲效果
                    //System.out.println("取消护甲效果");
                }
            }
        }
        //武陵道场加能量值
        if (player.getMap().getId() >= 925020000 && player.getMap().getId() < 925030000) {
            player.setDojoEnergy(player.isGM() ? 300 : player.getDojoEnergy() < 300 ? player.getDojoEnergy() + 1 : 0);
            player.getClient().getSession().write(MaplePacketCreator.getEnergy(player.getDojoEnergy()));
        }
    }
}
