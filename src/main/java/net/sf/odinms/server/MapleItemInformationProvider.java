//读Item.Wz
package net.sf.odinms.server;

import java.awt.Point;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import net.sf.odinms.client.*;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.channel.handler.FishingHandler.MapleFish;
import net.sf.odinms.provider.*;
import net.sf.odinms.server.ORM.*;
import net.sf.odinms.server.constants.InventoryConstants;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.Randomizer;
import org.hibernate.Session;

/**
 *
 * @author Matze
 *
 *         TODO: make faster
 *
 */
public class MapleItemInformationProvider {

	static org.apache.log4j.Logger log = org.apache.log4j.Logger
			.getLogger(MapleItemInformationProvider.class);
	private static MapleItemInformationProvider instance = null;
	protected MapleDataProvider itemData;
	protected MapleDataProvider equipData;
	protected MapleDataProvider stringData;
	protected MapleData cashStringData;
	protected MapleData consumeStringData;
	protected MapleData eqpStringData;
	protected MapleData etcStringData;
	protected MapleData insStringData;
	protected MapleData petStringData;
	protected Map<Integer, MapleInventoryType> inventoryTypeCache = new HashMap<Integer, MapleInventoryType>();
	protected Map<Integer, Short> slotMaxCache = new HashMap<Integer, Short>();
	protected Map<Integer, MapleStatEffect> itemEffects = new HashMap<Integer, MapleStatEffect>();
	protected Map<Integer, Map<String, Integer>> equipStatsCache = new HashMap<Integer, Map<String, Integer>>();
	protected Map<Integer, Equip> equipCache = new HashMap<Integer, Equip>();
	protected Map<Integer, Double> priceCache = new HashMap<Integer, Double>();
	protected Map<Integer, Integer> wholePriceCache = new HashMap<Integer, Integer>();
	protected Map<Integer, Integer> projectileWatkCache = new HashMap<Integer, Integer>();
	protected Map<Integer, String> nameCache = new HashMap<Integer, String>();
	protected Map<Integer, String> descCache = new HashMap<Integer, String>();
	protected Map<Integer, String> msgCache = new HashMap<Integer, String>();
	protected Map<Integer, Boolean> dropRestrictionCache = new HashMap<Integer, Boolean>();
	protected Map<Integer, Boolean> pickupRestrictionCache = new HashMap<Integer, Boolean>();
	protected Map<Integer, Boolean> isQuestItemCache = new HashMap<Integer, Boolean>();
	protected Map<Integer, List<SummonEntry>> summonEntryCache = new HashMap<Integer, List<SummonEntry>>();
	protected List<Pair<Integer, String>> itemNameCache = new ArrayList<Pair<Integer, String>>();
	protected Map<Integer, Integer> getMesoCache = new HashMap<Integer, Integer>();
	protected Map<Integer, Integer> getExpCache = new HashMap<Integer, Integer>();
	protected Map<Integer, String> itemTypeCache = new HashMap<Integer, String>();
	protected Map<Integer, MapleExpCardListInfo> getExpCardTimes = new HashMap<Integer, MapleExpCardListInfo>();
	protected Map<Integer, Integer> scriptedItemCache = new HashMap<Integer, Integer>();
	protected Map<Integer, Integer> monsterBookID = new HashMap<Integer, Integer>();
	protected Map<Integer, Boolean> consumeOnPickupCache = new HashMap<Integer, Boolean>();
	protected Map<Integer, List<Integer>> scrollRestrictionCache = new HashMap<Integer, List<Integer>>();
	protected Map<Integer, Boolean> karmaCache = new HashMap<Integer, Boolean>();
	protected Map<Integer, List<MapleFish>> fishingCache = new HashMap<Integer, List<MapleFish>>();
	protected Map<Integer, Integer> getItemLevel = new HashMap<Integer, Integer>();
	protected Map<Integer, Integer> getMakeItemMinLevel = new HashMap<Integer, Integer>();
	protected Map<Integer, Integer> getMakeItemMaxLevel = new HashMap<Integer, Integer>();
	protected Map<Integer, Pair<Integer, Integer>> getCrystalId = new HashMap<Integer, Pair<Integer, Integer>>();
	protected List<Integer> getAllCrystalId = new ArrayList<Integer>();
	protected Map<Integer, Pair<Integer, Integer>> getPotentialId = new HashMap<Integer, Pair<Integer, Integer>>();
	protected Map<Integer, MapleAndroidInfo> androids = new HashMap<Integer, MapleAndroidInfo>();

	protected MapleItemInformationProvider() {
		loadCardIdData();
		itemData = MapleDataProviderFactory.getDataProvider(new File(System
				.getProperty("net.sf.odinms.wzpath") + "/Item.wz"));
		equipData = MapleDataProviderFactory.getDataProvider(new File(System
				.getProperty("net.sf.odinms.wzpath") + "/Character.wz"));
		stringData = MapleDataProviderFactory.getDataProvider(new File(System
				.getProperty("net.sf.odinms.wzpath") + "/String.wz"));
		cashStringData = MapleDataProviderFactory.getDataProvider(
				new File(System.getProperty("net.sf.odinms.wzpath")
						+ "/String.wz")).getData("Cash.img");
		consumeStringData = MapleDataProviderFactory.getDataProvider(
				new File(System.getProperty("net.sf.odinms.wzpath")
						+ "/String.wz")).getData("Consume.img");
		eqpStringData = MapleDataProviderFactory.getDataProvider(
				new File(System.getProperty("net.sf.odinms.wzpath")
						+ "/String.wz")).getData("Eqp.img");
		etcStringData = MapleDataProviderFactory.getDataProvider(
				new File(System.getProperty("net.sf.odinms.wzpath")
						+ "/String.wz")).getData("Etc.img");
		insStringData = MapleDataProviderFactory.getDataProvider(
				new File(System.getProperty("net.sf.odinms.wzpath")
						+ "/String.wz")).getData("Ins.img");
		petStringData = MapleDataProviderFactory.getDataProvider(
				new File(System.getProperty("net.sf.odinms.wzpath")
						+ "/String.wz")).getData("Pet.img");
		loaddefData();
	}

	public static MapleItemInformationProvider getInstance() {
		if (instance == null) {
			instance = new MapleItemInformationProvider();
		}
		return instance;
	}

	public static void setInstance(MapleItemInformationProvider instance) {
		MapleItemInformationProvider.instance = instance;
	}

	public final void loaddefData() {
		Session session = DatabaseConnection.getSession();

		// List<MapleItemInventryType> itlist =
		// session.createQuery("from MapleItemInventryType").list();
		List<MapleItemInventryType> itlist = session.getNamedQuery("alliit")
				.list();
		for (MapleItemInventryType mapleItemInventryType : itlist) {
			inventoryTypeCache.put(mapleItemInventryType.getId(),
					mapleItemInventryType.getType());
		}

		List<MapleScriptedItemNpc> list = session.getNamedQuery("allsin")
				.list();
		for (MapleScriptedItemNpc mapleScriptedItemNpc : list) {
			scriptedItemCache.put(mapleScriptedItemNpc.getId(),
					mapleScriptedItemNpc.getNpc());
		}

		List<MapleExpCardListInfo> e_list = session.createQuery(
				"from MapleExpCardListInfo").list();
		for (MapleExpCardListInfo mapleExpCardListInfo : e_list) {
			getExpCardTimes.put(mapleExpCardListInfo.getItemid(),
					mapleExpCardListInfo);
		}

		List<MapleItemSlotMax> s_list = session.getNamedQuery("allism").list();
		for (MapleItemSlotMax max : s_list) {
			slotMaxCache.put(max.getItemid(), (short) max.getSlotmax());
		}

		List<MapleItemMeso> m_list = session.getNamedQuery("allim").list();
		for (MapleItemMeso meso : m_list) {
			getMesoCache.put(meso.getItemid(), meso.getMeso());
		}

		List<MapleItemWholePrice> w_list = session.getNamedQuery("alliwp")
				.list();
		for (MapleItemWholePrice wp : w_list) {
			wholePriceCache.put(wp.getItemid(), wp.getWholePrice());
		}

		List<MapleItemType> it_list = session.getNamedQuery("allit").list();
		for (MapleItemType itemType : it_list) {
			itemTypeCache.put(itemType.getItemid(), itemType.getType());
		}

		List<MapleItemPrice> ip_list = session.getNamedQuery("allip").list();
		for (MapleItemPrice mapleItemPrice : ip_list) {
			priceCache.put(mapleItemPrice.getItemid(),
					mapleItemPrice.getPrice());
		}

		List<MapleEquipStats> eslist = session.getNamedQuery("alles").list();
		for (MapleEquipStats mapleEquipStats : eslist) {
			equipStatsCache.put(mapleEquipStats.getItemid(),
					mapleEquipStats.getBaseStats());
		}

		List<MapleAndroidInfo> alist = session.getNamedQuery("allai").list();
		for (MapleAndroidInfo ainfo : alist) {
			androids.put(ainfo.getId(), ainfo);
		}

		session.close();
	}

	public MapleAndroidInfo getAndroidInfo(int index) {
		return androids.get(index);
	}

	public MapleInventoryType getInventoryType(int itemId) {
		if (inventoryTypeCache.containsKey(itemId)) {
			return inventoryTypeCache.get(itemId);
		} else {
			MapleInventoryType stats = MapleInventoryType.UNDEFINED;
			/*
			 * Session session = DatabaseConnection.getSession();
			 * MapleItemInventryType type = (MapleItemInventryType)
			 * session.createQuery
			 * ("from MapleItemInventryType as cs where cs.id = ?"
			 * ).setMaxResults(1).setInteger(0, itemId).uniqueResult(); if (type
			 * != null) { stats = type.getType(); } session.close();
			 */
			inventoryTypeCache.put(itemId, stats);
			return stats;
		}
	}

	public List<Pair<Integer, String>> getAllItems() {
		if (!itemNameCache.isEmpty()) {
			return itemNameCache;
		}
		List<Pair<Integer, String>> itemPairs = new ArrayList<Pair<Integer, String>>();
		MapleData itemsData;

		itemsData = stringData.getData("Cash.img");
		for (MapleData itemFolder : itemsData.getChildren()) {
			int itemId = Integer.parseInt(itemFolder.getName());
			String itemName = MapleDataTool.getString("name", itemFolder,
					"NO-NAME");
			itemPairs.add(new Pair<Integer, String>(itemId, itemName));
		}

		itemsData = stringData.getData("Consume.img");
		for (MapleData itemFolder : itemsData.getChildren()) {
			int itemId = Integer.parseInt(itemFolder.getName());
			String itemName = MapleDataTool.getString("name", itemFolder,
					"NO-NAME");
			itemPairs.add(new Pair<Integer, String>(itemId, itemName));
		}

		itemsData = stringData.getData("Eqp.img").getChildByPath("Eqp");
		for (MapleData eqpType : itemsData.getChildren()) {
			for (MapleData itemFolder : eqpType.getChildren()) {
				int itemId = Integer.parseInt(itemFolder.getName());
				String itemName = MapleDataTool.getString("name", itemFolder,
						"NO-NAME");
				itemPairs.add(new Pair<Integer, String>(itemId, itemName));
			}
		}

		itemsData = stringData.getData("Etc.img").getChildByPath("Etc");
		for (MapleData itemFolder : itemsData.getChildren()) {
			int itemId = Integer.parseInt(itemFolder.getName());
			String itemName = MapleDataTool.getString("name", itemFolder,
					"NO-NAME");
			itemPairs.add(new Pair<Integer, String>(itemId, itemName));
		}

		itemsData = stringData.getData("Ins.img");
		for (MapleData itemFolder : itemsData.getChildren()) {
			int itemId = Integer.parseInt(itemFolder.getName());
			String itemName = MapleDataTool.getString("name", itemFolder,
					"NO-NAME");
			itemPairs.add(new Pair<Integer, String>(itemId, itemName));
		}

		itemsData = stringData.getData("Pet.img");
		for (MapleData itemFolder : itemsData.getChildren()) {
			int itemId = Integer.parseInt(itemFolder.getName());
			String itemName = MapleDataTool.getString("name", itemFolder,
					"NO-NAME");
			itemPairs.add(new Pair<Integer, String>(itemId, itemName));
		}
		itemNameCache.addAll(itemPairs);
		return itemPairs;
	}

	public int getScriptedItemNpc(int itemId) {
		if (scriptedItemCache.containsKey(itemId)) {
			return scriptedItemCache.get(itemId);
		} else {
			return 0;
		}
	}

	public boolean isExpOrDropCardTime(int itemId) {
		if (getExpCardTimes.containsKey(itemId)) {
			return getExpCardTimes.get(itemId).HasRate();
		}
		return false;
	}

	public static class MapleDayInt {

		public static String getDayInt(int day) {
			if (day == 1) {
				return "SUN";
			} else if (day == 2) {
				return "MON";
			} else if (day == 3) {
				return "TUE";
			} else if (day == 4) {
				return "WED";
			} else if (day == 5) {
				return "THU";
			} else if (day == 6) {
				return "FRI";
			} else if (day == 7) {
				return "SAT";
			}
			return null;
		}
	}

	protected MapleData getStringData(int itemId) {
		String cat = "null";
		MapleData theData;
		if (itemId >= 5010000) {
			theData = cashStringData;
		} else if (itemId >= 2000000 && itemId < 3000000) {
			theData = consumeStringData;
		} else if (itemId >= 1010000 && itemId < 1040000 || itemId >= 1122000
				&& itemId < 1153000) {
			// 添加护肩
			theData = eqpStringData;
			cat = "Accessory";
		} else if (itemId >= 1000000 && itemId < 1010000) {
			theData = eqpStringData;
			cat = "Cap";
		} else if (itemId >= 1102000 && itemId < 1103000) {
			theData = eqpStringData;
			cat = "Cape";
		} else if (itemId >= 1040000 && itemId < 1050000) {
			theData = eqpStringData;
			cat = "Coat";
		} else if (itemId >= 20000 && itemId < 22000) {
			theData = eqpStringData;
			cat = "Face";
		} else if (itemId >= 1080000 && itemId < 1090000) {
			theData = eqpStringData;
			cat = "Glove";
		} else if (itemId >= 30000 && itemId < 32000) {
			theData = eqpStringData;
			cat = "Hair";
		} else if (itemId >= 1050000 && itemId < 1060000) {
			theData = eqpStringData;
			cat = "Longcoat";
		} else if (itemId >= 1060000 && itemId < 1070000) {
			theData = eqpStringData;
			cat = "Pants";
		} else if (itemId >= 1802000 && itemId < 1803000 || itemId >= 1812000
				&& itemId < 1813000 || itemId == 1822000 || itemId == 1832000) {
			theData = eqpStringData;
			cat = "PetEquip";
		} else if (itemId >= 1112000 && itemId < 1120000) {
			theData = eqpStringData;
			cat = "Ring";
		} else if (itemId >= 1092000 && itemId < 1100000) {
			theData = eqpStringData;
			cat = "Shield";
		} else if (itemId >= 1070000 && itemId < 1080000) {
			theData = eqpStringData;
			cat = "Shoes";
		} else if (itemId >= 1900000 && itemId < 1942000) {
			theData = eqpStringData;
			cat = "Taming";
		} else if (itemId >= 1942000 && itemId < 1982000) { // 龙神装备
			theData = eqpStringData;
			cat = "Dragon";
		} else if (itemId >= 1612000 && itemId < 1662000) { // 机械师装备
			theData = eqpStringData;
			cat = "Mechanic";
		} else if (itemId >= 1662000 && itemId < 1682000) { // 智能机械人装备
			theData = eqpStringData;
			cat = "Android";
		} else if (itemId >= 1210000 && itemId < 1800000) {
			theData = eqpStringData;
			cat = "Weapon";
		} else if (itemId >= 4000000 && itemId < 5000000) {
			theData = etcStringData;
		} else if (itemId >= 3000000 && itemId < 4000000) {
			theData = insStringData;
		} else if (itemId >= 5000000 && itemId < 5010000) {
			theData = petStringData;
		} else {
			return null;
		}
		if (cat.matches("null")) {
			if (theData != etcStringData || itemId == 4280000
					|| itemId == 4280001) {
				return theData.getChildByPath(String.valueOf(itemId));
			} else {
				return theData.getChildByPath("Etc/" + String.valueOf(itemId));
			}
		} else {
			if (theData == eqpStringData) {
				return theData.getChildByPath("Eqp/" + cat + "/" + itemId);
			} else {
				return theData.getChildByPath(cat + "/" + itemId);
			}
		}
	}

	protected MapleData getItemData(int itemId) {
		MapleData ret = null;
		String idStr = "0" + String.valueOf(itemId);
		MapleDataDirectoryEntry root = itemData.getRoot(); // 整个item.wz文件夹里的数据
		for (MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
			// we should have .img files here beginning with the first 4 IID
			for (MapleDataFileEntry iFile : topDir.getFiles()) {
				if (iFile.getName().equals(idStr.substring(0, 4) + ".img")) {
					ret = itemData.getData(topDir.getName() + "/"
							+ iFile.getName());
					if (ret == null) {
						return null;
					}
					ret = ret.getChildByPath(idStr);
					return ret;
				} else if (iFile.getName().equals(idStr.substring(1) + ".img")) {
					return itemData.getData(topDir.getName() + "/"
							+ iFile.getName());
				}
			}
		}
		root = equipData.getRoot();
		for (MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
			for (MapleDataFileEntry iFile : topDir.getFiles()) {
				if (iFile.getName().equals(idStr + ".img")) {
					return equipData.getData(topDir.getName() + "/"
							+ iFile.getName());
				}
			}
		}
		return ret;
	}

	public short getSlotMax(MapleClient c, int itemId) {
		short value;
		if (slotMaxCache.containsKey(itemId)) {
			value = slotMaxCache.get(itemId);
		} else {
			if (getInventoryType(itemId).getType() == MapleInventoryType.EQUIP
					.getType()) {
				value = 1;
			} else {
				value = 100;
			}
			if (!isThrowingStar(itemId) && !isBullet(itemId)) {
				slotMaxCache.put(itemId, value);
			}
		}
		return value;
	}

	public boolean isThrowingStar(int itemId) {
		// 是标
		return (itemId >= 2070000 && itemId < 2080000);
	}

	public int getMeso(int itemId) {
		if (getMesoCache.containsKey(itemId)) {
			return getMesoCache.get(itemId);
		} else {
			return -1;
		}
	}

	public int getWholePrice(int itemId) {
		if (wholePriceCache.containsKey(itemId)) {
			return wholePriceCache.get(itemId);
		} else {
			return -1;
		}
	}

	public String getType(int itemId) {
		if (itemTypeCache.containsKey(itemId)) {
			return itemTypeCache.get(itemId);
		} else {
			return "";
		}
	}

	public double getPrice(int itemId) {
		if (priceCache.containsKey(itemId)) {
			return priceCache.get(itemId);
		} else {
			return -1;
		}
	}

	protected Map<String, Integer> getEquipStats(int itemId) {
		if (equipStatsCache.containsKey(itemId)) {
			return equipStatsCache.get(itemId);
		} else {
			Session session = DatabaseConnection.getSession();
			// MapleEquipStats stats = (MapleEquipStats)
			// session.createQuery("from MapleEquipStats as cs where cs.itemid =:id").setParameter("id",
			// itemId).setMaxResults(1).uniqueResult();
			MapleEquipStats stats = (MapleEquipStats) session.get(
					MapleEquipStats.class, itemId);
			session.close();
			equipStatsCache.put(itemId, stats != null ? stats.getBaseStats()
					: null);
			return stats != null ? stats.getBaseStats() : null;
		}
		/*
		 * Map<String, Integer> ret = new LinkedHashMap<String, Integer>();
		 * MapleData item = getItemData(itemId); if (item == null) { return
		 * null; } MapleData info = item.getChildByPath("info"); if (info ==
		 * null) { return null; } for (MapleData data : info.getChildren()) { if
		 * (data.getName().startsWith("inc")) {
		 * ret.put(data.getName().substring(3),
		 * MapleDataTool.getIntConvert(data)); } } ret.put("tuc",
		 * MapleDataTool.getInt("tuc", info, 0)); ret.put("reqLevel",
		 * MapleDataTool.getInt("reqLevel", info, 0)); ret.put("reqJob",
		 * MapleDataTool.getInt("reqJob", info, 0)); ret.put("reqSTR",
		 * MapleDataTool.getInt("reqSTR", info, 0)); ret.put("reqDEX",
		 * MapleDataTool.getInt("reqDEX", info, 0)); ret.put("reqINT",
		 * MapleDataTool.getInt("reqINT", info, 0)); ret.put("reqLUK",
		 * MapleDataTool.getInt("reqLUK", info, 0)); ret.put("cash",
		 * MapleDataTool.getInt("cash", info, 0)); ret.put("cursed",
		 * MapleDataTool.getInt("cursed", info, 0)); ret.put("success",
		 * MapleDataTool.getInt("success", info, 0)); ret.put("durability",
		 * MapleDataTool.getInt("durability", info, -1)); //耐久度 if
		 * (info.getChildByPath("level") != null) { ret.put("skilllevel", 1); }
		 * else { ret.put("skilllevel", 0); } equipStatsCache.put(itemId, ret);
		 * return ret;
		 */
	}

	public int getReqLevel(int itemId) {
		final Integer req = getEquipStats(itemId).get("reqLevel");
		return req == null ? 0 : req.intValue();
	}

	public int getReqJob(int itemId) {
		final Integer req = getEquipStats(itemId).get("reqJob");
		return req == null ? 0 : req.intValue();
	}

	public int getReqStr(int itemId) {
		final Integer req = getEquipStats(itemId).get("reqSTR");
		return req == null ? 0 : req.intValue();
	}

	public int getReqDex(int itemId) {
		final Integer req = getEquipStats(itemId).get("reqDEX");
		return req == null ? 0 : req.intValue();
	}

	public int getReqInt(int itemId) {
		final Integer req = getEquipStats(itemId).get("reqINT");
		return req == null ? 0 : req.intValue();
	}

	public int getReqLuk(int itemId) {
		final Integer req = getEquipStats(itemId).get("reqLUK");
		return req == null ? 0 : req.intValue();
	}

	public boolean isCash(int itemId) {
		try {
			Map<String, Integer> stats = getEquipStats(itemId);
			if (stats == null) {
				return false;
			}
			final Integer req = stats.get("cash");
			if (req == null || req == 0) {
				return false;
			}
			return true;
		} catch (Exception e) {
			log.info("{}", e);
			return false;
		}
	}

	public List<Integer> getScrollReqs(int itemId) {
		if (scrollRestrictionCache.containsKey(itemId)) {
			return scrollRestrictionCache.get(itemId);
		}
		List<Integer> ret = new ArrayList<Integer>();
		MapleData data = getItemData(itemId);
		data = data.getChildByPath("req");
		if (data == null) {
			return ret;
		}
		for (MapleData req : data.getChildren()) {
			ret.add(MapleDataTool.getInt(req));
		}
		return ret;
	}

	public List<SummonEntry> getSummonMobs(int itemId) {
		if (this.summonEntryCache.containsKey(itemId)) {
			return summonEntryCache.get(itemId);
		}
		MapleData data = getItemData(itemId);
		int mobSize = data.getChildByPath("mob").getChildren().size();
		List<SummonEntry> ret = new LinkedList<SummonEntry>();
		for (int x = 0; x < mobSize; x++) {
			ret.add(new SummonEntry(MapleDataTool.getIntConvert("mob/" + x
					+ "/id", data), MapleDataTool.getIntConvert("mob/" + x
					+ "/prob", data)));
		}
		if (ret.isEmpty()) {
			log.warn("Empty summon bag, itemID: " + itemId);
		}
		summonEntryCache.put(itemId, ret);
		return ret;
	}

	public boolean isWeapon(int itemId) {
		return itemId >= 1302000 && itemId < 1492024;
	}

	public MapleWeaponType getWeaponType(int itemId) {
		int cat = itemId / 10000;
		cat = cat % 100;
		switch (cat) {
		case 30:
			return MapleWeaponType.SWORD1H;
		case 31:
			return MapleWeaponType.AXE1H;
		case 32:
			return MapleWeaponType.BLUNT1H;
		case 33:
			return MapleWeaponType.DAGGER;
		case 37:
			return MapleWeaponType.WAND;
		case 38:
			return MapleWeaponType.STAFF;
		case 40:
			return MapleWeaponType.SWORD2H;
		case 41:
			return MapleWeaponType.AXE2H;
		case 42:
			return MapleWeaponType.BLUNT2H;
		case 43:
			return MapleWeaponType.SPEAR;
		case 44:
			return MapleWeaponType.POLE_ARM;
		case 45:
			return MapleWeaponType.BOW;
		case 46:
			return MapleWeaponType.CROSSBOW;
		case 47:
			return MapleWeaponType.CLAW;
		case 39: // Barefists
		case 48:
			return MapleWeaponType.KNUCKLE;
		case 49:
			return MapleWeaponType.GUN;
		case 52:
			return MapleWeaponType.双弩枪;
		case 53:
			return MapleWeaponType.CANNON;
		case 24:
			return MapleWeaponType.能量剑;
		case 22:
			return MapleWeaponType.通用武器;
		}
		return MapleWeaponType.NOT_A_WEAPON;
	}

	public boolean isShield(int itemId) {
		int cat = itemId / 10000;
		cat = cat % 100;
		return cat == 9;
	}

	public boolean isEquip(int itemId) {
		return itemId / 1000000 == 1;
	}

	public boolean isCleanSlate(int scrollId) {
		switch (scrollId) {
		case 2049000:
		case 2049001:
		case 2049002:
		case 2049003:
			return true;
		}
		return false;
	}

	public boolean 附加卷轴(int scrollId) {
		switch (scrollId) {
		case 2049400:
		case 2049401:
		case 2049402:
		case 2049404:
			return true;
		}
		return false;
	}

	public boolean 强化卷轴(int scrollId) {
		switch (scrollId) {
		case 2049301:
		case 2049300:
		case 2049303:
		case 2049304:
		case 2049305:
			return true;
		}
		return false;
	}

	public boolean 不需要判断部位的卷轴(int scrollId) {
		switch (scrollId) {
		// 白医
		case 2049000:
		case 2049001:
		case 2049002:
		case 2049003:
		case 2049004: // 仙
		case 2049005: // 神
			// 混沌
		case 2049100:
		case 2049116: // 强化
		case 2049117:
			return true;
		}
		return false;
	}

	public Equip MakeItem(Equip equip, int scrollId) {
		if (equip instanceof Equip) {
			Equip nEquip = equip;
			Map<String, Integer> stats = this.getEquipStats(scrollId);
			for (Entry<String, Integer> stat : stats.entrySet()) {
				if (stat.getKey().equals("STR")) {
					nEquip.setStr((short) (nEquip.getStr() + stat.getValue()
							.intValue()));
				} else if (stat.getKey().equals("DEX")) {
					nEquip.setDex((short) (nEquip.getDex() + stat.getValue()
							.intValue()));
				} else if (stat.getKey().equals("INT")) {
					nEquip.setInt((short) (nEquip.getInt() + stat.getValue()
							.intValue()));
				} else if (stat.getKey().equals("LUK")) {
					nEquip.setLuk((short) (nEquip.getLuk() + stat.getValue()
							.intValue()));
				} else if (stat.getKey().equals("PAD")) {
					nEquip.setWatk((short) (nEquip.getWatk() + stat.getValue()
							.intValue()));
				} else if (stat.getKey().equals("PDD")) {
					nEquip.setWdef((short) (nEquip.getWdef() + stat.getValue()
							.intValue()));
				} else if (stat.getKey().equals("MAD")) {
					nEquip.setMatk((short) (nEquip.getMatk() + stat.getValue()
							.intValue()));
				} else if (stat.getKey().equals("MDD")) {
					nEquip.setMdef((short) (nEquip.getMdef() + stat.getValue()
							.intValue()));
				} else if (stat.getKey().equals("ACC")) {
					nEquip.setAcc((short) (nEquip.getAcc() + stat.getValue()
							.intValue()));
				} else if (stat.getKey().equals("EVA")) {
					nEquip.setAvoid((short) (nEquip.getAvoid() + stat
							.getValue().intValue()));
				} else if (stat.getKey().equals("Speed")) {
					nEquip.setSpeed((short) (nEquip.getSpeed() + stat
							.getValue().intValue()));
				} else if (stat.getKey().equals("Jump")) {
					nEquip.setJump((short) (nEquip.getJump() + stat.getValue()
							.intValue()));
				} else if (stat.getKey().equals("MHP")) {
					nEquip.setHp((short) (nEquip.getHp() + stat.getValue()
							.intValue()));
				} else if (stat.getKey().equals("MMP")) {
					nEquip.setMp((short) (nEquip.getMp() + stat.getValue()
							.intValue()));
				}
			}
		} else {
		}
		return equip;
	}

	/**
	 * 砸卷处理。
	 *
	 * @param equip
	 * @param scrollId
	 * @param usingWhiteScroll
	 * @param checkIfGM
	 * @return
	 */
	public IItem scrollEquipWithId(IItem equip, int scrollId,
			boolean usingWhiteScroll, boolean checkIfGM) {
		if (equip instanceof Equip) {
			Equip nEquip = (Equip) equip;
			Map<String, Integer> stats = this.getEquipStats(scrollId);
			Map<String, Integer> eqstats = this
					.getEquipStats(equip.getItemId());
			if ((nEquip.getUpgradeSlots() > 0 || isCleanSlate(scrollId))
					&& Math.ceil(Math.random() * 100.0) <= stats.get("success")
					|| (checkIfGM == true)) {
				switch (scrollId) {
				case 2040727:
					nEquip.AddFlag(InventoryConstants.Items.Flags.SPIKES);
					return equip;
				case 2041058:
					nEquip.AddFlag(InventoryConstants.Items.Flags.COLD);
					return equip;
				case 2049000:
				case 2049001:
				case 2049002:
				case 2049003:
					if (nEquip.getLevel() + nEquip.getUpgradeSlots() < eqstats
							.get("tuc")) {
						byte newSlots = (byte) (nEquip.getUpgradeSlots() + 1);
						nEquip.setUpgradeSlots(newSlots);
					}
					break;
				case 2049100: // 混沌卷轴 60%
				case 2049101: // 骗子树液 100%
				case 2049102: // 冒险岛糖浆 100%
				case 2049103: // 海蓝拖鞋专用卷轴 100%
				case 2049104: // 工作人员装备专用卷轴 100%
				case 2049112: // 企鹅国王的武器卷轴 100%
				case 2049115: // 拉瓦那
				case 2049116: // 强化混沌
				case 2049117: // 混沌卷轴 60%
					int increase = 1;
					if (Math.ceil(Math.random() * 100.0) <= 50) {
						increase = increase * -1;
					}
					if (nEquip.getStr() > 0) {
						short newStat = (short) (nEquip.getStr() + Math
								.ceil(Math.random() * 5.0) * increase);
						nEquip.setStr(newStat);
					}
					if (nEquip.getDex() > 0) {
						short newStat = (short) (nEquip.getDex() + Math
								.ceil(Math.random() * 5.0) * increase);
						nEquip.setDex(newStat);
					}
					if (nEquip.getInt() > 0) {
						short newStat = (short) (nEquip.getInt() + Math
								.ceil(Math.random() * 5.0) * increase);
						nEquip.setInt(newStat);
					}
					if (nEquip.getLuk() > 0) {
						short newStat = (short) (nEquip.getLuk() + Math
								.ceil(Math.random() * 5.0) * increase);
						nEquip.setLuk(newStat);
					}
					if (nEquip.getWatk() > 0) {
						short newStat = (short) (nEquip.getWatk() + Math
								.ceil(Math.random() * 5.0) * increase);
						nEquip.setWatk(newStat);
					}
					if (nEquip.getWdef() > 0) {
						short newStat = (short) (nEquip.getWdef() + Math
								.ceil(Math.random() * 5.0) * increase);
						nEquip.setWdef(newStat);
					}
					if (nEquip.getMatk() > 0) {
						short newStat = (short) (nEquip.getMatk() + Math
								.ceil(Math.random() * 5.0) * increase);
						nEquip.setMatk(newStat);
					}
					if (nEquip.getMdef() > 0) {
						short newStat = (short) (nEquip.getMdef() + Math
								.ceil(Math.random() * 5.0) * increase);
						nEquip.setMdef(newStat);
					}
					if (nEquip.getAcc() > 0) {
						short newStat = (short) (nEquip.getAcc() + Math
								.ceil(Math.random() * 5.0) * increase);
						nEquip.setAcc(newStat);
					}
					if (nEquip.getAvoid() > 0) {
						short newStat = (short) (nEquip.getAvoid() + Math
								.ceil(Math.random() * 5.0) * increase);
						nEquip.setAvoid(newStat);
					}
					if (nEquip.getSpeed() > 0) {
						short newStat = (short) (nEquip.getSpeed() + Math
								.ceil(Math.random() * 5.0) * increase);
						nEquip.setSpeed(newStat);
					}
					if (nEquip.getJump() > 0) {
						short newStat = (short) (nEquip.getJump() + Math
								.ceil(Math.random() * 5.0) * increase);
						nEquip.setJump(newStat);
					}
					if (nEquip.getHp() > 0) {
						short newStat = (short) (nEquip.getHp() + Math
								.ceil(Math.random() * 5.0) * increase);
						nEquip.setHp(newStat);
					}
					if (nEquip.getMp() > 0) {
						short newStat = (short) (nEquip.getMp() + Math
								.ceil(Math.random() * 5.0) * increase);
						nEquip.setMp(newStat);
					}
					break;
				default:
					for (Entry<String, Integer> stat : stats.entrySet()) {
						if (stat.getKey().equals("STR")) {
							nEquip.setStr((short) (nEquip.getStr() + stat
									.getValue().intValue()));
						} else if (stat.getKey().equals("DEX")) {
							nEquip.setDex((short) (nEquip.getDex() + stat
									.getValue().intValue()));
						} else if (stat.getKey().equals("INT")) {
							nEquip.setInt((short) (nEquip.getInt() + stat
									.getValue().intValue()));
						} else if (stat.getKey().equals("LUK")) {
							nEquip.setLuk((short) (nEquip.getLuk() + stat
									.getValue().intValue()));
						} else if (stat.getKey().equals("PAD")) {
							nEquip.setWatk((short) (nEquip.getWatk() + stat
									.getValue().intValue()));
						} else if (stat.getKey().equals("PDD")) {
							nEquip.setWdef((short) (nEquip.getWdef() + stat
									.getValue().intValue()));
						} else if (stat.getKey().equals("MAD")) {
							nEquip.setMatk((short) (nEquip.getMatk() + stat
									.getValue().intValue()));
						} else if (stat.getKey().equals("MDD")) {
							nEquip.setMdef((short) (nEquip.getMdef() + stat
									.getValue().intValue()));
						} else if (stat.getKey().equals("ACC")) {
							nEquip.setAcc((short) (nEquip.getAcc() + stat
									.getValue().intValue()));
						} else if (stat.getKey().equals("EVA")) {
							nEquip.setAvoid((short) (nEquip.getAvoid() + stat
									.getValue().intValue()));
						} else if (stat.getKey().equals("Speed")) {
							nEquip.setSpeed((short) (nEquip.getSpeed() + stat
									.getValue().intValue()));
						} else if (stat.getKey().equals("Jump")) {
							nEquip.setJump((short) (nEquip.getJump() + stat
									.getValue().intValue()));
						} else if (stat.getKey().equals("MHP")) {
							nEquip.setHp((short) (nEquip.getHp() + stat
									.getValue().intValue()));
						} else if (stat.getKey().equals("MMP")) {
							nEquip.setMp((short) (nEquip.getMp() + stat
									.getValue().intValue()));
						} else if (stat.getKey().equals("afterImage")) {
						}
					}
					break;
				}
				if (!isCleanSlate(scrollId)) {
					nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() - 1));
					nEquip.setLevel((byte) (nEquip.getLevel() + 1));
				}
			} else {
				if (!usingWhiteScroll && !isCleanSlate(scrollId)) {
					nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() - 1));
				}
				if (Math.ceil(1.0 + Math.random() * 100.0) < stats
						.get("cursed")) {
					if (!nEquip.HasFlag(InventoryConstants.Items.Flags.防爆卷轴)) {
						return null;
					}
				}
			}
		}
		if (equip.HasFlag(InventoryConstants.Items.Flags.防爆卷轴)) {
			equip.CanceFlag(InventoryConstants.Items.Flags.防爆卷轴);
		}
		return equip;
	}

	public IItem getEquipById(int equipId) {
		Equip nEquip;
		nEquip = new Equip(equipId, (byte) 0);
		nEquip.setQuantity((short) 1);
		Map<String, Integer> stats = this.getEquipStats(equipId);
		if (stats != null) {
			for (Entry<String, Integer> stat : stats.entrySet()) {
				if (stat.getKey().equals("STR")) {
					nEquip.setStr((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("DEX")) {
					nEquip.setDex((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("INT")) {
					nEquip.setInt((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("LUK")) {
					nEquip.setLuk((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("PAD")) {
					nEquip.setWatk((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("PDD")) {
					nEquip.setWdef((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("MAD")) {
					nEquip.setMatk((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("MDD")) {
					nEquip.setMdef((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("ACC")) {
					nEquip.setAcc((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("EVA")) {
					nEquip.setAvoid((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("Speed")) {
					nEquip.setSpeed((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("Jump")) {
					nEquip.setJump((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("MHP")) {
					nEquip.setHp((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("MMP")) {
					nEquip.setMp((short) stat.getValue().intValue());
				} else if (stat.getKey().equals("tuc")) {
					nEquip.setUpgradeSlots((byte) stat.getValue().intValue());
				} else if (isDropRestricted(equipId)) { // 设置flag节点
					nEquip.AddFlag(InventoryConstants.Items.Flags.UNTRADEABLE);// 不可交易
					// } else if (stat.getKey().equals("afterImage")) {
				} else if (stat.getKey().equals("skilllevel")) {
					nEquip.setItemLevel((byte) stat.getValue().intValue());
				} else if (stat.getKey().equals("durability")) {
					nEquip.setDurability(stat.getValue().intValue());
				}
			}
		}
		equipCache.put(equipId, nEquip);
		return nEquip.copy();
	}

	private short getRandStat(short defaultValue, int maxRange) {
		if (defaultValue == 0) {
			return 0;
		}

		// vary no more than ceil of 10% of stat
		int lMaxRange = (int) Math.min(Math.ceil(defaultValue * 0.1), maxRange);
		return (short) ((defaultValue - lMaxRange) + Math.floor(Math.random()
				* (lMaxRange * 2 + 1)));
	}

	public Equip randomizeStats(Equip equip) {
		equip.setStr(getRandStat(equip.getStr(), 5));
		equip.setDex(getRandStat(equip.getDex(), 5));
		equip.setInt(getRandStat(equip.getInt(), 5));
		equip.setLuk(getRandStat(equip.getLuk(), 5));
		equip.setMatk(getRandStat(equip.getMatk(), 5));
		equip.setWatk(getRandStat(equip.getWatk(), 5));
		equip.setAcc(getRandStat(equip.getAcc(), 5));
		equip.setAvoid(getRandStat(equip.getAvoid(), 5));
		equip.setJump(getRandStat(equip.getJump(), 5));
		equip.setSpeed(getRandStat(equip.getSpeed(), 5));
		equip.setWdef(getRandStat(equip.getWdef(), 10));
		equip.setMdef(getRandStat(equip.getMdef(), 10));
		equip.setHp(getRandStat(equip.getHp(), 10));
		equip.setMp(getRandStat(equip.getMp(), 10));
		int i = (int) (Math.floor(Math.random() * 1000) + 20);
		if (i < 20) { // 有20%几率打怪掉落未鉴定装备
			equip.setIdentify((byte) 1);
		}
		return equip;
	}

	public MapleStatEffect getItemEffect(int itemId) {
		MapleStatEffect ret = itemEffects.get(Integer.valueOf(itemId));
		if (ret == null) {
			MapleData item = getItemData(itemId);
			if (item == null) {
				return null;
			}
			MapleData spec = item.getChildByPath("spec");
			if (spec == null) {
				return null;
			}
			ret = MapleStatEffect.loadItemEffectFromData(spec, itemId);
			itemEffects.put(Integer.valueOf(itemId), ret);
		}
		return ret;
	}

	public boolean isBullet(int itemId) {
		int id = itemId / 10000;
		if (id == 233) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isRechargable(int itemId) {
		int id = itemId / 10000;
		if (id == 233 || id == 207) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isOverall(int itemId) {
		return itemId >= 1050000 && itemId < 1060000;
	}

	public boolean isPet(int itemId) {
		return GameConstants.isPet(itemId);
	}

	public boolean isArrowForCrossBow(int itemId) {
		// 弩箭
		return itemId >= 2061000 && itemId < 2062000;
	}

	public boolean isArrowForBow(int itemId) {
		// 弓箭
		return itemId >= 2060000 && itemId < 2061000;
	}

	public boolean isTwoHanded(int itemId) {
		switch (getWeaponType(itemId)) {
		case AXE2H:
		case BLUNT2H:
		case BOW:
		case CLAW:
		case CROSSBOW:
		case POLE_ARM:
		case SPEAR:
			// case SWORD2H:
		case GUN:
		case KNUCKLE:
			return true;
		default:
			return false;
		}
	}

	public boolean isTownScroll(int itemId) {
		return (itemId >= 2030000 && itemId < 2030020);
	}

	public boolean isGun(int itemId) {
		return itemId >= 1492000 && itemId <= 1492024;
	}

	public boolean isWritOfSolomon(int itemId) {
		return (itemId >= 2370000 && itemId <= 2370012);
	}

	public int getExpCache(int itemId) {
		if (this.getExpCache.containsKey(itemId)) {
			return getExpCache.get(itemId);
		}
		MapleData item = getItemData(itemId);
		if (item == null) {
			return 0;
		}
		int pEntry = 0;
		MapleData pData = item.getChildByPath("spec/exp");
		if (pData == null) {
			return 0;
		}
		pEntry = MapleDataTool.getInt(pData);

		getExpCache.put(itemId, pEntry);
		return pEntry;
	}

	public int getWatkForProjectile(int itemId) {
		Integer atk = projectileWatkCache.get(itemId);
		if (atk != null) {
			return atk.intValue();
		}
		MapleData data = getItemData(itemId);
		atk = Integer.valueOf(MapleDataTool.getInt("info/incPAD", data, 0));
		projectileWatkCache.put(itemId, atk);
		return atk.intValue();
	}

	public boolean canScroll(int scrollid, int itemid) {
		int scrollCategoryQualifier = (scrollid / 100) % 100;
		int itemCategoryQualifier = (itemid / 10000) % 100;
		return scrollCategoryQualifier == itemCategoryQualifier;
	}

	public String getName(int itemId) {
		if (nameCache.containsKey(itemId)) {
			return nameCache.get(itemId);
		}
		Session session = DatabaseConnection.getSession();
		MapleItemName name = (MapleItemName) session.get(MapleItemName.class,
				itemId);
		session.close();
		if (name == null) {
			return null;
		}
		nameCache.put(itemId, name.getName());
		return name.getName();

		/*
		 * MapleData strings = getStringData(itemId); if (strings == null) {
		 * return null; } String ret = MapleDataTool.getString("name", strings,
		 * null); nameCache.put(itemId, ret); return ret;
		 */
	}

	public String getDesc(int itemId) {
		if (descCache.containsKey(itemId)) {
			return descCache.get(itemId);
		}
		MapleData strings = getStringData(itemId);
		if (strings == null) {
			return null;
		}
		String ret = MapleDataTool.getString("desc", strings, null);
		descCache.put(itemId, ret);
		return ret;
	}

	public String getMsg(int itemId) {
		if (msgCache.containsKey(itemId)) {
			return msgCache.get(itemId);
		}
		MapleData strings = getStringData(itemId);
		if (strings == null) {
			return null;
		}
		String ret = MapleDataTool.getString("msg", strings, null);
		msgCache.put(itemId, ret);
		return ret;
	}

	// 设置装备的flag
	public boolean isDropRestricted(int itemId) {
		if (dropRestrictionCache.containsKey(itemId)) {
			return dropRestrictionCache.get(itemId);
		}

		MapleData data = getItemData(itemId);
		if (data == null) {
			return false;
		}
		boolean bRestricted = MapleDataTool.getIntConvert("info/tradeBlock",
				data, 0) == 1;
		if (!bRestricted) {
			bRestricted = MapleDataTool.getIntConvert("info/quest", data, 0) == 1;
		}
		if (!bRestricted) {
			bRestricted = MapleDataTool.getIntConvert("info/equipTradeBlock",
					data, 0) == 1; // 永恒装备
		}
		dropRestrictionCache.put(itemId, bRestricted);

		return bRestricted;
	}

	/**
	 * 丢弃限制。
	 *
	 * @param item
	 * @return
	 */
	public boolean isDropRestricted(IItem item) {
		return isDropRestricted(item.getItemId())
				| item.HasFlag(InventoryConstants.Items.Flags.UNTRADEABLE);
	}

	public boolean isPickupRestricted(int itemId) {
		if (pickupRestrictionCache.containsKey(itemId)) {
			return pickupRestrictionCache.get(itemId);
		}
		MapleData data = getItemData(itemId);
		boolean bRestricted = MapleDataTool.getIntConvert("info/only", data, 0) == 1;

		pickupRestrictionCache.put(itemId, bRestricted);
		return bRestricted;
	}

	public Map<String, Integer> getSkillStats(int itemId, double playerJob) {
		Map<String, Integer> ret = new LinkedHashMap<String, Integer>();
		MapleData item = getItemData(itemId);
		if (item == null) {
			return null;
		}
		MapleData info = item.getChildByPath("info");
		if (info == null) {
			return null;
		}
		for (MapleData data : info.getChildren()) {
			if (data.getName().startsWith("inc")) {
				ret.put(data.getName().substring(3),
						MapleDataTool.getIntConvert(data));
			}
		}
		ret.put("masterLevel", MapleDataTool.getInt("masterLevel", info, 0));
		ret.put("reqSkillLevel", MapleDataTool.getInt("reqSkillLevel", info, 0));
		ret.put("success", MapleDataTool.getInt("success", info, 0));

		MapleData skill = info.getChildByPath("skill");
		int curskill = 1;
		int size = skill.getChildren().size();
		for (int i = 0; i < size; i++) {
			curskill = MapleDataTool.getInt(Integer.toString(i), skill, 0);
			if (curskill == 0) // end - no more;
			{
				break;
			}
			double skillJob = Math.floor(curskill / 10000);
			if (skillJob == playerJob) {
				ret.put("skillid", curskill);
				break;
			}
		}

		if (ret.get("skillid") == null) {
			ret.put("skillid", 0);
		}
		return ret;
	}

	public List<Integer> petsCanConsume(int itemId) {
		List<Integer> ret = new ArrayList<Integer>();
		MapleData data = getItemData(itemId);
		int curPetId = 0;
		int size = data.getChildren().size();
		for (int i = 0; i < size; i++) {
			curPetId = MapleDataTool.getInt("spec/" + Integer.toString(i),
					data, 0);
			if (curPetId == 0) {
				break;
			}
			ret.add(Integer.valueOf(curPetId));
		}
		return ret;
	}

	public boolean isQuestItem(int itemId) {
		if (isQuestItemCache.containsKey(itemId)) {
			return isQuestItemCache.get(itemId);
		}
		MapleData data = getItemData(itemId);
		boolean questItem = MapleDataTool.getIntConvert("info/quest", data, 0) == 1;
		isQuestItemCache.put(itemId, questItem);
		return questItem;
	}

	public boolean isMiniDungeonMap(int mapId) {
		switch (mapId) {
		case 100020000:
		case 105040304:
		case 105050100:
		case 221023400:
			return true;
		default:
			return false;
		}
	}

	public boolean isDragonItem(int itemId) {
		switch (itemId) {
		case 1372032:
		case 1312031:
		case 1412026:
		case 1302059:
		case 1442045:
		case 1402036:
		case 1432038:
		case 1422028:
		case 1472051:
		case 1472052:
		case 1332049:
		case 1332050:
		case 1322052:
		case 1452044:
		case 1462039:
		case 1382036:
			return true;
		default:
			return false;
		}
	}

	public static class SummonEntry {

		private int chance, mobId;

		public SummonEntry(int a, int b) {
			this.mobId = a;
			this.chance = b;
		}

		public int getChance() {
			return chance;
		}

		public int getMobId() {
			return mobId;
		}
	}

	public boolean isKarmaAble(int itemId) {
		if (karmaCache.containsKey(itemId)) {
			return karmaCache.get(itemId);
		}
		MapleData data = getItemData(itemId);
		boolean bRestricted = MapleDataTool.getIntConvert(
				"info/tradeAvailable", data, 0) > 0;
		karmaCache.put(itemId, bRestricted);
		return bRestricted;
	}

	public boolean isConsumeOnPickup(int itemId) {
		if (consumeOnPickupCache.containsKey(itemId)) {
			return consumeOnPickupCache.get(itemId);
		}

		MapleData data = getItemData(itemId);

		boolean consume = MapleDataTool.getIntConvert("spec/consumeOnPickup",
				data, 0) == 1
				|| MapleDataTool.getIntConvert("specEx/consumeOnPickup", data,
						0) == 1;

		consumeOnPickupCache.put(itemId, consume);
		return consume;
	}

	private void loadCardIdData() {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = DatabaseConnection.getConnection().prepareStatement(
					"SELECT cardid, mobid FROM monstercarddata");
			rs = ps.executeQuery();
			while (rs.next()) {
				monsterBookID.put(rs.getInt(1), rs.getInt(2));
			}
			ps.getConnection().close();
			ps.close();
		} catch (SQLException e) {
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (ps != null) {
					ps.close();
				}
			} catch (SQLException e) {
			}
		}
	}

	public int getCardMobId(int id) {
		return monsterBookID.get(id);
	}

	public List<MapleFish> getFishReward(int itemId) {
		if (fishingCache.containsKey(itemId)) {
			return fishingCache.get(itemId);
		} else {
			List<MapleFish> rewards = new ArrayList<MapleFish>();
			MapleData data = getItemData(itemId);
			MapleData rewardData = data.getChildByPath("reward");
			for (MapleData child : rewardData.getChildren()) {
				int rewardItem = MapleDataTool.getInt("item", child, 0);
				int prob = MapleDataTool.getInt("prob", child, 0);
				int count = MapleDataTool.getInt("count", child, 0);
				String effect = MapleDataTool.getString("Effect", child, "");
				rewards.add(new MapleFish(rewardItem, prob, count, effect));
			}
			fishingCache.put(itemId, rewards);
			return rewards;
		}
	}

	public List<Pair<String, Integer>> getItemLevelupStats(int itemId,
			int level, boolean timeless) {
		// timeless 永恒
		List<Pair<String, Integer>> list = new LinkedList<Pair<String, Integer>>();
		MapleData data = getItemData(itemId); // 获得该物品所有节点
		MapleData data1 = data.getChildByPath("info").getChildByPath("level");
		/*
		 * if ((timeless && level == 5) || (!timeless && level == 3)) {
		 * MapleData skilldata =
		 * data1.getChildByPath("case").getChildByPath("1")
		 * .getChildByPath(timeless ? "6" : "4"); if (skilldata != null) { int
		 * skillid; List<MapleData> skills =
		 * skilldata.getChildByPath("Skill").getChildren(); for (int i = 0; i <
		 * skills.size(); i++) { skillid =
		 * MapleDataTool.getInt(skills.get(i).getChildByPath("id"));
		 * log.debug(skillid); if (Math.random() < 0.1) list.add(new
		 * Pair<String, Integer>("Skill" + i, skillid)); } } }
		 */
		if (data1 != null) { // 判断装备是否存在level节点
			MapleData data2 = data1.getChildByPath("info").getChildByPath(
					Integer.toString(level)); // 获取与装备的[道具等级]相应的节点
			if (data2 != null) {
				for (MapleData da : data2.getChildren()) {
					if (Math.random() < 0.9) {
						if (da.getName().startsWith("incDEXMin")) {
							list.add(new Pair<String, Integer>("incDEX", rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incDEXMax")))));
						} else if (da.getName().startsWith("incSTRMin")) {
							list.add(new Pair<String, Integer>("incSTR", rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incSTRMax")))));
						} else if (da.getName().startsWith("incINTMin")) {
							list.add(new Pair<String, Integer>("incINT", rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incINTMax")))));
						} else if (da.getName().startsWith("incLUKMin")) {
							list.add(new Pair<String, Integer>("incLUK", rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incLUKMax")))));
						} else if (da.getName().startsWith("incMHPMin")) {
							list.add(new Pair<String, Integer>("incMHP", rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incMHPMax")))));
						} else if (da.getName().startsWith("incMMPMin")) {
							list.add(new Pair<String, Integer>("incMMP", rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incMMPMax")))));
						} else if (da.getName().startsWith("incPADMin")) {
							list.add(new Pair<String, Integer>("incPAD", rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incPADMax")))));
						} else if (da.getName().startsWith("incMADMin")) {
							list.add(new Pair<String, Integer>("incMAD", rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incMADMax")))));
						} else if (da.getName().startsWith("incPDDMin")) {
							list.add(new Pair<String, Integer>("incPDD", rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incPDDMax")))));
						} else if (da.getName().startsWith("incMDDMin")) {
							list.add(new Pair<String, Integer>("incMDD", rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incMDDMax")))));
						} else if (da.getName().startsWith("incACCMin")) {
							list.add(new Pair<String, Integer>("incACC", rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incACCMax")))));
						} else if (da.getName().startsWith("incEVAMin")) {
							list.add(new Pair<String, Integer>("incEVA", rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incEVAMax")))));
						} else if (da.getName().startsWith("incSpeedMin")) {
							list.add(new Pair<String, Integer>(
									"incSpeed",
									rand(MapleDataTool.getInt(da),
											MapleDataTool.getInt(data2
													.getChildByPath("incSpeedMax")))));
						} else if (da.getName().startsWith("incJumpMin")) {
							list.add(new Pair<String, Integer>("incJump", rand(
									MapleDataTool.getInt(da),
									MapleDataTool.getInt(data2
											.getChildByPath("incJumpMax")))));
						}
					}
				}
			}
		}
		return list;
	}

	private static int rand(int lbound, int ubound) {
		return (int) ((Math.random() * (ubound - lbound + 1)) + lbound);
	}

	public int getMakeItemMinLevel(int itemId) { // 获取结晶对应的最小等级 锻造用 暂时不用
		if (getMakeItemMinLevel.containsKey(itemId)) {
			return getMakeItemMinLevel.get(itemId);
		}
		MapleData item = getItemData(itemId);
		if (item == null) {
			return -1;
		}
		int pEntry = 0;
		MapleData pData = item.getChildByPath("info/lvMin");
		if (pData == null) {
			return -1;
		}
		pEntry = MapleDataTool.getInt(pData);
		getMakeItemMinLevel.put(itemId, pEntry);
		return pEntry;
	}

	public int getMakeItemMaxLevel(int itemId) { // 获取结晶对应的最大等级 锻造用 暂时不用
		if (getMakeItemMaxLevel.containsKey(itemId)) {
			return getMakeItemMaxLevel.get(itemId);
		}
		MapleData item = getItemData(itemId);
		if (item == null) {
			return -1;
		}
		int pEntry = 0;
		MapleData pData = item.getChildByPath("info/lvMax");
		if (pData == null) {
			return -1;
		}
		pEntry = MapleDataTool.getInt(pData);
		getMakeItemMaxLevel.put(itemId, pEntry);
		return pEntry;
	}

	public int getItemLevel(int itemId) { // 获取物品对应的等级 锻造怪物结晶用到
		if (getItemLevel.containsKey(itemId)) {
			return getItemLevel.get(itemId);
		}
		MapleData item = getItemData(itemId);
		if (item == null) {
			return -1;
		}
		int pEntry = 0;
		MapleData pData = item.getChildByPath("info/lv");
		if (pData == null) {
			return -1;
		}
		pEntry = MapleDataTool.getInt(pData);
		getItemLevel.put(itemId, pEntry);
		return pEntry;
	}

	public int getCrystalId(int itemid) { // 输入扣除的itemid直接获得相应的怪物结晶 锻造用
		int itemid2 = 0; // 遍历Item.wz/Etc/0426.img获得的id
		int itemid4 = 0; // 要输出的itemid
		int itemlv = getItemLevel(itemid);
		if (itemlv > 0) {
			log.debug("物品等级 :" + itemlv);
			int min = 0;
			int max = 0;
			MapleDataProvider dataProvider = MapleDataProviderFactory
					.getDataProvider(new File(System
							.getProperty("net.sf.odinms.wzpath")
							+ "/"
							+ "Item.wz"));
			MapleData a = dataProvider.getData("Etc/0426.img");
			for (MapleData b : a.getChildren()) { // getChildren 子项名 04260000
				itemid2 = Integer.parseInt(b.getName()); // 获取itemid
				if (!getAllCrystalId.contains(itemid2)) // 如果没有储存到静态里 就遍历储存
				{
					getAllCrystalId.add(itemid2); // 储存结晶的itemid
				} // log.debug("遍历0426.img的itemid2: "+itemid2);
				if (!getCrystalId.containsKey(itemid2)) { // 如果没有储存到静态里 就遍历储存
					for (MapleData c : b.getChildren()) { // info
						for (MapleData d : c.getChildren()) { // lvMax lvMin
							if (d.getName().equals("lvMin")) {
								min = MapleDataTool.getInt(d); // 获取lvMin的值
							} else if (d.getName().equals("lvMax")) {
								max = MapleDataTool.getInt(d); // 获取lvMax的值
							}
						}
					}
					getCrystalId.put(itemid2, new Pair<Integer, Integer>(min,
							max));
				}
			}

			for (int itemid3 : getAllCrystalId) { // 把在前面遍历获得的itemid 再遍历回去
													// getAllCrystalId是储存所有结晶itemid的排列
				Pair<Integer, Integer> ret3 = getCrystalId.get(itemid3);// 返回ret
																		// 即获得上面的List<Pair<Integer,
																		// Integer>>
				if (ret3.getLeft() <= itemlv && ret3.getRight() >= itemlv) { // [31,50]
																				// 大小2边都可取等号
					itemid4 = itemid3;
					break;
				}
			}
			log.debug("获取的结晶ID :" + itemid4);
		}
		return itemid4;
	}

	private int getCrystalId2(int itemid) { // 通过扣除的普通怪物掉落物品获取要得到的怪物结晶id[数组遍历]
		MapleItemInformationProvider ii = MapleItemInformationProvider
				.getInstance();
		int[][] a = new int[9][3];
		int b, itemlv = ii.getItemLevel(itemid), i;

		for (i = 0; i < a.length; i++) { // 运行完后i再递增
			b = 4260000 + i;
			a[i][0] = b;
			a[i][1] = ii.getMakeItemMinLevel(b);
			a[i][2] = ii.getMakeItemMaxLevel(b);
		}

		for (i = 0; i < a.length; i++) { // 运行完后i再递增
			if (itemlv >= a[i][1] && itemlv <= a[i][2]) {
				return a[i][0];
			}
		}
		return 0;
	}

	public void setCrystalInSql() { // 遍历WZ把符合锻造结晶的物品加入Sql表里
		int itemid = 0; // 遍历Item.wz/Etc/0400.img获得的id
		MapleDataProvider dataProvider = MapleDataProviderFactory
				.getDataProvider(new File(System
						.getProperty("net.sf.odinms.wzpath") + "/" + "Item.wz"));
		MapleData a = dataProvider.getData("Etc/0400.img");
		List<Integer> ret = new ArrayList<Integer>(); // 储存所有0400.img的itemid
		for (MapleData b : a.getChildren()) { // getChildren 子项名 04000000
			itemid = Integer.parseInt(b.getName()); // 获取itemid
			// log.debug("遍历0400.img的itemid: "+itemid);
			ret.add(itemid);
		}
		int crystalid;
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con
					.prepareStatement("INSERT INTO makertcrystaldata (itemid, crystalid) VALUES (?, ?)");
			for (int ret2 : ret) {
				crystalid = getCrystalId(ret2);
				if (crystalid > 0) {
					ps.setInt(1, ret2);
					ps.setInt(2, crystalid);
					ps.executeUpdate();
				}
			}
			ps.close(); // 循环结束再关表
			con.close();
		} catch (SQLException se) {
			log.error("结晶储存进SQL出错", se);
		}
	}

	public void setMakeItemInfoInSql() { // 遍历WZ把锻造的物品需要的数据加入Sql表里
		MapleDataProvider dataProvider = MapleDataProviderFactory
				.getDataProvider(new File(System
						.getProperty("net.sf.odinms.wzpath") + "/" + "Etc.wz"));
		MapleData a = dataProvider.getData("ItemMake.img");
		Connection con = null;
		PreparedStatement ps = null;
		int i = 1;
		try {
			con = DatabaseConnection.getConnection();
			for (MapleData b : a.getChildren()) { // getChildren 子项名 0 1 2 4 8
													// 16
				int type = Integer.parseInt(b.getName()); // 属于哪个大类
				log.debug("遍历节点: " + type);
				for (MapleData c : b.getChildren()) { // itemid 01032062
					int itemid = Integer.parseInt(c.getName()); // 获取锻造要获得的itemid
					if (Integer.parseInt(c.getName()) == 1072362) // 这货 0_item
																	// 节点有问题
																	// 用HR打开会显示[未将对象引用设置到对象的实例]
					{
						continue;
					}
					log.debug("正在遍历的itemid: " + itemid);
					log.debug("对应的序号: " + i);
					i += 1;
					// log.debug("遍历ItemMake.img的itemid: "+itemid);
					int quantity = 0;
					int req_meso = 0;
					int req_level = 0;
					int req_maker_level = 0;
					int tuc = 0;
					int req_item = 0;
					int req_equip = 0;
					int catalyst = 0;
					for (MapleData d : c.getChildren()) { // 各属性节点
						if (d.getName().equals("itemNum")) // 需要扣除的数量 和 reqItem
															// reqEquip对应
															// 这2个节点不会同时存在
															// 不用担心冲突
						{
							quantity = MapleDataTool.getInt(d);
						} else if (d.getName().equals("meso")) {
							req_meso = MapleDataTool.getInt(d);
						} else if (d.getName().equals("reqLevel")) {
							req_level = MapleDataTool.getInt(d);
						} else if (d.getName().equals("reqSkillLevel")) {
							req_maker_level = MapleDataTool.getInt(d);
						} else if (d.getName().equals("tuc")) // 随机增加的砸卷次数上限
						{
							tuc = MapleDataTool.getInt(d);
						} else if (d.getName().equals("reqItem")) // 需要的额外其他栏的物品
						{
							req_item = MapleDataTool.getInt(d);
						} else if (d.getName().equals("reqEquip")) // 需要的额外装备物品
						{
							req_equip = MapleDataTool.getInt(d);
						} else if (d.getName().equals("catalyst")) // 催化剂
						{
							catalyst = MapleDataTool.getInt(d);
						} else if (d.getName().equals("recipe")) { // 需要额外材料
							int req_item2 = 0; // 这个是makerrecipedata表里的req_item
							int count = 0;
							for (MapleData e : d.getChildren()) { // 0 1
								for (MapleData f : e.getChildren()) { // count
																		// item
									if (f.getName().equals("count")) {
										count = MapleDataTool.getInt(f);
									} else if (f.getName().equals("item")) {
										req_item2 = MapleDataTool.getInt(f);
									}
								}
								ps = con.prepareStatement("INSERT INTO makerrecipedata (itemid, req_item, count) VALUES (?, ?, ?)");
								ps.setInt(1, itemid);
								ps.setInt(2, req_item2);
								ps.setInt(3, count);
								ps.executeUpdate();
							}
						} // recipe
					}
					ps = con.prepareStatement("INSERT INTO makercreatedata (type, itemid, req_level, req_maker_level, req_meso, req_item, req_equip, catalyst, quantity, tuc) "
							+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
					ps.setInt(1, type);
					ps.setInt(2, itemid);
					ps.setInt(3, req_level);
					ps.setInt(4, req_maker_level);
					ps.setInt(5, req_meso);
					ps.setInt(6, req_item);
					ps.setInt(7, req_equip);
					ps.setInt(8, catalyst);
					ps.setInt(9, quantity);
					ps.setInt(10, tuc);
					ps.executeUpdate();
				} // itemid 遍历完
			} // 所有遍历完
			ps.close();
			con.close();
		} catch (SQLException e) {
			log.error("makeitem储存进SQL出错1", e);
		} finally { // 无论前面的SQL语句有没错 都可以执行finally里的语句
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException ex) {
					log.error("makeitem储存进SQL出错2", ex);
				}
			}
		} // finally
	}

	public Point getMistItemLt(int itemId) { // 获取物品的lt节点 Mist类物品 0528.img
		MapleData item = getItemData(itemId);
		Point pData = (Point) item.getChildByPath("info/lt").getData();
		return pData;
	}

	public Point getMistItemRb(int itemId) { // 获取物品的rb节点 Mist类物品 0528.img
		MapleData item = getItemData(itemId);
		Point pData = (Point) item.getChildByPath("info/rb").getData();
		return pData;
	}

	// 潜能附加卷
	public IItem potentialScrollEquipWithId(IItem equip, int scrollId) {
		if (equip instanceof Equip) {
			Equip nEquip = (Equip) equip;
			double generator = Math.ceil(Math.random() * 100.0);
			boolean success = false;
			switch (scrollId) {
			case 2049400: {
				if (generator >= 10) {
					success = true;
				}
				break;
			}
			case 2049401: {
				if (generator >= 30) {
					success = true;
				}
				break;
			}
			default: {
				return null;
			}
			}
			if (success) {
				nEquip.setIdentify((byte) 1);
				if (nEquip.HasFlag(InventoryConstants.Items.Flags.防爆卷轴)) {
					nEquip.CanceFlag(InventoryConstants.Items.Flags.防爆卷轴);
				}
				return equip;
			} else {
				if (!nEquip.HasFlag(InventoryConstants.Items.Flags.防爆卷轴)) {
					return null;
				} else {
					nEquip.CanceFlag(InventoryConstants.Items.Flags.防爆卷轴);
					return equip;
				}
			}
		}
		return null;
	}

	public IItem potentialScrollEquipWithId_1(IItem equip, int scrollId) {
		Equip nEquip = (Equip) equip;
		double generator = Math.ceil(Math.random() * 100.0);
		boolean success = false;
		switch (scrollId) {
		case 2049400: {
			if (generator <= 90) {
				success = true;
			}
			break;
		}
		case 2049401: {
			if (generator <= 70) {
				success = true;
			}
			break;
		}
		default: {
			return null;
		}
		}
		if (success) {
			nEquip.setIdentify((byte) 1);
		}
		return equip; // return null是意味着装备消失了
	}

	// 放大镜处理
	public IItem MagniflerEquipWithId_2(IItem equip) {
		if (equip instanceof Equip) {
			Equip nEquip = (Equip) equip;
			boolean isLevelUp = false;
			boolean isFirst = false;
			boolean isLevelPro = false;
			int oldPotentialLevel = nEquip.getIdentified();
			if (oldPotentialLevel == 0x11) {
				if (Math.ceil(Math.random() * 50.0) >= 45) {
					isLevelUp = true;
					nEquip.setIdentify((byte) 0x12);
				}
			} else if (oldPotentialLevel == 0x12) {
				if (Math.ceil(Math.random() * 50.0) == 50) {
					isLevelUp = true;
					nEquip.setIdentify((byte) 0x13);
				}
			} else if (oldPotentialLevel == 0x13) {
				isLevelPro = true;
				nEquip.setIdentify((byte) 0x13);
			} else {
				isFirst = true;
				nEquip.setIdentify((byte) 0x11);
			}
			int count = 0;
			if (isLevelUp) {
				count = (int) (2 + Math.random() * (2 - 1 + 1));
				for (int i = 1; i <= count; i++) {
					nEquip.setPotential(
							i,
							randomPotentiaStat(nEquip.getIdentify(),
									nEquip.getItemId(), nEquip.getLevel()));
				}
			} else if (isFirst) {
				count = (int) (2 + Math.random() * (2 - 1 + 1));
				for (int i = 1; i <= count; i++) {
					nEquip.setPotential(
							i,
							randomPotentiaStat(1, nEquip.getItemId(),
									nEquip.getLevel()));
				}
			} else if (isLevelPro) {
				for (int i = 0; i < 3; i++) {
					if (nEquip.getPotential(i) != 0) {
						count++;
					}
				}
				for (int i = 1; i <= count; i++) {
					nEquip.setPotential(
							i,
							randomPotentiaStat(1, nEquip.getItemId(),
									nEquip.getLevel()));
				}
			} else {
				count = (int) (2 + Math.random() * (2 - 1 + 1));
				for (int i = 1; i <= count; i++) {
					nEquip.setPotential(
							i,
							randomPotentiaStat(nEquip.getIdentify(),
									nEquip.getItemId(), nEquip.getLevel()));
				}
				nEquip.setIdentify(nEquip.getIdentified());
			}
		}
		return equip;
	}

	public IEquip MagniflerEquipWithId(IItem equip) {
		int count = 0;
		Equip nEquip = (Equip) equip;
		boolean isFirst = false;
		int oldPotentialLevel = nEquip.getIdentified();
		double a = Math.ceil(Math.random() * 50.0);
		if (oldPotentialLevel == 0x11) { // B级
			if (a >= 35) {
				// 90%几率升A级
				nEquip.setIdentify((byte) 0x12);
			} else {
				nEquip.setIdentify(nEquip.getIdentified());
			}
		} else if (oldPotentialLevel == 0x12) { // A级
			if (a >= 45) {
				// 1%几率升S级
				nEquip.setIdentify((byte) 0x13);
			} else {
				nEquip.setIdentify(nEquip.getIdentified());
			}
		} else if (oldPotentialLevel == 0x13) { // S级
			nEquip.setIdentify((byte) 0x13);
		} else {
			// 第一次鉴定
			isFirst = true;
			if (a == 50) {
				nEquip.setIdentify((byte) 0x13); // S级
			} else if (a >= 45) {
				nEquip.setIdentify((byte) 0x12); // A级
			} else {
				nEquip.setIdentify((byte) 0x11); // B级
			}
			for (int i = 1; i <= (int) (2 + Math.random() * (2 - 1 + 1)); i++) {
				nEquip.setPotential(
						i,
						randomPotentiaStat_1(1, nEquip.getItemId(),
								nEquip.getLevel()));
			}
		}
		if (!isFirst) {
			// 不是第一次的话就重设潜能
			// 第一次的话在上面的判断中已经重新设置了
			for (int i = 0; i < 3; i++) {
				if (nEquip.getPotential(i) != 0) {
					count++;
				}
			}
			for (int i = 1; i <= count; i++) {
				nEquip.setPotential(
						i,
						randomPotentiaStat_1(nEquip.getIdentify(),
								nEquip.getItemId(), nEquip.getLevel()));
			}
			nEquip.setIdentify(nEquip.getIdentified());
		}
		return nEquip;
	}

	// 随机产生潜能
	private int randomPotentiaStat(int oldPotentiaLevel, int itemId,
			byte itemLevel) {
		int rndOne = 0;
		switch (oldPotentiaLevel) {
		case 1: {// the First
			int OptionId[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
					901, 902, 903, 904, 905 };
			rndOne = OptionId[(int) Math.floor(Math.random() * OptionId.length)];
			break;
		}
		case 5: {
			int rnd = Randomizer.getInstance().nextInt(5);
			if (rnd == 0) {
				int OptionId[] = { 10001, 10002, 10003, 10004, 10005, 10006,
						10007, 10008, 10009, 10010, 10011, 10012, 10013, 10014 };
				rndOne = OptionId[(int) Math.floor(Math.random()
						* OptionId.length)];
			} else if (rnd == 1) {
				int OptionId[] = { 10041, 10042, 10043, 10044, 10045, 10046,
						10047, 10048 };
				rndOne = OptionId[(int) Math.floor(Math.random()
						* OptionId.length)];
			} else if (rnd == 2) {
				int OptionId[] = { 10051, 10052, 10053, 10054, 10055 };
				rndOne = OptionId[(int) Math.floor(Math.random()
						* OptionId.length)];
			} else if (rnd == 3) {
				int OptionId[] = { 10070, 10081, 10151, 10156, 10201, 10206,
						10221, 10226, 10231, 10236, 10241, 10246, 10291 };
				rndOne = OptionId[(int) Math.floor(Math.random()
						* OptionId.length)];
			} else if (rnd == 4) {
				int OptionId[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
						14, 901, 902, 903, 904, 905, 10001, 10002, 10003,
						10004, 10005, 10006, 10007, 10008, 10009, 10010, 10011,
						10012, 10013, 10014, 10041, 10042, 10043, 10044, 10045,
						10046, 10047, 10048, 10051, 10052, 10053, 10054, 10055,
						10070, 10081, 10151, 10156, 10201, 10206, 10221, 10226,
						10231, 10236, 10241, 10246, 10291 };
				rndOne = OptionId[(int) Math.floor(Math.random()
						* OptionId.length)];
			}
			break;
		}
		case 6: {
			int rnd = Randomizer.getInstance().nextInt(5);
			if (rnd == 0) {
				int OptionId[] = { 10001, 10002, 10003, 10004, 10005, 10006,
						10007, 10008, 10009, 10010, 10011, 10012, 10013, 10014,
						20041, 20042, 20043, 20044, 20045, 20046, 20047, 20048 };
				rndOne = OptionId[(int) Math.floor(Math.random()
						* OptionId.length)];
			} else if (rnd == 1) {
				int OptionId[] = { 10041, 10042, 10043, 10044, 10045, 10046,
						10047, 10048, 20051, 20052, 20053, 20054, 20055 };
				rndOne = OptionId[(int) Math.floor(Math.random()
						* OptionId.length)];
			} else if (rnd == 2) {
				int OptionId[] = { 10051, 10052, 10053, 10054, 10055, 20070,
						20086, 20181, 20201, 20206, 20291, 20351, 20352, 20353,
						20366, 20376, 20396, 20401, 20406, 20501, 20511, 20650,
						20656 };
				rndOne = OptionId[(int) Math.floor(Math.random()
						* OptionId.length)];
			} else if (rnd == 3) {
				int OptionId[] = { 10070, 10081, 10151, 10156, 10201, 10206,
						10221, 10226, 10231, 10236, 10241, 10246, 10291, 20041,
						20042, 20043, 20044, 20045, 20046, 20047, 20048, 20051,
						20052, 20053, 20054, 20055, 20070, 20086, 20181, 20201,
						20206, 20291, 20351, 20352, 20353, 20366, 20376, 20396,
						20401, 20406, 20501, 20511, 20650, 20656 };
				rndOne = OptionId[(int) Math.floor(Math.random()
						* OptionId.length)];
			} else if (rnd == 4) {
				int OptionId[] = { 20041, 20042, 20043, 20044, 20045, 20046,
						20047, 20048, 20051, 20052, 20053, 20054, 20055, 20070,
						20086, 20181, 20201, 20206, 20291, 20351, 20352, 20353,
						20366, 20376, 20396, 20401, 20406, 20501, 20511, 20650,
						20656, 20041, 20042, 20043, 20044, 20045, 20046, 20047,
						20048, 20051, 20052, 20053, 20054, 20055, 20070, 20086,
						20181, 20201, 20206, 20291, 20351, 20352, 20353, 20366,
						20376, 20396, 20401, 20406, 20501, 20511, 20650, 20656 };
				rndOne = OptionId[(int) Math.floor(Math.random()
						* OptionId.length)];
			}
			break;
		}
		case 7: {
			int rnd = Randomizer.getInstance().nextInt(4);
			if (rnd == 0) {
				int OptionId[] = { 20041, 20042, 20043, 20044, 20045, 20046,
						20047, 20048, 30041, 30042, 30043, 30044, 30045, 30046,
						30047, 30048 };
				rndOne = OptionId[(int) Math.floor(Math.random()
						* OptionId.length)];
			} else if (rnd == 1) {
				int OptionId[] = { 20051, 20052, 20053, 20054, 20055, 30051,
						30052, 30053, 30054, 30055 };
				rndOne = OptionId[(int) Math.floor(Math.random()
						* OptionId.length)];
			} else if (rnd == 2) {
				int OptionId[] = { 20070, 20086, 20181, 20201, 20206, 20291,
						20351, 20352, 20353, 20366, 20376, 20396, 20401, 20406,
						20501, 20511, 20650, 20656, 30070, 30086, 30106, 30107,
						30291, 30356, 30357, 30366, 30371, 30376, 30501, 30511,
						30551, 30601, 30602, 30650, 30656, 30701, 30702, 31001,
						31002, 31003, 31004 };
				rndOne = OptionId[(int) Math.floor(Math.random()
						* OptionId.length)];
			} else if (rnd == 3) {
				int OptionId[] = { 20406, 20501, 20511, 20650, 20656, 20041,
						20042, 20043, 20044, 20045, 20046, 20047, 20048, 20051,
						20052, 20053, 20054, 20055, 20070, 20086, 20181, 20201,
						20206, 20291, 20351, 20352, 20353, 20366, 20376, 20396,
						20401, 20406, 20501, 20511, 20650, 20656, 30041, 30042,
						30043, 30044, 30045, 30046, 30047, 30048, 30051, 30052,
						30053, 30054, 30055, 30070, 30086, 30106, 30107, 30291,
						30356, 30357, 30366, 30371, 30376, 30501, 30511, 30551,
						30601, 30602, 30650, 30656, 30701, 30702, 31001, 31002,
						31003, 31004 };
				rndOne = OptionId[(int) Math.floor(Math.random()
						* OptionId.length)];
			}
			break;
		}
		}
		if (checkEquipStat(itemId, rndOne, itemLevel)) {
			return rndOne;
		} else {
			return randomPotentiaStat(oldPotentiaLevel, itemId, itemLevel);
		}
	}

	private int getItemType(int itemId) {
		if (itemId >= 1010000 && itemId < 1040000 || itemId >= 1122000
				&& itemId < 1143000) { // 挂坠 耳环
			return 20;
			// cat = "Accessory";
		} else if (itemId >= 1000000 && itemId < 1010000) { // 头盔
			return 51;
			// cat = "Cap";
		} else if (itemId >= 1102000 && itemId < 1103000) { // 披风
			return 53;
			// cat = "Cape";
		} else if (itemId >= 1040000 && itemId < 1050000) { // 上衣
			return 11;
			// cat = "Coat";
		} else if (itemId >= 1080000 && itemId < 1090000) { // 手套
			return 54;
			// cat = "Glove";
		} else if (itemId >= 1050000 && itemId < 1060000) { // 套服
			return 52;
			// cat = "Longcoat";
		} else if (itemId >= 1060000 && itemId < 1070000) { // 裤裙
			return 90;
			// cat = "Pants";
		} else if (itemId >= 1092000 && itemId < 1100000) { // 盾
			return -1;
			// cat = "Shield";
		} else if (itemId >= 1070000 && itemId < 1080000) { // 鞋子
			return 54;
			// cat = "Shoes";
		} else if (itemId >= 1300000 && itemId < 1800000) { // 武器
			return 10;
			// cat = "Weapon";
		} else {
			return -1;
		}
	}

	private boolean checkEquipStat(int itemid, int stat, byte itemLevel) {
		int itemtype = getItemType(itemid);
		if (itemtype == -1) {
			return true;
		}
		switch (stat) {
		case 11:
		case 12:
		case 10011:
		case 10012:
		case 10051:
		case 10052:
		case 10055:
		case 10070:
		case 10201:
		case 10206:
		case 10221:
		case 10226:
		case 10231:
		case 10236:
		case 10241:
		case 10246:
		case 10291:
		case 20051:
		case 20052:
		case 20055:
		case 20070:
		case 20201:
		case 20206:
		case 20291:
		case 30051:
		case 30052:
		case 30055:
		case 30070:
			if (itemtype == 10) {
				return true;
			} else {
				return false;
			}
		case 30291:// (50)
		case 30602:// (50)
			if (itemtype == 10 && (itemLevel >= 50)) {
				return true;
			} else {
				return false;
			}
		case 30601:// (60)
			if (itemtype == 10 && (itemLevel >= 60)) {
				return true;
			} else {
				return false;
			}
			// ///////////////////////////// 武器部分完毕 开始护甲部分
		case 13:
		case 14:
		case 10013:
		case 10014:
		case 10045:
		case 10046:
		case 10053:
		case 10054:
		case 20045:
		case 20046:
		case 20053:
		case 20054:
		case 30045:
		case 30046:
		case 30053:
		case 30054:
		case 30551:
			if (itemtype == 11) {
				return true;
			} else {
				return false;
			}
			// 护甲完毕 开始 20
		case 20351:
		case 20352:
		case 20353:
			if (itemtype == 20) {
				return true;
			} else {
				return false;
			}
		case 30356:// (20)
			if (itemtype == 20 && itemLevel >= 20) {
				return true;
			} else {
				return false;
			}
		case 30357:// (40)
			if (itemtype == 20 && itemLevel >= 40) {
				return true;
			} else {
				return false;
			}
			// 20完毕 开始40
		case 10151:
		case 10156:
			if (itemtype == 40) {
				return true;
			} else {
				return false;
			}
			// 40 完毕 开始 51头盔
		case 901:
		case 902:
		case 903:
		case 904:
		case 905:
			if (itemtype == 51) {
				return true;
			} else {
				return false;
			}
		case 30106:// (30)
			if (itemtype == 51 && itemLevel >= 30) {
				return true;
			} else {
				return false;
			}
		case 30107:// (50)
			if (itemtype == 51 && itemLevel >= 50) {
				return true;
			} else {
				return false;
			}
		case 31002:// (70)
			if (itemtype == 51 && itemLevel >= 70) {
				return true;
			} else {
				return false;
			}
			// 头盔结束 开始52 套服
		case 30366:
		case 30371:
		case 20366:
			if (itemtype == 52) {
				return true;
			} else {
				return false;
			}
		case 20396:// (50级)
			if (itemtype == 52 && itemLevel >= 50) {
				return true;
			} else {
				return false;
			}
			// 套服结束 开始53
		case 31004:// (70)
			if (itemtype == 53) {
				return true;
			} else {
				return false;
			}
			// 开始54手套
		case 20401:
		case 20406:
			if (itemtype == 54) {
				return true;
			} else {
				return false;
			}
		case 30701:// (20)
			if (itemtype == 54 && itemLevel >= 20) {
				return true;
			} else {
				return false;
			}
		case 30702:// (40)
			if (itemtype == 54 && itemLevel >= 40) {
				return true;
			} else {
				return false;
			}
		case 31003:// (120)
			if (itemtype == 54 && itemLevel >= 120) {
				return true;
			} else {
				return false;
			}
			// 55开始鞋子
		case 9:
		case 10:
		case 10009:
		case 10010:
			if (itemtype == 55) {
				return true;
			} else {
				return false;
			}
		case 31001:// (70)
			if (itemtype == 55 && itemLevel >= 70) {
				return true;
			} else {
				return false;
			}
			// 开始90
		case 20181:
		case 20376:
		case 30376:
			if (itemtype == 90) {
				return true;
			} else {
				return false;
			}
		case 20656:// (30)
			if (itemtype == 90 && itemLevel >= 30) {
				return true;
			} else {
				return false;
			}
		case 20501:// (50)
		case 20511:// (50)
		case 20650:// (50)
		case 30501:// (50)
		case 30511:// (50)
		case 30650:// (50)
		case 30656:// (50)
			if (itemtype == 90 && itemLevel >= 50) {
				return true;
			} else {
				return false;
			}
		default:
			return true;
		}
	}

	private int randomPotentiaStat_1(int oldPotentiaLevel, int itemId,
			byte itemLevel) {
		int rndOne = 0;
		int rnd = Randomizer.getInstance().nextInt(5);
		int[] tempOptionId = null;
		switch (oldPotentiaLevel) {
		case 1: { // 如果是第一次
			int OptionId[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
					901, 902, 903, 904, 905 };
			tempOptionId = OptionId;
			break;
		}
		case 5: { // B级
			// 产生 0 - 5 之间的随机整数
			if (rnd == 0) {
				int OptionId[] = { 10001, 10002, 10003, 10004, 10005, 10006,
						10007, 10008, 10009, 10010, 10011, 10012, 10013, 10014 };
				tempOptionId = OptionId;
			} else if (rnd == 1) {
				int OptionId[] = { 10041, 10042, 10043, 10044, 10045, 10046,
						10047, 10048 };
				tempOptionId = OptionId;
			} else if (rnd == 2) {
				int OptionId[] = { 10051, 10052, 10053, 10054, 10055 };
				tempOptionId = OptionId;
			} else if (rnd == 3) {
				int OptionId[] = { 10070, 10081, 10151, 10156, 10201, 10206,
						10221, 10226, 10231, 10236, 10241, 10246, 10291 };
				tempOptionId = OptionId;
			} else if (rnd == 4) {
				int OptionId[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
						14, 901, 902, 903, 904, 905, 10001, 10002, 10003,
						10004, 10005, 10006, 10007, 10008, 10009, 10010, 10011,
						10012, 10013, 10014, 10041, 10042, 10043, 10044, 10045,
						10046, 10047, 10048, 10051, 10052, 10053, 10054, 10055,
						10070, 10081, 10151, 10156, 10201, 10206, 10221, 10226,
						10231, 10236, 10241, 10246, 10291 };
				tempOptionId = OptionId;
			}
			break;
		}
		case 6: { // A级
			if (rnd == 0) {
				int OptionId[] = { 10001, 10002, 10003, 10004, 10005, 10006,
						10007, 10008, 10009, 10010, 10011, 10012, 10013, 10014,
						20041, 20042, 20043, 20044, 20045, 20046, 20047, 20048 };
				tempOptionId = OptionId;
			} else if (rnd == 1) {
				int OptionId[] = { 10041, 10042, 10043, 10044, 10045, 10046,
						10047, 10048, 20051, 20052, 20053, 20054, 20055 };
				tempOptionId = OptionId;
			} else if (rnd == 2) {
				int OptionId[] = { 10051, 10052, 10053, 10054, 10055, 20070,
						20086, 20181, 20201, 20206, 20291, 20351, 20352, 20353,
						20366, 20376, 20396, 20401, 20406, 20501, 20511, 20650,
						20656 };
				tempOptionId = OptionId;
			} else if (rnd == 3) {
				int OptionId[] = { 10070, 10081, 10151, 10156, 10201, 10206,
						10221, 10226, 10231, 10236, 10241, 10246, 10291, 20041,
						20042, 20043, 20044, 20045, 20046, 20047, 20048, 20051,
						20052, 20053, 20054, 20055, 20070, 20086, 20181, 20201,
						20206, 20291, 20351, 20352, 20353, 20366, 20376, 20396,
						20401, 20406, 20501, 20511, 20650, 20656 };
				tempOptionId = OptionId;
			} else if (rnd == 4) {
				int OptionId[] = { 20041, 20042, 20043, 20044, 20045, 20046,
						20047, 20048, 20051, 20052, 20053, 20054, 20055, 20070,
						20086, 20181, 20201, 20206, 20291, 20351, 20352, 20353,
						20366, 20376, 20396, 20401, 20406, 20501, 20511, 20650,
						20656, 20041, 20042, 20043, 20044, 20045, 20046, 20047,
						20048, 20051, 20052, 20053, 20054, 20055, 20070, 20086,
						20181, 20201, 20206, 20291, 20351, 20352, 20353, 20366,
						20376, 20396, 20401, 20406, 20501, 20511, 20650, 20656 };
				tempOptionId = OptionId;
			}
			break;
		}
		case 7: { // S级
			rnd = Randomizer.getInstance().nextInt(4);
			if (rnd == 0) {
				int OptionId[] = { 20041, 20042, 20043, 20044, 20045, 20046,
						20047, 20048, 30041, 30042, 30043, 30044, 30045, 30046,
						30047, 30048 };
				tempOptionId = OptionId;
			} else if (rnd == 1) {
				int OptionId[] = { 20051, 20052, 20053, 20054, 20055, 30051,
						30052, 30053, 30054, 30055 };
				tempOptionId = OptionId;
			} else if (rnd == 2) {
				int OptionId[] = { 20070, 20086, 20181, 20201, 20206, 20291,
						20351, 20352, 20353, 20366, 20376, 20396, 20401, 20406,
						20501, 20511, 20650, 20656, 30070, 30086, 30106, 30107,
						30291, 30356, 30357, 30366, 30371, 30376, 30501, 30511,
						30551, 30601, 30602, 30650, 30656, 30701, 30702, 31001,
						31002, 31003, 31004 };
				tempOptionId = OptionId;
			} else if (rnd == 3) {
				int OptionId[] = { 20406, 20501, 20511, 20650, 20656, 20041,
						20042, 20043, 20044, 20045, 20046, 20047, 20048, 20051,
						20052, 20053, 20054, 20055, 20070, 20086, 20181, 20201,
						20206, 20291, 20351, 20352, 20353, 20366, 20376, 20396,
						20401, 20406, 20501, 20511, 20650, 20656, 30041, 30042,
						30043, 30044, 30045, 30046, 30047, 30048, 30051, 30052,
						30053, 30054, 30055, 30070, 30086, 30106, 30107, 30291,
						30356, 30357, 30366, 30371, 30376, 30501, 30511, 30551,
						30601, 30602, 30650, 30656, 30701, 30702, 31001, 31002,
						31003, 31004 };
				tempOptionId = OptionId;
			}
			break;
		}
		}
		rndOne = tempOptionId[(int) Math.floor(Math.random()
				* tempOptionId.length)];
		/*
		 * System.out.print("备选潜能数组: "+tempOptionId); log.debug("潜能节点:
		 * "+rndOne);
		 */
		while (!potentiaCanGain(itemId, rndOne)) {
			// log.debug("潜能不符合条件 重新生成");
			return randomPotentiaStat_1(oldPotentiaLevel, itemId, itemLevel);
		}
		return rndOne;
	}

	public void setPotentialInSql() { // 遍历WZ把潜能节点加入Sql表里
		int potentialId = 0; // 遍历Item.wz/ItemOption.img获得的潜能id
		MapleDataProvider dataProvider = MapleDataProviderFactory
				.getDataProvider(new File(System
						.getProperty("net.sf.odinms.wzpath") + "/" + "Item.wz"));
		MapleData a = dataProvider.getData("ItemOption.img");
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con
					.prepareStatement("INSERT INTO potentialdata (potentialid, optionType, reqLevel) VALUES (?, ?, ?)");
			for (MapleData b : a.getChildren()) { // getChildren 子项名 000001 -
													// 031004
				int optionType = 0;
				int reqLevel = 0;
				potentialId = Integer.parseInt(b.getName()); // 获取潜能节点的id
				log.debug("遍历ItemOption.img的PotentialId: " + potentialId);
				for (MapleData c : b.getChildren()) { // info level
					if (c.getName().equals("info")) {
						for (MapleData d : c.getChildren()) { // optionType
																// reqLevel
							if (d.getName().equals("optionType")) {
								optionType = MapleDataTool.getInt(d);
							}
							if (d.getName().equals("reqLevel")) {
								reqLevel = MapleDataTool.getInt(d);
							}
						}
					}
					log.debug("optionType: " + optionType);
					log.debug("reqLevel: " + reqLevel);
				}
				getPotentialId.put(potentialId, new Pair<Integer, Integer>(
						optionType, reqLevel));
				ps.setInt(1, potentialId);
				ps.setInt(2, optionType);
				ps.setInt(3, reqLevel);
				ps.executeUpdate();
			}
			ps.close(); // 循环结束再关表
			con.close();
		} catch (SQLException se) {
			log.error("潜能节点储存进SQL出错", se);
		}
	}

	public Pair<Integer, Integer> getPotentialAtXml(int potentialid) { // 获取潜能节点相关属性
		Pair<Integer, Integer> stats = getPotentialId.get(potentialid);
		if (stats == null) {
			int potentialId = 0; // 遍历Item.wz/ItemOption.img获得的潜能id
			MapleDataProvider dataProvider = MapleDataProviderFactory
					.getDataProvider(new File(System
							.getProperty("net.sf.odinms.wzpath")
							+ "/"
							+ "Item.wz"));
			MapleData a = dataProvider.getData("ItemOption.img");
			for (MapleData b : a.getChildren()) { // getChildren 子项名 000001 -
													// 031004
				int optionType = 0;
				int reqLevel = 0;
				potentialId = Integer.parseInt(b.getName()); // 获取潜能节点的id
				for (MapleData c : b.getChildren()) { // info level
					if (c.getName().equals("info")) {
						for (MapleData d : c.getChildren()) { // optionType
																// reqLevel
							if (d.getName().equals("optionType")) {
								optionType = MapleDataTool.getInt(d);
							}
							if (d.getName().equals("reqLevel")) {
								reqLevel = MapleDataTool.getInt(d);
							}
						}
					}
				}
				getPotentialId.put(potentialId, new Pair<Integer, Integer>(
						optionType, reqLevel));
			}
		}
		return stats;
	}

	public String CheckitemType(int itemId) {
		String cat = "null";
		if (itemId >= 1010000 && itemId < 1040000 || itemId >= 1122000
				&& itemId < 1143000) {
			cat = "Accessory";
		} else if (itemId >= 1000000 && itemId < 1010000) {
			cat = "Cap";
		} else if (itemId >= 1102000 && itemId < 1103000) {
			cat = "Cape";
		} else if (itemId >= 1040000 && itemId < 1050000) {
			cat = "Coat";
		} else if (itemId >= 1080000 && itemId < 1090000) {
			cat = "Glove";
		} else if (itemId >= 1050000 && itemId < 1060000) {
			cat = "Longcoat";
		} else if (itemId >= 1060000 && itemId < 1070000) {
			cat = "Pants";
		} else if (itemId >= 1070000 && itemId < 1080000) {
			cat = "Shoes";
		} else if (itemId >= 1300000 && itemId < 1800000) {
			cat = "Weapon";
		} else if (itemId >= 1032000 && itemId < 1033000) {
			cat = "Ear";
		} else if (itemId >= 1942000 && itemId < 1982000) { // 龙神装备
			cat = "Dragon";
		} else if (itemId >= 1612000 && itemId < 1662000) { // 机械师装备
			cat = "Mechanic";
		} else if (itemId >= 1662000 && itemId < 1682000) { // 智能机械人装备
			cat = "Android";
		}
		return cat;
	}

	// 检查装备是否能接受该潜能
	public boolean potentiaCanGain(int itemid, int potentiaId) {
		int itemLevel = getReqLevel(itemid);
		String type = CheckitemType(itemid);
		boolean a = type.equals("Weapon");
		boolean b = type.equals("Accessory");
		boolean c = type.equals("Cap");
		boolean d = type.equals("Cape");
		boolean e = type.equals("Coat");
		boolean f = type.equals("Glove");
		boolean g = type.equals("Longcoat");
		boolean h = type.equals("Pants");
		boolean i = type.equals("Shoes");
		boolean j = type.equals("Ear");
		boolean k = c || d || e || f || g || h || i;
		boolean l = e || h || g;
		boolean m = (e || g) && !h;
		/*
		 * a 武器 [10] !a 非武器 [11] b 项链 c 帽子 [51] d 披风 e 上衣 f 手套 [54] g 套服 h 裤裙 i
		 * 鞋子 [55] j 耳环 [40] k = c + d + e + f + g + h + i 上衣 裤裙 套服 帽子 披风 鞋子 手套
		 * [20] l = e + h + g 上衣 裤裙 套服 [52] m = e + h 上衣 裤裙 不包括套服 [53]
		 */
		switch (potentiaId) {
		// 10 武器
		case 11:
		case 12:
		case 10011:
		case 10012:
		case 10051:
		case 10052:
		case 10055:
		case 10070:
		case 10201:
		case 10206:
		case 10221:
		case 10226:
		case 10231:
		case 10236:
		case 10241:
		case 10246:
		case 10291:
		case 20051:
		case 20052:
		case 20055:
		case 20070:
		case 20201:
		case 20206:
		case 20291:
		case 30051:
		case 30052:
		case 30055:
		case 30070:
			if (a) {
				return true;
			}
		case 30291:// (50)
		case 30602:// (50)
			if (a && itemLevel >= 50) {
				return true;
			}
		case 30601:// (60)
			if (a && itemLevel >= 60) {
				return true;
			}
			// 11 非武器
		case 13:
		case 14:
		case 10013:
		case 10014:
		case 10045:
		case 10046:
		case 10053:
		case 10054:
		case 20045:
		case 20046:
		case 20053:
		case 20054:
		case 30045:
		case 30046:
		case 30053:
		case 30054:
		case 30551:
			if (!a) {
				return true;
			}
			// 20 上衣 裤裙 套服 帽子 披风 鞋子 手套 不包括饰品
		case 20351:
		case 20352:
		case 20353:
			if (k) {
				return true;
			}
		case 30356:// (20)
			if (k && itemLevel >= 20) {
				return true;
			}
		case 30357:// (40)
			if (k && itemLevel >= 40) {
				return true;
			}
			// 40 耳环
		case 10151:
		case 10156:
			if (j) {
				return true;
			}
			// 51 帽子
		case 901:
		case 902:
		case 903:
		case 904:
		case 905:
			if (c) {
				return true;
			}
		case 30106:// (30)
			if (c && itemLevel >= 30) {
				return true;
			}
		case 30107:// (50)
			if (c && itemLevel >= 50) {
				return true;
			}
		case 31002:// (70)
			if (c && itemLevel >= 70) {
				return true;
			}
			// 52 上衣 裤裙 套服
		case 30366:
		case 30371:
		case 20366:
			if (l) {
				return true;
			}
		case 20396:// (50级)
			if (l && itemLevel >= 50) {
				return true;
			}
			// 53 上衣 裤裙 不包括套服
		case 31004:// (70)
			if (m && itemLevel >= 70) {
				return true;
			}
			// 54 手套
		case 20401:
		case 20406:
			if (f) {
				return true;
			}
		case 30701: // (20)
			if (f && itemLevel >= 20) {
				return true;
			}
		case 30702: // (40)
			if (f && itemLevel >= 40) {
				return true;
			}
		case 31003: // (120)
			if (f && itemLevel >= 120) {
				return true;
			}
			// 55 鞋子
		case 9:
		case 10:
		case 10009:
		case 10010:
			if (i) {
				return true;
			}
		case 31001:// (70)
			if (i && itemLevel >= 70) {
				return true;
			}
			/*
			 * ================= 20181 潜在能力未知 20376 潜在能力未知 20501 潜在能力未知 20511
			 * 潜在能力未知 20650 潜在能力未知 20656 潜在能力未知 30501 潜在能力未知 30511 潜在能力未知 30650
			 * 潜在能力未知 30656 潜在能力未知 ================= //开始90 case 20181: case
			 * 20376: case 30376: return true; case 20656://(30) if (itemLevel
			 * >= 30) return true; case 20501://(50) case 20511://(50) case
			 * 20650://(50) case 30501://(50) case 30511://(50) case
			 * 30650://(50) case 30656://(50) if (itemLevel >= 50) return true;
			 */
		}
		return false;
	}

	// 星级
	public IItem starScrollEquipWithId_2(IItem equip, int scrollId, boolean isGM) {
		if (equip instanceof Equip) {
			Equip nEquip = (Equip) equip;
			int upgradeLevel = nEquip.getLevel();
			int starLevel = nEquip.getStarlevel();
			double generator = Math.ceil(Math.random() * 1000.0);
			int increase = 1;
			boolean success = false;
			if (getItemType(nEquip.getItemId()) == 10) {
				if (nEquip.getWatk() > 0) {
					if (nEquip.getWatk() < 120) {
						increase = 2;
					} else if (nEquip.getWatk() >= 120
							&& nEquip.getWatk() < 150) {
						increase = 3;
					} else {
						increase = 4;
					}
				} else if (nEquip.getMatk() > 0) {
					if (nEquip.getMatk() < 120) {
						increase = 2;
					} else if (nEquip.getMatk() >= 120
							&& nEquip.getMatk() < 150) {
						increase = 3;
					} else {
						increase = 4;
					}
				}
			} else {
				if (upgradeLevel > 9) {
					increase += Randomizer.getInstance().nextInt(6);
				} else if (upgradeLevel > 7) {
					increase += Randomizer.getInstance().nextInt(5);
				} else if (upgradeLevel > 5) {
					increase += Randomizer.getInstance().nextInt(4);
				} else if (upgradeLevel > 3) {
					increase += Randomizer.getInstance().nextInt(3);
				} else {
					increase += Randomizer.getInstance().nextInt(2);
				}
			}
			if (isGM == true) {
				success = true;
			} else {
				switch (scrollId) {
				case 2049300: {// 高级装备强化卷轴
					switch (starLevel) {
					case 0: {// 100%
						success = true;
						break;
					}
					case 1: {
						if (generator <= 900) {
							success = true;
						}
						break;
					}
					case 2: {
						if (generator <= 800) {
							success = true;
						}
						break;
					}
					case 3: {
						if (generator <= 700) {
							success = true;
						}
						break;
					}
					case 4: {
						if (generator <= 600) {
							success = true;
						}
						break;
					}
					case 5: {
						if (generator <= 500) {
							success = true;
						}
						break;
					}
					case 6: {
						if (generator <= 400) {
							success = true;
						}
						break;
					}
					case 7: {
						if (generator <= 300) {
							success = true;
						}
						break;
					}
					case 8: {
						if (generator <= 200) {
							success = true;
						}
						break;
					}
					case 9: {
						if (generator <= 100) {
							success = true;
						}
						break;
					}
					}
					break;
				}
				case 2049301: {
					switch (starLevel) {
					case 0: {
						if (generator <= 950) {
							success = true;
						}
						break;
					}
					case 1: {
						if (generator <= 700) {
							success = true;
						}
						break;
					}
					case 2: {
						if (generator <= 600) {
							success = true;
						}
						break;
					}
					case 3: {
						if (generator <= 500) {
							success = true;
						}
						break;
					}
					case 4: {
						if (generator <= 400) {
							success = true;
						}
						break;
					}
					case 5: {
						if (generator <= 300) {
							success = true;
						}
						break;
					}
					case 6: {
						if (generator <= 200) {
							success = true;
						}
						break;
					}
					case 7: {
						if (generator <= 100) {
							success = true;
						}
						break;
					}
					case 8: {
						if (generator <= 100) {
							success = true;
						}
						break;
					}
					case 9: {
						if (generator <= 100) {
							success = true;
						}
						break;
					}
					}
					break;
				}
				default: {
					return null;
				}
				}
			}
			if (success) {
				nEquip.setStarlevel((byte) (nEquip.getStarlevel() + 1));
				// nEquip.setUpgradeLevel((byte) (nEquip.getUpgradeLevel() +
				// 1));
				if (nEquip.getStr() > 0) {
					nEquip.setStr((short) (nEquip.getStr() + increase));
				}
				if (nEquip.getDex() > 0) {
					nEquip.setDex((short) (nEquip.getDex() + increase));
				}
				if (nEquip.getInt() > 0) {
					nEquip.setInt((short) (nEquip.getInt() + increase));
				}
				if (nEquip.getLuk() > 0) {
					nEquip.setLuk((short) (nEquip.getLuk() + increase));
				}
				if (nEquip.getWatk() > 0) {
					nEquip.setWatk((short) (nEquip.getWatk() + increase));
				}
				if (nEquip.getWdef() > 0) {
					nEquip.setWdef((short) (nEquip.getWdef() + increase));
				}
				if (nEquip.getMatk() > 0) {
					nEquip.setMatk((short) (nEquip.getMatk() + increase));
				}
				if (nEquip.getMdef() > 0) {
					nEquip.setMdef((short) (nEquip.getMdef() + increase));
				}
				if (nEquip.getAcc() > 0) {
					nEquip.setAcc((short) (nEquip.getAcc() + increase));
				}
				if (nEquip.getAvoid() > 0) {
					nEquip.setAvoid((short) (nEquip.getAvoid() + increase));
				}
				if (nEquip.getSpeed() > 0) {
					nEquip.setSpeed((short) (nEquip.getSpeed() + increase));
				}
				if (nEquip.getJump() > 0) {
					nEquip.setJump((short) (nEquip.getJump() + increase));
				}
				if (nEquip.getHp() > 0) {
					nEquip.setHp((short) (nEquip.getHp() + increase));
				}
				if (nEquip.getMp() > 0) {
					nEquip.setMp((short) (nEquip.getMp() + increase));
				}
				if (nEquip.HasFlag(InventoryConstants.Items.Flags.防爆卷轴)) {
					nEquip.CanceFlag(InventoryConstants.Items.Flags.防爆卷轴);
				}
				return equip;
			} else {
				if (!nEquip.HasFlag(InventoryConstants.Items.Flags.防爆卷轴)) {
					return null;
				} else {
					nEquip.CanceFlag(InventoryConstants.Items.Flags.防爆卷轴);
					return equip;
				}
			}
		}
		return null;
	}

	public IItem starScrollEquipWithId(IItem equip, int scrollId, boolean isGM) {
		Equip nEquip = (Equip) equip;
		int 砸卷次数 = nEquip.getLevel();
		int starLevel = nEquip.getStarlevel();
		double generator = Math.ceil(Math.random() * 1000.0);
		int increase = 1; // 默认增加值
		boolean success = false;
		if (CheckitemType(nEquip.getItemId()).equals("Weapon")) {
			if (nEquip.getWatk() > 0) { // 物攻
				if (nEquip.getWatk() < 120) {
					increase = 2;
				} else if (nEquip.getWatk() >= 120 && nEquip.getWatk() < 150) {
					increase = 3;
				} else {
					increase = 4;
				}
			} else if (nEquip.getMatk() > 0) { // 魔攻
				if (nEquip.getMatk() < 120) {
					increase = 2;
				} else if (nEquip.getMatk() >= 120 && nEquip.getMatk() < 150) {
					increase = 3;
				} else {
					increase = 4;
				}
			}
		} else {
			if (砸卷次数 > 9) {
				increase += Randomizer.getInstance().nextInt(6);
			} else if (砸卷次数 > 7) {
				increase += Randomizer.getInstance().nextInt(5);
			} else if (砸卷次数 > 5) {
				increase += Randomizer.getInstance().nextInt(4);
			} else if (砸卷次数 > 3) {
				increase += Randomizer.getInstance().nextInt(3);
			} else {
				increase += Randomizer.getInstance().nextInt(2);
			}
		}
		switch (scrollId) {
		case 2049300: // 高级装备强化卷轴
			switch (starLevel) {
			case 0: // 100%
				success = true;
				break;
			case 1:
				if (generator <= 900) {
					success = true;
				}
				break;
			case 2:
				if (generator <= 800) {
					success = true;
				}
				break;
			case 3:
				if (generator <= 700) {
					success = true;
				}
				break;
			case 4:
				if (generator <= 600) {
					success = true;
				}
				break;
			case 5:
				if (generator <= 500) {
					success = true;
				}
				break;
			case 6:
				if (generator <= 400) {
					success = true;
				}
				break;
			case 7:
				if (generator <= 300) {
					success = true;
				}
				break;
			case 8:
				if (generator <= 200) {
					success = true;
				}
				break;
			case 9:
				if (generator <= 100) {
					success = true;
				}
				break;
			}
			break;
		case 2049301:
			switch (starLevel) {
			case 0:
				if (generator <= 800) {
					success = true;
				}
				break;
			case 1:
				if (generator <= 700) {
					success = true;
				}
				break;
			case 2:
				if (generator <= 600) {
					success = true;
				}
				break;
			case 3:
				if (generator <= 500) {
					success = true;
				}
				break;
			case 4:
				if (generator <= 400) {
					success = true;
				}
				break;
			case 5:
				if (generator <= 300) {
					success = true;
				}
				break;
			case 6:
				if (generator <= 200) {
					success = true;
				}
				break;
			case 7:
				if (generator <= 100) {
					success = true;
				}
				break;
			case 8:
				if (generator <= 100) {
					success = true;
				}
				break;
			case 9:
				if (generator <= 100) {
					success = true;
				}
				break;
			}
			break;
		}
		if (isGM == true || nEquip.getLocked()) {
			success = true;
		}
		if (nEquip.getLocked()) {
			nEquip.setLocked((byte) 0);
		}
		if (success) {
			nEquip.setStarlevel((byte) (nEquip.getStarlevel() + 1));
			if (nEquip.getStr() > 0) {
				nEquip.setStr((short) (nEquip.getStr() + increase));
			}
			if (nEquip.getDex() > 0) {
				nEquip.setDex((short) (nEquip.getDex() + increase));
			}
			if (nEquip.getInt() > 0) {
				nEquip.setInt((short) (nEquip.getInt() + increase));
			}
			if (nEquip.getLuk() > 0) {
				nEquip.setLuk((short) (nEquip.getLuk() + increase));
			}
			if (nEquip.getWatk() > 0) {
				nEquip.setWatk((short) (nEquip.getWatk() + increase));
			}
			if (nEquip.getWdef() > 0) {
				nEquip.setWdef((short) (nEquip.getWdef() + increase));
			}
			if (nEquip.getMatk() > 0) {
				nEquip.setMatk((short) (nEquip.getMatk() + increase));
			}
			if (nEquip.getMdef() > 0) {
				nEquip.setMdef((short) (nEquip.getMdef() + increase));
			}
			if (nEquip.getAcc() > 0) {
				nEquip.setAcc((short) (nEquip.getAcc() + increase));
			}
			if (nEquip.getAvoid() > 0) {
				nEquip.setAvoid((short) (nEquip.getAvoid() + increase));
			}
			if (nEquip.getSpeed() > 0) {
				nEquip.setSpeed((short) (nEquip.getSpeed() + increase));
			}
			if (nEquip.getJump() > 0) {
				nEquip.setJump((short) (nEquip.getJump() + increase));
			}
			if (nEquip.getHp() > 0) {
				nEquip.setHp((short) (nEquip.getHp() + increase));
			}
			if (nEquip.getMp() > 0) {
				nEquip.setMp((short) (nEquip.getMp() + increase));
			}
		}
		return equip;
	}
}