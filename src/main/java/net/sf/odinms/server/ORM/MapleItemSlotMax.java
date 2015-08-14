/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.ORM;

import java.util.List;
import java.sql.*;
import java.util.ArrayList;
import net.sf.odinms.database.DatabaseConnection;

/**
 *
 * @author Admin
 */
public class MapleItemSlotMax {

	private int itemid;
	private int slotmax;

	public int getItemid() {
		return itemid;
	}

	public void setItemid(int itemid) {
		this.itemid = itemid;
	}

	public int getSlotmax() {
		return slotmax;
	}

	public void setSlotmax(int slotmax) {
		this.slotmax = slotmax;
	}

	public static List<MapleItemSlotMax> list() {
		List<MapleItemSlotMax> list = new ArrayList<MapleItemSlotMax>();
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con.prepareStatement("select * from wz_ism");
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				MapleItemSlotMax max = new MapleItemSlotMax();
				max.itemid = rs.getInt("itemid");
				max.slotmax = rs.getInt("slotmax");
				list.add(max);
			}
			rs.close();
			ps.close();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}
}
