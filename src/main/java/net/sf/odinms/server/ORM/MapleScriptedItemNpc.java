/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.ORM;

import java.util.ArrayList;
import java.util.List;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.odinms.database.DatabaseConnection;

/**
 *
 * @author Admin
 */
public class MapleScriptedItemNpc {

	private int id;
	private int npc;

	public MapleScriptedItemNpc() {
	}

	public MapleScriptedItemNpc(int id, int npc) {
		this.id = id;
		this.npc = npc;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getNpc() {
		return npc;
	}

	public void setNpc(int npc) {
		this.npc = npc;
	}

	public static List<MapleScriptedItemNpc> list() {
		List<MapleScriptedItemNpc> list = new ArrayList<MapleScriptedItemNpc>();
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con.prepareStatement("select * from wz_sin");
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				MapleScriptedItemNpc i = new MapleScriptedItemNpc();
				i.id = rs.getInt("id");
				i.npc = rs.getInt("npc");
				list.add(i);
			}
			rs.close();
			ps.close();
			con.close();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		return list;
	}
}
