/*
 取消Buff的处理程序
 */
package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.client.skills.战法;
import net.sf.odinms.client.skills.机械师;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.MaplePacketHandler;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class CancelBuffHandler extends AbstractMaplePacketHandler implements
		MaplePacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		int sourceid = changeSkillId(slea.readInt(), c.getPlayer());// 技能ID
		MapleStatEffect effect;
		ISkill skill = SkillFactory.getSkill(sourceid);
		if (sourceid == 3121004
				|| sourceid == 5221004
				|| sourceid == 1121001
				|| sourceid == 1221001
				|| sourceid == 1321001
				|| sourceid == 2121001
				|| sourceid == 2221001
				|| sourceid == 2321001
				|| sourceid == 2111002
				|| sourceid == 4211001
				|| sourceid == 3221001
				|| sourceid == 5101004
				|| sourceid == 15101003
				|| sourceid == 5201002
				|| sourceid == 14111006
				|| sourceid == 13111002
				|| sourceid == 22121000 // 冰点寒气
				|| sourceid == 22151001 // 火焰喷射
				|| sourceid == 4341002 // 终极斩
				|| sourceid == 4341003 // 怪物炸弹
				|| sourceid == 33101005 // 弩骑 吞噬
				|| sourceid == 33121009 // 弩骑 狂野射击
				|| sourceid == 35001001 // 火焰喷射器
				|| sourceid == 机械师.强化火焰喷射器 // 头上有能量条
				|| sourceid == 23121000 // 伊师塔之环
				|| sourceid == 31101000 // 灵魂吞噬
				|| sourceid == 31001000 || sourceid == 31101002
				|| sourceid == 31111005 || sourceid == 5311002
				|| sourceid == 5721001 || sourceid == 24121000
				|| sourceid == 24121005 || sourceid == 60011216
				|| sourceid == 65121003 || sourceid == 27101202
				|| sourceid == 36121000 || sourceid == 36101001) {
			c.getPlayer()
					.getMap()
					.broadcastMessage(
							c.getPlayer(),
							MaplePacketCreator.skillCancel(c.getPlayer(),
									sourceid), false);
		}
		/*
		 * if (skill != null && skill.hasCharge()) { //if (sourceid == 23121000)
		 * { c.getPlayer().getMap().broadcastMessage(c.getPlayer(),
		 * MaplePacketCreator.skillCancel(c.getPlayer(), sourceid), false); }
		 */
		effect = skill.getEffect(1);
		if (sourceid == 21000000) {
			if (c.getPlayer().getBuffedValue(MapleBuffStat.矛连击强化) != null) {
				c.getPlayer().cancelEffect(effect, false, -1);
			}
			// log.debug("取消矛连击强化/人物Combo清0");
			c.getPlayer().setCombo(0);
		} else {
			// log.debug("取消的技能id是："+sourceid);
			// log.debug("取消的技能名字是："+SkillFactory.getSkillName(sourceid));
		}
		c.getPlayer().cancelEffect(effect, false, -1);
	}

	public int changeSkillId(int skillid, MapleCharacter chr) {
		int a = skillid;
		/*
		 * if(skillid == 机械师.金属机甲_重机枪 && chr.getBuffedValue(MapleBuffStat.机械师)
		 * != null) a = 35121013; else
		 */if (skillid == 战法.黑暗灵气 && chr.getSkillLevel(战法.进阶黑暗灵气) > 0) {
			a = 战法.进阶黑暗灵气;
		} else if (skillid == 战法.蓝色灵气 && chr.getSkillLevel(战法.进阶蓝色灵气) > 0) {
			a = 战法.进阶蓝色灵气;
		} else if (skillid == 战法.黄色灵气 && chr.getSkillLevel(战法.进阶黄色灵气) > 0) {
			a = 战法.进阶黄色灵气;
		}
		return a;
	}
}