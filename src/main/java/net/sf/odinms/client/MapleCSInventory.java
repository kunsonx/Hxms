package net.sf.odinms.client;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import org.apache.log4j.Logger;

/**
 * DELETE FROM items USING items,items_cs WHERE items_cs.gift = 0 &
 * items.items_id = items_cs.items_id & items.characterid = 1;
 *
 * @author NightCoffee
 */
public class MapleCSInventory implements MapleItemsNameSpace {

	private static Logger log = Logger.getLogger(MapleCSInventory.class);
	private static final int MAX_SLOT = 100;
	private Map<Long, MapleCSInventoryItem> csitems = new LinkedHashMap<Long, MapleCSInventoryItem>();
	private Map<Long, MapleCSInventoryItem> csgifts = new LinkedHashMap<Long, MapleCSInventoryItem>();
	private List<Integer> gift_items = new ArrayList<Integer>();

	public MapleCSInventory(MapleCharacter chr) {
		// loadFromDB(accountid);
	}

	public void LoadFromDB(java.sql.Connection con) {
	}

	public List<Integer> GetGiftItemsList() {
		return gift_items;
	}

	public void addFromDb(IItem item, ResultSet rs, boolean gift)
			throws SQLException {
		int sn = rs.getInt("sn");
		MapleCSInventoryItem csitem = new MapleCSInventoryItem(item, sn, gift);
		csitem.setSender(rs.getString("sender"));
		csitem.setMessage(rs.getString("message"));
		if (gift) {
			csgifts.put(item.getUniqueid(), csitem);
			csitem.setDBID(rs.getInt("items_id"));
			gift_items.add(csitem.getDBID());
		} else {
			csitems.put(item.getUniqueid(), csitem);
		}
	}

	/*
	 * public final void loadFromDB(int id) { try { Connection con =
	 * DatabaseConnection.getConnection(); PreparedStatement ps =
	 * con.prepareStatement("SELECT * FROM csinventory WHERE accountid = ?");
	 * ps.setInt(1, id); ResultSet rs = ps.executeQuery(); while (rs.next()) {
	 * MapleCSInventoryItem citem = new
	 * MapleCSInventoryItem(rs.getInt("uniqueid"), rs.getInt("itemid"),
	 * rs.getInt("sn"), (short) rs.getInt("quantity"), rs.getBoolean("gift"));
	 * citem.setExpire(rs.getTimestamp("expiredate"));
	 * citem.setSender(rs.getString("sender")); csitems.put(citem.getUniqueId(),
	 * citem); } rs.close(); ps.close();
	 * 
	 * ps = con.prepareStatement("SELECT * FROM csgifts WHERE accountid = ?");
	 * ps.setInt(1, accountid); rs = ps.executeQuery(); while (rs.next()) {
	 * MapleCSInventoryItem gift; if (rs.getInt("itemid") >= 5000000 &&
	 * rs.getInt("itemid") <= 5000100) { int petId =
	 * MaplePet.createPet(rs.getInt("itemid"), chr); gift = new
	 * MapleCSInventoryItem(petId, rs.getInt("itemid"), rs.getInt("sn"), (short)
	 * 1, true); } else { if (rs.getInt("isRing") > 0) { gift = new
	 * MapleCSInventoryItem(rs.getInt("isRing"), rs.getInt("itemid"),
	 * rs.getInt("sn"), (short) rs.getInt("quantity"), true);
	 * gift.setRing(true); } else { gift = new
	 * MapleCSInventoryItem(MapleCharacter.getNextUniqueId(),
	 * rs.getInt("itemid"), rs.getInt("sn"), (short) rs.getInt("quantity"),
	 * true); } } gift.setExpire(rs.getTimestamp("expiredate"));
	 * gift.setSender(rs.getString("sender"));
	 * gift.setMessage(rs.getString("message")); csgifts.put(gift.getUniqueId(),
	 * gift); csitems.put(gift.getUniqueId(), gift); saveToDB(); } rs.close();
	 * ps.close(); ps = con.prepareStatement("DELETE FROM csgifts WHERE
	 * accountid = ?"); ps.setInt(1, accountid); ps.executeUpdate(); ps.close();
	 * 
	 * } catch (SQLException e) { log.info("Error loading cs inventory from the
	 * database", e); } }
	 * 
	 * public void saveToDB() { try { Connection con =
	 * DatabaseConnection.getConnection(); PreparedStatement ps =
	 * con.prepareStatement("DELETE FROM csinventory WHERE accountid = ?");
	 * ps.setInt(1, accountid); ps.executeUpdate(); ps.close();
	 * 
	 * ps = con.prepareStatement("INSERT INTO csinventory (accountid, uniqueid,
	 * itemid, sn, quantity, sender, message, expiredate, gift, isRing) VALUES
	 * (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"); for (MapleCSInventoryItem citem :
	 * csitems.values()) { ps.setInt(1, accountid); ps.setInt(2,
	 * citem.getUniqueId()); ps.setInt(3, citem.getItemId()); ps.setInt(4,
	 * citem.getSn()); ps.setInt(5, citem.getQuantity()); ps.setString(6,
	 * citem.getSender()); ps.setString(7, citem.getMessage());
	 * ps.setTimestamp(8, citem.getExpire()); ps.setBoolean(9, citem.isGift());
	 * ps.setBoolean(10, citem.isRing()); ps.executeUpdate(); } ps.close();
	 * 
	 * } catch (SQLException e) { log.info("Error saving cs inventory to the
	 * database", e); }
	 * 
	 * }
	 */
	public Map<Long, MapleCSInventoryItem> getCSGifts() {
		return csgifts;
	}

	public Map<Long, MapleCSInventoryItem> getCSItems() {
		return csitems;
	}

	public void addItem(MapleCSInventoryItem citem) {
		csitems.put(citem.getUniqueId(), citem);
	}

	public void removeItem(long uniqueid) {
		if (csgifts.containsKey(uniqueid)) {
			csgifts.remove(uniqueid);
		}
		if (csitems.containsKey(uniqueid)) {
			csitems.remove(uniqueid);
		}
	}

	public MapleCSInventoryItem getItem(long uniqueid) {
		MapleCSInventoryItem item = null;
		if (csitems.containsKey(uniqueid)) {
			item = csitems.get(uniqueid);
		}
		if (csgifts.containsKey(uniqueid)) {
			item = csgifts.get(uniqueid);
		}
		return item;
	}

	@Override
	public MapleItemsNameSpaceType GetSpaceType() {
		return MapleItemsNameSpaceType.CsInventory;
	}

	@Override
	public Collection<IItem> AllItems() {
		ArrayList<IItem> items = new ArrayList<IItem>();
		for (MapleCSInventoryItem mapleCSInventoryItem : csitems.values()) {
			items.add(mapleCSInventoryItem.toItem());
		}
		for (MapleCSInventoryItem mapleCSInventoryItem : csgifts.values()) {
			items.add(mapleCSInventoryItem.toItem());
		}
		return items;
	}

	public List<MapleCSInventoryItem> GetAllCSInventoryItems() {
		ArrayList<MapleCSInventoryItem> items = new ArrayList<MapleCSInventoryItem>();
		for (MapleCSInventoryItem mapleCSInventoryItem : csitems.values()) {
			items.add(mapleCSInventoryItem);
		}
		for (MapleCSInventoryItem mapleCSInventoryItem : csgifts.values()) {
			items.add(mapleCSInventoryItem);
		}
		return items;
	}

	public int GetAllCsInventoryCount() {
		return csitems.size() + csgifts.size();
	}

	public boolean CheckSpace(int count) {
		return (GetAllCsInventoryCount() + count) < MAX_SLOT;
	}

	public boolean CheckSpace() {
		return CheckSpace(1);
	}
}