/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server;

import net.sf.odinms.client.MapleInventoryType;

/**
 *
 * @author Admin
 */
public class MapleItemInventryType {

	private int id;
	private MapleInventoryType type;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public MapleInventoryType getType() {
		return type;
	}

	public byte getTypevalue() {
		return type.getType();
	}

	public void setTypevalue(byte value) {
		this.type = MapleInventoryType.getByType(value);
	}

	public void setType(MapleInventoryType type) {
		this.type = type;
	}
}
