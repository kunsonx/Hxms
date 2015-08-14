//技能效果
package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.apache.log4j.Logger;

public class SkillEffectHandler extends AbstractMaplePacketHandler {

	private Logger log = Logger.getLogger(getClass());

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea,
			final MapleClient c) {
		int skillId = slea.readInt();// 技能ID
		int level = slea.readByte();// 等级
		byte flags = slea.readByte();// 80
		int speed = slea.readByte();// 武器攻击速度 && skillId != 23121000
		int op = slea.readByte();
		ISkill skill = SkillFactory.getSkill(skillId);
		/*
		 * if (skill != null && skill.hasCharge()) {
		 * c.getPlayer().getMap().broadcastMessage(c.getPlayer(),
		 * MaplePacketCreator.skillEffect(c.getPlayer(), skillId, level, flags,
		 * speed, op), false); }
		 */

		if (skillId == 33101005) {
			c.getPlayer().设置吞噬的怪id(slea.readInt());
		}
		// 暴风箭雨 金属风暴 磁石
		if ((skillId == 3121004
				|| skillId == 5221004
				|| skillId == 1121001
				|| skillId == 1221001
				|| skillId == 1321001
				|| skillId == 2121001
				|| skillId == 2221001
				|| skillId == 2321001
				|| skillId == 2111002
				|| skillId == 4211001
				|| skillId == 3221001
				|| skillId == 5101004
				|| skillId == 15101003
				|| skillId == 5201002
				|| skillId == 14111006
				|| skillId == 13111002
				|| skillId == 22121000 // 冰点寒气
				|| skillId == 22151001 // 火焰喷射
				|| skillId == 4341002 // 终极斩
				|| skillId == 4341003 // 怪物炸弹
				|| skillId == 33101005 // 弩骑 吞噬
				|| skillId == 33121009 // 弩骑 狂野射击
				|| skillId == 35001001 // 火焰喷射器
				|| skillId == 35101009// 强化火焰喷射器
				|| skillId == 23121000 // 伊师塔之环
				|| skillId == 31101000 // 灵魂吞噬
				|| skillId == 31001000 || skillId == 31101002
				|| skillId == 31111005 || skillId == 5311002
				|| skillId == 5721001 || skillId == 24121000
				|| skillId == 24121005 || skillId == 60011216
				|| skillId == 65121003 || skillId == 27101202
				|| skillId == 36121000 || skillId == 36101001)
				&& level >= 1) {
			c.getPlayer()
					.getMap()
					.broadcastMessage(
							c.getPlayer(),
							MaplePacketCreator.skillEffect(c.getPlayer(),
									skillId, level, flags, speed, op), false);
		} else {
			log.info("未登记技能效果挂断连接：" + skillId);
		}
	}
}