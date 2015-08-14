/*
自动分配AP处理程序
 */
package net.sf.odinms.net.channel.handler;

import java.util.ArrayList;
import java.util.List;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class DistributeAutoAPHandler extends AbstractMaplePacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		/*
		 * 8E 00 F9 E6 8F 00 02 00 00 00 00 02 00 00 00 00 00 00 DF 03 00 00 00
		 * 01 00 00 00 00 00 00 F3 01 00 00
		 */
		List<Pair<MapleStat, Number>> statupdate = new ArrayList<Pair<MapleStat, Number>>();
		slea.readInt();
		slea.readInt();
		if (c.getPlayer().getRemainingAp() > 0) {
			while (slea.available() > 0) {
				int update = (int) slea.readLong();
				int add = slea.readInt();
				if (c.getPlayer().getRemainingAp() < add) {
					return;
				}
				switch (update) {
				case 64: // Str
					if (c.getPlayer().getStr() >= 32767) {
						return;
					}
					c.getPlayer().setStr(c.getPlayer().getStr() + add);
					statupdate.add(new Pair<MapleStat, Number>(MapleStat.STR, c
							.getPlayer().getStr()));
					break;
				case 128: // Dex
					if (c.getPlayer().getDex() >= 32767) {
						return;
					}
					c.getPlayer().setDex(c.getPlayer().getDex() + add);
					statupdate.add(new Pair<MapleStat, Number>(MapleStat.DEX, c
							.getPlayer().getDex()));
					break;
				case 256: // Int
					if (c.getPlayer().getInt() >= 32767) {
						return;
					}
					c.getPlayer().setInt(c.getPlayer().getInt() + add);
					statupdate.add(new Pair<MapleStat, Number>(MapleStat.INT, c
							.getPlayer().getInt()));
					break;
				case 512: // Luk
					if (c.getPlayer().getLuk() >= 32767) {
						return;
					}
					c.getPlayer().setLuk(c.getPlayer().getLuk() + add);
					statupdate.add(new Pair<MapleStat, Number>(MapleStat.LUK, c
							.getPlayer().getLuk()));
					break;
				default:
					c.getSession().write(MaplePacketCreator.enableActions());
					return;
				}
				c.getPlayer().setRemainingAp(
						c.getPlayer().getRemainingAp() - add);
			}
			statupdate.add(new Pair<MapleStat, Number>(MapleStat.AVAILABLEAP, c
					.getPlayer().getRemainingAp()));
			c.getSession().write(
					MaplePacketCreator.updatePlayerStats(statupdate, true,
							c.getPlayer()));
		} else {
			System.out.printf("[h4x] Player %s is distributing AP with no AP",
					c.getPlayer().getName());
		}
		c.getSession().write(MaplePacketCreator.enableActions());
	}
}