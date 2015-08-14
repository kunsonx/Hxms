//使用宠物食品
package net.sf.odinms.net.channel.handler;

import java.util.List;
import net.sf.odinms.client.ExpTable;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.MaplePet;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Randomizer;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public class PetFoodHandler extends AbstractMaplePacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		if (c.getPlayer().getNoPets() == 0) {
			c.getSession().write(MaplePacketCreator.enableActions());
			return;
		}
		int slot = 0;
		for (MaplePet pet : c.getPlayer().getPets()) {
			if (pet != null && pet.getFullness() < 100) {
				slot = c.getPlayer().getPetSlot(pet);
			}
		}
		MaplePet pet = c.getPlayer().getPet(slot);
		if (pet == null) {
			c.getSession().write(MaplePacketCreator.enableActions());
			return;
		}
		slea.readInt();
		slea.readShort();
		int itemId = slea.readInt();

		boolean gainCloseness = false;

		if (Randomizer.getInstance().nextInt(101) > 50) {
			gainCloseness = true;
		}
		if (pet.getFullness() < 100) {
			int newFullness = pet.getFullness() + 30;
			if (newFullness > 100) {
				newFullness = 100;
			}
			pet.setFullness(newFullness);
			if (gainCloseness && pet.getCloseness() < 30000) {
				int newCloseness = pet.getCloseness()
						+ (1 * c.getChannelServer().getPetExpRate());
				if (newCloseness > 30000) {
					newCloseness = 30000;
				}
				pet.setCloseness(newCloseness);
				if (newCloseness >= ExpTable.getClosenessNeededForLevel(pet
						.getLevel() + 1)) {
					pet.setLevel(pet.getLevel() + 1);
					c.getSession().write(
							MaplePacketCreator.showOwnPetLevelUp(c.getPlayer()
									.getPetSlot(pet)));
					c.getPlayer()
							.getMap()
							.broadcastMessage(
									MaplePacketCreator.showPetLevelUp(c
											.getPlayer(), c.getPlayer()
											.getPetSlot(pet)));
				}
			}
			c.getSession().write(MaplePacketCreator.updatePet(pet));
			c.getPlayer()
					.getMap()
					.broadcastMessage(
							c.getPlayer(),
							MaplePacketCreator.commandResponse(c.getPlayer()
									.getId(), slot, 1, true), true);
		} else {
			if (gainCloseness) {
				int newCloseness = pet.getCloseness()
						- (1 * c.getChannelServer().getPetExpRate());
				if (newCloseness < 0) {
					newCloseness = 0;
				}
				pet.setCloseness(newCloseness);
				if (newCloseness < ExpTable.getClosenessNeededForLevel(pet
						.getLevel())) {
					pet.setLevel(pet.getLevel() - 1);
				}
			}
			c.getSession().write(MaplePacketCreator.updatePet(pet));
			c.getPlayer()
					.getMap()
					.broadcastMessage(
							c.getPlayer(),
							MaplePacketCreator.commandResponse(c.getPlayer()
									.getId(), slot, 1, false), true);
		}
		MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemId,
				1, true, false);
	}
}
