/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.server.ORM;

import java.util.Calendar;

/**
 *
 * @author Admin
 */
public class MapleExpCardInfo {

    private long db_id;
    private int id;
    private int starthour, stophour;

    public long getDb_id() {
        return db_id;
    }

    public void setDb_id(long db_id) {
        this.db_id = db_id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStarthour() {
        return starthour;
    }

    public void setStarthour(int starthour) {
        this.starthour = starthour;
    }

    public int getStophour() {
        return stophour;
    }

    public void setStophour(int stophour) {
        this.stophour = stophour;
    }

    public boolean HasRate(Calendar cal) {
        if (cal.get(Calendar.HOUR_OF_DAY) >= starthour && cal.get(Calendar.HOUR_OF_DAY) <= stophour) {
            return true;
        }
        return false;
    }
}
