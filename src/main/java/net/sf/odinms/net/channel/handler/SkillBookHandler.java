//技能书
package net.sf.odinms.net.channel.handler;

import java.util.Map;

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.tools.Randomizer;

public class SkillBookHandler extends AbstractMaplePacketHandler {

	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		// 55 00 4D 92 B5 01 11 00 [86 F1 22 00]
		if (!c.getPlayer().isAlive()) {
			c.getSession().write(MaplePacketCreator.enableActions());
			return;
		}
		slea.readInt();
		byte slot = (byte) slea.readShort();
		int itemId = slea.readInt();// 技能书ID
		MapleCharacter player = c.getPlayer();
		IItem toUse = c.getPlayer().getInventory(MapleInventoryType.USE)
				.getItem(slot);

		int skill = 0;
		int maxlevel;
		if (toUse != null && toUse.getQuantity() == 1) {
			if (toUse.getItemId() != itemId) {
				return;
			}
			Map<String, Integer> skilldata = MapleItemInformationProvider
					.getInstance().getSkillStats(toUse.getItemId(),
							c.getPlayer().getJob().getId());
			ISkill skill2 = SkillFactory.getSkill(skilldata.get("skillid"));
			boolean canuse = false;
			boolean success = false;
			if (skilldata == null) { // Hacking or used an unknown item
				return;
			}
			maxlevel = player.getMasterLevel(skill2);
			if (skilldata.get("skillid") == 0) { // Wrong Job
				canuse = false;
			} else if (player.getSkillLevel(SkillFactory.getSkill(skilldata
					.get("skillid"))) >= skilldata.get("reqSkillLevel")
					&& player.getMasterLevel(SkillFactory.getSkill(skilldata
							.get("skillid"))) < skilldata.get("masterLevel")) {
				canuse = true;
				if (Randomizer.getInstance().nextInt(101) < skilldata
						.get("success") && skilldata.get("success") != 0) {
					success = true;
					player.changeSkillLevel(skill2,
							player.getSkillLevel(skill2),
							skilldata.get("masterLevel"));
					skill = skilldata.get("skillid");
				} else {
					success = false;
				}
				MapleInventoryManipulator.removeFromSlot(c,
						MapleInventoryType.USE, slot, (short) 1, false);
			} else { // Failed to meet skill requirements
				canuse = false;
			}
			maxlevel = skilldata.get("masterLevel");
			if (!canuse) {
				success = false;
			}
			player.getClient()
					.getSession()
					.write(MaplePacketCreator.skillBookSuccess(player, skill,
							maxlevel, canuse, success));
		}
	}
}