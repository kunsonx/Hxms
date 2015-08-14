/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.ORM;

import java.util.ArrayList;
import java.util.List;
import java.sql.*;
import net.sf.odinms.database.DatabaseConnection;

/**
 *
 * @author Admin
 */
public class MapleItemMeso {

    private int itemid;
    private int meso;

    public int getItemid() {
        return itemid;
    }

    public void setItemid(int itemid) {
        this.itemid = itemid;
    }

    public int getMeso() {
        return meso;
    }

    public void setMeso(int meso) {
        this.meso = meso;
    }

    public static List<MapleItemMeso> list() {
        List<MapleItemMeso> list = new ArrayList<MapleItemMeso>();
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("select * from wz_im");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {                
                MapleItemMeso meso = new MapleItemMeso();
                meso.itemid = rs.getInt("itemid");
                meso.meso = rs.getInt("meso");
                list.add(meso);
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
