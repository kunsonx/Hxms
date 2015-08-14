/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.IEquip;
import net.sf.odinms.client.IItem;
import net.sf.odinms.client.ItemOptionFactory;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventory;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author 千石抚子
 */
public final class 放大镜 extends AbstractMaplePacketHandler {
	// 放大镜

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea,
			MapleClient c) {
		slea.readInt();
		byte slot = (byte) slea.readShort();
		byte dst = (byte) slea.readShort();
		MapleItemInformationProvider ii = MapleItemInformationProvider
				.getInstance();
		IEquip toUse = (IEquip) c.getPlayer()
				.getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
		if (dst >= 0) {
			toUse = (IEquip) c.getPlayer()
					.getInventory(MapleInventoryType.EQUIP).getItem(dst);
		}
		if (toUse == null) {
			c.getSession().write(MaplePacketCreator.enableActions());
			return;
		}
		MapleInventory useInventory = c.getPlayer().getInventory(
				MapleInventoryType.USE);
		IItem scroll = useInventory.getItem(slot);
		if (scroll.getQuantity() <= 0) {
			return;
		}
		useInventory.removeItem(scroll.getPosition(), (short) 1, false);
		// IEquip scrolled = (IEquip) ii.MagniflerEquipWithId(toUse);
		IEquip scrolled = ItemOptionFactory.MagniflerEquipWithId(toUse);
		c.getSession().write(
				MaplePacketCreator.scrolledItem(scroll, scrolled, false));
		c.getPlayer()
				.getMap()
				.broadcastMessage(
						MaplePacketCreator.showMagniflerEffect(c.getPlayer()
								.getId(), scrolled.getPosition()));
		if (dst < 0) {
			c.getPlayer().equipChanged();
		}
		c.getSession().write(MaplePacketCreator.enableActions());
	}
}
