/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.ORM;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import net.sf.odinms.database.DatabaseConnection;

/**
 *
 * @author Admin
 */
public class MapleItemWholePrice {

    private int itemid;
    private int wholePrice;

    public int getItemid() {
        return itemid;
    }

    public void setItemid(int itemid) {
        this.itemid = itemid;
    }

    public int getWholePrice() {
        return wholePrice;
    }

    public void setWholePrice(int wholePrice) {
        this.wholePrice = wholePrice;
    }

    public static List<MapleItemWholePrice> list() {
        List<MapleItemWholePrice> list = new ArrayList<MapleItemWholePrice>();
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("select * from wz_iwp");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {                
                MapleItemWholePrice w = new MapleItemWholePrice();
                w.itemid = rs.getInt("itemid");
                w.wholePrice = rs.getInt("wholePrice");
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
