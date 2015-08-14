/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.handler;

import java.util.ArrayList;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.apache.log4j.Logger;

/**
 * 游戏通用汇总处理
 *
 * @author HXMS
 */
public class MapleGameHandler {

	private static Logger log = Logger.getLogger(MapleGameHandler.class);

	/**
	 * 处理狂龙设置快捷键
	 *
	 * @author HXMS
	 */
	public static class HandlerSetCmd extends AbstractMaplePacketHandler {

		@Override
		public void handlePacket(SeekableLittleEndianAccessor slea,
				MapleClient c) {
			/**
			 * Packet 107. 3C 01 00 01 4C A2 A4 03
			 */
			slea.skip(1);
			int pos = slea.readByte();
			int skill = slea.readInt();
			c.getPlayer().getAttribute().getAttribute()
					.put("p_cmd" + pos, String.valueOf(skill));
			c.getSession().write(MaplePacketCreator.getChangeCmd(pos, skill));
		}
	}

	/**
	 * 狂龙技能效果
	 *
	 *
	 * @author hxms
	 */
	public static class AttackEffectHandler extends AbstractMaplePacketHandler {

		@Override
		public void handlePacket(SeekableLittleEndianAccessor slea,
				MapleClient c) {
			MapleCharacter player = c.getPlayer();
			int skillId = 61120007;
			if (player.getBuffManager().hasBuff(skillId)) {
				ArrayList<Integer> monsters = new ArrayList<Integer>();
				int count = slea.readInt();
				for (int i = 0; i < count; i++) {
					int oid = slea.readInt();
					if (player.getMap().getCore().contains(oid)) {
						monsters.add(oid);
					}
				}
				if (!monsters.isEmpty()) {
					player.getMap().broadcastMessage(
							MaplePacketCreator.getAttackEffect(c.getPlayer()
									.getId(), skillId, monsters, 2));
				}
				player.getBuffManager().cancelSkill(skillId);
			}
		}
	}

	/**
	 * 尖兵电力开关
	 */
	public static class PowerSwitchHandler extends AbstractMaplePacketHandler {

		@Override
		public void handlePacket(SeekableLittleEndianAccessor slea,
				MapleClient c) {
			if (c.getPlayer().getPower() >= 10) {
				c.getPlayer().setPowerOpen();
			} else {
				c.getPlayer().dropMessage("电力不足无法打开电源！");
			}
		}
	}
}
