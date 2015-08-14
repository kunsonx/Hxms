/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import net.sf.odinms.database.DatabaseConnection;

/**
 *
 * @author Administrator
 */
public class MapleItemIcon {

    private static String IconPath = "d://ItemIcon";
    private static Map<Integer, MapleItemIcon> instances = new HashMap<Integer, MapleItemIcon>();
    private int id;
    private byte[] data;

    public static void writeToDb() {
        try {
            File file = new File(IconPath);

            Connection c = DatabaseConnection.getConnection();

            PreparedStatement ps = c.prepareStatement("delete from wz_ic");
            ps.executeUpdate();
            ps.close();

            ps = c.prepareStatement("INSERT INTO wz_ic VALUES (? , ?)");
            for (File o_f : file.listFiles()) {
                ps.setInt(1, Integer.parseInt(o_f.getName().replace(".png", "")));
                ps.setBinaryStream(2, new FileInputStream(o_f));
                ps.addBatch();
            }
            ps.executeBatch();
            ps.close();

            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static MapleItemIcon get(int id) {
        MapleItemIcon ret = null;
        if (!instances.containsKey(id)) {
            try {
                Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement("select data from wz_ic where id = ?");
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    InputStream is = rs.getBinaryStream(1);
                    ret = new MapleItemIcon();
                    ret.id = id;
                    ret.data = new byte[is.available()];
                    is.read(ret.data);
                    is.close();
                }
                instances.put(id, ret);
                rs.close();
                ps.close();
                c.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            ret = instances.get(id);
        }
        return ret;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
