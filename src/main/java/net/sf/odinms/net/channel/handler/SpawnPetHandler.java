//召唤宠物
package net.sf.odinms.net.channel.handler;

import java.awt.Point;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.MaplePet;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class SpawnPetHandler extends AbstractMaplePacketHandler {

	/*
	 * TODO: 1. Move the equpping into a function.
	 */
	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		// 6B 00 [B3 20 9C 03] [05] [00]
		if (c.getPlayer().getMap().getId() == 910000000) {
			c.getSession().write(MaplePacketCreator.enableActions());
			return;
		}
		slea.readInt();
		byte slot = slea.readByte();
		boolean lead = slea.readByte() == 1;
		MaplePet pet = (MaplePet) c.getPlayer()
				.getInventory(MapleInventoryType.CASH).getItem(slot);
		pet.setSlot(c.getPlayer().getPetSlot(pet));
		if (pet.getSlot() != -1) {// 装载同样的宠物。就卸载。
			c.getPlayer().unequipPet(pet);
		} else {
			Point pos = c.getPlayer().getPosition();
			pos.y -= 12;
			pet.setPos(pos);
			pet.setFh(c.getPlayer().getMap().getFootholds()
					.findBelow(pet.getPos()).getId());
			pet.setStance(0);
			c.getPlayer().addPet(pet, lead);
			c.getPlayer()
					.getMap()
					.broadcastMessage(
							c.getPlayer(),
							MaplePacketCreator.showPet(c.getPlayer(), pet,
									false), true);
			long uniqueid = pet.getUniqueid();
			c.getSession().write(MaplePacketCreator.updatePet(pet));
			c.getSession().write(MaplePacketCreator.enableActions());
			pet.StartFullnessSchedule(c.getPlayer());
		}
	}
}