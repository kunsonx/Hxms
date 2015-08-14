/*
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * 
 */
package net.sf.odinms.client;

/**
 *
 * @author HXMS
 */
public enum MapleItemsNameSpaceType {

    Unknown(-1),
    Inventory(0),
    Storages(1),
    CsInventory(2),
    HiredMerchant(3);
    final int type;

    MapleItemsNameSpaceType(int type) {
        this.type = type;
    }

    public int GetType() {
        return type;
    }

    public static MapleItemsNameSpaceType GetNameSpaceByDbId(int dbid) {
        for (MapleItemsNameSpaceType mapleItemsNameSpace : values()) {
            if ((dbid / 10) == mapleItemsNameSpace.type) {
                return mapleItemsNameSpace;
            }
        }
        return Unknown;
    }

    public static MapleInventoryType GetInventoryTypeByDbId(int dbid) {
        return MapleInventoryType.getByType((byte) (dbid % 10));
    }

    public int GetDbId(MapleInventoryType type) {
        return GetType() * 10 + type.type;
    }
}
