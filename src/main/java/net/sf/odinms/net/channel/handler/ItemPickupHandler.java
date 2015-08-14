package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.GameConstants;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MaplePet;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.server.AutobanManager;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.maps.MapleMapItem;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.apache.log4j.Logger;

/**
 * @author Matze
 */
public class ItemPickupHandler extends AbstractMaplePacketHandler {

	private Logger log = Logger.getLogger(getClass());

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		slea.readByte();
		slea.readLong();
		int oid = slea.readInt();
		MapleMapObject ob = c.getPlayer().getMap().getMapObject(oid);
		if (!c.getPlayer().getMap().isLootable() && !c.getPlayer().isGM()) {
			c.getSession().write(MaplePacketCreator.enableActions());
			return;
		}
		if (ob == null) {
			c.getSession().write(MaplePacketCreator.serverNotice(1, "背包已满。"));
			c.getSession().write(MaplePacketCreator.enableActions());
			return;
		}
		synchronized (ob) {
			if (ob instanceof MapleMapItem) {
				MapleMapItem mapitem = (MapleMapItem) ob;
				if (log.isDebugEnabled() && c.getPlayer().isGM()
						&& mapitem.getItem() != null) {
					c.getPlayer().dropMessage(
							"捡取物品：" + mapitem.getItem().getItemId());
				}
				if (mapitem.isPickedUp()) {
					c.getSession().write(MaplePacketCreator.getInventoryFull());
					c.getSession().write(
							MaplePacketCreator.getShowInventoryFull());
					return;
				}
				mapitem.setPickedUp(true);
				double distance = c.getPlayer().getPosition()
						.distanceSq(mapitem.getPosition());
				c.getPlayer().getCheatTracker().checkPickupAgain();
				if (distance > 90000.0) {
					AutobanManager.getInstance().addPoints(c, 100, 300000,
							"Itemvac");
					c.getPlayer().getCheatTracker()
							.registerOffense(CheatingOffense.ITEMVAC);
				} else if (distance > 30000.0) {
					c.getPlayer().getCheatTracker()
							.registerOffense(CheatingOffense.SHORT_ITEMVAC);
				}
				if (mapitem.getMeso() > 0) {
					if (c.getPlayer().getParty() != null
							&& mapitem.getDropper() != c.getPlayer()) {
						ChannelServer cserv = c.getChannelServer();
						int mesosamm = mapitem.getMeso();
						int partynum = 0;
						for (MaplePartyCharacter partymem : c.getPlayer()
								.getParty().getMembers()) {
							if (partymem.isOnline()
									&& partymem.getMapid() == c.getPlayer()
											.getMap().getId()
									&& partymem.getChannel() == c.getChannel()) {
								partynum++;
							}
						}
						int mesosgain = mesosamm / partynum;
						for (MaplePartyCharacter partymem : c.getPlayer()
								.getParty().getMembers()) {
							if (partymem.isOnline()
									&& partymem.getMapid() == c.getPlayer()
											.getMap().getId()) {
								MapleCharacter somecharacter = cserv
										.getPlayerStorage().getCharacterById(
												partymem.getId());
								if (somecharacter != null) {
									somecharacter.gainMeso(mesosgain, true,
											true);
								}
							}
						}
					} else {
						c.getPlayer().gainMeso(mapitem.getMeso(), true, true);
					}
					c.getPlayer()
							.getMap()
							.broadcastMessage(
									MaplePacketCreator.removeItemFromMap(
											mapitem.getObjectId(), 2, c
													.getPlayer().getId()),
									mapitem.getPosition());
					c.getPlayer().getCheatTracker().pickupComplete();
					c.getPlayer().getMap().removeMapObject(ob);
				} else if (useItem(c, mapitem.getItem().getItemId())) {
					c.getPlayer()
							.getMap()
							.broadcastMessage(
									MaplePacketCreator.removeItemFromMap(
											mapitem.getObjectId(), 2, c
													.getPlayer().getId()),
									mapitem.getPosition());
					c.getPlayer().getMap().removeMapObject(ob);
				} else {
					if (GameConstants.isPet(mapitem.getItem().getItemId())) {
						MapleInventoryManipulator.addFromDrop(c, MaplePet
								.createPet(mapitem.getItem().getItemId()));
						c.getPlayer()
								.getMap()
								.broadcastMessage(
										MaplePacketCreator.removeItemFromMap(
												mapitem.getObjectId(), 2, c
														.getPlayer().getId()),
										mapitem.getPosition());
						c.getPlayer().getCheatTracker().pickupComplete();
						c.getPlayer().getMap().removeMapObject(ob);
					} else {

						if (MapleInventoryManipulator.addFromDrop(c,
								mapitem.getItem(), "Picked up by "
										+ c.getPlayer().getName(), true)) {
							c.getPlayer()
									.getMap()
									.broadcastMessage(
											MaplePacketCreator.removeItemFromMap(
													mapitem.getObjectId(), 2, c
															.getPlayer()
															.getId()),
											mapitem.getPosition());
							c.getPlayer().getCheatTracker().pickupComplete();
							c.getPlayer().getMap().removeMapObject(ob);
						} else {
							c.getPlayer().getCheatTracker().pickupComplete();
							return;
						}
					}
				}

			}
		}
		c.getSession().write(MaplePacketCreator.enableActions());
	}

	static boolean useItem(final MapleClient c, final int id) { // 怪物书拣取效果操作
		if (id / 1000000 == 2) {
			MapleItemInformationProvider ii = MapleItemInformationProvider
					.getInstance();
			if (ii.isConsumeOnPickup(id)) {
				ii.getItemEffect(id).applyTo(c.getPlayer());
				return true;
			}
			if (id == 2430513) {
				Integer LittleIndian = c.getPlayer().getAttribute()
						.getDataValue("LittleIndian");
				if (LittleIndian == null) {
					LittleIndian = 0;
				}
				LittleIndian++;
				c.getPlayer().getAttribute()
						.setDataValue("LittleIndian", LittleIndian);
				c.SendPacket(MaplePacketCreator.getGamePropertyChange(
						"LittleIndian", LittleIndian.toString()));
				return true;
			}
		}
		return false;
	}
}
