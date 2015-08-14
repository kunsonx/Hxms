/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.client;

import java.util.ArrayList;
import java.util.HashMap;
import net.sf.odinms.database.DatabaseConnection;
import org.apache.log4j.Logger;
import org.hibernate.Session;

/**
 *
 * @author Administrator
 */
public class ItemOptionFactory {

	private static final HashMap<Integer, ItemOption> Options = new HashMap<Integer, ItemOption>();
	private static final HashMap<Integer, ArrayList<Integer>> allid = new HashMap<Integer, ArrayList<Integer>>();
	private static final Logger log = Logger.getLogger(ItemOptionFactory.class);

	static {
		log.info("加载潜能数据...");
		/*
		 * MapleDataProvider dataRoot =
		 * MapleDataProviderFactory.getDataProvider(new
		 * File(System.getProperty("net.sf.odinms.wzpath") + "/Item.wz"));
		 * MapleData OptionData = dataRoot.getData("ItemOption.img"); for
		 * (MapleData Option : OptionData.getChildren()) { int oid =
		 * Integer.valueOf(Option.getName()); ItemOption O =
		 * ItemOption.loadFromData(oid, Option); Options.put(oid, O); }
		 */
		Session session = DatabaseConnection.getSession();
		java.util.List list = session.createQuery("from ItemOption").list();
		for (Object object : list) {
			if (object instanceof ItemOption) {
				ItemOption o = (ItemOption) object;
				Options.put(o.getId(), o);
			}
		}
		session.close();

		for (Integer integer : Options.keySet()) {
			int level = Math.max(integer / 10000, 1);
			if (!allid.containsKey(Integer.valueOf(level))) {
				allid.put(Integer.valueOf(level), new ArrayList<Integer>());
			}
			allid.get(Integer.valueOf(level)).add(integer);
		}
	}

	public static ItemOption getOption(final int id) {
		synchronized (Options) {
			if (!Options.isEmpty()) {
				return Options.get(Integer.valueOf(id));
			}
		}
		return null;
	}

	public static IEquip MagniflerEquipWithId(IItem equip) {
		Equip nEquip = (Equip) equip;
		boolean isFirst = false;
		int oldPotentialLevel = nEquip.getIdentified();
		// System.out.println(oldPotentialLevel);
		int random = (int) Math.ceil(Math.random() * 1000.0);
		if (oldPotentialLevel == 0x11) { // B级
			if (random >= 965) {
				// 90%几率升A级
				nEquip.setIdentify((byte) 0x12);
			} else {
				nEquip.setIdentify(nEquip.getIdentified());
			}
		} else if (oldPotentialLevel == 0x12) { // A级
			if (random >= 985) {
				// 1%几率升S级
				nEquip.setIdentify((byte) 0x13);
			} else {
				nEquip.setIdentify(nEquip.getIdentified());
			}
		} else if (oldPotentialLevel == 0x13) { // S级
			if (random >= 998 && nEquip.getIdentify() == 2) {
				// 1%几率升S级
				nEquip.setIdentify((byte) 0x14);
			} else {
				nEquip.setIdentify(nEquip.getIdentified());
			}
		} else if (oldPotentialLevel == 0x14) {
			nEquip.setIdentify(nEquip.getIdentified());
		} else {
			// 第一次鉴定
			isFirst = true;
			if (random == 1000) {
				nEquip.setIdentify((byte) 0x14); // SS级
			} else if (random >= 995) {
				nEquip.setIdentify((byte) 0x13); // S级
			} else if (random >= 960) {
				nEquip.setIdentify((byte) 0x12); // A级
			} else {
				nEquip.setIdentify((byte) 0x11); // B级
			}
			nEquip.setPotential(1, 1);
			nEquip.setPotential(2, 1);
			if (Math.random() > 0.7) {
				nEquip.setPotential(3, 1);
			}
		}
		if (!isFirst) {
			// 不是第一次的话就重设潜能
			// 第一次的话在上面的判断中已经重新设置了
			for (int i = 1; i <= 3; i++) {
				if (nEquip.getPotential(i) != 0 && Math.random() > 0.2) {
					nEquip.setPotential(i, 1);
				}
			}
		}

		for (int i = 1; i <= 3; i++) {
			if (nEquip.getPotential(i) == 1) {
				nEquip.setPotential(
						i,
						randomPotentiaStat(nEquip, nEquip.getIdentify(),
								nEquip.getItemId(), nEquip.getLevel()));
			}
		}

		if (log.isDebugEnabled()) {
			log.debug("潜能属性 - 1："
					+ getOption(nEquip.getPotential_1()).getString());
			log.debug("潜能属性 - 2："
					+ getOption(nEquip.getPotential_2()).getString());
			// log.debug("潜能属性 - 3：" +
			// getOption(nEquip.getPotential_3()).getString());
		}

		return nEquip;
	}

	public static int randomPotentiaStat(Equip equip, int oldPotentiaLevel,
			int itemId, byte itemLevel) {
		int rndOne;
		int base = oldPotentiaLevel - 16;
		if (1 > base) {
			base = 1;
		}
		rndOne = allid.get(Integer.valueOf(base)).get(
				(int) Math.floor(Math.random()
						* allid.get(Integer.valueOf(base)).size()));
		while (getOption(rndOne).getOptionType() == 10
				&& !GameConstants.isWeapon(equip.getItemId())) {
			rndOne = allid.get(Integer.valueOf(base)).get(
					(int) Math.floor(Math.random()
							* allid.get(Integer.valueOf(base)).size()));
		}
		return rndOne;
	}
}
