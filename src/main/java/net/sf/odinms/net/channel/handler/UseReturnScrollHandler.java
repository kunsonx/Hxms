/*
 使用回城卷程序
 */
package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Matze
 */
public class UseReturnScrollHandler extends AbstractMaplePacketHandler {

	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		if (!c.getPlayer().isAlive()) {
			c.getSession().write(MaplePacketCreator.enableActions());
			return;
		}
		slea.readInt();
		byte slot = (byte) slea.readShort();
		int itemId = slea.readInt();// 回城卷ID
		MapleItemInformationProvider ii = MapleItemInformationProvider
				.getInstance();
		IItem toUse = c.getPlayer().getInventory(MapleInventoryType.USE)
				.getItem(slot);

		if (toUse == null || toUse.getQuantity() < 1
				|| toUse.getItemId() != itemId) {
			c.getPlayer().dropMessage(1, "使用回城卷错误,我日!");
			return;
		}

		if (ii.getItemEffect(toUse.getItemId())
				.applyReturnScroll(c.getPlayer())) {
			MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE,
					slot, (short) 1, false);
		} else {
			c.getSession().write(MaplePacketCreator.enableActions());
		}
	}
}
