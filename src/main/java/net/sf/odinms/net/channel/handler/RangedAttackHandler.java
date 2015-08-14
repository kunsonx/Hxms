package net.sf.odinms.net.channel.handler;

import java.util.concurrent.ScheduledFuture;

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.Item;
import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleCharacter.CancelCooldownAction;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventory;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.MapleJob;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.client.MapleWeaponType;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.client.skills.弩骑;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class RangedAttackHandler extends DamageParseHandler {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(RangedAttackHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        ISkill theSkill;
        AttackInfo attack = parseRange(c.getPlayer(), slea);
        MapleCharacter player = c.getPlayer();
        int beforeMp = player.getMp();

        Short x = (short) attack.point.x;
        Short y = (short) attack.point.y;


        MapleInventory equip = player.getInventory(MapleInventoryType.EQUIPPED);
        IItem weapon = equip.getItem((byte) -11);
        MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
        MapleWeaponType type = mii.getWeaponType(weapon.getItemId()); //武器类型
        if (type == MapleWeaponType.NOT_A_WEAPON) { //如果是空手
            throw new RuntimeException("玩家 " + player.getName() + " 没有装备武器，不能进行攻击!");
        }
        MapleInventory use = player.getInventory(MapleInventoryType.USE);
        int projectile = 0;
        int bulletCount = 1;
        MapleStatEffect effect = null;
        if (attack.skill != 0) {
            theSkill = SkillFactory.getSkill(attack.skill);
            effect = attack.getAttackEffect(c.getPlayer(), theSkill);
            if ((attack.skill != 弩骑.吞噬_攻击 || attack.skill != 35101010 || attack.skill != 35101009) && effect != null) {
                bulletCount = effect.getBulletCount();
            }
            if (effect != null && effect.getCooldown() > 0) {
                c.getSession().write(MaplePacketCreator.skillCooldown(attack.skill, effect.getCooldown()));
            }
        }
        //使用影分身
        boolean useNoProjectile = player.useNoProjectile(attack.skill);

        useNoProjectile = type.equals(MapleWeaponType.CANNON)
                || type.equals(MapleWeaponType.能量剑)
                || useNoProjectile
                || type.equals(MapleWeaponType.双弩枪)
                || attack.slot == 0;

        boolean hasShadowPartner = player.getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null;
        int damageBulletCount = bulletCount;
        if (hasShadowPartner) {
            bulletCount *= 2;
        }

        if (attack.slot > 0) {
            Item it = (Item) use.getItem((short) attack.slot);
            projectile = it == null ? 0 : it.getItemId();
        }

        if (!useNoProjectile) {
            int bulletConsume = bulletCount;
            if (effect != null && effect.getBulletConsume() != 0) {
                bulletConsume = effect.getBulletConsume() * (hasShadowPartner ? 2 : 1);
            }
           // MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, projectile, bulletConsume, false, true);
        }
        if (projectile != 0 || useNoProjectile) {
            int visProjectile;
            if (attack.csstar > 0
                    && player.getInventory(MapleInventoryType.CASH).getItem((short) attack.csstar) != null) {
                visProjectile = player.getInventory(MapleInventoryType.CASH).getItem((short) attack.csstar).getItemId();
            } else {
                visProjectile = projectile;
            }
            MaplePacket packet;
            try {
                /*
                 * switch (attack.skill) { //这几个技能特殊 有包含direction case 3121004:
                 * case 3221001: case 5221004: case 13111002: packet =
                 * MaplePacketCreator.rangedAttack(player.getId(), attack.skill,
                 * attack.direction, attack.numAttackedAndDamage, visProjectile,
                 * attack.allDamage, attack.speed, x, y, attack.pos,
                 * player.getSkillLevel(attack.skill), attack.charge); break;
                 * default: packet =
                 * MaplePacketCreator.rangedAttack(player.getId(), attack.skill,
                 * attack.stance, attack.numAttackedAndDamage, visProjectile,
                 * attack.allDamage, attack.speed, x, y, attack.pos,
                 * player.getSkillLevel(attack.skill), attack.charge); break;
                 }
                 */
                packet = MaplePacketCreator.rangedAttack(attack, player, visProjectile);
                player.getMap().broadcastMessage(player, packet, false, true);
            } catch (Exception e) {
                log.warn("Failed to handle ranged attack..", e);
            }
            int basedamage;
            int projectileWatk = 0;
            if (projectile != 0) {
                projectileWatk = mii.getWatkForProjectile(projectile);
            }
            if (attack.skill != 4001344 && attack.skill != 14001004) { // not lucky 7
                if (projectileWatk != 0) {
                    basedamage = c.getPlayer().calculateMaxBaseDamage(c.getPlayer().getTotalWatk() + projectileWatk);
                } else {
                    basedamage = c.getPlayer().getCurrentMaxBaseDamage();
                }
            } else { // l7 has a different formula :>
                basedamage = (int) (((c.getPlayer().getTotalLuk() * 5.0) / 100.0) * (c.getPlayer().getTotalWatk() + projectileWatk));
            }
            if (attack.skill == 3101005) { // arrowbomb is hardcore like that O.o
                basedamage *= effect.getX() / 100.0;
            }
            int maxdamage = basedamage;
            double critdamagerate = 0.0;
            if (player.getJob().isA(MapleJob.ASSASSIN)) {
                ISkill criticalthrow = SkillFactory.getSkill(4100001);
                int critlevel = player.getSkillLevel(criticalthrow);
                if (critlevel > 0) {
                    critdamagerate = (criticalthrow.getEffect(player.getSkillLevel(criticalthrow)).getDamage() / 100.0);
                }
            } else if (player.getJob().isA(MapleJob.BOWMAN)) {
                ISkill criticalshot = SkillFactory.getSkill(3000001);
                int critlevel = player.getSkillLevel(criticalshot);
                if (critlevel > 0) {
                    critdamagerate = (criticalshot.getEffect(critlevel).getDamage() / 100.0) - 1.0;
                }
            }
            int critdamage = (int) (basedamage * critdamagerate);
            if (effect != null) {
                maxdamage *= attack.skill == 14101006 ? effect.getDamage() : effect.getDamage() / 100.0;
            }
            maxdamage += critdamage;
            maxdamage *= damageBulletCount;
            if (hasShadowPartner) {
                ISkill shadowPartner = SkillFactory.getSkill(4111002);
                int shadowPartnerLevel = player.getSkillLevel(shadowPartner);
                if (0 >= shadowPartnerLevel) {
                    shadowPartner = SkillFactory.getSkill(14111000);
                    shadowPartnerLevel = player.getSkillLevel(shadowPartner);
                }
                MapleStatEffect shadowPartnerEffect = shadowPartner.getEffect(shadowPartnerLevel);
                if (attack.skill != 0) {
                    maxdamage *= (1.0 + shadowPartnerEffect.getY() / 100.0);
                } else {
                    maxdamage *= (1.0 + shadowPartnerEffect.getX() / 100.0);
                }
            }
            if (attack.skill == 4111004) {
                maxdamage = 35000;
            }
            if (effect != null) {
                long money = effect.getMoneyCon();
                if (money != 0) {
                    double moneyMod = money * 0.5;
                    money = (int) (money + Math.random() * moneyMod);
                    if (money > player.getMeso()) {
                        money = player.getMeso();
                    }
                    player.gainMeso(-money, false);
                }
            }
            if (attack.skill != 0) {
                ISkill skill = SkillFactory.getSkill(attack.skill);
                int skillLevel = c.getPlayer().getSkillLevel(skill);
                MapleStatEffect effect_ = skill.getEffect(skillLevel);
                if (effect_.getCooldown() > 0) {
                    if (player.skillisCooling(attack.skill)) {
                        player.getCheatTracker().registerOffense(CheatingOffense.COOLDOWN_HACK);
                        return;
                    } else {
                        c.getSession().write(MaplePacketCreator.skillCooldown(attack.skill, effect_.getCooldown()));
                        ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(c.getPlayer(), attack.skill), effect_.getCooldown() * 1000);
                        player.addCooldown(attack.skill, System.currentTimeMillis(), effect_.getCooldown() * 1000, timer);
                    }
                }
            }
            //夜行者 驱逐
            if (player.getSkillLevel(SkillFactory.getSkill(14100005)) > 0 && player.getBuffedValue(MapleBuffStat.DARKSIGHT) != null && attack.numAttacked > 0) {
                player.cancelEffectFromBuffStat(MapleBuffStat.DARKSIGHT);
                player.cancelBuffStats(MapleBuffStat.DARKSIGHT);
            }

            applyAttack(attack, player, maxdamage, bulletCount);
            if (effect != null && effect.getMpCon() != 0) {
                if (player.getMp() - beforeMp < effect.getMpCon()) {
                    int remainingMp = beforeMp - effect.getMpCon();
                    c.getPlayer().setMp(remainingMp);
                    c.getPlayer().updateSingleStat(MapleStat.MP, remainingMp);
                }
            }
        }
    }
}
