/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.ORM;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import net.sf.odinms.database.DatabaseConnection;

/**
 *
 * @author Admin
 */
public class MapleItemPrice {

	private int itemid;
	private double price;

	public int getItemid() {
		return itemid;
	}

	public void setItemid(int itemid) {
		this.itemid = itemid;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public static List<MapleItemPrice> list() {
		List<MapleItemPrice> list = new ArrayList<MapleItemPrice>();
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con.prepareStatement("select * from wz_ip");
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				MapleItemPrice w = new MapleItemPrice();
				w.itemid = rs.getInt("itemid");
				w.price = rs.getDouble("price");
				list.add(w);
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
