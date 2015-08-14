package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventory;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Matze
 */
public class ItemMoveHandler extends AbstractMaplePacketHandler {

	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		// log.debug("装备移动包"+slea.toString());
		int actionId = slea.readInt();
		if (actionId <= c.getLastActionId()) {
			c.getSession().write(MaplePacketCreator.enableActions());
			return;
		}
		c.setLastActionId(actionId);
		MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
		short src = slea.readShort();// 背包位置
		short dst = slea.readShort();// 装备栏位置
		short quantity = slea.readShort();
		if (src < 0 && dst > 0) {
			// log.debug("脱装备  dst："+dst+"     src："+src+"");
			MapleInventoryManipulator.unequip(c, src, dst);// 脱掉装备
		} else if (dst < 0) {
			// log.debug("穿装备  dst："+dst+"     src："+src+"");
			MapleInventoryManipulator.equip(c, src, dst);// 穿上
		} else if (dst == 0) {
			// log.debug("丢装备  dst："+dst+"     src："+src+"");
			MapleInventoryManipulator.drop(c, type, src, quantity);
		} else {
			if (c.getPlayer().getGm() > 0) {
				int itemided = c.getPlayer().getInventory(type).getItem(src)
						.getItemId();
				c.getPlayer().dropMessage("item:" + itemided);
			}
			// log.debug("移装备  dst："+dst+"     src："+src+"");
			MapleInventoryManipulator.move(c, type, src, dst);
		}
	}
}