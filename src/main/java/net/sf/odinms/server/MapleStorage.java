/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.odinms.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.MapleItemsNameSpace;
import net.sf.odinms.client.MapleItemsNameSpaceType;
import net.sf.odinms.tools.MaplePacketCreator;
import org.apache.log4j.Logger;

/**
 *
 * @author Matze
 */
public class MapleStorage implements MapleItemsNameSpace {

	private int accountid;
	private List<IItem> items = new LinkedList<IItem>();
	private long meso = 0;
	private byte slots = 8;
	private EnumMap<MapleInventoryType, List<IItem>> typeItems = new EnumMap<MapleInventoryType, List<IItem>>(
			MapleInventoryType.class);
	private static Logger log = Logger.getLogger(MapleStorage.class);

	public MapleStorage(int id, Connection connection) {
		try {
			this.accountid = id;
			PreparedStatement ps = connection
					.prepareStatement("SELECT * FROM storages WHERE accountid = ?");
			ps.setInt(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				slots = rs.getByte("slots");
				meso = rs.getLong("meso");
			} else {
				ps = connection
						.prepareStatement("INSERT INTO storages (accountid, meso) VALUES (?, ?)");
				ps.setInt(1, id);
				ps.setLong(2, 0);
				ps.executeUpdate();
			}
			ps.close();
			rs.close();
			typeItems.put(MapleInventoryType.EQUIP, new ArrayList<IItem>());
			typeItems.put(MapleInventoryType.USE, new ArrayList<IItem>());
			typeItems.put(MapleInventoryType.SETUP, new ArrayList<IItem>());
			typeItems.put(MapleInventoryType.ETC, new ArrayList<IItem>());
			typeItems.put(MapleInventoryType.CASH, new ArrayList<IItem>());
			typeItems.put(MapleInventoryType.UNDEFINED, new ArrayList<IItem>());
		} catch (SQLException ex) {
			log.error("仓库构造函数错误：", ex);
		}
	}

	public void AddItem(IItem item) {
		MapleInventoryType type = MapleItemInformationProvider.getInstance()
				.getInventoryType(item.getItemId());
		if (type.equals(MapleInventoryType.UNDEFINED)) {
			log.error("加入无类型物品：" + item.getItemId());
			if (item.getItemId() == 0) {
				return;
			}
		}
		typeItems.get(type).add(item);
		items.add(item);

	}

	/*
	 * public static MapleStorage create(int id) { try { Connection con =
	 * DatabaseConnection.getConnection(); PreparedStatement ps =
	 * con.prepareStatement
	 * ("INSERT INTO storages (accountid, slots, meso) VALUES (?, ?, ?)");
	 * ps.setInt(1, id); ps.setInt(2, 4); ps.setInt(3, 0); ps.executeUpdate();
	 * ps.close(); } catch (SQLException ex) {
	 * log.error("Error creating storage", ex); } return loadOrCreateFromDB(id);
	 * }
	 */

	/*
	 * public static MapleStorage loadOrCreateFromDB(int id) { MapleStorage ret
	 * = null; int storeId; try { DatabaseConnection.GetLock(id).lock();
	 * Connection con = DatabaseConnection.getConnection(); PreparedStatement ps
	 * = con.prepareStatement("SELECT * FROM storages WHERE accountid = ?");
	 * ps.setInt(1, id); ResultSet rs = ps.executeQuery(); if (!rs.next()) {
	 * rs.close(); ps.close(); return create(id); } else { storeId =
	 * rs.getInt("storageid"); ret = new MapleStorage(storeId, (byte)
	 * rs.getInt("slots"), rs.getInt("meso")); rs.close(); ps.close(); String
	 * sql = "SELECT * FROM inventoryitems " +
	 * "LEFT JOIN inventoryequipment USING (inventoryitemid) " +
	 * "WHERE storageid = ?"; ps = con.prepareStatement(sql); ps.setInt(1,
	 * storeId); rs = ps.executeQuery(); while (rs.next()) { MapleInventoryType
	 * type = MapleInventoryType.getByType((byte) rs.getInt("inventorytype"));
	 * if (type.equals(MapleInventoryType.EQUIP) ||
	 * type.equals(MapleInventoryType.EQUIPPED)) { int itemid =
	 * rs.getInt("itemid"); Equip equip = new Equip(itemid, (byte)
	 * rs.getInt("position")); equip.setOwner(rs.getString("owner"));
	 * equip.setQuantity((short) rs.getInt("quantity")); equip.setAcc((short)
	 * rs.getInt("acc")); equip.setAvoid((short) rs.getInt("avoid"));
	 * equip.setDex((short) rs.getInt("dex")); equip.setHands((short)
	 * rs.getInt("hands")); equip.setHp((short) rs.getInt("hp"));
	 * equip.setInt((short) rs.getInt("int")); equip.setJump((short)
	 * rs.getInt("jump")); equip.setLuk((short) rs.getInt("luk"));
	 * equip.setMatk((short) rs.getInt("matk")); equip.setMdef((short)
	 * rs.getInt("mdef")); equip.setMp((short) rs.getInt("mp"));
	 * equip.setSpeed((short) rs.getInt("speed")); equip.setStr((short)
	 * rs.getInt("str")); equip.setWatk((short) rs.getInt("watk"));
	 * equip.setWdef((short) rs.getInt("wdef")); equip.setUpgradeSlots((byte)
	 * rs.getInt("upgradeslots")); equip.setLocked((byte) rs.getInt("locked"));
	 * equip.setLevel((byte) rs.getInt("level")); equip.setFlag((byte)
	 * rs.getInt("flag")); equip.setVicious((short) rs.getInt("vicious"));
	 * equip.setPotential_1((short) rs.getInt("Potential_1"));
	 * equip.setPotential_2((short) rs.getInt("Potential_2"));
	 * equip.setPotential_3((short) rs.getInt("Potential_3"));
	 * equip.setIdentify((byte) rs.getInt("Identify"));
	 * equip.setStarlevel((byte) rs.getInt("Starlevel"));
	 * equip.setIdentified((byte) rs.getInt("Identified"));
	 * equip.setItemLevel((short) rs.getInt("ItemLevel"));
	 * equip.setItemExp((short) rs.getInt("ItemExp"));
	 * equip.setItemSkill((short) rs.getInt("ItemSkill"));
	 * equip.setDurability((short) rs.getInt("Durability"));
	 * equip.setPvpWatk((short) rs.getInt("pvpWatk")); ret.items.add(equip); }
	 * else { Item item = new Item(rs.getInt("itemid"), (byte)
	 * rs.getInt("position"), (short) rs.getInt("quantity"));
	 * item.setOwner(rs.getString("owner")); ret.items.add(item); } }
	 * rs.close(); ps.close(); } } catch (SQLException ex) {
	 * log.error("Error loading storage", ex); } finally {
	 * DatabaseConnection.GetLock(id).unlock(); } return ret; }
	 */
	public int getSlots() {
		return slots;
	}

	public void gainSlots(byte gain) {
		setSlots((byte) (gain + getSlots()));
	}

	public void setSlots(byte set) {
		this.slots = set;
	}

	public void saveToDB(Connection con) throws SQLException {
		PreparedStatement ps = con
				.prepareStatement("UPDATE storages SET slots = ?, meso = ? WHERE accountid = ?");
		ps.setInt(1, slots);
		ps.setLong(2, meso);
		ps.setInt(3, accountid);
		ps.executeUpdate();
		ps.close();
	}

	/*
	 * public void saveToDB(Connection con) { try { PreparedStatement ps =
	 * con.prepareStatement
	 * ("UPDATE storages SET slots = ?, meso = ? WHERE storageid = ?");
	 * ps.setInt(1, slots); ps.setInt(2, meso); ps.setInt(3, id);
	 * ps.executeUpdate(); ps.close(); ps =
	 * con.prepareStatement("DELETE FROM inventoryitems WHERE storageid = ?");
	 * ps.setInt(1, id); ps.executeUpdate(); ps.close(); ps =
	 * con.prepareStatement(
	 * "INSERT INTO inventoryitems (storageid, itemid, inventorytype, position, quantity, owner) VALUES (?, ?, ?, ?, ?, ?)"
	 * ); PreparedStatement pse = con.prepareStatement(
	 * "INSERT INTO inventoryequipment VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
	 * ); MapleInventoryType type; for (IItem item : items) { ps.setInt(1, id);
	 * ps.setInt(2, item.getItemId()); type =
	 * MapleItemInformationProvider.getInstance
	 * ().getInventoryType(item.getItemId()); ps.setInt(3, type.getType());
	 * ps.setInt(4, item.getPosition()); ps.setInt(5, item.getQuantity());
	 * ps.setString(6, item.getOwner()); ps.executeUpdate(); ResultSet rs =
	 * ps.getGeneratedKeys(); int itemid; if (rs.next()) { itemid =
	 * rs.getInt(1); } else { throw new
	 * DatabaseException("Inserting char failed."); } rs.close(); if
	 * (type.equals(MapleInventoryType.EQUIP)) { pse.setInt(1, itemid); IEquip
	 * equip = (IEquip) item; pse.setInt(2, equip.getUpgradeSlots());
	 * pse.setInt(3, equip.getLevel()); pse.setInt(4, equip.getStr());
	 * pse.setInt(5, equip.getDex()); pse.setInt(6, equip.getInt());
	 * pse.setInt(7, equip.getLuk()); pse.setInt(8, equip.getHp());
	 * pse.setInt(9, equip.getMp()); pse.setInt(10, equip.getWatk());
	 * pse.setInt(11, equip.getMatk()); pse.setInt(12, equip.getWdef());
	 * pse.setInt(13, equip.getMdef()); pse.setInt(14, equip.getAcc());
	 * pse.setInt(15, equip.getAvoid()); pse.setInt(16, equip.getHands());
	 * pse.setInt(17, equip.getSpeed()); pse.setInt(18, equip.getJump());
	 * pse.setInt(19, equip.getLocked()); pse.setBoolean(20, equip.isRing());
	 * pse.setInt(21, equip.getVicious()); //pse.setInt(22, item.getFlag());
	 * pse.setInt(22, equip.getFlag()); //潜能 pse.setInt(23,
	 * equip.getPotential_1()); pse.setInt(24, equip.getPotential_2());
	 * pse.setInt(25, equip.getPotential_3()); pse.setInt(26,
	 * equip.getIdentify()); pse.setInt(27, equip.getStarlevel());
	 * pse.setInt(28, equip.getIdentified()); //道具等级 道具经验值 耐久度 pse.setInt(29,
	 * equip.getItemLevel()); pse.setInt(30, equip.getItemExp()); pse.setInt(31,
	 * equip.getItemSkill()); pse.setInt(32, equip.getDurability());
	 * pse.setInt(33, equip.getPvpWatk()); pse.executeUpdate(); } } ps.close();
	 * pse.close(); } catch (SQLException ex) {
	 * log.error("Error saving storage", ex); } }
	 */

	public IItem takeOut(byte slot) {
		IItem ret = items.remove(slot);
		MapleInventoryType type = MapleItemInformationProvider.getInstance()
				.getInventoryType(ret.getItemId());
		List<IItem> typeitem = typeItems.get(type);
		for (Iterator<IItem> it = typeitem.iterator(); it.hasNext();) {
			IItem iItem = it.next();
			if (iItem.equals(ret)) {
				it.remove();
				break;
			}
		}
		return ret;
	}

	public void store(IItem item) {
		/*
		 * items.add(item); MapleInventoryType type =
		 * MapleItemInformationProvider
		 * .getInstance().getInventoryType(item.getItemId());
		 * typeItems.put(type, new ArrayList<IItem>(filterItems(type)));
		 */
		AddItem(item);
	}

	public List<IItem> getItems() {
		return Collections.unmodifiableList(items);
	}

	public List<IItem> filterItems(MapleInventoryType type) {
		List<IItem> ret = new LinkedList<IItem>();
		MapleItemInformationProvider ii = MapleItemInformationProvider
				.getInstance();
		for (IItem item : items) {
			if (ii.getInventoryType(item.getItemId()) == type) {
				ret.add(item);
			}
		}
		return ret;
	}

	public byte getSlot(MapleInventoryType type, byte slot) {
		// MapleItemInformationProvider ii =
		// MapleItemInformationProvider.getInstance();
		byte ret = 0;
		for (IItem item : items) {
			if (item == typeItems.get(type).get(slot)) {
				return ret;
			}
			ret++;
		}
		return -1;
	}

	public void sendStorage(MapleClient c, int npcId) {
		final MapleItemInformationProvider ii = MapleItemInformationProvider
				.getInstance();
		// sort by inventorytype to avoid confusion
		Collections.sort(items, new Comparator<IItem>() {
			@Override
			public int compare(IItem o1, IItem o2) {
				if (ii.getInventoryType(o1.getItemId()).getType() < ii
						.getInventoryType(o2.getItemId()).getType()) {
					return -1;
				} else if (ii.getInventoryType(o1.getItemId()) == ii
						.getInventoryType(o2.getItemId())) {
					return 0;
				} else {
					return 1;
				}
			}
		});
		/*
		 * for (MapleInventoryType type : MapleInventoryType.values()) {
		 * typeItems.put(type, new ArrayList<IItem>(filterItems(type))); }
		 */
		c.getSession().write(
				MaplePacketCreator.getStorage(npcId, slots, meso, this));
	}

	public void sendStored(MapleClient c, MapleInventoryType type) {
		c.getSession().write(
				MaplePacketCreator.storeStorage(slots, type,
						typeItems.get(type)));
	}

	public void sendTakenOut(MapleClient c, MapleInventoryType type) {
		c.getSession().write(
				MaplePacketCreator.takeOutStorage(slots, type,
						typeItems.get(type)));
	}

	public long getMeso() {
		return meso;
	}

	public void setMeso(long meso) {
		if (meso < 0) {
			throw new RuntimeException();
		}
		this.meso = meso;
	}

	public void sendMeso(MapleClient c) {
		c.getSession().write(MaplePacketCreator.mesoStorage(slots, meso));
	}

	public boolean isFull() {
		return items.size() >= slots;
	}

	public void close() {
		// typeItems.clear();
	}

	@Override
	public MapleItemsNameSpaceType GetSpaceType() {
		return MapleItemsNameSpaceType.Storages;
	}

	@Override
	public Collection<IItem> AllItems() {
		return Collections.unmodifiableList(items);
	}

	public EnumMap<MapleInventoryType, List<IItem>> AllEnumMap() {
		return typeItems;
	}
}
