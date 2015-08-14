/*
 分配SP处理程序
 */
package net.sf.odinms.net.channel.handler;

import java.util.ArrayList;
import java.util.List;
import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleJob;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.client.skills.弩骑;
import net.sf.odinms.client.skills.战法;
import net.sf.odinms.client.skills.战神;
import net.sf.odinms.client.skills.机械师;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class DistributeSPHandler extends AbstractMaplePacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea,
			MapleClient c) {
		slea.readInt();
		int skillid = slea.readInt();
		int level = slea.readByte();
		MapleCharacter player = c.getPlayer();
		int remainingSp = 0;
		boolean isBeginnerSkill = false;
		if ( // 冒险家
		skillid > 0
				&& skillid <= 100
				|| skillid >= 1000
				&& skillid <= 1200
				// 骑士团
				|| skillid >= 10000000
				&& skillid <= 10000100
				|| skillid >= 10001000
				&& skillid <= 10001200
				// Aran
				|| skillid >= 20000000 && skillid <= 20000100
				|| skillid >= 20001000
				&& skillid <= 20001200
				// Evan
				|| skillid >= 20010000 && skillid <= 20010100
				|| skillid >= 20011000 && skillid <= 20011200
				//
				|| skillid >= 30000000 && skillid <= 30000100
				|| skillid >= 30001000 && skillid <= 30001200) { // 新手技能
			// log.debug("新手技能");
			isBeginnerSkill = true;
		}
		int type = 0;
		/*
		 * if (player.isEvan()) { type = player.getDragonSkill(skillid);
		 * remainingSp = player.getDragonSp(type); //log.debug("龙神技能"); } else
		 * if (player.isResistance()) { type = player.getBigBang(skillid);
		 * remainingSp = player.getBigBangSP(type); //log.debug("反抗者技能"); } else
		 * { remainingSp = player.getRemainingSp(); //log.debug("冒险家/骑士团技能"); }
		 */
		remainingSp = player.getRemainingSp(MapleJob.GetSkillAtSlot(skillid));
		ISkill skill = SkillFactory.getSkill(skillid);
		int curLevel = player.getSkillLevel(skill);
		if ((level <= remainingSp && curLevel + 1 <= (skill.hasMastery() ? player
				.getMasterLevel(skill) : skill.getMaxLevel()))
				|| isBeginnerSkill) {
			if (!isBeginnerSkill) {
				/*
				 * if (player.isEvan()) { player.givedragonSP(type, -1); } else
				 * if (player.isResistance()) { player.gainBigBangSP(type, -1);
				 * } else {
				 */
				player.setRemainingSp(remainingSp - level,
						MapleJob.GetSkillAtSlot(skillid));
				player.updateSingleStat(MapleStat.AVAILABLESP,
						player.getRemainingSp());
				// }
			}
			// player.changeSkillLevel(skill, curLevel + 1,
			// player.getMasterLevel(skill));
			teachLinkSkills(player, skill, curLevel + level,
					player.getMasterLevel(skill));
		}
		c.getSession().write(MaplePacketCreator.enableActions());

	}

	private void teachLinkSkills(MapleCharacter chr, ISkill skill,
			int skillLevel, int skillMasterLevel) {
		chr.changeSkillLevel(skill, skillLevel, skillMasterLevel); // 先加原来的技能
		int skillId = skill.getId();
		List<Integer> skillList = new ArrayList<Integer>();
		// 关联技能处理
		switch (skillId) {
		case 战神.全力挥击:
			skillList.add(战神.全力挥击_双重重击);
			skillList.add(战神.全力挥击_三重重击);
			chr.setRemainingSp(chr.getRemainingSp() + 2);
			break;
		case 战神.战神之舞:
			skillList.add(战神.战神之舞_双重重击);
			skillList.add(战神.战神之舞_三重重击);
			chr.setRemainingSp(chr.getRemainingSp() + 2);
			break;

		case 战法.霸体:
			skillList.add(战法.霸体2);
			skillList.add(战法.霸体3);
			skillList.add(战法.霸体4);
			break;
		// case 战法.惩戒:
		// skillList.add(战法.惩戒1);
		// skillList.add(战法.惩戒2);
		// skillList.add(战法.惩戒3);
		// skillList.add(战法.惩戒4);
		// chr.gainBigBangSP(2, 4);
		// break;

		case 弩骑.地雷:
			skillList.add(弩骑.地雷_自爆);
			break;
		case 弩骑.吞噬:
			skillList.add(弩骑.吞噬_攻击);
			skillList.add(弩骑.吞噬_消化);
			break;

		case 机械师.人造卫星:
			skillList.add(机械师.人造卫星2);
			skillList.add(机械师.人造卫星3);
			break;
		case 机械师.精准重武器:
			skillList.add(机械师.强化火焰喷射器);
			skillList.add(机械师.强化机枪扫射);
			break;
		case 机械师.金属机甲_导弹战车:
			skillList.add(机械师.金属机甲_重机枪_4转);
			break;
		case 机械师.机器人工厂_RM1:
			skillList.add(机械师.机器人工厂_机器人);
			break;
		}

		if (!skillList.isEmpty()) {
			for (int skillid : skillList) {
				chr.changeSkillLevel(SkillFactory.getSkill(skillid),
						skillLevel, 0); // 学习隐藏的关联技能
			}
		}

	}
}