/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.handler;

import java.sql.Timestamp;
import net.sf.odinms.client.GameConstants;
import net.sf.odinms.client.MapleCSInventoryItem;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.server.CashItemFactory;
import net.sf.odinms.server.CashItemInfo;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Administrator
 */
public class CashShopGiftHandler extends AbstractMaplePacketHandler {

	private static org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(CashShopGiftHandler.class);

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		// 商城赠送管理集。
		int snCS = slea.readInt();
		int type = slea.readByte();
		String recipient = slea.readMapleAsciiString();
		String message = slea.readMapleAsciiString();
		CashItemInfo item = CashItemFactory.getItemInSql(snCS);
		if (c.getPlayer().getCSPoints(type) >= item.getPrice()) {
			if ((item.getItemId() >= 5070000 && item.getItemId() <= 5077000)
					|| (item.getItemId() >= 5390000 && item.getItemId() <= 5390006)
					|| (item.getItemId() >= 5200000 && item.getItemId() <= 5200000)) {
				c.getPlayer().dropMessage("此物品已经被管理员封了!!");
				c.getSession().write(MaplePacketCreator.enableActions());
				return;
			}
			if (MapleCharacter.getAccountIdByName(recipient) != -1) {
				if (MapleCharacter.getAccountIdByName(recipient) == c
						.getPlayer().getAccountid()) {
					c.getSession().write(MaplePacketCreator.showCannotToMe());
				} else {
					c.getPlayer().modifyCSPoints(type, -item.getPrice());
					MapleCSInventoryItem gift = new MapleCSInventoryItem(0,
							item.getItemId(), snCS, (short) item.getCount(),
							true);
					gift.setSender(c.getPlayer().getName());
					gift.setMessage(message);
					Timestamp ExpirationDate = new Timestamp(
							System.currentTimeMillis());
					if (GameConstants.isPet(gift.getItemId())
							|| item.getItemId() == 1112906
							|| item.getItemId() == 1112905) {
						ExpirationDate = new Timestamp(
								((System.currentTimeMillis() / 1000) + (90 * 24 * 60 * 60)) * 1000);
					} else if (item.getItemId() == 5211047
							|| item.getItemId() == 5360014) {
						ExpirationDate = new Timestamp(
								((System.currentTimeMillis() / 1000) + (3 * 60 * 60)) * 1000);
					} else if (item.getPeriod() != 0) {
						ExpirationDate = new Timestamp(
								((System.currentTimeMillis() / 1000) + (item
										.getPeriod() * 24 * 60 * 60)) * 1000);
					} else {
						ExpirationDate = null;
					}
					gift.setExpire(ExpirationDate);
					CashShopHandler.GiftItem(gift, recipient, c.getPlayer()
							.getName());
					c.getSession().write(
							MaplePacketCreator.getGiftFinish(c.getPlayer()
									.getName(), item.getItemId(), (short) item
									.getCount()));
				}
			} else {
				c.getSession().write(MaplePacketCreator.showCheckName());
			}
		} else {
			c.getSession().write(MaplePacketCreator.enableActions());
			AutobanManager.getInstance().autoban(c, "试图购买现金物品，但是没有足够的点券。");
			return;
		}
	}
}
