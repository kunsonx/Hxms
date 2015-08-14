/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.ORM;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

/**
 *
 * @author Admin
 */
public class MapleExpCardListInfo {

    private int itemid;
    private MapleExpCardInfo[] all = new MapleExpCardInfo[7];

    public int getItemid() {
        return itemid;
    }

    public void setItemid(int itemid) {
        this.itemid = itemid;
    }

    public Set<MapleExpCardInfo> getAll() {
        Set set = new HashSet();
        for (MapleExpCardInfo mapleExpCardInfo : all) {
            if (mapleExpCardInfo != null) {
                set.add(mapleExpCardInfo);
            }
        }
        return set;
    }

    public void setAll(Set<MapleExpCardInfo> all) {
        for (MapleExpCardInfo mapleExpCardInfo : all) {
            if (mapleExpCardInfo != null) {
                this.all[mapleExpCardInfo.getId() - 1] = mapleExpCardInfo;
            }
        }
    }

    public boolean HasRate() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/ShangHai"));
        int week = cal.get(Calendar.DAY_OF_WEEK);
        if (all[week - 1] != null) {
            return all[week - 1].HasRate(cal);
        }
        return false;
    }

    public void addInstance(MapleExpCardInfo info) {
        all[info.getId() - 1] = info;
    }
}
