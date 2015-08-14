/*
   怪物炸弹 闹钟召唤的小怪 黑水雷
 * 双刀的怪物炸弹技能可以使所有怪都变成炸雷类
 */

package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class MonsterBombSkillHandler extends AbstractMaplePacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		/*
		 * 8A 00 06 02 00 00 82 FF FF FF 22 00 00 00 78 00 00 00 0B 3D 42 00 02
		 * 00 00 00
		 */
		slea.readInt();
		int x = slea.readInt();
		int y = slea.readInt();
		int skillid = slea.readInt();
		int skilllevel = slea.readInt();
		// c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.怪物炸弹效果(c.getPlayer(),
		// skillLevel));
		c.getSession().write(
				MaplePacketCreator.怪物炸弹效果(c.getPlayer(), x, y, skillid,
						skilllevel));
	}
}