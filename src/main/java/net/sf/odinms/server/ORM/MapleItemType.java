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
public class MapleItemType {

    private int itemid;
    private String type;

    public int getItemid() {
        return itemid;
    }

    public void setItemid(int itemid) {
        this.itemid = itemid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public static List<MapleItemType> list() {
        List<MapleItemType> list = new ArrayList<MapleItemType>();
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("select * from wz_it");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                MapleItemType w = new MapleItemType();
                w.itemid = rs.getInt("itemid");
                w.type = rs.getString("type");
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
