//卷轴
package net.sf.odinms.net.channel.handler;

import java.util.List;
import net.sf.odinms.client.IEquip.ScrollResult;
import net.sf.odinms.client.*;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author Matze
 * @author Frz
 */
public class ScrollHandler extends AbstractMaplePacketHandler {
	// private static Logger log = LoggerFactory.getLogger(ScrollHandler.class);
	// 裤裙防御必成卷 弓攻击必成卷 披风魔法防御必成卷 披风物理防御必成卷

	private static int[] bannedScrolls = { 2040603, 2044503, 2041024, 2041025,
			2044703, 2044603, 2043303, 2040303, 2040807, 2040006, 2040007,
			2043103, 2043203, 2043003, 2040507, 2040506, 2044403, 2040903,
			2040709, 2040710, 2040711, 2044303, 2043803, 2040403, 2044103,
			2044203, 2044003, 2043703 };

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		int actionId = slea.readInt();// 动作ID
		if (actionId <= c.getLastActionId()) {
			c.getSession().write(MaplePacketCreator.enableActions());
			return;
		}
		c.setLastActionId(actionId);
		// slea.readInt();
		byte slot = (byte) slea.readShort();
		byte dst = (byte) slea.readShort();
		byte ws = (byte) slea.readShort();
		boolean whiteScroll = false; // 祝福卷轴
		boolean legendarySpirit = false; // legendary spirit skill 工匠技能

		if ((ws & 2) == 2) {
			whiteScroll = true;
		}
		MapleItemInformationProvider ii = MapleItemInformationProvider
				.getInstance();
		IEquip toScroll;
		if (dst < 0) {
			toScroll = (IEquip) c.getPlayer()
					.getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
		} else {
			legendarySpirit = true;
			toScroll = (IEquip) c.getPlayer()
					.getInventory(MapleInventoryType.EQUIP).getItem(dst);
		}
		if (toScroll == null) {
			return;
		}
		byte oldLevel = toScroll.getLevel();
		byte oldSlots = toScroll.getUpgradeSlots();
		MapleInventory useInventory = c.getPlayer().getInventory(
				MapleInventoryType.USE);
		IItem scroll = useInventory.getItem(slot);
		IItem wscroll = null;

		switch (scroll.getItemId()) {
		case 2049000:// 白医卷轴
		case 2049001:// 白医卷轴
		case 2049002:// 白医卷轴
		case 2049003:// 白医卷轴
			break;
		default:
			if (toScroll.getUpgradeSlots() < 1) {
				c.getSession().write(MaplePacketCreator.getInventoryFull());
				return;
			}
			break;
		}

		List<Integer> scrollReqs = ii.getScrollReqs(scroll.getItemId());
		if (scrollReqs.size() > 0 && !scrollReqs.contains(toScroll.getItemId())) {
			c.getSession().write(MaplePacketCreator.getInventoryFull());
			return;
		}

		if (whiteScroll) {
			wscroll = useInventory.findById(2340000);// 祝福卷轴
			if (wscroll == null || wscroll.getItemId() != 2340000) {
				whiteScroll = false;
				return;
			}
		}
		// 在这里移除判断 例如白衣和混沌是不需要判断[卷轴能砸什么部位]的
		if (!ii.不需要判断部位的卷轴(scroll.getItemId())) {
			if (!ii.canScroll(scroll.getItemId(), toScroll.getItemId())) {
				c.getSession().write(MaplePacketCreator.enableActions());
				return;
			}
		}
		if (scroll.getQuantity() <= 0) {
			throw new InventoryException("<= 0 quantity when scrolling");
		}

		boolean checkIfGM = false;

		/*
		 * if (!c.getPlayer().isGM()) { for (int i = 0; i <
		 * bannedScrolls.length; i++) { if (scroll.getItemId() ==
		 * bannedScrolls[i]) { return; } } }
		 */

		IEquip scrolled = (IEquip) ii.scrollEquipWithId(toScroll,
				scroll.getItemId(), whiteScroll, checkIfGM);
		ScrollResult scrollSuccess = IEquip.ScrollResult.FAIL; // fail
		if (scrolled == null) {
			scrollSuccess = IEquip.ScrollResult.CURSE;
		} else if (scrolled.getLevel() > oldLevel
				|| (ii.isCleanSlate(scroll.getItemId()) && scrolled
						.getUpgradeSlots() == oldSlots + 1)) {
			scrollSuccess = IEquip.ScrollResult.SUCCESS;
		}
		useInventory.removeItem(scroll.getPosition(), (short) 1, false);
		if (whiteScroll) {
			useInventory.removeItem(wscroll.getPosition(), (short) 1, false);
			if (wscroll.getQuantity() < 1) {
				c.getSession().write(
						MaplePacketCreator.clearInventoryItem(
								MapleInventoryType.USE, wscroll.getPosition(),
								false));
			} else {
				c.getSession().write(
						MaplePacketCreator.updateInventorySlot(
								MapleInventoryType.USE, (Item) wscroll));
			}
		}
		if (scrollSuccess == IEquip.ScrollResult.CURSE) {
			c.getSession().write(
					MaplePacketCreator.scrolledItem(scroll, toScroll, true));
			if (dst < 0) {
				c.getPlayer().getInventory(MapleInventoryType.EQUIPPED)
						.removeItem(toScroll.getPosition());
			} else {
				c.getPlayer().getInventory(MapleInventoryType.EQUIP)
						.removeItem(toScroll.getPosition());
			}
		} else {
			c.getSession().write(
					MaplePacketCreator.scrolledItem(scroll, scrolled, false));
		}
		c.getPlayer()
				.getMap()
				.broadcastMessage(
						MaplePacketCreator.getScrollEffect(c.getPlayer()
								.getId(), scrollSuccess, legendarySpirit,
								scroll.getItemId(), toScroll.getItemId()));

		ISkill LS = SkillFactory.getSkill(1003);
		int LSLevel = c.getPlayer().getSkillLevel(LS);

		if (legendarySpirit && LSLevel <= 0) {
			return;
		}
		if (scrollSuccess == IEquip.ScrollResult.SUCCESS) {
			if (c.getPlayer().getLevel() >= 40) {
				c.getPlayer().finishAchievement(12);
			} else {
				c.getSession().write(
						MaplePacketCreator.serverNotice(5,
								"[系统奖励] 恭喜砸卷成功。但是因为系统限制，你未能得到相应的奖励！"));
			}
		}
		// equipped item was scrolled and changed
		if (dst < 0
				&& (scrollSuccess == IEquip.ScrollResult.SUCCESS || scrollSuccess == IEquip.ScrollResult.CURSE)) {
			c.getPlayer().equipChanged();
		}
	}
}