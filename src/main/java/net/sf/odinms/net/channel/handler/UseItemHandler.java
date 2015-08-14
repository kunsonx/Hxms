/*
   使用道具　　处理
 */

/*
 * UseItemHandler.java
 *
 * Created on 27. November 2007, 16:51
 */
package net.sf.odinms.net.channel.handler;

import java.util.List;
import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.world.MapleParty;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class UseItemHandler extends AbstractMaplePacketHandler {

	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		if (!c.getPlayer().isAlive()) {
			c.getSession().write(MaplePacketCreator.enableActions());
			return;
		}
		MapleCharacter chr = c.getPlayer();
		MaplePartyCharacter partyplayer = new MaplePartyCharacter(chr);
		MapleItemInformationProvider ii = MapleItemInformationProvider
				.getInstance();
		slea.readInt(); // 4位
		byte slot = (byte) slea.readShort();
		int itemId = slea.readInt();// 物品ID
		IItem toUse = c.getPlayer().getInventory(MapleInventoryType.USE)
				.getItem(slot);
		if (toUse != null && toUse.getQuantity() > 0
				&& toUse.getItemId() == itemId) {// 如果使用的道具不为空并且..
			if (itemId == 2022178 || itemId == 2022433 || itemId == 2050004) { // 万能疗伤药
																				// 武陵道场万能疗伤药
				c.getPlayer().dispelDebuffs();
				remove(c, slot);
				return;
			} else if (itemId == 2050003) { // 圣水
				c.getPlayer().dispelDebuffsi();
				remove(c, slot);
				return;
			}
			remove(c, slot);
			ii.getItemEffect(toUse.getItemId()).applyTo(c.getPlayer());
		}
	}

	private void remove(MapleClient c, byte slot) {
		MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE,
				slot, (short) 1, false);
		c.getSession().write(MaplePacketCreator.enableActions());
	}
}