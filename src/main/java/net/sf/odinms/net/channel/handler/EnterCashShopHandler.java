/*
 * 进入商场处理程序
 */
package net.sf.odinms.net.channel.handler;

import java.rmi.RemoteException;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.server.ServerExceptionHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Acrylic (Terry Han)
 */
public class EnterCashShopHandler extends AbstractMaplePacketHandler {

	org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(getClass());

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		if (c.getChannelServer().allowCashshop()) {
			/*
			 * if (c.getPlayer().getBuffedValue(MapleBuffStat.SUMMON) != null) {
			 * c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.SUMMON); if
			 * (c.getPlayer().getSkillLevel(1320009) > 0) { for (int i = 0; i <
			 * 5; i++) {
			 * c.getSession().write(MaplePacketCreator.cancelBuff(null, 2022125
			 * + i)); } } }
			 */
			try {
				WorldChannelInterface wci = c.getChannelServer()
						.getWorldInterface();
				wci.addBuffsToStorage(c.getPlayer().getId(), c.getPlayer()
						.getAllBuffs());
			} catch (RemoteException e) {
				ServerExceptionHandler.HandlerRemoteException(e);
				c.getChannelServer().reconnectWorld();
			}
			c.getPlayer().getMap().removePlayer(c.getPlayer());
			// c.getPlayer().saveToDB(true);
			c.getPlayer().setInCS(true);
			c.getSession().write(MaplePacketCreator.getcashinfo(c, false));
			c.getSession().write(MaplePacketCreator.getshopitemsinfo(c, false));
			c.getSession().write(
					MaplePacketCreator.showNXMapleTokens(c.getPlayer()));
			c.getSession().write(
					MaplePacketCreator.getCSInventory(c.getPlayer()));
			c.getSession().write(MaplePacketCreator.getCSGifts(c.getPlayer()));
			c.getSession().write(
					MaplePacketCreator.sendWishList(c.getPlayer().getId()));
		} else {
			c.getSession().write(MaplePacketCreator.sendBlockedMessage(3));
			c.getSession().write(MaplePacketCreator.enableActions());
		}

	}
}