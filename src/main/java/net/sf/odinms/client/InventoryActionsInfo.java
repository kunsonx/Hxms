/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.client;

/**
 *
 * @author HXMS
 */
public class InventoryActionsInfo {

    public Item item;
    public int src, dst;
    public MapleInventoryType type;
    public InventoryActions action;

    public InventoryActionsInfo(Item item, MapleInventoryType type, InventoryActions action) {
        this.item = item;
        this.type = type;
        this.action = action;
    }

    public InventoryActionsInfo(Item item, int src, int dst, MapleInventoryType type, InventoryActions action) {
        this.item = item;
        this.src = src;
        this.dst = dst;
        this.type = type;
        this.action = action;
    }

    public enum InventoryActions {

        ADD(0),
        UPDATEQUANTITY(1),
        UPDATEPOSITION(2),
        DELETE(3);
        private int value;

        public int getValue() {
            return value;
        }

        private InventoryActions(int value) {
            this.value = value;
        }
    }
}
