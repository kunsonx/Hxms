/*
 近距离攻击也就是普通攻击
 */
package net.sf.odinms.net.channel.handler;

import java.util.concurrent.ScheduledFuture;
import net.sf.odinms.client.MapleCharacter.CancelCooldownAction;
import net.sf.odinms.client.*;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.apache.log4j.Logger;

public class CloseRangeDamageHandler extends DamageParseHandler {

	private Logger log = Logger.getLogger(getClass());

	private boolean isFinisher(int skillId) {
		return (skillId >= 1111003 && skillId <= 1111006)
				|| (skillId >= 11111002 && skillId <= 11111003);
		// 恐慌 灵魂突刺
	}

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) { // 参数slea就是接到的数据
		// log.debug("普通攻击封包："+slea.toString());
		ISkill theSkill = null;
		AttackInfo attack = parseClose(c.getPlayer(), slea);// 将参数传给parseDamage，貌似是攻击解析
		MapleCharacter player = c.getPlayer();
		// log.debug(attack.pos + ":");
		// MaplePacket packet =
		// MaplePacketCreator.closeRangeAttack(player.getId(), attack.skill,
		// attack.stance, attack.numAttackedAndDamage, attack.allDamage,
		// attack.speed, attack.pos, player.getSkillLevel(attack.skill));
		player.getMap().broadcastMessage(player,
				MaplePacketCreator.closeRangeAttack(attack, player), false,
				true);

		if (player.getBuffManager().hasBuff(4221013) && attack.skill == 4221007
				&& attack.allDamage.size() > 0) {
			player.handle_JiShaDian_Gain();
		}

		// handle combo orb consume
		int numFinisherOrbs = 0;
		Integer comboBuff = player.getBuffedValue(MapleBuffStat.COMBO);
		if (isFinisher(attack.skill)) {
			if (comboBuff != null) {
				numFinisherOrbs = comboBuff.intValue() - 1;
			}
			player.handleOrbconsume();
		} else if (attack.numAttacked > 0) {
			// handle combo orbgain
			if (attack.skill != 1111008
					&& comboBuff != null
					&& (player.getSkillLevel(1111002) > 0 || player
							.getSkillLevel(11111001) > 0)) { // shout should not
																// give orbs虎咆哮
				player.handleOrbgain();
			}
		}

		// 处理减少的hp的
		if (attack.numAttacked > 0 && attack.skill == 1311005) { // 比如龙之献祭技能
			theSkill = SkillFactory.getSkill(attack.skill);
			int totDamageToOneMonster = attack.allDamage.get(0).getRight()
					.get(0).intValue(); // sacrifice attacks only 1 mob with 1
										// attack
			player.setHp(player.getHp() - totDamageToOneMonster
					* attack.getAttackEffect(player, theSkill).getX() / 100);
			player.updateSingleStat(MapleStat.HP, player.getHp());
		}

		// 处理带属性的
		if (attack.numAttacked > 0 && attack.skill == 1211002) {// 比如属性攻击
			// boolean advcharge_prob = false;
			int advcharge_level = player.getSkillLevel(SkillFactory
					.getSkill(1220010));
			if (advcharge_level > 0) {
				MapleStatEffect advcharge_effect = SkillFactory.getSkill(
						1220010).getEffect(advcharge_level);
				// advcharge_prob = advcharge_effect.makeChanceResult();
			} /*
			 * else { advcharge_prob = false; }
			 */
			/*
			 * if (!advcharge_prob) {
			 * player.cancelEffectFromBuffStat(MapleBuffStat.WK_CHARGE); }
			 */
		}

		int maxdamage = c.getPlayer().getCurrentMaxBaseDamage();
		int attackCount = 1;
		if (attack.skill != 0) {
			// System.out.println(attack.skill);
			theSkill = SkillFactory.getSkill(attack.skill);
			MapleStatEffect effect = attack.getAttackEffect(c.getPlayer(),
					theSkill);
			if (theSkill != null && effect != null) {
				attackCount = effect.getAttackCount();
				maxdamage *= effect.getDamage() / 100.0;
				maxdamage *= attackCount;
				if (effect.getCooldown() > 0) {
					c.getSession().write(
							MaplePacketCreator.skillCooldown(attack.skill,
									effect.getCooldown()));
					ScheduledFuture<?> timer = TimerManager.getInstance()
							.schedule(
									new CancelCooldownAction(c.getPlayer(),
											attack.skill),
									effect.getCooldown() * 1000);
					c.getPlayer().addCooldown(attack.skill,
							System.currentTimeMillis(),
							effect.getCooldown() * 1000, timer);
				}
			}
		}
		maxdamage = Math.min(maxdamage, 199999);// 近距离攻击最大值19W
		if (attack.skill == 4211006) {// 金钱炸弹
			maxdamage = 700000;// 70W伤害
		} else if (numFinisherOrbs > 0) {
			maxdamage *= numFinisherOrbs;
		} else if (comboBuff != null) {
			ISkill combo = SkillFactory.getSkill(1111002);// 斗气集中
			int comboLevel = player.getSkillLevel(combo);
			if (comboLevel == 0) {
				combo = SkillFactory.getSkill(11111001);
				comboLevel = player.getSkillLevel(combo);
			}
			MapleStatEffect comboEffect = combo.getEffect(comboLevel);
			double comboMod = 1.0 + (comboEffect.getDamage() / 100.0 - 1.0)
					* (comboBuff.intValue() - 1);
			maxdamage *= comboMod;
		}

		if (numFinisherOrbs == 0 && isFinisher(attack.skill)) {
			return; // can only happen when lagging o.o
		}
		if (isFinisher(attack.skill)) {
			maxdamage = 199999; // reenable damage calculation for finishers
		}
		/*
		 * if (attack.skill > 0) { }
		 */
		// 战神之舞 全力挥击 旋风
		if (attack.skill == 21120002 || attack.skill == 21110002
				|| attack.skill == 21110006) {
			ISkill skill = SkillFactory.getSkill(attack.skill);
			int skillLevel = c.getPlayer().getSkillLevel(skill);
			MapleStatEffect effect_ = skill.getEffect(skillLevel);
			if (effect_.getCooldown() > 0) {
				c.getSession().write(
						MaplePacketCreator.skillCooldown(attack.skill,
								effect_.getCooldown()));
				ScheduledFuture<?> timer = TimerManager.getInstance().schedule(
						new CancelCooldownAction(c.getPlayer(), attack.skill),
						effect_.getCooldown() * 1000);
				c.getPlayer().addCooldown(attack.skill,
						System.currentTimeMillis(),
						effect_.getCooldown() * 1000, timer);// 冷却时间
			}
		}
		applyAttack(attack, player, maxdamage, attackCount);
	}
}
