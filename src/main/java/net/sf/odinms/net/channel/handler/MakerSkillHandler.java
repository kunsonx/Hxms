//锻造
package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.Equip;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MakerItemFactory;
import net.sf.odinms.server.MakerItemFactory.MakerItemCreateEntry;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;

public final class MakerSkillHandler extends AbstractMaplePacketHandler {

	private static org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(MakerSkillHandler.class);

	public final void handlePacket(SeekableLittleEndianAccessor slea,
			MapleClient c) {
		MapleItemInformationProvider ii = MapleItemInformationProvider
				.getInstance();
		int Meso = 0;
		slea.readInt();
		int toCreate = slea.readInt();// 要制造的itemid
		if (slea.available() >= 5 && slea.available() <= 17) {
			log.debug("普通类 强化宝石 装备");
			boolean UseCatalyst = false; // 是否使用促进剂
			boolean UseCrystal = false; // 是否使用强化宝石
			if (slea.readByte() == 1) {
				UseCatalyst = true;
			}
			log.debug("获得的itemid：" + toCreate);
			// 通过此语句获取需要的条件 具体跳转声明去看
			MakerItemCreateEntry recipe = MakerItemFactory.getItemCreateEntry(
					toCreate, UseCatalyst);
			if (canCreate(c, recipe) // 判断是否可以制造 具体看下面的函数
					&& !c.getPlayer()
							.getInventory(ii.getInventoryType(toCreate))
							.isFull() // 锻造获得的物品所在的栏是否满了
			) {
				int a = recipe.getCost();
				Meso += a * 1.1; // 盛大加了10%的税
				for (Pair<Integer, Integer> p : recipe.getReqItems()) {
					// Left是itemid Right是数量
					int toRemove = p.getLeft();
					log.debug("扣除的itemid: " + toRemove);
					log.debug("扣除的item数量: " + p.getRight());
					MapleInventoryManipulator.removeById(c,
							ii.getInventoryType(toRemove), toRemove,
							p.getRight(), false, false);
				}

				Equip toDrop = (Equip) ii.getEquipById(toCreate);

				if (slea.available() > 4) { // 使用了强化宝石
					int UseAmount = slea.readInt();// 使用强化宝石的数量
					Meso += UseAmount * a * 0.9; // 每增加一颗强化宝石 金钱数量增加所需金钱节点值的90%
					if (UseAmount >= 1) {
						UseCrystal = true;// 不知道用来干什么好
						int UseCrystal1 = slea.readInt();// 强化宝石1
						toDrop = (Equip) ii.MakeItem(toDrop, UseCrystal1);
						MapleInventoryManipulator.removeById(c,
								ii.getInventoryType(UseCrystal1), UseCrystal1,
								1, false, false);
						log.debug("使用了1个强化宝石");
					}
					if (UseAmount >= 2) {
						int UseCrystal2 = slea.readInt();// 强化宝石2
						toDrop = (Equip) ii.MakeItem(toDrop, UseCrystal2);
						MapleInventoryManipulator.removeById(c,
								ii.getInventoryType(UseCrystal2), UseCrystal2,
								1, false, false);
						log.debug("使用了2个强化宝石");
					}
					if (UseAmount == 3) {
						int UseCrystal3 = slea.readInt();// 强化宝石3
						toDrop = (Equip) ii.MakeItem(toDrop, UseCrystal3);
						MapleInventoryManipulator.removeById(c,
								ii.getInventoryType(UseCrystal3), UseCrystal3,
								1, false, false);
						log.debug("使用了3个强化宝石");
					}
				}

				if (c.getPlayer().getMeso() >= Meso) {
					c.getPlayer().gainMeso(-Meso, false);
				} else {
					c.getPlayer().dropMessage(1,
							"您的金钱不足 需要" + Meso + "金币才可以锻造此物品。");
				}

				if (UseCatalyst && UseCrystal) {
					toDrop = ii.randomizeStats(toDrop);
					MapleInventoryManipulator.addFromDrop(c, toDrop, null);
					log.debug("使用了促进剂+强化宝石");

					// } else if(UseCrystal){
					// MapleInventoryManipulator.addFromDrop(c, toDrop, null);
					// log.debug("使用了强化宝石");
				} else {
					MapleInventoryManipulator.addFromDrop(c, toDrop, null);
					log.debug("什么都没有使用");
				}
			} else {
				c.getPlayer().dropMessage(1, "该物品尚未添加进数据库，请告知Gm。");
				log.debug("数据库缺少相应物品");
			}
		} else if (slea.available() == 0) {
			log.debug("怪物结晶类");
			// int itemid = getCrystalId(toCreate);
			int itemid = ii.getCrystalId(toCreate);
			if (itemid < 0) {
				c.getPlayer().dropMessage(1, "获取的itemid有错 值为： " + itemid);
			}
			log.debug("扣除的itemid: " + toCreate);
			log.debug("得到的itemid: " + itemid);
			MapleInventoryManipulator.removeById(c,
					ii.getInventoryType(toCreate), toCreate, 100, false, false);
			MapleInventoryManipulator.addById(c, itemid, (short) 1, null);
			c.getSession().write(
					MaplePacketCreator
							.getShowItemGain(itemid, (short) 1, false));
		} else {
			log.debug("未知类");
		}
		c.getSession().write(MaplePacketCreator.enableActions());
	}

	private boolean canCreate(MapleClient c, MakerItemCreateEntry recipe) {
		if (c.getPlayer().isEvan()) {
			log.debug("角色是龙神 判断的skill是20011007");
		}

		return hasItems(c, recipe) // 需要扣除的物品
				&& c.getPlayer().getMeso() >= recipe.getCost() // 金钱
				&& c.getPlayer().getLevel() >= recipe.getReqLevel() // 人物等级
				&& recipe.getRewardAmount() > 0
				// 龙神比较特殊
				&& (c.getPlayer().isEvan() ? 20011007
						: c.getPlayer()
								.getSkillLevel(
										c.getPlayer().getJob().getId() / 1000 * 10000000 + 1007)) >= recipe
						.getReqSkillLevel();
	}

	private boolean hasItems(MapleClient c, MakerItemCreateEntry recipe) {
		MapleItemInformationProvider ii = MapleItemInformationProvider
				.getInstance();
		for (Pair<Integer, Integer> p : recipe.getReqItems()) {
			// Left是itemid Right是数量
			int itemId = p.getLeft();
			if (c.getPlayer().getInventory(ii.getInventoryType(itemId))
					.countById(itemId) < p.getRight()) {
				return false;
			}
		}
		return true;
	}
}
