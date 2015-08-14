package net.sf.odinms.client;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import net.sf.odinms.server.MapleItemInformationProvider;
import org.apache.log4j.Logger;

/**
 *
 * 冒险岛的库存
 */
public class MapleInventory implements Iterable<IItem>, InventoryContainer,
		MapleItemsNameSpace {

	protected Map<Short, IItem> inventory;
	private byte slotLimit;
	private MapleInventoryType type;
	private Logger log = Logger.getLogger(getClass());

	/**
	 * Creates a new instance of MapleInventory 2ni
	 *
	 * @param type
	 * @param slotLimit
	 */
	public MapleInventory(MapleInventoryType type, byte slotLimit) {
		this.inventory = new ConcurrentSkipListMap<Short, IItem>();
		this.slotLimit = slotLimit;
		this.type = type;
	}

	/**
	 * Returns the item with its slot id if it exists within the inventory,
	 * otherwise null is returned
	 *
	 * @param itemId
	 * @return
	 */
	public IItem findById(int itemId) {
		for (IItem item : inventory.values()) {
			if (item.getItemId() == itemId) {
				return item;
			}
		}
		return null;
	}

	public IItem findByUniqueId(int uniqueid) {
		for (IItem item : inventory.values()) {
			if (item.getUniqueid() == uniqueid) {
				return item;
			}
		}
		return null;
	}

	public int countById(int itemId) {
		int possesed = 0;
		for (IItem item : inventory.values()) {
			if (item.getItemId() == itemId) {
				possesed += item.getQuantity();
			}
		}
		return possesed;
	}

	public List<IItem> listById(int itemId) {
		List<IItem> ret = new ArrayList<IItem>();
		for (IItem item : inventory.values()) {
			if (item.getItemId() == itemId) {
				ret.add(item);
			}
		}
		/**
		 * the linkedhashmap does impose insert order as returned order but we
		 * can not guarantee that this is still the correct order - blargh, we
		 * could empty the map and reinsert in the correct order after each
		 * inventory addition, or we could use an array/list, it's only 255
		 * entries anyway...
		 */
		if (ret.size() > 1) {
			Collections.sort(ret);
		}
		return ret;
	}

	public Collection<IItem> list() {
		return inventory.values();
	}

	/**
	 * Adds the item to the inventory and returns the assigned slot id
	 *
	 * @param item
	 * @return
	 */
	public short addItem(IItem item) {
		short slotId = getNextFreeSlot();
		if (slotId < 0) {
			return -1;
		}
		inventory.put(slotId, item);
		item.setPosition(slotId);
		return slotId;
	}

	public void addFromDB(IItem item) {
		// if (item.getPosition() > 0 &&
		// (type.equals(MapleInventoryType.EQUIPPED) ||
		// type.equals(MapleInventoryType.Android) ||
		// type.equals(MapleInventoryType.Dragon))) {
		if (item.getPosition() < 0 && !type.equals(MapleInventoryType.EQUIPPED)) {
			log.debug("此物品属于未规定的类型 请在源码中处理");
		}
		inventory.put(item.getPosition(), item);
	}

	public boolean move(short sSlot, short dSlot, short slotMax) {
		MapleItemInformationProvider ii = MapleItemInformationProvider
				.getInstance();
		Item source = (Item) inventory.get(sSlot);
		Item target = (Item) inventory.get(dSlot);
		if (source == null) {
			throw new InventoryException("Trying to move empty slot");
		}
		if (target == null) {
			source.setPosition(dSlot);
			inventory.put(dSlot, source);
			inventory.remove(sSlot);
		} else if (target.getItemId() == source.getItemId()
				&& !ii.isThrowingStar(source.getItemId())
				&& !ii.isBullet(source.getItemId())) {
			if (type.getType() == MapleInventoryType.EQUIP.getType()) {
				swap(target, source);
			}
			if (source.getQuantity() + target.getQuantity() > slotMax) {
				short rest = (short) ((source.getQuantity() + target
						.getQuantity()) - slotMax);
				if ((rest + slotMax) != (source.getQuantity() + target
						.getQuantity())) {
					return false;
				}
				source.setQuantity(rest);
				target.setQuantity(slotMax);
			} else {
				target.setQuantity((short) (source.getQuantity() + target
						.getQuantity()));
				inventory.remove(sSlot);
			}
		} else {
			swap(target, source);
		}
		return true;
	}

	public byte getSlots() {
		return slotLimit;
	}

	public void setSlotLimit(byte num) {
		slotLimit = num;
	}

	private void swap(IItem source, IItem target) {
		inventory.remove(source.getPosition());
		inventory.remove(target.getPosition());
		short swapPos = source.getPosition();
		source.setPosition(target.getPosition());
		target.setPosition(swapPos);
		inventory.put(source.getPosition(), source);
		inventory.put(target.getPosition(), target);
	}

	public IItem getItem(short slot) {
		return inventory.get(slot);
	}

	public void removeItem(short slot) {
		removeItem(slot, (short) 1, false);
	}

	public void removeItem(short slot, short quantity, boolean allowZero) {
		IItem item = inventory.get(slot);
		if (item == null) {
			return;
		}
		item.setQuantity((short) (item.getQuantity() - quantity));
		if (item.getQuantity() < 0) {
			item.setQuantity((short) 0);
		}
		if (item.getQuantity() == 0 && !allowZero) {
			removeSlot(slot);
		}
	}

	public void removeSlot(short slot) {
		inventory.remove(slot);
	}

	public boolean isFull() {
		return inventory.size() >= slotLimit;
	}

	public boolean isFull(int margin) {
		return inventory.size() + margin >= slotLimit;
	}

	/**
	 * Returns the next empty slot id, -1 if the inventory is full
	 *
	 * @return
	 */
	public short getNextFreeSlot() {
		if (isFull()) {
			return -1;
		}
		for (short i = 1; i <= slotLimit; i++) {
			if (!inventory.keySet().contains(i)) {
				return i;
			}
		}
		return -1;
	}

	public MapleInventoryType getType() {
		return type;
	}

	public int countItem() {
		/*
		 * int it = (int) itemtype.getType(); try { Connection con =
		 * DatabaseConnection.getConnection(); PreparedStatement ps =
		 * con.prepareStatement("SELECT COUNT(*) AS c FROM inventoryitems WHERE
		 * characterid = ? AND inventorytype = ?"); ps.setInt(1, charid);
		 * ps.setInt(2, it); ResultSet rs = ps.executeQuery(); if (rs.next()) {
		 * return Integer.parseInt(rs.getString("c")); } rs.close(); ps.close();
		 * } catch (Exception e) { e.printStackTrace(); } return 0;
		 */
		return inventory.size();
	}

	@Override
	public Iterator<IItem> iterator() {
		return Collections.unmodifiableCollection(inventory.values())
				.iterator();
	}

	@Override
	public Collection<MapleInventory> allInventories() {
		return Collections.singletonList(this);
	}

	public List<Short> findAllById(int itemId) {
		List<Short> slots = new ArrayList<Short>();
		for (IItem item : inventory.values()) {
			if (item.getItemId() == itemId) {
				slots.add(item.getPosition());
			}
		}
		return slots;
	}

	@Override
	public MapleItemsNameSpaceType GetSpaceType() {
		return MapleItemsNameSpaceType.Inventory;
	}

	@Override
	public Collection<IItem> AllItems() {
		return Collections.unmodifiableCollection(inventory.values());
	}

	public void Claen() {
		inventory.clear();
	}
}
