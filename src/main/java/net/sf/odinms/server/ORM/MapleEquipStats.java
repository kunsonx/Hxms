/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.ORM;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.tools.MySql_Uuid_Short;

/**
 *
 * @author Admin
 */
public class MapleEquipStats {

	private int itemid;
	private Map<String, Integer> stats = new HashMap<String, Integer>();

	public int getItemid() {
		return itemid;
	}

	public void setItemid(int itemid) {
		this.itemid = itemid;
	}

	public void add(String key, int value) {
		stats.put(key, value);
	}

	public Set<MapleEquipStatsInfo> getStats() {
		HashSet<MapleEquipStatsInfo> infos = new HashSet<MapleEquipStatsInfo>();
		for (String string : stats.keySet()) {
			MapleEquipStatsInfo info = new MapleEquipStatsInfo();
			info.setName(string);
			info.setValue(stats.get(string));
			infos.add(info);
		}
		return infos;
	}

	public void setStats(Set<MapleEquipStatsInfo> stats) {
		for (MapleEquipStatsInfo mapleEquipStatsInfo : stats) {
			this.stats.put(mapleEquipStatsInfo.getName(),
					mapleEquipStatsInfo.getValue());
		}
	}

	public Map<String, Integer> getBaseStats() {
		return stats;
	}

	public static void savetodb(MapleEquipStats status) {
		try {
			MySql_Uuid_Short sho = new MySql_Uuid_Short();
			java.sql.Connection con = DatabaseConnection.getConnection();
			java.sql.PreparedStatement ps = con
					.prepareStatement("INSERT INTO `wz_es`  VALUES (?)");
			ps.setInt(1, status.itemid);
			ps.executeUpdate();
			ps = con.prepareStatement("INSERT INTO `wz_es_s` VALUES (? , ?, ? ,?)");
			for (String string : status.stats.keySet()) {
				ps.setLong(1, (Long) sho.generate(null, null));
				ps.setString(2, string);
				ps.setInt(3, status.stats.get(string));
				ps.setInt(4, status.itemid);
				ps.execute();
			}
			// ps.executeBatch();
			ps.close();
			con.close();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	public static void deleteall() {
		try {
			java.sql.Connection con = DatabaseConnection.getConnection();
			java.sql.PreparedStatement ps = con
					.prepareStatement("delete from wz_es_s");
			ps.executeUpdate();
			ps.close();
			ps = con.prepareStatement("delete from wz_es");
			ps.executeUpdate();
			ps.close();
			con.close();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}
}
