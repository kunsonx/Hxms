/*
 更换地图处理程序
 */
package net.sf.odinms.net.channel.handler;

import java.net.InetAddress;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class ChangeMapHandler extends AbstractMaplePacketHandler {

	private static org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(ChangeMapHandler.class);

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		if (slea.available() == 0) {
			if (c.getPlayer().getParty() != null) {
				c.getPlayer().setParty(c.getPlayer().getParty());
			}
			String ip = c.getChannelServer().getIP(c.getChannel());
			String[] socket = ip.split(":");
			c.getPlayer().saveToDB(true);
			c.getPlayer().setInCS(false);
			c.getPlayer().setInMTS(false);
			c.getPlayer().cancelSavedBuffs();
			c.getChannelServer().removePlayer(c.getPlayer());
			c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
			try {
				c.getSession().write(
						MaplePacketCreator.getChannelChange(
								InetAddress.getByName(socket[0]),
								Integer.parseInt(socket[1])));
				// c.getSession().close(false);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			int type = slea.readByte(); // 1 = from dying 2 = regular portals
			int targetid = slea.readInt(); // FF FF FF FF
			String startwp = slea.readMapleAsciiString();
			MaplePortal portal = c.getPlayer().getMap().getPortal(startwp);
			MapleCharacter player = c.getPlayer();

			if (targetid != -1 && !c.getPlayer().isAlive()) {
				boolean executeStandardPath = true;
				if (player.getEventInstance() != null) {
					executeStandardPath = player.getEventInstance()
							.revivePlayer(player);
				}
				if (executeStandardPath) {
					c.getPlayer().initPower();
					if (c.getPlayer().haveItem(5510000, 1, false, true)) {
						c.getPlayer().setHp(50);
						MapleInventoryManipulator.removeById(c,
								MapleInventoryType.CASH, 5510000, 1, true,
								false);
						c.getPlayer().changeMap(c.getPlayer().getMap(),
								c.getPlayer().getMap().getPortal(0));
						c.getPlayer().updateSingleStat(MapleStat.HP, 50);
						c.getSession().write(
								MaplePacketCreator.serverNotice(5,
										"使用了原地复活术。死亡后您在当前地图复活。"));
					} else {
						player.setHp(50);
						if (c.getPlayer().getMap().getForcedReturnId() != 999999999) {
							MapleMap to = c.getPlayer().getMap()
									.getForcedReturnMap();
							MaplePortal pto = to.getPortal(0);
							player.setStance(0);
							player.changeMap(to, pto);
						} else {
							MapleMap to = c.getPlayer().getMap().getReturnMap();
							if (to == null) {
								to = c.getChannelServer().getMapFactory()
										.getMap(910000000);
							}
							MaplePortal pto = to.getPortal(0);
							player.setStance(0);
							player.changeMap(to, pto);
						}
					}
				}
				player.sendAddAttackLimit();
				player.sendDemonAvengerPacket();
			} else if (targetid != -1 && c.getPlayer().isGM()) {
				MapleMap to = c.getChannelServer().getMapFactory()
						.getMap(targetid);
				MaplePortal pto = to.getPortal(0);
				player.changeMap(to, pto);
			} else if (targetid != -1 && !c.getPlayer().isGM()) {
				MapleMap to = c.getChannelServer().getMapFactory()
						.getMap(targetid);
				if (c.getPlayer().isGM()
						|| ((player.getMapId() == 0 && to.getId() == 10000)
								|| (player.getMapId() == 914090010 && to
										.getId() == 914090011)
								|| (player.getMapId() == 914090011 && to
										.getId() == 914090012)
								|| (player.getMapId() == 914090012 && to
										.getId() == 914090013) || (player
								.getMapId() == 914090013 && to.getId() == 140090000))) {
					MaplePortal pto = to.getPortal(0);
					player.changeMap(to, pto);
				} else {
					c.getSession().write(MaplePacketCreator.enableActions());
					log.warn("玩家 " + c.getPlayer().getName() + " 试图以非正常方式切换地图！");
				}
			} else {
				if (portal != null) {
					portal.enterPortal(c);
				} else {
					c.getSession().write(MaplePacketCreator.enableActions());
					log.warn("传送点 " + startwp + " 没有在 "
							+ c.getPlayer().getMap().getId() + " 里找到");
				}
			}

		}
	}
}
