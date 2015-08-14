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
public final class 装备强化 extends AbstractMaplePacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea,
			MapleClient c) {
		slea.readInt();
		byte slot = (byte) slea.readShort();
		byte dst = (byte) slea.readShort();
		boolean legendarySpirit = false; // legendary spirit skill
		boolean checkIfGM = c.getPlayer().isGM();
		// boolean legendarySpirit = slea.readByte() == 1;
		IEquip toScroll;
		if (dst < 0) {
			toScroll = (IEquip) c.getPlayer()
					.getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
		} else {
			legendarySpirit = true;
			toScroll = (IEquip) c.getPlayer()
					.getInventory(MapleInventoryType.EQUIP).getItem(dst);
		}
		MapleItemInformationProvider ii = MapleItemInformationProvider
				.getInstance();
		if (toScroll.getStarlevel() >= 54 || toScroll.getUpgradeSlots() > 0
				|| toScroll == null) {
			c.getSession().write(MaplePacketCreator.enableActions());
			return;
		}
		MapleInventory useInventory = c.getPlayer().getInventory(
				MapleInventoryType.USE);
		IItem scroll = useInventory.getItem(slot);
		if (scroll.getQuantity() <= 0) {
			c.getSession().write(MaplePacketCreator.enableActions());
			return;
		}
		// useInventory.removeItem(scroll.getPosition(), (short) 1, false);
		int star = toScroll.getStarlevel() + 1;
		IEquip scrolled = (IEquip) ii.starScrollEquipWithId_2(toScroll,
				scroll.getItemId(), checkIfGM);
		ScrollResult scrollSuccess = IEquip.ScrollResult.FAIL; // fail
		if (scrolled == null) {
			scrollSuccess = IEquip.ScrollResult.CURSE;
		} else if (ii.强化卷轴(scroll.getItemId())
				&& scrolled.getStarlevel() == star) {
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
				slot, (short) 1, false);// 扣装备强化卷
		// useInventory.removeItem(scroll.getPosition(), (short) 1, false);
		c.getSession().write(MaplePacketCreator.enableActions());
	}
}