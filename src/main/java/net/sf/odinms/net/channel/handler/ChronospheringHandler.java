/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.apache.log4j.Logger;

/**
 *
 * @author HXMS
 */
public class ChronospheringHandler extends AbstractMaplePacketHandler {

	private static Logger log = Logger.getLogger(ChronospheringHandler.class);

	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		slea.readInt();
		int mapid = slea.readInt();
		log.debug("超时空传送功能：" + mapid);
		if (c.getPlayer().haveItem(5040005, 1, false, false)) {
			MapleInventoryManipulator
					.removeById(c, MapleItemInformationProvider.getInstance()
							.getInventoryType(5040005), 5040005, 1, true, false);
			c.getSession().write(
					MaplePacketCreator.getShowItemGain(5040005, (short) -1,
							true));
			c.getPlayer().changeMap(
					c.getChannelServer().getMapFactory().getMap(mapid));
			c.getPlayer().dropMessage("使用一张超时空卷。");
		} else {
			c.getPlayer().dropMessage(1, "您没有超时空卷，请购买。");
			c.getSession().write(MaplePacketCreator.enableActions());
		}
	}
}
