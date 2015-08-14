/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.net.channel.www;

import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.server.MapleItemInformationProvider;

/**
 *
 * @author Administrator
 */
public class WebPlayerItem {

    private long dbid;
    private int id;
    private int position;
    private int space;
    private int quantity;

    public WebPlayerItem() {
    }

    public WebPlayerItem(int id, int position) {
        this.id = id;
        this.position = position;
    }

    public WebPlayerItem(long dbid, int id, int position) {
        this.dbid = dbid;
        this.id = id;
        this.position = position;
    }

    public WebPlayerItem(long dbid, int id, int position, int space) {
        this.dbid = dbid;
        this.id = id;
        this.position = position;
        this.space = space;
    }

    public WebPlayerItem(long dbid, int id, int position, int space, int quantity) {
        this.dbid = dbid;
        this.id = id;
        this.position = position;
        this.space = space;
        this.quantity = quantity;
    }

    public int getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public String getName() {
        String name = MapleItemInformationProvider.getInstance().getName(id);
        if (name == null) {
            name = "ID:" + String.valueOf(getId());
        }
        return name;
    }

    public String getPositionStr() {
        return "第" + (position < 0 ? position * -1 : position) + "格";
    }

    public String getDesc() {
        return space > 0 ? "仓库栏" : (space == -1 ? "背包" : "商城");
    }

    public String getQM() {
        if (space == -1) {
            return getDesc() + (position > 0 ? MapleItemInformationProvider.getInstance().getInventoryType(id).getName() : MapleInventoryType.EQUIPPED.getName()) + getPositionStr();
        } else {
            return getDesc();
        }
    }

    public long getDbid() {
        return dbid;
    }

    public int getSpace() {
        return space;
    }

    public int getQuantity() {
        return quantity;
    }
}
