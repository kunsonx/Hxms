//宠物捡取
package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.GameConstants;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MaplePet;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.MaplePartyCharacter;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.maps.MapleMapItem;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Raz
 */
public class PetLootHandler extends AbstractMaplePacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		// System.out.println("宠物捡物包："+slea.toString());
		MapleItemInformationProvider ii = MapleItemInformationProvider
				.getInstance();
		if (c.getPlayer().getNoPets() == 0
				|| !c.getPlayer().getMap().isLootable()) {
			c.getSession().write(MaplePacketCreator.enableActions());
			return;
		}
		// MaplePet pet =
		// c.getPlayer().getPet(c.getPlayer().getPetByUniqueId(slea.readInt()));
		MaplePet pet = c.getPlayer().getPet(slea.readInt()); // 093修改
		slea.skip(9);// 089之前是13
		int oid = slea.readInt();// oid
		MapleMapObject ob = c.getPlayer().getMap().getMapObject(oid);
		if (ob == null || pet == null) {
			c.getSession().write(MaplePacketCreator.getInventoryFull());
			return;
		}
		if (ob instanceof MapleMapItem) {
			MapleMapItem mapitem = (MapleMapItem) ob;
			synchronized (mapitem) {
				boolean remove = false;
				if (mapitem.isPickedUp()) {
					c.getSession().write(MaplePacketCreator.getInventoryFull());
					return;
				}
				double distance = pet.getPos()
						.distanceSq(mapitem.getPosition());
				c.getPlayer().getCheatTracker().checkPickupAgain();
				if (distance > 90000.0) { // 300^2, 550 is approximatly the
											// range of ultis
					c.getPlayer().getCheatTracker()
							.registerOffense(CheatingOffense.ITEMVAC);
				} else if (distance > 22500.0) {
					c.getPlayer().getCheatTracker()
							.registerOffense(CheatingOffense.SHORT_ITEMVAC);
				}
				if (mapitem.getDropper() != c.getPlayer()) {
					if (mapitem.getMeso() > 0) {
						if (c.getPlayer().getParty() != null) {
							ChannelServer cserv = c.getChannelServer();
							int pMembers = 0;
							for (MaplePartyCharacter partymem : c.getPlayer()
									.getParty().getMembers()) {
								if (partymem != null
										&& cserv.getPlayerStorage()
												.getCharacterById(
														partymem.getId()) != null) {
									if (cserv.getPlayerStorage()
											.getCharacterById(partymem.getId())
											.getMapId() == c.getPlayer()
											.getMapId()) {
										pMembers++;
									}
								}
							}
							if (pMembers > 1) {
								for (MaplePartyCharacter partymem : c
										.getPlayer().getParty().getMembers()) {
									if (partymem != null
											&& cserv.getPlayerStorage()
													.getCharacterById(
															partymem.getId()) != null) {
										if (cserv
												.getPlayerStorage()
												.getCharacterById(
														partymem.getId())
												.getMapId() == c.getPlayer()
												.getMapId()) {
											cserv.getPlayerStorage()
													.getCharacterById(
															partymem.getId())
													.gainMeso(
															mapitem.getMeso()
																	/ pMembers,
															true, true);
										}
									}
								}
							} else {
								c.getPlayer().gainMeso(mapitem.getMeso(), true,
										true);
							}
							remove = true;
						} else {
							if (c.getPlayer().getMeso() == Integer.MAX_VALUE) {
								remove = false;
							} else {
								c.getPlayer().gainMeso(mapitem.getMeso(), true,
										true);
							}
							remove = true;
						}
						if (remove) {
							c.getPlayer()
									.getMap()
									.broadcastMessage(
											MaplePacketCreator.removeItemFromMap(
													mapitem.getObjectId(), 5, c
															.getPlayer()
															.getId(), true, c
															.getPlayer()
															.getPetSlot(pet)),
											mapitem.getPosition());
							c.getPlayer().getCheatTracker().pickupComplete();
							c.getPlayer().getMap().removeMapObject(ob);
						}
					} else if (mapitem.getItem() != null) {
						if (GameConstants.isPet(mapitem.getItem().getItemId())) {
							MapleInventoryManipulator.addFromDrop(c, MaplePet
									.createPet(mapitem.getItem().getItemId()));
							c.getPlayer()
									.getMap()
									.broadcastMessage(
											MaplePacketCreator.removeItemFromMap(
													mapitem.getObjectId(), 5, c
															.getPlayer()
															.getId()),
											mapitem.getPosition());
							c.getPlayer().getCheatTracker().pickupComplete();
							c.getPlayer().getMap().removeMapObject(ob);
							remove = true;
						} else {
							if (MapleInventoryManipulator.addFromDrop(c,
									mapitem.getItem(), "Picked up by "
											+ c.getPlayer().getName(), true)) {
								c.getPlayer()
										.getMap()
										.broadcastMessage(
												MaplePacketCreator
														.removeItemFromMap(
																mapitem.getObjectId(),
																5,
																c.getPlayer()
																		.getId(),
																true,
																c.getPlayer()
																		.getPetSlot(
																				pet)),
												mapitem.getPosition());
								c.getPlayer().getCheatTracker()
										.pickupComplete();
								c.getPlayer().getMap().removeMapObject(ob);
								remove = true;
							} else {
								c.getPlayer().getCheatTracker()
										.pickupComplete();
								remove = false;
							}
						}
					}
				}
				if (remove) {
					mapitem.setPickedUp(true);
				}
			}
		}
		c.getSession().write(MaplePacketCreator.enableActions());
	}
}