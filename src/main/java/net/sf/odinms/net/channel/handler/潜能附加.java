/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.IEquip.ScrollResult;
import net.sf.odinms.client.*;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author 千石抚子
 */
public final class 潜能附加 extends AbstractMaplePacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea,
			MapleClient c) {
		slea.readInt();
		byte slot = (byte) slea.readShort();
		byte dst = (byte) slea.readShort();
		boolean legendarySpirit = false;
		// boolean legendarySpirit = slea.readByte() == 1;
		MapleItemInformationProvider ii = MapleItemInformationProvider
				.getInstance();
		IEquip toScroll;
		if (dst < 0) {
			toScroll = (IEquip) c.getPlayer()
					.getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
		} else {
			legendarySpirit = true;
			toScroll = (IEquip) c.getPlayer()
					.getInventory(MapleInventoryType.EQUIP).getItem(dst);
		}
		if (toScroll == null) {
			return;
		}
		MapleInventory useInventory = c.getPlayer().getInventory(
				MapleInventoryType.USE);
		IItem scroll = useInventory.getItem(slot);
		if (scroll.getQuantity() <= 0) {
			return;
		}
		// useInventory.removeItem(scroll.getPosition(), (short) 1, false);
		IEquip scrolled = (IEquip) ii.potentialScrollEquipWithId(toScroll,
				scroll.getItemId());
		ScrollResult scrollSuccess = IEquip.ScrollResult.FAIL; // fail
		if (scrolled == null) {
			scrollSuccess = IEquip.ScrollResult.CURSE;
		} else if (ii.附加卷轴(scroll.getItemId()) && scrolled.getIdentify() > 0) {
			scrollSuccess = IEquip.ScrollResult.SUCCESS;
		}

		if (scrollSuccess == IEquip.ScrollResult.CURSE) {
			c.getSession().write(
					MaplePacketCreator.scrolledItem(scroll, toScroll, true));
			c.getPlayer().getInventory(MapleInventoryType.EQUIPPED)
					.removeItem(toScroll.getPosition());
			if (dst < 0) {
				c.getPlayer().getInventory(MapleInventoryType.EQUIPPED)
						.removeItem(toScroll.getPosition());
			} else {
				c.getPlayer().getInventory(MapleInventoryType.EQUIP)
						.removeItem(toScroll.getPosition());
			}
		} else {
			legendarySpirit = false;
			c.getSession().write(
					MaplePacketCreator.scrolledItem(scroll, scrolled, false));
		}

		c.getPlayer()
				.getMap()
				.broadcastMessage(
						MaplePacketCreator.getScrollEffect(c.getPlayer()
								.getId(), scrollSuccess, legendarySpirit,
								scroll.getItemId(), toScroll.getItemId()));
		if (dst < 0 && (scrollSuccess == IEquip.ScrollResult.SUCCESS)) {
			c.getPlayer().equipChanged();
		}
		MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE,
				slot, (short) 1, false);// 扣潜能附加卷
		// useInventory.removeItem(scroll.getPosition(), (short) 1, false);
		c.getSession().write(MaplePacketCreator.enableActions());
	}
}
