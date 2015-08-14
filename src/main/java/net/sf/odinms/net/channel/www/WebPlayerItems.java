/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.www;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import net.sf.odinms.database.DatabaseConnection;
import org.apache.log4j.Logger;

/**
 *
 * @author Administrator
 */
public class WebPlayerItems implements java.io.Serializable{

    private WebUserPlayer player;
    private static Logger log = Logger.getLogger(WebPlayerItems.class);
    private int cId, aId;
    private List<WebPlayerItem> storages = new ArrayList<WebPlayerItem>();
    private List<WebPlayerItem> items = new ArrayList<WebPlayerItem>();

    public static WebPlayerItems get(int chrId, int aId, WebUserPlayer player) {
        WebPlayerItems item = new WebPlayerItems();
        item.aId = aId;
        item.cId = chrId;
        item.player = player;
        try {
            Connection con = DatabaseConnection.getConnection();
            CallableStatement call = con.prepareCall("call getItems(? , ?)");
            call.setInt(1, aId);
            call.setInt(2, chrId);
            call.execute();
            java.sql.ResultSet rs = call.getResultSet();
            while (rs.next()) {
                item.storages.add(new WebPlayerItem(rs.getLong("items_id"), rs.getInt("itemid"), rs.getInt("position"), rs.getInt("space"), rs.getInt("quantity")));
            }
            rs.close();
            call.getMoreResults();
            rs = call.getResultSet();
            while (rs.next()) {
                item.items.add(new WebPlayerItem(rs.getLong("items_id"), rs.getInt("itemid"), rs.getInt("position"), rs.getInt("space"), rs.getInt("quantity")));
            }
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return item;
    }

    public List<WebPlayerItem> getStorages() {
        return storages;
    }

    public List<WebPlayerItem> getItems() {
        return items;
    }

    public String HandlerAction(String action, long id) {
        WebPlayerItem item = getItemFromDbId(id);
        boolean isstorages = storages.contains(item);
        String ret = "";
        if (action.equals("del")) {
            boolean isOnline = checkOnlineStats(isstorages);
            if (isOnline) {
                ret = "检测到您的角色/账号在线，无法进行该操作！";
            } else {
                int rows = deleteFromDbId(id);
                if (rows > 0) {
                    ret = "ok";
                    storages.remove(item);
                    items.remove(item);
                } else {
                    ret = "操作失败，请重新刷新网页进行操作！";
                    player.setItems(null);
                }
            }
        }
        return ret;
    }

    public int deleteFromDbId(long id) {
        int result = 0;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("delete from items where items_id = ?");
            ps.setLong(1, id);
            result = ps.executeUpdate();
            ps.close();
            con.close();
        } catch (Exception e) {
            log.error("SQL EXCEPTION:", e);
        }
        return result;
    }

    public boolean checkOnlineStats(boolean isStorages) {
        boolean result = true;
        try {
            Connection con = DatabaseConnection.getConnection();
            // PreparedStatement ps = con.prepareStatement(isStorages ? "SELECT loggedin <> 0 FROM accounts where id = ?" : "SELECT loggedin <> 0 from characters WHERE id = ?");
            //  ps.setInt(1, isStorages ? aId : cId);
            PreparedStatement ps = con.prepareStatement("SELECT loggedin <> 0 FROM accounts where id = ?");
            ps.setInt(1, aId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            result = rs.getBoolean(1);
            rs.close();
            ps.close();
            con.close();
        } catch (Exception e) {
            log.error("SQL EXCEPTION:", e);
        }
        return result;
    }

    public WebPlayerItem getItemFromDbId(long id) {
        WebPlayerItem item = null;
        for (WebPlayerItem webPlayerItem : storages) {
            if (webPlayerItem.getDbid() == id) {
                item = webPlayerItem;
                break;
            }
        }
        if (item == null) {
            for (WebPlayerItem webPlayerItem : items) {
                if (webPlayerItem.getDbid() == id) {
                    item = webPlayerItem;
                    break;
                }
            }
        }
        return item;
    }
}
